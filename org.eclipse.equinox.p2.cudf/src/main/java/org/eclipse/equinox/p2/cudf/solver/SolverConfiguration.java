package org.eclipse.equinox.p2.cudf.solver;

public class SolverConfiguration {
	public boolean verbose = false;
	public String objective = "p2";
	public String timeout = "default";
	public boolean explain = false;

	public SolverConfiguration(String objective, String timeout, boolean verbose, boolean explain) {
		if (objective != null)
			this.objective = objective;
		if (timeout != null)
			this.timeout = timeout;
		this.verbose = verbose;
		this.explain = explain;
	}
}
