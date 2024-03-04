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

package de.wps.radvis.backend.massnahme.domain;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MassnahmenZustaendigkeitsService {

	private final BenutzerRepository benutzerRepository;

	private final static Set<Rolle> ZUSTAENDIGE_BEARBEITER = Set.of(Rolle.RADWEGE_ERFASSERIN, Rolle.KREISKOORDINATOREN,
		Rolle.RADVERKEHRSBEAUFTRAGTER);

	public MassnahmenZustaendigkeitsService(BenutzerRepository benutzerRepository) {
		this.benutzerRepository = benutzerRepository;
	}

	public List<Benutzer> getZustaendigeBarbeiterVonUmsetzungsstandabfrage(Massnahme massnahme) {
		Optional<Verwaltungseinheit> zustaendigeVerwaltungseinheit = massnahme.getZustaendiger();

		if (zustaendigeVerwaltungseinheit.isEmpty()) {
			log.warn("Zu der Massnahme {} ({}) ist keine Verwaltungseinheit als Zuständig angegeben.",
				massnahme.getBezeichnung(), massnahme.getId());
			return Collections.emptyList();
		}

		Verwaltungseinheit zustaendigeOrganisation = zustaendigeVerwaltungseinheit.get();

		List<Benutzer> alleZusteandigeBenutzer = benutzerRepository
			.findByOrganisationAndStatus(zustaendigeOrganisation, BenutzerStatus.AKTIV);

		List<Benutzer> empfaengerkreis = alleZusteandigeBenutzer.stream()
			.filter(benutzer -> benutzer.getRollen().stream().anyMatch(ZUSTAENDIGE_BEARBEITER::contains))
			.collect(Collectors.toList());

		if (empfaengerkreis.isEmpty()) {
			log.warn(
				"Der Empfängerkreis zu der Massnahme {} ({}) ist leer: Es gibt keine Benutzer in der Organisation {} mit der Rolle Radwege Erfasser*in.",
				massnahme.getBezeichnung(),
				massnahme.getId(),
				zustaendigeOrganisation.getDisplayText());
			return Collections.emptyList();
		}

		return empfaengerkreis;
	}

	public List<Benutzer> getZustaendigeKreiskoordinatoren(Verwaltungseinheit verwaltungseinheit) {
		return benutzerRepository.findByOrganisationAndRollen(verwaltungseinheit, Rolle.KREISKOORDINATOREN);
	}
}
