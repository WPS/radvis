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

package de.wps.radvis.backend.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.wps.radvis.backend.auditing.AuditingConfiguration;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.schnittstelle.WithAuditingAspect;
import de.wps.radvis.backend.common.schnittstelle.RadVisTestContainer;
import liquibase.servicelocator.LiquibaseService;

@EnableAspectJAutoProxy
@TypeExcludeFilters({ DataJpaTypeExcludeFilter.class })
@Testcontainers
@AutoConfigureDataJpa
@AutoConfigureTestEntityManager
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@LiquibaseService
@DirtiesContext
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
@ContextConfiguration(classes = {
	AuditingConfiguration.class,
	WithAuditingAspect.class,
})
public abstract class AuditingTestIT {
	@Autowired
	protected TestEntityManager testEntityManager;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Container
	public static PostgreSQLContainer<RadVisTestContainer> postgreSQLContainer = RadVisTestContainer.getInstance();

	@DynamicPropertySource
	static void postgresqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
		registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
		registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
	}

	@AfterEach
	void cleanUp() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.executeWithoutResult(status -> {
			@SuppressWarnings("unchecked")
			List<String> tablesToclear = testEntityManager.getEntityManager()
				.createNativeQuery(
					"select relname from pg_stat_all_tables " +
						"where schemaname = 'public' and NOT ("
						+ "relname LIKE 'databasechangelog%' "
						+ "OR relname = 'spatial_ref_sys' "
						+ "OR relname LIKE '%materialized_view' "
						+ ")")
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

	@AfterAll
	static void cleanUpAll() {
		SecurityContextHolder.clearContext();

		// Durch einbinden der CommonConfiguration wird dieser Konstruktor mit einem ApplicationEventPublisher
		// aufgerufen und dieser wird in einer static am RadVisDomainEventPublisher abgelegt.
		// Dadurch erhalten alle folgenden Testklassen (auch reine Unit-Tests ohne SpringContext!!!)
		// eine bereits initialiserte static in der Klasse! Das darf nicht sein und @DirtiesContext hilft hier
		// auch nicht weiter. Deshalb erfolgt hier das Setzen auf null mittels abschließendem Konstruktoraufruf.
		// TODO geht das schöner? Hoffentlich...
		new RadVisDomainEventPublisher(null);
	}
}
