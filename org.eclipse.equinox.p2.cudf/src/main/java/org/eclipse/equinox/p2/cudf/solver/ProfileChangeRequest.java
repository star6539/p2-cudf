/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.cudf.solver;

import java.util.ArrayList;

import org.eclipse.equinox.p2.cudf.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.cudf.metadata.NotRequirement;
import org.eclipse.equinox.p2.cudf.query.QueryableArray;

public class ProfileChangeRequest {

	private final QueryableArray initialState;
	private ArrayList iusToRemove = new ArrayList(10); // list of ius to remove
	private ArrayList iusToAdd = new ArrayList(10); // list of ius to add
	private ArrayList iusToUpdate = new ArrayList(10); // list of ius to add
	
	public ProfileChangeRequest(QueryableArray initialState) {
		this.initialState = initialState;
	}
	
	public void addInstallableUnit(IRequiredCapability req) {
		iusToAdd.add(req);
	}

	public void removeInstallableUnit(IRequiredCapability toUninstall) {
		iusToRemove.add(new NotRequirement(toUninstall));
	}

	public void upgradeInstallableUnit(IRequiredCapability toUpgrade) {
		iusToUpdate.add(toUpgrade);
	}
	
	public ArrayList getAllRequests() {
		ArrayList result = new ArrayList(iusToAdd.size()  + iusToRemove.size() + iusToUpdate.size());
		result.addAll(iusToAdd);
		result.addAll(iusToRemove);
		result.addAll(iusToUpdate);
		return result;
	}
	
	public QueryableArray getInitialState() {
		return initialState;
	}
}
