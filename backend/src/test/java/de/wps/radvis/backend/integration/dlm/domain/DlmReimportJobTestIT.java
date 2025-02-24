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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.thymeleaf.TemplateEngine;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezug;
import de.wps.radvis.backend.barriere.domain.entity.provider.BarriereTestDataProvider;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteConfigurationProperties;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.fahrradroute.schnittstelle.ToubizConfigurationProperties;
import de.wps.radvis.backend.furtKreuzung.FurtKreuzungConfiguration;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungNetzBezug;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungTestDataProvider;
import de.wps.radvis.backend.furtKreuzung.domain.repository.FurtKreuzungRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.dlm.domain.entity.DlmReimportJobStatistik;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.MassnahmenConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandsabfrageConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.repository.CustomDlmMatchingRepositoryFactory;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.repository.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.service.CustomGrundnetzMappingServiceFactory;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GrundnetzMappingService;
import de.wps.radvis.backend.matching.domain.service.KanteUpdateElevationService;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopper;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group3")
@ContextConfiguration(classes = {
	NetzConfiguration.class, CommonConfiguration.class, MassnahmeConfiguration.class, BarriereConfiguration.class,
	FurtKreuzungConfiguration.class, FahrradrouteConfiguration.class, GeoConverterConfiguration.class
})
@EntityScan(basePackageClasses = { DokumentConfiguration.class, KommentarConfiguration.class })
@EnableJpaRepositories(basePackageClasses = { OrganisationConfiguration.class, BenutzerConfiguration.class })
@EnableConfigurationProperties(value = { JobConfigurationProperties.class, CommonConfigurationProperties.class,
	MassnahmenConfigurationProperties.class, NetzConfigurationProperties.class })
class DlmReimportJobTestIT extends DBIntegrationTestIT {
	DlmReimportJob dlmReimportJob;
	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	private DlmRepository dlmRepository;
	@Mock
	private DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory;
	@Mock
	private DlmMatchedGraphHopper dlmMatchedGraphHopper;

	@Autowired
	private NetzService netzService;
	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private KnotenRepository knotenRepository;
	@Autowired
	MassnahmeRepository massnahmeRepository;
	@Autowired
	BarriereRepository barriereRepository;
	@Autowired
	BenutzerRepository benutzerRepository;
	@Autowired
	FurtKreuzungRepository furtKreuzungRepository;
	@Autowired
	FahrradrouteRepository fahrradrouteRepository;
	@Autowired
	GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	protected PlatformTransactionManager transactionManager;

	@MockitoBean
	BenutzerResolver benutzerResolver;
	@MockitoBean
	FeatureToggleProperties featureToggleProperties;
	@MockitoBean
	PostgisConfigurationProperties postgisConfigurationProperties;
	@MockitoBean
	OrganisationConfigurationProperties organisationConfigurationProperties;
	@MockitoBean
	CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;
	@MockitoBean
	private DlmPbfErstellungService dlmPbfErstellungService;
	@MockitoBean
	private CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactory;
	@MockitoBean
	private CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory;
	@MockitoBean
	private SimpleMatchingService simpleMatchingService;
	@MockitoBean
	private VerwaltungseinheitService verwaltungseinheitService;
	@MockitoBean
	private BenutzerService benutzerService;
	@MockitoBean
	private TemplateEngine templateEngine;
	@MockitoBean
	private MailConfigurationProperties mailConfigurationProperties;
	@MockitoBean
	private UmsetzungsstandsabfrageConfigurationProperties umsetzungsstandsabfrageConfigurationProperties;
	@MockitoBean
	private ToubizConfigurationProperties toubizConfigurationProperties;
	@MockitoBean
	private GraphhopperRoutingRepository graphhopperRoutingRepository;
	@MockitoBean
	private KanteUpdateElevationService elevationUpdateService;
	@MockitoBean
	private FahrradrouteConfigurationProperties fahrradrouteConfigurationProperties;

	private Gebietskoerperschaft gebietskoerperschaft;
	private Benutzer benutzer;

	@BeforeEach
	void setup() throws IOException, KeineRouteGefundenException {
		MockitoAnnotations.openMocks(this);
		when(featureToggleProperties.isShowDlm()).thenReturn(true);
		when(postgisConfigurationProperties.getArgumentLimit()).thenReturn(2);

		gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Mega coole zuständige Organisation")
				.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK)
				.build());
		benutzer = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());
		when(benutzerService.getTechnischerBenutzer()).thenReturn(benutzer);

		when(graphhopperRoutingRepository.route(any(), anyLong(), anyBoolean()))
			.thenReturn(new RoutingResult(Collections.emptyList(), GeometryTestdataProvider.createLineString(),
				Hoehenunterschied.of(10.0), Hoehenunterschied.of(12.0)));

		when(customGrundnetzMappingServiceFactory.createGrundnetzMappingService(any()))
			.thenReturn(new GrundnetzMappingService(simpleMatchingService));
		when(customDlmMatchingRepositoryFactory.createCustomMatchingRepository(any()))
			.thenReturn(mock(DlmMatchingRepository.class));

		dlmReimportJob = new DlmReimportJob(
			jobExecutionDescriptionRepository,
			dlmPbfErstellungService,
			new KantenAttributeUebertragungService(Laenge.of(1.0)),
			new VernetzungService(kantenRepository, knotenRepository, netzService),
			netzService,
			new DlmImportService(dlmRepository, netzService),
			customDlmMatchingRepositoryFactory, customGrundnetzMappingServiceFactory);
	}

	@SuppressWarnings("unchecked")
	@Test
	void gematchteKanteInNetzbezug_updateNetzbezug() throws IOException {
		// arrange
		Kante kante1 = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build());

		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante1).zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(benutzer).build();
		Barriere barriere = BarriereTestDataProvider.withDefaultValues()
			.netzbezug(new BarriereNetzBezug(Set.of(new AbschnittsweiserKantenSeitenBezug(kante1,
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
				Collections.emptySet(),
				Collections.emptySet()))
			.verantwortlicheOrganisation(gebietskoerperschaft)
			.build();
		FurtKreuzung furtKreuzung = FurtKreuzungTestDataProvider.withDefaultValues()
			.netzbezug(new FurtKreuzungNetzBezug(Set.of(new AbschnittsweiserKantenSeitenBezug(kante1,
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
				Collections.emptySet(),
				Collections.emptySet()))
			.verantwortlicheOrganisation(gebietskoerperschaft)
			.build();
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.onKante(kante1)
			.verantwortlich(gebietskoerperschaft).build();

		massnahmeRepository.save(massnahme);
		barriereRepository.save(barriere);
		furtKreuzungRepository.save(furtKreuzung);
		fahrradrouteRepository.save(fahrradroute);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		ImportedFeature importedFeature = ImportedFeatureTestDataProvider
			.withLineString(kante1.getGeometry().reverse().getCoordinates())
			.fachId("456").build();
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					importedFeature));

		HashMap<String, Long> dlmIdToNewKanteId = new HashMap<>();
		doAnswer(invocationOnMock -> {
			((Collection<Kante>) invocationOnMock.getArgument(1)).forEach(kante -> {
				dlmIdToNewKanteId.put(kante.getDlmId().getValue(), kante.getId());
			});
			return null;
		}).when(dlmPbfErstellungService).erstellePbfForKanten(any(), any());

		when(simpleMatchingService.matche(eq(kante1.getGeometry()), any())).thenAnswer(invocationOnMock -> {
			OsmMatchResult importedFeatureMatchingResult = new OsmMatchResult(
				(LineString) importedFeature.getGeometrie(),
				List.of(OsmWayId.of(dlmIdToNewKanteId.get(importedFeature.getTechnischeId()))));
			return Optional.of(importedFeatureMatchingResult);
		});

		// act
		Optional<JobStatistik> statistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(StreamSupport
			.stream(kantenRepository.findAll().spliterator(), false).count()).isEqualTo(1);

		Optional<Massnahme> massnahmeAfterImport = StreamSupport
			.stream(massnahmeRepository.findAll().spliterator(), false).findAny();
		Optional<Barriere> barriereAfterImport = StreamSupport
			.stream(barriereRepository.findAll().spliterator(), false).findAny();
		Optional<FurtKreuzung> furtKreuzungAfterImport = StreamSupport
			.stream(furtKreuzungRepository.findAll().spliterator(), false).findAny();
		Optional<Fahrradroute> fahrradrouteAfterImport = StreamSupport
			.stream(fahrradrouteRepository.findAll().spliterator(), false).findAny();

		List<Kante> resultingKanten = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
			.toList();
		assertThat(resultingKanten).hasSize(1);
		Kante ersetzteKante = resultingKanten.get(0);

		assertThat(massnahmeAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(
			massnahmeAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next().getKante())
				.isEqualTo(ersetzteKante);

		assertThat(barriereAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(
			barriereAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next().getKante())
				.isEqualTo(ersetzteKante);

		assertThat(furtKreuzungAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(
			furtKreuzungAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next()
				.getKante())
					.isEqualTo(ersetzteKante);

		assertThat(fahrradrouteAfterImport.get().getAbschnittsweiserKantenBezug()).hasSize(1);
		assertThat(
			fahrradrouteAfterImport.get().getAbschnittsweiserKantenBezug().get(0).getKante())
				.isEqualTo(ersetzteKante);

		DlmReimportJobStatistik jobStatistik = (DlmReimportJobStatistik) statistik.get();
		assertThat(jobStatistik.netzbezugAnpassungStatistik.anzahlMitKanteErsetzt).isEqualTo(4);
		assertThat(jobStatistik.netzbezugAnpassungStatistik.anzahlMitKanteGeloescht).isEqualTo(0);
	}

	@Test
	void geloeschteKanteInNetzbezug_removeFromNetzbezug() {
		// arrange
		Kante kante1 = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build());
		Kante kante2 = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 200, 200, QuellSystem.DLM)
				.dlmId(DlmId.of("456")).build());

		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante1, kante2).zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(benutzer).build();
		Barriere barriere = BarriereTestDataProvider.withDefaultValues()
			.netzbezug(new BarriereNetzBezug(Set.of(new AbschnittsweiserKantenSeitenBezug(kante1,
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(kante2,
					LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
				Collections.emptySet(),
				Collections.emptySet()))
			.verantwortlicheOrganisation(gebietskoerperschaft)
			.build();
		FurtKreuzung furtKreuzung = FurtKreuzungTestDataProvider.withDefaultValues()
			.netzbezug(new FurtKreuzungNetzBezug(Set.of(new AbschnittsweiserKantenSeitenBezug(kante1,
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(kante2,
					LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
				Collections.emptySet(),
				Collections.emptySet()))
			.verantwortlicheOrganisation(gebietskoerperschaft)
			.build();
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.onKante(kante1, kante2)
			.verantwortlich(gebietskoerperschaft).build();

		massnahmeRepository.save(massnahme);
		barriereRepository.save(barriere);
		furtKreuzungRepository.save(furtKreuzung);
		fahrradrouteRepository.save(fahrradroute);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
						.fachId(kante2.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> statistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(StreamSupport
			.stream(kantenRepository.findAll().spliterator(), false).count()).isEqualTo(1);

		Optional<Massnahme> massnahmeAfterImport = StreamSupport
			.stream(massnahmeRepository.findAll().spliterator(), false).findAny();
		Optional<Barriere> barriereAfterImport = StreamSupport
			.stream(barriereRepository.findAll().spliterator(), false).findAny();
		Optional<FurtKreuzung> furtKreuzungAfterImport = StreamSupport
			.stream(furtKreuzungRepository.findAll().spliterator(), false).findAny();
		Optional<Fahrradroute> fahrradrouteAfterImport = StreamSupport
			.stream(fahrradrouteRepository.findAll().spliterator(), false).findAny();

		assertThat(massnahmeAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(
			massnahmeAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next().getKante())
				.isEqualTo(kante2);

		assertThat(barriereAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(
			barriereAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next().getKante())
				.isEqualTo(kante2);

		assertThat(furtKreuzungAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(
			furtKreuzungAfterImport.get().getNetzbezug().getImmutableKantenAbschnittBezug().iterator().next()
				.getKante())
					.isEqualTo(kante2);

		assertThat(fahrradrouteAfterImport.get().getAbschnittsweiserKantenBezug()).hasSize(1);
		assertThat(
			fahrradrouteAfterImport.get().getAbschnittsweiserKantenBezug().get(0).getKante())
				.isEqualTo(kante2);

		DlmReimportJobStatistik dlmReimportJobStatistik = (DlmReimportJobStatistik) statistik.get();
		assertThat(dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKanteGeloescht).isEqualTo(4);
		assertThat(dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKanteErsetzt).isEqualTo(0);
	}

	@Test
	void geloeschterKnotenInNetzbezug_removeFromNetzbezug() {
		// arrange
		Knoten knoten1 = netzService
			.saveKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build());
		Knoten knoten2 = netzService
			.saveKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).build());
		Knoten knoten3 = netzService
			.saveKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200), QuellSystem.DLM).build());
		Kante kante1 = netzService
			.saveKante(KanteTestDataProvider.fromKnoten(knoten1, knoten2).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build());
		Kante kante2 = netzService
			.saveKante(KanteTestDataProvider.fromKnoten(knoten2, knoten3).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("456")).build());

		Massnahme massnahme = MassnahmeTestDataProvider.withKnoten(knoten1, knoten2).zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(benutzer).build();
		Barriere barriere = BarriereTestDataProvider.withDefaultValues()
			.netzbezug(new BarriereNetzBezug(Collections.emptySet(),
				Collections.emptySet(),
				Set.of(knoten1, knoten2)))
			.verantwortlicheOrganisation(gebietskoerperschaft)
			.build();
		FurtKreuzung furtKreuzung = FurtKreuzungTestDataProvider.withDefaultValues()
			.netzbezug(new FurtKreuzungNetzBezug(Collections.emptySet(),
				Collections.emptySet(),
				Set.of(knoten1, knoten2)))
			.verantwortlicheOrganisation(gebietskoerperschaft)
			.build();

		massnahmeRepository.save(massnahme);
		barriereRepository.save(barriere);
		furtKreuzungRepository.save(furtKreuzung);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
						.fachId(kante2.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> statistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		List<Kante> kanten = StreamSupport
			.stream(kantenRepository.findAll().spliterator(), false).toList();
		assertThat(kanten).hasSize(1);
		assertThat(kanten).doesNotContain(kante1);

		List<Knoten> knoten = StreamSupport
			.stream(knotenRepository.findAll().spliterator(), false).toList();
		assertThat(knoten).hasSize(2);
		assertThat(knoten).doesNotContain(knoten1);

		Optional<Massnahme> massnahmeAfterImport = StreamSupport
			.stream(massnahmeRepository.findAll().spliterator(), false).findAny();
		Optional<Barriere> barriereAfterImport = StreamSupport
			.stream(barriereRepository.findAll().spliterator(), false).findAny();
		Optional<FurtKreuzung> furtKreuzungAfterImport = StreamSupport
			.stream(furtKreuzungRepository.findAll().spliterator(), false).findAny();

		assertThat(massnahmeAfterImport.get().getNetzbezug().getImmutableKnotenBezug()).hasSize(1);
		assertThat(
			massnahmeAfterImport.get().getNetzbezug().getImmutableKnotenBezug().iterator().next())
				.isEqualTo(knoten2);

		assertThat(barriereAfterImport.get().getNetzbezug().getImmutableKnotenBezug()).hasSize(1);
		assertThat(
			barriereAfterImport.get().getNetzbezug().getImmutableKnotenBezug().iterator().next())
				.isEqualTo(knoten2);

		assertThat(furtKreuzungAfterImport.get().getNetzbezug().getImmutableKnotenBezug()).hasSize(1);
		assertThat(
			furtKreuzungAfterImport.get().getNetzbezug().getImmutableKnotenBezug().iterator().next())
				.isEqualTo(knoten2);

		DlmReimportJobStatistik dlmReimportJobStatistik = (DlmReimportJobStatistik) statistik.get();
		assertThat(dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKnotenErsetzt).isEqualTo(0);
		assertThat(dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKnotenGeloescht).isEqualTo(3);
	}

	@Test
	void geloeschterKnotenInNetzbezug_updateNetzbezug() {
		// arrange
		Knoten knoten1 = netzService
			.saveKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build());
		Knoten knoten2 = netzService
			.saveKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).build());
		Knoten knoten3 = netzService
			.saveKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100.9), QuellSystem.DLM).build());
		Kante kante1 = netzService
			.saveKante(KanteTestDataProvider.fromKnoten(knoten1, knoten2).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build());
		Kante kante2 = netzService
			.saveKante(KanteTestDataProvider.fromKnoten(knoten2, knoten3).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("456")).build());

		Massnahme massnahme = MassnahmeTestDataProvider.withKnoten(knoten1, knoten3).zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(benutzer).build();
		Barriere barriere = BarriereTestDataProvider.withDefaultValues()
			.netzbezug(new BarriereNetzBezug(Collections.emptySet(),
				Collections.emptySet(),
				Set.of(knoten1, knoten3)))
			.verantwortlicheOrganisation(gebietskoerperschaft)
			.build();
		FurtKreuzung furtKreuzung = FurtKreuzungTestDataProvider.withDefaultValues()
			.netzbezug(new FurtKreuzungNetzBezug(Collections.emptySet(),
				Collections.emptySet(),
				Set.of(knoten1, knoten3)))
			.verantwortlicheOrganisation(gebietskoerperschaft)
			.build();

		massnahmeRepository.save(massnahme);
		barriereRepository.save(barriere);
		furtKreuzungRepository.save(furtKreuzung);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().getCoordinates())
						.fachId(kante1.getDlmId().getValue()).build()));

		// act
		Optional<JobStatistik> statistik = dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		List<Kante> kanten = StreamSupport
			.stream(kantenRepository.findAll().spliterator(), false).toList();
		assertThat(kanten).hasSize(1);
		assertThat(kanten).doesNotContain(kante2);

		List<Knoten> knoten = StreamSupport
			.stream(knotenRepository.findAll().spliterator(), false).toList();
		assertThat(knoten).hasSize(2);
		assertThat(knoten).doesNotContain(knoten3);

		Optional<Massnahme> massnahmeAfterImport = StreamSupport
			.stream(massnahmeRepository.findAll().spliterator(), false).findAny();
		Optional<Barriere> barriereAfterImport = StreamSupport
			.stream(barriereRepository.findAll().spliterator(), false).findAny();
		Optional<FurtKreuzung> furtKreuzungAfterImport = StreamSupport
			.stream(furtKreuzungRepository.findAll().spliterator(), false).findAny();

		assertThat(
			massnahmeAfterImport.get().getNetzbezug().getImmutableKnotenBezug()).containsExactlyInAnyOrder(knoten1,
				knoten2);
		assertThat(
			barriereAfterImport.get().getNetzbezug().getImmutableKnotenBezug()).containsExactlyInAnyOrder(knoten1,
				knoten2);
		assertThat(
			furtKreuzungAfterImport.get().getNetzbezug().getImmutableKnotenBezug()).containsExactlyInAnyOrder(knoten1,
				knoten2);

		DlmReimportJobStatistik dlmReimportJobStatistik = (DlmReimportJobStatistik) statistik.get();
		assertThat(dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKnotenErsetzt).isEqualTo(3);
		assertThat(dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKnotenGeloescht).isEqualTo(0);
	}

	@Test
	void vernetzeRadvisKantenNeu_withinToleranz_updateKnoten() {
		// arrange
		Kante kante1 = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build());
		Kante kante2 = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(80, 0, 80, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("456")).build());

		Kante radvisKante1 = netzService.saveKante(new Kante(kante1.getVonKnoten(), kante2.getVonKnoten()));
		Kante radvisKante2 = netzService.saveKante(new Kante(kante1.getNachKnoten(), kante2.getNachKnoten()));

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().getCoordinates())
						.fachId(kante1.getDlmId().getValue()).build(),
					ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(80, 0), new Coordinate(80, 90))
						.fachId(kante2.getDlmId().getValue()).build()));

		entityManager.flush();
		entityManager.clear();

		// act
		Optional<JobStatistik> statistik = dlmReimportJob.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(knotenRepository.count()).isEqualTo(4);

		Optional<Kante> radvisKante1After = kantenRepository.findById(radvisKante1.getId());
		Optional<Kante> radvisKante2After = kantenRepository.findById(radvisKante2.getId());

		Kante kante2After = kantenRepository.findAllByDlmIdIn(List.of(kante2.getDlmId())).get(0);
		Kante kante1After = kantenRepository.findAllByDlmIdIn(List.of(kante1.getDlmId())).get(0);

		assertThat(radvisKante2After.get().getNachKnoten()).isEqualTo(kante2After.getNachKnoten());
		assertThat(radvisKante2After.get().getVonKnoten()).isEqualTo(kante1After.getNachKnoten());

		assertThat(radvisKante1After.get().getVonKnoten()).isEqualTo(kante1After.getVonKnoten());
		assertThat(radvisKante1After.get().getNachKnoten()).isEqualTo(kante2After.getVonKnoten());

		assertThat(((DlmReimportJobStatistik) statistik.get()).anzahlGeloeschterKnoten).isEqualTo(1);
		assertThat(((DlmReimportJobStatistik) statistik.get()).anzahlVernetzungsfehlerNachJobausfuehrung)
			.isEqualTo(0);
		assertThat(((DlmReimportJobStatistik) statistik
			.get()).radvisKantenVernetzungStatistik.anzahlRadVisKantenTopologieKorrigiert).isEqualTo(1);
		assertThat(
			((DlmReimportJobStatistik) statistik.get()).radvisKantenVernetzungStatistik.anzahlRadVisKantenBetrachtet)
				.isEqualTo(2);
	}

	@Test
	void vernetzeRadvisKantenNeu_outsideToleranz_keepOldKnoten() {
		// arrange
		Kante kante1 = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("123")).build());
		Kante kante2 = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(80, 0, 80, 100, QuellSystem.DLM)
				.dlmId(DlmId.of("456")).build());

		Kante radvisKante1 = netzService.saveKante(new Kante(kante1.getVonKnoten(), kante2.getVonKnoten()));
		Kante radvisKante2 = netzService.saveKante(new Kante(kante1.getNachKnoten(), kante2.getNachKnoten()));

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().getCoordinates())
						.fachId(kante1.getDlmId().getValue()).build(),
					ImportedFeatureTestDataProvider
						.withLineString(new Coordinate(80, 0), new Coordinate(80, 80))
						.fachId(kante2.getDlmId().getValue()).build()));

		entityManager.flush();
		entityManager.clear();

		// act
		dlmReimportJob.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(knotenRepository.count()).isEqualTo(5);

		Optional<Kante> radvisKante1After = kantenRepository.findById(radvisKante1.getId());
		Optional<Kante> radvisKante2After = kantenRepository.findById(radvisKante2.getId());

		Kante kante2After = kantenRepository.findAllByDlmIdIn(List.of(kante2.getDlmId())).get(0);
		Kante kante1After = kantenRepository.findAllByDlmIdIn(List.of(kante1.getDlmId())).get(0);

		assertThat(radvisKante2After.get().getNachKnoten()).isEqualTo(radvisKante2.getNachKnoten());
		assertThat(radvisKante2After.get().getVonKnoten()).isEqualTo(kante1After.getNachKnoten());

		assertThat(radvisKante1After.get().getVonKnoten()).isEqualTo(kante1After.getVonKnoten());
		assertThat(radvisKante1After.get().getNachKnoten()).isEqualTo(kante2After.getVonKnoten());
	}
}
