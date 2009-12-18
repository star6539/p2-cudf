package org.eclipse.equinox.p2.cudf.tests;

import java.io.*;
import java.util.Collection;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.cudf.Parser;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class CheckInstance extends TestCase {
	private File inputFile = null;
	private boolean successExpected = true;

	public CheckInstance(File nextElement, boolean expected) {
		inputFile = nextElement;
		successExpected = expected;
	}

	public String getName() {
		return inputFile.getAbsolutePath();
	}

	protected void runTest() throws Throwable {
		InputStream inputStream = null;
		if (inputFile.getAbsolutePath().endsWith("bz2"))
			return;
		//			inputStream = new CBZip2InputStream(new FileInputStream(inputFile));
		else
			inputStream = new FileInputStream(inputFile);

		ProfileChangeRequest req = new Parser().parse(inputStream);

		Object result = new SimplePlanner().getSolutionFor(req);
		if (successExpected) {
			if (!(result instanceof Collection))
				fail("Can not resolve: " + inputFile);
			if (req.getExpected() != -10)
				assertEquals(req.getExpected(), ((Collection) result).size());
		} else {
			if (result instanceof Collection)
				fail("No solution was expected: " + inputFile);
		}

	}

	protected void tearDown() throws Exception {
		System.gc();
	}

}
