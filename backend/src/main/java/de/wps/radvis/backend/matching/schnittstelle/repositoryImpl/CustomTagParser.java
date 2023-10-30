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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import java.util.List;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

import de.wps.radvis.backend.matching.schnittstelle.encodedValues.CustomEncodedValueApplier;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.CustomEncodedValueCreator;

/**
 * Wird beim Einlesen einer PBF (sprich beim Bauen des Graphhopper-Caches) benutzt. Diese Klasse dient dazu einen
 * bestimmten OSM-Tag (spezifiziert durch den übergebenen key) in einen encoded-value zu speichern, welcher im Cache mit
 * abgespeichert wird. Beim Auslesen des Caches braucht man diesen Parser entsprechend dann nicht mehr.
 */
public class CustomTagParser<T> implements TagParser {
	private final String encodedValueKey;
	private final CustomEncodedValueCreator encodedValueCreator;
	private final CustomEncodedValueApplier encodedValueApplier;

	private EncodedValue encodedValue;

	public CustomTagParser(String encodedValueKey, CustomEncodedValueCreator encodedValueCreator,
		CustomEncodedValueApplier encodedValueApplier) {
		this.encodedValueKey = encodedValueKey;
		this.encodedValueCreator = encodedValueCreator;
		this.encodedValueApplier = encodedValueApplier;
	}

	@Override
	public void createEncodedValues(EncodedValueLookup lookup, List<EncodedValue> registerNewEncodedValue) {
		if (lookup.hasEncodedValue(encodedValueKey)) {
			encodedValue = lookup.getEncodedValue(encodedValueKey, EncodedValue.class);
		} else {
			encodedValue = encodedValueCreator.create();
			registerNewEncodedValue.add(encodedValue);
		}
	}

	@Override
	public IntsRef handleWayTags(IntsRef edgeFlags, ReaderWay way, boolean ferry, IntsRef relationFlags) {
		encodedValueApplier.apply(encodedValue, way, edgeFlags);
		return edgeFlags;
	}
}
