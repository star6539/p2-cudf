package org.eclipse.equinox.p2.cudf.solver;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.specs.TimeoutException;

/**
 * Job for computing SAT failure explanation in the background.
 */
class ExplanationJob extends Job {
	private static final boolean DEBUG = false;
	private Set explanation;
	private DependencyHelper dependencyHelper;

	public ExplanationJob(DependencyHelper dependencyHelper) {
		super(Messages.Planner_NoSolution);
		this.dependencyHelper = dependencyHelper;
		//explanations cannot be canceled directly, so don't show it to the user
		setSystem(true);
	}

	public boolean belongsTo(Object family) {
		return family == ExplanationJob.this;
	}

	protected void canceling() {
		super.canceling();
		dependencyHelper.stopExplanation();
	}

	public Set getExplanationResult() {
		return explanation;
	}

	protected IStatus run(IProgressMonitor monitor) {
		long start = 0;
		if (DEBUG) {
			start = System.currentTimeMillis();
			Tracing.debug("Determining cause of failure: " + start); //$NON-NLS-1$
		}
		try {
			explanation = dependencyHelper.why();
			if (DEBUG) {
				long stop = System.currentTimeMillis();
				Tracing.debug("Explanation found: " + (stop - start)); //$NON-NLS-1$
				Tracing.debug("Explanation:"); //$NON-NLS-1$
				for (Iterator i = explanation.iterator(); i.hasNext();) {
					Tracing.debug(i.next().toString());
				}
			}
		} catch (TimeoutException e) {
			if (DEBUG)
				Tracing.debug("Timeout while computing explanations"); //$NON-NLS-1$
		} finally {
			//must never have a null result, because caller is waiting on result to be non-null
			if (explanation == null)
				explanation = Collections.EMPTY_SET;
		}
		synchronized (this) {
			ExplanationJob.this.notify();
		}
		return Status.OK_STATUS;
	}

}
