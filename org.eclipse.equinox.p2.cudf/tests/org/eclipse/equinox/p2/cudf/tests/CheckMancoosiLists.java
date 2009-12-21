package org.eclipse.equinox.p2.cudf.tests;

import java.io.File;
import java.io.FilenameFilter;
import junit.framework.*;

public class CheckMancoosiLists extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(CheckMancoosiLists.class.getName());
		File resourceDirectory = new File("/Users/pascal/Downloads/mancoosi.org/~abate/cudfproblems/rand.biglist/");
		File[] resources = resourceDirectory.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				if (name.endsWith(".cudf"))
					return true;
				return false;
			}
		});
		for (int i = 0; i < resources.length; i++) {
			suite.addTest(new CheckInstance(resources[i], resources[i].getName().endsWith("-sol.cudf") ? true : hasSuccessFile(resources[i])));
		}
		return suite;
	}

	private static boolean hasSuccessFile(final File file) {
		return file.getParentFile().list(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				if (name.startsWith(file.getName()) && name.endsWith("success")) {
					return true;
				}
				return false;
			}
		}).length != 0;
	}
}
