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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public enum Massnahmenkategorie {

	// @formatter:off
//	Streckenmaßnahmenkategorien:

//	StVO Beschilderung / Änderung der verkehrsrechtlichen Anordnung
	EINRICHTUNG_MODALFILTER_PRUEFEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,"Einrichtung von Modalfiltern prüfen"),

	STRECKE_FUER_KFZVERKEHR_SPERREN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Strecke für Kfz-Verkehr sperren, Anlieger frei"),

	UMWIDMUNG_GEMEINSAMER_RADGEHWEG(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Umwidmung in gemeinsamen Rad-/Gehweg"),

	BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Benutzungspflicht für den Radverkehr aufheben"),

	BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Benutzungspflicht für den Radverkehr aufheben, Radfahrer frei"),

	BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI_REDUZIERUNG_HOECHSTGESCHWINDIGKEIT(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Benutzungspflicht für den Radverkehr aufheben, Reduzierung der vorgeschriebenen Höchstgeschwindigkeit"),

	BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI_REDUZIERUNG_HOECHSTGESCHWINDIGKEIT_RADFAHRER_FREI(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Benutzungspflicht für den Radverkehr aufheben, Reduzierung der vorgeschriebenen Höchstgeschwindigkeit, Radfahrer frei"),

	ZWEIRICHTUNGSFUEHRUNG_AUFHEBEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Zweirichtungsführung aufheben"),

	REDUZIERUNG_DER_VORGESCHRIEBENEN_HOECHSTGESCHWINDIGKEIT(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Reduzierung der vorgeschriebenen Höchstgeschwindigkeit"),

	OEFFNUNG_EINBAHNSTRASSE_RADVERKEHR_BEIDE_RICHTUNGEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Öffnung der Einbahnstraße für den Radverkehr in beide Richtungen"),


	EINRICHTUNG_FAHRRADSTRASSE(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Einrichtung einer Fahrradstraße"),

	RADFAHRER_FREI(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Radfahrer frei"),

	SONSTIGE_STVO_BESCHILDERUNG(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.STVO_BESCHILDERUNG,
		"Sonstige"),
//
//		Markierung


	MARKIERUNG_PIKTOGRAMMKETTE(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Piktogrammkette markieren"),
	
	NEUMARKIERUNG_PIKTOGRAMMKETTE(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Piktogrammkette anpassen"),
	
	NEUMARKIERUNG_SCHUTZSTREIFEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Neumarkierung Schutzstreifen (inkl. Neuordnung Straßenraum)"),

	NEUMARKIERUNG_RADFAHRSTREIFEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Neumarkierung Radfahrstreifen (inkl. Neuordnung Straßenraum)"),

	MARKIERUNG_SICHERHEITSTRENNSTREIFEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Markierung Sicherheitstrennstreifen"),

	MARKIERUNG_RADFAHRSTREIFEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Markierung Radfahrstreifen (inkl. Neuordnung Straßenraum)"),


	MARKIERUNG_SCHUTZSTREIFEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Markierung Schutzstreifen (inkl. Neuordnung Straßenraum)"),


	DEMARKIERUNG(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Demarkierung"),

	FURT_STVO_KONFORM(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Furt StVO konform herstellen oder umgestalten"),

	SONSTIGE_MARKIERUNG(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNG,
		"Sonstige"),
//
//		Ausbau
//


	AUSBAU_BESTEHENDEN_WEGES_NACH_QUALITAETSSTANDARD(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUSBAU_STRECKE,
		"Ausbau des bestehenden Weges nach Qualitätsstandard"),

	AUSBAU_BESTEHENDEN_WEGES_MIT_GERINGEREM_QUALITAETSSTANDARD(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUSBAU_STRECKE,
		"Ausbau des bestehenden Weges mit geringerem Qualitätsstandard"),
	
	STRASSENRAUM_BAULICH_NEU_ORDNEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUSBAU_STRECKE,
		"Straßenraum baulich neu ordnen"),

//		Neubau

	NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_160CM(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_STRECKE,
		"Neubau einer baulichen Radverkehrsanlage ≥ 1,60m (Fußverkehrsanlage muss ≥ 1,80m Breite beibehalten)"),

	NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_200CM(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_STRECKE,
		"Radweg neu bauen"),

	NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_250CM(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_STRECKE,
		"Neubau einer baulichen Radverkehrsanlage ≥ 2,50m"),

	NEUBAU_WEG_NACH_RADNETZ_QUALITAETSSTANDARD(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_STRECKE,
		"Neubau eines Weges nach RadNETZ-Qualitätsstandard (Stand 2016)"),

	NEUBAU_WEG_NACH_QUALITAETSSTANDARD(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_STRECKE,
		"Neubau eines Weges nach Qualitätsstandard"),

	NEUBAU_BAULICHE_ANLAGE(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_STRECKE,
		"Selbstständigen (Rad-)Weg neu bauen"),
//
//	Belag

	OBERFLAECHE_ASPHALTIEREN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BELAG,
		"Oberfläche asphaltieren"),

	SPURBAHN_MIT_DURCHGAENGIGEM_BELAG_ERSETZEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BELAG,
		"Spurbahn mit durchgängigem Belag ersetzen"),

	BELAG_ABSCHNITTSWEISE_ERNEUERN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BELAG,
		"Belag abschnittsweise erneuern"),

	PUNKTUELLE_DECKENERNEUERUNG(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BELAG,
		"Punktuelle Deckenerneuerung"),

	SONSTIGE_SANIERUNGSMASSNAHME(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BELAG,
		"Sonstige Sanierungsmaßnahme"),
	
	WASSERGEBUNDENE_DECKE_ANLEGEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BELAG,
		"Wassergebundene Decke herstellen"),
//
//	Sicherung Radweganfang/Ende

	ANFANG_UND_ENDE_RADWEG_SICHERN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.SICHERUNG_RADWEGANFANG_ENDE,
		"Anfang und Ende Radweg sichern"),

	EIN_ENDE_DES_RADWEGES_SICHERN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.SICHERUNG_RADWEGANFANG_ENDE,
		"Ein Ende des Radweges sichern"),

	QUERUNGSMOEGLICHKEIT_HERSTELLEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.SICHERUNG_RADWEGANFANG_ENDE,
		"Querungsmöglichkeit herstellen"),

	SONSTIGE_MASSNAHME_AN_RADWEGANFANG_ENDE(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.SICHERUNG_RADWEGANFANG_ENDE,
		"Sonstige Maßnahme an Radweganfang-/ende"),

//		Furten erneuern
//
	FURTEN_ERNEUERN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.FURTEN_ERNEUERN,
		"Furten erneuern"),

	// Furten herstellen
	FURTEN_HERSTELLEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.FURTEN_HERSTELLEN,
		"Furten herstellen"),
//
//	Herstellung Randmarkierung/Beleuchtung
//

	BELEUCHTUNG_HERSTELLEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.HERSTELLUNG_RANDMARKIERUNG_BELEUCHTUNG,
		"Beleuchtung herstellen"),

	RANDMARKIERUNG_HERSTELLEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.HERSTELLUNG_RANDMARKIERUNG_BELEUCHTUNG,
		"Randmarkierung herstellen"),
//
//	Herstellung von Absenkung

	RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_INNERORTS(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.HERSTELLUNG_ABSENKUNG,
		"Radwegabsenkungen an Grundstückszufahrten aufheben (innerorts)"),
	
	RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_AUSSERORTS(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.HERSTELLUNG_ABSENKUNG,
		"Radwegabsenkungen an Grundstückszufahrten aufheben (außerorts)"),

	RADWEG_AN_ZUFAHRTEN_NIVEAUGLEICH_AUSBAUEN(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.HERSTELLUNG_ABSENKUNG,
		"(Rad-)Weg an Zufahrten niveaugleich ausbauen"),

//
//	Absenken von Borden
	
	BORDABSENKUNGEN_HERSTELLEN_INNERORTS(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.ABSENKEN_VON_BORDEN,
		"Bordabsenkungen herstellen (innerorts)"),

	BORDABSENKUNGEN_HERSTELLEN_AUSSERORTS(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.ABSENKEN_VON_BORDEN,
		"Bordabsenkungen herstellen (außerorts)"),

	// Sonstige Baumaßnahme

	SONSTIGE_BAUMASSNAHME(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.SONSTIGE_BAUMASSNAHME,
		"Sonstige Baumaßnahme"),
	
	// Sonstige Streckenmaßnahme

	SONSTIGE_STRECKENMASSNAHME(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.SONSTIGE_STRECKENMASSNAHME,
		"Sonstige Streckenmaßnahme(n)"),

	// Planungen RSV/RVR
	
	MASSNAHME_RADSCHNELLVERBINDUNG(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.PLANUNGEN_RSV_PVR,
		"Maßnahmen im Zuge Radschnellverbindung"),

	MASSNAHME_RADVORRANGROUTE(
		MassnahmenkategorieArt.STRECKENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.PLANUNGEN_RSV_PVR,
		"Maßnahmen im Zuge Radvorrangroute"),
	
	
//	Knotenmaßnahmenkategorien:
//
//	Aus-/Umbau
//
	AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDNUNG(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Änderung der verkehrsrechtlichen Anordnung"),

	ANPASSUNG_AN_BESTEHENDEN_KREISVERKEHR(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Anpassung an bestehenden Kreisverkehr"),

	ANPASSUNG_AN_BESTEHENDER_QUERUNGSHILFE(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Anpassung an bestehender Querungshilfe"),

	ANPASSUNG_EINER_LSA(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Anpassung einer LSA"),

	ANPASSUNG_EINER_FAHRBAHNEINENGUNG(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Anpassung einer Fahrbahneinengung"),

	ANPASSUNG_AN_BAUWERK(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Anpassung an Bauwerk"),

	BELEUCHTUNG_HERSTELLEN_PUNKTUELL(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Kleinräumig Beleuchtung herstellen"),
	
	ANPASSUNG_RADWEGENDE_ANFANG_GESCHUETZT(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Radweganfang bzw. -ende anpassen"),
	
	ANPASSUNG_EINMUENDUNG_ZUFAHRT(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Einmündungs-/Zufahrtsbereich anpassen"),

	ERSATZBAUWERK_ERRICHTEN(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Ersatzbauwerk errichten"),

	ANPASSUNG_AN_BAUWERK_GERINGFUEGIG(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Bauwerk geringfügig anpassen"),

	AENDERUNG_DER_VORFAHRT_AM_KNOTENPUNKT(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.AUS_UMBAU_KNOTEN,
		"Vorfahrtsregelung ändern"),
	
//		Neubau

	BAU_EINER_FAHRBAHNEINENGUNG(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Bau einer Fahrbahneinengung"),

	BAU_EINER_FAHRBAHNEINENGUNG_UEBERGANG_ZWEIRICHTUNGSRADWEG(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Übergang Fahrbahn-Radweg sichern: Einengung"),

	BAU_EINER_NEUEN_LSA(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Überquerungsstelle sichern: LSA"),

	BAU_EINER_QUERUNGSHILFE(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Überquerungsstelle sichern: Querungshilfe"),

	BAU_EINER_QUERUNGSHILFE_UEBERGANG_ZWEIRICHTUNGSRADWEG(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Übergang Fahrbahn-Radweg sichern: Querungshilfe"),

	BAU_EINER_UEBERFUEHRUNG(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Bau einer Überführung"),

	BAU_EINER_UNTERFUEHRUNG(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Bau einer Unterführung"),

	BAU_KOMPAKT_TURBO_KREISVERKEHR(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Bau eines Kompakt-Kreisverkehrs / Turbo-Kreisverkehr"),

	BAU_MINIKREISVERKEHRS(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Bau eines Minikreisverkehrs"),

	FURT_PUNKTUELL_HERSTELLEN(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Furt punktuell herstellen"),

	BAU_RADWEGENDE_GESCHUETZT(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Übergang Fahrbahn-Radwegende baulich sichern"),

	BAU_RADWEGANFANG_GESCHUETZT(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.NEUBAU_KNOTEN,
		"Übergang Fahrbahn-Radweganfang baulich sichern"),

	MARKIERUNGSTECHNISCHE_MASSNAHME(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.MARKIERUNGSTECHNISCHE_MASSNAHME,
		"Markierungstechnische Maßnahme"),

	SONSTIGE_MASSNAHME_KNOTENPUNKT(
		MassnahmenkategorieArt.KNOTENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.SONSTIGE_MASSNAHME_KNOTENPUNKT,
		"Sonstige Maßnahme am Knotenpunkt"),


//	Barrierenmaßnahmenkategorien:

	AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDUNG(
		MassnahmenkategorieArt.BARRIERENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BARRIERENMASSNAHMENKATEGORIEN,
		"Änderung der verkehrsrechtlichen Anordung"),

	BARRIERE_SICHERN_BZW_PRUEFUNG_AUF_VERZICHT(
		MassnahmenkategorieArt.BARRIERENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BARRIERENMASSNAHMENKATEGORIEN,
		"Barriere sichern bzw. Prüfung auf Verzicht der Barriere"),

	SONSTIGE_MASSNAHME_AN_BARRIERE(
		MassnahmenkategorieArt.BARRIERENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BARRIERENMASSNAHMENKATEGORIEN,
		"Sonstige Maßnahme an Barriere"),

	ABBAU_BZW_ERSATZ_BARRIERE(
		MassnahmenkategorieArt.BARRIERENMASSNAHMENKATEGORIEN,
		MassnahmenOberkategorie.BARRIERENMASSNAHMENKATEGORIEN,
		"Abbau bzw. Ersatz Barriere");
	// @formatter:on

	@NonNull
	private final MassnahmenkategorieArt massnahmenkategorieArt;

	@NonNull
	@Getter
	private final MassnahmenOberkategorie massnahmenOberkategorie;

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public boolean isValidForKonzeptionsquelle(Konzeptionsquelle konzeptionsquelle) {
		require(konzeptionsquelle, notNullValue());

		if (konzeptionsquelle == Konzeptionsquelle.RADNETZ_MASSNAHME_2024) {
			return !kategorienNotAllowedForRadnetz2024.contains(this);
		}

		return true;
	}

	private static Set<Massnahmenkategorie> kategorienNotAllowedForRadnetz2024 = Set.of(
		Massnahmenkategorie.NEUBAU_WEG_NACH_RADNETZ_QUALITAETSSTANDARD,
		Massnahmenkategorie.NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_250CM,
		Massnahmenkategorie.NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_160CM,
		Massnahmenkategorie.FURT_STVO_KONFORM,
		Massnahmenkategorie.BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI_REDUZIERUNG_HOECHSTGESCHWINDIGKEIT_RADFAHRER_FREI,
		Massnahmenkategorie.ANFANG_UND_ENDE_RADWEG_SICHERN,
		Massnahmenkategorie.EIN_ENDE_DES_RADWEGES_SICHERN,
		Massnahmenkategorie.QUERUNGSMOEGLICHKEIT_HERSTELLEN,
		Massnahmenkategorie.SONSTIGE_MASSNAHME_AN_RADWEGANFANG_ENDE,
		Massnahmenkategorie.RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_INNERORTS,
		Massnahmenkategorie.RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_AUSSERORTS,
		Massnahmenkategorie.ABBAU_BZW_ERSATZ_BARRIERE,
		Massnahmenkategorie.AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDNUNG);
}
