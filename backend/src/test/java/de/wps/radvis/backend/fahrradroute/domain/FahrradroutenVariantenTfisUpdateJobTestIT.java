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
import static org.mockito.Mockito.when;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
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
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteVariante;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradroutenVariantenTfisImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteVarianteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.VarianteKategorie;
import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
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
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group4")
@ContextConfiguration(classes = { FahrradroutenVariantenTfisUpdateJobTestIT.TestConfiguration.class,
	CommonConfiguration.class,
	GeoConverterConfiguration.class, })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class })
public class FahrradroutenVariantenTfisUpdateJobTestIT extends DBIntegrationTestIT {
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
		@MockBean
		private VerwaltungseinheitService verwaltungseinheitService;
		@MockBean
		ShapeFileRepository shapeFileRepository;
		@MockBean
		DlmMatchingRepository dlmMatchingRepository;

		@Bean
		NetzService netzService() {
			return new NetzService(kantenRepository, knotenRepository, zustaendigkeitAttributGruppenRepository,
				fahrtrichtungAttributGruppeRepository, geschwindigkeitAttributGruppeRepository,
				fuehrungsformAttributGruppenRepository, kantenAttributGruppenRepository, verwaltungseinheitResolver);
		}

		@Bean
		TfisImportService tfisImportService() {
			return new TfisImportService(verwaltungseinheitService, shapeFileRepository, kantenRepository,
				org.springframework.data.util.Lazy.of(() -> dlmMatchingRepository));
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
	@Autowired
	KantenRepository kantenRepository;
	@Autowired
	TfisImportService tfisImportService;
	@Autowired
	ShapeFileRepository shapeFileRepository;
	@Autowired
	DlmMatchingRepository dlmMatchingRepository;

	@Autowired
	VerwaltungseinheitService verwaltungseinheitService;

	@TempDir
	Path tfisRadwegePath = Path.of("testpfad");

	FahrradroutenVariantenTfisUpdateJob job;

	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@Captor
	private ArgumentCaptor<Set<TfisId>> notToDeleteSetCaptor;

	@PersistenceContext
	EntityManager entityManager;

	private TfisId fahrradrouteId;

	private TfisId varianteId1;
	private LineString geometrie1;
	private VarianteKategorie kategorie1;
	private Kante netzbezugKante1;

	@BeforeEach
	void setup() throws IOException, KeinMatchGefundenException, ShapeProjectionException {
		MockitoAnnotations.openMocks(this);
		new File(tfisRadwegePath.toString(), "test.shp").createNewFile();

		job = new FahrradroutenVariantenTfisUpdateJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			shapeFileRepository, kantenRepository, tfisImportService, tfisRadwegePath);

		when(shapeFileRepository.transformGeometryToUTM32(any())).thenAnswer(
			((Answer<SimpleFeature>) invocation -> this.transformGeometryToUTM32(
				(SimpleFeature) invocation.getArgument(0))));
		when(dlmMatchingRepository.matchGeometryUndDetails(any(), any()))
			.thenAnswer((Answer<ProfilMatchResult>) invocation -> new ProfilMatchResult(invocation.getArgument(0),
				List.of(), List.of()));
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(
				GeometryTestdataProvider.createQuadratischerBereich(0, 0, 1000, 1000)));

		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);

		// arrange
		fahrradrouteId = TfisId.of("tfisId1");
		varianteId1 = TfisId.of("varianteTfisId1");
		DlmId dlmId1 = DlmId.of("dlmId1");
		geometrie1 = GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10, 10));
		kategorie1 = VarianteKategorie.ALTERNATIVSTRECKE;

		SimpleFeature varianteFeature1 = createVarianteTfisFeature(fahrradrouteId, varianteId1, dlmId1, geometrie1,
			kategorie1);

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(varianteFeature1))
			.thenReturn(Stream.of(varianteFeature1))
			.thenReturn(Stream.of(varianteFeature1)); // Stream wird zwei mal durchgegangen

		netzbezugKante1 = kantenRepository
			.save(KanteTestDataProvider.withDefaultValues().geometry(geometrie1).dlmId(dlmId1).build());
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	public void run_varianteHatNetzbezug_KeinUpdate() {
		// arrange
		LineString existingGeometrie = GeometryTestdataProvider.createLineString(new Coordinate(110, 110),
			new Coordinate(210, 210));
		Kante existingNetzbezugKante = kantenRepository
			.save(KanteTestDataProvider.withDefaultValues().geometry(existingGeometrie).build());
		FahrradrouteVariante existingVariante = FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(
				List.of(
					new AbschnittsweiserKantenBezug(existingNetzbezugKante, LinearReferenzierterAbschnitt.of(0, 1))))
			.kategorie(VarianteKategorie.GEGENRICHTUNG)
			.geometrie(existingGeometrie)
			.tfisId(varianteId1).build();
		// Damit die obere Variante betrachtet wird, sonst wird die Fahrradroute bereits rausgefiltert
		FahrradrouteVariante otherVarianteOhneGeometrie = FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(
				List.of(
					new AbschnittsweiserKantenBezug(existingNetzbezugKante, LinearReferenzierterAbschnitt.of(0, 1))))
			.kategorie(VarianteKategorie.GEGENRICHTUNG)
			.geometrie(null)
			.tfisId(TfisId.of("irgendeine Variante Ohne Geometrie")).build();
		Fahrradroute fahrradroute = fahrradrouteRepository.save(FahrradrouteTestDataProvider
			.onKante(netzbezugKante1)
			.verantwortlich(null)
			.tfisId(fahrradrouteId)
			.varianten(List.of(existingVariante, otherVarianteOhneGeometrie))
			.build());

		entityManager.flush();
		entityManager.clear();

		// act

		Optional<JobStatistik> statistik = job.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		Fahrradroute updatedFahrradroute = fahrradrouteRepository.findById(fahrradroute.getId()).get();
		assertThat(updatedFahrradroute.getVarianten()).hasSize(2);
		assertThat(updatedFahrradroute.findFahrradrouteVariante(varianteId1).get()).usingRecursiveComparison()
			.usingOverriddenEquals()
			.isEqualTo(existingVariante);

		FahrradroutenVariantenTfisImportStatistik importStatistik = (FahrradroutenVariantenTfisImportStatistik) statistik
			.get();
		assertThat(importStatistik.anzahlVariantenBetrachtet).isEqualTo(1);
		assertThat(importStatistik.anzahlVariantenAktualisiert).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenGeloescht).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenHinzugefuegt).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenIgnoriert).isEqualTo(0);
	}

	@Test
	public void run_varianteNichtInBW_wirdIngoriert() throws ShapeProjectionException, IOException {
		// arrange
		LineString existingGeometrie = GeometryTestdataProvider.createLineString(new Coordinate(110, 110),
			new Coordinate(210, 210));
		Kante existingNetzbezugKante = kantenRepository
			.save(KanteTestDataProvider.withDefaultValues().geometry(existingGeometrie).build());
		// Damit die Fahrradroute nicht vorher rausgefiltert wird
		FahrradrouteVariante ohneGeometrie = FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(
				List.of(
					new AbschnittsweiserKantenBezug(existingNetzbezugKante, LinearReferenzierterAbschnitt.of(0, 1))))
			.kategorie(VarianteKategorie.GEGENRICHTUNG)
			.geometrie(null)
			.tfisId(TfisId.of("irgendeine Variante Ohne Geometrie")).build();
		Fahrradroute fahrradroute = fahrradrouteRepository.save(FahrradrouteTestDataProvider
			.onKante(netzbezugKante1)
			.verantwortlich(null)
			.tfisId(fahrradrouteId)
			.varianten(List.of(ohneGeometrie))
			.build());

		SimpleFeature varianteOutsideBW = createVarianteTfisFeature(fahrradrouteId, varianteId1,
			netzbezugKante1.getDlmId(),
			GeometryTestdataProvider.createLineString(new Coordinate(10000, 10000), new Coordinate(11000, 11000)),
			kategorie1);

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(varianteOutsideBW))
			.thenReturn(Stream.of(varianteOutsideBW))
			.thenReturn(Stream.of(varianteOutsideBW)); // Stream wird zwei mal durchgegangen

		entityManager.flush();
		entityManager.clear();

		// act

		Optional<JobStatistik> statistik = job.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		Fahrradroute updatedFahrradroute = fahrradrouteRepository.findById(fahrradroute.getId()).get();
		assertThat(updatedFahrradroute.getVarianten()).hasSize(1);
		assertThat(updatedFahrradroute.findFahrradrouteVariante(varianteId1)).isEmpty();

		FahrradroutenVariantenTfisImportStatistik importStatistik = (FahrradroutenVariantenTfisImportStatistik) statistik
			.get();
		assertThat(importStatistik.anzahlVariantenBetrachtet).isEqualTo(1);
		assertThat(importStatistik.anzahlVariantenAktualisiert).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenGeloescht).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenHinzugefuegt).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenIgnoriert).isEqualTo(1);
	}

	@Test
	public void run_varianteHatKeinNetzbezug_updateNetzbezug() {
		// arrange
		LineString existingGeometrie = GeometryTestdataProvider.createLineString(new Coordinate(110, 110),
			new Coordinate(210, 210));
		Kante existingNetzbezugKante = kantenRepository
			.save(KanteTestDataProvider.withDefaultValues().geometry(existingGeometrie).build());
		FahrradrouteVariante existingVariante = FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(
				List.of(
					new AbschnittsweiserKantenBezug(existingNetzbezugKante, LinearReferenzierterAbschnitt.of(0, 1))))
			.kategorie(VarianteKategorie.GEGENRICHTUNG)
			.geometrie(null)
			.tfisId(varianteId1).build();
		Fahrradroute fahrradroute = fahrradrouteRepository.save(FahrradrouteTestDataProvider
			.onKante(netzbezugKante1)
			.verantwortlich(null)
			.tfisId(fahrradrouteId)
			.varianten(List.of(existingVariante))
			.build());

		entityManager.flush();
		entityManager.clear();

		// act

		Optional<JobStatistik> statistik = job.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		Fahrradroute updatedFahrradroute = fahrradrouteRepository.findById(fahrradroute.getId()).get();
		assertThat(updatedFahrradroute.getVarianten()).hasSize(1);

		FahrradrouteVariante expectedVariante1 = FahrradrouteVariante.tfisVarianteBuilder()
			.kategorie(existingVariante.getKategorie())
			.geometrie(geometrie1)
			.tfisId(varianteId1)
			.abschnittsweiserKantenBezug(
				List.of(new AbschnittsweiserKantenBezug(netzbezugKante1, LinearReferenzierterAbschnitt.of(0, 1))))
			.linearReferenzierteProfilEigenschaften(List.of())
			.id(fahrradroute.getVarianten().get(0).getId())
			.build();
		assertThat(updatedFahrradroute.getVarianten().get(0)).usingRecursiveComparison()
			.usingOverriddenEquals()
			.isEqualTo(expectedVariante1);

		FahrradroutenVariantenTfisImportStatistik importStatistik = (FahrradroutenVariantenTfisImportStatistik) statistik
			.get();
		assertThat(importStatistik.anzahlVariantenBetrachtet).isEqualTo(1);
		assertThat(importStatistik.anzahlVariantenAktualisiert).isEqualTo(1);
		assertThat(importStatistik.anzahlVariantenGeloescht).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenHinzugefuegt).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenIgnoriert).isEqualTo(0);

	}

	@Test
	public void run_nurFahrradroutenMitLeerenVariantenBetrachten() {
		// arrange
		LineString existingGeometrie = GeometryTestdataProvider.createLineString(new Coordinate(110, 110),
			new Coordinate(210, 210));
		Kante existingNetzbezugKante = kantenRepository
			.save(KanteTestDataProvider.withDefaultValues().geometry(existingGeometrie).build());
		FahrradrouteVariante existingVariante = FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(
				List.of(
					new AbschnittsweiserKantenBezug(existingNetzbezugKante, LinearReferenzierterAbschnitt.of(0, 1))))
			.kategorie(VarianteKategorie.GEGENRICHTUNG)
			.geometrie(existingGeometrie)
			.tfisId(varianteId1).build();
		Fahrradroute fahrradroute = fahrradrouteRepository.save(FahrradrouteTestDataProvider
			.onKante(netzbezugKante1)
			.verantwortlich(null)
			.tfisId(fahrradrouteId)
			.varianten(List.of(existingVariante))
			.build());

		entityManager.flush();
		entityManager.clear();

		// act

		Optional<JobStatistik> statistik = job.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		Fahrradroute updatedFahrradroute = fahrradrouteRepository.findById(fahrradroute.getId()).get();
		assertThat(updatedFahrradroute.getVarianten()).hasSize(1);
		assertThat(updatedFahrradroute.getVarianten().get(0)).usingRecursiveComparison()
			.usingOverriddenEquals()
			.isEqualTo(existingVariante);

		FahrradroutenVariantenTfisImportStatistik importStatistik = (FahrradroutenVariantenTfisImportStatistik) statistik
			.get();
		assertThat(importStatistik.anzahlVariantenBetrachtet).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenAktualisiert).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenGeloescht).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenHinzugefuegt).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenIgnoriert).isEqualTo(1);
	}

	@Test
	public void run_noInsert() {
		// arrange
		LineString existingGeometrie = GeometryTestdataProvider.createLineString(new Coordinate(110, 110),
			new Coordinate(210, 210));
		Kante existingNetzbezugKante = kantenRepository
			.save(KanteTestDataProvider.withDefaultValues().geometry(existingGeometrie).build());
		// Damit die obere Variante betrachtet wird, sonst wird die Fahrradroute bereits rausgefiltert
		FahrradrouteVariante otherVarianteOhneGeometrie = FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(
				List.of(
					new AbschnittsweiserKantenBezug(existingNetzbezugKante, LinearReferenzierterAbschnitt.of(0, 1))))
			.kategorie(VarianteKategorie.GEGENRICHTUNG)
			.geometrie(null)
			.tfisId(TfisId.of("irgendeine Variante Ohne Geometrie")).build();
		Fahrradroute fahrradroute = fahrradrouteRepository.save(FahrradrouteTestDataProvider
			.onKante(netzbezugKante1)
			.verantwortlich(null)
			.tfisId(fahrradrouteId)
			.varianten(List.of(otherVarianteOhneGeometrie))
			.build());

		entityManager.flush();
		entityManager.clear();

		// act

		Optional<JobStatistik> statistik = job.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		Fahrradroute updatedFahrradroute = fahrradrouteRepository.findById(fahrradroute.getId()).get();
		assertThat(updatedFahrradroute.getVarianten()).hasSize(1);
		assertThat(updatedFahrradroute.getVarianten().get(0)).isEqualTo(fahrradroute.getVarianten().get(0));

		FahrradroutenVariantenTfisImportStatistik importStatistik = (FahrradroutenVariantenTfisImportStatistik) statistik
			.get();
		assertThat(importStatistik.anzahlVariantenBetrachtet).isEqualTo(1);
		assertThat(importStatistik.anzahlVariantenAktualisiert).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenGeloescht).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenHinzugefuegt).isEqualTo(0);
		assertThat(importStatistik.anzahlVariantenIgnoriert).isEqualTo(0);
	}

	private SimpleFeature createVarianteTfisFeature(TfisId fahrradrouteId, TfisId varianteId, DlmId dlmId,
		LineString geometrie, VarianteKategorie kategorie) {
		HashMap<String, String> attribute = new HashMap<>();
		attribute.put("weg_shp_id", dlmId.getValue() + "#blubb#" + varianteId.getTfisId());
		attribute.put("art", kategorie.equals(VarianteKategorie.ZUBRINGERSTRECKE) ? "1002" : "1001");
		attribute.put("objid", fahrradrouteId.getTfisId());
		return SimpleFeatureTestDataProvider.withMultiLineStringAndAttributes(attribute,
			geometrie.getCoordinates());
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
}
