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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.equinox.p2.cudf.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.cudf.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.metadata.NotRequirement;
import org.eclipse.equinox.p2.cudf.metadata.ORRequirement;
import org.eclipse.equinox.p2.cudf.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.cudf.metadata.RequiredCapability;
import org.eclipse.equinox.p2.cudf.metadata.Version;
import org.eclipse.equinox.p2.cudf.metadata.VersionRange;
import org.eclipse.equinox.p2.cudf.query.QueryableArray;
import org.eclipse.equinox.p2.cudf.solver.ProfileChangeRequest;

/**
 * TODO - if we have 2 versions in a row for the same bundle, convert it to a version range (e>2,e<4 should be e (2,4))
 * TODO - are the keys in the stanzas always lower-case?
 */
public class Parser {

	private static final boolean DEBUG = false;
	private static InstallableUnit currentIU = null;
	private static ProfileChangeRequest currentRequest = null;
	private static List allIUs = new ArrayList();
	private static QueryableArray query = null;

	static class Tuple {
		String name;
		String version;
		String operator;

		Tuple(String line) {
			String[] tuple = new String[3];
			int i = 0;
			for (StringTokenizer iter = new StringTokenizer(line, " \t"); iter.hasMoreTokens(); i++)
				tuple[i] = iter.nextToken();
			name = tuple[0];
			operator = tuple[1];
			version = tuple[2];
		}
	}

	public static ProfileChangeRequest parse(File file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String next = reader.readLine();
			while (true) {

				// look-ahead to check for line continuation
				String line = next;
				for (next = reader.readLine(); next != null && next.length() > 0 && next.charAt(0) == ' '; next = reader.readLine()) {
					line = line + next.substring(1);
				}

				// terminating condition of the loop... reached the end of the file
				if (line == null) {
					validateAndAddIU();
					break;
				}

				// end of stanza
				if (line.length() == 0) {
					validateAndAddIU();
					continue;
				}

				// preamble stanza
				if (line.startsWith("#") || line.startsWith("preamble: ") || line.startsWith("property: ") || line.startsWith("univ-checksum: ")) {
					// ignore
				}

				// request stanza
				else if (line.startsWith("request: ")) {
					handleRequest(line);
				} else if (line.startsWith("install: ")) {
					handleInstall(line);
				} else if (line.startsWith("upgrade: ")) {
					handleUpgrade(line);
				} else if (line.startsWith("remove: ")) {
					handleRemove(line);
				}

				// package stanza
				else if (line.startsWith("package: ")) {
					handlePackage(line);
				} else if (line.startsWith("version: ")) {
					handleVersion(line);
				} else if (line.startsWith("installed: ")) {
					handleInstalled(line);
				} else if (line.startsWith("depends: ")) {
					handleDepends(line);
				} else if (line.startsWith("conflicts: ")) {
					handleConflicts(line);
				} else if (line.startsWith("provides: ")) {
					handleProvides(line);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
		}
		if (DEBUG) {
			for (Iterator iter = allIUs.iterator(); iter.hasNext();)
				debug((InstallableUnit) iter.next());

			debug(currentRequest);
		}
		return currentRequest;
	}

	/*
	 * Ensure that the current IU that we have been building is validate and if so, then
	 * add it to our collected list of all converted IUs from the file.
	 */
	private static void validateAndAddIU() {
		if (currentIU == null)
			return;
		// For a package stanze, the id and version are the only mandatory elements
		if (currentIU.getId() == null)
			throw new IllegalStateException("Malformed \'package\' stanza. No package element found.");
		if (currentIU.getVersion() == null)
			throw new IllegalStateException("Malformed \'package\' stanza. Package " + currentIU.getId() + " does not have a version.");
		allIUs.add(currentIU);
		// reset to be ready for the next stanza
		currentIU = null;
	}

	private static void handleInstalled(String line) {
		String value = line.substring("installed: ".length());
		if (value.length() != 0) {
			if (DEBUG)
				if (!Boolean.valueOf(value).booleanValue()) {
					System.err.println("Unexcepted value for installed.");
					return;
				}
			currentIU.setInstalled(true);
		}
	}

	private static void handleInstall(String line) {
		line = line.substring("install: ".length());
		List installRequest = createRequires(line);
		for (Iterator iterator = installRequest.iterator(); iterator.hasNext();) {
			currentRequest.addInstallableUnit((IRequiredCapability) iterator.next());
		}
		return;
	}

	private static void handleRequest(String line) {
		initializeQueryableArray();
		currentRequest = new ProfileChangeRequest(query);
	}

	private static void handleRemove(String line) {
		line = line.substring("remove: ".length());
		List removeRequest = createRequires(line);
		for (Iterator iterator = removeRequest.iterator(); iterator.hasNext();) {
			currentRequest.removeInstallableUnit((IRequiredCapability) iterator.next());
		}
		return;
	}

	private static void initializeQueryableArray() {
		query = new QueryableArray((InstallableUnit[]) allIUs.toArray(new InstallableUnit[allIUs.size()]));
	}

	private static void handleUpgrade(String line) {
		line = line.substring("upgrade: ".length());
		List removeRequest = createRequires(line);
		for (Iterator iterator = removeRequest.iterator(); iterator.hasNext();) {
			currentRequest.upgradeInstallableUnit((IRequiredCapability) iterator.next());
		}
		return;
	}

	/*
	 * Convert the version string to a version object and set it on the IU
	 */
	private static void handleVersion(String line) {
		currentIU.setVersion(new Version(line.substring("Version: ".length())));
	}

	// VPkg ::= PkgName (Sp + VConstr)?
	// VConstr ::= RelOp Sp + Ver
	// RelOp ::= "=" | "!=" | ">=" | ">" | "<=" | "<"
	// Sp ::= U+0020 (i.e. space)
	// | U+0009 (i.e. tab)
	// Ver ::= PosInt
	private static void handleDepends(String line) {
		mergeRequirements(createRequires(line.substring("depends: ".length())));
	}

	/*
	 * Conflicts are like depends except NOT'd.
	 */
	private static void handleConflicts(String line) {
		List reqs = createRequires(line.substring("conflicts: ".length()));
		List conflicts = new ArrayList();
		for (Iterator iter = reqs.iterator(); iter.hasNext();)
			conflicts.add(new NotRequirement((IRequiredCapability) iter.next()));
		mergeRequirements(conflicts);
	}

	/*
	 * Set the given list of requirements on teh current IU. Merge if necessary.
	 */
	private static void mergeRequirements(List requirements) {
		if (currentIU.getRequiredCapabilities() != null) {
			IRequiredCapability[] current = currentIU.getRequiredCapabilities();
			for (int i = 0; i < current.length; i++)
				requirements.add(current[i]);
		}
		currentIU.setRequiredCapabilities((IRequiredCapability[]) requirements.toArray(new IRequiredCapability[requirements.size()]));
	}

	private static List createPackageList(String line) {
		List result = new ArrayList();
		for (StringTokenizer outer = new StringTokenizer(line, ","); outer.hasMoreTokens();) {
			Tuple tuple = new Tuple(outer.nextToken());
			result.add(tuple);
		}
		return result;
	}

	/*
	 * Create and return a generic list of required capabilities. This list can be from a depends or conflicts entry.
	 */
	private static List createRequires(String line) {
		// break the string into per-package instructions
		List ANDs = new ArrayList();
		for (StringTokenizer outer = new StringTokenizer(line, ","); outer.hasMoreTokens();) {
			String andStmt = outer.nextToken();

			List ORs = new ArrayList();
			for (StringTokenizer inner = new StringTokenizer(andStmt, "|"); inner.hasMoreTokens();) {
				String orStmt = inner.nextToken();
				Tuple tuple = new Tuple(orStmt);

				// special code to handle not equals
				if (tuple.operator != null && "!=".equals(tuple.operator)) {
					// TODO Pascal to get an explanation on this but if you have "depends: a != 1" does that mean
					// you require at least one version of "a" and it can't be 1? Or is it ok to not have a requirement on "a"?
					ORs.add(new ORRequirement(new IRequiredCapability[] {createRequiredCapability(tuple.name, "<", tuple.version), createRequiredCapability(tuple.name, ">", tuple.version)}));
				} else
					ORs.add(createRequiredCapability(tuple.name, tuple.operator, tuple.version));
			}
			if (ORs.size() == 1)
				ANDs.add(ORs.get(0));
			else
				ANDs.add(new ORRequirement((IRequiredCapability[]) ORs.toArray(new IRequiredCapability[ORs.size()])));
		}
		return ANDs;
	}

	/*
	 * Create and return a required capability for the given info. operator and number can be null which means any version. (0.0.0)
	 */
	private static IRequiredCapability createRequiredCapability(String name, String operator, String number) {
		return new RequiredCapability(name, createVersionRange(operator, number));
	}

	private static IProvidedCapability createProvidedCapability(String name, String operator, String number) {
		// TODO not quite right... we are ignoring the operator
		Version version = number == null ? Version.emptyVersion : Version.parseVersion(number);
		return new ProvidedCapability(name, version);
	}

	/*
	 * Create and return a version range based on the given operator and number. Note that != is
	 * handled elsewhere.
	 */
	private static VersionRange createVersionRange(String operator, String number) {
		if (operator == null || number == null)
			return VersionRange.emptyRange;
		if ("=".equals(operator))
			return new VersionRange('[' + number + ',' + number + ']');
		if ("<".equals(operator))
			return new VersionRange("[0," + number + ')');
		if (">".equals(operator))
			return new VersionRange('(' + number + ',' + Integer.MAX_VALUE + ']');
		if ("<=".equals(operator))
			return new VersionRange("[0," + number + ']');
		if (">=".equals(operator))
			return new VersionRange('[' + number + ',' + Integer.MAX_VALUE + ']');
		return VersionRange.emptyRange;
	}

	// package name matches: "^[a-zA-Z0-9+./@()%-]+$"
	private static void handlePackage(String readLine) {
		currentIU = new InstallableUnit();
		currentIU.setId(readLine.substring("package: ".length()));
	}

	private static void handleProvides(String line) {
		List pkgs = createPackageList(line);
		for (Iterator iter = pkgs.iterator(); iter.hasNext();) {
			Tuple tuple = (Tuple) iter.next();
			//			pkgs.add(createProvidedCapability(tuple.name, tuple.operator, tuple.version));
		}

	}

	//	// copied from ProfileSynchronizer
	private static void debug(ProfileChangeRequest request) {
		if (!DEBUG || request == null)
			return;
		//		System.out.println("\nProfile Change Request:");
		//		InstallableUnit[] toAdd = request.getAddedInstallableUnit();
		//		if (toAdd == null || toAdd.length == 0) {
		//			System.out.println("No installable units to add.");
		//		} else {
		//			for (int i = 0; i < toAdd.length; i++)
		//				System.out.println("Adding IU: " + toAdd[i].getId() + ' ' + toAdd[i].getVersion());
		//		}
		//		Map propsToAdd = request.getInstallableUnitProfilePropertiesToAdd();
		//		if (propsToAdd == null || propsToAdd.isEmpty()) {
		//			System.out.println("No IU properties to add.");
		//		} else {
		//			for (Iterator iter = propsToAdd.keySet().iterator(); iter.hasNext();) {
		//				Object key = iter.next();
		//				System.out.println("Adding IU property: " + key + "->" + propsToAdd.get(key));
		//			}
		//		}
		//
		//		InstallableUnit[] toRemove = request.getRemovedInstallableUnits();
		//		if (toRemove == null || toRemove.length == 0) {
		//			System.out.println("No installable units to remove.");
		//		} else {
		//			for (int i = 0; i < toRemove.length; i++)
		//				System.out.println("Removing IU: " + toRemove[i].getId() + ' ' + toRemove[i].getVersion());
		//		}
		//		Map propsToRemove = request.getInstallableUnitProfilePropertiesToRemove();
		//		if (propsToRemove == null || propsToRemove.isEmpty()) {
		//			System.out.println("No IU properties to remove.");
		//		} else {
		//			for (Iterator iter = propsToRemove.keySet().iterator(); iter.hasNext();) {
		//				Object key = iter.next();
		//				System.out.println("Removing IU property: " + key + "->" + propsToRemove.get(key));
		//			}
		//		}
	}

	// dump info to console
	private static void debug(InstallableUnit unit) {
		if (!DEBUG)
			return;
		System.out.println("\nInstallableUnit: " + unit.getId());
		System.out.println("Version: " + unit.getVersion());
		if (unit.isInstalled())
			System.out.println("Installed: true");
		IRequiredCapability[] reqs = unit.getRequiredCapabilities();
		for (int i = 0; i < reqs.length; i++) {
			System.out.println("Requirement: " + reqs[i]);
		}
	}

}
