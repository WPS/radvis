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

package de.wps.radvis.backend.servicestation.domain.valueObject;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum ServicestationTyp {
	RADSERVICE_PUNKT_KLEIN("RadSERVICE-Punkt (klein)"),
	RADSERVICE_PUNKT_GROSS("RadSERVICE-Punkt (groß)"),
	SONSTIGER("Sonstiger");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public static ServicestationTyp fromString(String displayText) {
		switch (displayText) {
		case "RadSERVICE-Punkt (klein)":
			return ServicestationTyp.RADSERVICE_PUNKT_KLEIN;
		case "RadSERVICE-Punkt (groß)":
			return ServicestationTyp.RADSERVICE_PUNKT_GROSS;
		case "Sonstiger":
			return ServicestationTyp.SONSTIGER;
		}

		throw new RuntimeException("ServicestationTyp " + displayText + " kann nicht gelesen werden");
	}
}
