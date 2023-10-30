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

import com.graphhopper.routing.ev.EncodedValue;

/**
 * Dient dazu einen neuen Encoded-Value zu erzeugen. Pro Graphhopper Instanz sollte es für einen Encoded-Value Key
 * nur eine Instanz eines Encoded-Values geben. Das heißt auch, dass es pro mehrerer OSM-Keys einen Encoded-Value geben
 * kann. Beispiel: Für "breite:right" und "breite:left" würde es nur einen Encoded-Value "breite" geben, der aber Werte
 * seitenbezogen speichert.
 */
public interface CustomEncodedValueCreator {
	EncodedValue create();
}
