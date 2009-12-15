package org.eclipse.equinox.p2.cudf.tests;

import java.util.Collection;

import junit.framework.TestCase;

import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.cudf.metadata.RequiredCapability;
import org.eclipse.equinox.p2.cudf.metadata.Version;
import org.eclipse.equinox.p2.cudf.metadata.VersionRange;
import org.eclipse.equinox.p2.cudf.query.QueryableArray;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class TestRemoval extends TestCase {
	private QueryableArray dataSet;

	protected void setUp() throws Exception {
		InstallableUnit iu = new InstallableUnit();
		iu.setId("A");
		iu.setVersion(new Version(1,0,0));
		iu.setInstalled(true);
		iu.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new Version(1,0,0))} );
		
		InstallableUnit iu2 = new InstallableUnit();
		iu2.setId("A");
		iu2.setVersion(new Version(2,0,0));
		iu2.setInstalled(true);
		iu2.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new Version(2,0,0))} );
		
		InstallableUnit iu3 = new InstallableUnit();
		iu3.setId("A");
		iu3.setVersion(new Version(3,0,0));
		iu3.setInstalled(true);
		iu3.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new Version(3,0,0))} );
		
		dataSet = new QueryableArray(new InstallableUnit[] { iu,iu2,iu3 });
	}
	
	public void testRemoveEverything() {
		ProfileChangeRequest pcr = new ProfileChangeRequest(dataSet);
		pcr.removeInstallableUnit(new RequiredCapability("A", VersionRange.emptyRange));
		Collection result = (Collection) new SimplePlanner().getSolutionFor(pcr);
		assertEquals(0, result.size());
	}
	
	public void testRemoveOne() {
		ProfileChangeRequest pcr = new ProfileChangeRequest(dataSet);
		pcr.removeInstallableUnit(new RequiredCapability("A", new VersionRange("[3.0.0, 3.0.0]")));
		Collection result = (Collection) new SimplePlanner().getSolutionFor(pcr);
		assertEquals(2, result.size());
	}
	
}
