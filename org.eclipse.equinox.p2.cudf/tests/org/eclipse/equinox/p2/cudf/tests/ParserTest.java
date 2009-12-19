package org.eclipse.equinox.p2.cudf.tests;

import java.util.Iterator;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.cudf.Parser;
import org.eclipse.equinox.p2.cudf.metadata.*;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;

public class ParserTest extends TestCase {
	private ProfileChangeRequest pcr = null;

	protected void setUp() throws Exception {
		pcr = new Parser().parse(this.getClass().getClassLoader().getResource("testData/parsingTest.cudf").openStream());
	}

	private InstallableUnit getIU(String id) {
		Iterator it = pcr.getInitialState().iterator();
		while (it.hasNext()) {
			InstallableUnit iu = (InstallableUnit) it.next();
			if (id.equals(iu.getId()))
				return iu;
		}
		fail("Can't find: " + id);
		return null;
	}

	public void testCheckPackageA() {
		InstallableUnit iu = getIU("a");
		assertRequirement(new RequiredCapability("b", new VersionRange(new Version(2), true, Version.maxVersion, true)), iu.getRequiredCapabilities());
		assertRequirement(new RequiredCapability("c", VersionRange.emptyRange), iu.getRequiredCapabilities());
		assertRequirement(new NotRequirement(new RequiredCapability("d", new VersionRange(new Version(2)))), iu.getRequiredCapabilities());
		assertRequirement(new RequiredCapability("e", VersionRange.emptyRange), iu.getRequiredCapabilities());
		assertRequirement(new NotRequirement(new RequiredCapability("f", VersionRange.emptyRange)), iu.getRequiredCapabilities());
	}

	public void testCheckPackageB() {
		InstallableUnit iu = getIU("b");
		assertRequirement(new RequiredCapability("a", new VersionRange(new Version(5), true, Version.maxVersion, true)), iu.getRequiredCapabilities());
		IRequiredCapability[] reqs = iu.getRequiredCapabilities();
		for (int i = 0; i < reqs.length; i++) {
			if (reqs[i] instanceof ORRequirement) {
				assertRequirement(new RequiredCapability("c", VersionRange.emptyRange), ((ORRequirement) reqs[i]).getRequirements());
				assertRequirement(new RequiredCapability("d", new VersionRange(new Version(2), false, Version.maxVersion, true)), ((ORRequirement) reqs[i]).getRequirements());
			}
		}
		assertRequirement(new RequiredCapability("f", new VersionRange(new Version(5), true, Version.maxVersion, true)), iu.getRequiredCapabilities());
		assertRequirement(new NotRequirement(new RequiredCapability("g", VersionRange.emptyRange)), iu.getRequiredCapabilities());
	}

	public void testCheckPackageLibcbin() {
		InstallableUnit iu = getIU("libc-bin");
		assertEquals(true, iu.isSingleton());
		assertRequirement(new NotRequirement(new RequiredCapability("libc0.1", new VersionRange(Version.emptyVersion, true, new Version(1), false))), iu.getRequiredCapabilities());
		assertRequirement(new NotRequirement(new RequiredCapability("libc0.3", new VersionRange(Version.emptyVersion, true, new Version(1), false))), iu.getRequiredCapabilities());
		assertRequirement(new NotRequirement(new RequiredCapability("libc6", new VersionRange(Version.emptyVersion, true, new Version(17), false))), iu.getRequiredCapabilities());
		assertRequirement(new NotRequirement(new RequiredCapability("libc6.1", new VersionRange(Version.emptyVersion, true, new Version(1), false))), iu.getRequiredCapabilities());
	}

	public void testCheckLibx11data() {
		InstallableUnit iu = getIU("libx11-data");
		assertEquals(true, iu.isSingleton());
	}

	public void testlibtextCharwidthPerl() {
		InstallableUnit iu = getIU("libtext-charwidth-perl");
		assertEquals(true, iu.isSingleton());
		assertRequirement(new RequiredCapability("libc6", new VersionRange(new Version(1), true, Version.maxVersion, true)), iu.getRequiredCapabilities());
		assertRequirement(new RequiredCapability("perl-base", new VersionRange(new Version(12), true, Version.maxVersion, true)), iu.getRequiredCapabilities());
		IRequiredCapability[] reqs = iu.getRequiredCapabilities();
		for (int i = 0; i < reqs.length; i++) {
			if (reqs[i] instanceof ORRequirement) {
				assertRequirement(new RequiredCapability("perlapi-5.10.0--virtual", VersionRange.emptyRange), ((ORRequirement) reqs[i]).getRequirements());
				assertRequirement(new RequiredCapability("perlapi-5.10.0", VersionRange.emptyRange), ((ORRequirement) reqs[i]).getRequirements());
			}
		}
		assertNotRequirement(new NotRequirement(new RequiredCapability("libtext-charwidth-perl", VersionRange.emptyRange)), iu.getRequiredCapabilities());
	}

	public void testFoo() {
		InstallableUnit iu = getIU("foo");
		assertEquals(false, iu.isSingleton());
		assertProvide(new ProvidedCapability("x", VersionRange.emptyRange), iu.getProvidedCapabilities());
		assertRequirement(new NotRequirement(new RequiredCapability("x", VersionRange.emptyRange)), iu.getRequiredCapabilities());
	}

	private void assertNotRequirement(IRequiredCapability asserted, IRequiredCapability[] reqs) {
		for (int i = 0; i < reqs.length; i++) {
			if (asserted.getName().equals(reqs[i].getName())) {
				if (asserted.getRange().equals(reqs[i].getRange()) && asserted.getArity() == reqs[i].getArity() && asserted.isNegation() == reqs[i].isNegation())
					fail("Requirement not expected:" + asserted);
			}
		}
	}

	private void assertRequirement(IRequiredCapability asserted, IRequiredCapability[] reqs) {
		boolean found = true;
		for (int i = 0; i < reqs.length; i++) {
			if (asserted.getName().equals(reqs[i].getName())) {
				assertEquals(asserted.getRange(), reqs[i].getRange());
				assertEquals(asserted.getArity(), reqs[i].getArity());
				assertEquals(asserted.isNegation(), reqs[i].isNegation());
			}
		}
		assertEquals(true, found);
	}

	private void assertProvide(IProvidedCapability asserted, IProvidedCapability[] caps) {
		boolean found = true;
		for (int i = 0; i < caps.length; i++) {
			if (asserted.getName().equals(caps[i].getName())) {
				assertEquals(asserted.getVersion(), caps[i].getVersion());
			}
		}
		assertEquals(true, found);
	}

	public void testCheckPackageNegatedDepends() {
		InstallableUnit iu = getIU("negatedDepends");
		assertRequirement(new NotRequirement(new RequiredCapability("a", new VersionRange(new Version(2)))), iu.getRequiredCapabilities());
	}
}