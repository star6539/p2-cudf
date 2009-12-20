package org.eclipse.equinox.p2.cudf.tests;

import junit.framework.*;

public class AllTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ParserTest.class);
		suite.addTest(CheckAllPassingInstances.suite());
		suite.addTest(CheckAllFailingInstances.suite());
		//		suite.addTestSuite(TestAutomaticProvide.class);
		suite.addTestSuite(TestInstall.class);
		//		suite.addTestSuite(TestInstallUpdateConflict.class);
		//		suite.addTestSuite(TestNegationInDepends.class);
		//		suite.addTestSuite(TestNegationInRequest.class);
		suite.addTestSuite(TestOptional.class);
		suite.addTestSuite(TestRemoval.class);
		return suite;
	}

}
