package org.eclipse.equinox.p2.cudf.metadata;

public class NotRequirement implements IRequiredCapability {
	private IRequiredCapability negatedRequirement;

	public NotRequirement(IRequiredCapability iRequiredCapabilities) {
		negatedRequirement = iRequiredCapabilities;
	}

	public IRequiredCapability getRequirement() {
		return negatedRequirement;
	}

	public String getFilter() {
		return negatedRequirement.getFilter();
	}

	public String getName() {
		return negatedRequirement.getName();
	}

	public String getNamespace() {
		return negatedRequirement.getNamespace();
	}

	public VersionRange getRange() {
		return negatedRequirement.getRange();
	}

	public String[] getSelectors() {
		return negatedRequirement.getSelectors();
	}

	public boolean isGreedy() {
		return negatedRequirement.isGreedy();
	}

	public boolean isMultiple() {
		return negatedRequirement.isMultiple();
	}

	public boolean isOptional() {
		return negatedRequirement.isOptional();
	}

	public void setFilter(String filter) {
		// TODO Auto-generated method stub

	}

	public void setSelectors(String[] selectors) {
		// TODO Auto-generated method stub

	}

	public boolean isNegation() {
		return true;
	}

	public String toString() {
		return "NOT(" + negatedRequirement.toString() + ')'; //$NON-NLS-1$
	}

	public boolean satisfiedBy(IProvidedCapability cap) {
		return !negatedRequirement.satisfiedBy(cap);
	}
}
