/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.cudf;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class Main {
	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.cudf"; //$NON-NLS-1$

	public static void main(String[] args) {
		String filename = null;
		if (args.length > 0)
			filename = args[0];
		else {
			printFail("No input file specified.");
			return;
		}
		File input = new File(filename);
		if (!input.exists()) {
			printFail("Input file does not exist.");
			return;
		}
		printResults(invokeSolver(parseCUDF(input)));
	}

	private static void printResults(Object result) {
		if (result instanceof Collection) {
			printSolution((Collection) result);
			System.exit(0);
		} else if (result instanceof IStatus) {
			IStatus status = (IStatus) result;
			if (!status.isOK())
				printFail("Resulting status not OK: " + status.getMessage());
			System.exit(0);

		}
		printFail("Result not correct type. Expected Collection but was: " + result.getClass().getName());
	}

	private static void printFail(String message) {
		System.out.println("FAIL: " + message);
	}

	private static Object invokeSolver(ProfileChangeRequest request) {
		return new SimplePlanner().getSolutionFor(request);
	}

	private static ProfileChangeRequest parseCUDF(File file) {
		long start = System.currentTimeMillis();
		ProfileChangeRequest result = Parser.parse(file);
		System.out.println("Parsing and creating objects took: " + (System.currentTimeMillis() - start));
		return result;
	}

	private static void printSolution(Collection state) {
		ArrayList l = new ArrayList(state);
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			InstallableUnit iu = (InstallableUnit) iterator.next();
			System.out.println("package: " + iu.getId());
			System.out.println("version: " + iu.getVersion().getMajor());
		}
	}

}
