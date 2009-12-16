package org.eclipse.equinox.p2.cudf.metadata;

public class NotRequirement implements IRequiredCapability {
	private IRequiredCapability negatedRequirement;

	public NotRequirement(IRequiredCapability iRequiredCapabilities) {
		negatedRequirement = iRequiredCapabilities;
	}

	public IRequiredCapability getRequirement() {
		return negatedRequirement;
	}

	public String getName() {
		return negatedRequirement.getName();
	}

	public VersionRange getRange() {
		return negatedRequirement.getRange();
	}

	public boolean isNegation() {
		return true;
	}

	public String toString() {
		return "NOT(" + negatedRequirement.toString() + ')'; //$NON-NLS-1$
	}
}
