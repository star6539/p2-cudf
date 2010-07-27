/*******************************************************************************
 * Copyright (c) 2009 Daniel Le Berre and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Daniel Le Berre - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.cudf.solver;

import java.math.BigInteger;
import java.util.*;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.query.QueryableArray;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.pb.tools.WeightedObject;
import org.sat4j.specs.ContradictionException;

public abstract class OptimizationFunction {
	protected Map slice;
	protected Map noopVariables;
	protected QueryableArray picker;
	protected DependencyHelper dependencyHelper;
	protected List removalVariables = new ArrayList();
	protected List changeVariables = new ArrayList();
	protected List nouptodateVariables = new ArrayList();
	protected List newVariables = new ArrayList();
	protected List optionalityVariables;

	public abstract List createOptimizationFunction(InstallableUnit metaIu);

	public abstract void printSolutionValue();

	protected void removed(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			boolean installed = false;
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iuv = (InstallableUnit) iterator2.next();
				installed = installed || iuv.isInstalled();
			}
			if (installed) {
				try {
					Projector.AbstractVariable abs = new Projector.AbstractVariable(entry.getKey().toString());
					removalVariables.add(abs);
					// a => not iuv1 and ... and  not iuvn
					for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
						dependencyHelper.implication(new Object[] {abs}).impliesNot(iterator2.next()).named("OPT1");
					}
					// a <= not iuv1 and ... and  not iuvn
					Object[] clause = new Object[versions.size() + 1];
					versions.toArray(clause);
					clause[clause.length - 1] = abs;
					dependencyHelper.clause("OPT1", clause);
					weightedObjects.add(WeightedObject.newWO(abs, weight));
				} catch (ContradictionException e) {
					// should not happen
					e.printStackTrace();
				}
			}

		}
	}

	protected void changed(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			List changed = new ArrayList(versions.size());
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iu = (InstallableUnit) iterator2.next();
				if (iu.isInstalled()) {
					changed.add(dependencyHelper.not(iu));
				} else {
					changed.add(iu);
				}
			}
			try {
				Projector.AbstractVariable abs = new Projector.AbstractVariable(entry.getKey().toString());
				changeVariables.add(abs);
				// a <= iuv1 or not iuv2 or ... or  not iuvn
				for (Iterator iterator2 = changed.iterator(); iterator2.hasNext();) {
					dependencyHelper.implication(new Object[] {iterator2.next()}).implies(abs).named("OPT3");
				}
				// a => iuv1 or not iuv2 or ... or  not iuvn
				Object[] clause = new Object[changed.size()];
				changed.toArray(clause);
				dependencyHelper.implication(new Object[] {abs}).implies(clause).named("OPT3");
				weightedObjects.add(WeightedObject.newWO(abs, weight));
			} catch (ContradictionException e) {
				// should not happen
				e.printStackTrace();
			}
		}
	}

	protected void uptodate(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			HashMap versions = (HashMap) entry.getValue();
			List toSort = new ArrayList(versions.values());
			Collections.sort(toSort, Collections.reverseOrder());
			weightedObjects.add(WeightedObject.newWO(toSort.get(0), weight));
		}
	}

	protected void notuptodate(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			HashMap versions = (HashMap) entry.getValue();
			List toSort = new ArrayList(versions.values());
			Collections.sort(toSort, Collections.reverseOrder());
			Projector.AbstractVariable abs = new Projector.AbstractVariable(entry.getKey().toString());
			Object notlatest = dependencyHelper.not(toSort.get(0));
			try {
				dependencyHelper.implication(new Object[] {abs}).implies(notlatest).named("OPT4");
				Object[] clause = new Object[toSort.size()];
				toSort.toArray(clause);
				clause[0] = dependencyHelper.not(abs);
				dependencyHelper.clause("OPT4", clause);
				for (int i = 1; i < toSort.size(); i++) {
					dependencyHelper.implication(new Object[] {notlatest, toSort.get(i)}).implies(abs).named("OPT4");
				}
			} catch (ContradictionException e) {
				// should never happen
				e.printStackTrace();
			}

			weightedObjects.add(WeightedObject.newWO(abs, weight));
			nouptodateVariables.add(abs);
		}
	}

	protected void niou(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			boolean installed = false;
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iuv = (InstallableUnit) iterator2.next();
				installed = installed || iuv.isInstalled();
			}
			if (!installed) {
				try {
					Projector.AbstractVariable abs = new Projector.AbstractVariable(entry.getKey().toString());
					newVariables.add(abs);
					// a <= iuv1 or ... or iuvn
					for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
						dependencyHelper.implication(new Object[] {iterator2.next()}).implies(abs).named("OPT2");
					}
					// a => iuv1 or ... or iuvn
					Object[] clause = new Object[versions.size()];
					versions.toArray(clause);
					dependencyHelper.implication(new Object[] {abs}).implies(clause).named("OPT2");
					weightedObjects.add(WeightedObject.newWO(abs, weight));
				} catch (ContradictionException e) {
					// should not happen
					e.printStackTrace();
				}
			}

		}
	}

	protected void optional(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		for (Iterator it = optionalityVariables.iterator(); it.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(it.next(), weight));
		}
	}

	public abstract String getName();

}
