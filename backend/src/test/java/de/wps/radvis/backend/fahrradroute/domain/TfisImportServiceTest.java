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

package de.wps.radvis.backend.fahrradroute.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.opengis.feature.simple.SimpleFeature;

import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;

class TfisImportServiceTest {

	@Test
	void testGroupingByObjid() {
		// arrange
		SimpleFeature a = SimpleFeatureTestDataProvider.withAttributes(Map.of("objid", "id1", "testid", "a"));
		SimpleFeature b = SimpleFeatureTestDataProvider.withAttributes(Map.of("objid", "id1", "testid", "b"));
		SimpleFeature c = SimpleFeatureTestDataProvider.withAttributes(Map.of("objid", "id2", "testid", "c"));
		SimpleFeature d = SimpleFeatureTestDataProvider.withAttributes(Map.of("objid", "id2", "testid", "d"));
		SimpleFeature e = SimpleFeatureTestDataProvider.withAttributes(Map.of("objid", "id3", "testid", "e"));
		Stream<SimpleFeature> featureStream = Stream.of(a, b, c, d, e);

		// act
		Map<String, List<SimpleFeature>> collect = featureStream.collect(TfisImportService.groupingByObjid());

		// assert
		assertThat(collect).hasSize(3);

		assertThat(collect).contains(
			entry("id1", List.of(a, b)),
			entry("id2", List.of(c, d)),
			entry("id3", List.of(e)));
	}

}