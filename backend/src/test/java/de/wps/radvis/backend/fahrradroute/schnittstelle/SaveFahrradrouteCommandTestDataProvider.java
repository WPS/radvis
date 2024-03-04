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

import java.util.Collections;
import java.util.List;

import org.geojson.LineString;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.fahrradroute.schnittstelle.SaveFahrradrouteCommand.SaveFahrradrouteCommandBuilder;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;

public class SaveFahrradrouteCommandTestDataProvider {

	public static SaveFahrradrouteCommand.SaveFahrradrouteCommandBuilder withDefaultValue() {
		return SaveFahrradrouteCommand.builder()
			.name("Ein schöner neuer Name")
			.beschreibung("Kurze Beschreibung")
			.kurzbeschreibung("Normal lange und sehr detailierte Beschreibung")
			.kategorie(Kategorie.RADSCHNELLWEG)
			.tourenkategorie(Tourenkategorie.RADTOUR)
			.offizielleLaenge(Laenge.of(12345L))
			.homepage("https://neue-homepage.de")
			.verantwortlichId(1234L)
			.emailAnsprechpartner("neue@testRadvis.de")
			.lizenz("CC-BY-NEU 2.0")
			.lizenzNamensnennung("Neue Namesnennung").kantenIDs(List.of(1l))
			.routenVerlauf((LineString) GeoJsonConverter.createGeoJsonGeometry(
				GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(20, 20),
					new Coordinate(100, 100))))
			.stuetzpunkte(GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)))
			.varianten(Collections.emptyList())
			.profilEigenschaften(Collections.emptyList());
	}

	public static SaveFahrradrouteCommandBuilder withKante(long kanteId) {
		return withDefaultValue().kantenIDs(List.of(kanteId));
	}
}
