/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.cudf;

import java.net.URL;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.cudf"; //$NON-NLS-1$
	private static PackageAdmin packageAdmin;
	private static BundleContext bundleContext;

	public Activator() {
		//
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		packageAdmin = (PackageAdmin) context.getService(context.getServiceReference(PackageAdmin.class.getName()));
		bundleContext = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		packageAdmin = null;
		bundleContext = null;
	}

	public static URL getFile(String location) {
		return bundleContext.getBundle().getEntry(location);
	}

	private static boolean startEarly(String bundleName) throws BundleException {
		Bundle bundle = Activator.getBundle(bundleName);
		if (bundle == null)
			return false;
		bundle.start(Bundle.START_TRANSIENT);
		return true;
	}

	public static void startPrereqs() throws BundleException {
		if (!startEarly("org.eclipse.equinox.p2.exemplarysetup")) //$NON-NLS-1$
			return;
		if (!startEarly("org.eclipse.equinox.simpleconfigurator.manipulator")) //$NON-NLS-1$
			return;
		if (!startEarly("org.eclipse.equinox.frameworkadmin.equinox")) //$NON-NLS-1$
			return;
		if (!startEarly("org.eclipse.equinox.p2.engine")) //$NON-NLS-1$
			return;
	}

	/*
	 * Return the bundle with the given symbolic name, or null if it cannot be found.
	 */
	static synchronized Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

}
