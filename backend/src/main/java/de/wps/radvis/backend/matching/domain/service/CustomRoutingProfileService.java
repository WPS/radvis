/*
 * Copyright (c) 2023 WPS - Workplace Solutions GmbH
 *
 * Licensed under the EUPL, Version 1.2 or as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.wps.radvis.backend.matching.domain.service;

import java.util.Collection;
import java.util.List;

import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.matching.domain.entity.CustomRoutingProfile;
import de.wps.radvis.backend.matching.domain.event.CustomRoutingProfilesDeletedEvent;
import de.wps.radvis.backend.matching.domain.repository.CustomRoutingProfileRepository;
import jakarta.transaction.Transactional;

public class CustomRoutingProfileService {

	private final CustomRoutingProfileRepository customRoutingProfileRepository;

	public CustomRoutingProfileService(CustomRoutingProfileRepository customRoutingProfileRepository) {
		this.customRoutingProfileRepository = customRoutingProfileRepository;
	}

	@Transactional
	public Iterable<CustomRoutingProfile> updateCustomRoutingProfiles(Collection<CustomRoutingProfile> profiles) {
		List<CustomRoutingProfile> toDelete = this.customRoutingProfileRepository.findAll().stream()
			.filter(profile -> !profiles.contains(profile)).toList();

		this.customRoutingProfileRepository.deleteAll(toDelete);
		RadVisDomainEventPublisher.publish(
			new CustomRoutingProfilesDeletedEvent(toDelete.stream().map(CustomRoutingProfile::getId).toList()));

		return this.customRoutingProfileRepository.saveAll(profiles);
	}
}
