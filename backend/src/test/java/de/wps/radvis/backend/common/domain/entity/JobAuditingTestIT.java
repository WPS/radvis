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

package de.wps.radvis.backend.common.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import de.wps.radvis.backend.application.JacksonConfiguration;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.schnittstelle.RevInfo;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.grundnetz.IntegrationGrundnetzConfiguration;
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.integration.radwegedb.IntegrationRadwegeDBConfiguration;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.quellimport.common.ImportsCommonConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

@Tag("group7")
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	JacksonConfiguration.class,
	IntegrationGrundnetzConfiguration.class,
	NetzfehlerConfiguration.class, KommentarConfiguration.class,
	ImportsCommonConfiguration.class,
	CommonConfiguration.class,
	ImportsGrundnetzConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	IntegrationRadNetzConfiguration.class,
	IntegrationRadwegeDBConfiguration.class, KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	DLMConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class JobAuditingTestIT extends AuditingTestIT {

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Test
	void testAuditing_speichertJobExecutionDescriptionInRevInfo() {
		// arrange

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);
		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		Long kanteId = kantenRepository.save(kante).getId();

		LineString newVerlaufLineString = GeometryTestdataProvider.createLineString(new Coordinate(321, 321),
			new Coordinate(654, 654));
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.DLM_REIMPORT_JOB);

		AbstractJob job = new AbstractJob(jobExecutionDescriptionRepository) {

			@Override
			protected Optional<JobStatistik> doRun() {

				Optional<Kante> kanteById = kantenRepository.findById(kanteId);

				kanteById.get().updateVerlauf(newVerlaufLineString, newVerlaufLineString);

				return Optional.empty();
			}
		};

		// act
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		JobExecutionDescription jobExecutionDescription = template.execute(status -> job.run());

		// assert

		template.executeWithoutResult(status -> {
			AuditReader auditReader = AuditReaderFactory.get(testEntityManager.getEntityManager());

			@SuppressWarnings("unchecked")
			List<Object[]> resultKante = auditReader.createQuery()
				.forRevisionsOfEntity(Kante.class, false, true)
				.getResultList();

			assertThat(resultKante).hasSize(2);
			assertThat(resultKante).extracting(objArray -> objArray[0])
				.containsExactly(
					KanteTestDataProvider.withDefaultValues()
						.id(kante.getId())
						.build(),
					kante);
			assertThat(resultKante).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getAuditingContext)
				.containsExactly(AuditingContext.CREATE_KANTE_COMMAND, AuditingContext.DLM_REIMPORT_JOB);

			assertThat(resultKante).extracting(objArray -> (RevInfo) objArray[1])
				.extracting(RevInfo::getJobExecutionDescription)
				.containsExactly(null, jobExecutionDescription);

			assertThat(resultKante).extracting(objArray -> objArray[2])
				.containsExactly(RevisionType.ADD, RevisionType.MOD);

		});
	}

}
