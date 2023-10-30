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

package de.wps.radvis.backend.matching.schnittstelle;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.matching.domain.CustomRoutingProfileRepository;
import de.wps.radvis.backend.matching.domain.entity.CustomRoutingProfile;
import de.wps.radvis.backend.matching.domain.service.CustomRoutingProfileService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/custom-routing-profile")
@Validated
public class CustomRoutingProfileController {

	private final CustomRoutingProfileRepository customRoutingProfileRepository;
	private final CustomRoutingProfileService customRoutingProfileService;
	private final BenutzerResolver benutzerResolver;
	private final CustomRoutingProfileGuard guard;

	public CustomRoutingProfileController(CustomRoutingProfileRepository customRoutingProfileRepository,
		CustomRoutingProfileService customRoutingProfileService, BenutzerResolver benutzerResolver,
		CustomRoutingProfileGuard guard) {
		this.customRoutingProfileRepository = customRoutingProfileRepository;
		this.customRoutingProfileService = customRoutingProfileService;
		this.benutzerResolver = benutzerResolver;
		this.guard = guard;
	}

	@GetMapping("/list")
	public List<CustomRoutingProfile> getAll(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		if (benutzer == null) {
			return Collections.emptyList();
		}

		return this.customRoutingProfileRepository.findAll();
	}

	@Transactional
	@PostMapping("/save")
	public Iterable<CustomRoutingProfile> save(Authentication authentication,
		@RequestBody List<SaveCustomRoutingProfileCommand> commands) {
		guard.save(authentication, commands);
		
		List<CustomRoutingProfile> profiles = commands.stream().map(command ->
				CustomRoutingProfile.builder()
					.id(command.getId())
					.profilJson(command.getProfilJson())
					.name(command.getName())
					.build())
			.toList();

		return customRoutingProfileService.updateCustomRoutingProfiles(profiles);
	}
}
