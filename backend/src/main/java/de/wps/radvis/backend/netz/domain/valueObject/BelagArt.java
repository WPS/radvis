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

package de.wps.radvis.backend.netz.domain.valueObject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum BelagArt {
	//@formatter:off
	ASPHALT("Asphalt"),
	BETON("Beton"),
	NATURSTEINPFLASTER("Natursteinpflaster"),
	BETONSTEINPFLASTER_PLATTENBELAG("Betonsteinpflaster Plattenbelag"),
	WASSERGEBUNDENE_DECKE("Wassergebundene Decke"),
	UNGEBUNDENE_DECKE("Ungebundene Decke (Kies, Split, Sand, Erde, Gras, Spurbahn)"),
	SONSTIGER_BELAG("Sonstiger Belag"),
	WASSERGEBUNDENE_DECKE_MIT_GRUENSTREIFEN("Wassergebundene Decke mit Grünstreifen mittig"),
	ASPHALT_MIT_GRUENSTREIFEN("Asphalt mit Rasengittersteinen / Grünstreifen mittig"),
	BETON_MIT_GRUENSTREIFEN("Beton mit Rasengittersteinen / Grünstreifen mittig"),
	UNBEKANNT("Unbekannt");
	//@formatter:on

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return this.displayText;
	}

	public BelagArt nichtUnbekanntOrElse(BelagArt otherValue) {
		return !this.equals(BelagArt.UNBEKANNT) ? this : otherValue;
	}

	public boolean widerspruchZu(BelagArt other) {
		return !(this == BelagArt.UNBEKANNT || other == BelagArt.UNBEKANNT || this.equals(other));
	}
}