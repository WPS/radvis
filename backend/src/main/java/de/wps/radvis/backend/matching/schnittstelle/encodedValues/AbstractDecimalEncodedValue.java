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

package de.wps.radvis.backend.matching.schnittstelle.encodedValues;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.UnsignedDecimalEncodedValue;
import com.graphhopper.storage.IntsRef;

// 20 bits = etwas über 1mio., was mit einem Faktor von 100 also einen maximalen Wert von ca. 10.000 erlaubt. Das ist
// für z.B. die Breite groß genug.
public abstract class AbstractDecimalEncodedValue {
	public static UnsignedDecimalEncodedValue create(String encodedValueKey) {
		return new UnsignedDecimalEncodedValue(encodedValueKey, 20, 100, false);
	}

	public static UnsignedDecimalEncodedValue createSeitenbezogen(String encodedValueKey) {
		return new UnsignedDecimalEncodedValue(encodedValueKey, 20, 100, true);
	}

	public static Double find(String value) {
		return Double.parseDouble(value);
	}

	public static void apply(EncodedValue encodedValue, ReaderWay way, String osmTagKey, IntsRef intsRef) {
		boolean seitenbezogen = encodedValue.isStoreTwoDirections();

		if (!seitenbezogen) {
			if (way.hasTag(osmTagKey)) {
				final Double wayTag = find(way.getTag(osmTagKey));
				((UnsignedDecimalEncodedValue) encodedValue).setDecimal(false, intsRef, wayTag);
			}
		} else {
			if (way.hasTag(osmTagKey)) {
				final Double wayTag = find(way.getTag(osmTagKey));
				((UnsignedDecimalEncodedValue) encodedValue).setDecimal(false, intsRef, wayTag);
				((UnsignedDecimalEncodedValue) encodedValue).setDecimal(true, intsRef, wayTag);
			}
			if (way.hasTag(osmTagKey + ":right")) {
				final Double wayTag = find(way.getTag(osmTagKey + ":right"));
				((UnsignedDecimalEncodedValue) encodedValue).setDecimal(false, intsRef, wayTag);
			}
			if (way.hasTag(osmTagKey + ":left")) {
				final Double wayTag = find(way.getTag(osmTagKey + ":left"));
				((UnsignedDecimalEncodedValue) encodedValue).setDecimal(true, intsRef, wayTag);
			}
		}
	}
}
