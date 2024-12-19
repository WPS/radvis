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

package de.wps.radvis.backend.konsistenz.regeln.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;

@ContextConfiguration(classes = {
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	AbstractKonsistenzregelTestIT.TestConfiguration.class,
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	FeatureToggleProperties.class,
	PostgisConfigurationProperties.class,
	JobConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@AutoConfigureTestEntityManager
abstract class AbstractKonsistenzregelTestIT extends DBIntegrationTestIT {
	@Autowired
	protected TestEntityManager testEntityManager;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Configuration
	public static class TestConfiguration {
		@MockBean
		public JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	}

	@AfterEach
	public void cleanup() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.executeWithoutResult(status -> {
			@SuppressWarnings("unchecked")
			List<String> tablesToclear = testEntityManager.getEntityManager()
				.createNativeQuery(
					"""
						SELECT t.tablename
						FROM pg_catalog.pg_tables t
						WHERE t.schemaname = 'public'
							AND t.tablename != 'spatial_ref_sys'
							AND t.tablename NOT LIKE 'databasechangelog%'
						""")
				.getResultList();

			tablesToclear.forEach(table -> {
				testEntityManager.getEntityManager()
					.createNativeQuery("TRUNCATE " + table + " CASCADE").executeUpdate();

			});
		});
		AdditionalRevInfoHolder.clear();
		SecurityContextHolder.clearContext();
		assertThat(AdditionalRevInfoHolder.getAuditingContext()).isNull();
		assertThat(AdditionalRevInfoHolder.getJobExecutionDescription()).isNull();
	}
}
