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
public enum KnotenformKategorie {

	// @formatter:off
	BAUWERK("Bauwerk"),
	KNOTEN_MIT_KREISVERKEHR("Knoten mit Kreisverkehr"),
	KNOTEN_MIT_LSA("Knoten mit LSA"),
	SIGNALISIERTE_QUERUNGSSTELLE("Signalisierte Querungsstelle"),
	QUERUNG_EINER_UEBERGEORDNETEN_STRASSE("Querung einer übergeordneten Straße"),
	KNOTEN_MIT_VORFAHRTSREGELNDEN_VERKEHRSZEICHEN("Knoten mit vorfahrtsregelnden Verkehrszeichen"),
	SONSTIGER_KNOTEN("Sonstiger Knoten"),

	// Kann entfernt werden, nachdem es im RadnetzMapping ersetzt wurde
	RADNETZ_LEGACY("RadNETZ legacy")
	;
	// @formatter:on

	@NonNull
	private String displayText;

	@Override
	public String toString() {
		return displayText;
	}
}
