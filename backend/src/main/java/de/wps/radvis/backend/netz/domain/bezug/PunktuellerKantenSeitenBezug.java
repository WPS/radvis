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

package de.wps.radvis.backend.netz.domain.bezug;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class PunktuellerKantenSeitenBezug {

	@ManyToOne
	private Kante kante;

	// private setter für Hibernate (das Feld wird sonst beim Lesen aus der DB nicht gesetzt)
	@Setter(AccessLevel.PRIVATE)
	private LineareReferenz lineareReferenz;

	@Enumerated(EnumType.STRING)
	private Seitenbezug seitenbezug;

	public PunktuellerKantenSeitenBezug(Kante kante, LineareReferenz lineareReferenz, Seitenbezug seitenbezug) {
		require(kante, notNullValue());
		require(lineareReferenz, notNullValue());
		require(seitenbezug, notNullValue());
		this.kante = kante;
		this.lineareReferenz = lineareReferenz;
		this.seitenbezug = seitenbezug;
	}

	public Point getPointGeometry() {
		LineString linestring = kante.getGeometry();
		LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(linestring);
		GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
		return geometryFactory
			.createPoint(lengthIndexedLine.extractPoint(lineareReferenz.getAbschnittsmarke() * linestring.getLength()));
	}
}
