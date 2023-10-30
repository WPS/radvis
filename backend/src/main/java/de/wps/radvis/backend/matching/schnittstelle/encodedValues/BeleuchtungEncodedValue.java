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

import java.util.Arrays;
import java.util.List;

import com.graphhopper.routing.ev.StringEncodedValue;

import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;

public class BeleuchtungEncodedValue extends AbstractStringEncodedValue {
	public static StringEncodedValue create() {
		List<String> allItems = Arrays.stream(Beleuchtung.values()).map(Enum::name).toList();
		return create(allItems, getEncodedValueKey());
	}

	public static String getOsmKey() {
		return "beleuchtung";
	}

	public static String getEncodedValueKey() {
		return "beleuchtung";
	}

	public static CustomEncodedValueCreator getCreator() {
		return BeleuchtungEncodedValue::create;
	}

	public static CustomEncodedValueApplier getApplier() {
		return (encodedValue, way, osmTagKey) -> apply(encodedValue, way, getOsmKey(), osmTagKey);
	}
}
