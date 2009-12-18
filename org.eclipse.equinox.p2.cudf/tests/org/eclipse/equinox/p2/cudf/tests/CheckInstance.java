package org.eclipse.equinox.p2.cudf.tests;

import java.io.*;
import java.util.Collection;
import junit.framework.TestCase;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.eclipse.equinox.p2.cudf.Parser;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class CheckInstance extends TestCase {
	private File inputFile = null;

	public CheckInstance(File nextElement) {
		inputFile = nextElement;
	}

	public String getName() {
		return inputFile.getAbsolutePath();
	}

	protected void runTest() throws Throwable {
		InputStream inputStream = null;
		if (inputFile.getAbsolutePath().endsWith("bz2"))
			inputStream = new CBZip2InputStream(new FileInputStream(inputFile));
		else
			inputStream = new FileInputStream(inputFile);

		ProfileChangeRequest req = Parser.parse(inputStream);
		Object result = new SimplePlanner().getSolutionFor(req);
		if (!(result instanceof Collection))
			fail("Can not resolve: " + inputFile);
	}
}
