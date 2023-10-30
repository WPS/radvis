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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KanteDublette;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

class AttributProjektionsJobTest {

	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	private AttributProjektionsService attributProjektionsService;
	@Mock
	private AttributeAnreicherungsService attributAnreicherungsService;
	@Mock
	private AttributProjektionsStatistikService attributProjektionsStatistikService;
	@Mock
	private KantenDublettenPruefungService kantenDublettenPruefungService;
	@Mock
	private NetzService netzService;
	@Mock
	private ImportedFeaturePersistentRepository importedFeaturePersistentRepository;

	@Mock
	private DLMConfigurationProperties dlmConfigurationProperties;

	@Mock
	private NetzfehlerRepository netzfehlerRepository;

	@Mock
	EntityManager entityManager;

	private AttributProjektionsJob attributProjektionsJob;

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
	MockedStatic<AdditionalRevInfoHolder> auditingContextServiceMockedStatic;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		attributProjektionsJob = new AttributProjektionsJob(jobExecutionDescriptionRepository,
			attributProjektionsService, attributAnreicherungsService, attributProjektionsStatistikService,
			netzfehlerRepository, kantenDublettenPruefungService, netzService, importedFeaturePersistentRepository,
			dlmConfigurationProperties, entityManager, QuellSystem.RadNETZ);

		auditingContextServiceMockedStatic = mockStatic(AdditionalRevInfoHolder.class);
	}

	@AfterEach
	void cleanUp() {
		auditingContextServiceMockedStatic.close();
	}

	@Test
	public void testeDublettenPartitioniertHolen() {
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(1L).build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues().id(2L).build();
		Kante kante3 = KanteTestDataProvider.withDefaultValues().id(3L).build();
		Kante kante4 = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).id(4L).build();
		Kante kante5 = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).id(5L).build();
		Kante kante6 = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).id(6L).build();

		Envelope bereich1 = new Envelope(0, 10, 0, 30);
		Envelope bereich2 = new Envelope(10, 20, 0, 30);
		Envelope bereich3 = new Envelope(20, 30, 0, 30);

		when(netzService.getKantenInBereichNachQuelle(bereich1, QuellSystem.DLM)).thenReturn(
			Stream.of(kante1, kante2));
		Envelope copy1 = bereich1.copy();
		copy1.expandBy(1000);
		when(netzService.getKantenInBereichNachQuelleUndIsAbgebildet(copy1, QuellSystem.RadNETZ)).thenReturn(
			Set.of(kante4, kante5));

		when(netzService.getKantenInBereichNachQuelle(bereich2, QuellSystem.DLM)).thenReturn(
			Stream.of(kante2, kante3));
		Envelope copy2 = bereich2.copy();
		copy2.expandBy(1000);
		when(netzService.getKantenInBereichNachQuelleUndIsAbgebildet(copy2, QuellSystem.RadNETZ)).thenReturn(
			Set.of(kante5, kante6));

		when(netzService.getKantenInBereichNachQuelle(bereich3, QuellSystem.DLM)).thenReturn(
			Stream.of(kante3, kante4));
		Envelope copy3 = bereich3.copy();
		copy3.expandBy(1000);
		when(netzService.getKantenInBereichNachQuelleUndIsAbgebildet(copy3, QuellSystem.RadNETZ)).thenReturn(
			Set.of(kante4, kante6));

		KanteDublette mock = mock(KanteDublette.class);
		when(mock.getZielnetzUeberschneidung()).thenReturn(GEO_FACTORY.createLineString());
		KanteDublette mock2 = mock(KanteDublette.class);
		when(mock2.getZielnetzUeberschneidung()).thenReturn(GEO_FACTORY.createLineString());
		KanteDublette mock3 = mock(KanteDublette.class);
		when(mock3.getZielnetzUeberschneidung()).thenReturn(GEO_FACTORY.createLineString());

		List<KanteDublette> result1 = new ArrayList<>(List.of(mock, mock2));
		List<KanteDublette> result2 = new ArrayList<>(List.of(mock3));
		List<KanteDublette> result3 = new ArrayList<>(List.of());
		when(kantenDublettenPruefungService.findDubletten(Set.of(kante1, kante2), Set.of(kante4, kante5)))
			.thenReturn(result1);
		when(kantenDublettenPruefungService.findDubletten(Set.of(kante3), Set.of(kante5, kante6)))
			.thenReturn(result2);
		when(kantenDublettenPruefungService.findDubletten(Set.of(), Set.of(kante4, kante6)))
			.thenReturn(result3);

		when(dlmConfigurationProperties.getExtentProperty()).thenReturn(new ExtentProperty(0, 30, 0, 30));
		when(dlmConfigurationProperties.getPartitionenX()).thenReturn(3);

		when(importedFeaturePersistentRepository.getAllByQuelleAndArtAndGeometryType(any(), any(), any()))
			.thenAnswer((a) -> Stream.of());

		Query query = Mockito.mock(Query.class);

		when(entityManager.createQuery(any(String.class))).thenReturn(query);
		when(query.setParameter(any(String.class), any())).thenReturn(query);

		when(jobExecutionDescriptionRepository.save(
			any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

		attributProjektionsJob.run();

		verify(kantenDublettenPruefungService, times(1)).findDubletten(Set.of(kante1, kante2), Set.of(kante4, kante5));

		verify(kantenDublettenPruefungService, times(1)).findDubletten(Set.of(kante1, kante2), Set.of(kante5, kante4));

		verify(kantenDublettenPruefungService, times(1)).findDubletten(Set.of(kante1, kante2), Set.of(kante4, kante5));
		verify(attributProjektionsService, times(1))
			.projiziereAttributeAufGrundnetzKanten(same(result1), any(), any());
		verify(attributProjektionsService, times(1))
			.projiziereAttributeAufGrundnetzKanten(same(result2), any(), any());
	}

	@Test
	public void testeBerechneLaengeDesDLMAufDasProjiziertWurdeInMeter() {

		Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> projizierteNetzklassen = new HashMap<>();

		projizierteNetzklassen.put(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG),
			List.of(LinearReferenzierterAbschnitt.of(0, 0.2), LinearReferenzierterAbschnitt.of(0.1, 0.4),
				LinearReferenzierterAbschnitt.of(0.5, 0.6)));
		projizierteNetzklassen.put(Set.of(Netzklasse.RADNETZ_ALLTAG),
			List.of(LinearReferenzierterAbschnitt.of(0.3, 0.7), LinearReferenzierterAbschnitt.of(0.7, 0.8)));
		projizierteNetzklassen.put(Set.of(Netzklasse.KREISNETZ_ALLTAG),
			List.of(LinearReferenzierterAbschnitt.of(0.9, 1)));

		// act
		double result = attributProjektionsJob
			.berechneLaengeDesDLMAufDasProjiziertWurdeInMeter(100., projizierteNetzklassen);
		assertThat(result).isEqualTo(90.);
	}

}
