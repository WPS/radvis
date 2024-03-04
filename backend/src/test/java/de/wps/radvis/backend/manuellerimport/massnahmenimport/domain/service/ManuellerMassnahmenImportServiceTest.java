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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

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
import org.mockito.Mock;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportAttributeMapTestDataProvider;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnungTestDataProvider;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehler;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehlermeldung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportAttribute;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;

class ManuellerMassnahmenImportServiceTest {

	ManuellerMassnahmenImportService service;

	@Mock
	ManuellerImportService manuellerImportService;

	@Mock
	GeoJsonImportRepository geoJsonImportRepository;

	@Mock
	MassnahmeRepository massnahmeRepository;

	@Mock
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Mock
	EntityManager entityManager;

	@BeforeEach
	void setUp() {
		openMocks(this);
		when(entityManager.merge(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
		service = new ManuellerMassnahmenImportService(manuellerImportService, geoJsonImportRepository,
			verwaltungseinheitRepository, massnahmeRepository, entityManager, 10);

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
					geometry
				)
			)
		);

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
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(Stream.of(
				SimpleFeatureTestDataProvider.withGeometryAndAttributes(
					Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, " "),
					geometry
				)
			)
		);

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
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				),
				SimpleFeatureTestDataProvider.withGeometryAndAttributes(
					Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, doppelteId),
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

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
				//valid
				lineString
			),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idPoint),
				//valid
				point
			),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idMultiPolygon),
				//invalid
				multiPolygon
			),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idPolygon),
				//invalid
				polygon
			),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idMultiLineString),
				//valid
				multiLineString
			),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idMultiPoint),
				//valid
				multiPoint
			),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				//valid
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idGeometryCollection),
				geometryCollection
			),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idGeometryCollectionWithPolygon),
				//invalid
				GeometryTestdataProvider.creatGeometryCollection(multiPoint, polygon)
			),
			SimpleFeatureTestDataProvider.withGeometryAndAttributes(
				Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, idGeometryCollectionNested),
				//invalid
				GeometryTestdataProvider.creatGeometryCollection(multiPoint, multiLineString, geometryCollection)
			)
		);
		when(geoJsonImportRepository.readFeaturesFromByteArray(any())).thenReturn(features.stream());

		AtomicLong idSequence = new AtomicLong(1);

		Map<String, Massnahme> massnahmen = new HashMap<>();

		when(
			massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(any(), any())).thenAnswer(
			invocationOnMock ->
			{
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
					NetzbezugHinweis.ofWarnung(
						MappingFehlermeldung.FALSCHER_GEOMETRIE_TYP.getText(),
						MappingFehlermeldung.FALSCHER_GEOMETRIE_TYP.getText()))
				.hasStatus(MassnahmenImportZuordnungStatus.GEMAPPT)
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
				.hasStatus(MassnahmenImportZuordnungStatus.GEMAPPT)
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
					featureGeometry
				)
			)
		);

		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(
				(LineString) featureGeometry.getGeometryN(0), 11, 0)).id(1L).build()).build();

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
					MappingFehlermeldung.GEOMETRIEN_ABWEICHEND.getText(),
					MappingFehlermeldung.GEOMETRIEN_ABWEICHEND.getText()))
			.hasStatus(MassnahmenImportZuordnungStatus.GEMAPPT)
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
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

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
						ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "nein"
					), GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

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
						ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "ja"
					),
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

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
						ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "ja"
					),
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(1L).build();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(massnahmeId), session.getKonzeptionsquelle()
		)).thenReturn(List.of(massnahme));

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
	void testLadeFeatures_vorhandeneMassnahmenId_statusGEMAPPT() throws ReadGeoJSONException {
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
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

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
			.hasStatus(MassnahmenImportZuordnungStatus.GEMAPPT);
	}

	@Test
	void testLadeFeatures_sollStandardGesetztVorhandeneMassnahmenId_statusGEMAPPT()
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
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(1L).build();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndSollStandardAndGeloeschtFalse(
			MassnahmeKonzeptID.of(massnahmeId),
			session.getKonzeptionsquelle(),
			session.getSollStandard().get())
		).thenReturn(List.of(massnahme));

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
			.hasStatus(MassnahmenImportZuordnungStatus.GEMAPPT);
	}

	@Test
	void testLadeFeatures_doppeltVorhandeneMassnahmenId_statusGEMAPPTUndFehler()
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
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

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
			.hasStatus(MassnahmenImportZuordnungStatus.GEMAPPT);
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
						ManuellerMassnahmenImportService.GELOESCHT_ATTRIBUTENAME, "ja"
					), GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

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
						GeometryTestdataProvider.createLineString(new Coordinate(0, 1), new Coordinate(1, 2)))
				),
				// außerhalb
				SimpleFeatureTestDataProvider.withGeometryAndAttributes(
					Map.of(ManuellerMassnahmenImportService.MASSNAHME_ID_ATTRIBUTENAME, "M ID 6.0"),
					GeometryTestdataProvider.createMultiLineString(
						GeometryTestdataProvider.createLineString(new Coordinate(102, 101), new Coordinate(201, 202)))
				)
			)
		);

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
					GeometryTestdataProvider.createMultiLineString(GeometryTestdataProvider.createLineString())
				)
			)
		);

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

		MassnahmenImportSession session = getSession(zuordnung);
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

		MassnahmenImportSession session = getSession(zuordnung);
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

		MassnahmenImportSession session = getSession(zuordnung);
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

		MassnahmenImportSession session = getSession(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);

		List<MappingFehler> erwarteteFehler = getQuervalidierteAttribute().stream()
			.map(attribut -> MappingFehler.of(
				attribut.toString(),
				attribut == MassnahmenImportAttribute.KATEGORIEN ?
					MappingFehlermeldung.QUERVALIDIERUNG_MASSNAHMENKATEGORIE.getText() :
					MappingFehlermeldung.QUERVALIDIERUNG_PFLICHTATTRIBUTE.getText(
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
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.gemapptWithQuellAttributesAndMassnahme(
			quellAttribute, massnahme);

		MassnahmenImportSession session = getSession(zuordnung);
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
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.gemapptWithQuellAttributesAndMassnahme(
			quellAttribute, massnahme);

		MassnahmenImportSession session = getSession(zuordnung);
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
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.gemapptWithQuellAttributesAndMassnahme(
			quellAttribute, massnahme);

		MassnahmenImportSession session = getSession(zuordnung);
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

		MassnahmenImportSession session = getSession(zuordnung);
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

		MassnahmenImportSession session = getSession(zuordnung);
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
					isVerwaltungseinheit(attribut) ?
						MappingFehlermeldung.VERWALTUNGSEINHEIT_NICHT_GEFUNDEN.getText(input) :
						MappingFehlermeldung.ATTRIBUT_WERT_UNGUELTIG.getText(input));
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

		MassnahmenImportSession session = getSession(zuordnung);
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
					isVerwaltungseinheit(attribut) ?
						MappingFehlermeldung.VERWALTUNGSEINHEIT_NICHT_GEFUNDEN.getText(input) :
						MappingFehlermeldung.ATTRIBUT_WERT_UNGUELTIG.getText(input));
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

		MassnahmenImportSession session = getSession(zuordnung);
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

		MassnahmenImportSession session = getSession(zuordnung);
		List<MassnahmenImportAttribute> ausgewaehlteAttribute = getAlleAttribute();

		// Act
		service.attributeValidieren(session, ausgewaehlteAttribute);

		// Assert
		assertThatSessionHasCorrectState(session, ausgewaehlteAttribute);
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyFehler();
	}

	@NotNull
	private static MassnahmenImportSession getSession(MassnahmenImportZuordnung zuordnung) {
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		MassnahmenImportSession session = new MassnahmenImportSession(
			benutzer,
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100), "testBereichName", List.of(1L),
			Konzeptionsquelle.KOMMUNALES_KONZEPT,
			null
		);
		session.setZuordnungen(List.of(zuordnung));
		session.setSchritt(MassnahmenImportSession.ATTRIBUTE_AUSWAEHLEN);
		return session;
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
			MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER
		);
	}

	private static boolean isVerwaltungseinheit(MassnahmenImportAttribute attribut) {
		return List.of(
			MassnahmenImportAttribute.ZUSTAENDIGER,
			MassnahmenImportAttribute.BAULASTTRAEGER,
			MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER
		).contains(attribut);
	}
}