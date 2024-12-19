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

package de.wps.radvis.backend.netz.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.wps.radvis.backend.application.JacksonConfiguration;
import de.wps.radvis.backend.auditing.AuditingConfiguration;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.entity.RevInfo;
import de.wps.radvis.backend.auditing.schnittstelle.WithAuditingAspect;
import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeMergeService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingService;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.ZuordnungController;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.command.ChangeZuordnungCommand;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.command.LoescheZuordnungenCommand;
import de.wps.radvis.backend.integration.dlm.domain.DLMNetzbildungJob;
import de.wps.radvis.backend.integration.dlm.domain.DLMNetzbildungProtokollService;
import de.wps.radvis.backend.integration.dlm.domain.DLMNetzbildungService;
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.integration.radwegedb.IntegrationRadwegeDBConfiguration;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.schnittstelle.command.ChangeSeitenbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.CreateKanteCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveGeschwindigkeitAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveGeschwindigkeitAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteFahrtrichtungCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteVerlaufCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKnotenCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveZustaendigkeitAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveZustaendigkeitAttributeCommand;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.quellimport.common.ImportsCommonConfiguration;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;

@Tag("group7")
@ContextConfiguration(classes = {
	NetzControllerAuditingTestIT.TestConfig.class,
	AuditingConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	WithAuditingAspect.class,
	JacksonConfiguration.class,
	NetzfehlerConfiguration.class,
	KommentarConfiguration.class,
	ImportsCommonConfiguration.class,
	CommonConfiguration.class,
	ImportsGrundnetzConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	IntegrationRadNetzConfiguration.class,
	IntegrationRadwegeDBConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	DLMConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
	@MockBean(FahrradrouteRepository.class)
})
public class NetzControllerAuditingTestIT extends AuditingTestIT {

	// Der NetzController wird dort, wo es um Berechtigungen geht mit Mocks bestückt,
	// um die enstprechenden Prüfungen auszuhebeln
	static class TestConfig {

		@Autowired
		NetzService netzService;

		@Autowired
		SaveKanteCommandConverter saveKanteCommandConverter;

		@Autowired
		NetzToFeatureDetailsConverter netzToFeatureDetailsConverter;

		@MockBean
		BenutzerResolver benutzerResolver;

		@MockBean
		NetzGuard netzAutorisierungsService;

		@Autowired
		KantenAttributeMergeService kantenAttributeMergeService;

		@Autowired
		KantenMappingService kantenMappingService;

		@MockBean
		ZustaendigkeitsService zustaendigkeitsService;

		@Autowired
		private ImportedFeaturePersistentRepository importedFeatureRepository;

		@Autowired
		private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

		@Autowired
		private EntityManager entityManager;

		@Autowired
		private NetzfehlerRepository netzfehlerRepository;

		@Autowired
		private NetzConfigurationProperties netzConfigurationProperties;

		@Bean
		public NetzController netzController() {
			Mockito.when(benutzerResolver.fromAuthentication(Mockito.any()))
				.thenReturn(
					BenutzerTestDataProvider.admin(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
						.build());

			return new NetzController(netzService, netzAutorisierungsService, benutzerResolver, zustaendigkeitsService,
				saveKanteCommandConverter, netzToFeatureDetailsConverter, netzConfigurationProperties);
		}

		@Bean
		public ZuordnungController zuordnungController() {
			return new ZuordnungController(netzService, kantenAttributeMergeService, kantenMappingService);
		}

		@Bean
		public DLMNetzbildungJob dlmNetzbildungJob() {
			return new DLMNetzbildungJob(importedFeatureRepository, dlmNetzbildungService(),
				jobExecutionDescriptionRepository);
		}

		public DLMNetzbildungService dlmNetzbildungService() {
			return new DLMNetzbildungService(dlmNetzbildungProtokollService(), netzService, entityManager);
		}

		@Bean
		public DLMNetzbildungProtokollService dlmNetzbildungProtokollService() {
			return new DLMNetzbildungProtokollService(netzfehlerRepository);
		}

	}

	static final GeometryFactory GEOMETRY_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	private final Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
		.name("Kreis")
		.organisationsArt(OrganisationsArt.KREIS)
		.fachId(213)
		.bereich(GEOMETRY_FACTORY.createMultiPolygon(
			new Polygon[] {
				GEOMETRY_FACTORY.createPolygon(
					new Coordinate[] {
						new Coordinate(0, 0),
						new Coordinate(0, 1),
						new Coordinate(1, 1),
						new Coordinate(1, 0),
						new Coordinate(0, 0),
					})
			}))
		.build();

	static ObjectWriter objectWriter;

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	KnotenRepository knotenRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	BenutzerRepository benutzerRepository;

	@Autowired
	private TestEntityManager testEntityManager;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	NetzController netzController;

	@Autowired
	ZuordnungController zuordnungController;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DLMNetzbildungJob dlmNetzbildungJob;

	@Autowired
	private ImportedFeaturePersistentRepository importedFeaturePersistentRepository;

	@BeforeAll
	static void setupObjectWriter() {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("CustomGeometrySerializer", new Version(1, 0, 0, null, null, null));
		module.addSerializer(Geometry.class, new GeometryAsGeoJSONSerializer());
		mapper.registerModule(module);
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		objectWriter = mapper.writer().withDefaultPrettyPrinter();
	}

	@BeforeEach
	void setUp() {
		gebietskoerperschaftRepository.save((Gebietskoerperschaft) organisation);
	}

	@Test
	void testAuditing_speichertRevInfo() {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		Long kanteId = kantenRepository.save(kante).getId();

		// act

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CHANGE_SEITENBEZUG_COMMAND);

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			Kante finalKante = kantenRepository.findById(kanteId).orElseThrow();
			finalKante.changeSeitenbezug(true);
			return null;
		});

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.SAVE_KANTE_FAHRTRICHTUNG_COMMAND);

		template.execute(status -> {
			Kante finalKante = kantenRepository.findById(kanteId).orElseThrow();
			finalKante.getFahrtrichtungAttributGruppe().update(Richtung.GEGEN_RICHTUNG, Richtung.BEIDE_RICHTUNGEN);
			return null;
		});

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.SAVE_KANTE_ATTRIBUTE_COMMAND);

		kante.getKantenAttributGruppe().getNetzklassen().add(Netzklasse.RADNETZ_ALLTAG);
		kante.getKantenAttributGruppe().getIstStandards().add(IstStandard.BASISSTANDARD);

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.SAVE_FUEHRUNGSFORM_ATTRIBUT_GRUPPE_COMMAND);
		template.execute(status -> {
			Kante finalKante = kantenRepository.findById(kanteId).orElseThrow();
			finalKante.getFuehrungsformAttributGruppe().replaceFuehrungsformAttribute(
				List.of(
					FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.belagArt(BelagArt.UNGEBUNDENE_DECKE)
						.build()),
				List.of(
					FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.belagArt(BelagArt.ASPHALT)
						.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
						.build(),
					FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
						.belagArt(BelagArt.BETON)
						.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
						.build()));
			return null;
		});

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.SAVE_GESCHWINDIGKEIT_ATTRIBUT_GRUPPE_COMMAND);

		template.execute(status -> {
			Kante finalKante = kantenRepository.findById(kanteId).orElseThrow();
			finalKante.getGeschwindigkeitAttributGruppe().replaceGeschwindigkeitAttribute(
				List.of(GeschwindigkeitAttribute.builder()
					.ortslage(KantenOrtslage.INNERORTS)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
					.build()));
			return null;
		});

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.SAVE_ZUSTAENDIGKEIT_ATTRIBUT_GRUPPE_COMMAND);

		template.execute(status -> {
			Kante finalKante = kantenRepository.findById(kanteId).orElseThrow();
			finalKante.getZustaendigkeitAttributGruppe().replaceZustaendigkeitAttribute(
				List.of(
					ZustaendigkeitAttribute.builder()
						.erhaltsZustaendiger(organisation)
						.build()));
			return null;
		});

		AdditionalRevInfoHolder.clear();

		// assert

		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());

			@SuppressWarnings("unchecked")
			List<Object[]> resultKante = auditReader.createQuery()
				.forRevisionsOfEntity(Kante.class, false, true)
				.getResultList();

			@SuppressWarnings("unchecked")
			List<Object[]> resultFuehrungsformAttributGruppe = auditReader.createQuery()
				.forRevisionsOfEntity(FuehrungsformAttributGruppe.class, false, true)
				.getResultList();

			assertThat(resultKante).hasSize(2);
			assertThat(resultKante).extracting(objArray -> objArray[0])
				.containsExactly(KanteTestDataProvider.withDefaultValues().id(kante.getId()).build(), kante);
			assertThat(resultKante).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);

			assertThat(resultFuehrungsformAttributGruppe).hasSize(3);
			assertThat(resultFuehrungsformAttributGruppe).extracting(objArray -> objArray[0])
				.containsExactly(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.id(kante.getFuehrungsformAttributGruppe().getId())
						.build(),
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.id(kante.getFuehrungsformAttributGruppe().getId())
						.isZweiseitig(true)
						.build(),
					kante.getFuehrungsformAttributGruppe());
			assertThat(resultFuehrungsformAttributGruppe).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD, RevisionType.MOD);
			return null;
		});

	}

	@Test
	void testAuditing_keinAuditingContext_wirftException() {
		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> kantenRepository.save(kante));
	}

	@Test
	void testAuditing_mitBenutzer() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Benutzer benutzer = BenutzerTestDataProvider.admin(organisation).build();

		benutzerRepository.save(benutzer);

		UserDetails userDetails = new RadVisUserDetails(benutzer, Collections.emptyList());
		Authentication authentication = new RadVisAuthentication(userDetails);
		SecurityContext securityContext = Mockito.mock(SecurityContext.class);
		Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		// act
		kantenRepository.save(kante);

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKante = auditReader.createQuery()
				.forRevisionsOfEntity(Kante.class, false, true)
				.getResultList();

			assertThat(resultKante).extracting(objArray -> (RevInfo) objArray[1]).extracting(RevInfo::getBenutzer)
				.containsExactly(benutzer);
			return null;
		});

	}

	@Test
	void testNetzControllerAuditing_saveKanteAttributeCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		kantenRepository.save(kante);
		AdditionalRevInfoHolder.clear();

		// act
		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder().kanteId(kante.getId())
			.gruppenId(kante.getKantenAttributGruppe().getId())
			.gruppenVersion(kante.getKantenAttributGruppe().getVersion()).netzklassen(Set.of()).istStandards(Set.of())
			.gemeinde(null).beleuchtung(Beleuchtung.VORHANDEN).umfeld(Umfeld.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT).status(Status.UNTER_VERKEHR).build();

		String requestJson = objectWriter.writeValueAsString(List.of(command));

		mockMvc.perform(post("/api/netz/kanten/saveKanteAllgemein")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKantenAttributGruppe = auditReader.createQuery()
				.forRevisionsOfEntity(KantenAttributGruppe.class, false, true)
				.getResultList();

			assertThat(resultKantenAttributGruppe).hasSize(2);
			assertThat(resultKantenAttributGruppe).extracting(objArray -> objArray[0])
				.containsExactly(
					kante.getKantenAttributGruppe(),
					KantenAttributGruppeTestDataProvider.defaultValue()
						.id(kante.getKantenAttributGruppe().getId())
						.kantenAttribute(KantenAttribute.builder()
							.beleuchtung(Beleuchtung.VORHANDEN)
							.build())
						.build());
			assertThat(resultKantenAttributGruppe).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND, AuditingContext.SAVE_KANTE_ATTRIBUTE_COMMAND);
			assertThat(resultKantenAttributGruppe).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});

	}

	@Test
	void testNetzControllerAuditing_changeSeitenbezugCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		kantenRepository.save(kante);

		AdditionalRevInfoHolder.clear();

		// act

		ChangeSeitenbezugCommand command = ChangeSeitenbezugCommand.builder().build();

		ReflectionTestUtils.setField(command, "id", kante.getId());
		ReflectionTestUtils.setField(command, "version", kante.getVersion());

		ReflectionTestUtils.setField(command, "zweiseitig", true);

		String requestJson = objectWriter.writeValueAsString(List.of(command));

		mockMvc.perform(post("/api/netz/kanten/changeSeitenbezug")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKanten = auditReader.createQuery()
				.forRevisionsOfEntity(Kante.class, false, true)
				.getResultList();

			assertThat(resultKanten).hasSize(2);
			assertThat(resultKanten).extracting(objArray -> objArray[0])
				.containsExactly(
					kante,
					KanteTestDataProvider.withDefaultValuesAndZweiseitig().id(kante.getId()).build());
			assertThat(resultKanten).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND, AuditingContext.CHANGE_SEITENBEZUG_COMMAND);
			assertThat(resultKanten).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testNetzControllerAuditing_saveGeschwindigkeitAttributGruppeCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		kantenRepository.save(kante);

		AdditionalRevInfoHolder.clear();

		// act
		SaveGeschwindigkeitAttributGruppeCommand command = SaveGeschwindigkeitAttributGruppeCommand.builder()
			.kanteId(kante.getId())
			.gruppenID(kante.getGeschwindigkeitAttributGruppe().getId())
			.gruppenVersion(kante.getGeschwindigkeitAttributGruppe().getVersion())
			.geschwindigkeitAttribute(List.of(
				SaveGeschwindigkeitAttributeCommand.builder()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
					.build()))
			.build();

		String requestJson = objectWriter.writeValueAsString(List.of(command));

		mockMvc.perform(post("/api/netz/kanten/saveGeschwindigkeitAttributGruppe")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultGeschwindigkeitAttributGruppe = auditReader
				.createQuery()
				.forRevisionsOfEntity(GeschwindigkeitAttributGruppe.class, false, true)
				.getResultList();

			assertThat(resultGeschwindigkeitAttributGruppe).hasSize(2);
			assertThat(resultGeschwindigkeitAttributGruppe).extracting(objArray -> objArray[0])
				.containsExactly(
					kante.getGeschwindigkeitAttributGruppe(),
					GeschwindigkeitAttributGruppe.builder()
						.id(kante.getGeschwindigkeitAttributGruppe().getId())
						.geschwindigkeitAttribute(
							List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build()))
						.build());
			assertThat(resultGeschwindigkeitAttributGruppe).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND,
					AuditingContext.SAVE_GESCHWINDIGKEIT_ATTRIBUT_GRUPPE_COMMAND);
			assertThat(resultGeschwindigkeitAttributGruppe).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testNetzControllerAuditing_saveKanteVerlaufCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).build();

		kantenRepository.save(kante);

		AdditionalRevInfoHolder.clear();

		// act

		SaveKanteVerlaufCommand command = SaveKanteVerlaufCommand.builder()
			.id(kante.getId())
			.kantenVersion(kante.getVersion())
			.geometry(
				GEOMETRY_FACTORY.createLineString(new Coordinate[] { new Coordinate(1, 0), new Coordinate(100, 100) }))
			.build();

		String requestJson = objectWriter.writeValueAsString(List.of(command));

		mockMvc.perform(post("/api/netz/kanten/saveVerlauf")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKante = auditReader.createQuery()
				.forRevisionsOfEntity(Kante.class, false, true)
				.getResultList();

			assertThat(resultKante).hasSize(2);
			assertThat(resultKante).extracting(objArray -> objArray[0])
				.containsExactly(
					kante,
					KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis)
						.id(kante.getId())
						.geometry(GEOMETRY_FACTORY.createLineString(
							new Coordinate[] { new Coordinate(1, 0), new Coordinate(100, 100) }))
						.build());
			assertThat(resultKante).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND,
					AuditingContext.SAVE_KANTE_VERLAUF_COMMAND);
			assertThat(resultKante).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testNetzControllerAuditing_saveFuehrungsformAttributGruppeCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		kantenRepository.save(kante);

		AdditionalRevInfoHolder.clear();

		// act

		SaveFuehrungsformAttributeCommand fuehrungsformAttributeCommand = SaveFuehrungsformAttributeCommand
			.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.parkenForm(KfzParkenForm.UNBEKANNT)
			.parkenTyp(KfzParkenTyp.UNBEKANNT)
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
			.bordstein(Bordstein.UNBEKANNT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
			.belagArt(BelagArt.BETON)
			.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
			.trennstreifenBreiteRechts(Laenge.of(123))
			.trennstreifenBreiteLinks(Laenge.of(234))
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.build();

		SaveFuehrungsformAttributGruppeCommand command = SaveFuehrungsformAttributGruppeCommand.builder()
			.kanteId(kante.getId()).gruppenID(kante.getFuehrungsformAttributGruppe().getId())
			.gruppenVersion(kante.getFuehrungsformAttributGruppe().getVersion())
			.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeCommand))
			.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeCommand)).build();

		String requestJson = objectWriter.writeValueAsString(List.of(command));

		mockMvc.perform(post("/api/netz/kanten/saveFuehrungsformAttributGruppe")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultFuehrungsformAttributGruppe = auditReader.createQuery()
				.forRevisionsOfEntity(FuehrungsformAttributGruppe.class, false, true)
				.getResultList();

			assertThat(resultFuehrungsformAttributGruppe).hasSize(2);
			assertThat(resultFuehrungsformAttributGruppe).extracting(objArray -> objArray[0])
				.containsExactly(
					kante.getFuehrungsformAttributGruppe(),
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.id(kante.getFuehrungsformAttributGruppe().getId())
						.fuehrungsformAttributeLinks(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.BETON)
								.build()))
						.fuehrungsformAttributeRechts(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.BETON)
								.build()))
						.build());
			assertThat(resultFuehrungsformAttributGruppe).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND,
					AuditingContext.SAVE_FUEHRUNGSFORM_ATTRIBUT_GRUPPE_COMMAND);
			assertThat(resultFuehrungsformAttributGruppe).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testNetzControllerAuditing_saveZustaendigkeitAttributGruppeCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Kreis")
			.organisationsArt(OrganisationsArt.KREIS)
			.fachId(213)
			.bereich(GEOMETRY_FACTORY.createMultiPolygon(
				new Polygon[] {
					GEOMETRY_FACTORY.createPolygon(
						new Coordinate[] {
							new Coordinate(0, 0),
							new Coordinate(0, 1),
							new Coordinate(1, 1),
							new Coordinate(1, 0),
							new Coordinate(0, 0),
						})
				}))
			.build();

		gebietskoerperschaftRepository.save((Gebietskoerperschaft) organisation);

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		kantenRepository.save(kante);

		AdditionalRevInfoHolder.clear();

		// act

		SaveZustaendigkeitAttributeCommand zustaendigkeitAttributeCommand = new SaveZustaendigkeitAttributeCommand();

		ReflectionTestUtils.setField(zustaendigkeitAttributeCommand, "linearReferenzierterAbschnitt",
			LinearReferenzierterAbschnitt.of(0, 1));
		ReflectionTestUtils.setField(zustaendigkeitAttributeCommand, "baulastTraeger", organisation.getId());

		SaveZustaendigkeitAttributGruppeCommand command = SaveZustaendigkeitAttributGruppeCommand.builder().build();

		ReflectionTestUtils.setField(command, "kanteId", kante.getId());
		ReflectionTestUtils.setField(command, "gruppenID", kante.getZustaendigkeitAttributGruppe().getId());
		ReflectionTestUtils.setField(command, "gruppenVersion", kante.getZustaendigkeitAttributGruppe().getVersion());

		ReflectionTestUtils.setField(command, "zustaendigkeitAttribute",
			List.of(zustaendigkeitAttributeCommand));

		String requestJson = objectWriter.writeValueAsString(List.of(command));

		mockMvc.perform(post("/api/netz/kanten/saveZustaendigkeitAttributGruppe")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultZustaendigkeitAttributGruppe = auditReader.createQuery()
				.forRevisionsOfEntity(ZustaendigkeitAttributGruppe.class, false, true)
				.getResultList();

			assertThat(resultZustaendigkeitAttributGruppe).hasSize(2);
			assertThat(resultZustaendigkeitAttributGruppe).extracting(objArray -> objArray[0])
				.containsExactly(
					kante.getZustaendigkeitAttributGruppe(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
						.id(kante.getZustaendigkeitAttributGruppe().getId())
						.zustaendigkeitAttribute(
							List.of(ZustaendigkeitAttribute.builder()
								.baulastTraeger(organisation)
								.build()))
						.build());
			assertThat(resultZustaendigkeitAttributGruppe).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND,
					AuditingContext.SAVE_ZUSTAENDIGKEIT_ATTRIBUT_GRUPPE_COMMAND);
			assertThat(resultZustaendigkeitAttributGruppe).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testNetzControllerAuditing_saveKanteFahrtrichtungCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		kantenRepository.save(kante);

		AdditionalRevInfoHolder.clear();

		// act

		SaveKanteFahrtrichtungCommand command = SaveKanteFahrtrichtungCommand.builder().build();

		ReflectionTestUtils.setField(command, "kanteId", kante.getId());
		ReflectionTestUtils.setField(command, "gruppenId", kante.getFahrtrichtungAttributGruppe().getId());
		ReflectionTestUtils.setField(command, "gruppenVersion", kante.getFahrtrichtungAttributGruppe().getVersion());

		ReflectionTestUtils.setField(command, "fahrtrichtungLinks", Richtung.GEGEN_RICHTUNG);
		ReflectionTestUtils.setField(command, "fahrtrichtungRechts", Richtung.GEGEN_RICHTUNG);

		String requestJson = objectWriter.writeValueAsString(List.of(command));

		mockMvc.perform(post("/api/netz/kanten/saveFahrtrichtungAttributGruppe")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultFahrtrichtungAttributGruppe = auditReader.createQuery()
				.forRevisionsOfEntity(FahrtrichtungAttributGruppe.class, false, true)
				.getResultList();

			FahrtrichtungAttributGruppe expectedFahrtrichtungAttributGruppe = new FahrtrichtungAttributGruppe(
				Richtung.GEGEN_RICHTUNG, Richtung.GEGEN_RICHTUNG, false);

			ReflectionTestUtils.setField(expectedFahrtrichtungAttributGruppe, "id",
				kante.getFahrtrichtungAttributGruppe().getId());
			assertThat(resultFahrtrichtungAttributGruppe).hasSize(2);
			assertThat(resultFahrtrichtungAttributGruppe).extracting(objArray -> objArray[0])
				.containsExactly(
					kante.getFahrtrichtungAttributGruppe(),
					expectedFahrtrichtungAttributGruppe);
			assertThat(resultFahrtrichtungAttributGruppe).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND,
					AuditingContext.SAVE_KANTE_FAHRTRICHTUNG_COMMAND);
			assertThat(resultFahrtrichtungAttributGruppe).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testNetzControllerAuditing_saveKnotenCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Knoten knoten = KnotenTestDataProvider.withDefaultValues().build();

		knotenRepository.save(knoten);

		AdditionalRevInfoHolder.clear();

		// act

		SaveKnotenCommand command = SaveKnotenCommand.builder()
			.id(knoten.getId())
			.knotenVersion(knoten.getVersion())
			.knotenForm(KnotenForm.QUERUNGSSTELLE_OHNE_SICHERUNG)
			.build();

		String requestJson = objectWriter.writeValueAsString(command);

		mockMvc.perform(post("/api/netz/knoten/save")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON)
			.flashAttr("user",
				BenutzerTestDataProvider.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
					.build()))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKnoten = auditReader.createQuery()
				.forRevisionsOfEntity(Knoten.class, false, true)
				.getResultList();

			assertThat(resultKnoten).hasSize(2);
			assertThat(resultKnoten).extracting(objArray -> objArray[0])
				.containsExactly(
					knoten,
					KnotenTestDataProvider.withDefaultValues()
						.id(knoten.getId())
						.knotenAttribute(KnotenAttribute.builder()
							.knotenForm(KnotenForm.QUERUNGSSTELLE_OHNE_SICHERUNG)
							.build())
						.build());
			assertThat(resultKnoten).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND,
					AuditingContext.SAVE_KNOTEN_COMMAND);
			assertThat(resultKnoten).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testZuordnungControllerAuditing_changeZuordnungCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		Kante kanteRadNETZ = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ)
			.kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(
						KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
							.beleuchtung(Beleuchtung.VORHANDEN)
							.build())
					.build())
			.build();
		Kante kanteDLM = KanteTestDataProvider.withDefaultValues().build();

		kantenRepository.save(kanteRadNETZ);
		kantenRepository.save(kanteDLM);

		AdditionalRevInfoHolder.clear();

		// act

		ChangeZuordnungCommand command = new ChangeZuordnungCommand();

		ReflectionTestUtils.setField(command, "radnetzKanteId", kanteRadNETZ.getId());
		ReflectionTestUtils.setField(command, "dlmnetzKanteIds", Set.of(kanteDLM.getId()));

		String requestJson = objectWriter.writeValueAsString(command);

		mockMvc.perform(post("/api/zuordnung/changeZuordnung")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKantenAttributGruppe = auditReader.createQuery()
				.forRevisionsOfEntity(KantenAttributGruppe.class, false, true)
				.getResultList();

			assertThat(resultKantenAttributGruppe).hasSize(3);
			assertThat(resultKantenAttributGruppe).extracting(objArray -> objArray[0])
				.containsExactly(
					kanteRadNETZ.getKantenAttributGruppe(),
					kanteDLM.getKantenAttributGruppe(),
					KantenAttributGruppeTestDataProvider.defaultValue()
						.id(kanteDLM.getKantenAttributGruppe().getId())
						.kantenAttribute(kanteRadNETZ.getKantenAttributGruppe().getKantenAttribute())
						.build());
			assertThat(resultKantenAttributGruppe).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND, AuditingContext.CREATE_KANTE_COMMAND,
					AuditingContext.CHANGE_ZUORDNUNG_COMMAND);
			assertThat(resultKantenAttributGruppe).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testZuordnungControllerAuditing_loescheZuordnungCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);

		importedFeaturePersistentRepository.save(
			ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
				.quelle(QuellSystem.DLM)
				.fachId("id")
				.build());

		Kante kanteDLM = KanteTestDataProvider.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("id"))
			.kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider.defaultValue()
					.kantenAttribute(
						KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
							.beleuchtung(Beleuchtung.VORHANDEN)
							.build())
					.build())
			.build();

		kantenRepository.save(kanteDLM);

		AdditionalRevInfoHolder.clear();

		// act

		LoescheZuordnungenCommand command = new LoescheZuordnungenCommand();

		ReflectionTestUtils.setField(command, "dlmnetzKanteId", kanteDLM.getId());

		String requestJson = objectWriter.writeValueAsString(command);

		mockMvc.perform(post("/api/zuordnung/loescheZuordnung")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKantenAttributGruppe = auditReader.createQuery()
				.forRevisionsOfEntity(KantenAttributGruppe.class, false, true)
				.getResultList();

			assertThat(resultKantenAttributGruppe).hasSize(2);
			assertThat(resultKantenAttributGruppe).extracting(objArray -> objArray[0])
				.containsExactly(
					kanteDLM.getKantenAttributGruppe(),
					KantenAttributGruppeTestDataProvider.defaultValue()
						.id(kanteDLM.getKantenAttributGruppe().getId())
						.build());
			assertThat(resultKantenAttributGruppe).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND,
					AuditingContext.LOESCHE_ZUORDNUNGEN_COMMAND);
			assertThat(resultKantenAttributGruppe).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});
	}

	@Test
	void testNetzControllerAuditing_createKanteCommand_speichertKorrekteRevInfo()
		throws Exception {
		// arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten bisKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 20), QuellSystem.DLM)
			.build();

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.SAVE_KNOTEN_COMMAND);
		knotenRepository.saveAll(List.of(vonKnoten, bisKnoten));
		AdditionalRevInfoHolder.clear();

		// act
		CreateKanteCommand command = CreateKanteCommand.builder().vonKnotenId(vonKnoten.getId())
			.bisKnotenId(bisKnoten.getId()).build();

		String requestJson = objectWriter.writeValueAsString(command);

		mockMvc.perform(post("/api/netz/kanten/create")
			.content(requestJson)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKante = auditReader.createQuery()
				.forRevisionsOfEntity(Kante.class, false, true)
				.getResultList();

			assertThat(resultKante).hasSize(1);

			Kante kante = (Kante) resultKante.get(0)[0];
			assertThat(kante.getGeometry().getCoordinates())
				.isEqualTo(GeometryTestdataProvider
					.createLineString(vonKnoten.getKoordinate(), bisKnoten.getKoordinate()).getCoordinates());
			assertThat(kante.isGrundnetz()).isTrue();
			assertThat(kante.getQuelle()).isEqualTo(QuellSystem.RadVis);
			assertThat(kante.getVonKnoten()).isEqualTo(vonKnoten);
			assertThat(kante.getNachKnoten()).isEqualTo(bisKnoten);

			assertThat((RevInfo) resultKante.get(0)[1])
				.extracting(RevInfo::getAuditingContext)
				.isEqualTo(AuditingContext.CREATE_KANTE_COMMAND);

			assertThat((RevisionType) resultKante.get(0)[2])
				.isEqualTo(RevisionType.ADD);
			return null;
		});

	}

	@Test
	void testDlmNetzbildungsJob() {
		// arrange
		importedFeaturePersistentRepository.save(
			ImportedFeatureTestDataProvider.defaultRadNetzStrecke().quelle(QuellSystem.DLM).build());

		// act
		dlmNetzbildungJob.run();
		// assert
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.executeWithoutResult(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());
			@SuppressWarnings("unchecked")
			List<Object[]> resultKanten = auditReader.createQuery()
				.forRevisionsOfEntity(Kante.class, false, true)
				.getResultList();

			assertThat(resultKanten).hasSize(1);

			assertThat(resultKanten).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.NETZBILDUNG_DLM_JOB);
			assertThat(resultKanten).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD);
		});
	}
}
