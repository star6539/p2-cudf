package org.eclipse.equinox.p2.cudf.tests;

import java.io.File;
import junit.framework.*;

public class CheckAllPassingInstances extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(CheckAllPassingInstances.class.getName());
		File resourceDirectory = new File(CheckAllPassingInstances.class.getClassLoader().getResource("testData/instances/expectedSuccess/").toString().substring("file:".length()));
		File[] resources = resourceDirectory.listFiles();
		for (int i = 0; i < resources.length; i++) {
			suite.addTest(new CheckInstance(resources[i]));
		}
		return suite;
	}

	protected void tearDown() throws Exception {
		System.gc();
	}
}
