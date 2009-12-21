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

//	TRENDY:   we want to answer the user request, minimizing the number
//    of packages removed in the solution, maximizing the number
//    of packages at their most recent version in the solution, and
//    minimizing the number of extra packages installed;
//    the optimization criterion is
//
//         lex( min #removed, max #uptodate, min #new)
//
//    Hence, two solutions S1 and S2 will be compared as follows:
//
//    i) compute ri = #removed(U,Si), ui = #uptodate(U,Si), ni = #new(U,Si)
//
//    ii) S1 is better than S2 iff
//        r1 < r2 or (r1=r2 and (u1>u2 or (u1=u2 and n1<n2)))

public class TrendyOptimizationFunction extends OptimizationFunction {

	public List createOptimizationFunction(InstallableUnit metaIu) {
		List weightedObjects = new ArrayList();
		Collection ius = slice.values();
		int weight = slice.size() + 1;
		for (Iterator it = ius.iterator(); it.hasNext();) {
			InstallableUnit iu = (InstallableUnit) it.next();
			if (iu == metaIu)
				continue;

		}
		removed(weightedObjects, weight * weight);
		uptodate(weightedObjects, -weight);
		niou(weightedObjects, 1);
		if (!weightedObjects.isEmpty()) {
			return weightedObjects;
		}
		return null;
	}

	public String getName() {
		return "misc 2010, trendy";
	}
}
