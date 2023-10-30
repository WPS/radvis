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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

class CsvDataTest {
	@Test
	void rowsEnthaltenGenauHeader() {
		List<Map<String, String>> rows = List.of(
			Map.of("h1", "v1",
				"h2", "v2",
				"h3", "v3"));
		List<String> header = List.of("h1", "h2", "h3");

		assertDoesNotThrow(() -> CsvData.of(rows, header));
	}

	@Test
	void rowsEnthaltenGenauHeader_1headerUnterschiedl() {
		List<Map<String, String>> rows = List.of(
			Map.of("h1", "v1",
				"h2", "v2",
				"h4", "v3"));
		List<String> header = List.of("h1", "h2", "h3");

		assertThrows(RequireViolation.class, () -> CsvData.of(rows, header));
	}

	@Test
	void rowsEnthaltenGenauHeader_1headerZuWenig() {
		List<Map<String, String>> rows = List.of(
			Map.of("h1", "v1",
				"h2", "v2"));
		List<String> header = List.of("h1", "h2", "h3");

		assertThrows(RequireViolation.class, () -> CsvData.of(rows, header));
	}

	@Test
	void rowsEnthaltenGenauHeader_1headerZuViel() {
		List<Map<String, String>> rows = List.of(
			Map.of("h1", "v1",
				"h2", "v2",
				"h3", "v3",
				"h4", "v4")

		);
		List<String> header = List.of("h1", "h2", "h3");

		assertThrows(RequireViolation.class, () -> CsvData.of(rows, header));
	}

	@Test
	void rowsEnthaltenGenauHeader_zweiteZeileFalsch() {
		List<Map<String, String>> rows = List.of(
			Map.of("h1", "v1",
				"h2", "v2",
				"h3", "v3"),
			Map.of("h1", "v1",
				"h2", "v2",
				"h3", "v3",
				"h4", "v3"));
		List<String> header = List.of("h1", "h2", "h3");

		assertThrows(RequireViolation.class, () -> CsvData.of(rows, header));
	}
}