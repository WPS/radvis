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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.valid4j.Assertive.require;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * Modelliert eine lineare Referenzierung mittels des dezimalen Anteils (Fraction), von 0.0 bis 1.0,
 * an einem linearen gemessenen Feature bzw einer linearen Geometrie. Diese Klasse stellt eine solcher Markierungen
 * da - nicht den Bereich ab der / bis zur Markierung.
 */
@EqualsAndHashCode
public class LineareReferenz {
	private static final double PRECISION = 0.001;
	private static final DecimalFormat THREE_DECIMAL_PLACES = new DecimalFormat("#.###",
		DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	// private setter für Hibernate (das Feld wird sonst beim Lesen aus der DB nicht gesetzt)
	@JsonValue
	@Getter
	private final double abschnittsmarke;

	private LineareReferenz(double abschnittsmarke) {
		require(abschnittsmarke > 0.0 && abschnittsmarke < 1.0
			|| fractionEqual(abschnittsmarke, 1.0)
			|| fractionEqual(abschnittsmarke, 0.0));
		this.abschnittsmarke = abschnittsmarke;
	}

	public static LineareReferenz of(LineString projektionsziel, Coordinate punktZuProjizieren) {
		LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(projektionsziel);
		return LineareReferenz.of(lengthIndexedLine.project(punktZuProjizieren) / projektionsziel.getLength());
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static LineareReferenz of(double abschnittsmarke) {
		return new LineareReferenz(abschnittsmarke);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static LineareReferenz of(int value) {
		return new LineareReferenz(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static LineareReferenz of(@NonNull String value) {
		if (value.isEmpty()) {
			return null;
		}

		String fixedValue = value.replace(',', '.');
		return new LineareReferenz(Double.parseDouble(fixedValue));
	}

	public static boolean fractionEqual(LineareReferenz d1, LineareReferenz d2) {
		return Math.abs(d1.abschnittsmarke - d2.abschnittsmarke) < PRECISION;
	}

	public static boolean fractionEqual(Double d1, Double d2) {
		return Math.abs(d1 - d2) < PRECISION;
	}

	@Override
	public String toString() {
		return "LineareReferenz=" + THREE_DECIMAL_PLACES.format(abschnittsmarke);
	}
}
