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

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class Main {
	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.cudf"; //$NON-NLS-1$

	private static final void usage() {
		System.out.println("Usage: p2cudf <cudfin> [<cudfout> [(paranoid | trendy)]] ");
	}

	private static final void log(String str) {
		System.out.println("# " + str);
	}

	private static PrintStream out = System.out;

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			return;
		}
		String cudfin = args[0];
		File input = new File(cudfin);
		if (!input.exists()) {
			printFail("Input file does not exist.");
			return;
		}
		log("Using input file " + cudfin);
		if (args.length > 1) {
			String cudfout = args[1];
			File output = new File(cudfout);
			try {
				out = new PrintStream(new FileOutputStream(output));
			} catch (FileNotFoundException e) {
				printFail("Output file does not exist.");
				return;
			}
			log("Using output file " + cudfout);
		} else {
			log("Using standard output");
		}

		String criteria = "paranoid";
		if (args.length == 3) {
			if (!"paranoid".equalsIgnoreCase(args[2]) && !"trendy".equalsIgnoreCase(args[2]) && !"p2".equalsIgnoreCase(args[2])) {
				printFail("Wrong Optimization criteria: " + args[2]);
				return;
			}
			criteria = args[2].toLowerCase();
		}
		log("Using criteria " + criteria);
		printResults(invokeSolver(parseCUDF(input), criteria));
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
		out.println("FAIL");
		out.println(message);
	}

	private static Object invokeSolver(ProfileChangeRequest request, String criteria) {
		log("Solving ...");
		long begin = System.currentTimeMillis();
		Object result = new SimplePlanner().getSolutionFor(request, criteria);
		long end = System.currentTimeMillis();
		log("Solving done (" + (end - begin) / 1000.0 + "s).");
		return result;
	}

	private static ProfileChangeRequest parseCUDF(File file) {
		log("Parsing ...");
		long begin = System.currentTimeMillis();
		ProfileChangeRequest result = new Parser().parse(file);
		long end = System.currentTimeMillis();
		log("Parsing done (" + (end - begin) / 1000.0 + "s).");
		return result;
	}

	private static void printSolution(Collection state) {
		ArrayList l = new ArrayList(state);
		log("Solution contains:" + l.size());
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			InstallableUnit iu = (InstallableUnit) iterator.next();
			out.println("package: " + iu.getId());
			out.println("version: " + iu.getVersion().getMajor());
			out.println("installed: true");
			out.println();
		}
	}

}
