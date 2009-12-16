package org.eclipse.equinox.p2.cudf.tests;

import java.util.Collection;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.cudf.metadata.*;
import org.eclipse.equinox.p2.cudf.query.QueryableArray;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class TestOptional extends TestCase {
	private QueryableArray dataSet;

	protected void setUp() throws Exception {
		InstallableUnit iu = new InstallableUnit();
		iu.setId("A");
		iu.setVersion(new Version(1, 0, 0));
		iu.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new VersionRange(new Version(1, 0, 0), true, new Version(1, 0, 0), true))});

		InstallableUnit iu2 = new InstallableUnit();
		iu2.setId("A");
		iu2.setVersion(new Version(2, 0, 0));
		iu2.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new VersionRange(new Version(2, 0, 0), true, new Version(2, 0, 0), true))});

		InstallableUnit iu3 = new InstallableUnit();
		iu3.setId("A");
		iu3.setVersion(new Version(3, 0, 0));
		iu3.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new VersionRange(new Version(3, 0, 0), true, new Version(3, 0, 0), true))});
		iu3.setRequiredCapabilities(new IRequiredCapability[] {new RequiredCapability("missing", VersionRange.emptyRange)});

		dataSet = new QueryableArray(new InstallableUnit[] {iu, iu2, iu3});
	}

	public void testFailInstall() {
		ProfileChangeRequest pcr = new ProfileChangeRequest(dataSet);
		pcr.addInstallableUnit(new RequiredCapability("A", new VersionRange("[3.0.0, 3.0.0]")));
		pcr.addInstallableUnit(new RequiredCapability("A", new VersionRange("[2.0.0, 2.0.0]")));
		pcr.addInstallableUnit(new RequiredCapability("A", new VersionRange("[1.0.0, 1.0.0]")));
		Object result = new SimplePlanner().getSolutionFor(pcr);
		assertEquals(false, result instanceof Collection);
	}

	public void testPassInstall() {
		ProfileChangeRequest pcr = new ProfileChangeRequest(dataSet);
		pcr.addInstallableUnit(new RequiredCapability("A", new VersionRange("[3.0.0, 3.0.0]"), true));
		pcr.addInstallableUnit(new RequiredCapability("A", new VersionRange("[2.0.0, 2.0.0]")));
		pcr.addInstallableUnit(new RequiredCapability("A", new VersionRange("[1.0.0, 1.0.0]")));
		Object result = new SimplePlanner().getSolutionFor(pcr);
		assertEquals(true, result instanceof Collection);
		assertEquals(2, ((Collection) result).size());
	}
}
