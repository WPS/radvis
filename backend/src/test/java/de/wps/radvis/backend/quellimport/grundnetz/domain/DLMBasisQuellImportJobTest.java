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

package de.wps.radvis.backend.quellimport.grundnetz.domain;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;

class DLMBasisQuellImportJobTest {
	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	ImportedFeaturePersistentRepository importedFeaturePersistentRepository;

	@Mock
	DLMWFSImportRepository dlmWFSImportRepository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testPartitionierung() {
		Envelope partitionLeftTop = new Envelope(0, 100, 100, 200);
		Envelope partitionRightTop = new Envelope(100, 200, 100, 200);
		Envelope partitionLeftBot = new Envelope(0, 100, 0, 100);
		Envelope partitionRightBot = new Envelope(100, 200, 0, 100);

		when(dlmWFSImportRepository.getPartitionen()).thenReturn(
			List.of(partitionLeftTop, partitionRightTop, partitionLeftBot, partitionRightBot));

		ImportedFeature strassenFeature1 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID1")
			.addAttribut("bezeichnung", "K8")
			.build();
		ImportedFeature strassenFeature2 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID2")
			.addAttribut("bezeichnung", "B8")
			.build();
		ImportedFeature strassenFeature3 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID3")
			.addAttribut("bezeichnung", "B8")
			.build();
		ImportedFeature strassenFeature4 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID4")
			.addAttribut("bezeichnung", "A8")
			.build();
		ImportedFeature strassenFeature5 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID5")
			.addAttribut("bezeichnung", "E52;A8")
			.build();

		ImportedFeature wegeFeature1 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID6")
			.build();
		ImportedFeature wegeFeature2 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID7")
			.build();
		ImportedFeature wegeFeature3 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID8")
			.build();
		ImportedFeature wegeFeature4 = ImportedFeatureTestDataProvider.defaultWFSObject().fachId("technischeID9")
			.build();

		when(dlmWFSImportRepository.readStrassenFeatures(partitionLeftTop))
			.thenReturn(Stream.of(strassenFeature1, strassenFeature2));
		when(dlmWFSImportRepository.readStrassenFeatures(partitionRightTop)).thenReturn(Stream.of(strassenFeature2));
		when(dlmWFSImportRepository.readStrassenFeatures(partitionLeftBot))
			.thenReturn(Stream.of(strassenFeature3, strassenFeature2));
		when(dlmWFSImportRepository.readStrassenFeatures(partitionRightBot))
			.thenReturn(Stream.of(strassenFeature4, strassenFeature3, strassenFeature5));

		when(dlmWFSImportRepository.readWegeFeatures(partitionLeftTop))
			.thenReturn(Stream.of(wegeFeature1, wegeFeature2));
		when(dlmWFSImportRepository.readWegeFeatures(partitionRightTop)).thenReturn(Stream.of(wegeFeature2));
		when(dlmWFSImportRepository.readWegeFeatures(partitionLeftBot))
			.thenReturn(Stream.of(wegeFeature3, wegeFeature2));
		when(dlmWFSImportRepository.readWegeFeatures(partitionRightBot))
			.thenReturn(Stream.of(wegeFeature4, wegeFeature3));

		DLMBasisQuellImportJob job = new DLMBasisQuellImportJob(
			jobExecutionDescriptionRepository,
			importedFeaturePersistentRepository, dlmWFSImportRepository);

		// act
		job.doRun();

		verify(importedFeaturePersistentRepository, times(1)).save(same(strassenFeature1));
		verify(importedFeaturePersistentRepository, times(1)).save(same(strassenFeature2));
		verify(importedFeaturePersistentRepository, times(1)).save(same(strassenFeature3));
		verify(importedFeaturePersistentRepository, times(0)).save(
			same(strassenFeature4)); // Autobahn Features sollen nicht importiert werden
		verify(importedFeaturePersistentRepository, times(0)).save(
			same(strassenFeature5)); // Autobahn Features sollen nicht importiert werden
		verify(importedFeaturePersistentRepository, times(1)).save(same(wegeFeature1));
		verify(importedFeaturePersistentRepository, times(1)).save(same(wegeFeature2));
		verify(importedFeaturePersistentRepository, times(1)).save(same(wegeFeature3));
		verify(importedFeaturePersistentRepository, times(1)).save(same(wegeFeature4));
	}
}
