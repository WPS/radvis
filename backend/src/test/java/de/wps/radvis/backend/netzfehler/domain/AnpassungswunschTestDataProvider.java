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

package de.wps.radvis.backend.netzfehler.domain;

import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch.AnpassungswunschBuilder;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;

public class AnpassungswunschTestDataProvider {
	public static AnpassungswunschBuilder defaultValue() {
		return Anpassungswunsch.builder()
			.geometrie(GeometryTestdataProvider.createPoint(new Coordinate(1, 1)))
			.beschreibung("beschreibung 1")
			.status(AnpassungswunschStatus.OFFEN).kategorie(AnpassungswunschKategorie.DLM)
			.benutzerLetzteAenderung(BenutzerTestDataProvider.defaultBenutzer().build())
			.kommentarListe(new KommentarListe())
			.status(AnpassungswunschStatus.ERLEDIGT);
	}

	public static AnpassungswunschBuilder withPosition(double x, double y) {
		return defaultValue()
			.geometrie(GeometryTestdataProvider.createPoint(new Coordinate(x, y)));
	}
}
