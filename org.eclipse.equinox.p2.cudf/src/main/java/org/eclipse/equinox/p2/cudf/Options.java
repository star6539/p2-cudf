package org.eclipse.equinox.p2.cudf;

import java.io.File;

public class Options {
	public static final String PARANOID = "-removed,-changed";
	public static final String TRENDY = "-removed,-notuptodate,-unmet_recommends,-new";

	boolean verbose = false;
	String objective = PARANOID;
	String timeout = "default";
	boolean explain = false;
	public File input;
	public File output;
	public boolean sort = false;
	boolean encoding = false;
}
