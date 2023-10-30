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

package de.wps.radvis.backend.netz.domain.service;

import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.springframework.security.access.AccessDeniedException;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ZustaendigkeitsService {

	private OrganisationConfigurationProperties organisationConfigurationProperties;

	public boolean allInZustaendigkeitsbereich(Benutzer aktiverBenutzer, Stream<Kante> kantenAbschnitte) {
		return kantenAbschnitte.allMatch(kante -> istImZustaendigkeitsbereich(kante, aktiverBenutzer));
	}

	public boolean anyInZustaendigkeitsbereich(Benutzer aktiverBenutzer, Stream<Kante> kantenAbschnitte) {
		return kantenAbschnitte.anyMatch(kante -> istImZustaendigkeitsbereich(kante, aktiverBenutzer));
	}

	public void assertAllGeometriesInZustaendigkeitsbereich(Benutzer aktiverBenutzer, Stream<Kante> kantenAbschnitte,
		Stream<Knoten> knoten, Stream<Kante> kantenPunkte) {
		if (!allInZustaendigkeitsbereich(aktiverBenutzer, kantenAbschnitte)
			|| knoten.anyMatch(knoten1 -> !istImZustaendigkeitsbereich(knoten1, aktiverBenutzer))
			|| !allInZustaendigkeitsbereich(aktiverBenutzer, kantenPunkte)) {
			throw new AccessDeniedException(
				"Mindestens eine Kante oder ein Knoten liegt nicht in Ihrem Zuständigkeitsbereich.");
		}
	}

	public boolean istImZustaendigkeitsbereich(Kante kante, Benutzer benutzer) {
		return istImZustaendigkeitsbereich(kante.getGeometry(), benutzer);
	}

	public boolean istImZustaendigkeitsbereich(Knoten knoten, Benutzer benutzer) {
		return istImZustaendigkeitsbereich(knoten.getPoint(), benutzer);
	}

	public boolean istImZustaendigkeitsbereich(Geometry geometry, Benutzer benutzer) {
		return benutzer.getOrganisation().getBereichBuffer(organisationConfigurationProperties.getZustaendigkeitBufferInMeter())
			.intersects(geometry);
	}
}
