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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service;

import static de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service.MassnahmenImportZuordnungAssert.assertThatMassnahmenImportZuordnung;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.geotools.api.feature.simple.SimpleFeature;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Severity;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportAttributeMapTestDataProvider;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnungTestDataProvider;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehler;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehlermeldung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportAttribute;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweisText;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

class ManuellerMassnahmenImportServiceTest {

	ManuellerMassnahmenImportService service;

	@Mock
	ManuellerImportService manuellerImportService;

	@Mock
	MassnahmeNetzbezugService massnahmeNetzbezugService;

	@Mock
	GeoJsonImportRepository geoJsonImportRepository;

	@Mock
	MassnahmeRepository massnahmeRepository;

	@Mock
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Mock
	CsvRepository csvRepository;

	@Mock
	EntityManager entityManager;

	@BeforeEach
	void setUp() {
		openMocks(this);
		when(entityManager.merge(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
		service = new ManuellerMassnahmenImportService(manuellerImportService, massnahmeNetzbezugService,
			geoJsonImportRepository, verwaltungseinheitRepository, massnahmeRepository, entityManager, csvRepository,
			10);
	}

	@Test
	void testLadeFeatures_MassnahmenIdNull_statusFEHLERHAFT() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		MultiLineString geometry = GeometryTestdataProvider.createMultiLineString(
			GeometryTestdataProvider.createLineString());
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(),
				geometry)));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung1 = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung1)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_KEINE_ID.getText()))
			.hasStatus(MassnahmenImportZuordnungStatus.FEHLERHAFT)
			.doesNotHaveAnyMassnahme()
			.doesNotHaveAnyMassnahmeID();
	}

	@Test
	void testLadeFeatures_MassnahmenIdBlank_statusFEHLERHAFT() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		MultiLineString geometry = GeometryTestdataProvider.createMultiLineString(
			GeometryTestdataProvider.createLineString());
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(
			Stream.of(
				SimpleFeatureTestDataProvider.withGeometryAndAttributes(
					Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, " "),
					geometry)));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung1 = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung1)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_KEINE_ID.getText()))
			.hasStatus(MassnahmenImportZuordnungStatus.FEHLERHAFT)
			.doesNotHaveAnyMassnahme()
			.doesNotHaveAnyMassnahmeID();
	}

	@Test
	void testLadeFeatures_MassnahmenIdInvalid_statusFEHLERHAFT() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		MultiLineString geometry = GeometryTestdataProvider.createMultiLineString(
			GeometryTestdataProvider.createLineString());
		String invalideId = "hallo-1";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(
			Stream.of(
				SimpleFeatureTestDataProvider.withGeometryAndAttributes(
					Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, invalideId),
					geometry)));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung1 = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung1)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_ID_INVALID.getText(invalideId)))
			.hasStatus(MassnahmenImportZuordnungStatus.FEHLERHAFT)
			.doesNotHaveAnyMassnahme()
			.doesNotHaveAnyMassnahmeID();
	}

	@Test
	void testLadeFeatures_doppelteMassnahmenId_statusFEHLERHAFT() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String doppelteId = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, doppelteId),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, doppelteId),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(2);
		MassnahmenImportZuordnung zuordnung1 = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung1)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_MEHRFACH.getText()))
			.hasStatus(MassnahmenImportZuordnungStatus.FEHLERHAFT)
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(doppelteId));

		MassnahmenImportZuordnung zuordnung2 = session.getZuordnungen().get(1);
		assertThatMassnahmenImportZuordnung(zuordnung2)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_MEHRFACH.getText()))
			.hasStatus(MassnahmenImportZuordnungStatus.FEHLERHAFT)
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(doppelteId));
	}

	@Test
	void testLadeFeatures_falscheGeom_warnung() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(2, 2), new Coordinate(3, 3));
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(8, 8));
		MultiPolygon multiPolygon = GeometryTestdataProvider.createQuadratischerBereich(8, 8, 50, 50);
		Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
		MultiLineString multiLineString = GeometryTestdataProvider.createMultiLineString(lineString);
		MultiPoint multiPoint = GeometryTestdataProvider.createMultiPoint(new Coordinate(20, 30));
		GeometryCollection geometryCollection = GeometryTestdataProvider.creatGeometryCollection(multiPoint,
			multiLineString);
		String idLineString = "LineString";
		String idPoint = "Point";
		String idMultiPolygon = "MultiPolygon";
		String idPolygon = "Polygon";
		String idMultiLineString = "MultiLineString";
		String idMultiPoint = "MultiPoint";
		String idGeometryCollection = "GeometryCollection";
		String idGeometryCollectionWithPolygon = "GeometryCollectionWithPolygon";
		String idGeometryCollectionNested = "GeometryCollectionNested";
		List<SimpleFeature> features = List.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idLineString),
				// valid
				lineString),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idPoint),
				// valid
				point),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idMultiPolygon),
				// invalid
				multiPolygon),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idPolygon),
				// invalid
				polygon),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idMultiLineString),
				// valid
				multiLineString),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idMultiPoint),
				// valid
				multiPoint),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				// valid
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idGeometryCollection),
				geometryCollection),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idGeometryCollectionWithPolygon),
				// invalid
				GeometryTestdataProvider.creatGeometryCollection(multiPoint, polygon)),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idGeometryCollectionNested),
				// invalid
				GeometryTestdataProvider.creatGeometryCollection(multiPoint, multiLineString, geometryCollection)));
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(features.stream());

		AtomicLong idSequence = new AtomicLong(1);

		Map<String, Massnahme> massnahmen = new HashMap<>();

		when(
			massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(any(), any())).thenAnswer(
				invocationOnMock -> {
					MassnahmeKonzeptID id = invocationOnMock.getArgument(0);
					Konzeptionsquelle konzeptionsquelle = invocationOnMock.getArgument(1);
					Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
						.id(idSequence.getAndIncrement())
						.massnahmeKonzeptId(id)
						.konzeptionsquelle(konzeptionsquelle)
						.build();
					massnahmen.put(id.getValue(), massnahme);
					return List.of(massnahme);
				});

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		List<MassnahmenImportZuordnung> zuordnungen = session.getZuordnungen();

		Map<String, MassnahmenImportZuordnung> idToZuordnung = zuordnungen.stream().collect(
			Collectors.toMap(zuordnung -> zuordnung.getMassnahme().get().getMassnahmeKonzeptID().get().toString(),
				Function.identity()));

		assertThat(zuordnungen).hasSize(features.size());

		// Invalid
		List<String> invalid = List.of(idPolygon, idMultiPolygon, idGeometryCollectionWithPolygon,
			idGeometryCollectionNested);

		for (String id : invalid) {
			MassnahmenImportZuordnung zuordnung = idToZuordnung.get(id);

			assertThatMassnahmenImportZuordnung(zuordnung)
				.as("Zuordnung für id '%s' (invalid)", id)
				.hasExactlyNetzbezugsHinweise(
					NetzbezugHinweis.ofError(NetzbezugHinweisText.FALSCHER_GEOMETRIE_TYP))
				.hasStatus(MassnahmenImportZuordnungStatus.ZUGEORDNET)
				.hasMassnahme(massnahmen.get(id))
				.hasMassnahmeId(MassnahmeKonzeptID.of(id));
		}

		// valid
		List<String> valid = List.of(idLineString, idPoint, idMultiLineString, idMultiPoint, idGeometryCollection);

		assertThat(valid.size() + invalid.size()).isEqualTo(features.size());

		for (String id : valid) {
			MassnahmenImportZuordnung zuordnung = idToZuordnung.get(id);

			assertThatMassnahmenImportZuordnung(zuordnung)
				.as("Zuordnung für id '%s' (valid)", id)
				.doesNotHaveAnyFehler()
				.hasStatus(MassnahmenImportZuordnungStatus.ZUGEORDNET)
				.hasMassnahme(massnahmen.get(id))
				.hasMassnahmeId(MassnahmeKonzeptID.of(id));
		}
	}

	@Test
	void testLadeFeatures_abweichendeGeometrie_warnung() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		MultiLineString featureGeometry = GeometryTestdataProvider.createMultiLineString(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(0, 50)));
		String id = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, id),
				featureGeometry)));

		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(
				(LineString) featureGeometry.getGeometryN(0), 11, 0))
			.id(1L).build()).build();

		when(
			massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(any(), any())).thenReturn(
				List.of(massnahme));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasExactlyNetzbezugsHinweise(
				NetzbezugHinweis.ofWarnung(
					NetzbezugHinweisText.GEOMETRIEN_ABWEICHEND))
			.hasStatus(MassnahmenImportZuordnungStatus.ZUGEORDNET)
			.hasMassnahme(massnahme)
			.hasMassnahmeId(MassnahmeKonzeptID.of(id));
	}

	@Test
	void testLadeFeatures_neueMassnahmenId_statusNEU() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String id = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, id),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.doesNotHaveAnyFehler()
			.hasStatus(MassnahmenImportZuordnungStatus.NEU)
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(id));
	}

	@Test
	void testLadeFeatures_geloeschtFlagFalseNeueMassnahmenId_statusNEU() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String id = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, id,
					ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "nein"),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.doesNotHaveAnyFehler()
			.hasStatus(MassnahmenImportZuordnungStatus.NEU)
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(id));
	}

	@Test
	void testLadeFeatures_geloeschtFlagTrueNeueMassnahmenId_fehler() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String id = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, id,
					ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "ja"),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_NICHT_GEFUNDEN.getText()))
			.hasStatus(MassnahmenImportZuordnungStatus.GELOESCHT)
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(id));
	}

	@Test
	void testLadeFeatures_geloeschtFlagTrueVorhandeneMassnahmenId_statusGELOESCHT()
		throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String massnahmeId = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, massnahmeId,
					ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "ja"),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(1L).build();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(massnahmeId), session.getKonzeptionsquelle())).thenReturn(List.of(massnahme));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.doesNotHaveAnyFehler()
			.hasStatus(MassnahmenImportZuordnungStatus.GELOESCHT)
			.hasMassnahme(massnahme)
			.hasMassnahmeId(MassnahmeKonzeptID.of(massnahmeId));
	}

	@Test
	void testLadeFeatures_geloeschtFlagTrueVorhandeneMassnahmenIdAndRadNetzMassnahme_statusFehlerhaft()
		throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.RADNETZ_MASSNAHME,
			null);

		String massnahmeId = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, massnahmeId,
					ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "ja"),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(massnahmeId), session.getKonzeptionsquelle())).thenReturn(List.of(massnahme));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.LOESCHUNG_QUELLE_RADNETZ_UNGUELTIG.getText()))
			.hasStatus(MassnahmenImportZuordnungStatus.FEHLERHAFT)
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(massnahmeId));
	}

	@Test
	void testLadeFeatures_vorhandeneMassnahmenId_statusZUGEORDNET() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String massnahmeId = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, massnahmeId),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(1L).build();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(massnahmeId),
			session.getKonzeptionsquelle())).thenReturn(List.of(
				massnahme));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.doesNotHaveAnyFehler()
			.hasMassnahme(massnahme)
			.hasMassnahmeId(MassnahmeKonzeptID.of(massnahmeId))
			.hasStatus(MassnahmenImportZuordnungStatus.ZUGEORDNET);
	}

	@Test
	void testLadeFeatures_sollStandardGesetztVorhandeneMassnahmenId_statusZUGEORDNET()
		throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			SollStandard.BASISSTANDARD);

		String massnahmeId = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, massnahmeId),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(1L).build();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndSollStandardAndGeloeschtFalse(
			MassnahmeKonzeptID.of(massnahmeId),
			session.getKonzeptionsquelle(),
			session.getSollStandard().get())).thenReturn(List.of(massnahme));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.doesNotHaveAnyFehler()
			.hasMassnahme(massnahme)
			.hasMassnahmeId(MassnahmeKonzeptID.of(massnahmeId))
			.hasStatus(MassnahmenImportZuordnungStatus.ZUGEORDNET);
	}

	@Test
	void testLadeFeatures_doppeltVorhandeneMassnahmenId_statusZUGEORDNETUndFehler()
		throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String massnahmeId = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, massnahmeId),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		Massnahme massnahme1 = MassnahmeTestDataProvider.withDefaultValues().id(1L).build();
		Massnahme massnahme2 = MassnahmeTestDataProvider.withDefaultValues().id(2L).build();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(massnahmeId),
			session.getKonzeptionsquelle())).thenReturn(List.of(
				massnahme1, massnahme2));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_NICHT_EINDEUTIG.getText(2)))
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(massnahmeId))
			.hasStatus(MassnahmenImportZuordnungStatus.ZUGEORDNET);
	}

	@Test
	void testLadeFeatures_geloeschtFlagTrueDoppeltVorhandeneMassnahmenId_statusGELOESCHTUndFehler()
		throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String massnahmeId = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, massnahmeId,
					ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "ja"),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		Massnahme massnahme1 = MassnahmeTestDataProvider.withDefaultValues().id(1L).build();
		Massnahme massnahme2 = MassnahmeTestDataProvider.withDefaultValues().id(2L).build();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(massnahmeId),
			session.getKonzeptionsquelle())).thenReturn(List.of(
				massnahme1, massnahme2));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasExactlyMappingFehler(
				MappingFehler.of(
					ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_NICHT_EINDEUTIG.getText(2)))
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(massnahmeId))
			.hasStatus(MassnahmenImportZuordnungStatus.GELOESCHT);
	}

	@Test
	void testLadeFeatures_featureAusserhalbDesImportBereichs_ignored() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		String idInnerhalb = "M ID 4.0";
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			// innerhalb
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idInnerhalb),
				GeometryTestdataProvider.createMultiLineString(
					GeometryTestdataProvider.createLineString(new Coordinate(0, 1), new Coordinate(1, 2)))),
			// außerhalb
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "M ID 6.0"),
				GeometryTestdataProvider.createMultiLineString(
					GeometryTestdataProvider.createLineString(new Coordinate(102, 101), new Coordinate(201, 202))))));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getZuordnungen()).hasSize(1);
		MassnahmenImportZuordnung zuordnung = session.getZuordnungen().get(0);
		assertThatMassnahmenImportZuordnung(zuordnung)
			.doesNotHaveAnyFehler()
			.doesNotHaveAnyMassnahme()
			.hasMassnahmeId(MassnahmeKonzeptID.of(idInnerhalb))
			.hasStatus(MassnahmenImportZuordnungStatus.NEU);
	}

	@Test
	void testLadeFeatures_exceptionBeiVerarbeitung_ImportLogFehlerUnbekannt() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "M ID 4.0"),
				GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString()))));

		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of("M ID 4.0"),
			session.getKonzeptionsquelle())).thenThrow(
				new RuntimeException("RuntimeException in MassnahmeRepository for Testing"));

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).containsExactlyInAnyOrder(
			ImportLogEintrag.ofError("Es ist ein unbekannter Fehler bei der Verarbeitung aufgetreten."));
		assertThat(session.getZuordnungen()).isEmpty();
	}

	@Test
	void testLadeFeatures_exceptionBeiEinlesenDerGeoJSON_ImportLogFehlerUnbekannt() throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenThrow(
			new RuntimeException("RuntimeException in GeoJsonImportRepository for Testing"));

		when(
			massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(any(), any())).thenReturn(
				Collections.emptyList());

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).containsExactlyInAnyOrder(
			ImportLogEintrag.ofError("Es ist ein unbekannter Fehler beim Einlesen der GeoJSON aufgetreten."));
		assertThat(session.getZuordnungen()).isEmpty();
	}

	@Test
	void testLadeFeatures_ReadGeoJsonExceptionBeiValidierung_ImportLogFehlerMitValidierungsmeldung()
		throws ReadGeoJSONException {
		// Arrange
		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);

		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenThrow(
			new ReadGeoJSONException("Validation-Message"));

		when(
			massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(any(), any())).thenReturn(
				Collections.emptyList());

		// Act
		service.ladeFeatures(session, new byte[0]);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).containsExactlyInAnyOrder(
			ImportLogEintrag.ofError("Das Einlesen der GeoJSON-Daten ist fehlgeschlagen: Validation-Message"));
		assertThat(session.getZuordnungen()).isEmpty();
	}

	@Test
	void testAttributeValidieren_neueMassnahmenurPflichtattributeAusgewaehltalleEintraegeKorrekt_keineHinweise() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit).build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(
			massnahme);
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(
			quellAttribute);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = MassnahmenImportAttribute.getPflichtAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyFehler();
	}

	@Test
	void testAttributeValidieren_neueMassnahmealleAttributeAusgewaehltalleEintraegeKorrekt_keineHinweise() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit).build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(
			massnahme);
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(
			quellAttribute);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyFehler();
	}

	@Test
	void testAttributeValidieren_neueMassnahmekeineAttributeAusgewaehlt_hinweiseFuerPflichtattribute() {
		// Arrange
		Map<String, String> quellAttribute = new HashMap<>();
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(
			quellAttribute);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = Collections.emptyList();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);

		List<MappingFehler> erwarteteFehler = MassnahmenImportAttribute.getPflichtAttribute().stream()
			.map(attribut -> MappingFehler.of(
				attribut.toString(),
				MappingFehlermeldung.PFLICHTATTRIBUT_NICHT_AUSGEWAEHLT.getText(attribut.toString())))
			.toList();

		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasFehlerSize(erwarteteFehler.size())
			.hasExactlyInAnyOrderMappingFehler(erwarteteFehler);
	}

	@Test
	void testAttributeValidieren_neueMassanahmealleAttributeAusgewaehltquervalidierungenInkorrekt_hinweiseFuerQuervalidierungen() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit).build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(massnahme);
		quellAttribute.put(MassnahmenImportAttribute.UMSETZUNGSSTATUS.toString(), Umsetzungsstatus.PLANUNG.toString());
		quellAttribute.put(MassnahmenImportAttribute.KATEGORIEN.toString(),
			Set.of(
				Massnahmenkategorie.BARRIERE_SICHERN_BZW_PRUEFUNG_AUF_VERZICHT,
				Massnahmenkategorie.SONSTIGE_MASSNAHME_AN_BARRIERE)
				.stream()
				.map(Massnahmenkategorie::name)
				.collect(Collectors.joining(";")));
		quellAttribute.put(MassnahmenImportAttribute.DURCHFUEHRUNGSZEITRAUM.toString(), "");
		quellAttribute.put(MassnahmenImportAttribute.BAULASTTRAEGER.toString(), "");
		quellAttribute.put(MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER.toString(), "");
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(
			quellAttribute);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);

		List<MappingFehler> erwarteteFehler = getQuervalidierteAttribute().stream()
			.map(attribut -> MappingFehler.of(
				attribut.toString(),
				attribut == MassnahmenImportAttribute.KATEGORIEN
					? MappingFehlermeldung.QUERVALIDIERUNG_MASSNAHMENKATEGORIE.getText()
					: MappingFehlermeldung.QUERVALIDIERUNG_PFLICHTATTRIBUTE.getText(
						Umsetzungsstatus.PLANUNG)))
			.toList();

		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasFehlerSize(erwarteteFehler.size())
			.hasExactlyInAnyOrderMappingFehler(erwarteteFehler);
	}

	@Test
	void testAttributeValidieren_gemappteMassanahmekeineAttributeAusgewaehlt_keineHinweise() {
		// Arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().build();

		Map<String, String> quellAttribute = Collections.emptyMap();
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider
			.gemapptWithQuellAttributeAndMassnahme(
				quellAttribute, massnahme);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = Collections.emptyList();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyFehler();
	}

	@Test
	void testAttributeValidieren_gemappteMassanahmeattributeWerdenAusgelesen_keineHinweiseFuerQuervalidierungen() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit).build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Map<String, String> quellAttribute = Map.of(MassnahmenImportAttribute.UMSETZUNGSSTATUS.toString(),
			Umsetzungsstatus.PLANUNG.toString());
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider
			.gemapptWithQuellAttributeAndMassnahme(
				quellAttribute, massnahme);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = List.of(MassnahmenImportAttribute.UMSETZUNGSSTATUS);

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyFehler();
	}

	@Test
	void testAttributeValidieren_gemappteMassanahmepflichtfelderAbPlanungVergessen_hinweiseFuerQuervalidierungen() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit)
			.durchfuehrungszeitraum(null)
			.baulastZustaendiger(null)
			.handlungsverantwortlicher(null)
			.build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Map<String, String> quellAttribute = Map.of(MassnahmenImportAttribute.UMSETZUNGSSTATUS.toString(),
			Umsetzungsstatus.PLANUNG.toString());
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider
			.gemapptWithQuellAttributeAndMassnahme(
				quellAttribute, massnahme);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = List.of(MassnahmenImportAttribute.UMSETZUNGSSTATUS);

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);

		List<MappingFehler> erwarteteFehler = getQuervalidierteAttribute().stream()
			.filter(attribut -> attribut != MassnahmenImportAttribute.KATEGORIEN)
			.map(attribut -> MappingFehler.of(
				attribut.toString(),
				MappingFehlermeldung.QUERVALIDIERUNG_PFLICHTATTRIBUTE.getText(
					Umsetzungsstatus.PLANUNG)))
			.toList();

		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasFehlerSize(erwarteteFehler.size())
			.hasExactlyInAnyOrderMappingFehler(erwarteteFehler);
	}

	@Test
	void testAttributeValidieren_alleAttributeAusgewaehltkeineEintraegeVorhanden_hinweiseFuerPflichtattribute() {
		// Arrange
		Map<String, String> quellAttribute = new HashMap<>();
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(
			quellAttribute);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);

		List<MappingFehler> erwarteteFehler = MassnahmenImportAttribute.getPflichtAttribute().stream()
			.map(attribut -> MappingFehler.of(
				attribut.toString(),
				MappingFehlermeldung.PFLICHTATTRIBUT_NICHT_GESETZT.getText(attribut.toString())))
			.toList();

		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasFehlerSize(erwarteteFehler.size())
			.hasExactlyInAnyOrderMappingFehler(erwarteteFehler);
	}

	@Test
	void testAttributeValidieren_alleAttributeAusgewaehlteintraegeVorhandenAberLeer_hinweiseFuerPflichtattribute() {
		// Arrange
		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.withBlankValues();
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(
			quellAttribute);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);

		List<MappingFehler> erwarteteFehler = MassnahmenImportAttribute.getPflichtAttribute().stream()
			.map(attribut -> {
				String input = zuordnung.getFeature().getAttribute(attribut.toString()).toString();
				return MappingFehler.of(
					attribut.toString(),
					isVerwaltungseinheit(attribut) ? MappingFehlermeldung.VERWALTUNGSEINHEIT_NICHT_GEFUNDEN.getText(
						input) : MappingFehlermeldung.ATTRIBUT_WERT_UNGUELTIG.getText(input));
			})
			.toList();

		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasFehlerSize(erwarteteFehler.size())
			.hasExactlyInAnyOrderMappingFehler(erwarteteFehler);
	}

	@Test
	void testAttributeValidieren_alleAttributeAusgewaehlteintraegeVorhandenAberUngueltig_hinweiseFuerAlleAttribute() {
		// Arrange
		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.withIncorrectValues();
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(
			quellAttribute);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);

		List<MappingFehler> erwarteteFehler = getAlleAttribute().stream()
			.map(attribut -> {
				String input = zuordnung.getFeature().getAttribute(attribut.toString()).toString();
				return MappingFehler.of(
					attribut.toString(),
					isVerwaltungseinheit(attribut) ? MappingFehlermeldung.VERWALTUNGSEINHEIT_NICHT_GEFUNDEN.getText(
						input) : MappingFehlermeldung.ATTRIBUT_WERT_UNGUELTIG.getText(input));
			})
			.toList();

		assertThatMassnahmenImportZuordnung(zuordnung)
			.hasFehlerSize(erwarteteFehler.size())
			.hasExactlyInAnyOrderMappingFehler(erwarteteFehler);
	}

	@Test
	void testAttributeValidieren_geloeschteMassnahmeIgnoriert_keineHinweise() {
		// Arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().build();
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.geloeschtWithMassnahme(
			massnahme);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyFehler();
	}

	@Test
	void testAttributeValidieren_fehlerhafteMassnahmeIgnoriert_keineHinweise() {
		// Arrange
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.fehlerhaft();

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyFehler();
	}

	@Test
	void testAttributeValidieren_uneindeutigeZuordnung() {
		// Arrange
		// Maßnahme ist "null", da nicht eindeutig zuordenbar
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider
			.gemapptWithQuellAttributeAndMassnahme(new HashMap<>(), null);
		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);
		assertThatMassnahmenImportZuordnung(zuordnung).hasFehlerSize(0);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_selektionWirdUebernommen() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit).build();
		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(massnahme);

		MassnahmenImportSession session = getSessionKommunalesKonzept(
			MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(quellAttribute),
			MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(quellAttribute),
			MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(quellAttribute));

		Knoten knoten = KnotenTestDataProvider.withDefaultValues().build();
		MassnahmeNetzBezug netzbezug = NetzBezugTestDataProvider.forKnoten(knoten);
		session.getZuordnungen().get(0).aktualisiereNetzbezug(netzbezug, true);
		session.getZuordnungen().get(1).aktualisiereNetzbezug(netzbezug, true);
		session.getZuordnungen().get(2).aktualisiereNetzbezug(netzbezug, true);

		session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);
		// Vorbedingung, damit der Test im Assert aussagekräftig ist. Mit anderen Worten: Zuordnung 2 soll abgewählt
		// werden.
		assertThat(session.getZuordnungen().stream().allMatch(zuordnung -> zuordnung.isSelected()));

		// Act
		service.speichereMassnahmenDerZuordnungen(session,
			List.of(session.getZuordnungen().get(0).getId(), session.getZuordnungen().get(2).getId()));

		// Assert
		assertThat(session.getZuordnungen().get(0).isSelected()).isTrue();
		assertThat(session.getZuordnungen().get(1).isSelected()).isFalse();
		assertThat(session.getZuordnungen().get(2).isSelected()).isTrue();
		assertThat(session.isExecuting()).isFalse();
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_statusNeu_wirdErzeugtUndGespeichert() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Großoberkleinmitteluntenbach")
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(
			eq(verwaltungseinheit.getName()),
			eq(verwaltungseinheit.getOrganisationsArt())))
				.thenReturn(Optional.of(verwaltungseinheit));

		Massnahme expectedMassnahme = MassnahmeTestDataProvider.withDefaultValues()
			.zustaendiger(verwaltungseinheit)
			.baulastZustaendiger(verwaltungseinheit)
			.unterhaltsZustaendiger(verwaltungseinheit)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(
			expectedMassnahme);
		quellAttribute.put(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, expectedMassnahme
			.getMassnahmeKonzeptID().get().getValue());

		MassnahmenImportZuordnung zuordnung = new MassnahmenImportZuordnung(
			expectedMassnahme.getMassnahmeKonzeptID().get(),
			SimpleFeatureTestDataProvider.withAttributes(quellAttribute),
			null,
			MassnahmenImportZuordnungStatus.NEU);

		Knoten knoten = KnotenTestDataProvider.withDefaultValues().build();
		MassnahmeNetzBezug netzbezug = NetzBezugTestDataProvider.forKnoten(knoten);
		zuordnung.aktualisiereNetzbezug(netzbezug, true);

		MassnahmenImportSession session = getSession(Konzeptionsquelle.RADNETZ_MASSNAHME, zuordnung);

		session.setAttribute(List.of(MassnahmenImportAttribute.values()));
		session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);

		// Act
		service.speichereMassnahmenDerZuordnungen(session, List.of(session.getZuordnungen().get(0).getId()));

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();

		ArgumentCaptor<Massnahme> argument = ArgumentCaptor.forClass(Massnahme.class);
		verify(massnahmeRepository, atMostOnce()).save(argument.capture());
		Massnahme actualMassnahme = argument.getValue();
		assertMassnahmenAreEqual(actualMassnahme, expectedMassnahme,
			"id",
			"massnahmenPaketId",
			"originalRadNETZGeometrie",
			"netzbezug",
			"dokumentListe",
			"kommentarListe",
			"umsetzungsstand",
			"zuBenachrichtigendeBenutzer");
		assertThat(actualMassnahme.getUmsetzungsstand()).isPresent();
		assertThat(actualMassnahme.getUmsetzungsstand().get().getUmsetzungsstandStatus()).isEqualTo(
			UmsetzungsstandStatus.NEU_ANGELEGT);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_statusNeu_keinNetzbezug_wirdNichtGespeichert() {
		// Arrange
		MassnahmenImportSession session = createSessionWithDummyMassnahme(MassnahmenImportZuordnungStatus.NEU);

		session.getZuordnungen().get(0).aktualisiereNetzbezug(null, true);

		// Act
		service.speichereMassnahmenDerZuordnungen(session, List.of(session.getZuordnungen().get(0).getId()));

		// Assert
		assertThat(session.isExecuting()).isFalse();
		verify(massnahmeRepository, never()).save(any());
		assertThat(session.getLog()).isNotEmpty();
		assertThat(session.getLog().get(0).getSeverity()).isEqualTo(Severity.ERROR);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_statusNeu_attributfehler_wirdNichtGespeichert() {
		// Arrange
		MassnahmenImportSession session = createSessionWithDummyMassnahme(MassnahmenImportZuordnungStatus.NEU);

		session.getZuordnungen().get(0).addMappingFehler(MappingFehler.of("attributname", "fehlertext"));

		// Act
		service.speichereMassnahmenDerZuordnungen(session, List.of(session.getZuordnungen().get(0).getId()));

		// Assert
		assertThat(session.isExecuting()).isFalse();
		verify(massnahmeRepository, never()).save(any());
		assertThat(session.getLog()).isNotEmpty();
		assertThat(session.getLog().get(0).getSeverity()).isEqualTo(Severity.ERROR);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_statusNeu_netzbezugFehler_wirdNichtGespeichert() {
		// Arrange
		MassnahmenImportSession session = createSessionWithDummyMassnahme(MassnahmenImportZuordnungStatus.NEU);

		session.getZuordnungen().get(0)
			.addNetzbezugHinweis(NetzbezugHinweis.ofError(NetzbezugHinweisText.NETZBEZUG_UNVOLLSTAENDIG));

		// Act
		service.speichereMassnahmenDerZuordnungen(session, List.of(session.getZuordnungen().get(0).getId()));

		// Assert
		assertThat(session.isExecuting()).isFalse();
		verify(massnahmeRepository, never()).save(any());
		assertThat(session.getLog()).isNotEmpty();
		assertThat(session.getLog().get(0).getSeverity()).isEqualTo(Severity.ERROR);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_statusZugeordnet_wirdErzeugtUndGespeichert() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Massnahme massnahmeInDb = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit)
			.bezeichnung(Bezeichnung.of("Veraltete Bezeichnung"))
			.maViSID(MaViSID.of("veraltete mavis id"))
			.veroeffentlicht(false)
			.konzeptionsquelle(Konzeptionsquelle.KOMMUNALES_KONZEPT)
			.sonstigeKonzeptionsquelle(null)
			.build();

		MassnahmeNetzBezug neuerNetzbezug = NetzBezugTestDataProvider.forKnoten(KnotenTestDataProvider
			.withDefaultValues()
			.build());

		Massnahme expectedMassnahme = massnahmeInDb.toBuilder()
			.maViSID(MaViSID.of("tolle mavis id"))
			.veroeffentlicht(true)
			.umsetzungsstatus(massnahmeInDb.getUmsetzungsstatus())
			.bezeichnung(massnahmeInDb.getBezeichnung())
			.massnahmenkategorien(massnahmeInDb.getMassnahmenkategorien())
			.zustaendiger(massnahmeInDb.getZustaendiger().get())
			.sollStandard(massnahmeInDb.getSollStandard())
			.netzbezug(neuerNetzbezug)
			.build();

		MassnahmenImportSession session = createSessionFromMassnahmen(massnahmeInDb, expectedMassnahme,
			MassnahmenImportZuordnungStatus.ZUGEORDNET);

		// Act
		service.speichereMassnahmenDerZuordnungen(session, List.of(session.getZuordnungen().get(0).getId()));

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();
		verify(entityManager, atMostOnce()).merge(eq(session.getZuordnungen().get(0).getMassnahme().get()));

		ArgumentCaptor<Massnahme> argument = ArgumentCaptor.forClass(Massnahme.class);
		verify(massnahmeRepository, atMostOnce()).save(argument.capture());
		Massnahme actualMassnahme = argument.getValue();
		assertMassnahmenAreEqual(actualMassnahme, expectedMassnahme);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_sonstigeKonzeptionsquelle_neuWirdGespeichert() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		MassnahmeNetzBezug neuerNetzbezug = NetzBezugTestDataProvider.forKnoten(KnotenTestDataProvider
			.withDefaultValues()
			.build());

		Massnahme expectedMassnahme = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit)
			.id(null)
			.baulastZustaendiger(verwaltungseinheit)
			.unterhaltsZustaendiger(verwaltungseinheit)
			.zustaendiger(verwaltungseinheit)
			.bezeichnung(Bezeichnung.of("Sonstige Bezeichnung"))
			.veroeffentlicht(false)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.sonstigeKonzeptionsquelle("Manueller Import vom " + LocalDateTime.now().format(DateTimeFormatter.ofPattern(
				"dd.MM.yyyy")))
			.netzbezug(neuerNetzbezug)
			.build();

		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(
			expectedMassnahme);

		MassnahmenImportZuordnung zuordnung = new MassnahmenImportZuordnung(
			expectedMassnahme.getMassnahmeKonzeptID().get(),
			SimpleFeatureTestDataProvider.withAttributes(quellAttribute),
			null,
			MassnahmenImportZuordnungStatus.NEU);

		zuordnung.aktualisiereNetzbezug(neuerNetzbezug, true);

		MassnahmenImportSession session = getSession(Konzeptionsquelle.SONSTIGE, zuordnung);

		session.setAttribute(List.of(MassnahmenImportAttribute.values()));
		session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);

		// Act
		service.speichereMassnahmenDerZuordnungen(session, List.of(session.getZuordnungen().get(0).getId()));

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();

		ArgumentCaptor<Massnahme> argument = ArgumentCaptor.forClass(Massnahme.class);
		verify(massnahmeRepository, atMostOnce()).save(argument.capture());
		Massnahme actualMassnahme = argument.getValue();
		assertMassnahmenAreEqual(actualMassnahme, expectedMassnahme);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_sonstigeKonzeptionsquelle_bearbeitetWirdGespeichert() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Massnahme massnahmeInDb = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit)
			.bezeichnung(Bezeichnung.of("Sonstige Bezeichnung"))
			.veroeffentlicht(false)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.sonstigeKonzeptionsquelle("foobar quelle")
			.build();

		MassnahmeNetzBezug neuerNetzbezug = NetzBezugTestDataProvider.forKnoten(KnotenTestDataProvider
			.withDefaultValues()
			.build());

		Massnahme expectedMassnahme = massnahmeInDb.toBuilder()
			.veroeffentlicht(massnahmeInDb.getVeroeffentlicht())
			.umsetzungsstatus(massnahmeInDb.getUmsetzungsstatus())
			.konzeptionsquelle(massnahmeInDb.getKonzeptionsquelle())
			.sonstigeKonzeptionsquelle(massnahmeInDb.getSonstigeKonzeptionsquelle().get())
			.bezeichnung(Bezeichnung.of("Eine neue Bezeichnung"))
			.massnahmenkategorien(massnahmeInDb.getMassnahmenkategorien())
			.sollStandard(massnahmeInDb.getSollStandard())
			.netzbezug(neuerNetzbezug)
			.build();

		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(
			expectedMassnahme);
		quellAttribute.put(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, massnahmeInDb
			.getMassnahmeKonzeptID().get().getValue());

		MassnahmenImportZuordnung zuordnung = new MassnahmenImportZuordnung(
			MassnahmeKonzeptID.of(
				quellAttribute.getOrDefault(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "id")),
			SimpleFeatureTestDataProvider.withAttributes(quellAttribute),
			expectedMassnahme,
			MassnahmenImportZuordnungStatus.ZUGEORDNET);

		Knoten knoten = KnotenTestDataProvider.withDefaultValues().build();
		MassnahmeNetzBezug netzbezug = NetzBezugTestDataProvider.forKnoten(knoten);
		zuordnung.aktualisiereNetzbezug(netzbezug, true);

		MassnahmenImportSession session = getSession(Konzeptionsquelle.SONSTIGE, zuordnung);

		session.setAttribute(MassnahmenImportAttribute.getPflichtAttribute());
		session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);

		// Act
		service.speichereMassnahmenDerZuordnungen(session, List.of(session.getZuordnungen().get(0).getId()));

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();
		verify(entityManager, atMostOnce()).merge(eq(session.getZuordnungen().get(0).getMassnahme().get()));

		ArgumentCaptor<Massnahme> argument = ArgumentCaptor.forClass(Massnahme.class);
		verify(massnahmeRepository, atMostOnce()).save(argument.capture());
		Massnahme actualMassnahme = argument.getValue();
		assertMassnahmenAreEqual(actualMassnahme, expectedMassnahme);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_statusZugeordnet_beiExceptionBrichtNichtAllesAb() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Knoten knoten = KnotenTestDataProvider.withDefaultValues().build();
		MassnahmeNetzBezug netzbezug = NetzBezugTestDataProvider.forKnoten(knoten);

		// Massnahme 1 - Die wird eine Exception verursachen
		Massnahme massnahme1 = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit)
			.id(1L)
			.bezeichnung(Bezeichnung.of("Sehr bezeichnende Bezeichnung"))
			.maViSID(MaViSID.of("tolle mavis id 1"))
			.veroeffentlicht(true)
			.build();

		MassnahmenImportZuordnung zuordnung1 = MassnahmenImportZuordnungTestDataProvider
			.gemapptWithQuellAttributeAndMassnahme(
				MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(massnahme1),
				massnahme1);
		zuordnung1.aktualisiereNetzbezug(netzbezug, true);
		when(massnahmeRepository.save(massnahme1)).thenThrow(new OptimisticLockException(
			"Wurde in der zwischenzeit verändert"));

		// Massnahme 2 - Die wird ok, keine Exception
		Massnahme massnahme2 = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit)
			.id(2L)
			.massnahmeKonzeptId(MassnahmeKonzeptID.of("super_konzept_id 2"))
			.maViSID(MaViSID.of("tolle mavis id 2"))
			.veroeffentlicht(false)
			.build();

		MassnahmenImportZuordnung zuordnung2 = MassnahmenImportZuordnungTestDataProvider
			.gemapptWithQuellAttributeAndMassnahme(
				MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(massnahme2),
				massnahme2);
		zuordnung2.aktualisiereNetzbezug(netzbezug, true);

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung1, zuordnung2);

		ArrayList<MassnahmenImportAttribute> attribute = new ArrayList<>();
		attribute.addAll(MassnahmenImportAttribute.getPflichtAttribute());
		attribute.add(MassnahmenImportAttribute.MAVIS_ID);
		attribute.add(MassnahmenImportAttribute.VEROEFFENTLICHT);

		session.setAttribute(attribute);
		session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);

		// Act
		service.speichereMassnahmenDerZuordnungen(session,
			session.getZuordnungen().stream().map(z -> z.getId()).toList());

		// Assert
		assertThat(session.isExecuting()).isFalse();
		verify(entityManager, atMostOnce()).merge(eq(session.getZuordnungen().get(0).getMassnahme().get()));
		verify(entityManager, atMostOnce()).merge(eq(session.getZuordnungen().get(1).getMassnahme().get()));
		verify(massnahmeRepository, atMostOnce()).save(argThat((Massnahme massnahme) -> {
			return massnahme.getMaViSID().isPresent()
				&& massnahme.getMaViSID().get().getValue().equals("tolle mavis id 2")
				&& massnahme.getMassnahmeKonzeptID().isPresent()
				&& massnahme.getMassnahmeKonzeptID().get().getValue().equals("super_konzept_id 2")
				&& massnahme.getVeroeffentlicht();
		}));
		assertThat(session.getLog()).hasSize(1);
	}

	@Test
	void testMassnahmenDerZuordnungenSpeichern_statusGeloescht_wirdNurAlsGeloeschtMarkiert() {
		// Arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Massnahme massnahmeInDb = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit)
			.geloescht(false).build();
		MassnahmenImportSession session = createSessionFromMassnahmen(massnahmeInDb, massnahmeInDb,
			MassnahmenImportZuordnungStatus.GELOESCHT);

		// Act
		service.speichereMassnahmenDerZuordnungen(session, List.of(session.getZuordnungen().get(0).getId()));

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(massnahmeInDb.isGeloescht()).isTrue();
		assertThat(session.getLog()).isEmpty();
		verify(massnahmeRepository, atMostOnce()).save(eq(massnahmeInDb));
	}

	private @NotNull MassnahmenImportSession createSessionWithDummyMassnahme(
		MassnahmenImportZuordnungStatus zuordnungStatus) {
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		when(verwaltungseinheitRepository.findByNameAndOrganisationsArt(verwaltungseinheit.getName(),
			verwaltungseinheit.getOrganisationsArt())).thenReturn(
				Optional.of(verwaltungseinheit));

		Massnahme massnahme = null;
		if (zuordnungStatus != MassnahmenImportZuordnungStatus.NEU) {
			massnahme = MassnahmeTestDataProvider.withDefaultValuesAndOrganisation(verwaltungseinheit)
				.massnahmeKonzeptId(MassnahmeKonzeptID.of("super_konzept_id"))
				.maViSID(MaViSID.of("tolle mavis id"))
				.veroeffentlicht(true)
				.build();
		}

		return createSessionFromMassnahmen(massnahme, massnahme, zuordnungStatus);
	}

	private @NotNull MassnahmenImportSession createSessionFromMassnahmen(Massnahme existingMassnahmeInDb,
		Massnahme expectedMassnahmeAfterImport, MassnahmenImportZuordnungStatus zuordnungStatus) {
		Map<String, String> quellAttribute = MassnahmenImportAttributeMapTestDataProvider.dummyPflichtattribute();
		if (zuordnungStatus != MassnahmenImportZuordnungStatus.NEU) {
			quellAttribute = MassnahmenImportAttributeMapTestDataProvider.fromMassnahme(expectedMassnahmeAfterImport);
			quellAttribute.put(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, existingMassnahmeInDb
				.getMassnahmeKonzeptID().get().getValue());
		}

		MassnahmenImportZuordnung zuordnung = new MassnahmenImportZuordnung(
			MassnahmeKonzeptID.of(
				quellAttribute.getOrDefault(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "id")),
			SimpleFeatureTestDataProvider.withAttributes(quellAttribute),
			existingMassnahmeInDb,
			zuordnungStatus);

		if (zuordnungStatus != MassnahmenImportZuordnungStatus.GELOESCHT) {
			Knoten knoten = KnotenTestDataProvider.withDefaultValues().build();
			MassnahmeNetzBezug netzbezug = NetzBezugTestDataProvider.forKnoten(knoten);
			zuordnung.aktualisiereNetzbezug(netzbezug, true);
		}

		MassnahmenImportSession session = getSessionKommunalesKonzept(zuordnung);

		ArrayList<MassnahmenImportAttribute> attribute = new ArrayList<>();
		attribute.addAll(MassnahmenImportAttribute.getPflichtAttribute());
		attribute.add(MassnahmenImportAttribute.MAVIS_ID);
		attribute.add(MassnahmenImportAttribute.VEROEFFENTLICHT);

		session.setAttribute(attribute);
		session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);

		return session;
	}

	@NotNull
	private static MassnahmenImportSession getSessionKommunalesKonzept(MassnahmenImportZuordnung... zuordnungen) {
		Konzeptionsquelle konzeptionsquelle = Konzeptionsquelle.KOMMUNALES_KONZEPT;
		return getSession(konzeptionsquelle, zuordnungen);
	}

	private static @NotNull MassnahmenImportSession getSession(Konzeptionsquelle konzeptionsquelle,
		MassnahmenImportZuordnung... zuordnungen) {
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		MassnahmenImportSession session = new MassnahmenImportSession(
			benutzer,
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName",
			List.of(1L),
			konzeptionsquelle,
			null);
		session.setZuordnungen(List.of(zuordnungen));
		session.setSchritt(MassnahmenImportSession.ATTRIBUTE_AUSWAEHLEN);
		return session;
	}

	private static void assertMassnahmenAreEqual(Massnahme actualMassnahme, Massnahme expectedMassnahme,
		String... additionalFieldsToIgnore) {
		ArrayList<String> fieldsToIgnore = new ArrayList<>();
		fieldsToIgnore.addAll(List.of("version",
			"letzteAenderung",
			"benutzerLetzteAenderung"));
		fieldsToIgnore.addAll(Arrays.stream(additionalFieldsToIgnore).toList());

		Assertions.assertThat(actualMassnahme)
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Massnahme.class)
			.ignoringFields(fieldsToIgnore.toArray(new String[0]))
			.isEqualTo(expectedMassnahme);
	}

	private static void assertThatSessionHasCorrectState(MassnahmenImportSession session,
		List<MassnahmenImportAttribute> ausgewaehlteAttribute) {
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();
		assertThat(session.getAttribute()).containsExactlyInAnyOrderElementsOf(ausgewaehlteAttribute);
		assertThat(session.getSchritt()).isEqualTo(MassnahmenImportSession.ATTRIBUTFEHLER_UEBERPRUEFEN);
		assertThat(session.getZuordnungen()).hasSize(1);
	}

	private static List<MassnahmenImportAttribute> getAlleAttribute() {
		return List.of(MassnahmenImportAttribute.values());
	}

	private static List<MassnahmenImportAttribute> getQuervalidierteAttribute() {
		return List.of(MassnahmenImportAttribute.KATEGORIEN,
			MassnahmenImportAttribute.DURCHFUEHRUNGSZEITRAUM,
			MassnahmenImportAttribute.BAULASTTRAEGER,
			MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER);
	}

	private static boolean isVerwaltungseinheit(MassnahmenImportAttribute attribut) {
		return List.of(
			MassnahmenImportAttribute.ZUSTAENDIGER,
			MassnahmenImportAttribute.BAULASTTRAEGER,
			MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER).contains(attribut);
	}

	@Test
	void testErstelleNetzbezuege_alleZuordnungsstatusVorhanden_filtertKorrekt() {
		// Arrange
		Map<String, String> quellAttribute = new HashMap<>();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().build();
		List<MassnahmenImportZuordnung> zuordnungen = List.of(
			MassnahmenImportZuordnungTestDataProvider.neuWithQuellAttribute(quellAttribute),
			MassnahmenImportZuordnungTestDataProvider.gemapptWithQuellAttributeAndMassnahme(quellAttribute, massnahme),
			MassnahmenImportZuordnungTestDataProvider.geloeschtWithMassnahme(massnahme),
			MassnahmenImportZuordnungTestDataProvider.fehlerhaft());

		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName",
			List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);
		session.setZuordnungen(zuordnungen);
		session.setSchritt(MassnahmenImportSession.ATTRIBUTFEHLER_UEBERPRUEFEN);

		// Act
		service.erstelleNetzbezuege(session);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();
		assertThat(session.getSchritt()).isEqualTo(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);
		assertThat(session.getZuordnungen()).hasSize(4);

		ArgumentCaptor<MassnahmenImportZuordnung> zuordnungCaptor = ArgumentCaptor
			.forClass(MassnahmenImportZuordnung.class);
		verify(massnahmeNetzbezugService, times(2))
			.bestimmeNetzbezugDerZuordnung(zuordnungCaptor.capture(), any());
		assertThat(zuordnungCaptor.getAllValues()).hasSize(2)
			.allMatch(zuordnung -> zuordnung.getZuordnungStatus() == MassnahmenImportZuordnungStatus.ZUGEORDNET ||
				zuordnung.getZuordnungStatus() == MassnahmenImportZuordnungStatus.NEU);
	}

	@Test
	void testErstelleNetzbezuege_zugeordnetMitMappingFehlern_keinNetzbezugErzeugt() {
		// Arrange
		Map<String, String> quellAttribute = new HashMap<>();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().build();
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider
			.gemapptWithQuellAttributeAndMassnahme(
				quellAttribute, massnahme);
		zuordnung.addMappingFehler(MappingFehler.of("foobar", "fehler"));
		List<MassnahmenImportZuordnung> zuordnungen = List.of(zuordnung);

		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName",
			List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);
		session.setZuordnungen(zuordnungen);
		session.setSchritt(MassnahmenImportSession.ATTRIBUTFEHLER_UEBERPRUEFEN);

		// Act
		service.erstelleNetzbezuege(session);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();
		assertThat(session.getSchritt()).isEqualTo(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);
		assertThat(session.getZuordnungen()).hasSize(1);

		ArgumentCaptor<MassnahmenImportZuordnung> zuordnungCaptor = ArgumentCaptor.forClass(
			MassnahmenImportZuordnung.class);
		verify(massnahmeNetzbezugService, never()).bestimmeNetzbezugDerZuordnung(zuordnungCaptor.capture(), any());
	}

	@Test
	void testAktualisiereNetzbezug_netzbezugVorhanden_korrektGesetztUndKeineHinweise() {
		// Arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().build();
		MassnahmeNetzBezug alterNetzbezug = NetzBezugTestDataProvider.forKnoten(knoten);
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithNetzbezug(
			alterNetzbezug);
		NetzbezugHinweisText alterNetzbezugHinweisText = NetzbezugHinweisText.NETZBEZUG_UNVOLLSTAENDIG;
		zuordnung.getNetzbezugHinweise().add(NetzbezugHinweis.ofWarnung(alterNetzbezugHinweisText));
		List<MassnahmenImportZuordnung> zuordnungen = List.of(zuordnung);

		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName",
			List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);
		session.setZuordnungen(zuordnungen);
		session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);

		MassnahmeNetzBezug neuerNetzbezug = NetzBezugTestDataProvider.forKnoten(knoten);

		// Act
		service.aktualisiereNetzbezug(session, zuordnung.getId(), neuerNetzbezug);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();
		assertThat(session.getSchritt()).isEqualTo(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);
		assertThat(session.getZuordnungen()).hasSize(1);

		assertThat(zuordnung.getNetzbezug().get()).isSameAs(neuerNetzbezug);
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyHinweis();
	}

	@Test
	void testAktualisiereNetzbezug_netzbezugNichtVorhanden_korrektGesetztUndKorrekterHinweis() {
		// Arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().build();
		MassnahmeNetzBezug alterNetzbezug = NetzBezugTestDataProvider.forKnoten(knoten);
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithNetzbezug(
			alterNetzbezug);
		NetzbezugHinweisText alterNetzbezugHinweisText = NetzbezugHinweisText.NETZBEZUG_UNVOLLSTAENDIG;
		zuordnung.getNetzbezugHinweise().add(NetzbezugHinweis.ofWarnung(alterNetzbezugHinweisText));
		List<MassnahmenImportZuordnung> zuordnungen = List.of(zuordnung);

		MassnahmenImportSession session = new MassnahmenImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName",
			List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null);
		session.setZuordnungen(zuordnungen);
		session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);

		// Act
		service.aktualisiereNetzbezug(session, zuordnung.getId(), null);

		// Assert
		assertThat(session.isExecuting()).isFalse();
		assertThat(session.getLog()).isEmpty();
		assertThat(session.getSchritt()).isEqualTo(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);
		assertThat(session.getZuordnungen()).hasSize(1);

		NetzbezugHinweis neuerNetzbezugHinweis = NetzbezugHinweis.ofError(
			NetzbezugHinweisText.NETZBEZUG_NICHT_GEFUNDEN);

		assertThat(zuordnung.getNetzbezug()).isNotPresent();
		assertThatMassnahmenImportZuordnung(zuordnung).hasExactlyNetzbezugsHinweise(neuerNetzbezugHinweis);

	}
}