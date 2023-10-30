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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;

public class FractionIndexedLine {

	private final double[] fractions;

	public FractionIndexedLine(LineString lineString) {
		// Wir müssen den LS für die Berechnung in eine planare (i.e. längentreue) Projektion bringen,
		// da die Länge anonsten nicht mittels euklidischer Distanz berechnet werden kann. Die Berechnung
		// per orthodromic-distance bzw. great-circle-distance ist weniger performant
		LineString lineStringUtm32 = (LineString) CoordinateReferenceSystemConverterUtility.transformGeometry(
			lineString,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);

		Coordinate[] coordinates = lineStringUtm32.getCoordinates();
		int length = coordinates.length;
		fractions = new double[length];
		fractions[0] = .0;
		double currentDistance = .0;
		for (int i = 1; i < length; i++) {
			currentDistance += coordinates[i - 1].distance(coordinates[i]);
			fractions[i] = currentDistance / lineStringUtm32.getLength();
		}
	}

	public double getFractionAtIndex(int index) {
		require(index >= 0 && index < fractions.length, "Index must exists");
		return fractions[index];
	}
}
