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

package de.wps.radvis.backend.quellimport.common.domain;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureBuilder;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;

public class ImportedFeatureTestDataProvider {
	public static ImportedFeatureBuilder defaultRadNetzObject() {
		Map<String, Object> attribute = new HashMap<>();
		attribute.put("LRVN_KAT", 1);
		attribute.put("ORTSLAGE", "Außerorts");
		attribute.put("STRASSE", "Vzul 40 oder 50 km/h");
		attribute.put("RICHTUNG", "Zweirichtungsverkehr");
		attribute.put("WEGETYP", "Führung auf der Fahrbahn (unmarkiert)");
		attribute.put("BELAGART", "Wassergebundene Decke");
		attribute.put("LANDKREIS", "Klein Dorflingen");
		attribute.put("LAENGE", 1337d);
		attribute.put("LICHT", "Nicht vorhanden");

		return ImportedFeatureBuilder.empty().attribute(attribute)
			.fachId("Some id").point(new Coordinate(1, 1)).importDatum(LocalDateTime.of(2021, 3, 22, 14, 55))
			.art(Art.Strecke).quelle(QuellSystem.RadNETZ);
	}

	public static ImportedFeatureBuilder defaultWFSObject() {
		Map<String, Object> attribute = new HashMap<>();

		return ImportedFeatureBuilder.empty().attribute(attribute)
			.fachId("Some id").point(new Coordinate(1, 1)).importDatum(LocalDateTime.of(2021, 3, 22, 14, 55))
			.art(Art.Strecke).quelle(QuellSystem.DLM);
	}

	public static ImportedFeatureBuilder withLineString() {
		return ImportedFeatureTestDataProvider.defaultRadNetzObject()
			.lineString(new Coordinate(1, 1), new Coordinate(2, 2));
	}

	public static ImportedFeatureBuilder withLineString(Coordinate... coordinates) {
		return ImportedFeatureTestDataProvider.defaultRadNetzObject().lineString(coordinates);
	}

	public static ImportedFeatureBuilder empty() {
		return ImportedFeatureBuilder.empty()
			.fachId("Some id").point(new Coordinate(1, 1));
	}

	public static ImportedFeatureBuilder defaultRadNetzStrecke() {
		return ImportedFeatureBuilder.empty().art(Art.Strecke)
			.quelle(QuellSystem.RadNETZ).importDatum(LocalDateTime.of(2021, 3, 22, 15, 12))
			.fachId("Some id").lineString(new Coordinate(1, 1), new Coordinate(100, 100));
	}

	public static ImportedFeatureBuilder defaultRadNetzPunkt() {
		return ImportedFeatureBuilder.empty().art(Art.Strecke)
			.quelle(QuellSystem.RadNETZ).importDatum(LocalDateTime.of(2021, 3, 22, 15, 12))
			.fachId("Some id").point(new Coordinate(1, 1));
	}

}
