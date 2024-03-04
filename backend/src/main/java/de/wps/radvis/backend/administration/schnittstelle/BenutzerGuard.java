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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.administration.schnittstelle.command.SaveBenutzerCommand;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.NonNull;

public class BenutzerGuard {

	@NonNull
	private final BenutzerService benutzerService;

	@NonNull
	private final BenutzerResolver benutzerResolver;

	public BenutzerGuard(
		@NonNull BenutzerService benutzerService,
		@NonNull BenutzerResolver benutzerResolver) {
		this.benutzerService = benutzerService;
		this.benutzerResolver = benutzerResolver;
	}

	public void benutzerStatusAendern(Authentication authentication, long id, long version)
		throws BenutzerIstNichtRegistriertException {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Benutzer zuBearbeitenderBenutzer = benutzerService.getBenutzer(id);

		if (BenutzerService.benutzerverwaltungsRechte.stream().noneMatch(aktiverBenutzer::hatRecht)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Benutzer zu ändern.");
		}

		List<Rolle> rollenDieDerAktiveBenutzerNichtAendernDarf = rollenDieDerBenutzerNichtAendernDarf(aktiverBenutzer,
			zuBearbeitenderBenutzer.getRollen());

		if (!rollenDieDerAktiveBenutzerNichtAendernDarf.isEmpty()) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Benutzer mit folgenden Rollen zu ändern:"
					+ rollenDieDerAktiveBenutzerNichtAendernDarf.stream()
					.map(Rolle::toString)
					.collect(Collectors.joining("', '", " '", "'")));
		}

		if (!pruefeBearbeiterIstAutorisiertFuerBenutzer(aktiverBenutzer, zuBearbeitenderBenutzer)) {
			throw new AccessDeniedException("Sie sind nicht dazu autorisiert diesen Benutzer zu ändern.");
		}
	}

	public void getAlleBenutzer(Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		if (BenutzerService.benutzerverwaltungsRechte.stream().noneMatch(aktiverBenutzer::hatRecht)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung auf Benutzer zuzugreifen.");
		}
	}

	public void getBenutzer(Authentication authentication, Benutzer zuBearbeitenderBenutzer)
		throws BenutzerIstNichtRegistriertException {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		if (!benutzerService.pruefeBearbeiterIstAutorisiertFuerBenutzer(aktiverBenutzer, zuBearbeitenderBenutzer)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung auf diesen Benutzer zuzugreifen.");
		}
	}

	public void saveBenutzer(Authentication authentication, SaveBenutzerCommand command)
		throws BenutzerIstNichtRegistriertException {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Benutzer zuBearbeitenderBenutzer = benutzerService.getBenutzer(command.getId());

		if (BenutzerService.benutzerverwaltungsRechte.stream().noneMatch(aktiverBenutzer::hatRecht)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Benutzer zu speichern.");
		}

		if (!pruefeBearbeiterIstAutorisiertFuerBenutzer(aktiverBenutzer, zuBearbeitenderBenutzer)) {
			throw new AccessDeniedException("Sie sind nicht Berechtigt den Benutzer zu verändern.");
		}

		List<Rolle> rollenDieDerAktiveBenutzerNichtAendernDarf = rollenDieDerBenutzerAenderWillAberNichtDarf(
			aktiverBenutzer, zuBearbeitenderBenutzer, command.getRollen());

		if (!rollenDieDerAktiveBenutzerNichtAendernDarf.isEmpty()) {
			throw new AccessDeniedException(
				"Sie sind nicht dazu autorisiert diese Rollen zu vergeben oder zu entnehmen:"
					+ rollenDieDerAktiveBenutzerNichtAendernDarf.stream()
					.map(Rolle::toString)
					.collect(Collectors.joining("', '", " '", "'")));
		}
	}

	public void getBenutzerOrganisationen(Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		if (BenutzerService.benutzerverwaltungsRechte.stream().noneMatch(aktiverBenutzer::hatRecht)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung auf Benutzer zuzugreifen.");
		}
	}

	List<Rolle> rollenDieDerBenutzerAenderWillAberNichtDarf(Benutzer aktiverBenutzer, Benutzer zuBearbeitenderBenutzer,
		Set<Rolle> neueRollen) {
		return rollenDieDerBenutzerNichtAendernDarf(
			aktiverBenutzer,
			ermittleGeaenderterollen(zuBearbeitenderBenutzer, neueRollen));
	}

	List<Rolle> rollenDieDerBenutzerNichtAendernDarf(Benutzer aktiverBenutzer, Set<Rolle> neueRollen) {
		return neueRollen
			.stream()
			.filter(rolle -> benutzerService.ermittleVergaberechteFuerRolle(rolle).stream()
				.noneMatch(aktiverBenutzer::hatRecht))
			.collect(Collectors.toList());
	}

	boolean pruefeBearbeiterIstAutorisiertFuerBenutzer(Benutzer bearbeiter, Benutzer zuBearbeitenderBenutzer) {
		List<Benutzer> autorisierteBenutzer = zuBearbeitenderBenutzer.getRollen().contains(Rolle.RADVIS_ADMINISTRATOR)
			? benutzerService.getRadvisAdmins()
			: findAutorisierteFuerNormalenBenutzer(zuBearbeitenderBenutzer);
		return autorisierteBenutzer.contains(bearbeiter);
	}

	private List<Benutzer> findAutorisierteFuerNormalenBenutzer(Benutzer zuBearbeitenderBenutzer) {
		List<Benutzer> autorisierteBenutzer = new ArrayList<>();
		Verwaltungseinheit aktuelleOrganisationsebene = zuBearbeitenderBenutzer.getOrganisation();
		while (aktuelleOrganisationsebene != null) {
			autorisierteBenutzer.addAll(benutzerService.findAdminaufSelberEbene(aktuelleOrganisationsebene));
			aktuelleOrganisationsebene = aktuelleOrganisationsebene.getUebergeordneteVerwaltungseinheit()
				.orElse(null);
		}
		autorisierteBenutzer.addAll(benutzerService.getRadvisAdmins());
		return autorisierteBenutzer;
	}

	Set<Rolle> ermittleGeaenderterollen(Benutzer zuAendernderBenutzer, Set<Rolle> rollen) {
		// kopieren, da sonst die Originale verändert werden.
		Set<Rolle> alteRollen = new HashSet<>(zuAendernderBenutzer.getRollen());
		Set<Rolle> neueRollen = new HashSet<>(rollen);

		// gemeinsame Rollen entfernen
		alteRollen.removeAll(rollen); // alle Rollen die entfernt wurden
		neueRollen.removeAll(zuAendernderBenutzer.getRollen()); // alle Rollen, die neu hinzu gekommen sind

		// Menge der geänderten Klassen
		alteRollen.addAll(neueRollen);
		return alteRollen;
	}
}
