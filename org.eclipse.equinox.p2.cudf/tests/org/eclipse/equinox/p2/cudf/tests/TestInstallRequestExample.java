package org.eclipse.equinox.p2.cudf.tests;

import java.util.Collection;
import java.util.Iterator;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.cudf.Parser;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;
import org.eclipse.equinox.p2.cudf.solver.SimplePlanner;

public class TestInstallRequestExample extends TestCase {
	private ProfileChangeRequest pcr = null;

	protected void setUp() throws Exception {
		pcr = new Parser().parse(this.getClass().getClassLoader().getResource("testData/instances/expectedSuccess/installRequest.cudf").openStream());
	}

	public void testParanoid() {
		Object result = new SimplePlanner().getSolutionFor(pcr, "paranoid");
		if (result instanceof Collection) {
			Collection col = (Collection) result;
			assertEquals(col.toString(), 2, col.size());
		} else {
			fail("No result found!");
		}
	}

	public void testP2() {
		Object result = new SimplePlanner().getSolutionFor(pcr, "p2");
		if (result instanceof Collection) {
			Collection col = (Collection) result;
			assertEquals(col.toString(), 3, col.size());
		} else {
			fail("No result found!");
		}
	}

	public void testTrendy() {
		Object result = new SimplePlanner().getSolutionFor(pcr, "trendy");
		if (result instanceof Collection) {
			Collection col = (Collection) result;
			assertEquals(col.toString(), 2, col.size());
		} else {
			fail("No result found!");
		}
	}

	private InstallableUnit getIU(Collection col, String id) {
		Iterator it = col.iterator();
		while (it.hasNext()) {
			InstallableUnit iu = (InstallableUnit) it.next();
			if (id.equals(iu.getId()))
				return iu;
		}
		fail("Can't find: " + id);
		return null;
	}
}