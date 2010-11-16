package org.eclipse.equinox.p2.cudf.tests;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.eclipse.equinox.p2.cudf.Parser;
import org.eclipse.equinox.p2.cudf.solver.*;

public class SolverComparator {
	public static void main(String[] args) throws IOException {
		File inputFile = new File("/Users/Pascal/tmp/compet/data.mancoosi.org/misc2010/results/problems/debian-dudf/58a4a468-38a5-11df-a561-00163e7a6f5e.cudf.bz2");
		File solutionFile = new File("/Users/Pascal/tmp/compet/data.mancoosi.org/misc2010/results/solutions/p2cudf-trendy-1.6/58a4a468-38a5-11df-a561-00163e7a6f5e.cudf.debian-dudf.result.bz2");
		ProfileChangeRequest solution = new Parser().parse(CUDFTestHelper.getStream(solutionFile));

		ProfileChangeRequest req = new Parser().parse(CUDFTestHelper.getStream(inputFile));
		SolverConfiguration configuration = new SolverConfiguration("trendy", "300s", true, false);
		Object result = new SimplePlanner().getSolutionFor(req, configuration);
		if (result instanceof Collection) {
			if (((Collection) result).containsAll(solution.getInitialState().getList()) && solution.getInitialState().getList().containsAll((Collection) result))
				System.out.println("cool");
			else
				System.err.println("Computed solution does not match expected one");
		}
	}
}
