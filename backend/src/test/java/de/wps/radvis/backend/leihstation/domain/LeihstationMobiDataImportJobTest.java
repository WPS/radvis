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

package de.wps.radvis.backend.leihstation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.entity.LeihstationMobiDataImportStatistik;
import de.wps.radvis.backend.leihstation.domain.entity.LeihstationMobidataWFSElement;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.ExterneLeihstationenId;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

class LeihstationMobiDataImportJobTest {

	@Mock
	private JobExecutionDescriptionRepository jobRepository;
	@Mock
	private LeihstationRepository leihstationRepository;
	@Mock
	private LeihstationMobiDataWFSRepository mobidataRepository;
	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;

	@Captor
	ArgumentCaptor<Leihstation> leihstationenCaptor;

	LeihstationMobiDataImportJob job;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		PreparedGeometry bawue = mock(PreparedGeometry.class);
		when(bawue.intersects(any())).thenReturn(true);
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(bawue);
		job = new LeihstationMobiDataImportJob(jobRepository, verwaltungseinheitService, leihstationRepository,
			mobidataRepository);
	}

	@Test
	void doRun() {
		// Arrange
		/*
		/ Alte Leistationen. Es gibt 4 stück.
		/ - Eine wird unverändert übernommen
		/ - Eine wird modifiziert
		/ - Zwei werden Gelöscht
		/ - Eine neue kommt hinzu
		 */
		Leihstation alteMobiBleibt1 = LeihstationTestDataProvider.defaultAusMobiData()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.anzahlFahrraeder(Anzahl.of(10))
			.externeId(ExterneLeihstationenId.of("bleibt1_unveraendert"))
			.build();
		Leihstation alteMobiBleibt2 = LeihstationTestDataProvider.defaultAusMobiData()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("bleibt2_modifiziert"))
			.anzahlFahrraeder(Anzahl.of(20))
			.build();
		Leihstation alteMobiFliegtraus1 = LeihstationTestDataProvider.defaultAusMobiData()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("fliegt1"))
			.anzahlFahrraeder(Anzahl.of(5))
			.build();
		Leihstation alteMobiFliegtraus2 = LeihstationTestDataProvider.defaultAusMobiData()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("fliegt2"))
			.anzahlFahrraeder(Anzahl.of(5))
			.build();

		when(leihstationRepository.findByExterneIdAndQuellSystem(ExterneLeihstationenId.of("bleibt1_unveraendert"),
			LeihstationQuellSystem.MOBIDATABW))
			.thenReturn(Optional.of(alteMobiBleibt1));
		when(leihstationRepository.findByExterneIdAndQuellSystem(ExterneLeihstationenId.of("bleibt2_modifiziert"),
			LeihstationQuellSystem.MOBIDATABW))
			.thenReturn(Optional.of(alteMobiBleibt2));
		when(leihstationRepository.findByExterneIdAndQuellSystem(ExterneLeihstationenId.of("fliegt1"),
			LeihstationQuellSystem.MOBIDATABW))
			.thenReturn(Optional.of(alteMobiFliegtraus1));
		when(leihstationRepository.findByExterneIdAndQuellSystem(ExterneLeihstationenId.of("fliegt2"),
			LeihstationQuellSystem.MOBIDATABW))
			.thenReturn(Optional.of(alteMobiFliegtraus2));

		// veränderte/neue
		Leihstation neuMobi = LeihstationTestDataProvider.defaultAusMobiData()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("neu"))
			.anzahlFahrraeder(Anzahl.of(30))
			.build();
		Leihstation veraenderMobi = LeihstationTestDataProvider.defaultAusMobiData()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("bleibt2_modifiziert"))
			.anzahlFahrraeder(Anzahl.of(21))
			.build();

		Stream<LeihstationMobidataWFSElement> wfsFeatures = Stream.of(
			leihstationZuDto(neuMobi),
			leihstationZuDto(alteMobiBleibt1),
			leihstationZuDto(veraenderMobi));
		when(mobidataRepository.readBikeStationFeatures()).thenReturn(wfsFeatures);

		when(leihstationRepository.deleteByExterneIdNotInAndQuellSystem(
			eq(Set.of(ExterneLeihstationenId.of("neu"), ExterneLeihstationenId.of("bleibt1_unveraendert"),
				ExterneLeihstationenId.of("bleibt2_modifiziert"))),
			eq(LeihstationQuellSystem.MOBIDATABW)))
			.thenReturn(2);

		// Act
		Optional<JobStatistik> statisticOpt = job.doRun();

		// Assert
		verify(leihstationRepository, times(3)).save(leihstationenCaptor.capture());
		assertThat(leihstationenCaptor.getAllValues()).extracting(
				leihstation -> leihstation.getExterneId().get().getValue(),
				leihstation -> leihstation.getAnzahlFahrraeder().get().getValue())
			.containsExactlyInAnyOrder(
				Tuple.tuple("neu", 30),
				Tuple.tuple("bleibt1_unveraendert", 10),
				Tuple.tuple("bleibt2_modifiziert", 21));

		LeihstationMobiDataImportStatistik statistic = (LeihstationMobiDataImportStatistik) statisticOpt.get();
		assertThat(statistic.anzahlGeloescht).isEqualTo(2);
		assertThat(statistic.anzahlGeupdated).isEqualTo(2);
		assertThat(statistic.anzahlNeuErstellt).isEqualTo(1);

	}

	private LeihstationMobidataWFSElement leihstationZuDto(Leihstation neuMobi) {
		LeihstationMobidataWFSElement neueLeihstation = new LeihstationMobidataWFSElement();
		neueLeihstation.setId(neuMobi.getExterneId().get());
		neueLeihstation.setAnzahlFahrraeder(neuMobi.getAnzahlFahrraeder().get().getValue());
		neueLeihstation.setPosition(neuMobi.getGeometrie());
		return neueLeihstation;
	}

}