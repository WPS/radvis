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
public enum Hoechstgeschwindigkeit {

	KFZ_NICHT_ZUGELASSEN("Kfz nicht zugelassen"),
	MAX_9_KMH("9 km/h"),
	MAX_20_KMH("20 km/h"),
	MAX_30_KMH("30 km/h"),
	MAX_40_KMH("40 km/h"),
	MAX_50_KMH("50 km/h"),
	MAX_60_KMH("60 km/h"),
	MAX_70_KMH("70 km/h"),
	MAX_80_KMH("80 km/h"),
	MAX_90_KMH("90 km/h"),
	MAX_100_KMH("100 km/h"),
	UEBER_100_KMH("> 100 km/h"),
	UNBEKANNT("Unbekannt");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}
}