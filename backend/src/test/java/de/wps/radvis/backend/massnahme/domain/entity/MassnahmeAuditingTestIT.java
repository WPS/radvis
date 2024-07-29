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

package de.wps.radvis.backend.massnahme.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
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
import de.wps.radvis.backend.auditing.schnittstelle.WithAuditingAspect;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandabfrageService;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandsabfrageConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.schnittstelle.CreateMassnahmeCommandConverter;
import de.wps.radvis.backend.massnahme.schnittstelle.MassnahmeController;
import de.wps.radvis.backend.massnahme.schnittstelle.MassnahmeGuard;
import de.wps.radvis.backend.massnahme.schnittstelle.SaveMassnahmeCommandConverter;
import de.wps.radvis.backend.massnahme.schnittstelle.SaveUmsetzungsstandCommandConverter;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import lombok.NonNull;

@Tag("group7")
@ContextConfiguration(classes = {
	MassnahmeAuditingTestIT.TestConfig.class,
	AuditingConfiguration.class,
	MassnahmeConfiguration.class,
	DokumentConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	WithAuditingAspect.class,
	JacksonConfiguration.class,
	CommonConfiguration.class,
	KommentarConfiguration.class,
	MailConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	MailConfigurationProperties.class,
	UmsetzungsstandsabfrageConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class MassnahmeAuditingTestIT extends AuditingTestIT {

	// Der MassnahmeController wird dort, wo es um Berechtigungen geht mit Mocks bestückt,
	// um die enstprechenden Prüfungen auszuhebeln
	static class TestConfig {

		@Autowired
		MassnahmeService massnahmeService;

		@Autowired
		CreateMassnahmeCommandConverter createMassnahmeCommandConverter;

		@Autowired
		SaveMassnahmeCommandConverter saveMassnahmeCommandConverter;

		@Autowired
		SaveUmsetzungsstandCommandConverter saveUmsetzungsstandCommandConverter;

		@MockBean
		BenutzerResolver benutzerResolver;

		@MockBean
		@NonNull
		MassnahmeGuard massnahmeAuthorizationService;

		@MockBean
		ShapeFileRepository shapeFileRepository;

		@MockBean
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

		@MockBean
		SimpleMatchingService simpleMatchingService;

		@MockBean
		UmsetzungsstandabfrageService umsetzungsstandabfrageService;

		@MockBean
		private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

		@MockBean
		VerwaltungseinheitService verwaltungseinheitService;

		@Bean
		public MassnahmeController massnahmeController() {
			Mockito.when(benutzerResolver.fromAuthentication(Mockito.any()))
				.thenReturn(
					BenutzerTestDataProvider.admin(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
						.build());
			return new MassnahmeController(massnahmeService, umsetzungsstandabfrageService,
				createMassnahmeCommandConverter,
				saveMassnahmeCommandConverter, saveUmsetzungsstandCommandConverter,
				massnahmeAuthorizationService,
				benutzerResolver, verwaltungseinheitService);
		}

	}

	static final GeometryFactory GEOMETRY_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	static ObjectWriter objectWriter;

	@Autowired
	MassnahmeRepository massnahmeRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	KantenRepository kanteRepository;

	@Autowired
	BenutzerRepository benutzerRepository;

	@Autowired
	private TestEntityManager testEntityManager;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	MassnahmeController massnahmeController;

	@BeforeAll
	static void setupObjectWriter() {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("CustomGeometrySerializer", new Version(1, 0, 0, null, null, null));
		module.addSerializer(Geometry.class, new GeometryAsGeoJSONSerializer());
		mapper.registerModule(module);
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		objectWriter = mapper.writer().withDefaultPrettyPrinter();
	}

	@Test
	void testAuditing_speichertRevInfo() {
		// arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_MASSNAHME_COMMAND);

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues().build();

		Kante kante = massnahme.getNetzbezug().getImmutableKantenAbschnittBezug().stream().findFirst().get()
			.getKante();

		Benutzer benutzer = massnahme.getBenutzerLetzteAenderung();
		Verwaltungseinheit organisation = benutzer.getOrganisation();

		gebietskoerperschaftRepository.save((Gebietskoerperschaft) massnahme.getZustaendiger().get());
		gebietskoerperschaftRepository.save((Gebietskoerperschaft) organisation);
		benutzerRepository.save(benutzer);
		kanteRepository.save(kante);

		Long massnahmeId = massnahmeRepository.save(massnahme).getId();

		// act
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_MASSNAHME_COMMAND);

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			Massnahme finalMassnahme = massnahmeRepository.findById(massnahmeId).orElseThrow();
			ReflectionTestUtils.setField(finalMassnahme, "maViSID", MaViSID.of("neueMyViSID"));
			return null;
		});

		AdditionalRevInfoHolder.clear();

		// assert
		template.execute(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());

			@SuppressWarnings("unchecked")
			List<Object[]> resultMassnahme = auditReader.createQuery()
				.forRevisionsOfEntity(Massnahme.class, false, true)
				.getResultList();

			assertThat(resultMassnahme).hasSize(2);
			assertThat(resultMassnahme).extracting(objArray -> objArray[0])
				.containsExactly(MassnahmeTestDataProvider
					.withDefaultValues().id(massnahme.getId()).build(),
					massnahme);
			assertThat(resultMassnahme).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);
			return null;
		});

	}

	@Test
	void testAuditing_keinAuditingContext_wirftException() {
		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues().build();

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> massnahmeRepository.save(massnahme));
	}

}
