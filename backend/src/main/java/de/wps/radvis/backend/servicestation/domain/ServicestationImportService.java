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

package de.wps.radvis.backend.servicestation.domain;

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
import org.springframework.data.util.Pair;
import org.springframework.security.access.AccessDeniedException;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.AbstractCsvImportService;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.exception.CsvImportException;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation.ServicestationBuilder;
import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;
import de.wps.radvis.backend.servicestation.domain.valueObject.Fahrradhalterung;
import de.wps.radvis.backend.servicestation.domain.valueObject.Gebuehren;
import de.wps.radvis.backend.servicestation.domain.valueObject.Kettenwerkzeug;
import de.wps.radvis.backend.servicestation.domain.valueObject.Luftpumpe;
import de.wps.radvis.backend.servicestation.domain.valueObject.Marke;
import de.wps.radvis.backend.servicestation.domain.valueObject.Oeffnungszeiten;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationBeschreibung;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationName;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationStatus;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationTyp;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationenQuellSystem;
import de.wps.radvis.backend.servicestation.domain.valueObject.Werkzeug;

public class ServicestationImportService
	extends AbstractCsvImportService<Servicestation, Servicestation.ServicestationBuilder> {

	private final ServicestationRepository servicestationRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final String baseUrl;

	public ServicestationImportService(ServicestationRepository servicestationRepository,
		VerwaltungseinheitService verwaltungseinheitService, ZustaendigkeitsService zustaendigkeitsService,
		String baseUrl) {
		super(Servicestation.CsvHeader.ALL, Servicestation.CsvHeader.RAD_VIS_ID);
		this.servicestationRepository = servicestationRepository;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.baseUrl = baseUrl;
	}

	@Override
	protected String createUrl(Servicestation savedServicestation) {
		return baseUrl + FrontendLinks.servicestationDetails(savedServicestation.getId());
	}

	public CsvData importCsv(CsvData csvData, Benutzer benutzer) throws CsvImportException {
		if (!benutzer.getRechte()
			.contains(Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Servicestationen zu erstellen oder zu bearbeiten.");
		}

		return super.importCsv(csvData,
			(servicestationBuilder, row) -> this.mapAttributes(servicestationBuilder, row, benutzer));
	}

	public Servicestation mapAttributes(ServicestationBuilder servicestationBuilder, Map<String, String> row,
		Benutzer benutzer)
		throws ServicestationAttributMappingException {

		PreparedGeometry boundingArea = verwaltungseinheitService.getBundeslandBereichPrepared();

		// QUELLSYSTEM
		ServicestationenQuellSystem quellSystem;
		try {
			quellSystem = ServicestationenQuellSystem.fromString(
				row.get(Servicestation.CsvHeader.QUELL_SYSTEM));
		} catch (Exception exception) {
			throw new ServicestationAttributMappingException(
				"Quellsystem (" + row.get(Servicestation.CsvHeader.QUELL_SYSTEM)
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(ServicestationenQuellSystem.values())
					.map(ServicestationenQuellSystem::toString).collect(Collectors.joining(", ")));
		}
		if (quellSystem.equals(ServicestationenQuellSystem.MOBIDATABW)) {
			throw new ServicestationAttributMappingException(
				"Servicestationen mit dem Quellsystem MobiDataBW können nicht über den manuellen CSV-Import importiert werden.");
		} else {
			servicestationBuilder.quellSystem(quellSystem);
		}

		// GEOMETRIE
		try {
			Point point = extractPositionInternal(row);
			if (!boundingArea.contains(point)) {
				throw new ServicestationAttributMappingException(
					"Position " + point + " liegt nicht in Baden-Württemberg. Bitte prüfen Sie die Koordinaten.");
			}
			if (!zustaendigkeitsService.istImZustaendigkeitsbereich(point, benutzer)) {
				throw new AccessDeniedException(
					"Sie haben nicht die Berechtigung Servicestationen in diesem Verwaltungsbereich anzulegen oder zu bearbeiten.");
			}
			servicestationBuilder.geometrie(point);
		} catch (Exception exception) {
			throw new ServicestationAttributMappingException(
				"Geometrie (" + row.get(Servicestation.CsvHeader.POSITION_X_UTM32_N) + ";"
					+ row.get(Servicestation.CsvHeader.POSITION_Y_UTM32_N)
					+ ") kann nicht gelesen werden. Details: " + exception.getMessage());
		}

		// NAME
		if (row.get(Servicestation.CsvHeader.NAME).isEmpty()
			|| row.get(Servicestation.CsvHeader.NAME).length() > ServicestationName.MAX_LENGTH) {
			throw new ServicestationAttributMappingException(
				String.format("Name (%s) muss angegeben sein und darf max. %d Zeichen enthalten",
					row.get(Servicestation.CsvHeader.NAME),
					ServicestationName.MAX_LENGTH));
		}
		servicestationBuilder.name(ServicestationName.of(row.get(Servicestation.CsvHeader.NAME)));

		// MARKE
		if (!row.get(Servicestation.CsvHeader.MARKE).isEmpty()) {
			try {
				servicestationBuilder.marke(Marke.of(row.get(Servicestation.CsvHeader.MARKE)));
			} catch (Throwable t) {
				throw new ServicestationAttributMappingException(
					String.format("Marke darf nicht länger als %d Zeichen sein! %s",
						Marke.MAX_LENGTH,
						t.getMessage()));
			}
		}

		// GEBUEHREN
		String input = row.get(Servicestation.CsvHeader.GEBUEHREN);
		pruefeBooleanInput("Gebüren", input);
		servicestationBuilder.gebuehren(Gebuehren.of(input));

		// OEFFNUNGSZEITEN
		if (!row.get(Servicestation.CsvHeader.OEFFNUNGSZEITEN).isEmpty()) {
			try {
				servicestationBuilder.oeffnungszeiten(
					Oeffnungszeiten.of(row.get(Servicestation.CsvHeader.OEFFNUNGSZEITEN)));
			} catch (Exception e) {
				throw new ServicestationAttributMappingException(
					String.format("Öffnungszeiten darf nicht länger als %d Zeichen sein! %s",
						Oeffnungszeiten.MAX_LENGTH,
						e.getMessage()));
			}
		}

		// BETREIBER
		if (row.get(Servicestation.CsvHeader.BETREIBER).isEmpty()
			|| row.get(Servicestation.CsvHeader.BETREIBER).length() > Betreiber.MAX_LENGTH) {
			throw new ServicestationAttributMappingException(
				String.format("Betreiber (%s) muss angegeben sein und darf max. %d Zeichen enthalten",
					row.get(Servicestation.CsvHeader.BETREIBER),
					Betreiber.MAX_LENGTH));
		}
		servicestationBuilder.betreiber(Betreiber.of(row.get(Servicestation.CsvHeader.BETREIBER)));

		// LUFTPUMPE
		input = row.get(Servicestation.CsvHeader.LUFTPUMPE);
		pruefeBooleanInput("Luftpumpe", input);
		servicestationBuilder.luftpumpe(Luftpumpe.of(input));

		// KETTENWERKZEUG
		input = row.get(Servicestation.CsvHeader.KETTENWERKZEUG);
		pruefeBooleanInput("Kettenwerkzeug", input);
		servicestationBuilder.kettenwerkzeug(Kettenwerkzeug.of(input));

		// FAHRRADHALTERUNG
		input = row.get(Servicestation.CsvHeader.FAHRRADHALTERUNG);
		pruefeBooleanInput("Fahrradhalterung", input);
		servicestationBuilder.fahrradhalterung(Fahrradhalterung.of(input));

		// WERKZEUG
		input = row.get(Servicestation.CsvHeader.WERKZEUG);
		pruefeBooleanInput("Werkzeug", input);
		servicestationBuilder.werkzeug(Werkzeug.of(input));

		// BESCHREIBUNG
		if (!row.get(Servicestation.CsvHeader.BESCHREIBUNG).isEmpty()) {
			try {
				servicestationBuilder.beschreibung(
					ServicestationBeschreibung.of(row.get(Servicestation.CsvHeader.BESCHREIBUNG)));
			} catch (Exception e) {
				throw new ServicestationAttributMappingException(
					String.format(
						"Beschreibung darf nicht länger als %d Zeichen sein! %s",
						ServicestationBeschreibung.MAX_LENGTH,
						e.getMessage()));
			}
		}

		// ZUSTAENDIG_IN_RAD_VIS / Organisation
		final String bezeichnung = row.get(Servicestation.CsvHeader.ZUSTAENDIG_IN_RAD_VIS);
		Pair<String, OrganisationsArt> nameUndOrganisationsart;
		try {
			nameUndOrganisationsart = Verwaltungseinheit.parseBezeichnung(bezeichnung);
		} catch (Exception e) {
			throw new ServicestationAttributMappingException(
				String.format(
					"Verwaltungseinheit '%s' konnte nicht verarbeitet werden. Verwaltungseinheiten müssen in der Form 'Name (Organisationsart)' vorliegen",
					bezeichnung));
		}

		verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt(nameUndOrganisationsart.getFirst(),
				nameUndOrganisationsart.getSecond())
			.map(servicestationBuilder::organisation)
			.orElseThrow(() -> new ServicestationAttributMappingException(
				String.format("Verwaltungseinheit '%s' konnte nicht gefunden werden", bezeichnung)));

		// TYP
		try {
			servicestationBuilder.typ(ServicestationTyp.fromString(row.get(Servicestation.CsvHeader.TYP)));
		} catch (Exception exception) {
			throw new ServicestationAttributMappingException(
				"Typ (" + row.get(Servicestation.CsvHeader.TYP)
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(ServicestationTyp.values()).map(
						ServicestationTyp::toString)
					.collect(Collectors.joining(", ")));
		}

		// STATUS
		try {
			servicestationBuilder.status(ServicestationStatus.fromString(row.get(Servicestation.CsvHeader.STATUS)));
		} catch (Exception exception) {
			throw new ServicestationAttributMappingException(
				"Status (" + row.get(Servicestation.CsvHeader.STATUS)
					+ ") kann nicht gelesen werden. Erlaubt: " + Arrays.stream(ServicestationStatus.values())
					.map(ServicestationStatus::toString)
					.collect(Collectors.joining(", ")));
		}

		servicestationBuilder.dokumentListe(new DokumentListe());

		return servicestationBuilder.build();
	}

	private static void pruefeBooleanInput(String bezeichnung, String input)
		throws ServicestationAttributMappingException {
		String normalizedInput = input.toLowerCase();
		if (!(normalizedInput.equals("ja") || normalizedInput.equals("nein"))) {
			throw new ServicestationAttributMappingException(
				String.format("%s muss ja oder nein sein, aber ist '%s'", bezeichnung, input)
			);
		}
	}

	private static Point extractPositionInternal(Map<String, String> row) throws ParseException {
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		double x = format.parse(row.get(Servicestation.CsvHeader.POSITION_X_UTM32_N)).doubleValue();
		double y = format.parse(row.get(Servicestation.CsvHeader.POSITION_Y_UTM32_N)).doubleValue();

		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(new Coordinate(x, y));
	}

	@Override
	protected Servicestation save(Servicestation mappedEntity) {
		return servicestationRepository.save(mappedEntity);
	}

	@Override
	protected Optional<ServicestationBuilder> getBuilderFromPosition(Geometry extractedPosition) {
		require(extractedPosition.getGeometryType().equals(Geometry.TYPENAME_POINT), "Position muss ein Punkt sein");
		return servicestationRepository.findByPositionAndQuellSystemRadvis((Point) extractedPosition)
			.map(Servicestation::toBuilder);
	}

	@Override
	protected Geometry extractPosition(Map<String, String> row) throws ParseException {
		return extractPositionInternal(row);
	}

	@Override
	protected Optional<ServicestationBuilder> getBuilderFromId(long id) {
		return servicestationRepository.findByIdAndQuellSystem(id, ServicestationenQuellSystem.RADVIS)
			.map(Servicestation::toBuilder);
	}

	@Override
	protected ServicestationBuilder getDefaultBuilder() {
		return Servicestation.builder().dokumentListe(new DokumentListe());
	}
}
