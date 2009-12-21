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

//	PARANOID: we want to answer the user request, minimizing the number
//    of packages removed in the solution, and also the packages
//    changed by the solution; the optimization criterion is
//
//         lex( min #removed, min #changed)
//
//    Hence, two solutions S1 and S2 will be compared as follows:
//
//    i) compute ri = #removed(U,Si), ci = #changed(U,Si)
//
//    ii) S1 is better than S2 iff r1 < r2 or (r1=r2 and c1<c2)
public class ParanoidOptimizationFunction extends OptimizationFunction {

	public List createOptimizationFunction(InstallableUnit metaIu) {
		List weightedObjects = new ArrayList();
		Collection ius = slice.values();
		int weight = slice.size() + 1;
		for (Iterator it = ius.iterator(); it.hasNext();) {
			InstallableUnit iu = (InstallableUnit) it.next();
			if (iu == metaIu)
				continue;
			removed(weightedObjects, iu, weight);
			changed(weightedObjects, iu, 1);
		}
		if (!weightedObjects.isEmpty()) {
			return weightedObjects;
		}
		return null;
	}

	public String getName() {
		return "misc 2010 paranoid";
	}
}
