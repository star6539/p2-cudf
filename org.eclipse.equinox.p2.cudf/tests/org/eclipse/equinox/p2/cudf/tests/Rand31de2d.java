package org.eclipse.equinox.p2.cudf.tests;

import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;

import java.util.Iterator;

import java.io.File;
import java.util.Collection;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.cudf.Parser;
import org.eclipse.equinox.p2.cudf.metadata.*;
import org.eclipse.equinox.p2.cudf.query.*;
import org.eclipse.equinox.p2.cudf.solver.*;

public class Rand31de2d extends TestCase {
	public void testLibdmx1() {
		ProfileChangeRequest pcr =  Parser.parse(new File("/Users/pascal/dev/competition/org.eclipse.equinox.p2.cudf/success/rand31de2d-sol.cudf"));
		pcr.addInstallableUnit(new RequiredCapability("libtext-wrapi18n-perl", VersionRange.emptyRange));
		if (new SimplePlanner().getSolutionFor(pcr) instanceof Collection)
			return;
		String id = "libtext-wrapi18n-perl";
		Version v = new Version(1);
		QueryableArray res = null;
		res = slice(pcr.getInitialState(), id,v);
		restart:
		for (Iterator iterator = res.iterator(); iterator.hasNext();) {
			InstallableUnit iu = (InstallableUnit) iterator.next();
			if (iu.getId() == id)
				continue;
			System.out.println("Trying out: " + iu.getId());
			ProfileChangeRequest pcr2 = new ProfileChangeRequest(res);
			pcr2.addInstallableUnit(new RequiredCapability(iu.getId(), new VersionRange(iu.getVersion())));
			if (!(new SimplePlanner().getSolutionFor(pcr2) instanceof Collection)) {
				System.err.println(iu);
				id = iu.getId();
				v = iu.getVersion();
				res = slice(pcr.getInitialState(), id, v);
				continue restart;
			}
		}
	}
	
	private QueryableArray slice(QueryableArray input, String id, Version version) {
		return new Slicer(input).slice(new InstallableUnit[] { (InstallableUnit) input.query(new CapabilityQuery(new RequiredCapability(id, new VersionRange(version))), new Collector(), null).iterator().next() });
	}
	
	public void testValidateAll() {
		ProfileChangeRequest pcr =  Parser.parse(new File("/Users/pascal/dev/competition/org.eclipse.equinox.p2.cudf/success/rand31de2d-sol.cudf"));
		QueryableArray allIUs = pcr.getInitialState();
		for (Iterator iterator = allIUs.iterator(); iterator.hasNext();) {
			InstallableUnit iu = (InstallableUnit) iterator.next();
			ProfileChangeRequest pcr2 = new ProfileChangeRequest(allIUs);
			pcr2.addInstallableUnit(new RequiredCapability(iu.getId(), new VersionRange(iu.getVersion())));
			if (!(new SimplePlanner().getSolutionFor(pcr2) instanceof Collection))
				System.err.println(iu);
		}
	}
}
