package john.j.cool.maven.model.profile;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Calculates the active profiles among a given collection of profiles.
 * 
 * @author john.j.cool
 */
@Component(role = ProfileSelector.class)
public class AndActivationProfileSelector implements ProfileSelector {

	@Requirement(role = ProfileActivator.class)
	private List<ProfileActivator> _activators = new ArrayList<ProfileActivator>();

	public AndActivationProfileSelector addProfileActivator(ProfileActivator profileActivator) {
		if (profileActivator != null) {
			_activators.add(profileActivator);
		}
		return this;
	}

	public List<Profile> getActiveProfiles(Collection<Profile> profiles, ProfileActivationContext context, ModelProblemCollector problems) {
		Collection<String> activatedIds = new HashSet<String>(context.getActiveProfileIds());
		Collection<String> deactivatedIds = new HashSet<String>(context.getInactiveProfileIds());

		List<Profile> activeProfiles = new ArrayList<Profile>(profiles.size());
		List<Profile> activePomProfilesByDefault = new ArrayList<Profile>();
		boolean activatedPomProfileNotByDefault = false;

		for (Profile profile : profiles) {
			if (!deactivatedIds.contains(profile.getId())) {
				if (activatedIds.contains(profile.getId()) || isActive(profile, context, problems)) {
					activeProfiles.add(profile);
					if (Profile.SOURCE_POM.equals(profile.getSource())) {
						activatedPomProfileNotByDefault = true;
					}
				} else if (isActiveByDefault(profile)) {
					if (Profile.SOURCE_POM.equals(profile.getSource())) {
						activePomProfilesByDefault.add(profile);
					} else {
						activeProfiles.add(profile);
					}
				}

			}
		}

		if (!activatedPomProfileNotByDefault) {
			activeProfiles.addAll(activePomProfilesByDefault);
		}
		return activeProfiles;
	}

	private boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
		List<ProfileActivator> activators = getActivatorsByProfile(profile);
		boolean active = !activators.isEmpty();
		for (ProfileActivator activator : activators) {
			try {
				active = activator.isActive(profile, context, problems);
				if (!active) {
					break;
				}
			} catch (RuntimeException e) {
				problems.add(Severity.ERROR, "Failed to determine activation for profile " + profile.getId(), profile.getLocation(""), e);
				return false;
			}
		}
		return active;
	}
	
	private List<ProfileActivator> getActivatorsByProfile(Profile profile) {
		Activation activation = profile.getActivation();
		List<ProfileActivator> activators = new ArrayList<ProfileActivator>();
		if (profile.getActivation() != null) {
			if (activation.getFile() != null) {
				activators.add(getActivatorByType(FileProfileActivator.class));
			}
			if (activation.getJdk() != null) {
				activators.add(getActivatorByType(JdkVersionProfileActivator.class));
			}
			if (activation.getOs() != null) {
				activators.add(getActivatorByType(OperatingSystemProfileActivator.class));
			}
			if (activation.getProperty() != null) {
				activators.add(getActivatorByType(PropertyProfileActivator.class));
			}
		}
		return activators;
	}

	private ProfileActivator getActivatorByType(@SuppressWarnings("rawtypes") Class clazz) {
		for (ProfileActivator activator : _activators) {
			if (clazz.isInstance(activator)) {
				return activator;
			}
		}
		return null;
	}

	private boolean isActiveByDefault(Profile profile) {
		Activation activation = profile.getActivation();
		return activation != null && activation.isActiveByDefault();
	}

}
