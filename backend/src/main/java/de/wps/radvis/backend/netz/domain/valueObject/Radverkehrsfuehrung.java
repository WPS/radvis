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

import java.util.Set;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Radverkehrsfuehrung {

	// @formatter:off
	SONDERWEG_RADWEG_SELBSTSTAENDIG("Sonderweg Radweg (selbstständig)", RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	GEH_RADWEG_GETRENNT_SELBSTSTAENDIG("Geh-/Radweg getrennt (selbstständig)", RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG("Geh-/Radweg gemeinsam (selbstständig)", RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	GEHWEG_RAD_FREI_SELBSTSTAENDIG("Gehweg (Rad frei) (selbstständig)", RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG(
		"Gem. Rad-/Gehweg mit Gehweg (Rad frei in Gegenrichtung) (selbstständig)",
		RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG("Betriebsweg Landwirtschaft (selbstständig)",
		RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	BETRIEBSWEG_FORST("Betriebsweg Forst", RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	BETRIEBSWEG_WASSERWIRTSCHAFT("Betriebsweg Wasserwirtschaft", RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	SONSTIGER_BETRIEBSWEG("Sonstiger Betriebsweg", RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),
	OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER("Öffentliche Straße / Weg (mit Freigabe Anlieger)",
		RadverkehrsfuehrungKategorie.SELBSTSTAENDIG),

	SONDERWEG_RADWEG_STRASSENBEGLEITEND("Sonderweg Radweg (straßenbegleitend)",
		RadverkehrsfuehrungKategorie.STRASSENBEGLEITEND),
	GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND("Geh-/Radweg getrennt (straßenbegleitend)",
		RadverkehrsfuehrungKategorie.STRASSENBEGLEITEND),
	GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND("Geh-/Radweg gemeinsam (straßenbegleitend)",
		RadverkehrsfuehrungKategorie.STRASSENBEGLEITEND),
	GEHWEG_RAD_FREI_STRASSENBEGLEITEND("Gehweg (Rad frei) (straßenbegleitend)",
		RadverkehrsfuehrungKategorie.STRASSENBEGLEITEND),
	GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND(
		"Gem. Rad-/Gehweg mit Gehweg (Rad frei in Gegenrichtung) (straßenbegleitend)",
		RadverkehrsfuehrungKategorie.STRASSENBEGLEITEND),
	BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND("Betriebsweg Landwirtschaft (straßenbegleitend)",
		RadverkehrsfuehrungKategorie.STRASSENBEGLEITEND),

	PIKTOGRAMMKETTE("Piktogrammkette", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	SCHUTZSTREIFEN("Schutzstreifen", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	RADFAHRSTREIFEN("Radfahrstreifen", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR("Radfahrstreifen (mit Freigabe Busverkehr)",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR("Busfahrstreifen (mit Freigabe Radverkehr)",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	MEHRZWECKSTREIFEN("Mehrzweckstreifen", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN("Führung auf Fahrbahn (30 - 100 km/h) zweistreifige Fahrbahn",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN(
		"Führung auf Fahrbahn (30 - 100 km/h) vier- / mehrstreifige Fahrbahn", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_IN_T30_ZONE("Führung in T30-Zone", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_IN_T20_ZONE("Führung in T20-Zone", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH("Führung in Verkehrsberuhigter Bereich",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI("Führung in Fußg.-Zone (Rad frei)", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI("Führung in Fußg.-Zone (Rad zeitw. frei)",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI("Führung in Fußg.-Zone (Rad nicht frei)",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	BEGEGNUNBSZONE("Begegnungszone", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_IN_FAHRRADSTRASSE("Führung in Fahrradstraße", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	FUEHRUNG_IN_FAHRRADZONE("Führung in Fahrradzone", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30("Einbahnstraße (ohne Freigabe Radverkehr > 30 km/h)",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30("Einbahnstraße (ohne Freigabe Radverkehr bei ≤ 30 km/h)",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30("Einbahnstraße (mit Freigabe Radverkehr bei ≤ 30 km/h)",
		RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),
	SONSTIGE_STRASSE_WEG("Sonstige Straße / Weg", RadverkehrsfuehrungKategorie.FAHRBAHNFUEHRUNG),

	UNBEKANNT("Unbekannt", RadverkehrsfuehrungKategorie.UNBEKANNT);
	;
	// @formatter:on

	@NonNull
	private String displayText;

	@NonNull
	@Getter
	@Enumerated(EnumType.STRING)
	private RadverkehrsfuehrungKategorie radverkehrsfuehrungKategorie;

	@Override
	public String toString() {
		return displayText;
	}

	public Radverkehrsfuehrung nichtUnbekanntOrElse(Radverkehrsfuehrung otherValue) {
		return Radverkehrsfuehrung.UNBEKANNT.equals(this) ? otherValue : this;
	}

	public boolean widerspruchZu(Radverkehrsfuehrung other) {
		return !(this == Radverkehrsfuehrung.UNBEKANNT || other == Radverkehrsfuehrung.UNBEKANNT || this
			.equals(other));
	}

	public static Set<Radverkehrsfuehrung> mischverkehr() {
		return Set.of(
			//			Führung auf Fahrbahn (30 - 100 km/h) zweistreifige Fahrbahn
			Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN,
			//		Führung in T30-Zone
			Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE,
			//		Führung in T20-Zone
			Radverkehrsfuehrung.FUEHRUNG_IN_T20_ZONE,
			//		Führung in Verkehrsberuhigter Bereich
			Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH,
			//		Führung in Fußg.-Zone (Rad frei)
			Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI,
			//		Führung in Fußg.-Zone (Rad zeitw. frei)
			Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI,
			//		Führung in Fußg.-Zone (Rad nicht frei)
			Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI,
			//			Begegnungszone
			Radverkehrsfuehrung.BEGEGNUNBSZONE,
			//		Führung in Fahrradstraße
			Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE,
			//		Führung in Fahrradzone
			Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE,
			//		Einbahnstraße (ohne Freigabe Radverkehr > 30 km/h)
			Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30,
			//		Einbahnstraße (ohne Freigabe Radverkehr bei ≤ 30 km/h)
			Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,
			//		Einbahnstraße (mit Freigabe Radverkehr bei ≤ 30 km/h)
			Radverkehrsfuehrung.EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,
			//		Sonstige Straße / Weg
			Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG);
	}
}
