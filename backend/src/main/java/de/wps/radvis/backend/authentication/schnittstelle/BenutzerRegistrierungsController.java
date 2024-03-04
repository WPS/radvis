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

package de.wps.radvis.backend.authentication.schnittstelle;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerExistiertBereitsException;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.MailService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/benutzer")
@Validated
@Slf4j
public class BenutzerRegistrierungsController {
	private final CommonConfigurationProperties commonConfigurationProperties;
	private final BenutzerService benutzerService;
	private final MailService mailService;

	public BenutzerRegistrierungsController(
		CommonConfigurationProperties commonConfigurationProperties,
		BenutzerService benutzerService,
		MailService mailService) {
		this.benutzerService = benutzerService;
		this.mailService = mailService;
		this.commonConfigurationProperties = commonConfigurationProperties;
	}

	@PostMapping(path = "/registriere-benutzer")
	public void registriereBenutzer(Authentication authentication,
		@RequestBody @Valid RegistriereBenutzerCommand command)
		throws BenutzerExistiertBereitsException {

		RadVisUserDetails currentUser = (RadVisUserDetails) authentication.getPrincipal();

		Benutzer neuerBenutzer = benutzerService.registriereBenutzer(
			command.getVorname(),
			command.getNachname(),
			command.getOrganisation(),
			command.getRollen(),
			currentUser.getServiceBwId(),
			command.getEmail());

		// refresh SecurityContext
		UserDetails neueUserDetails = RadVisUserDetailsService.fromUser(neuerBenutzer);
		SecurityContextHolder.getContext().setAuthentication(new RadVisAuthentication(neueUserDetails));

		String linkAufBenutzer =
			commonConfigurationProperties.getBasisUrl() + FrontendLinks.benutzerAdministration(neuerBenutzer.getId());
		String mailText = String.format("Der Benutzer '%s %s' wurde angelegt und wartet auf Freischaltung.\n" +
				"Die Bearbeitung des Benutzers kann erfolgen unter %s .",
			neuerBenutzer.getVorname(), neuerBenutzer.getNachname(), linkAufBenutzer);
		List<Benutzer> zustaendigeBenutzer = benutzerService.getAlleZustaendigenBenutzer(neuerBenutzer);
		List<String> zustaendigeMailadressen = zustaendigeBenutzer.stream()
			.map(benutzer -> benutzer.getMailadresse().toString()).collect(Collectors.toList());
		mailService.sendMail(zustaendigeMailadressen, "Neuer Benutzer wurde angelegt", mailText);

		log.info("Benutzer mit ID '{}' wurde angelegt.", neuerBenutzer.getId());
	}
}
