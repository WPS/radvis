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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.RevisionSort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.entity.RevInfo;
import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.MappedKante;
import de.wps.radvis.backend.integration.dlm.IntegrationDlmConfiguration;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeNetzBezugAenderungRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeUmsetzungsstandViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.repository.CustomDlmMatchingRepositoryFactory;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.service.CustomGrundnetzMappingServiceFactory;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GrundnetzMappingService;
import de.wps.radvis.backend.matching.domain.service.KanteUpdateElevationService;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopper;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group7")
@ContextConfiguration(classes = {
	IntegrationDlmConfiguration.class,
	NetzConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	DlmReimportJobAuditingTestIT.TestConfiguration.class,
	BenutzerConfiguration.class,
	ImportsGrundnetzConfiguration.class,
	CommonConfiguration.class,
	MatchingConfiguration.class,
	BarriereConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	CommonConfigurationProperties.class,
	DLMConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	NetzkorrekturConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
@ActiveProfiles("dev")
@RecordApplicationEvents
class DlmReimportJobAuditingTestIT extends AuditingTestIT {

	@EnableJpaRepositories(basePackages = "de.wps.radvis.backend.massnahme")
	@EntityScan(basePackages = { "de.wps.radvis.backend.massnahme", "de.wps.radvis.backend.kommentar",
		"de.wps.radvis.backend.dokument" })
	public static class TestConfiguration {
		@Bean
		public NetzfehlerRepository netzfehlerRepository() {
			return Mockito.mock(NetzfehlerRepository.class);
		}

		@Bean
		public ImportedFeaturePersistentRepository importedFeaturePersistentRepository() {
			return Mockito.mock(ImportedFeaturePersistentRepository.class);
		}

		@MockBean
		public RadNetzNetzbildungService radNetzNetzbildungService;
		@MockBean
		public RadwegeDBNetzbildungService radwegeDBNetzbildungService;

		@Autowired
		MassnahmeRepository massnahmeRepository;

		@Autowired
		private KantenRepository kantenRepository;

		@Autowired
		private NetzService netzService;

		@Autowired
		private BenutzerService benutzerService;

		@MockBean
		private MassnahmeViewRepository massnahmeViewRepository;

		@MockBean
		private MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository;

		@MockBean
		private UmsetzungsstandRepository umsetzungsstandRepository;

		@MockBean
		private FahrradrouteRepository fahrradrouteRepository;

		@Bean
		public MassnahmeService massnahmeService() {
			return new MassnahmeService(massnahmeRepository, massnahmeViewRepository,
				massnahmeUmsetzungsstandViewRepository, umsetzungsstandRepository, kantenRepository,
				massnahmeNetzbezugAenderungProtokollierungsService(), benutzerService, fahrradrouteRepository,
				netzService, 20, 1.0);
		}

		@Autowired
		private MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository;

		@Bean
		public MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService() {
			return new MassnahmeNetzbezugAenderungProtokollierungsService(massnahmeNetzBezugAenderungRepository);
		}
	}

	private DlmReimportJob dlmReimportJob;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NetzService netzService;

	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private KantenMappingRepository kantenMappingRepository;
	@Autowired
	private BenutzerRepository benutzerRepository;
	@Autowired
	private KnotenRepository knotenRepository;

	@MockBean
	private DlmPbfErstellungService dlmPbfErstellungService;
	@MockBean
	private CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactory;
	@MockBean
	private CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Autowired
	private ApplicationEvents applicationEvents;

	@Mock
	private DlmRepository dlmImportRepository;
	@Mock
	private KanteUpdateElevationService elevationUpdateService;
	@Mock
	private SimpleMatchingService simpleMatchingService;
	@Mock
	private DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory;
	@Mock
	private DlmMatchedGraphHopper dlmMatchedGraphHopper;

	private Kante dlmKante1;

	private Kante dlmKante2;

	private Kante dlmKante3;

	@BeforeEach
	void setup() {
		openMocks(this);

		// technischen Benutzer anlegen
		Benutzer technischerBenutzer = BenutzerTestDataProvider.technischerBenutzer().build();
		if (benutzerRepository.findByServiceBwId(technischerBenutzer.getServiceBwId()).isEmpty()) {
			gebietskoerperschaftRepository.save((Gebietskoerperschaft) technischerBenutzer.getOrganisation());
			benutzerRepository.save(technischerBenutzer);
		}

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
			new DlmImportService(dlmImportRepository, netzService),
			customDlmMatchingRepositoryFactory, customGrundnetzMappingServiceFactory);
	}

	private void erstelleVeraltetesBasisnetz() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("testOrganisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build());

		Knoten knoten1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build());
		Knoten knoten2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 0), QuellSystem.DLM).build());
		Knoten knoten3 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 0), QuellSystem.DLM).build());
		Knoten knoten4 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 0), QuellSystem.DLM).build());
		Knoten knoten5 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(40, 0), QuellSystem.DLM).build());

		KantenAttribute kantenAttribute1 = KantenAttributeTestDataProvider
			.withLeereGrundnetzAttribute()
			.strassenName(StrassenName.of("bestehendeKante1"))
			.strassenNummer(StrassenNummer.of("16"))
			.beleuchtung(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG)
			.umfeld(Umfeld.GESCHAEFTSSTRASSE)
			.kommentar(Kommentar.of("testBestehendeKante1")).build();

		dlmKante1 = netzService.saveKante(KanteTestDataProvider.fromKnoten(knoten1, knoten2)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("1"))
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.kantenAttribute(kantenAttribute1)
					.netzklassen(Set.of(Netzklasse.KREISNETZ_ALLTAG))
					.istStandards(Set.of(IstStandard.BASISSTANDARD))
					.build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder()
				.zustaendigkeitAttribute(List.of(ZustaendigkeitAttributGruppeTestDataProvider
					.withLineareReferenz(0.0, 1.0)
					.erhaltsZustaendiger(gebietskoerperschaft)
					.vereinbarungsKennung(VereinbarungsKennung.of("ABC")).build()))
				.build())
			.build());

		KantenAttribute kantenAttribute2 = KantenAttributeTestDataProvider
			.withLeereGrundnetzAttribute()
			.strassenName(StrassenName.of("bestehendeKante2"))
			.strassenNummer(StrassenNummer.of("27"))
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.GEWERBEGEBIET)
			.kommentar(Kommentar.of("testBestehendeKante2")).build();

		dlmKante2 = netzService.saveKante(KanteTestDataProvider.fromKnoten(knoten2, knoten3)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("2"))
			.kantenAttributGruppe(
				KantenAttributGruppe.builder().kantenAttribute(kantenAttribute2)
					.netzklassen(Set.of(Netzklasse.KREISNETZ_ALLTAG))
					.istStandards(Set.of(IstStandard.BASISSTANDARD))
					.build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder()
				.zustaendigkeitAttribute(List.of(ZustaendigkeitAttributGruppeTestDataProvider
					.withLineareReferenz(0.0, 0.5)
					.erhaltsZustaendiger(gebietskoerperschaft)
					.vereinbarungsKennung(VereinbarungsKennung.of("DEF")).build(),
					ZustaendigkeitAttributGruppeTestDataProvider
						.withLineareReferenz(0.5, 1.0)
						.erhaltsZustaendiger(gebietskoerperschaft)
						.vereinbarungsKennung(VereinbarungsKennung.of("GHI")).build()))
				.build())
			.build());

		KantenAttribute kantenAttribute3 = KantenAttributeTestDataProvider
			.withLeereGrundnetzAttribute()
			.strassenName(StrassenName.of("bestehendeKante3"))
			.strassenNummer(StrassenNummer.of("38"))
			.beleuchtung(Beleuchtung.NICHT_VORHANDEN)
			.umfeld(Umfeld.UNBEKANNT)
			.kommentar(Kommentar.of("testBestehendeKante3"))
			.build();

		dlmKante3 = netzService.saveKante(KanteTestDataProvider.fromKnoten(knoten3, knoten4)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("3"))
			.kantenAttributGruppe(
				KantenAttributGruppe.builder().kantenAttribute(kantenAttribute3)
					.netzklassen(Set.of(Netzklasse.KREISNETZ_ALLTAG))
					.istStandards(Set.of(IstStandard.BASISSTANDARD))
					.build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder()
				.zustaendigkeitAttribute(List.of(ZustaendigkeitAttributGruppeTestDataProvider
					.withLineareReferenz(0.0, 1.0)
					.erhaltsZustaendiger(gebietskoerperschaft)
					.vereinbarungsKennung(VereinbarungsKennung.of("GHI")).build()))
				.build())
			.build());

		KantenAttribute kantenAttribute4 = KantenAttributeTestDataProvider
			.withLeereGrundnetzAttribute()
			.strassenName(StrassenName.of("radVisStrasse"))
			.build();

		// RadVis Kante zwischen zwei DLM-Knoten
		netzService.saveKante(KanteTestDataProvider
			.fromKnotenUndQuelle(knoten4, knoten5, QuellSystem.RadVis)
			.kantenAttributGruppe(KantenAttributGruppe.builder().kantenAttribute(kantenAttribute4).build())
			.build());

		// sollten nie verändert werden
		Knoten radNetzKnoten1 = netzService.saveKnoten(KnotenTestDataProvider
			.withCoordinateAndQuelle(new Coordinate(0, 10), QuellSystem.RadNETZ)
			.build());
		Knoten radNetzKnoten2 = netzService.saveKnoten(KnotenTestDataProvider
			.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.RadNETZ)
			.build());
		Kante radNETZKante = netzService.saveKante(KanteTestDataProvider
			.fromKnoten(radNetzKnoten1, radNetzKnoten2)
			.dlmId(null)
			.quelle(QuellSystem.RadNETZ).build());

		entityManager.flush();

		kantenMappingRepository.save(new KantenMapping(dlmKante2.getId(), QuellSystem.RadNETZ,
			List.of(
				new MappedKante(LinearReferenzierterAbschnitt.of(0, 1), LinearReferenzierterAbschnitt.of(0, 1), false,
					radNETZKante.getId()))));

		Kante radwegeDBKante = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 10, 10, QuellSystem.RadwegeDB).build());

		kantenMappingRepository.save(new KantenMapping(dlmKante2.getId(), QuellSystem.RadwegeDB, List.of(
			new MappedKante(LinearReferenzierterAbschnitt.of(0, 1), LinearReferenzierterAbschnitt.of(0, 1), false,
				radwegeDBKante.getId()))));

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void writesCorrectAuditingEintraege() {
		// arrange
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		Envelope bereich = new Envelope(0, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		transactionTemplate.executeWithoutResult((status) -> {
			erstelleVeraltetesBasisnetz();
			netzService.refreshNetzMaterializedViews();
		});
		transactionTemplate.executeWithoutResult((status) -> assertThat(kantenRepository.findAll()).hasSize(6));

		Stream<ImportedFeature> importedFeatures = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(dlmKante1.getGeometry().getCoordinates())
				.fachId(dlmKante1.getDlmId().getValue())
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(dlmKante2.getVonKnoten().getKoordinate(), new Coordinate(69, 54),
					dlmKante2.getNachKnoten().getKoordinate())
				.fachId(dlmKante2.getDlmId().getValue())
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(100, 20), new Coordinate(80, 80))
				.fachId("789")
				.build());

		when(dlmImportRepository.getKanten(bereich)).thenReturn(importedFeatures.toList());

		// act
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.DLM_REIMPORT_JOB);
		transactionTemplate.executeWithoutResult((status) -> dlmReimportJob.run());

		// assert
		Iterator<Revision<Long, Kante>> iterator = kantenRepository.findRevisions(dlmKante1.getId(),
			PageRequest.of(0, 2, RevisionSort.desc())).iterator();

		assertThat(iterator.hasNext()).isTrue();
		RevisionMetadata<Long> revisionMetadata = iterator.next().getMetadata();
		assertThat(revisionMetadata.getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.INSERT);
		assertThat(((RevInfo) revisionMetadata.getDelegate()).getAuditingContext())
			.isNotEqualTo(AuditingContext.DLM_REIMPORT_JOB);

		iterator = kantenRepository.findRevisions(dlmKante2.getId(),
			PageRequest.of(0, 2, RevisionSort.desc())).iterator();

		assertThat(iterator.hasNext()).isTrue();
		revisionMetadata = iterator.next().getMetadata();
		assertThat(revisionMetadata.getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.UPDATE);
		assertThat(((RevInfo) revisionMetadata.getDelegate()).getAuditingContext())
			.isEqualTo(AuditingContext.DLM_REIMPORT_JOB);

		iterator = kantenRepository.findRevisions(dlmKante3.getId(),
			PageRequest.of(0, 2, RevisionSort.desc())).iterator();

		assertThat(iterator.hasNext()).isTrue();
		revisionMetadata = iterator.next().getMetadata();
		assertThat(revisionMetadata.getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.DELETE);
		assertThat(((RevInfo) revisionMetadata.getDelegate()).getAuditingContext())
			.isEqualTo(AuditingContext.DLM_REIMPORT_JOB);

		Optional<Kante> newDlmKante = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
			.filter(k -> k.getQuelle().equals(QuellSystem.DLM) && k.getDlmId().getValue().equals("789")).findAny();
		assertThat(newDlmKante).isPresent();
		iterator = kantenRepository.findRevisions(newDlmKante.get().getId(),
			PageRequest.of(0, 2, RevisionSort.desc())).iterator();

		assertThat(iterator.hasNext()).isTrue();
		revisionMetadata = iterator.next().getMetadata();
		assertThat(revisionMetadata.getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.INSERT);
		assertThat(((RevInfo) revisionMetadata.getDelegate()).getAuditingContext())
			.isEqualTo(AuditingContext.DLM_REIMPORT_JOB);
	}

	@Test
	void materializedViewsNotRefreshedWithinThisJob() {
		// arrange
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		Envelope bereich = new Envelope(0, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// Repositories mit bestehende Knoten und Kanten fuellen
		transactionTemplate.executeWithoutResult((status) -> {
			erstelleVeraltetesBasisnetz();
			netzService.refreshNetzMaterializedViews();
		});
		transactionTemplate.executeWithoutResult((status) -> assertThat(kantenRepository.findAll()).hasSize(6));

		// Kante 4 ist neu, sonst alles beim alten
		Stream<ImportedFeature> importedFeatures = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 0), new Coordinate(10, 0))
				.fachId("1")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(10, 0), new Coordinate(20, 0))
				.fachId("2")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(20, 0), new Coordinate(30, 0))
				.fachId("3")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(30, 0), new Coordinate(40, 0))
				.fachId("4")
				.build());

		when(dlmImportRepository.getKanten(bereich)).thenReturn(importedFeatures.toList());
		applicationEvents.clear();

		// act
		List<Map<String, Object>> allViewEntriesBeforeRefresh = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntriesBeforeRefresh).hasSize(4);

		transactionTemplate.executeWithoutResult((status) -> dlmReimportJob.run());

		// assert
		assertThat(kantenRepository.findAll()).hasSize(7);
		List<Map<String, Object>> allViewEntriesAfterRefresh = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntriesAfterRefresh).hasSize(4);
	}
}
