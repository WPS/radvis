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

package de.wps.radvis.backend.fahrradroute.schnittstelle;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteService;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import lombok.NonNull;

public class FahrradrouteGuard {

	private final NetzService netzService;
	private final BenutzerResolver benutzerResolver;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final FahrradrouteService fahrradrouteService;

	public FahrradrouteGuard(@NonNull NetzService netzService, @NonNull BenutzerResolver benutzerResolver,
		@NonNull ZustaendigkeitsService zustaendigkeitsService, @NonNull FahrradrouteService fahrradrouteService) {
		this.netzService = netzService;
		this.benutzerResolver = benutzerResolver;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.fahrradrouteService = fahrradrouteService;
	}

	public void createFahrradroute(Authentication authentication, CreateFahrradrouteCommand command) {
		assertDarfFahrradrouteBearbeiten(authentication, command.getKantenIDs().stream());
	}

	public void saveFahrradroute(Authentication authentication, SaveFahrradrouteCommand command,
		Fahrradroute fahrradroute) {

		if (fahrradroute.getFahrradrouteTyp() != FahrradrouteTyp.RADVIS_ROUTE) {
			throw new AccessDeniedException("Es dürfen nur RadVIS-Routen bearbeitet werden.");
		}

		assertDarfFahrradrouteBearbeiten(authentication, command.getKantenIDs().stream());

		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		if (!darfFahrradrouteBearbeiten(aktiverBenutzer, fahrradroute)) {
			throw new AccessDeniedException("Sie sind nicht berechtigt diese Fahrradroute zu bearbeiten.");
		}
	}

	public void changeVeroeffentlicht(Authentication authentication, ChangeFahrradrouteVeroeffentlichtCommand command) {
		Fahrradroute fahrradroute = fahrradrouteService.loadForModification(command.getId(),
			command.getVersion());
		Stream<Long> kantenIDs = fahrradroute.getAbschnittsweiserKantenBezug().stream().map(b -> b.getKante().getId());
		assertDarfFahrradrouteBearbeiten(authentication, kantenIDs);
	}

	public void deleteFahrradroute(Authentication authentication, Fahrradroute fahrradroute) {
		if (Kategorie.LANDESRADFERNWEG.equals(fahrradroute.getKategorie())) {
			throw new AccessDeniedException("Landesradfernwege können nicht gelöscht werden.");
		}
		if (Kategorie.D_ROUTE.equals(fahrradroute.getKategorie())) {
			throw new AccessDeniedException("D-Routen können nicht gelöscht werden.");
		}

		assertDarfFahrradrouteBearbeiten(authentication, fahrradroute.getAbschnittsweiserKantenBezug().stream()
			.map(abschnittsweiserKantenBezug -> abschnittsweiserKantenBezug.getKante().getId()));
	}

	private void assertDarfFahrradrouteBearbeiten(Authentication authentication, Stream<Long> kantenIDs) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		if (aktiverBenutzer.hatRecht(Recht.ALLE_RADROUTEN_ERFASSEN_BEARBEITEN)) {
			return;
		}

		if (!aktiverBenutzer.hatRecht(Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException("Sie sind nicht berechtigt Radrouten zu bearbeiten.");
		}

		Set<Long> kantenIdSet = kantenIDs.collect(Collectors.toSet());
		if (kantenIdSet.isEmpty()) {
			return;
		}

		List<Kante> kantenAbschnitte = netzService.getKanten(kantenIdSet);
		if (!zustaendigkeitsService.anyInZustaendigkeitsbereich(aktiverBenutzer, kantenAbschnitte.stream())) {
			throw new AccessDeniedException("Es liegt keine Kante oder Knoten in Ihrem Zuständigkeitsbereich.");
		}
	}

	public boolean darfFahrradrouteBearbeiten(Benutzer aktiverBenutzer, Fahrradroute fahrradroute) {
		if (aktiverBenutzer.hatRecht(Recht.ALLE_RADROUTEN_ERFASSEN_BEARBEITEN)) {
			return true;
		}

		List<AbschnittsweiserKantenBezug> fahrradrouteKantenAbschnitte = fahrradroute.getAbschnittsweiserKantenBezug();
		Stream<Kante> kantenAbschnitte = fahrradrouteKantenAbschnitte.stream()
			.map(AbschnittsweiserKantenBezug::getKante);

		return aktiverBenutzer.hatRecht(Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN) &&
			(
				fahrradrouteKantenAbschnitte.isEmpty() ||
					zustaendigkeitsService.anyInZustaendigkeitsbereich(aktiverBenutzer, kantenAbschnitte)
			);
	}
}
