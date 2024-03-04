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

package de.wps.radvis.backend.fahrradroute.domain.entity.provider;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.fahrradroute.domain.entity.ImportedToubizRoute;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;

public class ImportedToubizRouteTestDataProvider {
	public static ImportedToubizRoute.ImportedToubizRouteBuilder withDefaultValues() {
		LineString originalGeometrie = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(20, 20));
		return ImportedToubizRoute.builder()
			.toubizId(ToubizId.of("TestDataToubizId"))
			.name(FahrradrouteName.of("TestImportedToubizRoute"))
			.beschreibung("Die ist eine Fahrradroute, die durch den FahrradrouteTestDataProvider erstellt wurde")
			.linksZuWeiterenMedien(new ArrayList<>())
			.kurzbezeichnung("kurzbezeichnung")
			.info("info")
			.tourenkategorie(Tourenkategorie.RADTOUR)
			.offizielleLaenge(Laenge.of(2))
			.homepage("testRadvis.de")
			.emailAnsprechpartner("testmail@testRadvis.de")
			.lizenz("MIT")
			.lizenzNamensnennung("keine")
			.zuletztBearbeitet(LocalDateTime.of(2022, 1, 1, 10, 10))
			.originalGeometrie(originalGeometrie);
	}
}
