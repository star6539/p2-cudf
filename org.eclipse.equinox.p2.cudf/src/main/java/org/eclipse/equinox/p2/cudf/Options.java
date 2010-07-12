package org.eclipse.equinox.p2.cudf;

import java.io.File;

public class Options {
	public static final String P2 = "p2";
	public static final String PARANOID = "paranoid";
	public static final String TRENDY = "trendy";

	boolean verbose = false;
	String objective = P2;
	String timeout = "default";
	boolean explain = false;
	public File input;
	public File output;
	public boolean sort = false;
	boolean encoding = false;
}
