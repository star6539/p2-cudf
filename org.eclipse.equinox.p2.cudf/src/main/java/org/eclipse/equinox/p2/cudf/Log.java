package org.eclipse.equinox.p2.cudf;

public class Log {
	static boolean verbose = true;

	public static void println(String s) {
		if (verbose)
			System.out.println("#" + s);
	}
}
