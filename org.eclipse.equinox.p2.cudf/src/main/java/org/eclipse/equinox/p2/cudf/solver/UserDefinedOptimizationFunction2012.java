package org.eclipse.equinox.p2.cudf.solver;

import java.math.BigInteger;
import java.util.*;
import org.eclipse.equinox.p2.cudf.Options;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.sat4j.pb.tools.WeightedObject;
import org.sat4j.specs.IVec;

public class UserDefinedOptimizationFunction2012 extends UserDefinedOptimizationFunction {

	private String optfunction;

	public UserDefinedOptimizationFunction2012(String optfunction) {
		super(optfunction);
		this.optfunction = optfunction;
	}

	public List createOptimizationFunction(InstallableUnit metaIu) {
		List weightedObjects = new ArrayList();
		List objects = new ArrayList();
		BigInteger weight = BigInteger.valueOf(slice.size() + 1);
		//String[] criteria = optfunction.split(",");
		String[] criteria = null;
		try {
			criteria = splitCriteria(optfunction);
		} catch (Exception e) {
			e.printStackTrace();
		}
		BigInteger currentWeight = weight.pow(criteria.length - 1);
		int formermaxvarid = dependencyHelper.getSolver().nextFreeVarId(false);
		int newmaxvarid;
		boolean maximizes;
		Object thing;
		for (int i = 0; i < criteria.length; i++) {
			if (criteria[i].endsWith("new")) {
				weightedObjects.clear();
				niou(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
			} else if (criteria[i].endsWith("removed")) {
				weightedObjects.clear();
				removed(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
			} else if (criteria[i].endsWith("notuptodate")) {
				weightedObjects.clear();
				notuptodate(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
			} else if (criteria[i].endsWith("unsat_recommends")) {
				weightedObjects.clear();
				optional(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
			} else if (criteria[i].endsWith("versionchanged")) {
				weightedObjects.clear();
				versionChanged(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
			} else if (criteria[i].endsWith("changed")) {
				weightedObjects.clear();
				changed2012(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
			} else if (criteria[i].endsWith("up")) {
				weightedObjects.clear();
				up(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
			} else if (criteria[i].endsWith("down")) {
				weightedObjects.clear();
				down(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
			} else if (criteria[i].contains("aligned")) {
				weightedObjects.clear();
				StringTokenizer tokenizer = new StringTokenizer(criteria[i].substring(9), ",)");
				String prop1 = tokenizer.nextToken();
				String prop2 = tokenizer.nextToken();
				aligned(weightedObjects, criteria[i].charAt(0) == '-', metaIu, prop1, prop2);
				dependencyHelper.addWeightedCriterion(weightedObjects);
				System.out.println("# criteria " + criteria[i].substring(1) + " size is " + weightedObjects.size());
				continue;
			} else if (criteria[i].contains("sum")) {
				weightedObjects.clear();
				sum(weightedObjects, criteria[i].charAt(0) == '-', metaIu, Options.extractSumProperty(criteria[i]));
				dependencyHelper.addWeightedCriterion(weightedObjects);
				System.out.println("# criteria " + criteria[i].substring(1) + " size is " + weightedObjects.size());
				continue;
			} else {
				System.out.println("Skipping unknown criteria:" + criteria[i]);
			}
			objects.clear();
			maximizes = criteria[i].startsWith("+");
			for (Iterator it = weightedObjects.iterator(); it.hasNext();) {
				thing = ((WeightedObject) it.next()).thing;
				if (maximizes) {
					thing = dependencyHelper.not(thing);
				}
				objects.add(thing);
			}
			dependencyHelper.addCriterion(objects);
			newmaxvarid = dependencyHelper.getSolver().nextFreeVarId(false);
			System.out.println("# criteria " + criteria[i].substring(1) + " size is " + objects.size() + " using new vars " + formermaxvarid + " to " + newmaxvarid);
			formermaxvarid = newmaxvarid;
		}
		weightedObjects.clear();
		return null;
	}

	private String[] splitCriteria(String opt) {
		List<String> res = new ArrayList<String>();
		String crit = "";
		int lookFrom = 0;
		while (crit != null) {
			crit = nextElement(opt, lookFrom);
			if (crit != null) {
				res.add(simplifyCriterion(crit));
				lookFrom += crit.length() + 1;
			}
		}
		String[] resArray = new String[res.size()];
		resArray = res.toArray(resArray);
		return resArray;
	}

	private String simplifyCriterion(String crit) {
		crit = removeCountFunction(crit);
		crit = removeSolutionToken(crit);
		return crit;
	}

	private String removeCountFunction(String crit) {
		String countFunction = "count";
		if (crit.substring(1).startsWith(countFunction + "(")) {
			crit = crit.substring(0, 1) + crit.substring(2 + countFunction.length(), crit.length() - 1);
		}
		return crit;
	}

	private String removeSolutionToken(String crit) {
		String solutionToken = "solution";
		int solutionTokenStart = crit.indexOf(solutionToken);
		if (solutionTokenStart != -1) {
			int solutionTokenEnd = solutionTokenStart + solutionToken.length();
			if (crit.charAt(solutionTokenEnd) == ',') {
				++solutionTokenEnd;
			}
			crit = crit.substring(0, solutionTokenStart) + crit.substring(solutionTokenEnd);
		}
		return crit;
	}

	private String nextElement(String opt, int lookFrom) {
		if (lookFrom >= opt.length()) {
			return null;
		}
		int parCpt = 0;
		for (int i = lookFrom; i < opt.length(); ++i) {
			char curChar = opt.charAt(i);
			if (parCpt == 0 && curChar == ',') {
				return opt.substring(lookFrom, i);
			}
			if (curChar == '(') {
				++parCpt;
			}
			if (curChar == ')') {
				--parCpt;
			}
		}
		return opt.substring(lookFrom, opt.length());
	}

	public String getName() {
		return "User defined:" + optfunction;
	}

	public void printSolutionValue() {
		int counter;
		List proof = new ArrayList();
		//String[] criteria = optfunction.split(",");
		String[] criteria = null;
		try {
			criteria = splitCriteria(optfunction);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < criteria.length; i++) {
			if (criteria[i].endsWith("new")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < newVariables.size(); j++) {
					Object var = newVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString().substring(18));
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Newly installed packages: " + proof);
				continue;
			}
			if (criteria[i].endsWith("removed")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < removalVariables.size(); j++) {
					Object var = removalVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString().substring(18));
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Removed packages: " + proof);
				continue;
			}
			if (criteria[i].endsWith("notuptodate")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < nouptodateVariables.size(); j++) {
					Object var = nouptodateVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString().substring(18));
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Not up-to-date packages: " + proof);
				continue;
			}
			if (criteria[i].endsWith("recommended") || criteria[i].endsWith("unsat_recommends")) {
				proof.clear();
				counter = 0;
				for (Iterator it = unmetVariables.iterator(); it.hasNext();) {
					Object var = it.next();
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString().substring(18));
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Not installed recommended packages: " + proof);
				continue;
			}
			if (criteria[i].endsWith("versionchanged")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < versionChangeVariables.size(); j++) {
					Object var = versionChangeVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString().substring(18));
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Packages with version change: " + proof);
				continue;
			}
			if (criteria[i].endsWith("changed")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < changeVariables.size(); j++) {
					Object var = changeVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString());
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Changed packages: " + proof);
				continue;
			}
			if (criteria[i].endsWith("up")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < upVariables.size(); j++) {
					Object var = upVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString());
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Upgraded packages: " + proof);
				continue;
			}
			if (criteria[i].endsWith("down")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < downVariables.size(); j++) {
					Object var = downVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString());
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Downgraded packages: " + proof);
				continue;
			}
			if (criteria[i].contains("sum")) {
				String sumpProperty = Options.extractSumProperty(criteria[i]);
				long sum = 0;
				IVec sol = dependencyHelper.getSolution();
				for (Iterator it = sol.iterator(); it.hasNext();) {
					Object element = it.next();
					if (element instanceof InstallableUnit) {
						InstallableUnit iu = (InstallableUnit) element;
						sum += iu.getSumProperty();
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + sum);
				continue;
			}

			if (criteria[i].endsWith("aligned")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < secondLvlAlignedVariables.size(); j++) {
					Object var = secondLvlAlignedVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
					}
				}
				for (int j = 0; j < firstLvlAlignedVariables.size(); j++) {
					Object var = firstLvlAlignedVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter--;
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				continue;
			}
		}
	}
}
