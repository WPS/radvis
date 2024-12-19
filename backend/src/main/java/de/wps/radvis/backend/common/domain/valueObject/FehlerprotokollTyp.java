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
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum FehlerprotokollTyp {
	DLM_REIMPORT_JOB_MASSNAHMEN("DLM-Reimport (Maßnahmen)", 11.5),
	DLM_REIMPORT_JOB_FAHRRADROUTEN("DLM-Reimport (Fahrradrouten)", 11.5),
	DLM_REIMPORT_JOB_BARRIEREN("DLM-Reimport (Barrieren)", 11.5),
	DLM_REIMPORT_JOB_FURTEN_KREUZUNGEN("DLM-Reimport (Furten und Kreuzungen)", 11.5),
	ATTRIBUTLUECKEN_SCHLIESSEN("Attributlücken", 11.5),
	TOUBIZ_IMPORT_FAHRRADROUTEN("Toubiz-Routen Import", 9),
	TFIS_IMPORT_FAHRRADROUTEN("TFIS-Routen Import", 9),
	TFIS_IMPORT_LRFW("Landesradfernwege (Import aus TFIS)", 9),
	OSM_ABBILDUNG_RADNETZ("OSM-Ausleitung (RadNETZ)", 11.5),
	OSM_ABBILDUNG_KREISNETZ("OSM-Ausleitung (Kreisnetz)", 11.5),
	OSM_ABBILDUNG_KOMMUNALNETZ("OSM-Ausleitung (Kommunalnetz)", 11.5),
	OSM_ABBILDUNG_SONSTIGE("OSM-Ausleitung (Sonstige)", 11.5);

	private final String displayText;
	@Getter
	private final double minZoom;

	@Override
	public String toString() {
		return displayText;
	}
}
