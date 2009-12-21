/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 * Daniel Le Berre - Fix in the encoding and the optimization function
 * Alban Browaeys - Optimized string concatenation in bug 251357
 * Jed Anderson - switch from opb files to API calls to DependencyHelper in bug 200380
 ******************************************************************************/
package org.eclipse.equinox.p2.cudf.solver;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.cudf.Main;
import org.eclipse.equinox.p2.cudf.metadata.*;
import org.eclipse.equinox.p2.cudf.query.*;
import org.eclipse.osgi.util.NLS;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.pb.tools.WeightedObject;
import org.sat4j.specs.*;

/**
 * This class is the interface between SAT4J and the planner. It produces a
 * boolean satisfiability problem, invokes the solver, and converts the solver result
 * back into information understandable by the planner.
 */
public class Projector {
	private static final boolean DEBUG = false; //SET THIS TO FALSE FOR THE COMPETITION
	private static final boolean TIMING = true; //SET THIS TO FALSE FOR THE COMPETITION
	private static boolean DEBUG_ENCODING = false;
	private QueryableArray picker;

	private Map noopVariables; //key IU, value AbstractVariable
	private List abstractVariables;

	private TwoTierMap slice; //The IUs that have been considered to be part of the problem

	DependencyHelper dependencyHelper;
	private Collection solution;
	private Collection assumptions;

	private MultiStatus result;

	private InstallableUnit entryPoint;

	static class AbstractVariable {
		public String toString() {
			return "AbstractVariable: " + hashCode(); //$NON-NLS-1$
		}
	}

	public Projector(QueryableArray q) {
		picker = q;
		noopVariables = new HashMap();
		slice = new TwoTierMap();
		abstractVariables = new ArrayList();
		result = new MultiStatus(Main.PLUGIN_ID, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
		assumptions = new ArrayList();
	}

	public void encode(InstallableUnit entryPointIU, String optFunction) {
		this.entryPoint = entryPointIU;
		try {
			long start = 0;
			if (TIMING) {
				start = System.currentTimeMillis();
				Tracing.debug("Starting projection ... "); //$NON-NLS-1$
			}
			IPBSolver solver;
			if (DEBUG_ENCODING) {
				solver = SolverFactory.newOPBStringSolver();
			} else {
				solver = SolverFactory.newEclipseP2();
			}
			// solver.setTimeoutOnConflicts(1000);
			solver.setTimeout(250);
			solver.setVerbose(true);
			solver.setLogPrefix("# ");
			//			Collector collector = picker.query(InstallableUnitQuery.ANY, new Collector(), null);
			dependencyHelper = new DependencyHelper(solver);

			Iterator iusToEncode = picker.iterator();
			if (DEBUG) {
				List iusToOrder = new ArrayList();
				while (iusToEncode.hasNext()) {
					iusToOrder.add(iusToEncode.next());
				}
				Collections.sort(iusToOrder);
				iusToEncode = iusToOrder.iterator();
			}
			while (iusToEncode.hasNext()) {
				InstallableUnit iuToEncode = (InstallableUnit) iusToEncode.next();
				if (iuToEncode != entryPointIU) {
					processIU(iuToEncode, false);
				}
			}
			createConstraintsForSingleton();

			createMustHave(entryPointIU);

			setObjectiveFunction(getOptimizationFactory(optFunction).createOptimizationFunction(entryPointIU));
			if (TIMING) {
				long stop = System.currentTimeMillis();
				Tracing.debug("Projection completed: " + (stop - start) + "ms."); //$NON-NLS-1$
			}
			if (DEBUG_ENCODING) {
				System.out.println(solver.toString());
			}
		} catch (IllegalStateException e) {
			result.add(new Status(IStatus.ERROR, Main.PLUGIN_ID, e.getMessage(), e));
		} catch (ContradictionException e) {
			result.add(new Status(IStatus.ERROR, Main.PLUGIN_ID, Messages.Planner_Unsatisfiable_problem));
		}
	}

	private OptimizationFunction getOptimizationFactory(String optFunction) {
		OptimizationFunction function = null;
		if ("p2".equalsIgnoreCase(optFunction)) {
			function = new P2OptimizationFunction(); //p2
		} else if ("paranoid".equalsIgnoreCase(optFunction)) {
			function = new ParanoidOptimizationFunction(); //paranoid
		} else if ("trendy".equalsIgnoreCase(optFunction)) {
			function = new TrendyOptimizationFunction(); // trendy
		} else {
			throw new IllegalArgumentException("Unknown optimisation function: " + optFunction);
		}
		System.out.println("# Optimization function: " + function.getName());
		function.slice = slice;
		function.noopVariables = noopVariables;
		function.abstractVariables = abstractVariables;
		function.picker = picker;
		return function;
	}

	private void setObjectiveFunction(List weightedObjects) {
		if (weightedObjects == null)
			return;
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (Iterator i = weightedObjects.iterator(); i.hasNext();) {
				WeightedObject object = (WeightedObject) i.next();
				if (b.length() > 0)
					b.append(", "); //$NON-NLS-1$
				b.append(object.getWeight());
				b.append(' ');
				b.append(object.thing);
			}
			Tracing.debug("objective function: " + b); //$NON-NLS-1$
		}
		dependencyHelper.setObjectiveFunction((WeightedObject[]) weightedObjects.toArray(new WeightedObject[weightedObjects.size()]));
	}

	private void createMustHave(InstallableUnit iu) throws ContradictionException {
		processIU(iu, true);
		if (DEBUG) {
			Tracing.debug(iu + "=1"); //$NON-NLS-1$
		}
		// dependencyHelper.setTrue(variable, new Explanation.IUToInstall(iu));
		assumptions.add(iu);
	}

	private void createNegation(InstallableUnit iu, IRequiredCapability req) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(iu + "=0"); //$NON-NLS-1$
		}
		dependencyHelper.setFalse(iu, new Explanation.MissingIU(iu, req));
	}

	private void expandNegatedRequirement(IRequiredCapability req, InstallableUnit iu, List optionalAbstractRequirements, boolean isRootIu) throws ContradictionException {
		IRequiredCapability negatedReq = ((NotRequirement) req).getRequirement();
		List matches = getApplicableMatches(negatedReq);
		matches.remove(iu);
		if (matches.isEmpty()) {
			return;
		}
		Explanation explanation;
		if (isRootIu) {
			InstallableUnit reqIu = (InstallableUnit) matches.iterator().next();
			explanation = new Explanation.IUToInstall(reqIu);
		} else {
			explanation = new Explanation.HardRequirement(iu, req);
		}
		createNegationImplication(iu, matches, explanation);
	}

	private void expandRequirement(IRequiredCapability req, InstallableUnit iu, List optionalAbstractRequirements, boolean isRootIu) throws ContradictionException {
		if (req.isNegation()) {
			expandNegatedRequirement(req, iu, optionalAbstractRequirements, isRootIu);
			return;
		}
		List matches = getApplicableMatches(req);
		if (!req.isOptional()) {
			if (matches.isEmpty()) {
				missingRequirement(iu, req);
			} else {
				if (req.getArity() == 1) {
					createAtMostOne((InstallableUnit[]) matches.toArray(new InstallableUnit[matches.size()]));
					return;
				}
				InstallableUnit reqIu = (InstallableUnit) matches.iterator().next();
				Explanation explanation = new Explanation.IUToInstall(reqIu);
				createImplication(iu, matches, explanation);
			}
		} else {
			if (!matches.isEmpty()) {
				AbstractVariable abs = getAbstractVariable();
				createImplication(new Object[] {abs, iu}, matches, Explanation.OPTIONAL_REQUIREMENT);
				optionalAbstractRequirements.add(abs);
			}
		}
	}

	private void expandRequirements(IRequiredCapability[] reqs, InstallableUnit iu, boolean isRootIu) throws ContradictionException {
		if (reqs.length == 0) {
			return;
		}
		List optionalAbstractRequirements = new ArrayList();
		for (int i = 0; i < reqs.length; i++) {
			expandRequirement(reqs[i], iu, optionalAbstractRequirements, isRootIu);
		}
		createOptionalityExpression(iu, optionalAbstractRequirements);
	}

	public void processIU(InstallableUnit iu, boolean isRootIU) throws ContradictionException {
		slice.put(iu.getId(), iu.getVersion(), iu);
		expandRequirements(getRequiredCapabilities(iu), iu, isRootIU);
	}

	private IRequiredCapability[] getRequiredCapabilities(InstallableUnit iu) {
		return iu.getRequiredCapabilities();
	}

	private void missingRequirement(InstallableUnit iu, IRequiredCapability req) throws ContradictionException {
		result.add(new Status(IStatus.WARNING, Main.PLUGIN_ID, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
		createNegation(iu, req);
	}

	/**
	 * @param req
	 * @return a list of mandatory requirements if any, an empty list if req.isOptional().
	 */
	private List getApplicableMatches(IRequiredCapability req) {
		List target = new ArrayList();
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			InstallableUnit match = (InstallableUnit) iterator.next();
			target.add(match);
		}
		return target;
	}

	//This will create as many implication as there is element in the right argument
	private void createNegationImplication(Object left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (Iterator iterator = right.iterator(); iterator.hasNext();) {
			dependencyHelper.implication(new Object[] {left}).impliesNot(iterator.next()).named(name);
		}

	}

	private void createImplication(Object left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(new Object[] {left}).implies(right.toArray()).named(name);
	}

	private void createImplication(Object[] left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + Arrays.asList(left) + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(left).implies(right.toArray()).named(name);
	}

	//Create constraints to deal with singleton
	//When there is a mix of singleton and non singleton, several constraints are generated
	private void createConstraintsForSingleton() throws ContradictionException {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() < 2)
				continue;

			Collection conflictingVersions = conflictingEntries.values();
			List singletons = new ArrayList();
			List nonSingletons = new ArrayList();
			for (Iterator conflictIterator = conflictingVersions.iterator(); conflictIterator.hasNext();) {
				InstallableUnit iu = (InstallableUnit) conflictIterator.next();
				if (iu.isSingleton()) {
					singletons.add(iu);
				} else {
					nonSingletons.add(iu);
				}
			}
			if (singletons.isEmpty())
				continue;

			InstallableUnit[] singletonArray;
			if (nonSingletons.isEmpty()) {
				singletonArray = (InstallableUnit[]) singletons.toArray(new InstallableUnit[singletons.size()]);
				createAtMostOne(singletonArray);
			} else {
				singletonArray = (InstallableUnit[]) singletons.toArray(new InstallableUnit[singletons.size() + 1]);
				for (Iterator iterator2 = nonSingletons.iterator(); iterator2.hasNext();) {
					singletonArray[singletonArray.length - 1] = (InstallableUnit) iterator2.next();
					createAtMostOne(singletonArray);
				}
			}
		}
	}

	private void createIncompatibleValues(AbstractVariable v1, AbstractVariable v2) throws ContradictionException {
		AbstractVariable[] vars = {v1, v2};
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < vars.length; i++) {
				b.append(vars[i].toString());
			}
			Tracing.debug("At most 1 of " + b); //$NON-NLS-1$
		}
		dependencyHelper.atMost(1, vars).named(Explanation.OPTIONAL_REQUIREMENT);
	}

	private void createOptionalityExpression(InstallableUnit iu, List optionalRequirements) throws ContradictionException {
		if (optionalRequirements.isEmpty())
			return;
		AbstractVariable noop = getNoOperationVariable(iu);
		for (Iterator i = optionalRequirements.iterator(); i.hasNext();) {
			AbstractVariable abs = (AbstractVariable) i.next();
			createIncompatibleValues(abs, noop);
		}
		optionalRequirements.add(noop);
		createImplication(iu, optionalRequirements, Explanation.OPTIONAL_REQUIREMENT);
	}

	private AbstractVariable getNoOperationVariable(InstallableUnit iu) {
		AbstractVariable v = (AbstractVariable) noopVariables.get(iu);
		if (v == null) {
			v = new AbstractVariable();
			noopVariables.put(iu, v);
		}
		return v;
	}

	private void createAtMostOne(InstallableUnit[] ius) throws ContradictionException {
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < ius.length; i++) {
				b.append(ius[i].toString());
			}
			Tracing.debug("At most 1 of " + b); //$NON-NLS-1$
		}
		dependencyHelper.atMost(1, ius).named(new Explanation.Singleton(ius));
	}

	private AbstractVariable getAbstractVariable() {
		AbstractVariable abstractVariable = new AbstractVariable();
		abstractVariables.add(abstractVariable);
		return abstractVariable;
	}

	public IStatus invokeSolver() {
		if (result.getSeverity() == IStatus.ERROR)
			return result;
		// CNF filename is given on the command line
		long start = System.currentTimeMillis();
		if (TIMING)
			Tracing.debug("Invoking solver ..."); //$NON-NLS-1$
		try {
			if (dependencyHelper.hasASolution(assumptions)) {
				if (DEBUG) {
					Tracing.debug("Satisfiable !"); //$NON-NLS-1$
				}
				backToIU();
				long stop = System.currentTimeMillis();
				if (TIMING)
					Tracing.debug("Solver solution found: " + (stop - start) + "ms."); //$NON-NLS-1$
			} else {
				long stop = System.currentTimeMillis();
				if (DEBUG) {
					Tracing.debug("Unsatisfiable !"); //$NON-NLS-1$
					Tracing.debug("Solver solution NOT found: " + (stop - start)); //$NON-NLS-1$
				}
				result.merge(new Status(IStatus.ERROR, Main.PLUGIN_ID, Messages.Planner_Unsatisfiable_problem));
			}
		} catch (TimeoutException e) {
			result.merge(new Status(IStatus.ERROR, Main.PLUGIN_ID, Messages.Planner_Timeout));
		} catch (Exception e) {
			result.merge(new Status(IStatus.ERROR, Main.PLUGIN_ID, Messages.Planner_Unexpected_problem, e));
		}
		if (DEBUG)
			System.out.println();
		return result;
	}

	private void backToIU() {
		solution = new ArrayList();
		IVec sat4jSolution = dependencyHelper.getSolution();
		for (Iterator i = sat4jSolution.iterator(); i.hasNext();) {
			Object var = i.next();
			if (var instanceof InstallableUnit) {
				InstallableUnit iu = (InstallableUnit) var;
				if (iu == entryPoint)
					continue;
				solution.add(iu);
			}
		}
	}

	private void printSolution(Collection state) {
		ArrayList l = new ArrayList(state);
		Collections.sort(l);
		Tracing.debug("Solution:"); //$NON-NLS-1$
		Tracing.debug("Numbers of IUs selected: " + l.size()); //$NON-NLS-1$
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			Tracing.debug(iterator.next().toString());
		}
	}

	public Collection extractSolution() {
		if (DEBUG)
			printSolution(solution);
		return solution;
	}

	public Set getExplanation() {
		ExplanationJob job = new ExplanationJob(dependencyHelper);
		job.schedule();
		IProgressMonitor pm = new NullProgressMonitor();
		pm.beginTask(Messages.Planner_NoSolution, 1000);
		try {
			synchronized (job) {
				while (job.getExplanationResult() == null && job.getState() != Job.NONE) {
					if (pm.isCanceled()) {
						job.cancel();
						throw new OperationCanceledException();
					}
					pm.worked(1);
					try {
						job.wait(100);
					} catch (InterruptedException e) {
						if (DEBUG)
							Tracing.debug("Interrupted while computing explanations"); //$NON-NLS-1$
					}
				}
			}
		} finally {
			pm.done();
		}
		return job.getExplanationResult();
	}

}