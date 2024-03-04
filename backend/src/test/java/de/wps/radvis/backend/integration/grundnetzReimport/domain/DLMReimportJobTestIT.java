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

package de.wps.radvis.backend.integration.grundnetzReimport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.MappedKante;
import de.wps.radvis.backend.integration.grundnetz.IntegrationGrundnetzConfiguration;
import de.wps.radvis.backend.integration.grundnetzReimport.GrundnetzReimportConfiguration;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.manuellerimport.common.ManuellerImportCommonConfiguration;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezugAenderung;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeNetzBezugAenderungRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeUmsetzungsstandViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.KanteUpdateElevationService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KanteTopologieChangedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
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
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMWFSImportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group3")
@ContextConfiguration(classes = {
	GrundnetzReimportConfiguration.class,
	NetzConfiguration.class,
	IntegrationGrundnetzConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	DLMReimportJobTestIT.TestConfiguration.class,
	BenutzerConfiguration.class,
	ImportsGrundnetzConfiguration.class,
	CommonConfiguration.class,
	ManuellerImportCommonConfiguration.class,
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
	GraphhopperDlmConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
@ActiveProfiles("dev")
@RecordApplicationEvents
class DLMReimportJobTestIT extends DBIntegrationTestIT {

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
		private BenutzerService benutzerService;

		@MockBean
		private MassnahmeViewRepository massnahmeViewRepository;

		@MockBean
		private MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository;

		@MockBean
		private UmsetzungsstandRepository umsetzungsstandRepository;

		@Bean
		public MassnahmeService massnahmeService() {
			return new MassnahmeService(massnahmeRepository, massnahmeViewRepository,
				massnahmeUmsetzungsstandViewRepository, umsetzungsstandRepository, kantenRepository,
				massnahmeNetzbezugAenderungProtokollierungsService());
		}

		@Autowired
		private MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository;

		@Bean
		public MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService() {
			return new MassnahmeNetzbezugAenderungProtokollierungsService(benutzerService,
				massnahmeNetzBezugAenderungRepository);
		}
	}

	private DLMReimportJob dlmReimportJob;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private NetzService netzService;

	@Autowired
	private UpdateKantenService updateKantenService;
	@Autowired
	private CreateKantenService createKantenService;
	@Autowired
	private ExecuteTopologischeUpdatesService executeTopologischeUpdatesService;
	@Autowired
	private KnotenRepository knotenRepository;
	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private KantenMappingRepository kantenMappingRepository;
	@Autowired
	private MassnahmeRepository massnahmeRepository;
	@Autowired
	private BenutzerRepository benutzerRepository;
	@Autowired
	private MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository;
	@Autowired
	private ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Autowired
	private ApplicationEvents applicationEvents;

	@Mock
	private DLMWFSImportRepository dlmImportRepository;
	@Mock
	private KanteUpdateElevationService kanteUpdateElevationService;

	MockedStatic<AdditionalRevInfoHolder> auditingContextServiceMockedStatic;

	@BeforeEach
	void setup() {
		openMocks(this);

		// technischen Benutzer anlegen
		Benutzer technischerBenutzer = BenutzerTestDataProvider.technischerBenutzer().build();
		if (benutzerRepository.findByServiceBwId(technischerBenutzer.getServiceBwId()).isEmpty()) {
			gebietskoerperschaftRepository.save((Gebietskoerperschaft) technischerBenutzer.getOrganisation());
			benutzerRepository.save(technischerBenutzer);
		}

		dlmReimportJob = new DLMReimportJob(
			jobExecutionDescriptionRepository,
			dlmImportRepository, netzService, updateKantenService,
			createKantenService, executeTopologischeUpdatesService, kantenMappingRepository,
			entityManager, new VernetzungService(kantenRepository, knotenRepository, netzService),
			kanteUpdateElevationService);

		auditingContextServiceMockedStatic = mockStatic(AdditionalRevInfoHolder.class);
	}

	@AfterEach
	void cleanUp() {
		auditingContextServiceMockedStatic.close();
	}

	List<Knoten> basisNetzKnoten = new ArrayList<>();
	List<Kante> basisNetzKanten = new ArrayList<>();

	private void erstelleVeraltetesBasisnetz() {
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
		// Das macht fachlich nicht so viel Sinn, aber sonst wird der Knoten am Ende weggeschmissen, weil wir nur
		// DLM-Knoten behalten
		Knoten knoten5 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(40, 0), QuellSystem.RadVis).build());

		basisNetzKnoten.addAll(List.of(knoten1, knoten2, knoten3, knoten4, knoten5));

		KantenAttribute kantenAttribute1 = KantenAttributeTestDataProvider
			.withLeereGrundnetzAttribute()
			.strassenName(StrassenName.of("bestehendeKante1"))
			.strassenNummer(StrassenNummer.of("16"))
			.beleuchtung(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG)
			.umfeld(Umfeld.GESCHAEFTSSTRASSE)
			.kommentar(Kommentar.of("testBestehendeKante1")).build();

		Kante kante1 = netzService.saveKante(KanteTestDataProvider.fromKnoten(knoten1, knoten2)
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

		Kante kante2 = netzService.saveKante(KanteTestDataProvider.fromKnoten(knoten2, knoten3)
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

		Kante kante3 = netzService.saveKante(KanteTestDataProvider.fromKnoten(knoten3, knoten4)
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
		Kante kante4 = netzService.saveKante(KanteTestDataProvider
			.fromKnotenUndQuelle(knoten4, knoten5, QuellSystem.RadVis)
			.kantenAttributGruppe(KantenAttributGruppe.builder().kantenAttribute(kantenAttribute4).build())
			.build());

		basisNetzKanten.addAll(List.of(kante1, kante2, kante3, kante4));

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

		kantenMappingRepository.save(new KantenMapping(kante2.getId(), QuellSystem.RadNETZ,
			List.of(
				new MappedKante(LinearReferenzierterAbschnitt.of(0, 1), LinearReferenzierterAbschnitt.of(0, 1), false,
					radNETZKante.getId()))));

		Kante radwegeDBKante = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 10, 10, QuellSystem.RadwegeDB).build());

		kantenMappingRepository.save(new KantenMapping(kante2.getId(), QuellSystem.RadwegeDB, List.of(
			new MappedKante(LinearReferenzierterAbschnitt.of(0, 1), LinearReferenzierterAbschnitt.of(0, 1), false,
				radwegeDBKante.getId()))));

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	public void testeDoRun_keineGeometrieAenderungen_kanteUeberZweiPartitionen_nurAttributeVeraendert() {
		// ---------------------------- arrange ------------------------------

		Envelope bereich1 = new Envelope(0, 13, 0, 50);
		Envelope bereich2 = new Envelope(13, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich1, bereich2);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// Repositories mit bestehende Knoten und Kanten fuellen
		erstelleVeraltetesBasisnetz();

		// Zu importierende features festlegen
		Map<String, Object> featureAttribute1 = new HashMap<>();
		featureAttribute1.put("eigenname", "bestehendeKante1"); // Strassenname
		featureAttribute1.put("bezeichnung", "16"); // Strassennummer
		Map<String, Object> featureAttribute2 = new HashMap<>();
		featureAttribute2.put("eigenname", "bestehendeKante2");
		featureAttribute2.put("bezeichnung", "27");
		Map<String, Object> featureAttribute3 = new HashMap<>();
		featureAttribute3.put("eigenname", "andererStrassenname3");
		featureAttribute3.put("bezeichnung", "42");

		Stream<ImportedFeature> importedFeatureBereich1 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 0), new Coordinate(10, 0))
				.fachId("1")
				.attribute(featureAttribute1)
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(10, 0), new Coordinate(20, 0))
				.fachId("2")
				.attribute(featureAttribute2)
				.build());
		Stream<ImportedFeature> importedFeatureBereich2 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(20, 0), new Coordinate(30, 0))
				.fachId("3")
				.attribute(featureAttribute3)
				.build());

		when(dlmImportRepository.readStrassenFeatures(bereich1)).thenReturn(importedFeatureBereich1);
		when(dlmImportRepository.readStrassenFeatures(bereich2)).thenReturn(importedFeatureBereich2);

		applicationEvents.clear();

		// ------------------------------ act --------------------------------
		dlmReimportJob.run();

		// ---------------------------- assert -------------------------------
		// Knoten bleiben unverändert
		assertThat(Stream.concat(knotenRepository.findKnotenByQuelle(QuellSystem.DLM),
			knotenRepository.findKnotenByQuelle(QuellSystem.RadVis)))
			.containsExactlyInAnyOrderElementsOf(basisNetzKnoten);

		// Kanten
		List<Kante> alleKanten = findAllDLMAndRadVisKanten();
		assertThat(alleKanten).containsExactlyInAnyOrderElementsOf(basisNetzKanten);
		assertThat(alleKanten).map(Kante::getGeometry)
			.containsExactlyInAnyOrderElementsOf(basisNetzKanten.stream().map(Kante::getGeometry).collect(
				Collectors.toList()));

		List<Kante> dlmKanten = alleKanten.stream().filter(k -> QuellSystem.DLM == k.getQuelle())
			.collect(Collectors.toList());

		List<KantenAttribute> kantenAttribute = dlmKanten.stream().map(Kante::getKantenAttributGruppe)
			.map(KantenAttributGruppe::getKantenAttribute).collect(
				Collectors.toList());
		// Strassenname
		List<String> expectedStrassenNamen = List.of("bestehendeKante1", "bestehendeKante2", "andererStrassenname3");
		assertThat(kantenAttribute)
			.map(KantenAttribute::getStrassenName)
			.allMatch(Optional::isPresent)
			.map(Optional::get)
			.map(StrassenName::toString)
			.containsExactlyElementsOf(expectedStrassenNamen);

		// Beleuchtung stellvertretend fuer die Attribute
		assertThat(kantenAttribute).map(KantenAttribute::getBeleuchtung)
			.containsExactlyInAnyOrder(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG, Beleuchtung.VORHANDEN,
				Beleuchtung.NICHT_VORHANDEN);

		assertThatRadNETZUnberuehrt();

		assertThat(applicationEvents.stream()).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testeDoRun_SplitOhneVerschiebung_zweiPartitionen_kanteAufgeteiltUndAttributeVonAlterKanteUebernommen() {
		// ---------------------------- arrange ------------------------------

		Envelope bereich1 = new Envelope(0, 13, 0, 50);
		Envelope bereich2 = new Envelope(13, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich1, bereich2);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// Repositories mit bestehende Knoten und Kanten fuellen
		erstelleVeraltetesBasisnetz();

		// Zu importierende features festlegen
		// Attribute der zu importierenden Features erstellen
		Map<String, Object> featureAttribute1 = new HashMap<>();
		featureAttribute1.put("eigenname", "bestehendeKante1"); // Strassenname
		featureAttribute1.put("bezeichnung", "16"); // Strassennummer
		Map<String, Object> featureAttribute2 = new HashMap<>();
		featureAttribute2.put("eigenname", "bestehendeKante2");
		featureAttribute2.put("bezeichnung", "27");
		Map<String, Object> featureAttribute3 = new HashMap<>();
		featureAttribute3.put("eigenname", "bestehendeKante3");
		featureAttribute3.put("bezeichnung", "38");

		// Attribute der Kante, die durch den Split neu hinzugefügt wird
		Map<String, Object> featureAttribute4 = new HashMap<>();
		featureAttribute4.put("eigenname", "neueStrasse");
		featureAttribute4.put("bezeichnung", "42");

		// Vier DLM-Kanten als importedFeatures erstellen mit passenden fachIds zu den schon vorhandenen Kanten
		// und einer neuen, die durch den Split hinzugefügt worden ist mit neuer fachId.
		// Die alte Kante mit der ID 2 sollte in die Kanten mit den IDs 2 und 4 gesplittet werden.
		Coordinate splitKoordinate = new Coordinate(15, 0);

		Stream<ImportedFeature> importedFeatureBereich1 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 0), new Coordinate(10, 0))
				.fachId("1")
				.attribute(featureAttribute1)
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject() // Gesplittetes Feature
				.lineString(new Coordinate(10, 0), splitKoordinate)
				.fachId("2")
				.attribute(featureAttribute2)
				.build());

		Stream<ImportedFeature> importedFeatureBereich2 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject() // neues Feature
				.lineString(splitKoordinate, new Coordinate(20, 0))
				.fachId("4")
				.attribute(featureAttribute4)
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(20, 0), new Coordinate(30, 0))
				.fachId("3")
				.attribute(featureAttribute3)
				.build());

		when(dlmImportRepository.readStrassenFeatures(bereich1)).thenReturn(importedFeatureBereich1);
		when(dlmImportRepository.readStrassenFeatures(bereich2)).thenReturn(importedFeatureBereich2);

		applicationEvents.clear();

		// ------------------------------ act --------------------------------
		dlmReimportJob.run();

		// ---------------------------- assert -------------------------------

		// Knoten
		List<Coordinate> expectedKoordinaten = basisNetzKnoten.stream().map(Knoten::getKoordinate)
			.collect(Collectors.toList());
		expectedKoordinaten.add(splitKoordinate);

		assertThat(Stream.concat(knotenRepository.findKnotenByQuelle(QuellSystem.DLM),
			knotenRepository.findKnotenByQuelle(QuellSystem.RadVis)))
			.hasSize(6)
			.containsAll(basisNetzKnoten)
			.map(Knoten::getKoordinate)
			.containsExactlyInAnyOrderElementsOf(expectedKoordinaten);

		// Kanten
		List<Kante> alleKanten = findAllDLMAndRadVisKanten().stream()
			.sorted(Comparator.comparing(kante -> kante.getVonKnoten().getPoint().getX())).collect(Collectors.toList());

		List<LineString> expectedLineStrings = List.of(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10.0, 0)),
			GeometryTestdataProvider.createLineString(new Coordinate(10, 0), new Coordinate(15.0, 0)),
			GeometryTestdataProvider.createLineString(new Coordinate(15, 0), new Coordinate(20.0, 0)),
			GeometryTestdataProvider.createLineString(new Coordinate(20, 0), new Coordinate(30.0, 0)),
			GeometryTestdataProvider.createLineString(new Coordinate(30, 0), new Coordinate(40.0, 0)));

		assertThat(alleKanten)
			.hasSize(5)
			.containsAll(basisNetzKanten)
			.map(Kante::getGeometry)
			.containsExactlyInAnyOrderElementsOf(expectedLineStrings);

		// Strecke bleibt topologisch erhalten
		for (int i = 0; i < alleKanten.size() - 1; i++) {
			assertThat(alleKanten.get(i).getNachKnoten()).isEqualTo(alleKanten.get(i + 1).getVonKnoten());
		}

		// Strassenname
		List<String> expectedStrassenNamen = List.of("bestehendeKante1", "bestehendeKante2",
			"neueStrasse", "bestehendeKante3", "radVisStrasse");
		assertThat(alleKanten)
			.map(kante -> kante.getKantenAttributGruppe().getKantenAttribute().getStrassenName())
			.allMatch(Optional::isPresent)
			.map(Optional::get)
			.map(StrassenName::toString)
			.containsExactlyInAnyOrderElementsOf(expectedStrassenNamen);

		// Beleuchtung stellvertretend fuer die Attribute
		List<Beleuchtung> expectedBeleuchtung = List.of(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG,
			Beleuchtung.VORHANDEN, Beleuchtung.VORHANDEN, Beleuchtung.NICHT_VORHANDEN, Beleuchtung.UNBEKANNT);
		assertThat(alleKanten).map(kante -> kante.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung())
			.containsExactlyElementsOf(expectedBeleuchtung);

		// Vereinbarungskennung stellvertretend fuer die linear referenzierten Attribute
		// an der veraenderten Kante 2 und neuen Kante 4 pruefen
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute2 = alleKanten.get(1).getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().stream().sorted(
				Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
					LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toUnmodifiableList());
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute4 = alleKanten.get(2).getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().stream().sorted(
				Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
					LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toUnmodifiableList());

		// Kante zwei ist laenger geworden und deshalb sollten die linear referenzierten Attribute
		// von 0 - 0.75 gleich bleiben. Von 0.75 bis 1.0 sollten sie on Kante drei uebernommen werden
		// Kante drei sollte seine linear referenzierten Attribute von 0 bis 1 behalten, obwohl sie kuerzer
		// geworden ist
		assertThat(zustaendigkeitAttribute2).hasSize(1);
		assertThat(zustaendigkeitAttribute4).hasSize(1);

		// Kante zwei 0 bis 0.33 "DEF"
		assertThat(zustaendigkeitAttribute2.get(0).getLinearReferenzierterAbschnitt().getVonValue())
			.isEqualTo(0.0, withPrecision(0.01));
		assertThat(zustaendigkeitAttribute2.get(0).getLinearReferenzierterAbschnitt().getBisValue())
			.isEqualTo(1.0, withPrecision(0.01));
		Optional<VereinbarungsKennung> vK2 = zustaendigkeitAttribute2.get(0).getVereinbarungsKennung();
		assertThat(vK2.isPresent()).isTrue();
		assertThat(vK2.get().toString()).isEqualTo("DEF");

		// Kante vier 0.0 bis 1.0 "GHI"
		assertThat(zustaendigkeitAttribute4.get(0).getLinearReferenzierterAbschnitt().getVonValue())
			.isEqualTo(0.0, withPrecision(0.01));
		assertThat(zustaendigkeitAttribute4.get(0).getLinearReferenzierterAbschnitt().getBisValue())
			.isEqualTo(1.0, withPrecision(0.01));
		Optional<VereinbarungsKennung> vK4 = zustaendigkeitAttribute4.get(0).getVereinbarungsKennung();
		assertThat(vK4.isPresent()).isTrue();
		assertThat(vK4.get().toString()).isEqualTo("GHI");

		assertThatRadNETZUnberuehrt();

		KanteTopologieChangedEvent[] kanteTopologieChangedEvents = applicationEvents.stream(
			KanteTopologieChangedEvent.class).toArray(KanteTopologieChangedEvent[]::new);
		assertThat(kanteTopologieChangedEvents)
			.usingRecursiveFieldByFieldElementComparatorOnFields("kanteId", "ausloeser")
			.containsExactly(
				new KanteTopologieChangedEvent(basisNetzKanten.get(1).getId(),
					basisNetzKanten.get(1).getGeometry(),
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()));
		assertThat(kanteTopologieChangedEvents[0].getGeometry()).isEqualTo(basisNetzKanten.get(1).getGeometry());

	}

	@Test
	public void testeDoRun_kanteWirdLaenger_linearReferenzierteAttributeWerdenAngepasst() {
		// ---------------------------- arrange ------------------------------

		Envelope bereich1 = new Envelope(0, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich1);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// Repositories mit bestehende Knoten und Kanten fuellen
		erstelleVeraltetesBasisnetz();

		// Zu importierende features festlegen
		// Attribute der zu importierenden Features erstellen
		Map<String, Object> featureAttribute1 = new HashMap<>();
		featureAttribute1.put("eigenname", "bestehendeKante1"); // Strassenname
		featureAttribute1.put("bezeichnung", "16"); // Strassennummer
		Map<String, Object> featureAttribute2 = new HashMap<>();
		featureAttribute2.put("eigenname", "neuerName2");
		featureAttribute2.put("bezeichnung", "42");
		Map<String, Object> featureAttribute3 = new HashMap<>();
		featureAttribute3.put("eigenname", "neuerName3");
		featureAttribute3.put("bezeichnung", "84");

		// Kante 2 wird länger, kante 3 wird kürzer
		Stream<ImportedFeature> importedFeatureBereich1 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 0), new Coordinate(10, 0))
				.fachId("1")
				.attribute(featureAttribute1)
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(10, 0), new Coordinate(25, 0))
				.fachId("2")
				.attribute(featureAttribute2)
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(25, 0), new Coordinate(30, 0))
				.fachId("3")
				.attribute(featureAttribute3)
				.build());

		when(dlmImportRepository.readStrassenFeatures(bereich1)).thenReturn(importedFeatureBereich1);

		applicationEvents.clear();

		// ------------------------------ act --------------------------------
		dlmReimportJob.run();

		// ---------------------------- assert -------------------------------

		// Knoten
		List<Knoten> bestehendeKnoten = basisNetzKnoten.stream()
			.filter(k -> !k.getKoordinate().equals(new Coordinate(20, 0)))
			.collect(Collectors.toList());
		assertThat(bestehendeKnoten).hasSize(4);
		assertThat(Stream.concat(knotenRepository.findKnotenByQuelle(QuellSystem.DLM),
			knotenRepository.findKnotenByQuelle(QuellSystem.RadVis)))
			.hasSize(5)
			.containsAll(bestehendeKnoten)
			.map(Knoten::getKoordinate)
			.containsExactlyInAnyOrder(new Coordinate(0, 0), new Coordinate(10, 0), new Coordinate(25, 0),
				new Coordinate(30, 0), new Coordinate(40, 0));

		// Kanten
		List<Kante> alleKanten = findAllDLMAndRadVisKanten().stream()
			.sorted(Comparator.comparing(kante -> kante.getVonKnoten().getPoint().getX())).collect(Collectors.toList());

		List<LineString> expectedLineStrings = List.of(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10.0, 0)),
			GeometryTestdataProvider.createLineString(new Coordinate(10, 0), new Coordinate(25.0, 0)),
			GeometryTestdataProvider.createLineString(new Coordinate(25, 0), new Coordinate(30.0, 0)),
			GeometryTestdataProvider.createLineString(new Coordinate(30, 0), new Coordinate(40.0, 0)));

		assertThat(alleKanten)
			.hasSize(4)
			.map(Kante::getGeometry)
			.containsExactlyElementsOf(expectedLineStrings);

		// Topologie der Strecke bleibt erhalten
		for (int i = 0; i < alleKanten.size() - 1; i++) {
			assertThat(alleKanten.get(i).getNachKnoten()).isEqualTo(alleKanten.get(i + 1).getVonKnoten());
		}

		// Strassenname
		List<String> expectedStrassenNamen = List.of("bestehendeKante1", "neuerName2", "neuerName3", "radVisStrasse");
		assertThat(alleKanten.stream()
			.map(kante -> kante.getKantenAttributGruppe().getKantenAttribute().getStrassenName().get().toString()))
			.containsExactlyInAnyOrderElementsOf(expectedStrassenNamen);

		// Beleuchtung stellvertretend fuer die Attribute
		List<Beleuchtung> expectedBeleuchtung = List.of(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG,
			Beleuchtung.VORHANDEN, Beleuchtung.NICHT_VORHANDEN, Beleuchtung.UNBEKANNT);
		assertThat(alleKanten).map(kante -> kante.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung())
			.containsExactlyElementsOf(expectedBeleuchtung);

		// Vereinbarungskennung stellvertretend fuer die linear referenzierten Attribute
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute2 = alleKanten.get(1).getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().stream().sorted(
				Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
					LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toUnmodifiableList());
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute3 = alleKanten.get(2).getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().stream().sorted(
				Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
					LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toUnmodifiableList());

		// Kante zwei ist laenger geworden und deshalb sollten die linear referenzierten Attribute
		// von 0 - 0.75 gleich bleiben. Von 0.75 bis 1.0 sollten sie on Kante drei uebernommen werden
		// Kante drei sollte seine linear referenzierten Attribute von 0 bis 1 behalten, obwohl sie kuerzer
		// geworden ist
		assertThat(zustaendigkeitAttribute2).hasSize(2);
		assertThat(zustaendigkeitAttribute3).hasSize(1);

		// Kante zwei 0 bis 0.33 "DEF"
		assertThat(zustaendigkeitAttribute2.get(0).getLinearReferenzierterAbschnitt().getVonValue())
			.isEqualTo(0.0, withPrecision(0.01));
		assertThat(zustaendigkeitAttribute2.get(0).getLinearReferenzierterAbschnitt().getBisValue())
			.isEqualTo(0.3333, withPrecision(0.01));
		Optional<VereinbarungsKennung> vK21 = zustaendigkeitAttribute2.get(0).getVereinbarungsKennung();
		assertThat(vK21.isPresent()).isTrue();
		assertThat(vK21.get().toString()).isEqualTo("DEF");

		// Kante zwei 0.33 bis 1.0 "GHI"
		assertThat(zustaendigkeitAttribute2.get(1).getLinearReferenzierterAbschnitt().getVonValue())
			.isEqualTo(0.3333, withPrecision(0.01));
		assertThat(zustaendigkeitAttribute2.get(1).getLinearReferenzierterAbschnitt().getBisValue())
			.isEqualTo(1.0, withPrecision(0.01));
		Optional<VereinbarungsKennung> vK22 = zustaendigkeitAttribute2.get(1).getVereinbarungsKennung();
		assertThat(vK22.isPresent()).isTrue();
		assertThat(vK22.get().toString()).isEqualTo("GHI");

		// Kante drei 0.0 bis 1.0 "GHI"
		assertThat(zustaendigkeitAttribute3.get(0).getLinearReferenzierterAbschnitt().getVonValue())
			.isEqualTo(0.0, withPrecision(0.01));
		assertThat(zustaendigkeitAttribute3.get(0).getLinearReferenzierterAbschnitt().getBisValue())
			.isEqualTo(1.0, withPrecision(0.01));
		Optional<VereinbarungsKennung> vK3 = zustaendigkeitAttribute3.get(0).getVereinbarungsKennung();
		assertThat(vK3.isPresent()).isTrue();
		assertThat(vK3.get().toString()).isEqualTo("GHI");

		assertThatRadNETZUnberuehrt();

		assertThat(applicationEvents.stream(KnotenDeletedEvent.class))
			.usingRecursiveFieldByFieldElementComparatorOnFields("knotenId", "ausloeser")
			.containsExactly(
				new KnotenDeletedEvent(basisNetzKnoten.get(2).getId(),
					basisNetzKnoten.get(2).getPoint(),
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testeDoRun_kanteWirdLaenger_vernetzeAdjazenteRadvisKanteNeu() {
		// ---------------------------- arrange ------------------------------
		Envelope bereich1 = new Envelope(0, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich1);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		Knoten knoten1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build());
		Knoten knoten2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 0), QuellSystem.DLM).build());
		Knoten knoten3 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.RadVis).build());

		String dlmId = "1";
		Kante dlmKante = netzService.saveKante(KanteTestDataProvider
			.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM).dlmId(DlmId.of(dlmId)).build());

		Kante radvisKante = netzService.saveKante(KanteTestDataProvider
			.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.RadVis).build());

		// Zu importierende features festlegen
		// Attribute der zu importierenden Features erstellen
		Map<String, Object> featureAttribute2 = new HashMap<>();
		featureAttribute2.put("eigenname", "neuerName2");
		featureAttribute2.put("bezeichnung", "42");

		// Kante 2 wird länger, kante 3 wird kürzer
		Coordinate newKnotenKoordinate = new Coordinate(15, 0);
		Stream<ImportedFeature> importedFeatureBereich1 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(knoten1.getKoordinate(), newKnotenKoordinate)
				.fachId(dlmId)
				.attribute(featureAttribute2)
				.build());

		when(dlmImportRepository.readStrassenFeatures(bereich1)).thenReturn(importedFeatureBereich1);

		applicationEvents.clear();
		entityManager.flush();
		entityManager.clear();

		// ------------------------------ act --------------------------------
		dlmReimportJob.run();
		entityManager.flush();
		entityManager.clear();

		// ---------------------------- assert -------------------------------

		Kante updatedDlmKante = kantenRepository.findById(dlmKante.getId()).get();
		Kante updatedRadvisKante = kantenRepository.findById(radvisKante.getId()).get();

		// sicherstellen, dass es kein false positive ist
		assertThat(updatedDlmKante.getNachKnoten().getKoordinate()).isEqualTo(newKnotenKoordinate);

		assertThat(updatedRadvisKante.getVonKnoten()).isEqualTo(updatedDlmKante.getNachKnoten());
		assertThat(updatedRadvisKante.getNachKnoten()).isEqualTo(radvisKante.getNachKnoten());

		assertThat(radvisKante.getGeometry()).isNotEqualTo(updatedRadvisKante.getGeometry());
		assertThat(updatedRadvisKante.getGeometry().getStartPoint().getCoordinate())
			.isEqualTo(newKnotenKoordinate);

		assertThat(applicationEvents.stream(KnotenDeletedEvent.class).collect(Collectors.toList()))
			.extracting("knotenId")
			.containsExactly(knoten2.getId());
	}

	@Test
	void testDoRun_neueKante_grundnetzStatusKorrektGesetzt() {
		// ---------------------------- arrange ------------------------------

		Envelope bereich = new Envelope(0, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// Repositories mit bestehende Knoten und Kanten fuellen
		erstelleVeraltetesBasisnetz();

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

		when(dlmImportRepository.readStrassenFeatures(bereich)).thenReturn(importedFeatures);

		applicationEvents.clear();

		// act
		dlmReimportJob.run();

		// assert
		assertThat(netzService.findKanteByQuelle(QuellSystem.DLM)
			.filter(kante -> kante.getDlmId().getValue().equals("4")))
			.extracting(Kante::isGrundnetz)
			.containsExactly(true);

		assertThat(applicationEvents.stream()).isEmpty();
	}

	@Test
	void testDoRun_KantenSplitInQualitaetsgesichertemLandkreis_grundnetzStatusKorrektGesetzt() {
		// ---------------------------- arrange ------------------------------

		gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("testOrganisation")
				.organisationsArt(OrganisationsArt.KREIS)
				.istQualitaetsgesichert(true)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 50, 50))
				.build());

		Envelope bereich = new Envelope(0, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// Repositories mit bestehende Knoten und Kanten fuellen
		erstelleVeraltetesBasisnetz();

		basisNetzKanten.forEach(kante -> kante.setGrundnetz(true));

		Kante kante1 = basisNetzKanten.stream().filter(kante -> kante.getDlmId().getValue().equals("1")).findFirst()
			.get();

		kante1.getKantenAttributGruppe().update(Set.of(Netzklasse.RADNETZ_ALLTAG),
			kante1.getKantenAttributGruppe().getIstStandards(), kante1.getKantenAttributGruppe().getKantenAttribute());

		LineString geometryKante1 = kante1.getGeometry();
		LineString geometryKante2 = basisNetzKanten.get(1).getGeometry();

		netzService.saveKanten(basisNetzKanten);

		entityManager.flush();
		entityManager.clear();

		// Kante 1 ist RadNETZ und wird gesplittet, Kante 2 ist nicht RadNETZ und wird gesplittet
		Stream<ImportedFeature> importedFeatures = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 0), new Coordinate(6, 0))
				.fachId("1")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(6, 0), new Coordinate(10, 0))
				.fachId("1_1")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(10, 0), new Coordinate(15, 0))
				.fachId("2_2")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(15, 0), new Coordinate(20, 0))
				.fachId("2")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(20, 0), new Coordinate(30, 0))
				.fachId("3")
				.build());

		when(dlmImportRepository.readStrassenFeatures(bereich)).thenReturn(importedFeatures);
		applicationEvents.clear();

		// act
		dlmReimportJob.run();

		// assert
		assertThat(findAllDLMAndRadVisKanten())
			.allMatch(Kante::isGrundnetz);

		assertThat(applicationEvents.stream(KanteTopologieChangedEvent.class))
			.usingRecursiveFieldByFieldElementComparatorOnFields("kanteId", "ausloeser")
			.containsExactlyInAnyOrder(
				new KanteTopologieChangedEvent(
					basisNetzKanten.get(0).getId(),
					geometryKante1,
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()),
				new KanteTopologieChangedEvent(
					basisNetzKanten.get(1).getId(),
					geometryKante2,
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()));
	}

	@Test
	void testDoRun_KantenSplitInNichtQualitaetsgesichertemLandkreis_grundnetzStatusKorrektGesetzt() {
		// ---------------------------- arrange ------------------------------

		gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("testOrganisation")
				.organisationsArt(OrganisationsArt.KREIS)
				.istQualitaetsgesichert(false)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 50, 50))
				.build());

		Envelope bereich = new Envelope(0, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// Repositories mit bestehende Knoten und Kanten fuellen
		erstelleVeraltetesBasisnetz();
		basisNetzKanten.forEach(kante -> kante.setGrundnetz(true));

		Kante kante1 = basisNetzKanten.stream().filter(kante -> kante.getDlmId().getValue().equals("1")).findFirst()
			.get();

		kante1.getKantenAttributGruppe().update(Set.of(Netzklasse.RADNETZ_ALLTAG),
			kante1.getKantenAttributGruppe().getIstStandards(), kante1.getKantenAttributGruppe().getKantenAttribute());

		kante1.setGrundnetz(false);

		LineString geometryKante1 = kante1.getGeometry();
		LineString geometryKante2 = basisNetzKanten.get(1).getGeometry();

		netzService.saveKanten(basisNetzKanten);

		entityManager.flush();
		entityManager.clear();

		// Kante 1 ist RadNETZ und wird gesplittet, Kante 2 ist nicht RadNETZ und wird gesplittet
		Stream<ImportedFeature> importedFeatures = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 0), new Coordinate(6, 0))
				.fachId("1")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(6, 0), new Coordinate(10, 0))
				.fachId("1_1")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(10, 0), new Coordinate(15, 0))
				.fachId("2_2")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(15, 0), new Coordinate(20, 0))
				.fachId("2")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(20, 0), new Coordinate(30, 0))
				.fachId("3")
				.build());

		when(dlmImportRepository.readStrassenFeatures(bereich)).thenReturn(importedFeatures);

		applicationEvents.clear();

		// act
		dlmReimportJob.run();

		// assert
		assertThat(netzService.findKanteByQuelle(QuellSystem.DLM))
			.allMatch(kante -> kante.getDlmId().getValue().startsWith("1") != kante.isGrundnetz());

		assertThatRadNETZUnberuehrt();
		assertThat(kantenMappingRepository.findAll()).hasSize(2);

		assertThat(applicationEvents.stream(KanteTopologieChangedEvent.class))
			.usingRecursiveFieldByFieldElementComparatorOnFields("kanteId", "ausloeser")
			.containsExactlyInAnyOrder(
				new KanteTopologieChangedEvent(
					basisNetzKanten.get(0).getId(),
					geometryKante1,
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()),
				new KanteTopologieChangedEvent(
					basisNetzKanten.get(1).getId(),
					geometryKante2,
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testeDoRun_loescheTatsaechlichKantenUndKnoten() { // test existiert um richtigen umgang mit hibernate
		// sicherzustellen
		Envelope bereich = new Envelope(0, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich);

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		Map<String, Object> featureAttribute1 = new HashMap<>();
		featureAttribute1.put("eigenname", "bestehendeKante1");
		featureAttribute1.put("bezeichnung", "16");
		when(dlmImportRepository.readStrassenFeatures(bereich))
			.thenReturn(Stream.of(ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 10), new Coordinate(10, 0))
				.fachId("1")
				.attribute(featureAttribute1)
				.build()));
		when(dlmImportRepository.readWegeFeatures(bereich)).thenReturn(Stream.empty());

		erstelleVeraltetesBasisnetz();

		applicationEvents.clear();

		// act
		dlmReimportJob.doRun();

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(findAllDLMAndRadVisKanten())
			.hasSize(2);
		assertThat(knotenRepository.getKnotenFuerKanteIds(
			findAllDLMAndRadVisKanten().stream().map(Kante::getId).collect(Collectors.toSet())))
			.hasSize(4);

		// Lösche verschobenen Knoten
		assertThat(knotenRepository.getKnotenInBereichFuerQuelle(new Envelope(-3, 3, -3, 3), QuellSystem.DLM))
			.isEmpty();
		assertThat(knotenRepository.findById(basisNetzKnoten.get(0).getId())).isEmpty();

		List<Knoten> verbliebeneKnoten = StreamSupport.stream(knotenRepository.findAll().spliterator(), false)
			.filter(
				knoten -> knoten.getQuelle().equals(QuellSystem.DLM) || knoten.getQuelle().equals(QuellSystem.RadVis))
			.collect(Collectors.toList());
		assertThat(verbliebeneKnoten)
			.hasSize(4)
			.extracting(Knoten::getKoordinate)
			.containsExactlyInAnyOrder(
				new Coordinate(10, 0),
				new Coordinate(30, 0),
				new Coordinate(40, 0),
				new Coordinate(0, 10));

		assertThatRadNETZUnberuehrt();

		assertThat(kantenMappingRepository.findAll()).hasSize(0);

		KnotenDeletedEvent[] knotenDeletedEvents = applicationEvents.stream(KnotenDeletedEvent.class)
			.sorted(Comparator.comparingLong(KnotenDeletedEvent::getKnotenId))
			.toArray(KnotenDeletedEvent[]::new);
		assertThat(knotenDeletedEvents[0].getKnotenId()).isEqualTo(basisNetzKnoten.get(0).getId());
		assertThat(knotenDeletedEvents[0].getGeometry()).isEqualTo(basisNetzKnoten.get(0).getPoint());
		assertThat(knotenDeletedEvents[0].getAusloeser()).isEqualTo(NetzAenderungAusloeser.DLM_REIMPORT_JOB);
		assertThat(knotenDeletedEvents[1].getKnotenId()).isEqualTo(basisNetzKnoten.get(2).getId());
		assertThat(knotenDeletedEvents[1].getGeometry()).isEqualTo(basisNetzKnoten.get(2).getPoint());
		assertThat(knotenDeletedEvents[1].getAusloeser()).isEqualTo(NetzAenderungAusloeser.DLM_REIMPORT_JOB);

		KanteDeletedEvent[] kanteDeletedEvents = applicationEvents.stream(KanteDeletedEvent.class)
			.toArray(KanteDeletedEvent[]::new);
		assertThat(kanteDeletedEvents[0].getKanteId()).isEqualTo(basisNetzKanten.get(1).getId());
		assertThat(kanteDeletedEvents[0].getGeometry()).isEqualTo(basisNetzKanten.get(1).getGeometry());
		assertThat(kanteDeletedEvents[0].getAusloeser()).isEqualTo(NetzAenderungAusloeser.DLM_REIMPORT_JOB);
		assertThat(kanteDeletedEvents[1].getKanteId()).isEqualTo(basisNetzKanten.get(2).getId());
		assertThat(kanteDeletedEvents[1].getGeometry()).isEqualTo(basisNetzKanten.get(2).getGeometry());
		assertThat(kanteDeletedEvents[1].getAusloeser()).isEqualTo(NetzAenderungAusloeser.DLM_REIMPORT_JOB);
	}

	@Test
	public void testeDeleteKantenAusNetzUndEntferneVerwaisteKnoten() {
		Envelope bereich = new Envelope(50, 200, 50, 200);
		List<Envelope> partitions = List.of(bereich);
		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// arrange
		Knoten k1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 110), QuellSystem.DLM).build());
		Knoten k2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 110), QuellSystem.DLM).build());
		Knoten k3 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(140, 140), QuellSystem.DLM).build());

		Knoten k4 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 110), QuellSystem.DLM).build());
		Knoten k5 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(80, 110), QuellSystem.DLM).build());

		Knoten verwaistDurchTopologieUpdate = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 120), QuellSystem.DLM).build());

		Knoten vonVorneReinverwaist = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 120), QuellSystem.DLM).build());

		Knoten radNETZ1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 110), QuellSystem.RadNETZ).build());
		Knoten radNETZ2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 110), QuellSystem.RadNETZ).build());
		Knoten radNETZ3Verwaist = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(150, 150), QuellSystem.RadNETZ).build());

		Kante kante1 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(k1, k2).quelle(QuellSystem.DLM).dlmId(DlmId.of("1")).build());
		Kante kante2 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(verwaistDurchTopologieUpdate, k1).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("2")).build());
		Kante kante3ToDelete = netzService
			.saveKante(KanteTestDataProvider.fromKnoten(k1, k3).quelle(QuellSystem.DLM).dlmId(DlmId.of("3")).build());
		Kante kante4 = netzService
			.saveKante(KanteTestDataProvider.withCoordinatesAndQuelle(99, 110, 80, 110, QuellSystem.DLM)
				.dlmId(DlmId.of("4"))
				.vonKnoten(k4)
				.nachKnoten(k5).build());
		Kante radNETZKante = netzService
			.saveKante(
				KanteTestDataProvider.fromKnoten(radNETZ1, radNETZ2).quelle(QuellSystem.RadNETZ).dlmId(null).build());

		// Kanten 1 und 2 werden reimportiert
		Stream<ImportedFeature> importedFeatures = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(110, 110), new Coordinate(120, 110))
				.fachId("1")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(110, 110), new Coordinate(120, 110))
				.fachId("2")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(99, 110), new Coordinate(80, 110))
				.fachId("4")
				.build());
		when(dlmImportRepository.readStrassenFeatures(any())).thenReturn(importedFeatures);
		when(dlmImportRepository.readWegeFeatures(any())).thenReturn(Stream.empty());

		assertThat(knotenRepository.findAll())
			.containsExactlyInAnyOrder(k1, k2, verwaistDurchTopologieUpdate, k3, vonVorneReinverwaist, k4,
				k5, radNETZ1, radNETZ2, radNETZ3Verwaist);
		assertThat(kantenRepository.findAll())
			.containsExactlyInAnyOrder(kante1, kante2, kante3ToDelete, kante4, radNETZKante);

		applicationEvents.clear();

		// act
		dlmReimportJob.doRun();

		// assert
		assertThat(knotenRepository.findAll())
			.containsExactlyInAnyOrder(k1, k2, k4, k5, radNETZ1, radNETZ2, radNETZ3Verwaist);
		assertThat(kantenRepository.findAll())
			.containsExactlyInAnyOrder(kante1, kante2, kante4, radNETZKante);

		assertThat(applicationEvents.stream(KnotenDeletedEvent.class))
			.usingRecursiveFieldByFieldElementComparatorOnFields("knotenId", "ausloeser")
			.containsExactlyInAnyOrder(
				new KnotenDeletedEvent(
					verwaistDurchTopologieUpdate.getId(),
					verwaistDurchTopologieUpdate.getPoint(),
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()),
				new KnotenDeletedEvent(
					vonVorneReinverwaist.getId(),
					vonVorneReinverwaist.getPoint(),
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()),
				new KnotenDeletedEvent(
					k3.getId(),
					k3.getPoint(),
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()));

		assertThat(applicationEvents.stream(KanteDeletedEvent.class))
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withIgnoredFields("datum").build())
			.containsExactlyInAnyOrder(
				new KanteDeletedEvent(
					kante3ToDelete.getId(),
					kante3ToDelete.getGeometry(),
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()));
	}

	@Test
	public void deleteKante_vonMassnahmeReferenziert() {
		Envelope bereich = new Envelope(50, 200, 50, 200);
		List<Envelope> partitions = List.of(bereich);
		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// arrange
		Knoten k1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 110), QuellSystem.DLM).build());
		Knoten k2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 110), QuellSystem.DLM).build());

		Kante kante1 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(k1, k2).quelle(QuellSystem.DLM).dlmId(DlmId.of("1")).build());

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("testOrganisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build());
		Benutzer benutzerLetzteAenderung = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());

		Massnahme kantenMassnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValues()
				.baulastZustaendiger(null)
				.zustaendiger(gebietskoerperschaft)
				.unterhaltsZustaendiger(null)
				.benutzerLetzteAenderung(benutzerLetzteAenderung)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante1)).build());

		Massnahme kantenPunktMassnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValues()
				.baulastZustaendiger(null)
				.zustaendiger(gebietskoerperschaft)
				.unterhaltsZustaendiger(null)
				.benutzerLetzteAenderung(benutzerLetzteAenderung)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante1)).build());

		when(dlmImportRepository.readStrassenFeatures(any())).thenReturn(Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(30, 30), new Coordinate(10, 10))
				.fachId("2")
				.build()));
		when(dlmImportRepository.readWegeFeatures(any())).thenReturn(Stream.empty());

		assertThat(knotenRepository.findAll())
			.containsExactlyInAnyOrder(k1, k2);
		assertThat(kantenRepository.findAll())
			.containsExactlyInAnyOrder(kante1);

		// act
		dlmReimportJob.doRun();

		// assert
		assertThat(kantenRepository.findAll()).doesNotContain(kante1);
		assertThat(knotenRepository.findAll()).doesNotContain(k1, k2);

		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderungs = (List<MassnahmeNetzBezugAenderung>) massnahmeNetzBezugAenderungRepository
			.findAll();
		assertThat(massnahmeNetzBezugAenderungs).hasSize(2);
		assertThat(massnahmeNetzBezugAenderungs).extracting(MassnahmeNetzBezugAenderung::getNetzBezugAenderungsArt)
			.containsExactlyInAnyOrder(
				NetzBezugAenderungsArt.KANTE_GELOESCHT, NetzBezugAenderungsArt.KANTE_GELOESCHT);
		assertThat(massnahmeNetzBezugAenderungs).extracting(MassnahmeNetzBezugAenderung::getNetzEntityId)
			.containsExactlyInAnyOrder(kante1.getId(), kante1.getId());
		assertThat(massnahmeNetzBezugAenderungs).extracting(MassnahmeNetzBezugAenderung::getMassnahme)
			.extracting(AbstractEntity::getId).containsExactlyInAnyOrder(
				kantenMassnahme.getId(), kantenPunktMassnahme.getId());
	}

	@Test
	public void deleteKante_vonManuellerImportFehlerReferenziert() {
		Envelope bereich = new Envelope(50, 200, 50, 200);
		List<Envelope> partitions = List.of(bereich);
		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// arrange
		Knoten k1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 110), QuellSystem.DLM).build());
		Knoten k2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 110), QuellSystem.DLM).build());

		Kante kante1 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(k1, k2).quelle(QuellSystem.DLM).dlmId(DlmId.of("1")).build());

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("testOrganisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build());
		Benutzer benutzer = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());

		ManuellerImportFehler manuellerImportFehler = manuellerImportFehlerRepository.save(
			new ManuellerImportFehler(kante1, LocalDateTime.now(), benutzer, gebietskoerperschaft, new HashSet<>()));

		when(dlmImportRepository.readStrassenFeatures(any())).thenReturn(Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(30, 30), new Coordinate(10, 10))
				.fachId("2")
				.build()));
		when(dlmImportRepository.readWegeFeatures(any())).thenReturn(Stream.empty());

		assertThat(knotenRepository.findAll())
			.containsExactlyInAnyOrder(k1, k2);
		assertThat(kantenRepository.findAll())
			.containsExactlyInAnyOrder(kante1);

		// act
		dlmReimportJob.doRun();

		// assert
		assertThat(kantenRepository.findAll()).doesNotContain(kante1);
		assertThat(knotenRepository.findAll()).doesNotContain(k1, k2);

		assertThat(manuellerImportFehlerRepository.findAllByKanteId(kante1.getId())).isEmpty();
		ManuellerImportFehler manuellerImportFehlerNachReImport = manuellerImportFehlerRepository.findById(
			manuellerImportFehler.getId()).get();
		assertThat(manuellerImportFehlerNachReImport.getKante()).isEmpty();
		assertThat(manuellerImportFehlerNachReImport.getOriginalGeometrie()).contains(kante1.getGeometry());
		assertThat(manuellerImportFehlerNachReImport.getBeschreibung()).startsWith(
			"HINWEIS: Die betroffene Kante mit Id " + kante1.getId() + " existiert nicht mehr!");
	}

	@Test
	public void deleteKante_vonGeloeschterMassnahmeReferenziert() {
		Envelope bereich = new Envelope(50, 200, 50, 200);
		List<Envelope> partitions = List.of(bereich);
		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// arrange
		Knoten k1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 110), QuellSystem.DLM).build());
		Knoten k2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 110), QuellSystem.DLM).build());

		Kante kante1 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(k1, k2).quelle(QuellSystem.DLM).dlmId(DlmId.of("1")).build());

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("testOrganisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build());
		Benutzer benutzerLetzteAenderung = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());

		Massnahme kantenMassnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValues()
				.baulastZustaendiger(null)
				.unterhaltsZustaendiger(null)
				.zustaendiger(gebietskoerperschaft)
				.benutzerLetzteAenderung(benutzerLetzteAenderung)
				.geloescht(true)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante1)).build());

		Massnahme kantenPunktMassnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValues()
				.baulastZustaendiger(null)
				.zustaendiger(gebietskoerperschaft)
				.unterhaltsZustaendiger(null)
				.benutzerLetzteAenderung(benutzerLetzteAenderung)
				.geloescht(true)
				.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante1)).build());

		when(dlmImportRepository.readStrassenFeatures(any())).thenReturn(Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(30, 30), new Coordinate(10, 10))
				.fachId("2")
				.build()));
		when(dlmImportRepository.readWegeFeatures(any())).thenReturn(Stream.empty());

		assertThat(knotenRepository.findAll())
			.containsExactlyInAnyOrder(k1, k2);
		assertThat(kantenRepository.findAll())
			.containsExactlyInAnyOrder(kante1);

		// act
		dlmReimportJob.doRun();

		// assert
		assertThat(kantenRepository.findAll()).doesNotContain(kante1);
		assertThat(knotenRepository.findAll()).doesNotContain(k1, k2);

		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderungs = (List<MassnahmeNetzBezugAenderung>) massnahmeNetzBezugAenderungRepository
			.findAll();
		assertThat(massnahmeNetzBezugAenderungs).hasSize(0);
		assertThat(massnahmeRepository.findById(kantenMassnahme.getId()).get().getNetzbezug()
			.getImmutableKantenAbschnittBezug())
			.extracting(AbschnittsweiserKantenBezug::getKante)
			.doesNotContain(kante1);
		assertThat(massnahmeRepository.findById(kantenPunktMassnahme.getId()).get().getNetzbezug()
			.getImmutableKantenPunktBezug())
			.extracting(PunktuellerKantenSeitenBezug::getKante)
			.doesNotContain(kante1);
	}

	@Test
	public void deleteKnoten_vonMassnahmeReferenziert() {
		Envelope bereich = new Envelope(50, 200, 50, 200);
		List<Envelope> partitions = List.of(bereich);
		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// arrange
		Knoten k1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 110), QuellSystem.DLM).build());
		Knoten k2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 110), QuellSystem.DLM).build());

		Kante kante1 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(k1, k2).quelle(QuellSystem.DLM).dlmId(DlmId.of("1")).build());

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("testOrganisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build());
		Benutzer benutzerLetzteAenderung = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());

		Massnahme knotenMassnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValues()
				.baulastZustaendiger(null)
				.zustaendiger(gebietskoerperschaft)
				.unterhaltsZustaendiger(null)
				.benutzerLetzteAenderung(benutzerLetzteAenderung)
				.netzbezug(NetzBezugTestDataProvider.forKnoten(k1)).build());

		when(dlmImportRepository.readStrassenFeatures(any())).thenReturn(Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(30, 30), new Coordinate(10, 10))
				.fachId("2")
				.build()));
		when(dlmImportRepository.readWegeFeatures(any())).thenReturn(Stream.empty());

		assertThat(knotenRepository.findAll())
			.containsExactlyInAnyOrder(k1, k2);
		assertThat(kantenRepository.findAll())
			.containsExactlyInAnyOrder(kante1);

		// act
		dlmReimportJob.doRun();

		// assert
		assertThat(kantenRepository.findAll()).doesNotContain(kante1);
		assertThat(knotenRepository.findAll()).doesNotContain(k1, k2);

		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderungs = (List<MassnahmeNetzBezugAenderung>) massnahmeNetzBezugAenderungRepository
			.findAll();
		assertThat(massnahmeNetzBezugAenderungs).hasSize(1);
		assertThat(massnahmeNetzBezugAenderungs.get(0).getNetzBezugAenderungsArt()).isEqualTo(
			NetzBezugAenderungsArt.KNOTEN_GELOESCHT);
		assertThat(massnahmeNetzBezugAenderungs.get(0).getNetzEntityId()).isEqualTo(k1.getId());
		assertThat(massnahmeNetzBezugAenderungs.get(0).getMassnahme().getId()).isEqualTo(knotenMassnahme.getId());
	}

	@Test
	public void deleteKnoten_vonGeloeschterMassnahmeReferenziert() {
		Envelope bereich = new Envelope(50, 200, 50, 200);
		List<Envelope> partitions = List.of(bereich);
		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// arrange
		Knoten k1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 110), QuellSystem.DLM).build());
		Knoten k2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 110), QuellSystem.DLM).build());

		Kante kante1 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(k1, k2).quelle(QuellSystem.DLM).dlmId(DlmId.of("1")).build());

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("testOrganisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build());
		Benutzer benutzerLetzteAenderung = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());

		Massnahme knotenMassnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValues()
				.baulastZustaendiger(null)
				.zustaendiger(gebietskoerperschaft)
				.unterhaltsZustaendiger(null)
				.benutzerLetzteAenderung(benutzerLetzteAenderung)
				.geloescht(true)
				.netzbezug(NetzBezugTestDataProvider.forKnoten(k1)).build());

		when(dlmImportRepository.readStrassenFeatures(any())).thenReturn(Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(30, 30), new Coordinate(10, 10))
				.fachId("2")
				.build()));
		when(dlmImportRepository.readWegeFeatures(any())).thenReturn(Stream.empty());

		assertThat(knotenRepository.findAll())
			.containsExactlyInAnyOrder(k1, k2);
		assertThat(kantenRepository.findAll())
			.containsExactlyInAnyOrder(kante1);

		// act
		dlmReimportJob.doRun();

		// assert
		assertThat(kantenRepository.findAll()).doesNotContain(kante1);
		assertThat(knotenRepository.findAll()).doesNotContain(k1, k2);

		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderungs = (List<MassnahmeNetzBezugAenderung>) massnahmeNetzBezugAenderungRepository
			.findAll();
		assertThat(massnahmeNetzBezugAenderungs).hasSize(0);
		assertThat(massnahmeRepository.findById(knotenMassnahme.getId()).get().getNetzbezug()
			.getImmutableKnotenBezug())
			.doesNotContain(k1);
	}

	@Test
	public void verschiebeKante_vonMassnahmeReferenziert() {
		Envelope bereich = new Envelope(50, 200, 50, 200);
		List<Envelope> partitions = List.of(bereich);
		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		// arrange
		Knoten k1 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(110, 110), QuellSystem.DLM).build());
		Knoten k2 = netzService.saveKnoten(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 110), QuellSystem.DLM).build());

		Kante kante1 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(k1, k2).quelle(QuellSystem.DLM).dlmId(DlmId.of("1")).build());

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("testOrganisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build());
		Benutzer benutzerLetzteAenderung = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());

		Massnahme kantenMassnahme = massnahmeRepository.save(
			MassnahmeTestDataProvider.withDefaultValues()
				.baulastZustaendiger(null)
				.zustaendiger(gebietskoerperschaft)
				.unterhaltsZustaendiger(null)
				.benutzerLetzteAenderung(benutzerLetzteAenderung)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante1)).build());

		when(dlmImportRepository.readStrassenFeatures(any())).thenReturn(Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(30, 30), new Coordinate(10, 10))
				.fachId("1")
				.build()));
		when(dlmImportRepository.readWegeFeatures(any())).thenReturn(Stream.empty());

		assertThat(knotenRepository.findAll())
			.containsExactlyInAnyOrder(k1, k2);
		assertThat(kantenRepository.findAll())
			.containsExactlyInAnyOrder(kante1);

		// act
		dlmReimportJob.doRun();

		// assert
		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderungs = (List<MassnahmeNetzBezugAenderung>) massnahmeNetzBezugAenderungRepository
			.findAll();
		assertThat(massnahmeNetzBezugAenderungs).hasSize(1);
		assertThat(massnahmeNetzBezugAenderungs.get(0).getNetzBezugAenderungsArt()).isEqualTo(
			NetzBezugAenderungsArt.KANTE_VERAENDERT);
		assertThat(massnahmeNetzBezugAenderungs.get(0).getNetzEntityId()).isEqualTo(kante1.getId());
		assertThat(massnahmeNetzBezugAenderungs.get(0).getMassnahme().getId()).isEqualTo(kantenMassnahme.getId());
	}

	private void assertThatRadNETZUnberuehrt() {
		assertThat(knotenRepository.findKnotenByQuelle(QuellSystem.RadNETZ)).hasSize(2);
		assertThat(netzService.findKanteByQuelle(QuellSystem.RadNETZ)).hasSize(1);
	}

	private List<Kante> findAllDLMAndRadVisKanten() {
		List<Kante> alleKanten = kantenRepository.findKanteByQuelle(QuellSystem.DLM).collect(Collectors.toList());
		alleKanten.addAll(kantenRepository.findKanteByQuelle(QuellSystem.RadVis).collect(Collectors.toList()));
		return alleKanten;
	}
}
