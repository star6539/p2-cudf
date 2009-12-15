/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.cudf.metadata;

/**
 * Describes a capability as exposed or required by an installable unit
 */
public class ProvidedCapability implements IProvidedCapability {
	private final String name;
	private final Version version;

	public ProvidedCapability(String name, Version version) {
		this.name = name;
		this.version = version == null ? Version.emptyVersion : version;
	}

	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof IProvidedCapability))
			return false;
		IProvidedCapability otherCapability = (IProvidedCapability) other;
		if (!(name.equals(otherCapability.getName())))
			return false;
		return version.equals(otherCapability.getVersion());
	}

	public String getName() {
		return name;
	}

	public Version getVersion() {
		return version;
	}

	public int hashCode() {
		return name.hashCode() * version.hashCode();
	}

	/**
	 * Returns whether this provided capability satisfies the given required capability.
	 * @return <code>true</code> if this capability satisfies the given required
	 * capability, and <code>false</code> otherwise.
	 */
	public boolean satisfies(IRequiredCapability candidate) {
		return candidate.satisfiedBy(this);
	}

	public String toString() {
		return name + '/' + version;
	}

}
