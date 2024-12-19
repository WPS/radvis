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

package de.wps.radvis.backend.netz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LineareReferenzenDefragmentierungJobStatistik;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;

class LineareReferenzenDefragmentierungJobTest {
	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	private ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppeRepository;
	@Mock
	private FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppeRepository;
	@Mock
	private GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;
	@Mock
	private KantenRepository kantenRepository;

	private LineareReferenzenDefragmentierungJob lineareReferenzenDefragmentierungJob;

	private Laenge minimaleSegmentLaenge = Laenge.of(15.0);

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		lineareReferenzenDefragmentierungJob = new LineareReferenzenDefragmentierungJob(
			jobExecutionDescriptionRepository,
			zustaendigkeitAttributGruppeRepository, fuehrungsformAttributGruppeRepository,
			geschwindigkeitAttributGruppeRepository, kantenRepository, minimaleSegmentLaenge);
	}

	@SuppressWarnings("unchecked")
	@Test
	void doRun() {
		// arrange
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = ZustaendigkeitAttributGruppe.builder()
			.id(1l)
			.zustaendigkeitAttribute(
				List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.1).build(),
					ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.1, 1).build()))
			.build();

		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.id(2l)
			.geschwindigkeitAttribute(
				List.of(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.1).build(),
					GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.1, 1).build()))
			.build();

		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.id(3l)
			.fuehrungsformAttributeLinks(
				List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 1).build()))
			.fuehrungsformAttributeRechts(
				List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 1).build()))
			.build();

		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.id(4l)
			.zustaendigkeitAttributGruppe(zustaendigkeitAttributGruppe)
			.fuehrungsformAttributGruppe(fuehrungsformAttributGruppe).build();

		Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM)
			.id(5l)
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppe).build();

		Kante kante3 = KanteTestDataProvider.withCoordinatesAndQuelle(0, minimaleSegmentLaenge.getValue(), 0, 0,
			QuellSystem.DLM).build();

		when(fuehrungsformAttributGruppeRepository.findAllWithSegmenteKleinerAls(anyDouble()))
			.thenReturn(List.of(fuehrungsformAttributGruppe));
		when(geschwindigkeitAttributGruppeRepository.findAllWithSegmenteKleinerAls(anyDouble()))
			.thenReturn(List.of(geschwindigkeitAttributGruppe));
		when(zustaendigkeitAttributGruppeRepository.findAllWithSegmenteKleinerAls(anyDouble()))
			.thenReturn(List.of(zustaendigkeitAttributGruppe));

		when(kantenRepository.findAllByFuehrungsformAttributGruppeIn(anyList())).thenReturn(Set.of(kante1, kante3));
		when(kantenRepository.findAllByZustaendigkeitAttributGruppeIn(anyList())).thenReturn(Set.of(kante1, kante3));
		when(kantenRepository.findAllByGeschwindigkeitAttributeGruppeIn(anyList())).thenReturn(Set.of(kante2, kante3));

		// act
		Optional<JobStatistik> statistik = lineareReferenzenDefragmentierungJob.doRun();

		// assert
		verify(fuehrungsformAttributGruppeRepository).findAllWithSegmenteKleinerAls(eq(15.0));
		verify(zustaendigkeitAttributGruppeRepository).findAllWithSegmenteKleinerAls(eq(15.0));
		verify(geschwindigkeitAttributGruppeRepository).findAllWithSegmenteKleinerAls(eq(15.0));

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> findAllByFuehrungsformAttributGruppeInArgumentCaptor = ArgumentCaptor.forClass(List.class);
		verify(kantenRepository)
			.findAllByFuehrungsformAttributGruppeIn(findAllByFuehrungsformAttributGruppeInArgumentCaptor.capture());
		assertThat(findAllByFuehrungsformAttributGruppeInArgumentCaptor.getValue())
			.containsExactly(fuehrungsformAttributGruppe);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> findAllByGeschwindigkeitAttributeGruppeInArgumentCaptor = ArgumentCaptor
			.forClass(List.class);
		verify(kantenRepository)
			.findAllByGeschwindigkeitAttributeGruppeIn(
				findAllByGeschwindigkeitAttributeGruppeInArgumentCaptor.capture());
		assertThat(findAllByGeschwindigkeitAttributeGruppeInArgumentCaptor.getValue())
			.containsExactly(geschwindigkeitAttributGruppe);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> findAllByZustaendigkeitAttributGruppeInArgumentCaptor = ArgumentCaptor
			.forClass(List.class);
		verify(kantenRepository)
			.findAllByZustaendigkeitAttributGruppeIn(findAllByZustaendigkeitAttributGruppeInArgumentCaptor.capture());
		assertThat(findAllByZustaendigkeitAttributGruppeInArgumentCaptor.getValue())
			.containsExactly(zustaendigkeitAttributGruppe);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<Set> saveKantenArgumentCaptor = ArgumentCaptor.forClass(Set.class);
		verify(kantenRepository).saveAll(saveKantenArgumentCaptor.capture());
		assertThat(saveKantenArgumentCaptor.getValue()).containsExactlyInAnyOrder(kante1, kante2, kante3);

		assertThat(kante1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.containsExactly(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1).build());
		assertThat(kante1.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
			.containsExactly(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1).build());
		assertThat(kante2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build());
		assertThat(kante2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.containsExactly(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build());

		assertThat(statistik).isNotEmpty();
		LineareReferenzenDefragmentierungJobStatistik lineareReferenzenDefragmentierungJobStatistik = (LineareReferenzenDefragmentierungJobStatistik) statistik
			.get();
		assertThat(lineareReferenzenDefragmentierungJobStatistik.anzahlBetrachteteKanten).isEqualTo(3);
		assertThat(lineareReferenzenDefragmentierungJobStatistik.anzahlEntfernterSegmente).isEqualTo(4);
		assertThat(lineareReferenzenDefragmentierungJobStatistik.anzahlFuehrungsformAttributGruppen).isEqualTo(1);
		assertThat(lineareReferenzenDefragmentierungJobStatistik.anzahlGeschwindigkeitAttributGruppen).isEqualTo(1);
		assertThat(lineareReferenzenDefragmentierungJobStatistik.anzahlZustaendigkeitAttributGruppen).isEqualTo(1);
		assertThat(lineareReferenzenDefragmentierungJobStatistik.anzahlKantenKleinerAlsMinimaleSegmentLaenge)
			.isEqualTo(1);
	}
}
