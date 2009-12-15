/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.cudf.solver;

import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;


public class PlannerHelper {
	public static String createOptionalInclusionRule(InstallableUnit iu) {
		return "OPTIONAL"; //$NON-NLS-1$
	}

	public static String createStrictInclusionRule(InstallableUnit iu) {
		return "STRICT"; //$NON-NLS-1$
	}
}
