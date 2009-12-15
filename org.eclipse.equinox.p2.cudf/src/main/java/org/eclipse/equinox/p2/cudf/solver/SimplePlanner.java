package org.eclipse.equinox.p2.cudf.solver;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.cudf.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.cudf.metadata.InstallableUnit;
import org.eclipse.equinox.p2.cudf.metadata.Version;
import org.eclipse.equinox.p2.cudf.query.QueryableArray;

public class SimplePlanner {
	public Object getSolutionFor(ProfileChangeRequest profileChangeRequest) {
			QueryableArray profile = profileChangeRequest.getInitialState();

			InstallableUnit updatedPlan = updatePlannerInfo(profileChangeRequest);

//			Slicer slicer = new Slicer(profile, null, satisfyMetaRequirements(profileChangeRequest.getProfileProperties()));
//			IQueryable slice = slicer.slice(new InstallableUnit[] {(InstallableUnit) updatedPlan[0]}, sub.newChild(ExpandWork / 4));
//			if (slice == null)
//				return new ProvisioningPlan(slicer.getStatus(), profileChangeRequest, null);
			Projector projector = new Projector(profile);
			projector.encode(updatedPlan);
			IStatus s = projector.invokeSolver();
			if (s.getSeverity() != IStatus.OK) {
				System.out.println(projector.getExplanation(null));
				return s;//projector.getExplanation(null);
			}
			
			return projector.extractSolution();
	}
	
	private InstallableUnit updatePlannerInfo(ProfileChangeRequest profileChangeRequest) {
		return createIURepresentingTheProfile(profileChangeRequest.getAllRequests());
	}
	
	private InstallableUnit createIURepresentingTheProfile(ArrayList allRequirements) {
		InstallableUnit iud = new InstallableUnit();
		String time = Long.toString(System.currentTimeMillis());
		iud.setId(time);
		iud.setVersion(new Version(0, 0, 0, time));
		iud.setRequiredCapabilities((IRequiredCapability[]) allRequirements.toArray(new IRequiredCapability[allRequirements.size()]));
		return iud;
	}
}
