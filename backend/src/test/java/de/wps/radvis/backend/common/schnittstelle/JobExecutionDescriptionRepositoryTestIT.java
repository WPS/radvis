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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group6")
@ContextConfiguration(classes = JobExecutionDescriptionRepositoryTestIT.TestConfiguration.class)
public class JobExecutionDescriptionRepositoryTestIT extends DBIntegrationTestIT {

	@Configuration
	@EnableJpaRepositories(basePackages = "de.wps.radvis.backend.common")
	@EntityScan({ "de.wps.radvis.backend.common.domain.entity", "de.wps.radvis.backend.common.domain.valueObject" })
	public static class TestConfiguration {
	}

	@Autowired
	private JobExecutionDescriptionRepository repository;

	@PersistenceContext
	EntityManager entityManager;

	@Test
	public void saveAndGet() {
		// Arrange
		assertThat(repository).isNotNull();

		var start = LocalDateTime.of(2021, 1, 1, 5, 0);
		var end = LocalDateTime.of(2021, 1, 1, 5, 2);

		var jobDescription = JobExecutionDescription.builder()
			.name("TestJob")
			.inputSummary("Datei vom 1.1.2021")
			.executionStart(start)
			.executionEnd(end)
			.statistic(
				"{Ich bin eine Statistik. Ich bin ein JSON. Sieht man doch, habe schließlich geschweifte klammern!}")
			.build();

		// Act
		long id = repository.save(jobDescription).getId();

		entityManager.flush();
		entityManager.clear();

		Optional<JobExecutionDescription> result = repository.findById(id);

		// Assert
		assertThat(result).isPresent();
		JobExecutionDescription resultingFeature = result.get();

		assertThat(resultingFeature.getName()).isEqualTo("TestJob");
		assertThat(resultingFeature.getInputSummary()).isEqualTo("Datei vom 1.1.2021");
		assertThat(resultingFeature.getExecutionStart()).isEqualTo(start);
		assertThat(resultingFeature.getExecutionEnd()).isEqualTo(end);
		assertThat(resultingFeature.getStatistic()).isEqualTo(
			"{Ich bin eine Statistik. Ich bin ein JSON. Sieht man doch, habe schließlich geschweifte klammern!}");
	}

	@Test
	public void findFirstByNameEqualsOrderByExecutionStartDesc() {
		// Arrange
		assertThat(repository).isNotNull();

		var start1 = LocalDateTime.of(2021, 1, 1, 5, 0);
		var end1 = LocalDateTime.of(2021, 1, 1, 5, 2);
		var jobDescription1 = JobExecutionDescription.builder()
			.name("TestJob")
			.inputSummary("Datei vom 1.1.2021")
			.executionStart(start1)
			.executionEnd(end1)
			.build();

		var start2 = LocalDateTime.of(2021, 1, 2, 5, 0);
		var end2 = LocalDateTime.of(2021, 1, 2, 5, 2);
		var jobDescription2 = JobExecutionDescription.builder()
			.name("TestJob")
			.inputSummary("Datei vom 2.1.2021")
			.executionStart(start2)
			.executionEnd(end2)
			.build();

		// Dieser Job ist zwar später, aber vom Typ "TestJob 2", daher soll er nicht gefunden werden.
		var start3 = LocalDateTime.of(2021, 1, 3, 5, 0);
		var end3 = LocalDateTime.of(2021, 1, 3, 5, 2);
		var jobDescription3 = JobExecutionDescription.builder()
			.name("TestJob 2")
			.inputSummary("Datei vom 3.1.2021")
			.executionStart(start3)
			.executionEnd(end3)
			.build();

		repository.save(jobDescription1).getId();
		long id2 = repository.save(jobDescription2).getId();
		repository.save(jobDescription3).getId();

		entityManager.flush();

		// Act
		Optional<JobExecutionDescription> result = repository.findFirstByNameEqualsOrderByExecutionStartDesc("TestJob");

		// Assert
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(id2);
	}

	@Test
	public void findAllByNameInAfterOrderByExecutionStartDesc() {
		// Arrange
		assertThat(repository).isNotNull();

		var jobDescription1 = JobExecutionDescription.builder()
			.name("TestJob")
			.inputSummary("Datei vom 1.1.2021")
			.executionStart(LocalDateTime.of(2021, 1, 1, 5, 0))
			.executionEnd(LocalDateTime.of(2021, 1, 1, 5, 2))
			.build();

		var jobDescription2 = JobExecutionDescription.builder()
			.name("TestJob2")
			.inputSummary("Datei vom 2.1.2021")
			.executionStart(LocalDateTime.of(2021, 1, 2, 5, 0))
			.executionEnd(LocalDateTime.of(2021, 1, 2, 5, 2))
			.build();

		var jobDescription3 = JobExecutionDescription.builder()
			.name("TestJob2")
			.inputSummary("Datei vom 1.1.2021")
			.executionStart(LocalDateTime.of(2021, 1, 1, 3, 0))
			.executionEnd(LocalDateTime.of(2021, 1, 1, 3, 2))
			.build();

		// falscher typ
		var jobDescription4 = JobExecutionDescription.builder()
			.name("TestJob3")
			.inputSummary("Datei vom 2.1.2021")
			.executionStart(LocalDateTime.of(2021, 1, 2, 5, 0))
			.executionEnd(LocalDateTime.of(2021, 1, 2, 5, 2))
			.build();

		// Zu alt
		var jobDescription5 = JobExecutionDescription.builder()
			.name("TestJob")
			.inputSummary("Datei vom 3.1.2021")
			.executionStart(LocalDateTime.of(2020, 1, 3, 5, 0))
			.executionEnd(LocalDateTime.of(2020, 1, 3, 5, 0))
			.build();

		jobDescription1 = repository.save(jobDescription1);
		jobDescription2 = repository.save(jobDescription2);
		jobDescription3 = repository.save(jobDescription3);
		repository.save(jobDescription4);
		repository.save(jobDescription5);

		entityManager.flush();

		// Act
		List<JobExecutionDescription> result = repository
			.findAllByNameInAfterOrderByExecutionStartDesc(LocalDateTime.of(2021, 1, 1, 0, 0),
				List.of("TestJob", "TestJob2"));

		// Assert
		assertThat(result).containsExactly(jobDescription2, jobDescription1, jobDescription3);
	}
}
