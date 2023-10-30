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
public enum AbstellanlagenStatus {

	GEPLANT("Geplant"),
	AKTIV("Aktiv"),
	AUSSER_BETRIEB("Außer Betrieb");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public static AbstellanlagenStatus fromString(String displayText) {
		switch (displayText) {
		case "Geplant":
			return AbstellanlagenStatus.GEPLANT;
		case "Aktiv":
			return AbstellanlagenStatus.AKTIV;
		case "Außer Betrieb":
			return AbstellanlagenStatus.AUSSER_BETRIEB;
		}
		throw new RuntimeException("Status " + displayText + " kann nicht gelesen werden");
	}
}
