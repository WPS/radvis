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

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.administration.domain.AdministrationService;
import de.wps.radvis.backend.administration.schnittstelle.command.CreateOrganisationCommand;
import de.wps.radvis.backend.administration.schnittstelle.command.SaveOrganisationCommand;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityNotFoundException;

public class OrganisationGuard {

	private final BenutzerResolver benutzerResolver;
	private final BenutzerService benutzerService;
	private final AdministrationService administrationService;
	private final OrganisationRepository organisationRepository;

	public OrganisationGuard(BenutzerResolver benutzerResolver, BenutzerService benutzerService,
		AdministrationService administrationService,
		OrganisationRepository organisationRepository) {
		this.benutzerResolver = benutzerResolver;
		this.benutzerService = benutzerService;
		this.administrationService = administrationService;
		this.organisationRepository = organisationRepository;
	}

	public void create(Authentication authentication, CreateOrganisationCommand command) {
		if (command.getOrganisationsArt().istGebietskoerperschaft()) {
			throw new AccessDeniedException("Organisationen dieses Typs dürfen nicht angelegt werden.");
		}

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		pruefeBerechtigungOrganisationenVerwalten(benutzer);

		if (command.getZustaendigFuerBereichOf().size() > 0) {
			if (!administrationService.getAllZuweisbareOrganisationenForBenutzer(benutzer).stream()
				.map(VerwaltungseinheitDbView::getId)
				.collect(Collectors.toList())
				.containsAll(command.getZustaendigFuerBereichOf())) {
				throw new AccessDeniedException(
					"Sie haben nicht die Berechtigung, diesen Zuständigkeitsbereich zuzuweisen.");

			}
		}
	}

	public void save(Authentication authentication, SaveOrganisationCommand command) {
		Organisation organisation = organisationRepository.findById(command.getId())
			.orElseThrow(EntityNotFoundException::new);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (hasNameOrArtChanged(organisation, command)) {
			if (!benutzerService
				.darfBenutzerOrganisationErstellenOderBearbeiten(benutzer,
					organisation)) {
				throw new AccessDeniedException("Sie haben nicht die Berechtigung Organisationen zu verwalten.");

			}
		}

		Set<Long> geaenderteZustaendigkeiten = filterGeaenderteZustaendigkeiten(command, organisation);
		if (geaenderteZustaendigkeiten.size() > 0) {
			if (!administrationService.getAllZuweisbareOrganisationenForBenutzer(benutzer).stream()
				.map(VerwaltungseinheitDbView::getId)
				.collect(Collectors.toList())
				.containsAll(geaenderteZustaendigkeiten)) {
				throw new AccessDeniedException(
					"Sie haben nicht die Berechtigung, diesen Zuständigkeitsbereich zuzuweisen.");

			}
		}
	}

	public void deaktiviereOrganisation(Authentication authentication, long id) {
		Verwaltungseinheit organisation = organisationRepository.findById(id)
			.orElseThrow(EntityNotFoundException::new);

		if (!benutzerService
			.darfBenutzerOrganisationErstellenOderBearbeiten(benutzerResolver.fromAuthentication(authentication),
				organisation)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Organisationen zu deaktivieren.");
		}
	}

	public void aktiviereOrganisation(Authentication authentication, long id) {
		Verwaltungseinheit organisation = organisationRepository.findById(id)
			.orElseThrow(EntityNotFoundException::new);

		if (!benutzerService
			.darfBenutzerOrganisationErstellenOderBearbeiten(benutzerResolver.fromAuthentication(authentication),
				organisation)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Organisationen zu aktivieren.");
		}
	}

	public void listNichtGebietskoerperschaften(Authentication authentication) {
		pruefeBerechtigungOrganisationenVerwalten(benutzerResolver.fromAuthentication(authentication));
	}

	public void listAlleZuweisbaren(Authentication authentication) {
		pruefeBerechtigungOrganisationenVerwalten(benutzerResolver.fromAuthentication(authentication));
	}

	public void organisationView(Authentication authentication) {
		pruefeBerechtigungOrganisationenVerwalten(benutzerResolver.fromAuthentication(authentication));
	}

	private Set<Long> filterGeaenderteZustaendigkeiten(SaveOrganisationCommand command,
		Organisation organisation) {
		Set<Long> currentOrgIds = organisation.getZustaendigFuerBereichOf().stream().map(Gebietskoerperschaft::getId)
			.collect(Collectors.toSet());

		Set<Long> removed = currentOrgIds.stream().filter(curr -> !command.getZustaendigFuerBereichOf().contains(curr))
			.collect(Collectors.toSet());

		Set<Long> added = command.getZustaendigFuerBereichOf().stream()
			.filter(incoming -> !currentOrgIds.contains(incoming))
			.collect(Collectors.toSet());

		Set<Long> result = removed;
		result.addAll(added);

		return result;
	}

	private boolean hasNameOrArtChanged(Verwaltungseinheit organisation, SaveOrganisationCommand command) {
		return !organisation.getName().equals(command.getName())
			|| !organisation.getOrganisationsArt().equals(command.getOrganisationsArt());
	}

	private void pruefeBerechtigungOrganisationenVerwalten(Benutzer benutzer) {
		if (!benutzer.getRechte().contains(Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN)
			&& !benutzer.getRechte()
			.contains(Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN)
			&& !benutzer.getRechte().contains(Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Organisationen zu verwalten.");

		}
	}
}
