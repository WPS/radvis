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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.GeometryTestdataProvider;

class ExportDataTest {

	@Test
	void create_defaultHeaders() {
		// arrange
		Map<String, String> properties = new HashMap<>();

		properties.put("Header 1", null);
		properties.put("Header 2", null);

		// act
		ExportData exportData = new ExportData(GeometryTestdataProvider.createLineString(), properties);

		// assert
		assertThat(exportData.getHeaders()).containsExactlyInAnyOrder("Header 1", "Header 2");
	}

	@Test
	void removeField() {
		// arrange
		Map<String, String> properties = new HashMap<>();

		properties.put("Include This", null);
		properties.put("Exclude This", null);

		ExportData exportData = new ExportData(GeometryTestdataProvider.createLineString(), properties);

		// act
		exportData.removeField("Exclude This");

		// assert
		assertThat(exportData.getHeaders()).containsExactly("Include This");
		assertThat(exportData.getProperties()).hasSize(1);
		assertThat(exportData.getProperties().containsKey("Exclude This")).isFalse();
	}

	@Test
	void removeField_immutableHeaderList() {
		// arrange
		Map<String, String> properties = new HashMap<>();

		properties.put("Include This", null);
		properties.put("Exclude This", null);

		ExportData exportData = new ExportData(GeometryTestdataProvider.createLineString(), properties,
			List.of("Include This", "Exclude This"));

		// act
		exportData.removeField("Exclude This");

		// assert
		assertThat(exportData.getHeaders()).containsExactly("Include This");
		assertThat(exportData.getProperties()).hasSize(1);
		assertThat(exportData.getProperties().containsKey("Exclude This")).isFalse();
	}

}
