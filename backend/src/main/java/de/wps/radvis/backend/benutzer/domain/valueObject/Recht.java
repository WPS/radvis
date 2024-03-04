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

package de.wps.radvis.backend.benutzer.domain.valueObject;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum Recht {

	// @formatter:off
	JOBS_AUSFUEHREN("Jobs ausführen"),
	LOGS_EINSEHEN("Zugriff auf Grafana"),

	ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN("Alle Benutzer und Organisationen bearbeiten"),
	BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN("Benutzer und Organisationen meines Verwaltungsbereichs bearbeiten"),
	EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN("Den Zuständigkeitsbereich meiner Organisation zum Zuständigkeitsbereich einer Organisation hinzufügen"),
	RADNETZ_ROUTENVERLEGUNGEN("RadNETZ Routenverlegungen "),
	BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT("Bearbeitung von Radwegstrecken des eigenen geographischen Zuständigkeit"),
	BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN("Bearbeitung von allen Radwegstrecken "),

	ALLE_ROLLEN("Alle Rollen vergeben"),
	KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER("Rollen \"KreiskoordinatorIn\", \"Radwege ErfasserIn - Kommune/Kreis\", \"Importe- VerantwortlicheR\" und \"Maßnahmen VerantworlicheR\" vergeben"),
	RADVERKEHRSBEAUFTRAGTER("Rolle \"RadNETZ-ErfasserIn Regierungsbezirk\" vergeben"),
	BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN("Rolle \"BearbeiterIn (VM)/RadNETZ-AdministratorIn\" vergeben"),
	BETRACHTER_EXTERNER_DIENSTLEISTER("Rolle \"BetrachterIn\", \"Externer Dienstleister\" vergeben"),

	MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN("Maßnahme im Zuständigkeitsbereich erfassen/bearbeiten/veröffentlichen"),
	ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN("Alle Maßnahmen erfassen/bearbeiten"),
	UMSETZUNGSSTANDSABFRAGEN_STARTEN("Umsetzungsstandsabfragen starten"),
	UMSETZUNGSSTANDSABFRAGEN_AUSWERTEN("Umsetzungsstandsabfragen auswerten"),

	RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN("Radrouten im eigenen Zuständigkeitsbereich erfassen/bearbeiten"),
	ALLE_RADROUTEN_ERFASSEN_BEARBEITEN("Alle Radrouten erfassen/bearbeiten"),

	FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN("Furten/Kreuzungen im eigenen Zuständigkeitsbereich erfassen/bearbeiten"),
	BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN("Barrieren im eigenen Zuständigkeitsbereich erfassen/bearbeiten"),

	SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN("Serviceangebote im eigenen Zuständigkeitsbereich erfassen/bearbeiten"),

	STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN("Streckendaten des eigenen Zuständigkeitsbereiches importieren"),
	MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN("Manuelles Matching zuordnen und bearbeiten"),
	ANPASSUNGSWUENSCHE_BEARBEITEN("Anpassungswünsche bearbeiten"),
	ANPASSUNGSWUENSCHE_ERFASSEN("Anpassungswünsche erfassen")
	;
	// @formatter:on

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}
}
