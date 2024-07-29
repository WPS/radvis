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

package de.wps.radvis.backend.konsistenz.regeln.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class KonsistenzregelVerletzungsDetails {

	private Geometry position;

	private Geometry originalGeometry;

	@Getter
	private String beschreibung;

	@Getter
	private String identity;

	/**
	 * Details einer KonsistenzregelVerletzung
	 *
	 * @param position
	 *     Hier wird das Icon in der Web-Oerfläche angezeigt/ Geometrie für WFS
	 * @param originalGeometry
	 *     (optional) zweite Geometrie, auf die sich der Fehler bezieht. Wird für ausgewählten Fehler zusätzlich
	 *     in der Web-Oberfläche angezeigt
	 * @param beschreibung
	 *     Beschreibungstext
	 * @param identity
	 *     (max 255 Zeichen) Ist dieser String für zwei Verletzungen derselben Regel gleich, so wird davon
	 *     ausgegangen, dass es dieselbe Verletzung ist
	 */
	public KonsistenzregelVerletzungsDetails(Point position, Geometry originalGeometry, String beschreibung,
		String identity) {
		require(position, notNullValue());
		require(beschreibung, notNullValue());
		require(identity, notNullValue());
		require(identity.length() <= 255);

		this.position = position;
		this.originalGeometry = originalGeometry;
		this.beschreibung = beschreibung;
		this.identity = identity;
	}

	public KonsistenzregelVerletzungsDetails(Point position, String beschreibung,
		String identity) {
		this(position, null, beschreibung, identity);
	}

	public Point getPosition() {
		return (Point) position;
	}

	// IntelliJ zeigt hier einen error an, jedoch compiliert und funktioniert es so
	// Fehler ist bekannt: https://youtrack.jetbrains.com/issue/IDEA-240844
	public Optional<Geometry> getOriginalGeometry() {
		return Optional.ofNullable(originalGeometry);
	}
}
