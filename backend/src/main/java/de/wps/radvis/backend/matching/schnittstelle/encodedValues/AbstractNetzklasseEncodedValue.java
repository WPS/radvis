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

import org.apache.commons.lang3.NotImplementedException;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;
import com.graphhopper.storage.IntsRef;

/**
 * Jede Netzklasse ist als separater Encoded-Value modelliert, da es pro Kante mehrere Netzklassen geben kann und
 * Graphhopper kein Konzept für Listen bei Encoded Values hat. EnumEncodedValues MÜSSEN (zumindest mit Graphhopper 4.0)
 * im Paket "com.graphhopper.routing.ev.*" liegen, womit man keine externen EnumEncodedValues definieren kann.
 */
public abstract class AbstractNetzklasseEncodedValue {

	/**
	 * Key unter dem die Netzklassen in der PBF gespeichert sind
	 */
	public static String getOsmKey() {
		// Muss in der konkreten Klasse überschrieben werden.
		throw new NotImplementedException(
			"Abstrakte Encoded-Values haben keinen eigenen OSM-Key. Stattdessen Konkrete Klasse nutzen.");
	}

	/**
	 * Key/Name des encoded-values, welcher auch im JSON des custom-models benutzt wird
	 */
	public static String getEncodedValueKey() {
		// Muss in der konkreten Klasse überschrieben werden.
		throw new NotImplementedException(
			"Abstrakte Encoded-Values haben keinen eigenen Key. Stattdessen Konkrete Klasse nutzen.");
	}

	public static boolean find(String value) {
		return Boolean.parseBoolean(value);
	}

	public static void apply(EncodedValue encodedValue, ReaderWay way, String osmKey, IntsRef intsRef) {
		if(way.hasTag(osmKey)) {
			boolean value = Boolean.parseBoolean(way.getTag(osmKey));
			((SimpleBooleanEncodedValue) encodedValue).setBool(false, intsRef, value);
		}
	}
}
