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

package de.wps.radvis.backend.massnahme.domain.valueObject;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum MassnahmenOberkategorie {
	STVO_BESCHILDERUNG("StVO Beschilderung / Änderung der verkehrsrechtlichen Anordnung"),
	MARKIERUNG("Markierung"),
	AUSBAU_STRECKE("Ausbau Strecke"),
	NEUBAU_STRECKE("Neubau Strecke"),
	BELAG("Belag"),
	SICHERUNG_RADWEGANFANG_ENDE("Sicherung Radweganfang/Ende"),
	FURTEN_ERNEUERN("Furten erneuern"),
	FURTEN_HERSTELLEN("Furten herstellen"),
	HERSTELLUNG_RANDMARKIERUNG_BELEUCHTUNG("Herstellung Randmarkierung/Beleuchtung"),
	HERSTELLUNG_ABSENKUNG("Herstellung von Absenkung"),
	ABSENKEN_VON_BORDEN("Absenken von Borden"),
	AUS_UMBAU_KNOTEN("Aus-/Umbau Knoten"),
	NEUBAU_KNOTEN("Neubau Knoten"),
	MARKIERUNGSTECHNISCHE_MASSNAHME("Markierungstechnische Maßnahme"),
	SONSTIGE_MASSNAHME_KNOTENPUNKT("Sonstige Maßnahme am Knotenpunkt"),
	BARRIERENMASSNAHMENKATEGORIEN("Barrierenmaßnahmenkategorien"),
	SONSTIGE_BAUMASSNAHME("Sonstige Baumaßnahme"),
	SONSTIGE_STRECKENMASSNAHME("Sonstige Streckenmaßnahme"),
	PLANUNGEN_RSV_PVR("Planungen RSV/RVR"),
	;

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

}
