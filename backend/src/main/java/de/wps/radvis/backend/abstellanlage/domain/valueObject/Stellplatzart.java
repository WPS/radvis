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
public enum Stellplatzart {

	VORDERRADANSCHLUSS("Vorderradanschluss"),
	ANLEHNBUEGEL("Anlehnbügel"),
	FAHRRADBOX("Fahrradbox"),
	DOPPELSTOECKIG("Doppelstöckig"),
	SAMMELANLAGE("Sammelanlage"),
	FAHRRADPARKHAUS("Fahrradparkhaus"),
	AUTOMATISCHES_PARKSYSTEM("Automatisches Parksystem"),
	SONSTIGE("Sonstige");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public static Stellplatzart fromString(String displayText) {
		switch (displayText) {
		case "Vorderradanschluss":
			return Stellplatzart.VORDERRADANSCHLUSS;
		case "Anlehnbügel":
			return Stellplatzart.ANLEHNBUEGEL;
		case "Fahrradbox":
			return Stellplatzart.FAHRRADBOX;
		case "Doppelstöckig":
			return Stellplatzart.DOPPELSTOECKIG;
		case "Sammelanlage":
			return Stellplatzart.SAMMELANLAGE;
		case "Fahrradparkhaus":
			return Stellplatzart.FAHRRADPARKHAUS;
		case "Automatisches Parksystem":
			return Stellplatzart.AUTOMATISCHES_PARKSYSTEM;
		case "Sonstige":
			return Stellplatzart.SONSTIGE;
		}
		throw new RuntimeException("Stellplatzart " + displayText + " kann nicht gelesen werden");
	}
}
