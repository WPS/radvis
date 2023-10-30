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

package de.wps.radvis.backend.barriere.domain.valueObject;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum VerbleibendeDurchfahrtsbreite {

	KEINE_DURCHFAHRT_MOEGLICH("keine Durchfahrt möglich"),
	KLEINER_130CM("< 1,30 m"),
	ZWISCHEN_130CM_160CM("1,30 m bis 1,60 m"),
	ZWISCHEN_160CM_250CM("1,60 m bis 2,50 m"),
	GROESSER_250CM("> 2,50 m");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

}
