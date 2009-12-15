package org.eclipse.equinox.p2.cudf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class Main {
	public static void main(String[] args) {
		String filename = null;
		if (args.length > 0)
			filename = args[0];
		printResults(invokeSolver(parseCUDF(new File(filename))));
	}

	private static void printResults(Object result) {
		if(result instanceof Collection) {
			printSolution((Collection) result);
			System.exit(0);
		} 
		printFail();
		
	}

	private static void printFail() {
		System.out.println("FAIL");
	}

	private static Object invokeSolver(ProfileChangeRequest request) {
		return new SimplePlanner().getSolutionFor(request, new NullProgressMonitor());
	}

	private static ProfileChangeRequest parseCUDF(File file) {
		return Parser.parse(file);
	}
	
	private static void printSolution(Collection state) {
		ArrayList l = new ArrayList(state);
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			InstallableUnit iu = (InstallableUnit) iterator.next();
			System.out.println("package: " + iu.getId());
			System.out.println("version: " + iu.getVersion().getMajor());
		}
	}

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.cudf"; //$NON-NLS-1$
}
