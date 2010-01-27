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
import java.util.Map.Entry;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.solver.*;

public class Main {
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.cudf"; //$NON-NLS-1$
	private static final String VERBOSE = "-verbose";
	private static final String OBJECTIVE = "-obj";
	private static final String TIMEOUT = "-timeout";
	private static final String SORT = "-sort";
	private static final String EXPLAIN = "-explain";
	private static final String ENCODING = "-encoding";

	protected static transient Thread shutdownHook = new Thread() {
		public void run() {
			if (planner != null) {
				if (options.encoding) {
					out.println(planner.getSolver().toString());
					PrintWriter outMapping = null;
					try {
						String mappingFilename;
						if (options.output == null) {
							mappingFilename = "stdout.mapping";
						} else {
							mappingFilename = options.output + ".mapping";
						}
						outMapping = new PrintWriter(new FileWriter(mappingFilename));
						Map mapping = planner.getMappingToDomain();
						Set entries = mapping.entrySet();
						for (Iterator it = entries.iterator(); it.hasNext();) {
							Map.Entry entry = (Entry) it.next();
							outMapping.println(entry.getKey() + "=" + entry.getValue());
						}
					} catch (IOException e) {
						System.out.println("# cannot write mapping: " + e.getMessage());
					} finally {
						if (outMapping != null) {
							outMapping.close();
						}
					}
				} else {
					planner.stopSolver();
					long end = System.currentTimeMillis();
					Log.println(("Solving done (" + (end - begin) / 1000.0 + "s)."));
					Collection col = planner.getBestSolutionFoundSoFar();
					if (col.isEmpty()) {
						printFail("Cannot find a solution");
						if (options.explain) {
							out.println(planner.getExplanation());
						}
					} else {
						printSolution(col, options);
					}
					if (options.output != null)
						out.close();
				}
			}
		}
	};

	static {
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private static final void usage() {
		System.out.println("Usage: p2cudf [flags] inputFile [outputFile]");
		System.out.println("-obj (paranoid | trendy | p2)     The objective function to be used to resolve the problem. p2 is used by default.");
		System.out.println("-timeout <number>(c|s)            The time out after which the solver will stop. e.g. 10s stops after 10 seconds, 10c stops after 10 conflicts. Default is set to 200c for p2 and 2000c for other objective functions.");
		System.out.println("-sort                             Sort the output.");
		System.out.println("-explain                          Provides one reason of the unability to fullfil the request");
		System.out.println("-verbose                          Display details on the platform, internal SAT solver and steps reached");
		System.out.println("-encoding                         Output the original cudf request into an OPB problem");
	}

	private static PrintStream out = System.out;
	private static SimplePlanner planner;
	private static Options options;
	private static long begin;

	public static Options processArguments(String[] args) {
		Options result = new Options();
		if (args == null)
			return result;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase(VERBOSE)) {
				result.verbose = true;
				continue;
			}

			if (args[i].equalsIgnoreCase(ENCODING)) {
				result.encoding = true;
				continue;
			}
			if (args[i].equalsIgnoreCase(EXPLAIN)) {
				result.explain = true;
				continue;
			}

			if (args[i].equalsIgnoreCase(OBJECTIVE)) {
				if (args[i + 1].startsWith("-")) {
					printFail("-obj should be followed by the name of the objective function.");
					System.exit(1);
				}
				result.objective = args[++i];
				continue;
			}

			if (args[i].equalsIgnoreCase(TIMEOUT)) {
				if (args[i + 1].startsWith("-")) {
					printFail("-" + TIMEOUT + " should be followed by a time in seconds or a number of conflicts.");
					System.exit(1);
				}
				result.timeout = args[++i];
				continue;
			}

			if (args[i].equalsIgnoreCase(SORT)) {
				result.sort = true;
				continue;
			}
			if (result.input == null)
				result.input = new File(args[i]);
			else
				result.output = new File(args[i]);
		}
		return result;
	}

	private static boolean validateOptions(Options options) {
		boolean error = false;
		if (!"paranoid".equalsIgnoreCase(options.objective) && !"trendy".equalsIgnoreCase(options.objective) && !"p2".equalsIgnoreCase(options.objective)) {
			printFail("Wrong Optimization criteria: " + options.objective);
			error = true;
		}
		if (options.input == null || !options.input.exists()) {
			printFail("Missing input file.");
			error = true;
		}
		if (options.timeout != null && !options.timeout.equals("default") && !options.timeout.endsWith("c") && !options.timeout.endsWith("s")) {
			printFail("Timeout should be either <number>s (100s) or <number>c (100c)");
			error = true;
		}
		return error;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			return;
		}
		options = processArguments(args);
		if (validateOptions(options)) {
			System.exit(1);
		}
		Log.verbose = options.verbose;
		logOptions(options);
		logVmDetails();

		if (options.output != null) {
			try {
				out = new PrintStream(new FileOutputStream(options.output));
			} catch (FileNotFoundException e) {
				printFail("Output file does not exist.");
				return;
			}
		}
		boolean result = printResults(invokeSolver(parseCUDF(options.input), new SolverConfiguration(options.objective, options.timeout, options.verbose, options.explain, options.encoding)), options);
		System.exit(result ? 0 : 1);
	}

	private static void logOptions(Options options) {
		if (!options.verbose)
			return;
		Log.println("Using input file " + options.input.getAbsolutePath());
		Log.println("Using ouput file " + (options.output == null ? "STDOUT" : options.output.getAbsolutePath()));
		Log.println("Objective function " + options.objective);
		Log.println("Timeout " + options.timeout);
	}

	private static void logVmDetails() {
		Properties prop = System.getProperties();
		String[] infoskeys = {"java.runtime.name", "java.vm.name", "java.vm.version", "java.vm.vendor", "sun.arch.data.model", "java.version", "os.name", "os.version", "os.arch"}; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
		for (int i = 0; i < infoskeys.length; i++) {
			String key = infoskeys[i];
			Log.println((key + ((key.length() < 14) ? "\t\t" : "\t") + prop.getProperty(key))); //$NON-NLS-1$
		}
		Runtime runtime = Runtime.getRuntime();
		Log.println(("Free memory \t\t" + runtime.freeMemory())); //$NON-NLS-1$
		Log.println(("Max memory \t\t" + runtime.maxMemory())); //$NON-NLS-1$
		Log.println(("Total memory \t\t" + runtime.totalMemory())); //$NON-NLS-1$
		Log.println(("Number of processors \t" + runtime.availableProcessors())); //$NON-NLS-1$
	}

	private static boolean printResults(Object result, Options options) {
		if (result instanceof Collection) {
			// printSolution((Collection) result, options);
			return true;
			//		} else if (result instanceof IStatus) {
			//			IStatus status = (IStatus) result;
			//			if (!status.isOK()) {
			//				printFail("Resulting status not OK: " + status.getMessage());
			//				return false;
			//			}
		}
		//		printFail("Result not correct type. Expected Collection but was: " + result.getClass().getName());
		return false;
	}

	private static void printFail(String message) {
		out.println("FAIL");
		out.println(message);
	}

	private static Object invokeSolver(ProfileChangeRequest request, SolverConfiguration configuration) {
		Log.println("Solving ...");
		begin = System.currentTimeMillis();
		planner = new SimplePlanner();
		Object result = planner.getSolutionFor(request, configuration);
		return result;
	}

	private static ProfileChangeRequest parseCUDF(File file) {
		Log.println("Parsing ...");
		long begin = System.currentTimeMillis();
		ProfileChangeRequest result = new Parser().parse(file);
		long end = System.currentTimeMillis();
		Log.println(("Parsing done (" + (end - begin) / 1000.0 + "s)."));
		return result;
	}

	private static void printSolution(Collection state, Options options) {
		if (options.sort) {
			ArrayList tmp = new ArrayList(state);
			Collections.sort(tmp);
			state = tmp;
		}
		Log.println(("Solution contains:" + state.size()));
		for (Iterator iterator = state.iterator(); iterator.hasNext();) {
			InstallableUnit iu = (InstallableUnit) iterator.next();
			out.println("package: " + iu.getId());
			out.println("version: " + iu.getVersion().getMajor());
			out.println("installed: true");
			out.println();
		}
	}

}
