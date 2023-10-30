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

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum KnotenForm {

	// @formatter:off
	UEBERFUEHRUNG("Überführung", KnotenformKategorie.BAUWERK),
	UNTERFUEHRUNG_TUNNEL("Unterführung/Tunnel", KnotenformKategorie.BAUWERK),

	MINIKREISVERKEHR_24_M("Minikreisverkehr (< 24 m)", KnotenformKategorie.KNOTEN_MIT_KREISVERKEHR),
	KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_KREISFAHRBAHN("Kompaktkreisverkehr (Führung nur über Kreisfahrbahn)", KnotenformKategorie.KNOTEN_MIT_KREISVERKEHR),
	KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE("Kompaktkreisverkehr (Führung nur über Nebenanlage)", KnotenformKategorie.KNOTEN_MIT_KREISVERKEHR),
	KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE("Kompaktkreisverkehr (Führung über Kreisfahrbahn und Nebenanlage)", KnotenformKategorie.KNOTEN_MIT_KREISVERKEHR),
	GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_KREISFAHRBAHN("Großkreisel/Sonderform (mehrstreifig/Turbokreisel) (Führung nur über Kreisfahrbahn)", KnotenformKategorie.KNOTEN_MIT_KREISVERKEHR),
	GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE("Großkreisel/Sonderform (mehrstreifig/Turbokreisel) (Führung nur über Nebenanlage)", KnotenformKategorie.KNOTEN_MIT_KREISVERKEHR),
	GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE("Großkreisel/Sonderform (mehrstreifig/Turbokreisel) (Führung über Kreisfahrbahn und Nebenanlage)", KnotenformKategorie.KNOTEN_MIT_KREISVERKEHR),

	LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_BAULICHE_NEBENANLAGE("LSA-Knoten mit Radverkehrsführung über bauliche Nebenanlage", KnotenformKategorie.KNOTEN_MIT_LSA),
	LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_FAHRBAHN_MARKIERUNGSTECHN_GESCHUETZT("LSA-Knoten mit Radverkehrsführung über Fahrbahn (markierungstechn. geschützt)", KnotenformKategorie.KNOTEN_MIT_LSA),
	LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_FAHRBAHN_MISCHVERKEHR("LSA-Knoten mit Radverkehrsführung über Fahrbahn (Mischverkehr)", KnotenformKategorie.KNOTEN_MIT_LSA),

	ERWEITERTE_FUSS_RADFAHRER_LSA("erweiterte Fuß-/Radfahrer-LSA", KnotenformKategorie.SIGNALISIERTE_QUERUNGSSTELLE),
	FUSS_RADFAHRER_LSA("Fuß-/Radfahrer-LSA", KnotenformKategorie.SIGNALISIERTE_QUERUNGSSTELLE),

	MITTELINSEL("Mittelinsel", KnotenformKategorie.QUERUNG_EINER_UEBERGEORDNETEN_STRASSE),
	FAHRBAHNEINENGUNG("Fahrbahneinengung", KnotenformKategorie.QUERUNG_EINER_UEBERGEORDNETEN_STRASSE),
	QUERUNGSSTELLE_OHNE_SICHERUNG("Querungsstelle ohne Sicherung", KnotenformKategorie.QUERUNG_EINER_UEBERGEORDNETEN_STRASSE),
	FUSSGAENGERUEBERWEG("Fußgängerüberweg", KnotenformKategorie.QUERUNG_EINER_UEBERGEORDNETEN_STRASSE),

	RECHTS_VOR_LINKS_REGELUNG("rechts-vor-links Regelung", KnotenformKategorie.KNOTEN_MIT_VORFAHRTSREGELNDEN_VERKEHRSZEICHEN),
	ABKNICKENDE_VORFAHRT("Abknickende Vorfahrt", KnotenformKategorie.KNOTEN_MIT_VORFAHRTSREGELNDEN_VERKEHRSZEICHEN),
	NICHT_ABKNICKENDE_VORFAHRT("Nicht-abknickende Vorfahrt", KnotenformKategorie.KNOTEN_MIT_VORFAHRTSREGELNDEN_VERKEHRSZEICHEN),
	SONSTIGER_KNOTEN("Sonstiger Knoten", KnotenformKategorie.SONSTIGER_KNOTEN)
	;

	// @formatter:on

	@NonNull
	private String displayText;

	@NonNull
	@Getter
	@Enumerated(EnumType.STRING)
	private KnotenformKategorie knotenformKategorie;

	@Override
	public String toString() {
		return displayText;
	}

	public boolean isLSAKnotenForm() {
		return this.knotenformKategorie.equals(KnotenformKategorie.KNOTEN_MIT_LSA) ||
			this.knotenformKategorie.equals(KnotenformKategorie.SIGNALISIERTE_QUERUNGSSTELLE);
	}
}
