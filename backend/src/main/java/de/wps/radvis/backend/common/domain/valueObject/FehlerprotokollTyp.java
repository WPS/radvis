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

package de.wps.radvis.backend.common.domain.valueObject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum FehlerprotokollTyp {
	DLM_REIMPORT_JOB_MASSNAHMEN("DLM-Reimport (Maßnahmen)"),
	DLM_REIMPORT_JOB_FAHRRADROUTEN("DLM-Reimport (Fahrradrouten)"),
	TOUBIZ_IMPORT_FAHRRADROUTEN("Toubiz-Routen Import"),
	TFIS_IMPORT_FAHRRADROUTEN("TFIS-Routen Import"),
	TFIS_IMPORT_LRFW("Landesradfernwege (Import aus TFIS)"),
	OSM_ABBILDUNG_RADNETZ("Abbildung vom RadNETZ auf das OSM-Netz"),
	OSM_ABBILDUNG_KREISNETZ("Abbildung vom Kreisnetz auf das OSM-Netz"),
	OSM_ABBILDUNG_KOMMUNALNETZ("Abbildung vom Kommunalnetz auf das OSM-Netz"),
	OSM_ABBILDUNG_SONSTIGE(
		"Abbildung vom unklassifizierten Netz, Radschnellverbindungen und Radvorrangrouten auf das OSM-Netz");

	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}
}
