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

package de.wps.radvis.backend.administration.schnittstelle;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.administration.schnittstelle.command.SaveBenutzerCommand;
import de.wps.radvis.backend.administration.schnittstelle.command.SaveBenutzerCommandConverter;
import de.wps.radvis.backend.administration.schnittstelle.view.BenutzerEditView;
import de.wps.radvis.backend.administration.schnittstelle.view.BenutzerListView;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/benutzer")
@Validated
@Slf4j
public class BenutzerVerwaltungController {

	private final BenutzerService benutzerService;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final SaveBenutzerCommandConverter saveBenutzerCommandConverter;
	private final MailService mailService;
	private final SessionRegistry sessionRegistry;
	private final CommonConfigurationProperties commonConfigurationProperties;
	private final BenutzerResolver benutzerResolver;
	private final BenutzerGuard benutzerGuard;

	public BenutzerVerwaltungController(BenutzerService benutzerService,
		VerwaltungseinheitService verwaltungseinheitService,
		SaveBenutzerCommandConverter saveBenutzerCommandConverter,
		MailService mailService,
		BenutzerResolver benutzerResolver,
		SessionRegistry sessionRegistry, CommonConfigurationProperties commonConfigurationProperties,
		BenutzerGuard benutzerGuard) {
		this.benutzerService = benutzerService;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.saveBenutzerCommandConverter = saveBenutzerCommandConverter;
		this.mailService = mailService;
		this.sessionRegistry = sessionRegistry;
		this.benutzerResolver = benutzerResolver;
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.benutzerGuard = benutzerGuard;
	}

	@GetMapping(path = "/benutzerorganisationen")
	public List<VerwaltungseinheitView> getBenutzerOrganisationen(Authentication authentication) {
		this.benutzerGuard.getBenutzerOrganisationen(authentication);

		return verwaltungseinheitService.getAll()
			.stream()
			.map(VerwaltungseinheitView::new)
			.sorted(Comparator.comparing(VerwaltungseinheitView::getName))
			.collect(Collectors.toList());
	}

	@GetMapping(path = "/list")
	public List<BenutzerListView> getAlleBenutzer(Authentication authentication) {
		this.benutzerGuard.getAlleBenutzer(authentication);
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		return benutzerService.getAlleBenutzerByZustaendigerBenutzer(aktiverBenutzer).stream()
			.map(BenutzerListView::new).collect(Collectors.toList());
	}

	@GetMapping(path = "/{id}")
	public BenutzerEditView getBenutzer(@PathVariable long id, Authentication authentication)
		throws BenutzerIstNichtRegistriertException {
		Benutzer zuBearbeitenderBenutzer = benutzerService.getBenutzer(id);
		this.benutzerGuard.getBenutzer(authentication, zuBearbeitenderBenutzer);

		return new BenutzerEditView(zuBearbeitenderBenutzer);
	}

	@PostMapping(path = "/save")
	public BenutzerEditView saveBenutzer(Authentication authentication,
		@RequestBody SaveBenutzerCommand command)
		throws Exception {
		this.benutzerGuard.saveBenutzer(authentication, command);

		Benutzer zuAendernderBenutzer = benutzerService.getBenutzerForModifikation(command.getId(),
			command.getVersion());

		saveBenutzerCommandConverter.apply(zuAendernderBenutzer, command);
		zuAendernderBenutzer = benutzerService.save(zuAendernderBenutzer);
		expireUserSessions(zuAendernderBenutzer);
		return new BenutzerEditView(zuAendernderBenutzer);
	}

	@PostMapping(path = "aktiviere-benutzer")
	public BenutzerEditView aktiviereBenutzer(Authentication authentication, @RequestParam long id,
		@RequestParam long version) throws Exception {
		this.benutzerGuard.aktiviereBenutzer(authentication, id, version);

		Benutzer benutzer = benutzerService.aendereBenutzerstatus(id, version, true);

		String mailText = String.format(
			"Hallo %s %s,\nIhr RadVIS-Account für %s wurde aktiviert und kann nach einem erneuten Einloggen verwendet werden.",
			benutzer.getVorname(),
			benutzer.getNachname(),
			benutzer.getMailadresse())
			+ "\nSie können RadVIS unter " + commonConfigurationProperties.getBasisUrl() + " erreichen.";

		mailService.sendMail(List.of(benutzer.getMailadresse().toString()), "Sie wurden für RadVIS freigeschaltet",
			mailText);

		expireUserSessions(benutzer);
		log.info("Benutzer [{} {}, '{}' von {}] wurde Freigeschaltet.", benutzer.getVorname(),
			benutzer.getNachname(), benutzer.getMailadresse(), benutzer.getOrganisation().getName());

		return new BenutzerEditView(benutzer);
	}

	@PostMapping(path = "deaktiviere-benutzer")
	public BenutzerEditView deaktiviereBenutzer(Authentication authentication, @RequestParam long id,
		@RequestParam long version)
		throws Exception {
		this.benutzerGuard.deaktiviereBenutzer(authentication, id, version);

		Benutzer benutzer = benutzerService.aendereBenutzerstatus(id, version, false);

		expireUserSessions(benutzer);
		log.info("Benutzer [{} {}, '{}' von {}] wurde gesperrt.", benutzer.getVorname(), benutzer.getNachname(),
			benutzer.getMailadresse(), benutzer.getOrganisation().getName());

		return new BenutzerEditView(benutzer);
	}

	private void expireUserSessions(Benutzer benutzer) {
		sessionRegistry.getAllPrincipals().stream()
			// passenden benutzer aus den session suchen
			.map(user -> (UserDetails) user)
			.filter(user -> user.getUsername().equals(benutzer.getServiceBwId().toString()))
			// alle session für den user holen und beenden
			.flatMap(user -> sessionRegistry.getAllSessions(user, false).stream())
			.forEach(SessionInformation::expireNow);
	}
}
