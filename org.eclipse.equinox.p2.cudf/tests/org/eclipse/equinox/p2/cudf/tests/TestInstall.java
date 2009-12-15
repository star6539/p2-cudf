package org.eclipse.equinox.p2.cudf.tests;

import junit.framework.TestCase;

import java.util.Collection;
import org.eclipse.equinox.p2.cudf.metadata.*;
import org.eclipse.equinox.p2.cudf.query.QueryableArray;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class TestInstall extends TestCase {
	private QueryableArray dataSet;

	protected void setUp() throws Exception {
		InstallableUnit iu = new InstallableUnit();
		iu.setId("A");
		iu.setVersion(new Version(1,0,0));
		iu.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new Version(1,0,0))} );
		
		InstallableUnit iu2 = new InstallableUnit();
		iu2.setId("A");
		iu2.setVersion(new Version(2,0,0));
		iu2.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new Version(2,0,0))} );
		
		InstallableUnit iu3 = new InstallableUnit();
		iu3.setId("A");
		iu3.setVersion(new Version(3,0,0));
		iu3.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("A", new Version(3,0,0))} );
		
		dataSet = new QueryableArray(new InstallableUnit[] { iu,iu2,iu3 });
	}
	
	public void testRemoveEverything() {
		ProfileChangeRequest pcr = new ProfileChangeRequest(dataSet);
		pcr.addInstallableUnit(new RequiredCapability("A", VersionRange.emptyRange));
		Collection result = (Collection) new SimplePlanner().getSolutionFor(pcr);
		System.out.println(result);
	}
}
