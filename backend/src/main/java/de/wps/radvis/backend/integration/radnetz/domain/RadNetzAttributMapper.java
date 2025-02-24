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

package de.wps.radvis.backend.integration.radnetz.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.wps.radvis.backend.integration.netzbildung.domain.exception.AttributMappingException;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.AttributNichtImportiertException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute.GeschwindigkeitAttributeBuilder;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute.KantenAttributeBuilder;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import lombok.NonNull;

public class RadNetzAttributMapper {
	private final RadNetzNetzbildungProtokollService radNetzNetzbildungProtokollService;

	public RadNetzAttributMapper(@NonNull RadNetzNetzbildungProtokollService radNetzNetzbildungProtokollService) {
		this.radNetzNetzbildungProtokollService = radNetzNetzbildungProtokollService;
	}

	public KantenAttribute mapKantenAttribute(ImportedFeature feature) {
		KantenAttributeBuilder builder = KantenAttribute.builder();

		if (feature.hasAttribut("LICHT")) {
			try {
				builder.beleuchtung(mapBeleuchtung(feature.getAttribut("LICHT").toString()));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}

		if (feature.hasAttribut("ANM") || feature.hasAttribut("Anm_Nach")) {
			builder
				.kommentar(mapKommentar(feature.hasAttribut("ANM") ? feature.getAttribut("ANM").toString() : null,
					feature.hasAttribut("Anm_Nach") ? feature.getAttribut("Anm_Nach").toString() : null));

		}

		return builder.build();
	}

	private String normalizeRICHTUNG(String richtung) {
		if ("Zweirichtugnsverkehr".equalsIgnoreCase(richtung)) {
			return "Zweirichtungsverkehr";
		}
		return richtung;
	}

	private String normalizeSTRASSE(String strasse) {
		switch (strasse) {
		case "Vzul < 30 km/h":
			return "< 30 km/h";
		case "Vzul 30 km/h":
		case "Vzul 30":
			return "30 km/h";
		case "Vzul 40 oder 50 km/h":
			return "40 oder 50 km/h";
		case "Vzul 50 km/h":
			return "50 km/h";
		case "Vzul 60 oder 70 km/h":
			return "60 oder 70 km/h";
		case "Vzul 80 oder 90 km/h":
			return "80 oder 90 km/h";
		case "Vzul 100 km/h oder höher":
			return "100 km/h oder höher";
		}

		return strasse;
	}

	private String normalizeWEGEART(String wegeart) {
		switch (wegeart) {
		case "Weg mit Zusatzzeichen Anlieger frei":
		case "Weg mit Zusatzzeichen \"Anlieger frei\"":
		case "Weg mit Zusatzzeichen \"\"Anlieger frei\"\"":
			return "Weg mit Zusatzzeichen (Anlieger frei)";
		case "Gemeinsamer Geh-Radweg":
			return "Geh-/Radweg gemeinsam";
		case "Führung auf Fahrbahn (30 - 100 km/h)":
		case "Führung auf der Fahrbahn (30 - 100 km / h)":
			return "Führung auf der Fahrbahn (30 - 100 km/h)";
		case "Landw.-/Forstw./-Wasserw.-/ Anlieger frei Weg":
			return "Landw.-/Forstw.-/Wasserw.-/ Anlieger frei Weg";
		}

		return wegeart;
	}

	private String normalizeWEGETYP(String wegetyp) {
		switch (wegetyp) {
		case "Führung auf Fahrbahn (markiert)":
			return "Führung auf der Fahrbahn (markiert)";
		case "Führung auf Fahrbahn (unmarkiert)":
			return "Führung auf der Fahrbahn (unmarkiert)";
		case "Landw.-/Forstw./-Wasserw.-/ Anlieger frei Weg":
			return "Landw.-/Forstw.-/Wasserw.-/ Anlieger frei Weg";
		}

		return wegetyp;
	}

	private String normalizeKUTYP(String kutyp) {
		switch (kutyp) {
		case "Kompaktkreisverkehr":
			return "Kompaktkreisel";
		case "Minikreisverkehr (< 24 m)":
			return "Minikreisverkehr (<24m)";
		}
		return kutyp;
	}

	private String normalizeIstZustand(String istZustand) {
		if ("Zielstandard nicht erfülllt".equals(istZustand)) {
			return "Zielstandard nicht erfüllt";
		}
		if ("Startstandard nicht erreicht".equals(istZustand)) {
			return "Startstandard nicht erfüllt";
		}
		return istZustand;
	}

	private String normalizeBelagart(String belagart) {
		if ("Betonsteinpflaster/ Plattenbelag".equals(belagart)) {
			return "Betonsteinpflaster / Plattenbelag";
		}
		if ("ungebundene Decke (Kies/Split/Sand/Erde/Gras)".equals(belagart)) {
			return "Ungebundene Decke (Kies/Split/Sand/Erde/Gras)";
		}
		return belagart;
	}

	private Kommentar mapKommentar(String anm, String anm_nach) {
		StringBuilder kommentar = new StringBuilder();
		if (anm != null && !anm.isEmpty()) {
			kommentar.append(anm);
		}
		// wenn beides vorhanden und nicht leer, trenne mit newline
		if (anm != null && !anm.isEmpty() && anm_nach != null && !anm_nach.isEmpty()) {
			kommentar.append("\n");
		}
		if (anm_nach != null && !anm_nach.isEmpty()) {
			kommentar.append(anm_nach);
		}

		if (kommentar.toString().isEmpty()) {
			return null;
		} else {
			return Kommentar.of(kommentar.toString());
		}
	}

	private Set<Netzklasse> mapRadnetzklassen(String lrvn_kat)
		throws AttributMappingException {
		switch (lrvn_kat) {
		case "1.0":
		case "1":
			return Set.of(Netzklasse.RADNETZ_FREIZEIT);
		case "3.0":
		case "3":
			return Set.of(Netzklasse.RADNETZ_ALLTAG);
		case "4.0":
		case "4":
			return Set.of(Netzklasse.RADNETZ_ZIELNETZ);
		case "6.0":
		case "6":
			return Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT);
		}
		throw new AttributMappingException("Ungültiger Wert für 'LRVN_KAT': " + lrvn_kat);
	}

	private void handleAttributeMappingException(ImportedFeature feature, AttributMappingException e) {
		radNetzNetzbildungProtokollService.handle(
			new AttributNichtImportiertException(feature.getGeometrie(), e.getMessage()),
			RadNETZNetzbildungJob.class.getSimpleName());
	}

	public FahrtrichtungAttributGruppe mapFahrtrichtungAttributGruppe(ImportedFeature feature) {
		if (feature.hasAttribut("RICHTUNG")) {
			try {
				return new FahrtrichtungAttributGruppe(mapRichtung(
					normalizeRICHTUNG(
						feature.getAttribut("RICHTUNG").toString())),
					false);
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}
		return FahrtrichtungAttributGruppe.builder().build();
	}

	public GeschwindigkeitAttribute mapGeschwindigkeitAttribute(ImportedFeature feature) {
		GeschwindigkeitAttributeBuilder builder = GeschwindigkeitAttribute.builder();

		if (feature.hasAttribut("ORTSLAGE")) {
			try {
				builder.ortslage(mapOrtslage(feature.getAttribut("ORTSLAGE").toString()));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}

		if (feature.hasAttribut("STRASSE")) {
			try {
				builder.hoechstgeschwindigkeit(
					mapHoechstgeschwindigkeit(
						normalizeSTRASSE(feature.getAttribut("STRASSE").toString()),
						feature.hasAttribut("WEGEART") ? normalizeWEGEART(feature.getAttribut("WEGEART").toString())
							: null));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}

		return builder.build();
	}

	public ZustaendigkeitAttribute mapZustaendigkeitAttribute() {
		ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder builder = ZustaendigkeitAttribute
			.builder();

		return builder.build();
	}

	public FuehrungsformAttribute mapFuehrungsformAttribute(ImportedFeature feature) {
		FuehrungsformAttribute.FuehrungsformAttributeBuilder builder = FuehrungsformAttribute
			.builder();
		if (feature.hasAttribut("BELAGART")) {
			try {
				String belagart = feature.getAttribut("BELAGART").toString();
				builder.belagArt(mapBelagArt(normalizeBelagart(belagart)));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}
		if (feature.hasAttribut("BORD") && !feature.getAttribut("BORD").toString().isEmpty()) {
			try {
				builder.bordstein(mapBordstein(feature.getAttribut("BORD").toString()));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}
		String[] breitenHierarchie = { "BreiteRW", "BreitNA", "BreiteWG", "BreiteMF", "BREITEVA" };
		for (String attributName : breitenHierarchie) {
			if (feature.hasAttribut(attributName)) {
				try {
					Laenge breite = mapBreite(feature.getAttribut(attributName).toString(),
						attributName.equals("BREITEVA"));
					if (breite == null) {
						continue;
					}
					builder.breite(breite);
				} catch (AttributMappingException e) {
					handleAttributeMappingException(feature, e);
					continue;
				}
				break;
			}
		}

		if (feature.hasAttribut("WEGETYP") && feature.hasAttribut("WEGEART")) {
			try {
				builder.radverkehrsfuehrung(
					mapRadverkehrsfuehrung(
						normalizeWEGETYP(feature.getAttribut("WEGETYP").toString()),
						normalizeWEGEART(feature.getAttribut("WEGEART").toString()),
						feature.hasAttribut("STRASSE") ? normalizeSTRASSE(feature.getAttribut("STRASSE").toString())
							: null));
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		}

		return builder.build();
	}

	private Radverkehrsfuehrung mapRadverkehrsfuehrung(String wegetyp, String wegeart, String strasse)
		throws AttributMappingException {
		if (wegetyp.isEmpty() && wegeart.isEmpty()) {
			return Radverkehrsfuehrung.UNBEKANNT;
		}

		switch (wegetyp) {
		case "Selbstständig geführter Weg":
			switch (wegeart) {
			case "Radweg":
				return Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG;
			case "Geh-/Radweg getrennt":
				return Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG;
			case "Geh-/Radweg gemeinsam":
				return Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG;
			case "Gehweg (Rad frei)":
				return Radverkehrsfuehrung.GEHWEG_RAD_FREI_SELBSTSTAENDIG;
			case "Gem. Rad-/Gehweg mit Gehweg (Rad frei in Gegenrichtung)":
				return Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG;
			case "Weg an Wasserstraßen":
				return Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT;
			case "Landwirtschaftlicher Weg (selbstständig)":
				return Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG;
			case "Forstwirtschaftlicher Weg":
				return Radverkehrsfuehrung.BETRIEBSWEG_FORST;
			case "Führung auf der Fahrbahn (30 - 100 km/h)":
				return Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN;
			case "Weg mit Zusatzzeichen (Anlieger frei)":
				return Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER;
			case "Sonstiger Weg":
				return Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG;
			default:
				throw new AttributMappingException("Ungültige Kombination für '" + wegetyp
					+ "' wegetyp und '" + wegeart + "' wegeart für Radverkehrsfuehrung");
			}
		case "Straßenbegleitender Weg":
			switch (wegeart) {
			case "Radweg":
				return Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND;
			case "Geh-/Radweg getrennt":
				return Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND;
			case "Geh-/Radweg gemeinsam":
				return Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND;
			case "Gehweg (Rad frei)":
				return Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND;
			case "Gem. Rad-/Gehweg mit Gehweg (Rad frei in Gegenrichtung)":
				return Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND;
			case "Landwirtschaftlicher Weg (straßenbegleitend)":
				return Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND;
			case "Führung auf der Fahrbahn (30 - 100 km/h)":
				return Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN;
			case "Führung in Fahrradstraße":
				return Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE;
			case "Sonstiger Weg":
				return Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG;
			case "":
				return Radverkehrsfuehrung.UNBEKANNT;
			default:
				throw new AttributMappingException("Ungültige Kombination für '" + wegetyp
					+ "' wegetyp und '" + wegeart + "' wegeart für Radverkehrsfuehrung");
			}
		case "Führung auf der Fahrbahn (markiert)":
			switch (wegeart) {
			case "Schutzstreifen":
				return Radverkehrsfuehrung.SCHUTZSTREIFEN;
			case "Radfahrstreifen":
				return Radverkehrsfuehrung.RADFAHRSTREIFEN;
			case "Busspur (Rad frei)":
				return Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR;
			case "Führung auf der Fahrbahn (30 - 100 km/h)":
				return Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN;
			case "Führung in Fahrradstraße":
				return Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE;
			default:
				throw new AttributMappingException("Ungültige Kombination für '" + wegetyp
					+ "' wegetyp und '" + wegeart + "' wegeart für Radverkehrsfuehrung");
			}
		case "Führung auf der Fahrbahn (unmarkiert)":
			switch (wegeart) {
			case "Gehweg (Rad frei)":
				return Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND;
			case "Führung auf der Fahrbahn (30 - 100 km/h)":
			case "":
				return Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN;
			case "Führung in T30-Zone":
			case "Tempo-30-Zone":
				return Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE;
			case "Tempo-20-Zone":
			case "Führung in T20-Zone":
				return Radverkehrsfuehrung.FUEHRUNG_IN_T20_ZONE;
			case "Führung in Verkehrsberuhigter Bereich":
			case "Verkehrsberuhigter Bereich":
				return Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH;
			case "Führung in Fußg.-Zone (Rad frei)":
				return Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI;
			case "Führung in Fußg.-Zone (Rad zeitw. frei)":
				return Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI;
			case "Führung in Fahrradstraße":
				return Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE;
			case "Einbahnstraße (für Rad frei)":
				return Radverkehrsfuehrung.EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30;
			case "Forstwirtschaftlicher Weg":
				return Radverkehrsfuehrung.BETRIEBSWEG_FORST;
			case "Weg mit Zusatzzeichen (Anlieger frei)":
				return Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER;
			case "Sonstiger Weg":
				return Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG;
			case "Einbahnstraße (für Rad nicht freigegeben)":
				switch (strasse) {
				case "40 oder 50 km/h":
				case "Kfz nicht zugelassen":
					return Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30;
				case "30 km/h":
					return Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30;
				case "< 30 km/h": // korrigiert con Confluenc, wo nur "zul" statt "Vzul" steht
					return Radverkehrsfuehrung.EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30;
				default:
					throw new AttributMappingException("Ungültige Kombination für '" + wegetyp
						+ "' wegetyp und '" + wegeart + "' wegeart und '" + strasse
						+ "' strasse für Radverkehrsfuehrung");
				}
			default:
				throw new AttributMappingException("Ungültige Kombination für '" + wegetyp
					+ "' wegetyp und '" + wegeart + "' wegeart für Radverkehrsfuehrung");
			}
		case "Landw.-/Forstw.-/Wasserw.-/ Anlieger frei Weg":
			switch (wegeart) {
			case "Landwirtschaftlicher Weg (selbstständig)":
			case "Landw.-/Forstw.-/Wasserw.-/ Anlieger frei Weg":
				return Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG;
			case "Landwirtschaftlicher Weg (straßenbegleitend)":
			case "Straßenbegleitender Weg":
				return Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND;
			case "Forstwirtschaftlicher Weg":
				return Radverkehrsfuehrung.BETRIEBSWEG_FORST;
			case "Weg an Wasserstraßen":
				return Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT;
			case "Weg mit Zusatzzeichen (Anlieger frei)":
				return Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER;
			default:
				throw new AttributMappingException("Ungültige Kombination für '" + wegetyp
					+ "' wegetyp und '" + wegeart + "' wegeart für Radverkehrsfuehrung");
			}
		case "Sonstiger Weg":
			if ("Sonstiger Weg".equals(wegeart) || "".equals(wegeart)) {
				return Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG;
			} else {
				throw new AttributMappingException("Ungültige Kombination für '" + wegetyp
					+ "' wegetyp und '" + wegeart + "' wegeart für Radverkehrsfuehrung");
			}
		default:
			throw new AttributMappingException("Ungültige Kombination für '" + wegetyp
				+ "' wegetyp und '" + wegeart + "' wegeart für Radverkehrsfuehrung");
		}
	}

	private Laenge mapBreite(String breite, boolean istBreitenklasse) throws AttributMappingException {
		if (!istBreitenklasse) {
			if (breite.isEmpty()) {
				return null;
			}
			if (breite.equals("0")) {
				return null;
			}

			String fixedValue = breite.replace(',', '.');
			// Umwandlung von cm in meter

			double val = Double.parseDouble(fixedValue) / 100.;

			if (val > 0.) {
				return Laenge.of(val);
			} else {
				throw new AttributMappingException("Ungültige Wert '" + breite + "' für Breite");
			}

		} else {
			switch (breite) {
			case "< 1,30 m":
				return Laenge.of(1.29);
			case "1,30 m bis < 1,55 m":
			case "1,30 m bis < 1,55": // robustheit
				return Laenge.of(1.3);
			case "< 1,50 m":
				return Laenge.of(1.49);
			case "1,50 m bis < 1,60 m":
			case "1,50 m bis  < 1,60 m": // robustheit
				return Laenge.of(1.5);
			case "1,55 m oder größer":
				return Laenge.of(1.55);
			case "< 1,60 m":
				return Laenge.of(1.59);
			case "1,60 m bis < 2,00 m":
			case "1,60 m bis < 1,85 m":
				return Laenge.of(1.6);
			case "1,85 m bis < 2,00 m":
				return Laenge.of(1.85);
			case "< 2,00 m":
				return Laenge.of(1.99);
			case "2,00 m oder größer":
			case "2,00 m bis < 2,50 m":
				return Laenge.of(2.);
			case "< 2,50 m":
				return Laenge.of(2.49);
			case "2,50 m bis < 3,00 m":
				return Laenge.of(2.5);
			case "3,00 m oder größer":
			case "3,00 m bis < 4,00 m":
			case "3,00 m bis < 3,50 m":
				return Laenge.of(3.);
			case "3,50 m bis < 4,75 m":
				return Laenge.of(3.5);
			case "4,00 m oder größer":
			case "4,00m oder größer": // robustheit
				return Laenge.of(4.);
			case "4,75 m oder größer":
				return Laenge.of(4.75);
			case "":
				return null;
			default:
				throw new AttributMappingException("Ungültiger Wert '" + breite
					+ "' für Breitenklasse, für den kein Wert für das Attribut Breite ermittelt werden konnte");
			}
		}

	}

	private Bordstein mapBordstein(String bord) throws AttributMappingException {
		switch (bord) {
		case "Bordsteine auf Abschnitt überwiegend nicht abgesenkt":
			return Bordstein.KEINE_ABSENKUNG;
		case "Bordsteine auf Abschnitt überwiegend abgesenkt":
			return Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER;
		default:
			throw new AttributMappingException("Ungültiger Wert '" + bord + "' für Bordstein");
		}
	}

	public KnotenAttribute uebersetzePunktAttributeNachRadVis(ImportedFeature importedFeature) {
		KnotenAttribute.KnotenAttributeBuilder knotenAttributeBuilder = KnotenAttribute.builder();

		Kommentar kommentar = Kommentar.of("");

		Map<String, Object> featureAttribute = importedFeature.getAttribute();
		for (Map.Entry<String, Object> entry : featureAttribute.entrySet()) {

			String value = String.valueOf(entry.getValue());
			String key = entry.getKey();

			if (value == null || value.equals("null") || value.equals("")) {
				continue;
			}
			try {
				switch (key) {

				case "Anm_Nach": {
					kommentar = kommentar.getValue().isEmpty() ? Kommentar.of(value)
						: Kommentar.of(String.format("%s\n%s", value, kommentar));

					knotenAttributeBuilder.kommentar(kommentar);
					break;
				}
				case "Anm_NachB": {
					kommentar = kommentar.getValue().isEmpty() ? Kommentar.of(value)
						: Kommentar.of(String.format("%s\n%s", kommentar, value));

					knotenAttributeBuilder.kommentar(kommentar);
					break;
				}
				case "KDETAIL1":
				case "KDETAIL2":
				case "KUUTY":
					// wird separat hinterher gehandhabt
					break;

				case "FOTO1":
				case "FOTO2":
				case "FOTO3":
				case "AUDIO20": // Audio-Dateien werden nicht übernommen
				case "AUDIO9": // Audio-Dateien werden nicht übernommen
				case "BARRIERE":
				case "LANDKREIS":
				case "GEMEINDE":
				case "KREIS":
				case "the_geom": // geometrie ist woanders gespeichert
				case "LRVN_ID": // muss nicht übernommen werden
				case "MASSN_P": // Maßnahmenpaket ist keine Netzeigenschaft
				case "STATUS_WW": // zur Filterung verwendet
				case "Status_WW": // zur Filterung verwendet
				case "AufnTYP": // implizit aus Objekttyp ableitbar
				case "MASSNFIN":
				case "VORSCHL3":
					break;
				case "KUTYP": {
					String KUUTY = featureAttribute.get("KUUTY") == null ? null
						: featureAttribute.get("KUUTY").toString();

					knotenAttributeBuilder.knotenForm(
						mapKnotenFormFromKutyp(
							normalizeKUTYP(value), KUUTY));
					break;
				}
				default:
					// Sonstige Attribute werden ignoriert.
				}
			} catch (AttributMappingException e) {
				handleAttributeMappingException(importedFeature, e);
			}
		}

		String KUUTY = featureAttribute.get("KUUTY") == null ? null : featureAttribute.get("KUUTY").toString();
		String KDETAIL1 = featureAttribute.get("KDETAIL1") == null ? null : featureAttribute.get("KDETAIL1").toString();
		String KDETAIL2 = featureAttribute.get("KDETAIL2") == null ? null : featureAttribute.get("KDETAIL2").toString();

		knotenAttributeBuilder.zustandsbeschreibung(mapZustandsbeschreibung(KUUTY, KDETAIL1, KDETAIL2));
		return knotenAttributeBuilder.build();
	}

	private Zustandsbeschreibung mapZustandsbeschreibung(String KUUTY, String KDETAIL1,
		String KDETAIL2) {
		if (KUUTY != null && !KUUTY.equals("null") && !KUUTY.equals("")) {
			if (KUUTY.equalsIgnoreCase("Führung in Kreisfahrbahn")
				|| KUUTY.equalsIgnoreCase(
					"Führung nur über Kreisfahrbahn")
				|| KUUTY.equalsIgnoreCase(
					"Führung nur über Nebenanlage")
				|| KUUTY.equalsIgnoreCase(
					"Führung über Kreisfahrbahn und Nebenanlage")) {
				if (KDETAIL1 != null) {
					String beschreibung = "" + KDETAIL1;
					if (KDETAIL2 != null) {
						beschreibung += " (";
						beschreibung += KDETAIL2;
						beschreibung += ")";
					}
					return Zustandsbeschreibung.of(beschreibung);
				} else {
					return null;
				}
			} else {
				String beschreibung = "" + KUUTY;
				if (KDETAIL1 != null && !KDETAIL1.isEmpty()) {
					beschreibung += " - ";
					beschreibung += KDETAIL1;
				}
				return Zustandsbeschreibung.of(beschreibung);
			}
		} else {
			return null;
		}
	}

	private KnotenForm mapKnotenFormFromKutyp(String kutyp, String kuutyp) throws AttributMappingException {
		switch (kutyp) {
		case "Fuß-/Radfahrer LSA":
			return KnotenForm.FUSS_RADFAHRER_LSA;
		case "LSA-Knoten mit Radverkehrsführung über Fahrbahn (Mischverkehr)":
			return KnotenForm.LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_FAHRBAHN_MISCHVERKEHR;
		case "LSA-Knoten mit Radverkehrsführung über Fahrbahn (markierungstechn. geschützt)":
			return KnotenForm.LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_FAHRBAHN_MARKIERUNGSTECHN_GESCHUETZT;
		case "LSA-Knoten mit Radverkehrsführung über bauliche Nebenanlage":
			return KnotenForm.LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_BAULICHE_NEBENANLAGE;
		case "erweiterte Fuß-/Radfahrer LSA":
			return KnotenForm.ERWEITERTE_FUSS_RADFAHRER_LSA;
		case "Kompaktkreisel":
			switch (kuutyp) {
			case "Führung nur über Kreisfahrbahn":
			case "Führung in Kreisfahrbahn":
				return KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_KREISFAHRBAHN;
			case "Führung nur über Nebenanlage":
				return KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE;
			case "Führung über Kreisfahrbahn und Nebenanlage":
				return KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE;
			default:
				throw new AttributMappingException("Ungültiger Kombination an Werten für KnotenForm: '"
					+ kutyp + "' (kutyp) und '" + kuutyp + "' (kuutyp)");
			}
		case "Minikreisverkehr (<24m)":
			return KnotenForm.MINIKREISVERKEHR_NICHT_UEBERFAHRBAR;
		case "Unterführung":
		case "Unterführung / Tunnel":
		case "Unterführung/Tunnel":
			return KnotenForm.UNTERFUEHRUNG_TUNNEL;
		case "Überführung":
			return KnotenForm.UEBERFUEHRUNG;
		case "Abknickende Vorfahrt":
		case "abnkickende Vorfahrt":
			return KnotenForm.ABKNICKENDE_VORFAHRT_OHNE_LSA;
		case "Nicht-abknickende Vorfahrt":
			return KnotenForm.NICHT_ABKNICKENDE_VORFAHRT_OHNE_LSA;
		case "Fahrbahneinengung":
			return KnotenForm.FAHRBAHNEINENGUNG;
		case "Mittelinsel":
			return KnotenForm.MITTELINSEL_EINFACH;
		case "Querungsstelle ohne Sicherung":
			return KnotenForm.QUERUNGSSTELLE_OHNE_SICHERUNG;
		case "Großkreisel/Sonderform (mehrstreifig/Turbokreisel)":
			switch (kuutyp) {
			case "Führung nur über Kreisfahrbahn":
			case "Führung in Kreisfahrbahn":
				return KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_KREISFAHRBAHN;
			case "Führung nur über Nebenanlage":
				return KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE;
			case "Führung über Kreisfahrbahn und Nebenanlage":
				return KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE;
			default:
				throw new AttributMappingException("Ungültiger Kombination an Werten für KnotenForm: '"
					+ kutyp + "' (kutyp) und '" + kuutyp + "' (kuutyp)");
			}
		case "Querung Gleise":
			return KnotenForm.SONSTIGER_KNOTEN;
		default:
			throw new AttributMappingException("Ungültiger Wert für KnotenForm: " + kutyp);
		}
	}

	private KantenOrtslage mapOrtslage(String value) throws AttributMappingException {
		if (value.equalsIgnoreCase("innerorts")) {
			return KantenOrtslage.INNERORTS;
		}
		if (value.equalsIgnoreCase("außerorts")) {
			return KantenOrtslage.AUSSERORTS;
		}
		if (value.isEmpty()) {
			return null;
		}
		throw new AttributMappingException("Ungültiger Wert für Ortslage: " + value);
	}

	private Hoechstgeschwindigkeit mapHoechstgeschwindigkeit(String strasse, String wegeArt)
		throws AttributMappingException {
		if (strasse.equals("< 30 km/h")) {
			switch (wegeArt) {
			case "Führung in Verkehrsberuhigter Bereich":
			case "Verkehrsberuhigter Bereich":
			case "Führung in Fußg.-Zone (Rad frei)":
				return Hoechstgeschwindigkeit.MAX_9_KMH;
			case "Führung in T20-Zone":
			case "Tempo-20-Zone":
				return Hoechstgeschwindigkeit.MAX_20_KMH;
			case "Gem. Rad-/Gehweg mit Gehweg (Rad frei in Gegenrichtung)":
			case "Geh-/Radweg getrennt":
			case "Geh-/Radweg gemeinsam":
			case "Gehweg (Rad frei)":
			case "Schutzstreifen":
			case "Landwirtschaftlicher Weg (selbstständig)":
			case "Busspur (Rad frei)":
			case "Einbahnstraße (für Rad nicht freigegeben)":
			case "":
			case "Weg mit Zusatzzeichen \"\"Anlieger frei\"\"":
				return Hoechstgeschwindigkeit.UNBEKANNT;
			case "Weg mit Zusatzzeichen (Anlieger frei)":
			case "Führung in Fahrradstraße":
			case "Führung auf der Fahrbahn (30 - 100 km/h)":
			case "Führung in T30-Zone":
			case "Einbahnstraße (für Rad frei)":
				return Hoechstgeschwindigkeit.MAX_30_KMH;
			default:
				throw new AttributMappingException(
					"Ungültiger Wert '" + wegeArt + "' für Wegeart in Verbindung mit Geschwindigkeit < 30 km/h");
			}
		}

		switch (strasse) {
		case "Kfz nicht zugelassen":
			return Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN;
		case "30 km/h":
		case "Vzul < Vzul 30 km/h":
			return Hoechstgeschwindigkeit.MAX_30_KMH;
		case "40 oder 50 km/h":
		case "40 bis 50 km/h":
		case "50 km/h":
			return Hoechstgeschwindigkeit.MAX_50_KMH;
		case "60 oder 70 km/h":
		case "60 bis 70 km/h":
			return Hoechstgeschwindigkeit.MAX_70_KMH;
		case "80 oder 90 km/h":
			return Hoechstgeschwindigkeit.MAX_90_KMH;
		case "100 km/h oder höher":
			return Hoechstgeschwindigkeit.MAX_100_KMH;
		case "":
			return Hoechstgeschwindigkeit.UNBEKANNT;
		}

		throw new AttributMappingException("Ungültiger Wert '" + strasse + "' für Höchstgeschwindigkeit");
	}

	private Richtung mapRichtung(String value) throws AttributMappingException {
		switch (value.toLowerCase()) {
		case "zweirichtungsverkehr":
			return Richtung.BEIDE_RICHTUNGEN;
		case "einrichtungsverkehr":
			return Richtung.IN_RICHTUNG;
		case "":
			return Richtung.defaultWert();
		}
		throw new AttributMappingException("Ungültiger Wert für Richtung: " + value);
	}

	private BelagArt mapBelagArt(String value) throws AttributMappingException {
		switch (value) {
		case "Asphalt":
			return BelagArt.ASPHALT;
		case "Beton":
			return BelagArt.BETON;
		case "Natursteinpflaster":
			return BelagArt.NATURSTEINPFLASTER;
		case "Betonsteinpflaster / Plattenbelag":
		case "Betonsteinpflaster mit Fase":
			return BelagArt.BETONSTEINPFLASTER_PLATTENBELAG;
		case "Wassergebundene Decke":
			return BelagArt.WASSERGEBUNDENE_DECKE;
		case "Ungebundene Decke (Kies/Split/Sand/Erde/Gras)":
		case "Spurbahn":
			return BelagArt.UNGEBUNDENE_DECKE;
		case "Sonstiger Belag":
		case "Flickenartig wechselnde Beläge auf gesamtem Abschnitt":
			return BelagArt.SONSTIGER_BELAG;
		case "":
			return BelagArt.UNBEKANNT;
		}
		throw new AttributMappingException("Ungültiger Wert für BelagArt: " + value);
	}

	private Beleuchtung mapBeleuchtung(String value) throws AttributMappingException {
		if (value.equalsIgnoreCase("vorhanden")) {
			return Beleuchtung.VORHANDEN;
		}
		if (value.equalsIgnoreCase("nicht vorhanden")) {
			return Beleuchtung.NICHT_VORHANDEN;
		}
		if (value.isEmpty()) {
			return Beleuchtung.UNBEKANNT;
		}
		throw new AttributMappingException("Ungültiger Wert für Beleuchtung: " + value);
	}

	public Set<Netzklasse> mapNetzKlassen(ImportedFeature feature) {
		Set<Netzklasse> netzKlassen = new HashSet<>();

		if (feature.hasAttribut("LRVN_KAT")) {
			String kategoriestring = feature.getAttribut("LRVN_KAT").toString();
			try {
				netzKlassen = mapRadnetzklassen(kategoriestring);
			} catch (AttributMappingException e) {
				handleAttributeMappingException(feature, e);
			}
		} else {
			handleAttributeMappingException(feature, new AttributMappingException(
				"Fehlendes Quellattribut 'LRVN_KAT'. Netzklasse kann nicht bestimmt werden"));
		}

		return netzKlassen;
	}

	public Set<IstStandard> mapIstStandards(ImportedFeature feature) {
		if (feature.hasAttribut("IstZustand")) {
			String istZustand = normalizeIstZustand(feature.getAttribut("IstZustand").toString());
			switch (istZustand) {
			case "Zielstandard nicht erfüllt":
				return Set.of(IstStandard.STARTSTANDARD_RADNETZ);
			case "Start- und Zielstandard erfüllt":
				return Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.ZIELSTANDARD_RADNETZ);
			case "Startstandard nicht erfüllt":
			case "":
				return Collections.emptySet();
			default:
				handleAttributeMappingException(feature, new AttributMappingException(
					"Ungültiger Wert '" + istZustand + "' für Quellattribut IstZustand,"
						+ " kann nicht auf IstStandard gemappt werden."));
			}
		}
		return Collections.emptySet();
	}
}
