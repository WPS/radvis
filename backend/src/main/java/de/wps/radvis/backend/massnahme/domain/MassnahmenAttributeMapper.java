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

package de.wps.radvis.backend.massnahme.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.Name;

import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MassnahmenAttributeMapper {

	private final static String PREFIX_ANMERKUNG_BEMERKUNGE = "Anmerkung aus der Ursprungserfassung 2014 - 2016: "
		+ System.lineSeparator();
	private final static String PREFIX_REALISIERU = "Musterlösung (2014-2016): " + System.lineSeparator();
	private final static String PREFIX_ANM_NACH = "Anmerkung aus den Nachbefahrungen 2020-2021: " + System
		.lineSeparator();

	public List<String> mapKommentarTexte(Collection<Property> properties) {
		List<String> result = new ArrayList<>();

		// Anmerkung und Bemerkungen sollen zusammengefasst werden
		Optional<Property> anmerkung = properties.stream()
			.filter(property -> property.getName().toString().equals("Anmerkung")).findFirst();

		String anmerkungText = null;
		String bemerkungeText = null;

		if (anmerkung.isPresent()) {
			Object value = anmerkung.get().getValue();
			if (value != null && !value.toString().isBlank()) {
				anmerkungText = value.toString();
			}
		} else {
			// TODO: Löschen wenn wir das nicht mehr brauchen
			// log.warn("property Anmerkung nicht gesetzt!");
		}

		Optional<Property> bemerkunge = properties.stream()
			.filter(property -> property.getName().toString().equals("Bemerkunge")).findFirst();

		if (bemerkunge.isPresent()) {
			Object value = bemerkunge.get().getValue();
			if (value != null && !value.toString().isBlank()) {
				bemerkungeText = value.toString();
			}
		} else {
			log.warn("property Bemerkunge nicht gesetzt!");
		}

		if (anmerkungText != null || bemerkungeText != null) {
			String anmerkungBemerkungeText = Stream.of(anmerkungText, bemerkungeText).filter(Objects::nonNull)
				.collect(Collectors.joining(System.lineSeparator()));
			result.add(PREFIX_ANMERKUNG_BEMERKUNGE + anmerkungBemerkungeText);
		}

		// Realisieru
		Optional<Property> realisieru = properties.stream()
			.filter(property -> property.getName().toString().equals("Realisieru")).findFirst();

		if (realisieru.isPresent()) {
			Object value = realisieru.get().getValue();
			if (value != null && !value.toString().isBlank()) {
				result.add(PREFIX_REALISIERU + value);
			}
		} else {
			log.warn("property Realisieru nicht gesetzt!");
		}

		// Anm_Nach
		Optional<Property> anmNach = properties.stream()
			.filter(property -> property.getName().toString().equals("Anm_Nach")).findFirst();

		if (anmNach.isPresent()) {
			Object value = anmNach.get().getValue();
			if (value != null && !value.toString().isBlank()) {
				result.add(PREFIX_ANM_NACH + value);
			}
		} else {
			log.warn("property Anm_Nach nicht gesetzt!");
		}

		return result;
	}

	public Set<Netzklasse> mapNetzklassen(Collection<Property> properties) {
		Property netzklassenProperty = properties.stream()
			.filter(property -> property.getName().toString().equals("Kategorie")).findFirst().get();

		String value = netzklassenProperty.getValue().toString();

		switch (value) {
		case "0":
		case "0.0":
			// protokollieren
			return Set.of();
		case "1":
		case "1.0":
			return Set.of(Netzklasse.RADNETZ_FREIZEIT);
		case "3":
		case "3.0":
			return Set.of(Netzklasse.RADNETZ_ALLTAG);
		case "4":
		case "4.0":
			return Set.of(Netzklasse.RADNETZ_ZIELNETZ);
		case "6":
		case "6.0":
			return Set.of(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.KREISNETZ_ALLTAG);
		default:
			throw new RuntimeException("Kein Netzklassen-Mapping für: " + value);
		}
	}

	public String getBaulastString(Collection<Property> properties) {
		Property baulastProperty = properties.stream()
			.filter(property -> property.getName().toString().equals("Baulast")).findFirst()
			.orElseGet(() -> properties.stream()
				.filter(property -> property.getName().toString().equals("Baulasttr")).findFirst().get());
		return baulastProperty.getValue().toString();
	}

	public OrganisationsArt mapOrganisationsArtFuerBaulastZustaendiger(Collection<Property> properties) {

		String value = getBaulastString(properties).toLowerCase();

		if (value.contains("dritte") || value.contains("bund")) {
			// Hier differenzieren wir später noch nach dritte vs. bund, wenn wir die tatsächliche Orga bestimmen
			return OrganisationsArt.SONSTIGES;
		} else if (value.contains("land")) {
			return OrganisationsArt.BUNDESLAND;
		} else if (value.contains("kreis") || value.contains("kreid")) {
			return OrganisationsArt.KREIS;
		} else if (value.contains("gemeinde") || value.contains("stadt")) {
			return OrganisationsArt.GEMEINDE;
		}
		log.warn("Kein OrganisationsArtFuerBaulastZustaendiger-Mapping für: {}", value);
		return null;
	}

	public Prioritaet mapStartPrioritaet(Collection<Property> properties) {
		Property startPrioritaetProperty = properties.stream()
			.filter(property -> property.getName().toString().equals("Prio_S")).findFirst().get();
		String value = startPrioritaetProperty.getValue().toString();
		switch (value) {
		case "":
		case "0":
		case "0.0":
		case "4":
			return null;
		case "Sofortmaßnahme":
			return Prioritaet.of(1);
		case "1":
		case "1.0":
			return Prioritaet.of(2);
		case "2":
		case "2.0":
			return Prioritaet.of(3);
		case "Bei Bedarf":
			return Prioritaet.of(4);
		default:
			throw new RuntimeException("Kein StartPrioritaet-Mapping für: " + value);
		}
	}

	public Prioritaet mapZielPrioritaet(Collection<Property> properties) {
		Property startPrioritaetProperty = properties.stream()
			.filter(property -> property.getName().toString().equals("Prio_Z")).findFirst().get();
		String value = startPrioritaetProperty.getValue().toString();

		switch (value) {
		case "":
		case "0":
		case "0.0":
		case "Ausbau wäre wünschenswert, 2,40 m sind aber":
			return null;
		case "Sofortmaßnahme":
			return Prioritaet.of(5);
		case "3":
		case "3.0":
			return Prioritaet.of(6);
		case "4":
		case "4.0":
			return Prioritaet.of(7);
		case "Bei Bedarf":
			return Prioritaet.of(8);
		default:
			throw new RuntimeException("Kein ZielPrioritaet-Mapping für: " + value);
		}
	}

	public Umsetzungsstatus mapUmsetzungsstatus(Collection<Property> properties) {

		Property umsetzungsstatusProperty = properties.stream()
			.filter(property -> property.getName().toString().equals("IstZustand")).findFirst().get();

		Object value = umsetzungsstatusProperty.getValue();
		if (value == null) {
			return Umsetzungsstatus.IDEE;
		}

		switch (value.toString()) {
		case "1":
			return Umsetzungsstatus.UMGESETZT;
		case "2":
			return Umsetzungsstatus.UMSETZUNG;
		case "3":
		case "4":
			return Umsetzungsstatus.STORNIERT;
		default:
			return Umsetzungsstatus.IDEE;
		}
	}

	public Set<Massnahmenkategorie> mapStreckenStartKategorien(Collection<Property> properties) {
		return this.mapKategorien(this.getStreckenStartKategorienStrings(properties));
	}

	public Set<Massnahmenkategorie> mapStreckenZielKategorien(Collection<Property> properties) {
		return this.mapKategorien(this.getStreckenZielKategorienStrings(properties));
	}

	public Massnahmenkategorie mapPunktStartKategorie(Collection<Property> properties) {

		Property startKategorieProperty = properties.stream()
			.filter(property -> property.getName().toString().equals("MASSN_Star")).findFirst().get();
		return this.mapKategorie(startKategorieProperty.getValue().toString());
	}

	public Massnahmenkategorie mapPunktZielKategorie(Collection<Property> properties) {

		Property startKategorieProperty = properties.stream()
			.filter(property -> property.getName().toString().equals("MASSN_Ziel")).findFirst().get();
		return this.mapKategorie(startKategorieProperty.getValue().toString());
	}

	private Set<Massnahmenkategorie> mapKategorien(Collection<String> values) {
		return values.stream().map(this::mapKategorie).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private Set<String> getStreckenStartKategorienStrings(Collection<Property> properties) {
		return properties.stream().map(property -> {
			Name propertyName = property.getName();

			switch (propertyName.toString()) {
			case "Start_B":
			case "Start_M":
			case "Start_A":
			case "Start_N":
			case "START_SC":
			case "Start_RW":
			case "Start_F2":
			case "Start_F3":
				return property.getValue().toString();
			default:
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private Set<String> getStreckenZielKategorienStrings(Collection<Property> properties) {
		return properties.stream().map(property -> {
			Name propertyName = property.getName();

			switch (propertyName.toString()) {
			case "Ziel_B":
			case "Ziel_M":
			case "Ziel_A":
			case "Ziel_N":
			case "Ziel_SC":
			case "Ziel_LI":
			case "Ziel_AB":
			case "Ziel_BO":
			case "Ziel_RW":
			case "Ziel_F2":
			case "Ziel_F3":
				return property.getValue().toString();
			default:
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private Massnahmenkategorie mapKategorie(String value) {
		switch (value) {
		case "Anfang und Ende des Radweges sichern":
			return Massnahmenkategorie.ANFANG_UND_ENDE_RADWEG_SICHERN;
		case "Anpassung an bestehender Querungshilfe":
			return Massnahmenkategorie.ANPASSUNG_AN_BESTEHENDER_QUERUNGSHILFE;
		case "Anpassung einer LSA zur optimierten Führung des Radverkehrs (geringer Aufwand)":
		case "Anpassung einer LSA zur optimierten Querung des Fuß-/ Radverkehrs":
		case "Anpassung einer LSA zur optimierten Querung des Fuß - / Radverkehrs":
		case "Führung ist von Nebenanlage auf Fahrbahn zu verlegen":
		case "Anpassung einer LSA zur optimierten Führung des Radverkehrs (mittlerer Aufwand)":
		case "Anpassung einer LSA zur optimierten Führung des Radverkehrs (hoher Aufwand)":
			return Massnahmenkategorie.ANPASSUNG_EINER_LSA;
		case "Ausbau des bestehenden Weges nach Qualitätsstandard":
			return Massnahmenkategorie.AUSBAU_BESTEHENDEN_WEGES_NACH_QUALITAETSSTANDARD;
		case "Bau einer Fahrbahneinengung":
			return Massnahmenkategorie.BAU_EINER_FAHRBAHNEINENGUNG;
		case "Bau einer Unterführung":
		case "Bau einer Unterführung(geringer Aufwand)":
		case "Bau einer Unterführung(mittlerer Aufwand)":
		case "Bau einer Unterführung(hoher Aufwand)":
		case "Bau einer Unterführung (geringer Aufwand)":
		case "Bau einer Unterführung (mittlerer Aufwand)":
		case "Bau einer Unterführung (hoher Aufwand)":
			return Massnahmenkategorie.BAU_EINER_UNTERFUEHRUNG;
		case "Bau einer Überführung (geringer Aufwand)":
		case "Bau einer Überführung (hoher Aufwand)":
		case "Neubau eines Überführungsbauwerkes":
		case "Bau einer Überführung(geringer Aufwand)":
		case "Bau einer Überführung(mittlerer Aufwand)":
		case "Bau einer Überführung(hoher Aufwand)":
		case "Bau einer Überführung (mittlerer Aufwand)":
		case "Bau einer Überführung (Holzbrücke)":
		case "Bau einer Überführung":
		case "Bau einer Holzbrücke":
		case "Neubau einer ausreichend dimensionierten Überführung":
			return Massnahmenkategorie.BAU_EINER_UEBERFUEHRUNG;
		case "Belag abschnittsweise erneuern":
			return Massnahmenkategorie.BELAG_ABSCHNITTSWEISE_ERNEUERN;
		case "Beleuchtung herstellen":
			return Massnahmenkategorie.BELEUCHTUNG_HERSTELLEN;
		case "Benutzungspflicht für den Radverkehr aufheben":
		case "Benutzungspflicht für den Radverkehr aufheben, Reduzierung der zul. Höchstgeschwindigkeit prüfen":
			return Massnahmenkategorie.BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN;
		case "Benutzungspflicht für den Radverkehr aufheben, Radfahrer frei":
		case "Benutzungspflicht für den Radverkehr aufheben, Radfahrer frei, Reduzierung der zul. Höchstgeschwindigkeit prüfen":
		case "Benutzungspflicht für den Radverkehr aufheben, Radfahrer frei, Zweirichtungsführung aufheben":
			return Massnahmenkategorie.BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI;
		case "Bordabsenkungen herstellen (außerorts)":
			return Massnahmenkategorie.BORDABSENKUNGEN_HERSTELLEN_AUSSERORTS;
		case "Bordabsenkungen herstellen (innerorts)":
			return Massnahmenkategorie.BORDABSENKUNGEN_HERSTELLEN_INNERORTS;
		case "Demarkierung":
			return Massnahmenkategorie.DEMARKIERUNG;
		case "Ein Ende des Radweges sichern":
			return Massnahmenkategorie.EIN_ENDE_DES_RADWEGES_SICHERN;
		case "Einrichtung einer Fahrradstraße":
			return Massnahmenkategorie.EINRICHTUNG_FAHRRADSTRASSE;
		case "1 Furt erneuern":
		case "1 Furt erneuern, erledigt":
		case "1 Furten erneuern":
		case "2 Furten erneuern":
		case "3 Furten erneuern":
		case "4 Furten erneuern":
		case "5 Furten erneuern":
		case "6 Furten erneuern":
		case "7 Furten erneuern":
		case "8 Furten erneuern":
		case "9 Furten erneuern":
			return Massnahmenkategorie.FURTEN_ERNEUERN;
		case "1 Furt herstellen":
		case "1 Furten herstellen":
		case "10 Furten herstellen":
		case "11 Furten herstellen":
		case "12 Furten herstellen":
		case "13 Furten herstellen":
		case "2 Furten herstellen":
		case "3 Furten herstellen":
		case "4 Furten herstellen":
		case "5 Furten herstellen":
		case "6 Furten herstellen":
		case "7 Furten herstellen":
		case "8 Furten herstellen":
		case "StVO konforme Furt herstellen, kleine bauliche Anpassung":
			return Massnahmenkategorie.FURTEN_HERSTELLEN;
		case "Markierung Schutzstreifen (inkl. Neuordnung Straßenraum)":
			return Massnahmenkategorie.MARKIERUNG_SCHUTZSTREIFEN;
		case "Markierung Sicherheitstrennstreifen":
			return Massnahmenkategorie.MARKIERUNG_SICHERHEITSTRENNSTREIFEN;
		case "Neubau bauliche Anlage (3,50 m Breite) [z.B. landw. Weg]":
			return Massnahmenkategorie.NEUBAU_BAULICHE_ANLAGE;
		case "Neubau einer baulichen Radverkehrsanlage ≥ 2,00m":
			return Massnahmenkategorie.NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_200CM;
		case "Neubau einer baulichen Radverkehrsanlage ≥ 2,50m":
		case "Neubau einer baulichen Radverkehrsanlage = 2,50m":
			return Massnahmenkategorie.NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_250CM;
		case "Neubau eines Weges nach Qualitätsstandard":
		case "Neubau eines Verbindungsweges":
			return Massnahmenkategorie.NEUBAU_WEG_NACH_QUALITAETSSTANDARD;
		case "Oberfläche asphaltieren":
			return Massnahmenkategorie.OBERFLAECHE_ASPHALTIEREN;
		case "Punktuelle Deckenerneuerung":
			return Massnahmenkategorie.PUNKTUELLE_DECKENERNEUERUNG;
		case "Radfahrer frei":
			return Massnahmenkategorie.RADFAHRER_FREI;
		case "Absenkungen aufheben (innerorts)":
			return Massnahmenkategorie.RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_INNERORTS;
		case "Randmarkierung herstellen":
			return Massnahmenkategorie.RANDMARKIERUNG_HERSTELLEN;
		case "Reduzierung der zulässigen Höchstgeschwindigkeit":
		case "Reduzierung der zul. Höchstgeschwindigkeit prüfen":
			return Massnahmenkategorie.REDUZIERUNG_DER_VORGESCHRIEBENEN_HOECHSTGESCHWINDIGKEIT;
		case "Spurbahn mit durchgängigem Belag ersetzen":
			return Massnahmenkategorie.SPURBAHN_MIT_DURCHGAENGIGEM_BELAG_ERSETZEN;
		case "Strecke für Kfz-Verkehr sperren, Anlieger frei":
			return Massnahmenkategorie.STRECKE_FUER_KFZVERKEHR_SPERREN;
		case "Umwidmung in gemeinsamen Rad-/Gehweg":
		case "Umwidmung in gemeinsamen Geh-/Radweg":
		case "Umwidmung in gemeinsamen Geh-/Radweg (in beide Richtungen)":
			return Massnahmenkategorie.UMWIDMUNG_GEMEINSAMER_RADGEHWEG;
		case "Zweirichtungsführung aufheben":
			return Massnahmenkategorie.ZWEIRICHTUNGSFUEHRUNG_AUFHEBEN;
		case "Zweirichtungsführung aufheben, Reduzierung der zul. Höchstgeschwindigkeit prüfen":
			return Massnahmenkategorie.ZWEIRICHTUNGSFUEHRUNG_AUFHEBEN;
		case "Öffnung der Einbahnstraße für den Radverkehr in beide Richtungen":
			return Massnahmenkategorie.OEFFNUNG_EINBAHNSTRASSE_RADVERKEHR_BEIDE_RICHTUNGEN;
		case "Absenkungen aufheben (außerorts)":
			return Massnahmenkategorie.RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_AUSSERORTS;
		case "Markierung Schutzstreifen (beidseitig, inkl. Neuordnung Straßenraum)":
		case "Markierung Schutzstreifen (beidseitig, inkl. Neuordnung Straßenraum) und Reduzierung der vorgeschriebenen Höchstgeschwindigkeit (Kosten in Beschilderung!)":
		case "Markierung Schutzstreifen (einseitig, inkl. Neuordnung Straßenraum)":
		case "Markierung Schutzstreifen (einseitig, inkl. Neuordnung Straßenraum)_Umwidmung Gemeindestr. Tempo auf 30 reduziert":
			return Massnahmenkategorie.MARKIERUNG_SCHUTZSTREIFEN;
		case "Markierung Radfahrstreifen (beidseitig, inkl. Neuordnung Straßenraum)":
		case "Markierung Radfahrstreifen (einseitig, inkl. Neuordnung Straßenraum)":
			return Massnahmenkategorie.MARKIERUNG_RADFAHRSTREIFEN;
		case "Neumarkierung Radfahrstreifen (beidseitig, inkl. Neuordnung Straßenraum)":
		case "Neumarkierung Radfahrstreifen (einseitig, inkl. Neuordnung Straßenraum)":
			return Massnahmenkategorie.NEUMARKIERUNG_RADFAHRSTREIFEN;
		case "Neumarkierung Schutzstreifen (beidseitig, inkl. Neuordnung Straßenraum)":
		case "Neumarkierung Schutzstreifen (einseitig, inkl. Neuordnung Straßenraum)":
			return Massnahmenkategorie.NEUMARKIERUNG_SCHUTZSTREIFEN;
		case "Querungsmöglichkeit (geringer Aufwand)":
		case "Querungsmöglichkeit (großer Aufwand)":
		case "Querungsmöglichkeit (mittlerer Aufwand)":
			return Massnahmenkategorie.QUERUNGSMOEGLICHKEIT_HERSTELLEN;
		case "Anlagengerechte Beschilderung":
		case "Ortstafel versetzen":
		case "Radfahrer frei aufheben":
		case "Verkehrszeichen 357-50 StVO aufstellen":
		case "Zweirichtungsführung herstellen":
			return Massnahmenkategorie.SONSTIGE_STVO_BESCHILDERUNG;
		case "Aufweitung der Kurvenbereiche":
		case "Rückbau der B29 (alt) als Geh- und Radweg":
		case "Umbaumaßnahme an Bauwerk (geringer Aufwand)":
			return Massnahmenkategorie.SONSTIGE_BAUMASSNAHME;
		case "Belag nach Qualitätsstandard ersetzen":
			return Massnahmenkategorie.SONSTIGE_SANIERUNGSMASSNAHME;
		case "Markierungen zur Verdeutlichung der Führung des Radverkehrs":
			return Massnahmenkategorie.SONSTIGE_MARKIERUNG;

		// KNOTENMASSNAHMEN
		case "keine Maßnahme erforderlich":
			return null;
		case "StVO konforme Furt herstellen":
		case "StVO-konforme Furten an allen Zufahrten herstellen":
		case "markierungstechnische Maßnahme an LSA (geringer Aufwand)":
		case "markierungstechnische Maßnahme an LSA (hoher Aufwand)":
		case "markierungstechnische Maßnahmen an LSA (hoher Aufwand)":
		case "markierungstechnische Maßnahmen an LSA (geringer Aufwand)":
		case "Markierungstechnische Maßnahmen an LSA":
		case "Markierungstechnische Maßnahmen an LSA (geringer Aufwand)":
		case "Markierungstechnische Maßnahmen an LSA (hoher Aufwand)":
		case "StVO - konforme Furten an allen Zufahrten herstellen":
		case "Markierungstechnische Maßnahme":
		case "Markierungstechnische Maßnahmen am Knoten":
		case "Markierungstechnische Maßnahme am Knoten (hoher Aufwand)":
		case "Markierungstechnische Maßnahme am Knoten":
		case "Markierungstechnische Maßnahme am Knoten (geringer Aufwand)":
		case "Furtmarkierungen im Zuge der Industriestraße zur Erreichung des linksseitigen Radwegeangebotes.":
		case "Markierungstechnische Maßnahme (geringer Aufwand)":
		case "Aufweitung des Kurvenbereichs und Markierung einer Leitlinie":
		case "StVO-konforme Furt herstellen":
			return Massnahmenkategorie.MARKIERUNGSTECHNISCHE_MASSNAHME;
		case "Herstellung des Sichtfeldes":
		case "Bauliche Umgestaltung des Knotenpunktes (gering)":
		case "Bauliche Umgestaltung des Knotenpunktes (mittel)":
		case "Bauliche Umgestaltung des Knotenpunktes (hoch)":
		case "Bauliche Umgestaltung des Knotenpunktes mit Aufweitung des Radweges":
		case "Unebenheiten am Gleisübergang beseitigen":
		case "Unebenheiten an der Strecke beseitigen":
		case "Neuordnung Verkehrsraum":
		case "Umbaumaßnahme":
		case "Belag erneuern":
		case "Schiebestrecke beseitigen bzw. Rampe nach Qualitätsstandard ausbauen":
		case "Umbau des Knotens, Verschmälerung des Einmündungsbereichs":
		case "Aufstellen eines Spiegels":
		case "Strecke für den Radverkehr öffnen":
		case "Aufhebung der Benutzungspflicht":
		case "Bauliche Umgestaltung des Knotenpunktes":
			return Massnahmenkategorie.SONSTIGE_MASSNAHME_KNOTENPUNKT;
		case "Bau einer neuen LSA zur Querung des Fuß-/ Radverkehrs":
		case "Bau einer neuen LSA zur optimierten Führung des Radverkehrs(geringer Aufwand)":
		case "Bau einer neuen LSA zur optimierten Führung des Radverkehrs(mittlerer Aufwand)":
		case "Bau einer neuen LSA zur optimierten Führung des Radverkehrs(hoher Aufwand)":
		case "Bau einer neuen LSA zur optimierten Führung des Radverkehrs (geringer Aufwand)":
		case "Bau einer neuen LSA zur optimierten Führung des Radverkehrs (mittlerer Aufwand)":
		case "Bau einer neuen LSA zur optimierten Führung des Radverkehrs (hoher Aufwand)":
			return Massnahmenkategorie.BAU_EINER_NEUEN_LSA;
		case "Anlage einer Mittelinsel, Anordnung von Tempo 70":
		case "Querungshilfe(geringer Aufwand)":
		case "Querungshilfe(mittlerer Aufwand)":
		case "Querungshilfe(großer Aufwand)":
		case "Querungshilfe (geringer Aufwand)":
		case "Querungshilfe (mittlerer Aufwand)":
		case "Querungshilfe (großer Aufwand)":
			return Massnahmenkategorie.BAU_EINER_QUERUNGSHILFE;
		case "Reduzierung der vorgeschriebenen Höchstgeschwindigkeit":
		case "Änderung der Vorfahrt - Regelung(Verkehrszeichen)":
		case "Änderung der Vorfahrts-Regelung (Verkehrszeichen)":
		case "Schiebestrecke beseitigen":
		case "Vorgeschriebene Fahrtrichtung für den Radverkehr freigeben":
			return Massnahmenkategorie.AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDNUNG;
		case "Umbau einer Fahrbahneinengung":
			return Massnahmenkategorie.ANPASSUNG_EINER_FAHRBAHNEINENGUNG;
		case "Bau eines Kompakt - Kreisverkehrs(geringer Aufwand)":
		case "Bau eines Kompakt - Kreisverkehrs(mittlerer Aufwand)":
		case "Bau eines Kompakt - Kreisverkehrs(hoher Aufwand)":
		case "Bau eines Kompakt - Kreisverkehrs (geringer Aufwand)":
		case "Bau eines Kompakt - Kreisverkehrs (mittlerer Aufwand)":
		case "Bau eines Kompakt - Kreisverkehrs (hoher Aufwand)":
		case "Bau eines Kompakt-Kreisverkehrs (geringer Aufwand)":
		case "Bau eines Kompakt-Kreisverkehrs (mittlerer Aufwand)":
		case "Bau eines Kompakt-Kreisverkehrs (hoher Aufwand)":
		case "Bau eines Kompakt-Kreisverkehrs":
			return Massnahmenkategorie.BAU_KOMPAKT_TURBO_KREISVERKEHR;
		case "Bau eines Minikreisverkehrs":
		case "Bau eines Mini-Kreisverkehrs":
			return Massnahmenkategorie.BAU_MINIKREISVERKEHRS;
		case "Zufahrten verschmälern":
		case "Kreisfahrbahn verschmälern":
		case "Verdeutlichung der Führung des Radverkehrs":
		case "Anpassungen an Minikreisverkehr":
		case "StVO-konforme Furten an allen Zufahrten herstellen, Auframpung des Innenrings, regelkonfrome Umgestaltung der Zufahrten":
		case "StVO-konforme Furten an allen Zufahrten herstellen, Auframpung eines Innenrings, regelkonfome Umgestaltung des Zufahrten":
		case "Kreisfahrbahn verschmälern, StVO-konforme Furten an allen Zufahrten herstellen":
		case "Regelkonforme Umgestaltung des Kreisverkehrs":
		case "Zufahrten und Kreisfahrbahn verschmälern":
		case "Anpassungen am Kreisverkehr":
		case "StVO-konforme Furten an allen Zufahrten herstellen, Auframpung und Aufweitung des Innenrings, Zufahrten verschmälern":
		case "Zufahrten verschmälern; Innenring auframpen":
		case "Auframpung des Innenrings":
			return Massnahmenkategorie.ANPASSUNG_AN_BESTEHENDEN_KREISVERKEHR;
		case "Geländer erhöhen":
		case "Geländer erhöhen (kurze Brücke)":
		case "Geländer erhöhen (lange Brücke)":
		case "Umbaumaßnahme an Bauwerk":
		case "Umbaumaßnahme an Bauwerk(geringer Aufwand)":
		case "Umbaumaßnahme an Bauwerk(hoher Aufwand)":
		case "Umbaumaßnahme an Bauwerk (hoher Aufwand)":
		case "Markierungstechnische Maßnahme am Bauwerk":
		case "Umbaumaßnahme an Bauwerk, bzw. an beiden Zufahrten":
		case "Markierungen in Unterführung, Aufweitung/Begradiung der zuführenden Wege":
		case "Umbaumaßnahme an Bauwerk bzw. Zuwegung":
		case "Umbaumaßnahme an Bauwerk bzw. Straßenraum":
		case "Umbaumaßnahme an Bauwerk; Markierungen":
		case "Geländer erhöhen, Belag ausbessern":
			return Massnahmenkategorie.ANPASSUNG_AN_BAUWERK;
		case "Barriere sichern bzw. Prüfung auf Verzicht der Barriere":
		case "Poller sichern (Warnmarkierung)":
		case "Prüfung auf Verzicht des Pollers":
		case "Prüfung auf Verzicht der Schranke":
		case "Poller sichern (Warnmarkierung) bzw. Prüfung auf Verzicht des Pollers":
		case "Abbau bzw. Ersatz Barriere, ggf. Warnmarkierung":
		case "Drängelgitter sichern und Abstand vergößern":
		case "Prüfung auf Verzicht der Schranken":
		case "Schranken sichern":
		case "Standort Pfosten prüfen":
		case "Schranke sichern bzw. Prüfung auf Verzicht der Schranke":
		case "Poller verlegen und sichern (Warnmarkierung)":
		case "Mindestens Abstand vergrößern, Prüfung auf Verzicht des Drängelgitters":
		case "Abbau der Barriere":
		case "Abschrankung sichern (Warnmarkierung)":
		case "Schranke sichern":
		case "Abbau bzw. Ersatz des Schildermastes":
			return Massnahmenkategorie.BARRIERE_SICHERN_BZW_PRUEFUNG_AUF_VERZICHT;
		case "Abbau bzw. Ersatz Barriere":
			return Massnahmenkategorie.ABBAU_BZW_ERSATZ_BARRIERE;
		case "Aufweitung des Radweges mit direkt befahrbarer Zuleitung am Radweganfang und Sicherung der Querung am Radwegende":
		case "Überleitung des Radverkehrs vom Radweg auf die Fahrbahn":
			return Massnahmenkategorie.SONSTIGE_MASSNAHME_AN_RADWEGANFANG_ENDE;
		case "":
			return null;
		default:
			log.error("KMKM: " + value);
			throw new RuntimeException("Kein Massnahmenkatgeorie-Mapping für: " + value);
		}
	}
}
