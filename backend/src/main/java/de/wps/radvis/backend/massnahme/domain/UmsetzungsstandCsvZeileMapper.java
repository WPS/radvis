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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import de.wps.radvis.backend.massnahme.domain.entity.UmsetzungsstandImportStatistik;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandCsvZeile;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UmsetzungsstandCsvZeileMapper {

	public boolean mapUmsetzungErfolgt(UmsetzungsstandCsvZeile umsetzungsstandCsvZeile,
		UmsetzungsstandImportStatistik statistik) {
		String umsetzungErfolgt = umsetzungsstandCsvZeile.getUmsetzungErfolgt();
		if (umsetzungErfolgt == null || umsetzungErfolgt.isBlank()) {
			return false;
		}
		if (umsetzungErfolgt.equals("Ja (weiter mit Frage 2)")) {
			return true;
		} else if (umsetzungErfolgt.equals("Nein (weiter mit Frage 8)")) {
			return false;
		} else {
			statistik.addUmsetzungErfolgtMappingFehler(umsetzungsstandCsvZeile.getDateiname(),
				umsetzungsstandCsvZeile.getMassnahmennummer());
			return false;
		}
	}

	public boolean mapUmsetzungGemaessMassnahmenblatt(UmsetzungsstandCsvZeile umsetzungsstandCsvZeile) {
		String umsetzungGemaessMassnahmenblatt = umsetzungsstandCsvZeile.getUmsetzungLautMassnahmenblatt();
		if (umsetzungGemaessMassnahmenblatt == null || umsetzungGemaessMassnahmenblatt.isBlank()) {
			return false;
		}
		if (umsetzungGemaessMassnahmenblatt.equals("Ja (weiter mit Frage 6)")) {
			return true;
		} else if (umsetzungGemaessMassnahmenblatt.equals("Nein (weiter mit Frage 3)")) {
			return false;
		} else {
			log.warn("Konnte \"UmsetzungGemaessMassnahmenblatt\" von Maßnahme "
				+ umsetzungsstandCsvZeile.getReferenceString()
				+ " nicht parsen. Eingabewert: \"" + umsetzungGemaessMassnahmenblatt + "\"");
			return false;
		}
	}

	public GrundFuerAbweichungZumMassnahmenblatt mapGrundFuerAbweichungZumMassnahmenblatt(
		UmsetzungsstandCsvZeile umsetzungsstandCsvZeile) {
		String grundString = umsetzungsstandCsvZeile.getGrundFuerAbweichungZumMassnahmenblatt();
		if (grundString == null || grundString.isBlank()) {
			return null;
		}
		return Stream.of(GrundFuerAbweichungZumMassnahmenblatt.values())
			.filter(g -> g.getDisplayText().equals(grundString)).findFirst().orElseGet(() -> {
				log.warn("Konnte \"GrundFuerAbweichungZumMassnahmenblatt\" von Maßnahme "
					+ umsetzungsstandCsvZeile.getReferenceString() + " nicht parsen. Eingabewert: \"" + grundString
					+ "\"");
				return null;
			});
	}

	public PruefungQualitaetsstandardsErfolgt mapPruefungQualitaetsstandardsErfolgt(
		UmsetzungsstandCsvZeile umsetzungsstandCsvZeile) {
		String geprueftString = umsetzungsstandCsvZeile.getEinhaltungQualitaetsstandardGeprueft();
		if (geprueftString == null || geprueftString.isBlank()) {
			return null;
		}
		return Stream.of(PruefungQualitaetsstandardsErfolgt.values())
			.filter(g -> g.getDisplayText().equals(geprueftString)).findFirst().orElseGet(() -> {
				log.warn("Konnte \"PruefungQualitaetsstandardsErfolgt\" von Maßnahme "
					+ umsetzungsstandCsvZeile.getReferenceString() + " nicht parsen. Eingabewert: \"" + geprueftString
					+ "\"");
				return null;
			});
	}

	public String mapBeschreibungAbweichenderMassnahme(UmsetzungsstandCsvZeile umsetzungsstandCsvZeile) {
		String beschreibung = umsetzungsstandCsvZeile.getBeschreibungAbweichenderMassnahme();
		return beschreibung == null ? null : beschreibung.substring(0, Math.min(beschreibung.length(), 3000));
	}

	public Long mapKostenDerMassnahme(UmsetzungsstandCsvZeile umsetzungsstandCsvZeile,
		UmsetzungsstandImportStatistik statistik) {
		String kostenString = umsetzungsstandCsvZeile.getKostenFuerMassnahme();
		if (kostenString == null || kostenString.isBlank()) {
			return null;
		}
		kostenString = kostenString.trim();
		// Entferne "ca." am Anfang
		kostenString = kostenString.replaceFirst("^ca\\.", "");
		// Entferne , - € und Leerzeichen am Ende
		kostenString = kostenString.replaceFirst("[\\, \\-€]*$", "");
		// Entferne Euro
		kostenString = kostenString.replace("Euro", "");
		kostenString = kostenString.trim();
		// Auf ganze Zahl abrunden
		if (kostenString.matches("^[0-9]+[\\,\\.]{1}[0-9]{1,2}$")) {
			kostenString = kostenString.replaceFirst("[\\,\\.]{1}[0-9]{1,2}$", "");
		}
		// Punkt als Zifferngruppierungszeichen entfernen
		if (kostenString.matches("^[0-9]{1,3}(\\.[0-9]{3})+$")) {
			kostenString = kostenString.replace(".", "");
		}
		try {
			return Long.parseLong(kostenString);
		} catch (NumberFormatException e) {
			statistik.addKostenMappingFehler(umsetzungsstandCsvZeile.getDateiname(),
				umsetzungsstandCsvZeile.getMassnahmennummer());
			return null;
		}
	}

	public GrundFuerNichtUmsetzungDerMassnahme mapGrundFuerNichtUmsetzungDerMassnahme(
		UmsetzungsstandCsvZeile umsetzungsstandCsvZeile) {
		String grundString = umsetzungsstandCsvZeile.getGrundFuerNichtUmsetzungDerMassnahme();
		if (grundString == null || grundString.isBlank()) {
			return null;
		}
		return Stream.of(GrundFuerNichtUmsetzungDerMassnahme.values())
			.filter(g -> g.getDisplayText().equals(grundString)).findFirst().orElseGet(() -> {
				log.warn("Konnte \"GrundFuerNichtUmsetzungDerMassnahme\" von Maßnahme "
					+ umsetzungsstandCsvZeile.getReferenceString() + " nicht parsen. Eingabewert: \"" + grundString
					+ "\"");
				return null;
			});
	}

	public String mapAnmerkung(UmsetzungsstandCsvZeile umsetzungsstandCsvZeile) {
		String anmerkung1 =
			umsetzungsstandCsvZeile.getAnmerkungsfeld() == null ? "" : umsetzungsstandCsvZeile.getAnmerkungsfeld();
		String anmerkung2 =
			umsetzungsstandCsvZeile.getAnmerkungsfeld2() == null ? "" : umsetzungsstandCsvZeile.getAnmerkungsfeld2();
		String anmerkung = anmerkung1 + " " + anmerkung2;
		anmerkung = anmerkung.trim();
		return anmerkung.substring(0, Math.min(anmerkung.length(), 3000));
	}

	public Optional<Verwaltungseinheit> mapKorrigierterBaulastZustaendiger(
		UmsetzungsstandCsvZeile umsetzungsstandCsvZeile,
		List<Verwaltungseinheit> organisationen) {
		final String zustaendigString = umsetzungsstandCsvZeile.getBaullasttraegerKorrigiert().trim();

		if (
			// 5534 Treffer
			zustaendigString.isEmpty()
				// 62 Treffer
				|| zustaendigString.equalsIgnoreCase("nein")) {
			return Optional.empty();
		}

		String gemeindeAusCSV = umsetzungsstandCsvZeile.getGemeinde().trim();
		String stadtLandkreisAusCSV = umsetzungsstandCsvZeile.getStadtLandkreis().trim();

		for (Verwaltungseinheit organisation : organisationen) {
			String organisationName = organisation.getName().toLowerCase();
			OrganisationsArt organisationsArt = organisation.getOrganisationsArt();

			// 278 Treffer
			if (
				// 234 Treffer
				(zustaendigString.equalsIgnoreCase("Gemeinde")
					||
					// 4 weitere Treffer
					(
						zustaendigString.toLowerCase().startsWith("gemeinde")
							&& zustaendigString.toLowerCase().endsWith("herabstufung")))
					&& organisationsArt.equals(OrganisationsArt.GEMEINDE)
					&& organisationName.equalsIgnoreCase(
					gemeindeAusCSV
						// 40 weitere Treffer
						.replace(", Stadt", ""))) {
				return Optional.of(organisation);
			}

			// 40 Treffer
			if (
				// 1 Treffer
				(zustaendigString.equalsIgnoreCase("Baulastträger Land")
					// 16 Treffer
					|| zustaendigString.equalsIgnoreCase("Bund")
					// 23 Treffer
					|| zustaendigString.equalsIgnoreCase("Land"))
					&& organisationsArt.equals(OrganisationsArt.BUNDESLAND)) {
				return Optional.of(organisation);
			}

			// 34 Treffer
			if (
				// 15 Treffer
				(zustaendigString.equalsIgnoreCase("Kreis")
					// 19 Treffer
					|| zustaendigString.equalsIgnoreCase("Stadt"))
					&& organisationName.equalsIgnoreCase(stadtLandkreisAusCSV)
					&& organisationsArt.equals(OrganisationsArt.KREIS)) {
				return Optional.of(organisation);
			}

			// 17 Treffer
			if (zustaendigString.equalsIgnoreCase("Forst-BW")
				&& organisationName.equalsIgnoreCase("Landesforstverwaltung BW")) {
				return Optional.of(organisation);
			}

			// 13 Treffer
			if (zustaendigString.startsWith("Gemeinde ")
				&& organisationName.equalsIgnoreCase(zustaendigString.replace("Gemeinde ", ""))) {
				return Optional.of(organisation);
			}

			// 12 Treffer
			if (zustaendigString.startsWith("Stadt ")
				&& organisationName.equalsIgnoreCase(zustaendigString.replace("Stadt ", ""))) {
				return Optional.of(organisation);
			}

			// 4 Treffer
			if (
				// 2 Treffer
				(zustaendigString.equalsIgnoreCase("Deutsche Bahn")
					// 1 Treffer
					|| zustaendigString.equalsIgnoreCase("Untereisesheim")
					// 1 Treffer
					|| zustaendigString.equalsIgnoreCase("Linkenheim-Hochstetten"))
					&& organisationName.equalsIgnoreCase(zustaendigString)) {
				return Optional.of(organisation);
			}

			// 1 Treffer
			if (zustaendigString.equalsIgnoreCase("FDS")
				&& organisationName.equalsIgnoreCase("Freudenstadt")
				&& organisationsArt.equals(OrganisationsArt.KREIS)) {
				return Optional.of(organisation);
			}
		}

		return Optional.empty();
	}
}
