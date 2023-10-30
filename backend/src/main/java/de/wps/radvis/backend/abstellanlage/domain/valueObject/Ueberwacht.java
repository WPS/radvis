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

package de.wps.radvis.backend.abstellanlage.domain.valueObject;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum Ueberwacht {

	KEINE("Keine"),
	VIDEO("Video"),
	VOR_ORT_PERSONAL("Vor-Ort-Personal"),
	UNBEKANNT("Unbekannt");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public static Ueberwacht fromString(String displayText) {
		switch (displayText) {
		case "Keine":
			return Ueberwacht.KEINE;
		case "Video":
			return Ueberwacht.VIDEO;
		case "Vor-Ort-Personal":
			return Ueberwacht.VOR_ORT_PERSONAL;
		case "Unbekannt":
			return Ueberwacht.UNBEKANNT;
		}
		throw new RuntimeException("Überwacht " + displayText + " kann nicht gelesen werden");
	}

}
