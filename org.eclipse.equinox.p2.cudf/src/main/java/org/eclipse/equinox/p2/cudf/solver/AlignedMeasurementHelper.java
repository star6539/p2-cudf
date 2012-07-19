package org.eclipse.equinox.p2.cudf.solver;

import java.util.*;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;

public class AlignedMeasurementHelper {

	private String firstLvlProperty;
	private String secondLvlProperty;

	protected Map<String, Map<String, List<InstallableUnit>>> clusterMap = new HashMap<String, Map<String, List<InstallableUnit>>>();

	public AlignedMeasurementHelper(String firstLvlProperty, String secondLvlProperty) {
		this.firstLvlProperty = firstLvlProperty;
		this.secondLvlProperty = secondLvlProperty;
	}

	public void addIU(InstallableUnit iu) {
		String firstLvlPropertyValue = iu.getExtraPropertyValue(this.firstLvlProperty);
		String secondLvlPropertyValue = iu.getExtraPropertyValue(this.secondLvlProperty);
		checkPropertyValuesAreNotNull(iu, firstLvlPropertyValue, secondLvlPropertyValue);
		Map<String, List<InstallableUnit>> secondLevelMap = this.clusterMap.get(firstLvlPropertyValue);
		if (secondLevelMap == null) {
			secondLevelMap = new HashMap<String, List<InstallableUnit>>();
			this.clusterMap.put(firstLvlPropertyValue, secondLevelMap);
		}
		List<InstallableUnit> ius = secondLevelMap.get(secondLvlPropertyValue);
		if (ius == null) {
			ius = new ArrayList<InstallableUnit>();
			secondLevelMap.put(secondLvlPropertyValue, ius);
		}
		ius.add(iu);
	}

	private void checkPropertyValuesAreNotNull(InstallableUnit iu, String firstLvlPropertyValue, String secondLvlPropertyValue) {
		if ((firstLvlPropertyValue == null) || (secondLvlPropertyValue == null)) {
			String exceptionMessage1 = (firstLvlPropertyValue == null) ? ("IU \"" + iu.toString() + "\" has no property \"" + this.firstLvlProperty + "\"") : ("");
			String exceptionMessage2 = (secondLvlPropertyValue == null) ? ("IU \"" + iu.toString() + "\" has no property \"" + this.secondLvlProperty + "\"") : ("");
			String exceptionMessageSep = ((firstLvlPropertyValue == null) || (secondLvlPropertyValue == null)) ? ("") : ("\n");
			String exceptionMessage = exceptionMessage1 + exceptionMessageSep + exceptionMessage2;
			throw new IllegalArgumentException(exceptionMessage);
		}
	}

	public Iterator<List<InstallableUnit>> firstLvlClustersIterator() {
		return new FirstLvlClusterIUsIterator();
	}

	public Iterator<List<InstallableUnit>> secondLvlClustersIterator() {
		return new SecondLvlClusterIUsIterator();
	}

	private class FirstLvlClusterIUsIterator implements Iterator<List<InstallableUnit>> {

		private Iterator<String> clusterMapKeysetIt;

		public FirstLvlClusterIUsIterator() {
			this.clusterMapKeysetIt = clusterMap.keySet().iterator();
		}

		public boolean hasNext() {
			return this.clusterMapKeysetIt.hasNext();
		}

		public List<InstallableUnit> next() {
			String currentKey = this.clusterMapKeysetIt.next();
			List<InstallableUnit> res = new ArrayList<InstallableUnit>();
			Map<String, List<InstallableUnit>> secondLvlMap = clusterMap.get(currentKey);
			Iterator<String> secondLvlMapKeysetIt = secondLvlMap.keySet().iterator();
			while (secondLvlMapKeysetIt.hasNext()) {
				res.addAll(secondLvlMap.get(secondLvlMapKeysetIt.next()));
			}
			return res;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private class SecondLvlClusterIUsIterator implements Iterator<List<InstallableUnit>> {

		private Iterator<String> clusterMapKeysetIt;

		private Iterator<String> currentClusterKeysetIt = null;

		private Map<String, List<InstallableUnit>> secondLvlMap;

		public SecondLvlClusterIUsIterator() {
			this.clusterMapKeysetIt = clusterMap.keySet().iterator();
			if (this.clusterMapKeysetIt.hasNext()) {
				nextSecondLvlIt();
			}
		}

		public boolean hasNext() {
			return this.clusterMapKeysetIt.hasNext() || (this.currentClusterKeysetIt != null && this.currentClusterKeysetIt.hasNext());
		}

		public List<InstallableUnit> next() {
			if (!this.currentClusterKeysetIt.hasNext()) {
				nextSecondLvlIt();
			}
			return new ArrayList<InstallableUnit>(this.secondLvlMap.get(this.currentClusterKeysetIt.next()));
		}

		private void nextSecondLvlIt() {
			String firstKey = this.clusterMapKeysetIt.next();
			this.secondLvlMap = clusterMap.get(firstKey);
			this.currentClusterKeysetIt = secondLvlMap.keySet().iterator();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}
}