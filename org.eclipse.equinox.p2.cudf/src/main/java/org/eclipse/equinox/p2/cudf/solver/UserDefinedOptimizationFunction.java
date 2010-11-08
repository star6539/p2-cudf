package org.eclipse.equinox.p2.cudf.solver;

import java.math.BigInteger;
import java.util.*;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;

public class UserDefinedOptimizationFunction extends OptimizationFunction {

	private String optfunction;

	public UserDefinedOptimizationFunction(String optfunction) {
		this.optfunction = optfunction;
	}

	public List createOptimizationFunction(InstallableUnit metaIu) {
		List weightedObjects = new ArrayList();
		BigInteger weight = BigInteger.valueOf(slice.size() + 1);
		String[] criteria = optfunction.split(",");
		BigInteger currentWeight = weight.pow(criteria.length - 1);
		for (int i = 0; i < criteria.length; i++) {
			if (criteria[i].endsWith("new")) {
				niou(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
				continue;
			}
			if (criteria[i].endsWith("removed")) {
				removed(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
				continue;
			}
			if (criteria[i].endsWith("notuptodate")) {
				notuptodate(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
				continue;
			}
			if (criteria[i].endsWith("recommended")) {
				optional(weightedObjects, criteria[i].startsWith("-") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
				continue;
			}
			if (criteria[i].endsWith("unmet_recommends")) {
				optional(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
				continue;
			}
			if (criteria[i].endsWith("changed")) {
				changed(weightedObjects, criteria[i].startsWith("+") ? currentWeight.negate() : currentWeight, metaIu);
				currentWeight = currentWeight.divide(weight);
				continue;
			}
			System.out.println("Skipping unknown criteria:" + criteria[i]);
		}
		return weightedObjects;
	}

	public String getName() {
		return "User defined:" + optfunction;
	}

	public void printSolutionValue() {
		int counter;
		List proof = new ArrayList();
		String[] criteria = optfunction.split(",");
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
			if (criteria[i].endsWith("recommended") || criteria[i].endsWith("unmet_recommends")) {
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
			if (criteria[i].endsWith("changed")) {
				proof.clear();
				counter = 0;
				for (int j = 0; j < changeVariables.size(); j++) {
					Object var = changeVariables.get(j);
					if (dependencyHelper.getBooleanValueFor(var)) {
						counter++;
						proof.add(var.toString().substring(18));
					}
				}
				System.out.println("# " + criteria[i] + " criteria value: " + counter);
				System.out.println("# Changed packages: " + proof);
				continue;
			}
		}
	}
}
