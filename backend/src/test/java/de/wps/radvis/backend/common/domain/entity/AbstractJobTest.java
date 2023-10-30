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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

class AbstractJobTest {

	public static class TestStatistik extends JobStatistik {
		public int testFeld;

		public TestStatistik(int number) {
			this.testFeld = number;
		}
	}

	public static class MockService {
		public boolean modifySomething() {
			return false;
		}
	}

	@EqualsAndHashCode(callSuper = true)
	public static class TestJobExecutionDescription extends JobExecutionDescription {

		public TestJobExecutionDescription(Long id, @NonNull String name, @NonNull String inputSummary,
			@NonNull LocalDateTime executionStart, @NonNull LocalDateTime executionEnd,
			String statistic) {
			super(name, inputSummary, executionStart, executionEnd, statistic);
			this.id = id;
		}
	}

	public static class TestAbstractJobImplementation extends AbstractJob {

		public AtomicInteger executionCount;
		public MockService mockService;

		public TestAbstractJobImplementation(JobExecutionDescriptionRepository repository, MockService mockService) {
			super(repository);
			this.executionCount = new AtomicInteger(0);
			this.mockService = mockService;
			this.setInputSummarySupplier(() -> "痛みを");
		}

		@Override
		protected Optional<JobStatistik> doRun() {
			mockService.modifySomething();
			return Optional.of(new TestStatistik(executionCount.incrementAndGet()));
		}
	}

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	MockService mockService;

	TestAbstractJobImplementation job;

	JobExecutionDescription defaultJobExecutionDescription;

	MockedStatic<AdditionalRevInfoHolder> auditingContextServiceMockedStatic;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		job = new TestAbstractJobImplementation(jobExecutionDescriptionRepository, mockService);
		defaultJobExecutionDescription = new TestJobExecutionDescription(1L, "花譜", "痛みを",
			LocalDateTime.of(2022, 4, 29, 10, 35),
			LocalDateTime.of(2022, 4, 30, 10, 35), "1:答えを答えを答えを探す意味を知らず");

		auditingContextServiceMockedStatic = mockStatic(AdditionalRevInfoHolder.class);
	}

	@AfterEach
	void cleanUp() {
		auditingContextServiceMockedStatic.close();
	}

	@Test
	public void testeRun_notForced_alreadyExecuted_doesNotExecuteAgain() {
		job.executionCount.incrementAndGet();
		when(jobExecutionDescriptionRepository.findFirstByNameEqualsOrderByExecutionStartDesc(
			any())).thenReturn(Optional.of(defaultJobExecutionDescription
		));

		when(jobExecutionDescriptionRepository.save(
			any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

		JobExecutionDescription result = job.run();
		assertThat(result).isEqualTo(defaultJobExecutionDescription);
		assertThat(result.getStatistic().contains("1")).isTrue(); // first execution
		assertThat(result.getStatistic().contains("2")).isFalse();
		verify(mockService, Mockito.never()).modifySomething();
	}

	@Test
	public void testeRun_notForced_NotExecuted_doesExecute() {
		when(jobExecutionDescriptionRepository.findFirstByNameEqualsOrderByExecutionStartDesc(
			any())).thenReturn(Optional.empty());

		when(jobExecutionDescriptionRepository.save(
			any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

		JobExecutionDescription result = job.run();
		assertThat(result.getStatistic().contains("1")).isTrue(); // first execution
		verify(mockService).modifySomething();
	}

	@Test
	public void testeRun_Forced_onlyexecutesOnce() {
		job.executionCount.incrementAndGet(); // execution 1
		when(jobExecutionDescriptionRepository.findFirstByNameEqualsOrderByExecutionStartDesc(
			TestAbstractJobImplementation.class.getSimpleName())).thenReturn(Optional.of(defaultJobExecutionDescription
		));

		when(jobExecutionDescriptionRepository.save(
			any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

		JobExecutionDescription result = job.run(true);

		assertThat(result.getStatistic().contains("2")).isTrue(); // second execution
		assertThat(result.getStatistic().contains("1")).isFalse();
		verify(mockService, Mockito.only()).modifySomething();
	}
	
}
