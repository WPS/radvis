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
import static org.mockito.ArgumentMatchers.eq;
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

import org.geotools.api.feature.simple.SimpleFeature;
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

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.ShapeFileRepositoryImpl;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
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

class FahrradroutenTfisImportJobTest {
	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95)));
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

	FahrradroutenTfisImportJob job;

	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@Captor
	private ArgumentCaptor<Set<TfisId>> notToDeleteSetCaptor;

	@BeforeEach
	void setup() throws IOException, KeinMatchGefundenException {
		MockitoAnnotations.openMocks(this);
		new File(tfisRadwegePath.toString(), "test.shp").createNewFile();

		job = new FahrradroutenTfisImportJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			shapeFileRepository, kantenRepository, tfisImportService, tfisRadwegePath);

		when(shapeFileRepository.transformGeometryToUTM32(any())).thenAnswer(
			((Answer<SimpleFeature>) invocation -> this.transformGeometryToUTM32(
				(SimpleFeature) invocation.getArguments()[0])));
		when(tfisImportService.isGeometryInBW(any())).thenReturn(true);
		when(tfisImportService.sindAttributeProGruppeEindeutig(any())).thenReturn(true);
		when(fahrradrouteRepository.save(any())) // identity
			.thenAnswer((Answer<Fahrradroute>) invocation -> (Fahrradroute) invocation.getArguments()[0]);
		when(tfisImportService.routeProfil(any())).thenReturn(
			new ProfilMatchResult(GeometryTestdataProvider.createLineString(),
				Collections.emptyList(), Collections.emptyList()));

		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	void testeCreatesNewFahrradrouten_aberNurNichtLRFWUndNichtVarianten() throws ShapeProjectionException, IOException {
		// arrange

		SimpleFeature simpleFeature1 = createNichtVariantenFeature("tfisID_0",
			"eins name",
			Map.of(),
			new Coordinate(10, 10), new Coordinate(20, 10));

		SimpleFeature simpleFeature2 = createNichtVariantenFeature("tfisID_1",
			"zwei name",
			Map.of(),
			new Coordinate(10, 50), new Coordinate(20, 50));

		SimpleFeature simpleFeature3 = createNichtVariantenFeature("tfisID_2",
			"drei name",
			Map.of(),
			new Coordinate(10, 100), new Coordinate(20, 100));

		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3)); // Steam wird zwei mal durchgegangen

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isLandesradfernweg(simpleFeature2)).thenReturn(true);
		when(tfisImportService.isLandesradfernweg(simpleFeature3)).thenReturn(false);

		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature2)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature3)).thenReturn(false);

		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of("ID 0"));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		Kante netzbezugKante = KanteTestDataProvider.withDefaultValues().id(0L).dlmId(DlmId.of("ID 0"))
			.geometry((LineString) geometry1.getGeometryN(0))
			.build();
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(kantenRepository.findAllByDlmIdIn(List.of(DlmId.of("ID 0"))))
			.thenReturn(new ArrayList<>(List.of(netzbezugKante)));

		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn("ID 0");

		// da keine nicht gemappten features können wir uns ein paar mocks sparen

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);
		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.empty());
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();

		// act
		job.doRun();

		// assert
		ArgumentCaptor<Fahrradroute> captor = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteRepository).save(captor.capture());

		List<Fahrradroute> values = captor.getAllValues();
		assertThat(values.size()).isEqualTo(1);
		Fahrradroute erstellteFahrradroute = values.get(0);
		assertThat(erstellteFahrradroute.getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.TFIS_ROUTE);
		assertThat(erstellteFahrradroute.getTfisId()).isEqualTo(TfisId.of("tfisID_0"));
		assertThat(erstellteFahrradroute.getOriginalGeometrie()).contains(geometry1);
	}

	@Test
	void startPunktNaeheEndpunkt_wirdUmgedreht()
		throws ShapeProjectionException, IOException, KeinMatchGefundenException {
		// arrange
		SimpleFeature simpleFeature1 = createNichtVariantenFeature("tfisID_0",
			"eins name",
			Map.of(),
			new Coordinate(10, 20), new Coordinate(10, 100));
		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1)); // Steam wird zwei mal durchgegangen
		// neu importieren
		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.empty());

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
		Kante netzbezugKante2 = KanteTestDataProvider.fromKnoten(knoten2, knoten3)
			.id(2L)
			.dlmId(DlmId.of(dlmId2)).build();
		Kante netzbezugKante3 = KanteTestDataProvider.fromKnoten(knoten3, knoten4)
			.id(3L)
			.dlmId(DlmId.of(dlmId3)).build();
		when(kantenRepository.findAllByDlmIdIn(any()))
			.thenReturn(new ArrayList<>(List.of(netzbezugKante1, netzbezugKante2, netzbezugKante3)));

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);
		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of(dlmId1, dlmId2, dlmId3));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn(dlmId1);
		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));
		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();
		when(tfisImportService.extractStartpunkt(simpleFeature1)).thenReturn(Optional.of(
			GeometryTestdataProvider.createPoint(knoten4.getKoordinate())));

		// act
		job.doRun();

		// assert
		ArgumentCaptor<LineString> captor = ArgumentCaptor.forClass(LineString.class);
		verify(tfisImportService).routeProfil(captor.capture());

		assertThat(captor.getValue().getStartPoint())
			.isEqualTo(knoten4.getPoint());
	}

	@Test
	void startPunktNaeheStartpunkt_wirdNichtUmgedreht()
		throws ShapeProjectionException, IOException, KeinMatchGefundenException {
		// arrange
		SimpleFeature simpleFeature1 = createNichtVariantenFeature("tfisID_0",
			"eins name",
			Map.of(),
			new Coordinate(10, 20), new Coordinate(10, 100));
		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1)); // Steam wird zwei mal durchgegangen
		// neu importieren
		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.empty());

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
		Kante netzbezugKante2 = KanteTestDataProvider.fromKnoten(knoten2, knoten3)
			.id(2L)
			.dlmId(DlmId.of(dlmId2)).build();
		Kante netzbezugKante3 = KanteTestDataProvider.fromKnoten(knoten3, knoten4)
			.id(3L)
			.dlmId(DlmId.of(dlmId3)).build();
		when(kantenRepository.findAllByDlmIdIn(any()))
			.thenReturn(new ArrayList<>(List.of(netzbezugKante1, netzbezugKante2, netzbezugKante3)));

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);
		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of(dlmId1, dlmId2, dlmId3));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn(dlmId1);
		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));
		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();
		when(tfisImportService.extractStartpunkt(simpleFeature1)).thenReturn(Optional.of(
			GeometryTestdataProvider.createPoint(knoten1.getKoordinate())));

		// act
		job.doRun();

		// assert
		ArgumentCaptor<LineString> captor = ArgumentCaptor.forClass(LineString.class);
		verify(tfisImportService).routeProfil(captor.capture());

		assertThat(captor.getValue().getStartPoint())
			.isEqualTo(knoten1.getPoint());
	}

	@Test
	void testeUpdatesExistingFahrradroute() throws ShapeProjectionException, IOException {
		// arrange

		SimpleFeature simpleFeature1 = createNichtVariantenFeature("tfisID_0",
			"eins name",
			Map.of(),
			new Coordinate(10, 10), new Coordinate(20, 10));

		SimpleFeature simpleFeature2 = createNichtVariantenFeature("tfisID_1",
			"zwei name",
			Map.of(),
			new Coordinate(10, 50), new Coordinate(20, 50));

		SimpleFeature simpleFeature3 = createNichtVariantenFeature("tfisID_2",
			"drei name",
			Map.of(),
			new Coordinate(10, 100), new Coordinate(20, 100));

		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3)); // Steam wird zwei mal durchgegangen

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isLandesradfernweg(simpleFeature2)).thenReturn(true);
		when(tfisImportService.isLandesradfernweg(simpleFeature3)).thenReturn(false);

		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature2)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature3)).thenReturn(false);

		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of("ID 0"));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		Kante netzbezugKante = KanteTestDataProvider.withDefaultValues().id(0L)
			.geometry((LineString) geometry1.getGeometryN(0))
			.dlmId(DlmId.of("ID 0")).build();
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(kantenRepository.findAllByDlmIdIn(List.of(DlmId.of("ID 0"))))
			.thenReturn(new ArrayList<>(List.of(netzbezugKante)));

		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn("ID 0");

		// da keine nicht gemappten features können wir uns ein paar mocks sparen

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues().id(8465L)
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.tfisId(TfisId.of("tfisID_0"))
			.beschreibung("Blöde beschrebung")
			.abschnittsweiserKantenBezug(List.of())
			.name(FahrradrouteName.of("Blödhein")).build();

		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.of(fahrradroute));
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("MEDAL SUZDAL PANIC◎○●");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("Wi(l)d Screen Baroque!");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("Giraffitalism");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();

		// act
		job.doRun();

		// assert
		ArgumentCaptor<Fahrradroute> fahrradrouteCaptor = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteRepository).save(fahrradrouteCaptor.capture());
		assertThat(fahrradrouteCaptor.getValue().getId()).isEqualTo(fahrradroute.getId());
		assertThat(fahrradrouteCaptor.getValue().getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.TFIS_ROUTE);
		assertThat(fahrradrouteCaptor.getValue().getTfisId()).isEqualTo(TfisId.of("tfisID_0"));
		assertThat(fahrradrouteCaptor.getValue().getKurzbeschreibung()).isEqualTo("MEDAL SUZDAL PANIC◎○●");
		assertThat(fahrradrouteCaptor.getValue().getBeschreibung()).isEqualTo("Wi(l)d Screen Baroque!");
		assertThat(fahrradrouteCaptor.getValue().getInfo()).isEqualTo("Giraffitalism");
		assertThat(fahrradrouteCaptor.getValue().getOriginalGeometrie()).contains(geometry1);
	}

	@Test
	void testeHatVarianteWirdKorrektGesetzt() throws ShapeProjectionException, IOException {
		// arrange

		SimpleFeature simpleFeature1 = createNichtVariantenFeature("tfisID_0",
			"eins name",
			Map.of(),
			new Coordinate(10, 10), new Coordinate(20, 10));

		SimpleFeature simpleFeature4 = SimpleFeatureTestDataProvider.withMultiLineStringAndAttributes(
			Map.of("objid", "tfisID_0", "art", "1001", "nam", "eins name"), // variante
			new Coordinate(10, 12), new Coordinate(20, 12));

		SimpleFeature simpleFeature2 = createNichtVariantenFeature("tfisID_1",
			"zwei name",
			Map.of(),
			new Coordinate(10, 50), new Coordinate(20, 50));

		SimpleFeature simpleFeature3 = createNichtVariantenFeature("tfisID_2",
			"drei name",
			Map.of(),
			new Coordinate(10, 100), new Coordinate(20, 100));

		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any()))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3, simpleFeature4))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3, simpleFeature4))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3,
				simpleFeature4)); // Steam wird zwei mal durchgegangen

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isLandesradfernweg(simpleFeature2)).thenReturn(true);
		when(tfisImportService.isLandesradfernweg(simpleFeature3)).thenReturn(false);

		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature2)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature3)).thenReturn(false);

		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of("ID 0"));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		Kante netzbezugKante = KanteTestDataProvider.withDefaultValues().id(0L)
			.geometry((LineString) geometry1.getGeometryN(0))
			.dlmId(DlmId.of("ID 0")).build();
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(kantenRepository.findAllByDlmIdIn(List.of(DlmId.of("ID 0"))))
			.thenReturn(new ArrayList<>(List.of(netzbezugKante)));

		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn("ID 0");

		// da keine nicht gemappten features können wir uns ein paar mocks sparen

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues().id(1L)
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.tfisId(TfisId.of("tfisID_0"))
			.beschreibung("Blöde beschrebung")
			.abschnittsweiserKantenBezug(List.of())
			.name(FahrradrouteName.of("Blödhein")).build();

		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.of(fahrradroute));
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("MEDAL SUZDAL PANIC◎○●");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("Wi(l)d Screen Baroque!");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("Giraffitalism");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();

		// act
		job.doRun();

		// assert
		ArgumentCaptor<Fahrradroute> fahrradrouteCaptor = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteRepository).save(fahrradrouteCaptor.capture());
		assertThat(fahrradrouteCaptor.getValue().getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.TFIS_ROUTE);
		assertThat(fahrradrouteCaptor.getValue().getTfisId()).isEqualTo(TfisId.of("tfisID_0"));
		assertThat(fahrradrouteCaptor.getValue().getKurzbeschreibung()).isEqualTo("MEDAL SUZDAL PANIC◎○●");
		assertThat(fahrradrouteCaptor.getValue().getBeschreibung()).isEqualTo("Wi(l)d Screen Baroque!");
		assertThat(fahrradrouteCaptor.getValue().getInfo()).isEqualTo("Giraffitalism");
		assertThat(fahrradrouteCaptor.getValue().getOriginalGeometrie()).contains(geometry1);
	}

	@Test
	void testeDeletesNoLongerActiveFahrradrouten() throws ShapeProjectionException, IOException {
		// arrange

		SimpleFeature simpleFeature1 = createNichtVariantenFeature("tfisID_0",
			"eins name",
			Map.of(),
			new Coordinate(10, 10), new Coordinate(20, 10));

		SimpleFeature simpleFeature2 = createNichtVariantenFeature("tfisID_1",
			"zwei name",
			Map.of(),
			new Coordinate(10, 50), new Coordinate(20, 50));

		SimpleFeature simpleFeature3 = createNichtVariantenFeature("tfisID_2",
			"drei name",
			Map.of(),
			new Coordinate(10, 100), new Coordinate(20, 100));

		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any()))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3))
			.thenReturn(Stream.of(simpleFeature1, simpleFeature2, simpleFeature3)); // Steam wird zwei mal durchgegangen

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isLandesradfernweg(simpleFeature2)).thenReturn(true);
		when(tfisImportService.isLandesradfernweg(simpleFeature3)).thenReturn(false);

		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature2)).thenReturn(true);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature3)).thenReturn(false);

		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of("ID 0"));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		Kante netzbezugKante = KanteTestDataProvider.withDefaultValues().id(0L)
			.geometry((LineString) geometry1.getGeometryN(0))
			.dlmId(DlmId.of("ID 0")).build();
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(kantenRepository.findAllByDlmIdIn(List.of(DlmId.of("ID 0"))))
			.thenReturn(new ArrayList<>(List.of(netzbezugKante)));

		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn("ID 0");

		// da keine nicht gemappten features können wir uns ein paar mocks sparen

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues().id(1L)
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.tfisId(TfisId.of("tfisID_0"))
			.beschreibung("Blöde beschrebung")
			.abschnittsweiserKantenBezug(List.of())
			.name(FahrradrouteName.of("Blödhein")).build();

		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.of(fahrradroute));
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("MEDAL SUZDAL PANIC◎○●");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("Wi(l)d Screen Baroque!");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("Giraffitalism");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();

		// act
		job.doRun();

		// assert
		verify(fahrradrouteRepository).deleteAllByFahrradrouteTypAndTfisIdNotIn(eq(FahrradrouteTyp.TFIS_ROUTE),
			notToDeleteSetCaptor.capture());

		Set<TfisId> notDeletedIDsOfTypTfis_Route = notToDeleteSetCaptor.getValue();
		assertThat(notDeletedIDsOfTypTfis_Route).isEqualTo(Set.of(fahrradroute.getTfisId()));
	}

	@Test
	void teste_MatcheWennGleicheDlmIdAndGeometryUnterschiedl_unterEinemFeatureZweiKanten()
		throws ShapeProjectionException, IOException, KeinMatchGefundenException {
		// arrange

		SimpleFeature simpleFeature1 = createNichtVariantenFeature("tfisID_0",
			"eins name",
			Map.of(),
			new Coordinate(10, 10), new Coordinate(50, 10));

		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1)); // Steam wird zwei mal durchgegangen

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);

		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);

		DlmId dlmId1 = DlmId.of("ID 0");
		DlmId dlmId2 = DlmId.of("ID 20");

		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of(dlmId1.getValue()));
		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		// Kantengeometrie weicht absichtich von Featuregeometrie ab
		Kante netzbezugKante1 = KanteTestDataProvider.withDefaultValues().id(1L).dlmId(dlmId1)
			.geometry(
				GeometryTestdataProvider.createLineString(
					new Coordinate(10, 10), new Coordinate(20, 10)))
			.build();
		Kante netzbezugKante2 = KanteTestDataProvider.withDefaultValues().id(2L).dlmId(dlmId2)
			.geometry(
				GeometryTestdataProvider.createLineString(
					new Coordinate(20, 10), new Coordinate(50, 10)))
			.build();
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);
		when(kantenRepository.findAllByDlmIdIn(List.of(dlmId1)))
			.thenReturn(new ArrayList<>(List.of(netzbezugKante1)));

		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn(dlmId1.getValue());

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);
		when(fahrradrouteRepository.findByTfisId(TfisId.of("tfisID_0"))).thenReturn(Optional.empty());
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();

		// Da die Geometrie der Kante und des Features abweicht, kommt dieses Feature in die nichtGemappteFeatures Liste
		// Und wir muessen das matching auch mocken
		LineString netzbezugLineString = GeometryTestdataProvider.createLineString(new Coordinate(10, 10),
			new Coordinate(50, 10));
		when(tfisImportService.findMatchingKanten(any(), any())).thenReturn(
			List.of(netzbezugKante1, netzbezugKante2));
		when(tfisImportService.routeProfil(any())).thenReturn(new ProfilMatchResult(
			netzbezugLineString,
			List.of(OsmWayId.of(1L), OsmWayId.of(2L)),
			Collections.emptyList()));

		when(kantenRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(netzbezugKante1, netzbezugKante2));

		// act
		job.doRun();

		// assert
		ArgumentCaptor<Fahrradroute> captor = ArgumentCaptor.forClass(Fahrradroute.class);
		verify(fahrradrouteRepository).save(captor.capture());

		List<Fahrradroute> values = captor.getAllValues();
		assertThat(values.size()).isEqualTo(1);
		Fahrradroute erstellteFahrradroute = values.get(0);
		assertThat(erstellteFahrradroute.getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.TFIS_ROUTE);
		assertThat(erstellteFahrradroute.getTfisId()).isEqualTo(TfisId.of("tfisID_0"));
		assertThat(erstellteFahrradroute.getOriginalGeometrie()).contains(geometry1);
		assertThat(erstellteFahrradroute.getAbschnittsweiserKantenBezug().size()).isEqualTo(2);
		assertThat(erstellteFahrradroute.getAbschnittsweiserKantenBezug().get(0).getKante().getId()).isEqualTo(1L);
		assertThat(erstellteFahrradroute.getAbschnittsweiserKantenBezug().get(1).getKante().getId()).isEqualTo(2L);
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
