/*******************************************************************************
 * Copyright (c) 2009 Daniel Le Berre and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Daniel Le Berre - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.cudf.solver;

import java.util.*;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.query.QueryableArray;
import org.sat4j.pb.tools.WeightedObject;

public abstract class OptimizationFunction {
	protected Map slice;
	protected Map noopVariables;
	protected List abstractVariables;
	protected QueryableArray picker;

	public abstract List createOptimizationFunction(InstallableUnit metaIu);

	protected void removed(List weightedObjects, InstallableUnit iu, int weight) {
		if (iu.isInstalled()) {
			// IU was installed in CUDF, got a penalty if removed from the solution.
			weightedObjects.add(WeightedObject.newWO(iu, -weight));
		}
	}

	protected void changed(List weightedObjects, InstallableUnit iu, int weight) {
		if (iu.isInstalled()) {
			// IU was installed in CUDF, got a penalty if removed from the solution.
			weightedObjects.add(WeightedObject.newWO(iu, -weight));
		} else {
			// IU was not installed in CUDF, got a penalty if added to the solution.
			weightedObjects.add(WeightedObject.newWO(iu, weight));
		}
	}

	protected void uptodate(List weightedObjects, int weight) {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap versions = (HashMap) entry.getValue();
			List toSort = new ArrayList(versions.values());
			Collections.sort(toSort, Collections.reverseOrder());
			weightedObjects.add(WeightedObject.newWO(toSort.get(0), weight));
		}
	}

	protected void niou(List weightedObjects, InstallableUnit iu, int weight) {
		if (!iu.isInstalled()) {
			// IU was not installed in CUDF, got a penalty if added to the solution.
			weightedObjects.add(WeightedObject.newWO(iu, weight));
		}
	}

	public abstract String getName();
}
