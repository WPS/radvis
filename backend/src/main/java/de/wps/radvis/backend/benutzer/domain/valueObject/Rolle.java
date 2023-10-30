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

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NonNull;

public enum Rolle {
	RADVIS_ADMINISTRATOR("RadVIS AdministratorIn",
		Recht.JOBS_AUSFUEHREN,
		Recht.LOGS_EINSEHEN,
		Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN,
		Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN,
		Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN,
		Recht.RADNETZ_ROUTENVERLEGUNGEN,
		Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,
		Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN,
		Recht.ALLE_ROLLEN,
		Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER,
		Recht.RADVERKEHRSBEAUFTRAGTER,
		Recht.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN,
		Recht.BETRACHTER_EXTERNER_DIENSTLEISTER,

		Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
		Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN,
		Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN,
		Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		//-------------------------------------------
		Recht.MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN,
		Recht.UMSETZUNGSSTANDSABFRAGEN_VERWALTEN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_ERFASSEN),

	KREISKOORDINATOREN("KreiskoordinatorIn",
		Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN,
		Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN,
		Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,

		Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER,
		Recht.BETRACHTER_EXTERNER_DIENSTLEISTER,

		Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
		Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_ERFASSEN),

	RADWEGE_ERFASSERIN("Radwege ErfasserIn - Kommune/Kreis/Regierungsbezirk",
		Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN,
		Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,
		Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
		Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_ERFASSEN),

	RADVERKEHRSBEAUFTRAGTER("RadverkehrsbeauftragteR Regierungsbezirk",
		Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN,
		Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN,
		Recht.RADNETZ_ROUTENVERLEGUNGEN,
		Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,

		Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER,
		Recht.RADVERKEHRSBEAUFTRAGTER,
		Recht.BETRACHTER_EXTERNER_DIENSTLEISTER,

		Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
		Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_ERFASSEN),

	BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN("BearbeiterIn (VM)/RadNETZ-AdministratorIn",
		Recht.RADNETZ_ROUTENVERLEGUNGEN,
		Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,
		Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
		Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.UMSETZUNGSSTANDSABFRAGEN_VERWALTEN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_ERFASSEN),

	RADNETZ_QUALITAETSSICHERIN("RadNETZ-QualitätssicherIn",
		Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
		Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.RADNETZ_ROUTENVERLEGUNGEN,
		Recht.MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_ERFASSEN),

	LGL_MITARBEITERIN("LGL-MitarbeiterIn",
		Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
		Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN),

	EXTERNER_DIENSTLEISTER("Externer Dienstleister",
		Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,
		Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN,

		Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
		Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
		Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_ERFASSEN),

	RADVIS_BETRACHTER("BetrachterIn"),

	RADROUTEN_BEARBEITERIN("RadroutenbearbeiterIn",
		Recht.ALLE_RADROUTEN_ERFASSEN_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
		Recht.ANPASSUNGSWUENSCHE_ERFASSEN);

	@NonNull
	private final String displayText;

	@Getter
	private final Recht[] rechte;

	Rolle(@NotNull String displayText, Recht... rechte) {
		this.displayText = displayText;
		this.rechte = rechte;
	}

	@Override
	public String toString() {
		return displayText;
	}

}