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
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import de.wps.radvis.backend.application.JacksonConfiguration;
import de.wps.radvis.backend.auditing.AuditingConfiguration;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.schnittstelle.WithAuditingAspect;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeMergeService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingService;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.ZuordnungController;
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
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteAttributeCommand;
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
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import lombok.NonNull;

@Tag("group7")
@ContextConfiguration(classes = {
	NetzControllerIntegrationTestIT.TestConfig.class,
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
public class NetzControllerIntegrationTestIT extends AuditingTestIT {

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

		@Autowired
		KantenAttributeMergeService kantenAttributeMergeService;

		@Autowired
		KantenMappingService kantenMappingService;

		@MockBean
		ZustaendigkeitsService zustaendigkeitsService;

		@MockBean
		NetzGuard netzGuard;

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

			return new NetzController(netzService, netzGuard, benutzerResolver, zustaendigkeitsService,
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
	private EntityManager entityManager;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	NetzController netzController;

	@Autowired
	ZuordnungController zuordnungController;

	@BeforeEach
	void setUp() {
		gebietskoerperschaftRepository.save((Gebietskoerperschaft) organisation);
	}

	@Test
	@Transactional
	void saveKanteAllgemein_preserveNonEditableFields() {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.SAVE_KANTE_ATTRIBUTE_COMMAND);
		Kante kanteBefore = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.strassenName(StrassenName.of("Straßenname")).strassenNummer(StrassenNummer.of("8")).build(),
				Collections.emptySet(), Collections.emptySet()))
			.build());
		entityManager.flush();
		entityManager.clear();

		// act
		@NonNull
		KantenAttribute kantenAttributeBefore = kanteBefore.getKantenAttributGruppe().getKantenAttribute();
		netzController.saveKanteAllgemein(mock(Authentication.class),
			List.of(new SaveKanteAttributeCommand(kanteBefore.getKantenAttributGruppe().getId(),
				kanteBefore.getKantenAttributGruppe().getVersion(), kanteBefore.getId(),
				kantenAttributeBefore.getWegeNiveau().orElse(null),
				kantenAttributeBefore.getBeleuchtung(),
				kantenAttributeBefore.getUmfeld(),
				kantenAttributeBefore.getStrassenkategorieRIN().orElse(null),
				kantenAttributeBefore.getStrassenquerschnittRASt06(),
				kantenAttributeBefore.getLaengeManuellErfasst().orElse(null),
				kantenAttributeBefore.getDtvFussverkehr().orElse(null),
				kantenAttributeBefore.getDtvRadverkehr().orElse(null),
				kantenAttributeBefore.getDtvPkw().orElse(null),
				kantenAttributeBefore.getSv().orElse(null),
				kantenAttributeBefore.getKommentar().orElse(null),
				kantenAttributeBefore.getGemeinde().map(g -> g.getId()).orElse(null),
				kantenAttributeBefore.getStatus(),
				kanteBefore.getKantenAttributGruppe().getNetzklassen(),
				kanteBefore.getKantenAttributGruppe().getIstStandards())));
		entityManager.flush();
		entityManager.clear();

		// assert
		Kante kanteAfter = kantenRepository.findById(kanteBefore.getId()).get();
		assertThat(kanteAfter.getKantenAttributGruppe().getKantenAttribute())
			.isEqualTo(kantenAttributeBefore);
	}
}
