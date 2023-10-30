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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
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
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.ShapeFileRepositoryImpl;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import lombok.NonNull;

class LandesradfernwegeTfisImportJobTest {
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

	LandesradfernwegeTfisImportJob job;

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

		job = new LandesradfernwegeTfisImportJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			shapeFileRepository, kantenRepository, tfisImportService, tfisRadwegePath);

		when(shapeFileRepository.transformGeometryToUTM32(any())).thenAnswer(
			((Answer<SimpleFeature>) invocation -> this.transformGeometryToUTM32(
				(SimpleFeature) invocation.getArguments()[0])));
		when(tfisImportService.isGeometryInBW(any())).thenReturn(true);
		when(tfisImportService.sindAttributeProGruppeEindeutig(any())).thenReturn(true);
		when(fahrradrouteRepository.save(any())) // identity
			.thenAnswer((Answer<Fahrradroute>) invocation -> (Fahrradroute) invocation.getArguments()[0]);
		when(tfisImportService.routeProfil(any())).thenReturn(new ProfilMatchResult(
			GeometryTestdataProvider.createLineString(),
			Collections.emptyList(), Collections.emptyList()));

		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	void testeCreatesNewFahrradrouten_aberNurLRFW()
		throws ShapeProjectionException, IOException, KeinMatchGefundenException {
		// arrange

		SimpleFeature simpleFeature1_lrfw = createNichtVariantenFeature("tfisID_0",
			"eins name",
			Map.of(),
			new Coordinate(10, 10), new Coordinate(20, 10));

		SimpleFeature simpleFeature2_tfisNormal = createNichtVariantenFeature("tfisID_1",
			"zwei name",
			Map.of(),
			new Coordinate(10, 50), new Coordinate(20, 50));

		SimpleFeature simpleFeature3_stichweg = createNichtVariantenFeature("tfisID_2",
			"drei name",
			Map.of(),
			new Coordinate(10, 100), new Coordinate(20, 100));

		MultiLineString geometry1 = (MultiLineString) simpleFeature1_lrfw.getDefaultGeometry();

		when(shapeFileRepository.readShape(any()))
			.thenReturn(Stream.of(simpleFeature1_lrfw, simpleFeature2_tfisNormal, simpleFeature3_stichweg))
			.thenReturn(Stream.of(simpleFeature1_lrfw, simpleFeature2_tfisNormal, simpleFeature3_stichweg))
			.thenReturn(Stream.of(simpleFeature1_lrfw, simpleFeature2_tfisNormal,
				simpleFeature3_stichweg)); // Steam wird zwei mal durchgegangen

		when(tfisImportService.isLandesradfernweg(simpleFeature1_lrfw)).thenReturn(true);
		when(tfisImportService.isLandesradfernweg(simpleFeature2_tfisNormal)).thenReturn(false);
		when(tfisImportService.isLandesradfernweg(simpleFeature3_stichweg)).thenReturn(false);

		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1_lrfw)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature2_tfisNormal)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature3_stichweg)).thenReturn(false);

		when(tfisImportService.extractDlmIds(List.of(simpleFeature1_lrfw))).thenReturn(Set.of("ID 0"));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1_lrfw)))
			.thenReturn(geometry1);
		Kante netzbezugKante = KanteTestDataProvider.withDefaultValues().id(0L).dlmId(DlmId.of("ID 0"))
			.geometry((LineString) geometry1.getGeometryN(0))
			.build();
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(kantenRepository.findAllByDlmIdIn(List.of(DlmId.of("ID 0"))))
			.thenReturn(new ArrayList<>(List.of(netzbezugKante)));

		when(tfisImportService.extractDlmId(simpleFeature1_lrfw)).thenReturn("ID 0");
		when(tfisImportService.routeProfil(any())).thenReturn(
			new ProfilMatchResult(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(20, 10)),
				List.of(OsmWayId.of(0L)),
				List.of() // Die Profileigenschaften werden in diesem Test nicht gebraucht
			));
		when(kantenRepository.findAllById(List.of(0L))).thenReturn(List.of(netzbezugKante));

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1_lrfw)).thenReturn(Kategorie.LANDESRADFERNWEG);
		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.empty());
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1_lrfw)).thenReturn("");
		when(tfisImportService.extractBeschreibung(simpleFeature1_lrfw)).thenReturn("");
		when(tfisImportService.extractInfo(simpleFeature1_lrfw)).thenReturn("");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1_lrfw)).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(simpleFeature1_lrfw)).thenCallRealMethod();

		// act
		job.doRun();

		// assert
		ArgumentCaptor<Fahrradroute> captor = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteRepository).save(captor.capture());

		List<Fahrradroute> values = captor.getAllValues();
		assertThat(values.size()).isEqualTo(1);
		Fahrradroute erstellteFahrradroute = values.get(0);
		assertThat(erstellteFahrradroute.getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.RADVIS_ROUTE);
		assertThat(erstellteFahrradroute.getTfisId()).isEqualTo(TfisId.of("tfisID_0"));
		assertThat(erstellteFahrradroute.getOriginalGeometrie()).contains(geometry1);
		assertThat(erstellteFahrradroute.getStuetzpunkte()).isPresent();
		assertThat(erstellteFahrradroute.getStuetzpunkte().get().getCoordinates()).containsExactly(
			new Coordinate(10.0, 10.0),
			new Coordinate(15.0, 10.0),
			new Coordinate(20.0, 10.0));
	}

	@Test
	void testeCreatesNewFahrradrouten_netzbezugKantenIds_inRichtigerReihenfolge()
		throws ShapeProjectionException, IOException, KeinMatchGefundenException {
		// arrange

		Knoten k1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.id(1L).build();
		Knoten k2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
			.id(2L).build();
		Knoten k3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM)
			.id(3L).build();
		Knoten k4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(40, 10), QuellSystem.DLM)
			.id(4L).build();

		SimpleFeature lrfw_f1 = createNichtVariantenFeature("tfisID_0",
			"feature1",
			Map.of(),
			k1.getKoordinate(), k2.getKoordinate());
		SimpleFeature lrfw_f2 = createNichtVariantenFeature("tfisID_0",
			"feature2",
			Map.of(),
			k3.getKoordinate(), k4.getKoordinate());
		SimpleFeature lrfw_f3 = createNichtVariantenFeature("tfisID_0",
			"feature3",
			Map.of(),
			k2.getKoordinate(), k3.getKoordinate());

		DlmId dlmId1 = DlmId.of("ID 1");
		DlmId dlmId2 = DlmId.of("ID 2");
		DlmId dlmId3 = DlmId.of("ID 3");

		Kante kante1 = KanteTestDataProvider.fromKnoten(k1, k2)
			.id(0L).dlmId(dlmId1)
			.build();
		Kante kante2 = KanteTestDataProvider.fromKnoten(k3, k4)
			.id(1L).dlmId(dlmId2)
			.build();
		Kante kante3 = KanteTestDataProvider.fromKnoten(k2, k3)
			.id(2L).dlmId(dlmId3)
			.build();

		MultiLineString gesamtGeometrie = GeometryTestdataProvider.createMultiLineString(
			kante1.getGeometry(), kante2.getGeometry(), kante3.getGeometry());

		when(shapeFileRepository.readShape(any()))
			.thenReturn(Stream.of(lrfw_f1, lrfw_f2, lrfw_f3))
			.thenReturn(Stream.of(lrfw_f1, lrfw_f2, lrfw_f3))
			.thenReturn(Stream.of(lrfw_f1, lrfw_f2, lrfw_f3));

		when(tfisImportService.isLandesradfernweg(any())).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(any())).thenReturn(true);

		when(tfisImportService.extractDlmIds(any()))
			.thenReturn(Set.of(dlmId1.getValue(), dlmId2.getValue(), dlmId3.getValue()));
		when(tfisImportService.konstruiereOriginalGeometrie(any()))
			.thenReturn(gesamtGeometrie);
		when(tfisImportService.anteilInBW(gesamtGeometrie)).thenReturn(1.);
		when(kantenRepository.findAllByDlmIdIn(any()))
			.thenReturn(new ArrayList<>(List.of(kante1, kante2, kante3)));

		when(tfisImportService.extractDlmId(lrfw_f1)).thenReturn(dlmId1.getValue());
		when(tfisImportService.extractDlmId(lrfw_f2)).thenReturn(dlmId2.getValue());
		when(tfisImportService.extractDlmId(lrfw_f3)).thenReturn(dlmId3.getValue());

		when(tfisImportService.routeProfil(any())).thenReturn(
			new ProfilMatchResult(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(40, 10)),
				List.of(OsmWayId.of(0L), OsmWayId.of(2L), OsmWayId.of(1L)),
				List.of() // Die Profileigenschaften werden in diesem Test nicht gebraucht
			));

		when(kantenRepository.findAllById(List.of(0L, 2L, 1L))).thenReturn(List.of(kante1, kante3, kante2));

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(any())).thenReturn(Kategorie.LANDESRADFERNWEG);
		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.empty());
		when(tfisImportService.extractKurzbeschreibung(any())).thenReturn("");
		when(tfisImportService.extractBeschreibung(any())).thenReturn("");
		when(tfisImportService.extractInfo(any())).thenReturn("");
		when(tfisImportService.extractOffizielleLaenge(any())).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(any())).thenCallRealMethod();

		// act
		job.doRun();

		// assert
		ArgumentCaptor<Fahrradroute> captor = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteRepository).save(captor.capture());

		List<Fahrradroute> values = captor.getAllValues();
		assertThat(values.size()).isEqualTo(1);
		Fahrradroute erstellteFahrradroute = values.get(0);
		assertThat(erstellteFahrradroute.getAbschnittsweiserKantenBezug().get(0).getKante().getId()).isEqualTo(0L);
		assertThat(erstellteFahrradroute.getAbschnittsweiserKantenBezug().get(1).getKante().getId()).isEqualTo(2L);
		assertThat(erstellteFahrradroute.getAbschnittsweiserKantenBezug().get(2).getKante().getId()).isEqualTo(1L);

		assertThat(erstellteFahrradroute.getStuetzpunkte()).isPresent();
		assertThat(erstellteFahrradroute.getStuetzpunkte().get().getCoordinates()).containsExactly(
			new Coordinate(10.0, 10.0),
			new Coordinate(15.0, 10.0),
			new Coordinate(25.0, 10.0),
			new Coordinate(35.0, 10.0),
			new Coordinate(40.0, 10.0));
	}

	@Test
	void teste_importFromTfis_NetzbezugVonMatchGeometryUndDetailsVerwenden()
		throws ShapeProjectionException, IOException, KeinMatchGefundenException {
		// arrange
		Knoten k1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.id(1L).build();
		Knoten k2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM)
			.id(2L).build();

		SimpleFeature lrfw_f1 = createNichtVariantenFeature("tfisID_0",
			"feature1",
			Map.of(),
			k1.getKoordinate(), k2.getKoordinate());

		DlmId dlmId1 = DlmId.of("ID 1");

		Kante kante1 = KanteTestDataProvider.fromKnoten(k1, k2)
			.id(0L).dlmId(dlmId1)
			.build();

		MultiLineString gesamtGeometrie = GeometryTestdataProvider.createMultiLineString(
			kante1.getGeometry());

		when(shapeFileRepository.readShape(any())).thenAnswer(invocationOnMock -> Stream.of(lrfw_f1));

		when(tfisImportService.isLandesradfernweg(any())).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(any())).thenReturn(true);

		when(tfisImportService.extractDlmIds(any()))
			.thenReturn(Set.of(dlmId1.getValue()));
		when(tfisImportService.konstruiereOriginalGeometrie(any()))
			.thenReturn(gesamtGeometrie);
		when(tfisImportService.anteilInBW(gesamtGeometrie)).thenReturn(1.);
		when(kantenRepository.findAllByDlmIdIn(any()))
			.thenReturn(new ArrayList<>(List.of(kante1)));

		when(tfisImportService.extractDlmId(lrfw_f1)).thenReturn(dlmId1.getValue());

		when(tfisImportService.routeProfil(any())).thenReturn(
			new ProfilMatchResult(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(40, 10)),
				List.of(OsmWayId.of(0L)),
				List.of() // Die Profileigenschaften werden in diesem Test nicht gebraucht
			));

		when(kantenRepository.findAllById(List.of(0L))).thenReturn(List.of(kante1));

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(any())).thenReturn(Kategorie.LANDESRADFERNWEG);
		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.empty());
		when(tfisImportService.extractKurzbeschreibung(any())).thenReturn("");
		when(tfisImportService.extractBeschreibung(any())).thenReturn("");
		when(tfisImportService.extractInfo(any())).thenReturn("");
		when(tfisImportService.extractOffizielleLaenge(any())).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(any())).thenCallRealMethod();

		// act
		job.doRun();

		// assert
		ArgumentCaptor<Fahrradroute> captor = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteRepository).save(captor.capture());

		List<Fahrradroute> values = captor.getAllValues();
		assertThat(values.size()).isEqualTo(1);
		Fahrradroute erstellteFahrradroute = values.get(0);
		assertThat(erstellteFahrradroute.getAbschnittsweiserKantenBezug().size()).isEqualTo(1);
		assertThat(erstellteFahrradroute.getAbschnittsweiserKantenBezug().get(0).getKante().getId()).isEqualTo(0L);
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
