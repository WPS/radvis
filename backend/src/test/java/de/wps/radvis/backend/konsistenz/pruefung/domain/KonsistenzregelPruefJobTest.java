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

package de.wps.radvis.backend.konsistenz.pruefung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.konsistenz.KonsistenzregelVerletzungTestdataProvider;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.konsistenz.regeln.domain.Konsistenzregel;

class KonsistenzregelPruefJobTest {

	private final LocalDateTime datumAlterDurchlauf = LocalDateTime.of(2023, 2, 21, 0, 0, 0);
	private KonsistenzregelPruefJob konsistenzregelPruefJob;

	@Mock
	private KonsistenzregelVerletzungsRepository verletzungsRepository;

	@Mock
	private Konsistenzregel konsistenzregel;

	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	static MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(konsistenzregel.getTitel()).thenReturn("Doktor");
		when(konsistenzregel.getVerletzungsTyp()).thenReturn("Toller Typ");

		when(verletzungsRepository.findAllByTyp(any())).thenReturn(List.of(
			KonsistenzregelVerletzungTestdataProvider.defaultVerletzung("1").build(),
			KonsistenzregelVerletzungTestdataProvider.defaultVerletzung("2").datum(datumAlterDurchlauf).build(),
			KonsistenzregelVerletzungTestdataProvider.defaultVerletzung("3").datum(datumAlterDurchlauf).build())
		);

		when(konsistenzregel.pruefen()).thenReturn(List.of(
			KonsistenzregelVerletzungTestdataProvider.getDefaultKonsistenzregelVerletzungsDetails("2"),
			KonsistenzregelVerletzungTestdataProvider.getDefaultKonsistenzregelVerletzungsDetails("3",
				"Neue Beschreibung!"),
			KonsistenzregelVerletzungTestdataProvider.getDefaultKonsistenzregelVerletzungsDetails("4")));

		konsistenzregelPruefJob = new KonsistenzregelPruefJob(List.of(konsistenzregel),
			verletzungsRepository, jobExecutionDescriptionRepository);
		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterAll
	static void afterAll() {
		domainPublisherMock.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	void doRun() {
		// Act
		konsistenzregelPruefJob.doRun();

		// Assert
		// Delete
		ArgumentCaptor<List<String>> deleteAllArgument = ArgumentCaptor.forClass(List.class);
		verify(verletzungsRepository).deleteAllByTypAndIdentityIn(anyString(), deleteAllArgument.capture());
		assertThat(deleteAllArgument.getValue()).containsExactly("1");

		ArgumentCaptor<List<KonsistenzregelVerletzung>> saveAllArgument = ArgumentCaptor.forClass(List.class);
		verify(verletzungsRepository, times(2)).saveAll(saveAllArgument.capture());

		// Erster Aufruf von saveAll (updateIfNotEqual)
		List<KonsistenzregelVerletzung> updatedVerletzungen = saveAllArgument.getAllValues().get(0);
		assertThat(updatedVerletzungen).extracting(KonsistenzregelVerletzung::getIdentity)
			.doesNotContain("2");

		assertThat(updatedVerletzungen).extracting(KonsistenzregelVerletzung::getIdentity).contains("3");
		assertThat(updatedVerletzungen).extracting(KonsistenzregelVerletzung::getBeschreibung)
			.containsExactly("Neue Beschreibung!");
		assertThat(updatedVerletzungen).extracting(KonsistenzregelVerletzung::getDatum)
			.doesNotContain(datumAlterDurchlauf);

		// Zweiter Aufruf von saveAll (create)
		assertThat(saveAllArgument.getAllValues().get(1)).extracting(KonsistenzregelVerletzung::getIdentity)
			.containsExactly("4");
	}
}
