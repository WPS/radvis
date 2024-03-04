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

package de.wps.radvis.backend.leihstation.domain;

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
import org.springframework.security.access.AccessDeniedException;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.AbstractCsvImportService;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.exception.CsvAttributMappingException;
import de.wps.radvis.backend.common.domain.exception.CsvImportException;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation.LeihstationBuilder;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;
import de.wps.radvis.backend.leihstation.domain.valueObject.UrlAdresse;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;

public class LeihstationImportService extends AbstractCsvImportService<Leihstation, Leihstation.LeihstationBuilder> {

	private final LeihstationRepository leihstationRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final String baseUrl;

	public LeihstationImportService(LeihstationRepository leihstationRepository,
		VerwaltungseinheitService verwaltungseinheitService, ZustaendigkeitsService zustaendigkeitsService,
		String baseUrl) {
		super(Leihstation.CsvHeader.ALL, Leihstation.CsvHeader.RAD_VIS_ID);
		this.leihstationRepository = leihstationRepository;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.baseUrl = baseUrl;
	}

	@Override
	protected String createUrl(Leihstation savedLeihstation) {
		return baseUrl + FrontendLinks.leihstationDetails(savedLeihstation.getId());
	}

	public CsvData importCsv(CsvData csvData, Benutzer benutzer) throws CsvImportException {
		if (!benutzer.getRechte()
			.contains(Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Leihstationen zu erstellen oder zu bearbeiten.");
		}

		return super.importCsv(csvData,
			(leihstationBuilder, row) -> this.mapAttributes(leihstationBuilder, row, benutzer));
	}

	public Leihstation mapAttributes(LeihstationBuilder leihstationBuilder, Map<String, String> row, Benutzer benutzer)
		throws CsvAttributMappingException {

		PreparedGeometry boundingArea = verwaltungseinheitService.getBundeslandBereichPrepared();

		// QUELLSYSTEM
		LeihstationQuellSystem quellSystem;
		try {
			quellSystem = LeihstationQuellSystem.fromString(
				row.get(Leihstation.CsvHeader.QUELLSYSTEM));
		} catch (Exception exception) {
			throw new LeihstationAttributMappingException(
				"Quellsystem (" + row.get(Leihstation.CsvHeader.QUELLSYSTEM)
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(LeihstationQuellSystem.values())
					.map(LeihstationQuellSystem::toString).collect(Collectors.joining(", ")));
		}
		if (quellSystem.equals(LeihstationQuellSystem.MOBIDATABW)) {
			throw new LeihstationAttributMappingException(
				"Leihstationen mit dem Quellsystem MobiDataBW können nicht über den manuellen CSV-Import importiert werden.");
		} else {
			leihstationBuilder.quellSystem(quellSystem);
		}

		try {
			Point point = extractPositionInternal(row);
			if (!boundingArea.contains(point)) {
				throw new LeihstationAttributMappingException(
					"Position " + point + " liegt nicht in Baden-Württemberg. Bitte prüfen Sie die Koordinaten.");
			}
			if (!zustaendigkeitsService.istImZustaendigkeitsbereich(point, benutzer)) {
				throw new AccessDeniedException(
					"Sie haben nicht die Berechtigung Leihstationen in diesem Verwaltungsbereich anzulegen oder zu bearbeiten.");
			}
			leihstationBuilder.geometrie(point);
		} catch (Exception exception) {
			throw new LeihstationAttributMappingException(
				"Geometrie (" + row.get(Leihstation.CsvHeader.POSITION_X_UTM32_N) + ";"
					+ row.get(Leihstation.CsvHeader.POSITION_Y_UTM32_N)
					+ ") kann nicht gelesen werden. Details: " + exception.getMessage());
		}

		String input = row.get(Leihstation.CsvHeader.FREIES_ABSTELLEN_MOEGLICH).toLowerCase();
		if (!(input.equals("ja") || input.equals("nein"))) {
			throw new LeihstationAttributMappingException(
				"Freies Abstellen möglich (" + row.get(Leihstation.CsvHeader.FREIES_ABSTELLEN_MOEGLICH)
					+ ") muss ja oder nein sein.");

		}
		leihstationBuilder.freiesAbstellen(input.equals("ja"));

		try {
			leihstationBuilder.anzahlFahrraeder(Anzahl.of(row.get(Leihstation.CsvHeader.ANZAHL_FAHRRAEDER)));
		} catch (NumberFormatException e) {
			throw new LeihstationAttributMappingException(
				"Anzahl Fahrräder (" + row.get(Leihstation.CsvHeader.ANZAHL_FAHRRAEDER)
					+ ") kann nicht in eine Zahl umgewandelt werden");
		}

		try {
			leihstationBuilder.anzahlPedelecs(Anzahl.of(row.get(Leihstation.CsvHeader.ANZAHL_PEDELECS)));
		} catch (NumberFormatException e) {
			throw new LeihstationAttributMappingException(
				"Anzahl Pedelecs (" + row.get(Leihstation.CsvHeader.ANZAHL_PEDELECS)
					+ ") kann nicht in eine Zahl umgewandelt werden");
		}

		try {
			leihstationBuilder
				.anzahlAbstellmoeglichkeiten(Anzahl.of(row.get(Leihstation.CsvHeader.ANZAHL_ABSTELLMOEGLICHKEITEN)));
		} catch (NumberFormatException e) {
			throw new LeihstationAttributMappingException(
				"Anzahl Abstellmöglichkeiten (" + row.get(Leihstation.CsvHeader.ANZAHL_ABSTELLMOEGLICHKEITEN)
					+ ") kann nicht in eine Zahl umgewandelt werden");
		}

		if (!row.get(Leihstation.CsvHeader.BUCHUNGS_URL).isEmpty()) {
			leihstationBuilder.buchungsUrl(UrlAdresse.of(row.get(Leihstation.CsvHeader.BUCHUNGS_URL)));
		}

		if (row.get(Leihstation.CsvHeader.BETREIBER).isEmpty()
			|| row.get(Leihstation.CsvHeader.BETREIBER).length() > Betreiber.MAX_LENGTH) {
			throw new LeihstationAttributMappingException(
				String.format("Betreiber (%s) muss angegeben sein und darf max. %d Zeichen enthalten",
					row.get(Leihstation.CsvHeader.BETREIBER),
					Betreiber.MAX_LENGTH));
		}
		leihstationBuilder.betreiber(row.get(Leihstation.CsvHeader.BETREIBER));

		try {
			leihstationBuilder.status(LeihstationStatus.fromString(row.get(Leihstation.CsvHeader.STATUS)));
		} catch (Exception exception) {
			throw new LeihstationAttributMappingException(
				"Status (" + row.get(Leihstation.CsvHeader.STATUS)
					+ ") kann nicht gelesen werden. Erlaubt: " + String.join(", ",
					Arrays.asList(LeihstationStatus.values()).stream().map(status -> status.toString())
						.collect(Collectors.toList())));
		}

		try {
			Point point = extractPositionInternal(row);
			if (!boundingArea.contains(point)) {
				throw new LeihstationAttributMappingException(
					"Position " + point + " liegt nicht in Baden-Württemberg. Bitte prüfen Sie die Koordinaten.");
			}
			leihstationBuilder.geometrie(point);
		} catch (Exception exception) {
			throw new LeihstationAttributMappingException(
				"Geometrie (" + row.get(Leihstation.CsvHeader.POSITION_X_UTM32_N) + ";"
					+ row.get(Leihstation.CsvHeader.POSITION_Y_UTM32_N)
					+ ") kann nicht gelesen werden. Details: " + exception.getMessage());
		}

		return leihstationBuilder.build();
	}

	private static Point extractPositionInternal(Map<String, String> row) throws ParseException {
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		double x = format.parse(row.get(Leihstation.CsvHeader.POSITION_X_UTM32_N)).doubleValue();
		double y = format.parse(row.get(Leihstation.CsvHeader.POSITION_Y_UTM32_N)).doubleValue();

		Point point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(new Coordinate(x, y));
		return point;
	}

	@Override
	protected Leihstation save(Leihstation mappedEntity) {
		return leihstationRepository.save(mappedEntity);
	}

	@Override
	protected Optional<LeihstationBuilder> getBuilderFromPosition(Geometry extractedPosition) {
		require(extractedPosition.getGeometryType().equals(Geometry.TYPENAME_POINT), "Position muss ein Punkt sein");
		return leihstationRepository.findByPositionAndQuellSystemRadVis((Point) extractedPosition)
			.map(Leihstation::toBuilder);
	}

	@Override
	protected Geometry extractPosition(Map<String, String> row) throws ParseException {
		return extractPositionInternal(row);
	}

	@Override
	protected Optional<LeihstationBuilder> getBuilderFromId(long id) {
		return leihstationRepository.findByIdAndQuellSystem(id, LeihstationQuellSystem.RADVIS)
			.map(Leihstation::toBuilder);
	}

	@Override
	protected LeihstationBuilder getDefaultBuilder() {
		return Leihstation.builder();
	}
}
