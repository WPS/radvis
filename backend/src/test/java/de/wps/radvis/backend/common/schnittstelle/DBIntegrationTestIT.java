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

package de.wps.radvis.backend.common.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.wps.radvis.backend.auditing.AuditingConfiguration;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import liquibase.servicelocator.LiquibaseService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@LiquibaseService
@ContextConfiguration(classes = {
	AuditingConfiguration.class,
})
@EntityScan(basePackageClasses = { BenutzerConfiguration.class, OrganisationConfiguration.class,
	CommonConfiguration.class })
@ActiveProfiles("test")
// Da wir die statische Initialisierung des RadVisDomainEventPublisher am Ende jeder IT-Testklasse
// zurücksetzen müssen wir sicherstellen, dass dieser in folgenden IT-Test neu gebaut wird.
@DirtiesContext
@Testcontainers
public abstract class DBIntegrationTestIT {

	@MockitoBean
	private MailService mailService;

	@Container
	public static PostgreSQLContainer<RadVisTestContainer> postgreSQLContainer = RadVisTestContainer.getInstance();

	@DynamicPropertySource
	static void postgresqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
		registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
		registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
	}

	@AfterAll
	static void cleanUpRadVisDomainEventPublisher() {
		// Durch einbinden der CommonConfiguration wird dieser Konstruktor mit einem ApplicationEventPublisher
		// aufgerufen und dieser wird in einer static am RadVisDomainEventPublisher abgelegt.
		// Dadurch erhalten alle folgenden Testklassen (auch reine Unit-Tests ohne SpringContext!!!)
		// eine bereits initialiserte static in der Klasse! Das darf nicht sein und @DirtiesContext hilft hier
		// auch nicht weiter. Deshalb erfolgt hier das Setzen auf null mittels abschließendem Konstruktoraufruf.
		// TODO geht das schöner? Hoffentlich...
		new RadVisDomainEventPublisher(null);
	}

	@AfterEach
	void checkAuditingCleanUp() {
		assertThat(AdditionalRevInfoHolder.getAuditingContext()).isNull();
		assertThat(AdditionalRevInfoHolder.getJobExecutionDescription()).isNull();
	}

}
