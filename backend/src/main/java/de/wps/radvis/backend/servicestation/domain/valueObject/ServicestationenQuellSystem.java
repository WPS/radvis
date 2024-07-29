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
public enum ServicestationenQuellSystem {

	RADVIS("RadVIS"),
	MOBIDATABW("MobiDataBW");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public static ServicestationenQuellSystem fromString(String displayText) {
		switch (displayText) {
		case "RadVIS":
			return ServicestationenQuellSystem.RADVIS;
		case "MobiDataBW":
			return ServicestationenQuellSystem.MOBIDATABW;
		}
		throw new RuntimeException("Quellsystem " + displayText + " wurde nicht erkannt.");
	}
}
