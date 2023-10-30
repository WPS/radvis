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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.schnittstelle.CSVExportConverter;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.exception.CsvReadException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.CsvRepositoryImpl;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;
import de.wps.radvis.backend.servicestation.domain.valueObject.Fahrradhalterung;
import de.wps.radvis.backend.servicestation.domain.valueObject.Gebuehren;
import de.wps.radvis.backend.servicestation.domain.valueObject.Kettenwerkzeug;
import de.wps.radvis.backend.servicestation.domain.valueObject.Luftpumpe;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationName;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationStatus;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationTyp;
import de.wps.radvis.backend.servicestation.domain.valueObject.Werkzeug;

class ServicestationImportServiceTest {

	@Mock
	ServicestationRepository servicestationRepository;
	@Mock
	VerwaltungseinheitService verwaltungseinheitService;
	@Mock
	ZustaendigkeitsService zustaendigkeitsService;

	private ServicestationImportService servicestationImportService;
	private Benutzer adminBenutzer;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		servicestationImportService = new ServicestationImportService(servicestationRepository,
			verwaltungseinheitService, zustaendigkeitsService, "foo");
		adminBenutzer = BenutzerTestDataProvider
			.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.build();

		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 1000, 1000)));
		when(zustaendigkeitsService.istImZustaendigkeitsbereich(any(Geometry.class), eq(adminBenutzer))).thenReturn(
			true);
	}

	@Test
	void mapAttributes_fromRadVISExport() throws CsvReadException, ServicestationAttributMappingException {
		// arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultOrganisation().build();
		when(verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt()))
			.thenReturn(Optional.of(verwaltungseinheit));

		Servicestation servicestation1 = Servicestation.builder()
			.id(10L)
			.name(ServicestationName.of("Station1"))
			.betreiber(Betreiber.of("Mein Betreiber"))
			.gebuehren(Gebuehren.of(true))
			.luftpumpe(Luftpumpe.of(true))
			.fahrradhalterung(Fahrradhalterung.of(true))
			.kettenwerkzeug(Kettenwerkzeug.of(true))
			.werkzeug(Werkzeug.of(true))
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
			.status(ServicestationStatus.AUSSER_BETRIEB)
			.typ(ServicestationTyp.RADSERVICE_PUNKT_KLEIN)
			.organisation(verwaltungseinheit)
			.dokumentListe(new DokumentListe())
			.build();
		Servicestation servicestation2 = Servicestation.builder()
			.id(20L)
			.name(ServicestationName.of("Station1"))
			.betreiber(Betreiber.of("Mein Betreiber 2"))
			.gebuehren(Gebuehren.of(false))
			.luftpumpe(Luftpumpe.of(false))
			.fahrradhalterung(Fahrradhalterung.of(false))
			.kettenwerkzeug(Kettenwerkzeug.of(false))
			.werkzeug(Werkzeug.of(false))
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(500, 600)))
			.status(ServicestationStatus.GEPLANT)
			.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
			.organisation(verwaltungseinheit)
			.dokumentListe(new DokumentListe())
			.build();

		when(servicestationRepository.findAllById(any())).thenReturn(List.of(servicestation1, servicestation2));
		List<ExportData> export = new ServicestationExporterService(servicestationRepository)
			.export(List.of(servicestation1.getId(), servicestation2.getId()));

		CsvRepository csvRepository = new CsvRepositoryImpl();
		byte[] csv = new CSVExportConverter(csvRepository).convert(export);
		CsvData csvData = csvRepository.read(csv, Servicestation.CsvHeader.ALL);

		// act
		Servicestation result1 = servicestationImportService
			.mapAttributes(Servicestation.builder().id(servicestation1.getId()), csvData.getRows().get(0), adminBenutzer);
		Servicestation result2 = servicestationImportService
			.mapAttributes(Servicestation.builder().id(servicestation2.getId()), csvData.getRows().get(1), adminBenutzer);

		// assert
		assertThat(result1).usingRecursiveComparison().usingOverriddenEquals().isEqualTo(servicestation1);
		assertThat(result2).usingRecursiveComparison().usingOverriddenEquals().isEqualTo(servicestation2);
	}

	@Test
	void getBuilderFromPosition() {
		// arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultOrganisation().build();

		Servicestation servicestation1 = Servicestation.builder()
			.id(10L)
			.name(ServicestationName.of("Station1"))
			.betreiber(Betreiber.of("Mein Betreiber"))
			.gebuehren(Gebuehren.of(true))
			.luftpumpe(Luftpumpe.of(true))
			.fahrradhalterung(Fahrradhalterung.of(true))
			.kettenwerkzeug(Kettenwerkzeug.of(true))
			.werkzeug(Werkzeug.of(true))
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
			.status(ServicestationStatus.AUSSER_BETRIEB)
			.typ(ServicestationTyp.RADSERVICE_PUNKT_KLEIN)
			.organisation(verwaltungseinheit)
			.dokumentListe(new DokumentListe())
			.build();
		Servicestation servicestation2 = Servicestation.builder()
			.id(20L)
			.name(ServicestationName.of("Station1"))
			.betreiber(Betreiber.of("Mein Betreiber 2"))
			.gebuehren(Gebuehren.of(false))
			.luftpumpe(Luftpumpe.of(false))
			.fahrradhalterung(Fahrradhalterung.of(false))
			.kettenwerkzeug(Kettenwerkzeug.of(false))
			.werkzeug(Werkzeug.of(false))
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(500, 600)))
			.status(ServicestationStatus.GEPLANT)
			.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
			.organisation(verwaltungseinheit)
			.dokumentListe(new DokumentListe())
			.build();

		when(servicestationRepository.findByPosition(any())).thenAnswer(
			invocationOnMock -> {
				Geometry position = invocationOnMock.getArgument(0);
				if (position.distance(servicestation1.getGeometrie()) < 1) {
					return Optional.of(servicestation1);
				}

				return position.distance(servicestation2.getGeometrie()) < 1 ?
					Optional.of(servicestation2) :
					Optional.empty();
			});

		// act
		Optional<Servicestation.ServicestationBuilder> builderForStation1 = servicestationImportService.getBuilderFromPosition(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100.5, 200.5)));
		Optional<Servicestation.ServicestationBuilder> builderForStation2 = servicestationImportService.getBuilderFromPosition(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(500.5, 600.5)));
		Optional<Servicestation.ServicestationBuilder> noBuilder = servicestationImportService.getBuilderFromPosition(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(300, 300)));

		// assert
		assertThat(builderForStation1).isPresent();
		assertThat(builderForStation1.get().build()).extracting(AbstractEntity::getId)
			.isEqualTo(servicestation1.getId());

		assertThat(builderForStation2).isPresent();
		assertThat(builderForStation2.get().build()).extracting(AbstractEntity::getId)
			.isEqualTo(servicestation2.getId());

		assertThat(noBuilder).isEmpty();
	}

	@Nested
	class ServicestationAttributMappingExceptionTest {
		private CsvData csvData;

		@BeforeEach
		void setup() throws CsvReadException {

			Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultOrganisation().build();
			when(verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt(verwaltungseinheit.getName(),
				verwaltungseinheit.getOrganisationsArt()))
				.thenReturn(Optional.of(verwaltungseinheit));

			Servicestation servicestation1 = Servicestation.builder()
				.id(10L)
				.name(ServicestationName.of("Station1"))
				.betreiber(Betreiber.of("Mein Betreiber")).gebuehren(Gebuehren.of(true))
				.luftpumpe(Luftpumpe.of(true))
				.fahrradhalterung(Fahrradhalterung.of(true))
				.kettenwerkzeug(Kettenwerkzeug.of(true))
				.werkzeug(Werkzeug.of(true))
				.geometrie(
					KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
				.status(ServicestationStatus.AUSSER_BETRIEB)
				.typ(ServicestationTyp.RADSERVICE_PUNKT_KLEIN)
				.organisation(verwaltungseinheit)
				.dokumentListe(new DokumentListe())
				.build();

			ServicestationRepository servicestationRepository = Mockito.mock(ServicestationRepository.class);
			when(servicestationRepository.findAllById(any())).thenReturn(List.of(servicestation1));
			List<ExportData> export = new ServicestationExporterService(servicestationRepository)
				.export(List.of(servicestation1.getId()));

			CsvRepository csvRepository = new CsvRepositoryImpl();
			byte[] csv = new CSVExportConverter(csvRepository).convert(export);
			csvData = csvRepository.read(csv, Servicestation.CsvHeader.ALL);

			assertDoesNotThrow(
				() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}

		@Test
		void coordinates_wrongLocale() {
			List<Map<String, String>> rows = csvData.getRows();
			HashMap<String, String> incorrectAttributes = new HashMap<>(rows.get(0));
			incorrectAttributes.put("Position X (UTM32_N)",
				incorrectAttributes.get("Position X (UTM32_N)").replace(",", "."));
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(ServicestationAttributMappingException.class,
				() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}

		@Test
		void betreiber_leer() {
			List<Map<String, String>> rows = csvData.getRows();
			HashMap<String, String> incorrectAttributes = new HashMap<>(rows.get(0));
			incorrectAttributes.put(Servicestation.CsvHeader.BETREIBER, "");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(ServicestationAttributMappingException.class,
				() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}

		@Test
		void luftpumpe_keinJaNein() {
			List<Map<String, String>> rows = csvData.getRows();
			HashMap<String, String> incorrectAttributes = new HashMap<>(rows.get(0));
			incorrectAttributes.put(Servicestation.CsvHeader.LUFTPUMPE, "na sichi!");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(ServicestationAttributMappingException.class,
				() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}

		@Test
		void typ_unsinn() {
			List<Map<String, String>> rows = csvData.getRows();
			HashMap<String, String> incorrectAttributes = new HashMap<>(rows.get(0));
			incorrectAttributes.put(Servicestation.CsvHeader.TYP, "Geiler Typ");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(ServicestationAttributMappingException.class,
				() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}

		@Test
		void organisation_nichtvorhanden() {
			when(verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt("Quatschburg", OrganisationsArt.KREIS))
				.thenReturn(Optional.empty());

			List<Map<String, String>> rows = csvData.getRows();
			HashMap<String, String> incorrectAttributes = new HashMap<>(rows.get(0));
			incorrectAttributes.put(Servicestation.CsvHeader.ZUSTAENDIG_IN_RAD_VIS, "Quatschburg (Landkreis)");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThatThrownBy(() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
					adminBenutzer))
				.isInstanceOf(ServicestationAttributMappingException.class)
				.hasMessageContaining("Verwaltungseinheit 'Quatschburg (Landkreis)' konnte nicht gefunden werden");
		}

		@Test
		void organisation_parsingFehlerhaft() {
			when(verwaltungseinheitService.getVerwaltungseinheitnachNameUndArt("Quatschburg", OrganisationsArt.KREIS))
				.thenReturn(Optional.empty());

			List<Map<String, String>> rows = csvData.getRows();
			HashMap<String, String> incorrectAttributes = new HashMap<>(rows.get(0));
			incorrectAttributes.put(Servicestation.CsvHeader.ZUSTAENDIG_IN_RAD_VIS, "Quatschburg (foo)");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThatThrownBy(() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
				adminBenutzer))
				.isInstanceOf(ServicestationAttributMappingException.class)
				.hasMessageContaining("Verwaltungseinheit 'Quatschburg (foo)' konnte nicht verarbeitet werden");
		}

		@Test
		void marke_zulang() {
			String lorem =
				"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum."
					+ " Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
					+ " Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat,"
					+ " vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim"
					+ " qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi."
					+ " Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt"
					+ " ut laoreet dolore magna aliquam erat volutpat."
					+ "	Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl"
					+ " ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate"
					+ " velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan"
					+ " et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi."
					+ "	Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer";

			List<Map<String, String>> rows = csvData.getRows();
			HashMap<String, String> incorrectAttributes = new HashMap<>(rows.get(0));
			incorrectAttributes.put(Servicestation.CsvHeader.MARKE, lorem);
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(ServicestationAttributMappingException.class,
				() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}

		@Test
		void nicht_im_zustaendigkeitsbereich() {
			assertDoesNotThrow(() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
				adminBenutzer));

			when(zustaendigkeitsService.istImZustaendigkeitsbereich(any(Geometry.class), eq(adminBenutzer))).thenReturn(false);

			assertThrows(ServicestationAttributMappingException.class,
				() -> servicestationImportService.mapAttributes(Servicestation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}
	}
}
