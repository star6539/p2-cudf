package org.eclipse.equinox.p2.cudf;

public class Log {
	static boolean verbose = false;

	public static void println(String s) {
		if (verbose)
			System.out.println("#" + s);
	}
}
