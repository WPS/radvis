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
import static org.mockito.Mockito.when;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
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
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.FahrtrichtungAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.NonNull;

@Tag("group1")
@ContextConfiguration(classes = { FahrradroutenTfisImportJobTestIT.TestConfiguration.class, CommonConfiguration.class,
	GeoConverterConfiguration.class, })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class })
public class FahrradroutenTfisImportJobTestIT extends DBIntegrationTestIT {
	@EnableEnversRepositories(basePackages = { "de.wps.radvis.backend.fahrradroute", "de.wps.radvis.backend.netz",
		"de.wps.radvis.backend.organisation" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.fahrradroute", "de.wps.radvis.backend.organisation",
		"de.wps.radvis.backend.netz", "de.wps.radvis.backend.benutzer", "de.wps.radvis.backend.common" })
	public static class TestConfiguration {
		@Autowired
		private KantenRepository kantenRepository;
		@Autowired
		private KnotenRepository knotenRepository;
		@MockBean
		private ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppenRepository;
		@MockBean
		private FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository;
		@MockBean
		private GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;
		@MockBean
		private FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppenRepository;
		@MockBean
		private KantenAttributGruppeRepository kantenAttributGruppenRepository;
		@MockBean
		private VerwaltungseinheitResolver verwaltungseinheitResolver;

		@Bean
		NetzService netzService() {
			return new NetzService(kantenRepository, knotenRepository, zustaendigkeitAttributGruppenRepository,
				fahrtrichtungAttributGruppeRepository, geschwindigkeitAttributGruppeRepository,
				fuehrungsformAttributGruppenRepository, kantenAttributGruppenRepository, verwaltungseinheitResolver);
		}

	}

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95)));

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	FahrradrouteService fahrradrouteService;
	@Autowired
	FahrradrouteRepository fahrradrouteRepository;
	@Mock
	ShapeFileRepositoryImpl shapeFileRepository;
	@Autowired
	KantenRepository kantenRepository;
	@Mock
	TfisImportService tfisImportService;

	@TempDir
	Path tfisRadwegePath = Path.of("testpfad");

	FahrradroutenTfisImportJob job;

	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@Captor
	private ArgumentCaptor<Set<TfisId>> notToDeleteSetCaptor;

	@PersistenceContext
	EntityManager entityManager;

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
	void testeInsertFahrradroute() throws ShapeProjectionException, IOException {
		// arrange
		String tfisId = "tfisID_0";
		SimpleFeature simpleFeature1 = createNichtVariantenFeature(tfisId,
			"eins name",
			Map.of(),
			new Coordinate(10, 10), new Coordinate(20, 10));

		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1)); // Steam wird zwei mal durchgegangen

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);

		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);

		String dlmId = "ID 0";
		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of(dlmId));
		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn(dlmId);
		Kante netzbezugKante = KanteTestDataProvider.withDefaultValues()
			.geometry((LineString) geometry1.getGeometryN(0))
			.dlmId(DlmId.of(dlmId)).build();
		kantenRepository.save(netzbezugKante);

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("MEDAL SUZDAL PANIC◎○●");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("Wi(l)d Screen Baroque!");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("Giraffitalism");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));

		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();

		// act
		job.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		Fahrradroute fahrradroute2 = fahrradrouteRepository.findByTfisId(TfisId.of(tfisId)).get();
		assertThat(fahrradroute2.getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.TFIS_ROUTE);
		assertThat(fahrradroute2.getTfisId()).isEqualTo(TfisId.of(tfisId));
		assertThat(fahrradroute2.getKurzbeschreibung()).isEqualTo("MEDAL SUZDAL PANIC◎○●");
		assertThat(fahrradroute2.getBeschreibung()).isEqualTo("Wi(l)d Screen Baroque!");
		assertThat(fahrradroute2.getInfo()).isEqualTo("Giraffitalism");
		assertThat(fahrradroute2.getOriginalGeometrie()).contains(geometry1);
	}

	@Test
	void testeUpdatesExistingFahrradroute() throws ShapeProjectionException, IOException {
		// arrange

		String tfisId = "tfisID_0";
		SimpleFeature simpleFeature1 = createNichtVariantenFeature(tfisId,
			"eins name",
			Map.of(),
			new Coordinate(10, 10), new Coordinate(20, 10));

		MultiLineString geometry1 = (MultiLineString) simpleFeature1.getDefaultGeometry();

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1))
			.thenReturn(Stream.of(simpleFeature1)); // Steam wird zwei mal durchgegangen

		when(tfisImportService.isLandesradfernweg(simpleFeature1)).thenReturn(false);
		when(tfisImportService.isNotStichwegOrAlternativStrecke(simpleFeature1)).thenReturn(true);

		when(tfisImportService.konstruiereOriginalGeometrie(List.of(simpleFeature1)))
			.thenReturn(geometry1);
		when(tfisImportService.anteilInBW(geometry1)).thenReturn(1.);

		String dlmId = "ID 0";
		when(tfisImportService.extractDlmIds(List.of(simpleFeature1))).thenReturn(Set.of(dlmId));
		when(tfisImportService.extractDlmId(simpleFeature1)).thenReturn(dlmId);
		Kante netzbezugKante = KanteTestDataProvider.withDefaultValues()
			.geometry((LineString) geometry1.getGeometryN(0))
			.dlmId(DlmId.of(dlmId)).build();
		kantenRepository.save(netzbezugKante);

		when(tfisImportService.extractName(any())).thenCallRealMethod();
		when(tfisImportService.extractKategorie(simpleFeature1)).thenReturn(Kategorie.REGIONALER_RADWANDERWEG);
		when(tfisImportService.extractKurzbeschreibung(simpleFeature1)).thenReturn("MEDAL SUZDAL PANIC◎○●");
		when(tfisImportService.extractBeschreibung(simpleFeature1)).thenReturn("Wi(l)d Screen Baroque!");
		when(tfisImportService.extractInfo(simpleFeature1)).thenReturn("Giraffitalism");
		when(tfisImportService.extractOffizielleLaenge(simpleFeature1)).thenReturn(Laenge.of(1L));

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.tfisId(TfisId.of(tfisId))
			.beschreibung("Beschreibung")
			.abschnittsweiserKantenBezug(List.of())
			.name(FahrradrouteName.of("Schöne Aussicht")).buildTfisRoute();

		fahrradroute = fahrradrouteRepository.save(fahrradroute);

		when(tfisImportService.extractLineString(simpleFeature1)).thenCallRealMethod();

		entityManager.flush();
		entityManager.clear();

		// act
		job.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		Fahrradroute fahrradroute2 = fahrradrouteRepository.findByTfisId(TfisId.of(tfisId)).get();
		assertThat(fahrradroute2.getId()).isEqualTo(fahrradroute.getId());
		assertThat(fahrradroute2.getFahrradrouteTyp()).isEqualTo(FahrradrouteTyp.TFIS_ROUTE);
		assertThat(fahrradroute2.getTfisId()).isEqualTo(TfisId.of(tfisId));
		assertThat(fahrradroute2.getKurzbeschreibung()).isEqualTo("MEDAL SUZDAL PANIC◎○●");
		assertThat(fahrradroute2.getBeschreibung()).isEqualTo("Wi(l)d Screen Baroque!");
		assertThat(fahrradroute2.getInfo()).isEqualTo("Giraffitalism");
		assertThat(fahrradroute2.getOriginalGeometrie()).contains(geometry1);
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
