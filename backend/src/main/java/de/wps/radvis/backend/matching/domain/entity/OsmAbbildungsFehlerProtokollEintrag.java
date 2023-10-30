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

package de.wps.radvis.backend.matching.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;

public class OsmAbbildungsFehlerProtokollEintrag implements FehlerprotokollEintrag {

	private final Long kanteId;
	private final Geometry originalGeometry;
	private final LocalDateTime datum;

	private final String netzklassen;

	public OsmAbbildungsFehlerProtokollEintrag(OsmAbbildungsFehler osmAbbildungsFehler) {
		this.kanteId = osmAbbildungsFehler.getKanteId();
		this.originalGeometry = osmAbbildungsFehler.getOriginalGeometry();
		this.datum = osmAbbildungsFehler.getDatum();

		if (osmAbbildungsFehler.isSonstigeNetzklasse()) {
			this.netzklassen = "Sonstige"; // Kein RadNETZ, Kreisnetz, Kommunalnetz
		} else {
			List<String> netzklassenDisplayText = new ArrayList<>();
			if (osmAbbildungsFehler.isRadnetz()) {
				netzklassenDisplayText.add("RadNETZ");
			}
			if (osmAbbildungsFehler.isKreisnetz()) {
				netzklassenDisplayText.add("Kreisnetz");
			}
			if (osmAbbildungsFehler.isKommunalnetz()) {
				netzklassenDisplayText.add("Kommunalnetz");
			}
			this.netzklassen = String.join(", ", netzklassenDisplayText);
		}
	}

	@Override
	public Long getId() {
		return this.kanteId;
	}

	@Override
	public Geometry getIconPosition() {
		return this.originalGeometry.getCentroid();
	}

	@Override
	public Geometry getOriginalGeometry() {
		return this.originalGeometry;
	}

	@Override
	public LocalDateTime getDatum() {
		return this.datum;
	}

	@Override
	public String getTitel() {
		return "OSM-Abbildungsfehler: " + netzklassen;
	}

	@Override
	public String getBeschreibung() {
		return "Für die attribuierte Kante mit der ID " + this.kanteId + " konnte bei der Ausleitung "
			+ "in das OSM-Modell kein entsprechender Abschnitt gefunden werden.";
	}

	@Override
	public String getEntityLink() {
		return FrontendLinks.kanteDetailView(kanteId);
	}
}
  