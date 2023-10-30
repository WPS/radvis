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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Map;
import java.util.Optional;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class MappedFeature {
	private final LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;
	private final LineString geometry;
	private final Map<String, Object> properties;

	// Beschreibt die geometrische Haendigkeit, also ob und wie weit das Feature rechts/links von der dazugehoerigen
	// Kante liegt.
	// Geometrische Haendigkeit wird nur beruecksichtigt, wenn kein attributierter Seitenbezug vorhanden ist
	private final Haendigkeit haendigkeit;

	// Wird zuvor aus den properties mitHilfe des Mappers berechnet
	// (aus dem Attribut "seite", welches nur beim RadVIS-Attribute-Import-Format vorhanden ist)
	// Das Feld ist null, wenn in den properties kein Attribut "seite" vorhanden ist
	private final Seitenbezug attributierterSeitenbezug;

	@Builder
	private MappedFeature(LineString lineString, Map<String, Object> properties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Haendigkeit haendigkeit, Seitenbezug attributierterSeitenbezug) {
		require(lineString, notNullValue());
		require(properties, notNullValue());
		require(linearReferenzierterAbschnitt, notNullValue());
		require(haendigkeit, notNullValue());

		this.geometry = lineString;
		this.properties = properties;
		this.linearReferenzierterAbschnitt = linearReferenzierterAbschnitt;
		this.haendigkeit = haendigkeit;
		this.attributierterSeitenbezug = attributierterSeitenbezug;
	}

	public static MappedFeature of(LineString lineString, Map<String, Object> properties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Haendigkeit haendigkeit, Seitenbezug seitenbezug) {
		return new MappedFeature(lineString, properties, linearReferenzierterAbschnitt, haendigkeit, seitenbezug);
	}

	public Optional<Seitenbezug> getAttributierterSeitenbezug() {
		return Optional.ofNullable(attributierterSeitenbezug);
	}
}