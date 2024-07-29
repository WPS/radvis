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

package de.wps.radvis.backend.abstellanlage.domain;

import static org.valid4j.Assertive.require;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.springframework.data.util.Pair;
import org.springframework.security.access.AccessDeniedException;

import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage.AbstellanlageBuilder;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBeschreibung;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBetreiber;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenOrt;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenStatus;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenWeitereInformation;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlLademoeglichkeiten;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlSchliessfaecher;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlStellplaetze;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.ExterneAbstellanlagenId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProJahr;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProMonat;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProTag;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Groessenklasse;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.AbstractCsvImportService;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.exception.CsvImportException;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityNotFoundException;

public class AbstellanlageImportService extends AbstractCsvImportService<Abstellanlage, AbstellanlageBuilder> {

	private final AbstellanlageRepository abstellanlageRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final String baseUrl;

	public AbstellanlageImportService(AbstellanlageRepository abstellanlageRepository,
		VerwaltungseinheitService verwaltungseinheitService, ZustaendigkeitsService zustaendigkeitsService,
		String baseUrl) {
		super(Abstellanlage.CsvHeader.ALL, Abstellanlage.CsvHeader.RAD_VIS_ID);
		this.abstellanlageRepository = abstellanlageRepository;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.baseUrl = baseUrl;
	}

	@Override
	protected String createUrl(Abstellanlage savedAbstellanlage) {
		return baseUrl + FrontendLinks.abstellanlageDetails(savedAbstellanlage.getId());
	}

	public CsvData importCsv(CsvData csvData, Benutzer benutzer) throws CsvImportException {
		if (!benutzer.getRechte().contains(Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Abstellanlagen zu erstellen oder zu bearbeiten.");
		}

		Verwaltungseinheit obersteVerwaltungseinheit = verwaltungseinheitService.getObersteGebietskoerperschaft()
			.orElseThrow(EntityNotFoundException::new);
		PreparedGeometry boundingArea = PreparedGeometryFactory.prepare(obersteVerwaltungseinheit.getBereich().get());

		return super.importCsv(csvData,
			(abstellanlageBuilder, row) -> this.mapAttributes(abstellanlageBuilder, boundingArea, row, benutzer));
	}

	private Abstellanlage mapAttributes(AbstellanlageBuilder abstellanlageBuilder, PreparedGeometry boundingArea,
		Map<String, String> row,
		Benutzer benutzer)
		throws AbstellanlageAttributMappingException {

		// QUELLSYSTEM
		AbstellanlagenQuellSystem quellSystem;
		try {
			quellSystem = AbstellanlagenQuellSystem.fromString(
				row.get(Abstellanlage.CsvHeader.QUELLSYSTEM));
		} catch (Exception exception) {
			throw new AbstellanlageAttributMappingException(
				"Quellsystem (" + row.get(Abstellanlage.CsvHeader.QUELLSYSTEM)
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(AbstellanlagenQuellSystem.values())
						.map(AbstellanlagenQuellSystem::toString).collect(Collectors.joining(", ")));
		}
		if (quellSystem.equals(AbstellanlagenQuellSystem.MOBIDATABW)) {
			throw new AbstellanlageAttributMappingException(
				"Fahrradabstellanlagen mit dem Quellsystem MobiDataBW können nicht über den manuellen CSV-Import importiert werden.");
		} else {
			abstellanlageBuilder.quellSystem(quellSystem);
		}

		// GEOMETRIE
		try {
			Point point = extractPositionInternal(row);
			if (!boundingArea.contains(point)) {
				throw new AbstellanlageAttributMappingException(
					"Position " + point
						+ " liegt nicht im Bereich, der von der Anwendung verwaltet wird. Bitte prüfen Sie die Koordinaten.");
			}
			if (!zustaendigkeitsService.istImZustaendigkeitsbereich(point, benutzer)) {
				throw new AccessDeniedException(
					"Sie haben nicht die Berechtigung Abstellanlagen in diesem Verwaltungsbereich anzulegen oder zu bearbeiten.");
			}
			abstellanlageBuilder.geometrie(point);
		} catch (Exception exception) {
			throw new AbstellanlageAttributMappingException(
				"Geometrie (" + row.get(Abstellanlage.CsvHeader.POSITION_X_UTM32_N) + ";" + row.get(
					Abstellanlage.CsvHeader.POSITION_Y_UTM32_N) + ") kann nicht gelesen werden. Details: "
					+ exception.getMessage());
		}

		// BETREIBER
		if (row.get(Abstellanlage.CsvHeader.BETREIBER).isEmpty()
			|| row.get(Abstellanlage.CsvHeader.BETREIBER).length() > AbstellanlagenBetreiber.MAX_LENGTH) {
			throw new AbstellanlageAttributMappingException(
				String.format("Betreiber (%s) muss angegeben sein und darf max. %d Zeichen enthalten",
					row.get(Abstellanlage.CsvHeader.BETREIBER), AbstellanlagenBetreiber.MAX_LENGTH));
		}
		abstellanlageBuilder.betreiber(AbstellanlagenBetreiber.of(row.get(Abstellanlage.CsvHeader.BETREIBER)));

		// EXTERNE_ID
		String externdeIdString = row.get(Abstellanlage.CsvHeader.EXTERNE_ID);
		if (externdeIdString.isEmpty()) {
			abstellanlageBuilder.externeId(null);
		} else if (externdeIdString.length() > ExterneAbstellanlagenId.MAX_LENGTH) {
			throw new AbstellanlageAttributMappingException(
				String.format("Externe ID (%s) darf max. %d Zeichen enthalten",
					row.get(Abstellanlage.CsvHeader.EXTERNE_ID), ExterneAbstellanlagenId.MAX_LENGTH));
		} else {
			abstellanlageBuilder.externeId(ExterneAbstellanlagenId.of(externdeIdString));
		}

		// ZUSTANDIG_IN_RAD_VIS
		final String zustaendigBezeichnung = row.get(Abstellanlage.CsvHeader.ZUSTAENDIG_IN_RAD_VIS);
		if (zustaendigBezeichnung.isEmpty()) {
			abstellanlageBuilder.zustaendig(null);
		} else {
			Pair<String, OrganisationsArt> nameUndOrganisationsart;
			try {
				nameUndOrganisationsart = Verwaltungseinheit.parseBezeichnung(zustaendigBezeichnung);
			} catch (Exception e) {
				throw new AbstellanlageAttributMappingException(
					String.format(
						"Verwaltungseinheit '%s' konnte nicht verarbeitet werden. Verwaltugnseinheiten müssen in der Form 'Name (Organisationsart)' vorliegen",
						zustaendigBezeichnung));
			}
			verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt(nameUndOrganisationsart.getFirst(),
				nameUndOrganisationsart.getSecond())
				.map(abstellanlageBuilder::zustaendig)
				.orElseThrow(() -> new AbstellanlageAttributMappingException(
					String.format("Verwaltungseinheit '%s' konnte nicht gefunden werden", zustaendigBezeichnung)));
		}

		// ANZAHL_STELLPLAETZE
		try {
			abstellanlageBuilder.anzahlStellplaetze(
				AnzahlStellplaetze.of(row.get(Abstellanlage.CsvHeader.ANZAHL_STELLPLAETZE)));
		} catch (NumberFormatException e) {
			throw new AbstellanlageAttributMappingException(
				"Anzahl Stellplätze (" + row.get(Abstellanlage.CsvHeader.ANZAHL_STELLPLAETZE)
					+ ") kann nicht in eine Zahl umgewandelt werden");
		}

		// ANZAHL_SCHLIESSFAECHER
		String anzahlSchliessfaecherString = row.get(Abstellanlage.CsvHeader.ANZAHL_SCHLIESSFAECHER);
		if (anzahlSchliessfaecherString.isEmpty()) {
			abstellanlageBuilder.anzahlSchliessfaecher(null);
		} else {
			try {
				abstellanlageBuilder.anzahlSchliessfaecher(
					AnzahlSchliessfaecher.of(anzahlSchliessfaecherString));
			} catch (NumberFormatException e) {
				throw new AbstellanlageAttributMappingException(
					"Anzahl Schließfächer (" + anzahlSchliessfaecherString
						+ ") kann nicht in eine Zahl umgewandelt werden");
			}
		}

		// ANZAHL_LADEMOEGLICHKEITEN
		String anzahlLademoeglichkeitenString = row.get(Abstellanlage.CsvHeader.ANZAHL_LADEMOEGLICHKEITEN);
		if (anzahlLademoeglichkeitenString.isEmpty()) {
			abstellanlageBuilder.anzahlLademoeglichkeiten(null);
		} else {
			try {
				abstellanlageBuilder.anzahlLademoeglichkeiten(
					AnzahlLademoeglichkeiten.of(row.get(Abstellanlage.CsvHeader.ANZAHL_LADEMOEGLICHKEITEN)));
			} catch (
				NumberFormatException e) {
				throw new AbstellanlageAttributMappingException(
					"Anzahl Lademöglichkeiten (" + row.get(Abstellanlage.CsvHeader.ANZAHL_LADEMOEGLICHKEITEN)
						+ ") kann nicht in eine Zahl umgewandelt werden");
			}
		}

		// UEBERWACHT
		try {
			abstellanlageBuilder.ueberwacht(Ueberwacht.fromString(row.get(Abstellanlage.CsvHeader.UEBERWACHT)));
		} catch (
			Exception exception) {
			throw new AbstellanlageAttributMappingException(
				"Überwacht (" + row.get(Abstellanlage.CsvHeader.UEBERWACHT)
					+ ") kann nicht gelesen werden. Erlaubt: "
					+ Arrays.stream(Ueberwacht.values()).map(Ueberwacht::toString)
						.collect(Collectors.joining(", ")));
		}

		// ABSTELLANLAGEN_ORT
		AbstellanlagenOrt abstellanlagenOrt;
		try {
			abstellanlagenOrt = AbstellanlagenOrt.fromString(row.get(Abstellanlage.CsvHeader.ABSTELLANLAGEN_ORT));
			abstellanlageBuilder.abstellanlagenOrt(abstellanlagenOrt);
		} catch (
			Exception exception) {
			throw new AbstellanlageAttributMappingException(
				"AbstellanlagenOrt (" + row.get(Abstellanlage.CsvHeader.ABSTELLANLAGEN_ORT)
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(AbstellanlagenOrt.values())
						.map(AbstellanlagenOrt::toString).collect(Collectors.joining(", ")));
		}

		// GROESSENKLASSE
		String input = row.get(Abstellanlage.CsvHeader.GROESSENKLASSE);
		if (abstellanlagenOrt != AbstellanlagenOrt.BIKE_AND_RIDE && !input.isEmpty()) {
			throw new AbstellanlageAttributMappingException(
				"Eine Größenklasse darf nur gesetzt sein, wenn es sich um eine Bike and Ride Abstellanlage handelt.");
		}
		try {
			if (input.isEmpty()) {
				abstellanlageBuilder.groessenklasse(null);
			} else {
				abstellanlageBuilder.groessenklasse(Groessenklasse.fromString(input));
			}
		} catch (
			Exception exception) {
			throw new AbstellanlageAttributMappingException(
				"Größenklasse (" + input
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(Groessenklasse.values())
						.map(Groessenklasse::toString).collect(Collectors.joining(", ")));
		}

		// STELLPLATZART
		try {
			abstellanlageBuilder.stellplatzart(
				Stellplatzart.fromString(row.get(Abstellanlage.CsvHeader.STELLPLATZART)));
		} catch (
			Exception exception) {
			throw new AbstellanlageAttributMappingException(
				"Stellplatzart (" + row.get(Abstellanlage.CsvHeader.STELLPLATZART)
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(Stellplatzart.values())
						.map(Stellplatzart::toString).collect(Collectors.joining(", ")));
		}

		// UEBERDACHT
		pruefeBooleanInput("Überdacht", row.get(Abstellanlage.CsvHeader.UEBERDACHT));
		abstellanlageBuilder.ueberdacht(Ueberdacht.of(row.get(Abstellanlage.CsvHeader.UEBERDACHT).

			equals("ja")));

		// GEBUEHREN_PRO_TAG
		String gebuehrenTagString = row.get(Abstellanlage.CsvHeader.GEBUEHREN_PRO_TAG);
		try {
			if (gebuehrenTagString.isEmpty()) {
				abstellanlageBuilder.gebuehrenProTag(null);
			} else {
				abstellanlageBuilder.gebuehrenProTag(
					GebuehrenProTag.of(gebuehrenTagString));
			}
		} catch (
			NumberFormatException e) {
			throw new AbstellanlageAttributMappingException(
				"Gebühren pro Tag (" + gebuehrenTagString
					+ ") kann nicht in eine Zahl umgewandelt werden");
		}

		// GEBUEHREN_PRO_MONAT
		String gebuehrenMonatString = row.get(Abstellanlage.CsvHeader.GEBUEHREN_PRO_MONAT);
		try {
			if (gebuehrenMonatString.isEmpty()) {
				abstellanlageBuilder.gebuehrenProMonat(null);
			} else {
				abstellanlageBuilder.gebuehrenProMonat(
					GebuehrenProMonat.of(gebuehrenMonatString));
			}
		} catch (
			NumberFormatException e) {
			throw new AbstellanlageAttributMappingException(
				"Gebühren pro Monat (" + gebuehrenMonatString + ") kann nicht in eine Zahl umgewandelt werden");
		}

		// GEBUEHREN_PRO_JAHR
		String gebuehrenJahrString = row.get(Abstellanlage.CsvHeader.GEBUEHREN_PRO_JAHR);
		try {
			if (gebuehrenJahrString.isEmpty()) {
				abstellanlageBuilder.gebuehrenProJahr(null);
			} else {
				abstellanlageBuilder.gebuehrenProJahr(
					GebuehrenProJahr.of(gebuehrenJahrString));
			}
		} catch (
			NumberFormatException e) {
			throw new AbstellanlageAttributMappingException(
				"Gebühren pro Jahr (" + gebuehrenJahrString + ") kann nicht in eine Zahl umgewandelt werden");
		}

		// BESCHREIBUNG
		String beschreibungString = row.get(Abstellanlage.CsvHeader.BESCHREIBUNG);
		if (beschreibungString.isEmpty()) {
			abstellanlageBuilder.beschreibung(null);
		} else if (beschreibungString.length() > AbstellanlagenBeschreibung.MAX_LENGTH) {
			throw new AbstellanlageAttributMappingException(
				String.format("Beschreibung darf max. %d Zeichen enthalten",
					AbstellanlagenBeschreibung.MAX_LENGTH));
		} else {
			abstellanlageBuilder.beschreibung(
				AbstellanlagenBeschreibung.of(beschreibungString));
		}

		// WEITERE_INFORMATION
		String weitereInformationString = row.get(Abstellanlage.CsvHeader.WEITERE_INFORMATION);
		if (weitereInformationString.isEmpty()) {
			abstellanlageBuilder.weitereInformation(null);
		} else if (weitereInformationString.length() > AbstellanlagenWeitereInformation.MAX_LENGTH) {
			throw new AbstellanlageAttributMappingException(
				String.format("Weitere Informationen darf max. %d Zeichen enthalten",
					AbstellanlagenWeitereInformation.MAX_LENGTH));
		} else {
			abstellanlageBuilder.weitereInformation(AbstellanlagenWeitereInformation.of(weitereInformationString));
		}

		// ABSTELLANLAGENSTATUS
		try {
			abstellanlageBuilder.status(
				AbstellanlagenStatus.fromString(row.get(Abstellanlage.CsvHeader.ABSTELLANLAGEN_STATUS)));
		} catch (
			Exception exception) {
			throw new AbstellanlageAttributMappingException(
				"Status (" + row.get(Abstellanlage.CsvHeader.ABSTELLANLAGEN_STATUS)
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(AbstellanlagenStatus.values())
						.map(AbstellanlagenStatus::toString).collect(Collectors.joining(", ")));
		}

		// DOKUMENTLISTE nicht veraendern

		return abstellanlageBuilder.build();
	}

	private static Point extractPositionInternal(Map<String, String> row) throws ParseException {
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		double x = format.parse(row.get(Abstellanlage.CsvHeader.POSITION_X_UTM32_N)).doubleValue();
		double y = format.parse(row.get(Abstellanlage.CsvHeader.POSITION_Y_UTM32_N)).doubleValue();

		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(x, y));
	}

	@Override
	protected Abstellanlage save(Abstellanlage mappedEntity) {
		return abstellanlageRepository.save(mappedEntity);
	}

	@Override
	protected Optional<AbstellanlageBuilder> getBuilderFromPosition(Geometry extractedPosition) {
		require(extractedPosition.getGeometryType().equals(Geometry.TYPENAME_POINT), "Position muss ein Punkt sein");
		return abstellanlageRepository.findByPositionAndQuellSystemRadVis((Point) extractedPosition)
			.map(Abstellanlage::toBuilder);
	}

	@Override
	protected Geometry extractPosition(Map<String, String> row) throws ParseException {
		return extractPositionInternal(row);
	}

	@Override
	protected Optional<AbstellanlageBuilder> getBuilderFromId(long id) {
		return abstellanlageRepository.findByIdAndQuellSystem(id, AbstellanlagenQuellSystem.RADVIS)
			.map(Abstellanlage::toBuilder);
	}

	@Override
	protected AbstellanlageBuilder getDefaultBuilder() {
		return Abstellanlage.builder().dokumentListe(new DokumentListe());
	}

	public static void pruefeBooleanInput(String bezeichnung, String input)
		throws AbstellanlageAttributMappingException {
		String normalizedInput = input.toLowerCase();
		if (!(normalizedInput.equals("ja") || normalizedInput.equals("nein"))) {
			throw new AbstellanlageAttributMappingException(
				String.format("%s muss ja oder nein sein, aber ist '%s'", bezeichnung, input));
		}
	}
}
