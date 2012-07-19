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
import org.sat4j.core.Vec;
import org.sat4j.pb.tools.LexicoHelper;
import org.sat4j.pb.tools.WeightedObject;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;

public abstract class OptimizationFunction {
	protected Map slice;
	protected Map noopVariables;
	protected QueryableArray picker;
	protected LexicoHelper dependencyHelper;
	protected List removalVariables = new ArrayList();
	protected List changeVariables = new ArrayList();
	protected List versionChangeVariables = new ArrayList();
	protected List nouptodateVariables = new ArrayList();
	protected List newVariables = new ArrayList();
	protected List unmetVariables = new ArrayList();
	protected List upVariables = new ArrayList();
	protected List downVariables = new ArrayList();
	protected List firstLvlAlignedVariables = new ArrayList();
	protected List secondLvlAlignedVariables = new ArrayList();
	protected List optionalityVariables;
	protected List optionalityPairs;

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
			Object[] literals = new Object[versions.size()];
			int i = 0;
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iuv = (InstallableUnit) iterator2.next();
				installed = installed || iuv.isInstalled();
				literals[i++] = dependencyHelper.not(iuv);
			}
			if (installed) {
				try {
					Projector.AbstractVariable abs = new Projector.AbstractVariable(entry.getKey().toString());
					removalVariables.add(abs);
					// abs <=> not iuv1 and ... and  not iuvn
					dependencyHelper.and("OPT1", abs, literals);
					weightedObjects.add(WeightedObject.newWO(abs, weight));
				} catch (ContradictionException e) {
					// should not happen
					e.printStackTrace();
				}
			}

		}
	}

	protected void versionChanged(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			boolean installed = false;
			IVec<InstallableUnit> changed = new Vec<InstallableUnit>(versions.size());
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iu = (InstallableUnit) iterator2.next();
				installed = installed || iu.isInstalled();
				if (!iu.isInstalled()) {
					changed.push(iu);
				}
			}
			if (installed) {
				Object[] changedarray = new Object[changed.size()];
				changed.copyTo(changedarray);
				try {
					Projector.AbstractVariable abs = new Projector.AbstractVariable(entry.getKey().toString());
					versionChangeVariables.add(abs);
					// abs <=> iuv1 or not iuv2 or ... or  not iuvn
					dependencyHelper.or("OPT3", abs, changedarray);
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
			Object[] changed = new Object[versions.size()];
			int i = 0;
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iu = (InstallableUnit) iterator2.next();

				changed[i++] = iu.isInstalled() ? dependencyHelper.not(iu) : iu;
			}
			try {
				Projector.AbstractVariable abs = new Projector.AbstractVariable(entry.getKey().toString());
				changeVariables.add(abs);
				// abs <=> iuv1 or not iuv2 or ... or  not iuvn
				dependencyHelper.or("OPT3", abs, changed);
				weightedObjects.add(WeightedObject.newWO(abs, weight));
			} catch (ContradictionException e) {
				// should not happen
				e.printStackTrace();
			}
		}
	}

	protected void changed2012(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			Object[] changed = new Object[versions.size()];
			int i = 0;
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iu = (InstallableUnit) iterator2.next();
				changed[i++] = iu.isInstalled() ? dependencyHelper.not(iu) : iu;
			}
			for (Object obj : changed) {
				changeVariables.add(obj);
				weightedObjects.add(WeightedObject.newWO(obj, weight));
			}
		}
	}

	protected void up(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			List changed = null;
			int i = 0;
			boolean pkgInstalled = false;
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iu = (InstallableUnit) iterator2.next();
				if (iu.isInstalled()) {
					changed = new ArrayList();
					pkgInstalled = true;
				} else {
					if (pkgInstalled) {
						changed.add(iu);
					}
				}
			}
			if (changed != null) {
				for (Object obj : changed) {
					upVariables.add(obj);
					weightedObjects.add(WeightedObject.newWO(obj, weight));
				}
			}
		}
	}

	protected void down(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			List changed = new ArrayList();
			int i = 0;
			boolean pkgInstalled = false;
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iu = (InstallableUnit) iterator2.next();
				if (iu.isInstalled()) {
					pkgInstalled = true;
					break;
				} else {
					changed.add(iu);
				}
			}
			if (pkgInstalled) {
				for (Object obj : changed) {
					downVariables.add(obj);
					weightedObjects.add(WeightedObject.newWO(obj, weight));
				}
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
				// notuptodate <=> not iuvn and (iuv1 or iuv2 or ... iuvn-1) 
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

	protected void unmetRecommends(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		for (Iterator iterator = optionalityPairs.iterator(); iterator.hasNext();) {
			Pair entry = (Pair) iterator.next();
			if (entry.left == metaIu) {
				// weightedObjects.add(WeightedObject.newWO(entry.right, weight));
				continue;
			}

			Projector.AbstractVariable abs = new Projector.AbstractVariable(entry.left.toString() + entry.right);
			try {
				dependencyHelper.and("OPTX", abs, new Object[] {entry.right, entry.left});
			} catch (ContradictionException e) {
				// should never happen
				e.printStackTrace();
			}
			weightedObjects.add(WeightedObject.newWO(abs, weight));
			unmetVariables.add(abs);
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
					// a <=> iuv1 or ... or iuvn
					Object[] clause = new Object[versions.size()];
					versions.toArray(clause);
					dependencyHelper.or("OPT2", abs, clause);
					weightedObjects.add(WeightedObject.newWO(abs, weight));
				} catch (ContradictionException e) {
					// should not happen
					e.printStackTrace();
				}
			}

		}
	}

	protected void optional(List weightedObjects, BigInteger weight, InstallableUnit metaIu) {
		for (Iterator it = optionalityPairs.iterator(); it.hasNext();) {
			Pair pair = (Pair) it.next();
			if (pair.left != metaIu) {
				weightedObjects.add(WeightedObject.newWO(pair.right, weight));
				unmetVariables.add(pair.right);
			}
		}
	}

	protected void sum(List weightedObjects, boolean minimize, InstallableUnit metaIu, String sumProperty) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iuv = (InstallableUnit) iterator2.next();
				if (iuv.getSumProperty() != 0) {
					BigInteger weight = BigInteger.valueOf(iuv.getSumProperty());
					weightedObjects.add(WeightedObject.newWO(iuv, minimize ? weight : weight.negate()));
				}
			}
		}
	}

	protected void aligned(List weightedObjects, boolean minimize, InstallableUnit metaIu, String prop1, String prop2) {
		AlignedMeasurementHelper helper = new AlignedMeasurementHelper(prop1, prop2);
		fillHelper(metaIu, helper);
		processOnSecondLvlClusters(weightedObjects, minimize, helper);
		processOnFirstLvlClusters(weightedObjects, minimize, helper);
	}

	private void processOnFirstLvlClusters(List weightedObjects, boolean minimize, AlignedMeasurementHelper helper) {
		Iterator<List<InstallableUnit>> firstLvlClusterIt = helper.firstLvlClustersIterator();
		int cpt = 0;
		BigInteger weight = (!minimize) ? (BigInteger.ONE) : (BigInteger.valueOf(-1));
		while (firstLvlClusterIt.hasNext()) {
			List<InstallableUnit> ius = firstLvlClusterIt.next();
			if (ius.size() > 1) {
				try {
					Projector.AbstractVariable abs = new Projector.AbstractVariable("aligned_lvl1_" + cpt);
					firstLvlAlignedVariables.add(abs);
					dependencyHelper.or("align_lvl1", abs, ius.toArray());
					weightedObjects.add(WeightedObject.newWO(abs, weight));
				} catch (ContradictionException e) {
					// should never happen
					e.printStackTrace();
				}
				++cpt;
			} else {
				InstallableUnit iu = ius.get(0);
				firstLvlAlignedVariables.add(iu);
				weightedObjects.add(WeightedObject.newWO(iu, weight));
			}
		}
	}

	private void processOnSecondLvlClusters(List weightedObjects, boolean minimize, AlignedMeasurementHelper helper) {
		Iterator<List<InstallableUnit>> secondLvlClusterIt = helper.secondLvlClustersIterator();
		int cpt = 0;
		BigInteger weight = (minimize) ? (BigInteger.ONE) : (BigInteger.valueOf(-1));
		while (secondLvlClusterIt.hasNext()) {
			List<InstallableUnit> ius = secondLvlClusterIt.next();
			if (ius.size() > 1) {
				try {
					Projector.AbstractVariable abs = new Projector.AbstractVariable("aligned_lvl2_" + cpt);
					secondLvlAlignedVariables.add(abs);
					dependencyHelper.or("align_lvl2", abs, ius.toArray());
					weightedObjects.add(WeightedObject.newWO(abs, weight));
				} catch (ContradictionException e) {
					// should never happen
					e.printStackTrace();
				}
				++cpt;
			} else {
				InstallableUnit iu = ius.get(0);
				secondLvlAlignedVariables.add(iu);
				weightedObjects.add(WeightedObject.newWO(iu, weight));
			}

		}
	}

	private void fillHelper(InstallableUnit metaIu, AlignedMeasurementHelper helper) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (entry.getKey() == metaIu.getId())
				continue;
			Collection versions = ((HashMap) entry.getValue()).values();
			for (Iterator iterator2 = versions.iterator(); iterator2.hasNext();) {
				InstallableUnit iuv = (InstallableUnit) iterator2.next();
				helper.addIU(iuv);
			}
		}
	}

	public abstract String getName();

}
