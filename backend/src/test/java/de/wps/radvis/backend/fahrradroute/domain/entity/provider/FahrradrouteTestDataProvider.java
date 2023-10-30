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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class FahrradrouteTestDataProvider {

	public static Fahrradroute.FahrradrouteBuilder onKante(Kante... kanten) {
		return withDefaultValues()
			.abschnittsweiserKantenBezug(
				Arrays.stream(kanten)
					.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0.0, 1.0)))
					.collect(Collectors.toList()));
	}

	public static Fahrradroute.FahrradrouteBuilder withDefaultValues() {

		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		LineString originalGeometrie = kante.getGeometry();
		AbschnittsweiserKantenBezug abschnittsweiserKantenBezug = new AbschnittsweiserKantenBezug(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation = new FahrradroutenMatchingAndRoutingInformation();

		return Fahrradroute.builder()
			.toubizId(ToubizId.of("testToubizId"))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.name(FahrradrouteName.of("TestFahrradroute"))
			.kurzbeschreibung("Krzbschrbng")
			.info("info")
			.beschreibung("Die ist eine Fahrradroute, die durch den FahrradrouteTestDataProvider erstellt wurde")
			.tourenkategorie(Tourenkategorie.GRAVEL_TOUR)
			.kategorie(Kategorie.SONSTIGER_RADWANDERWEG)
			.offizielleLaenge(Laenge.of(123))
			.homepage("https://web.site")
			.verantwortlich(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.emailAnsprechpartner("e@ma.il")
			.lizenz("LI-ZE-NS 2.0")
			.lizenzNamensnennung("Namen nennen? Ok äh... Hans, Petra, Walter, Kunigunde ... äh Bert ... Waltraut ...")
			.zuletztBearbeitet(LocalDateTime.of(2022, 8, 1, 10, 0))
			.originalGeometrie(originalGeometrie)
			.iconLocation(originalGeometrie.getStartPoint())
			.abschnittsweiserKantenBezug(new ArrayList<>(List.of(abschnittsweiserKantenBezug)))
			.netzbezugLineString(kante.getGeometry())
			.fahrradroutenMatchingAndRoutingInformation(fahrradroutenMatchingAndRoutingInformation)
			.varianten(new ArrayList<>())
			.linearReferenzierteProfilEigenschaften(new ArrayList<>())
			.veroeffentlicht(false);
	}

	public static Fahrradroute.FahrradrouteBuilder defaultWithCustomNetzbezug(
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs,
		LineString netzbezugLineString,
		Geometry original) {
		return withDefaultValues()
			.abschnittsweiserKantenBezug(abschnittsweiserKantenBezugs)
			.netzbezugLineString(netzbezugLineString)
			.originalGeometrie(original);
	}
}
