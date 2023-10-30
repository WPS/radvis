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

package de.wps.radvis.backend.fahrradroute.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.opengis.feature.simple.SimpleFeature;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.ShapeFileRepositoryImpl;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import lombok.NonNull;

class FahrradroutenTfisUpdateJobTest {
	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	FahrradrouteService fahrradrouteService;
	@Mock
	FahrradrouteRepository fahrradrouteRepository;
	@Mock
	ShapeFileRepositoryImpl shapeFileRepository;
	@Mock
	KantenRepository kantenRepository;
	@Mock
	TfisImportService tfisImportService;

	@TempDir
	Path tfisRadwegePath = Path.of("testpfad");

	FahrradroutenTfisUpdateJob job;

	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95)));

	@Captor
	private ArgumentCaptor<Set<TfisId>> notToDeleteSetCaptor;

	@BeforeEach
	void setup() throws IOException, KeinMatchGefundenException {
		MockitoAnnotations.openMocks(this);
		new File(tfisRadwegePath.toString(), "test.shp").createNewFile();

		job = new FahrradroutenTfisUpdateJob(jobExecutionDescriptionRepository, tfisImportService, kantenRepository,
			shapeFileRepository, tfisRadwegePath, fahrradrouteRepository);

		when(shapeFileRepository.transformGeometryToUTM32(any())).thenAnswer(
			((Answer<SimpleFeature>) invocation -> this.transformGeometryToUTM32(
				(SimpleFeature) invocation.getArguments()[0])));
		when(tfisImportService.isGeometryInBW(any())).thenReturn(true);
		when(tfisImportService.sindAttributeProGruppeEindeutig(any())).thenReturn(true);
		when(fahrradrouteRepository.save(any())) // identity
			.thenAnswer((Answer<Fahrradroute>) invocation -> (Fahrradroute) invocation.getArguments()[0]);
		when(tfisImportService.routeProfil(any())).thenReturn(new ProfilMatchResult(null,
			Collections.emptyList(), Collections.emptyList()));

		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	void updatesOnlyImportedWithoutLinestring()
		throws ShapeProjectionException, IOException, KeinMatchGefundenException {
		// arrange
		String tfisIdZuImportieren = "tfisID_0";
		SimpleFeature simpleFeature1 = createNichtVariantenFeature(tfisIdZuImportieren,
			"eins name",
			Map.of(),
			new Coordinate(10, 0), new Coordinate(10, 100));
		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		String tfisIdNichtZuImportieren = "tfisID_1";
		SimpleFeature simpleFeature2 = createNichtVariantenFeature(tfisIdNichtZuImportieren,
			"eins name",
			Map.of(),
			new Coordinate(10, 100), new Coordinate(200, 100));

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1, simpleFeature2))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2)); // Steam wird zwei mal durchgegangen

		// matching faken
		String dlmId1 = "DLM1";
		String dlmId2 = "DLM2";
		String dlmId3 = "DLM3";

		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 0), QuellSystem.DLM).id(1L)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 100), QuellSystem.DLM).id(2L)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 100), QuellSystem.DLM)
			.id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 400), QuellSystem.DLM)
			.id(4L).build();

		Kante netzbezugKante1 = KanteTestDataProvider.fromKnoten(knoten1, knoten2).id(1L)
			.dlmId(DlmId.of(dlmId1)).build();
		Kante netzbezugKante2 = KanteTestDataProvider.fromKnoten(knoten2, knoten3)
			.id(2L)
			.dlmId(DlmId.of(dlmId2)).build();
		Kante netzbezugKante3 = KanteTestDataProvider.fromKnoten(knoten3, knoten4)
			.id(3L)
			.dlmId(DlmId.of(dlmId3)).build();

		List<Kante> netzbezug = new ArrayList<>(List.of(netzbezugKante3, netzbezugKante2, netzbezugKante1));
		when(kantenRepository.findAllByDlmIdIn(any()))
			.thenReturn(netzbezug);

		when(fahrradrouteRepository.findAllTfisIdsWithoutNetzbezugLineString())
			.thenReturn(Set.of(TfisId.of(tfisIdZuImportieren)));
		Fahrradroute fahrradrouteToUpdate = FahrradrouteTestDataProvider.withDefaultValues()
			.build();
		when(fahrradrouteRepository.findByTfisId(any()))
			.thenReturn(Optional.of(fahrradrouteToUpdate));

		List<LinearReferenzierteProfilEigenschaften> profilEigenschaften = List
			.of(new LinearReferenzierteProfilEigenschaften(
				FahrradrouteProfilEigenschaften.of(BelagArt.ASPHALT, Radverkehrsfuehrung.BEGEGNUNBSZONE),
				LinearReferenzierterAbschnitt.of(0.0, 1.0)));

		when(tfisImportService.routeProfil(any()))
			.thenAnswer(
				invocation -> new ProfilMatchResult((LineString) invocation.getArguments()[0],
					List.of(OsmWayId.of(3L), OsmWayId.of(2L), OsmWayId.of(1L)),
					profilEigenschaften));

		when(kantenRepository.findAllById(List.of(3L, 2L, 1L))).thenReturn(List.of(
			netzbezugKante1, netzbezugKante2, netzbezugKante3));

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);
		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of(dlmId1, dlmId2, dlmId3));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn(dlmId1);
		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();
		when(tfisImportService.extractStartpunkt(simpleFeature1)).thenReturn(Optional.of(
			GeometryTestdataProvider.createPoint(knoten4.getKoordinate())));

		// act
		job.doRun();

		// assert
		ArgumentCaptor<Fahrradroute> savedFahrradrouteCaptor = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteRepository, times(1)).save(savedFahrradrouteCaptor.capture());
		ArgumentCaptor<TfisId> loadedTfisIdCaptor = ArgumentCaptor.forClass(TfisId.class);
		verify(fahrradrouteRepository, times(1)).findByTfisId(loadedTfisIdCaptor.capture());
		assertThat(savedFahrradrouteCaptor.getValue()).isEqualTo(fahrradrouteToUpdate);
		assertThat(loadedTfisIdCaptor.getValue().getTfisId()).isEqualTo(tfisIdZuImportieren);
		assertThat(fahrradrouteToUpdate.getOriginalGeometrie()).isNotEmpty();
		assertThat(fahrradrouteToUpdate.getOriginalGeometrie().get().getCoordinates()).containsExactly(
			geometry1.getCoordinates());
		assertThat(fahrradrouteToUpdate.getNetzbezugLineString()).isNotEmpty();
		assertThat(fahrradrouteToUpdate.getAbschnittsweiserKantenBezug())
			.extracting(AbschnittsweiserKantenBezug::getKante)
			.containsExactlyElementsOf(netzbezug);
		assertThat(fahrradrouteToUpdate.getLinearReferenzierteProfilEigenschaften()).isEqualTo(profilEigenschaften);
		assertThat(fahrradrouteToUpdate.getStuetzpunkte()).isPresent();
		assertThat(fahrradrouteToUpdate.getStuetzpunkte().get().getCoordinates()).containsExactly(
			new Coordinate(200, 400),
			new Coordinate(200, 250),
			new Coordinate(105, 100),
			new Coordinate(10, 50),
			new Coordinate(10, 0));
	}

	@Test
	void updatesNotWhenNoNetzbezugLineString()
		throws ShapeProjectionException, IOException, KeinMatchGefundenException {
		// arrange
		String tfisIdZuImportieren = "tfisID_0";
		SimpleFeature simpleFeature1 = createNichtVariantenFeature(tfisIdZuImportieren,
			"eins name",
			Map.of(),
			new Coordinate(10, 20), new Coordinate(10, 100));
		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1)); // Steam wird zwei mal durchgegangen

		when(fahrradrouteRepository.findAllTfisIdsWithoutNetzbezugLineString())
			.thenReturn(Set.of(TfisId.of(tfisIdZuImportieren)));

		// matching faken
		String dlmId1 = "DLM1";
		String dlmId2 = "DLM2";
		String dlmId3 = "DLM3";

		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 20), QuellSystem.DLM).id(1L)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 100), QuellSystem.DLM).id(2L)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 100), QuellSystem.DLM)
			.id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 400), QuellSystem.DLM)
			.id(4L).build();

		Kante netzbezugKante1 = KanteTestDataProvider.fromKnoten(knoten1, knoten2).id(1L)
			.dlmId(DlmId.of(dlmId1)).build();
		Kante netzbezugKante3 = KanteTestDataProvider.fromKnoten(knoten3, knoten4)
			.id(3L)
			.dlmId(DlmId.of(dlmId3)).build();
		List<Kante> netzbezug = new ArrayList<>(List.of(netzbezugKante1, netzbezugKante3));
		when(kantenRepository.findAllByDlmIdIn(any()))
			.thenReturn(netzbezug);

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);
		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of(dlmId1, dlmId2, dlmId3));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn(dlmId1);
		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();
		when(tfisImportService.extractStartpunkt(simpleFeature1)).thenReturn(Optional.of(
			GeometryTestdataProvider.createPoint(knoten4.getKoordinate())));

		// act
		job.doRun();

		// assert
		verify(fahrradrouteRepository, never()).save(any());
		verify(fahrradrouteRepository, never()).findByTfisId(any());
	}

	private SimpleFeature transformGeometryToUTM32(SimpleFeature simpleFeature) {
		require(simpleFeature.getDefaultGeometry(), notNullValue());

		Geometry UTM32Geometry = coordinateReferenceSystemConverter.transformGeometry(
			(Geometry) simpleFeature.getDefaultGeometry(),
			KoordinatenReferenzSystem.ETRS89_UTM32_N);

		simpleFeature.setDefaultGeometry(UTM32Geometry);
		setzeSRIDAufGeometrie(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid())
			.accept(simpleFeature);
		return simpleFeature;
	}

	@NonNull
	private Consumer<SimpleFeature> setzeSRIDAufGeometrie(int SRID) {
		return feature -> {
			Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
			if (defaultGeometry != null) {
				defaultGeometry.setSRID(SRID);
				if (defaultGeometry instanceof GeometryCollection) {
					for (int i = 0; i < defaultGeometry.getNumGeometries(); i++) {
						defaultGeometry.getGeometryN(i).setSRID(SRID);
					}
				}
			}
		};
	}

	private SimpleFeature createNichtVariantenFeature(String objID, String name, Map<String, String> attributes,
		Coordinate... coordinates) {
		HashMap<String, String> alleAttribute = new HashMap<>();
		alleAttribute.put("art", "1000");
		alleAttribute.put("objid", objID);
		alleAttribute.put("nam", name);
		alleAttribute.putAll(attributes);
		return SimpleFeatureTestDataProvider.withMultiLineStringAndAttributes(alleAttribute, coordinates);
	}
}
