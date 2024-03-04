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
public enum Sicherung {

	KEINE_BODENMARKIERUNG("Keine Bodenmarkierung vorhanden"),
	BODENMARKIERUNG("Bodenmarkierung vorhanden"),
	RETROREFLEKTIERENDE_BODENMARKIERUNG("Retroreflektierende Bodenmarkierung vorhanden"),
	TAKTILE_BODENMARKIERUNG("Taktile Bodenmarkierung vorhanden"),
	BAULICHE_SICHERUNG("Bauliche Sicherung (z.B. durch Aufpflasterung vorhanden)"),
	BELEUCHTUNG("Beleuchtung vorhanden"),
	BELEUCHTUNG_UND_RETROREFLEKTIERENDE_BODENMARKIERUNG(
		"Beleuchtung und retroreflektierende Bodenmarkierung vorhanden");

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

}
