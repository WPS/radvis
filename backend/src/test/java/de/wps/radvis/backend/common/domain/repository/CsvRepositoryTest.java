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

package de.wps.radvis.backend.common.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.exception.CsvReadException;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.CsvRepositoryImpl;

public class CsvRepositoryTest {
	CsvRepository csvRepository;

	@BeforeEach
	public void setup() {
		csvRepository = new CsvRepositoryImpl();
	}

	@Test
	void write_read() throws IOException, CsvReadException {
		// arrange
		Map<String, String> row1 = new HashMap<>();
		row1.put("key1", "value1row1");
		row1.put("key2", "value2row1");
		Map<String, String> row2 = new HashMap<>();
		row2.put("key1", "value1row2");
		row2.put("key2", "value2row2");
		List<Map<String, String>> rows = List.of(row1, row2);
		List<String> header = List.of("key1", "key2");
		CsvData csvData = CsvData.of(rows, header);

		// act
		byte[] csv = csvRepository.write(csvData);
		CsvData result = csvRepository.read(csv, csvData.getHeader());

		// assert
		assertThat(result.getHeader()).containsExactlyElementsOf(csvData.getHeader());
		assertThat(result.getRows()).containsExactlyElementsOf(csvData.getRows());
	}

	@Test
	void read_noHeader_Throws() {
		byte[] bytes = "value1;value2".getBytes();

		assertThrows(CsvReadException.class, () -> csvRepository.read(bytes, List.of("key1", "key2")));
	}

	@Test
	void read_headerMissing_Throws() {
		byte[] bytes = "key1;key2\nvalue1;value2".getBytes();

		assertThrows(CsvReadException.class, () -> csvRepository.read(bytes, List.of("key1", "key2", "key3")));
	}

	@Test
	void read_additionalHeaders_Ignored() throws CsvReadException {
		byte[] bytes = "key1;key2\nvalue1;value2".getBytes();

		List<String> requiredHeaders = List.of("key2");
		CsvData result = csvRepository.read(bytes, requiredHeaders);

		// assert
		Map<String, String> row1 = new HashMap<>();
		row1.put("key2", "value2");

		assertThat(result.getHeader()).containsExactlyElementsOf(requiredHeaders);
		assertThat(result.getRows()).containsExactly(row1);
	}

	@Test
	void read_wrongDelimiter_Throws() throws CsvReadException {
		byte[] bytes = "key1|key2\nvalue1|value2".getBytes();

		List<String> requiredHeaders = List.of("key1", "key2");
		assertThrows(CsvReadException.class, () -> csvRepository.read(bytes, requiredHeaders));
	}

	@Test
	void read_trimSpaces() throws CsvReadException {
		// arrange
		String csv = "key1;key2\n2 ; 100,3 ";

		List<String> requiredHeaders = List.of("key1", "key2");
		CsvData result = csvRepository.read(csv.getBytes(), requiredHeaders);

		// assert
		Map<String, String> row1 = new HashMap<>();
		row1.put("key1", "2");
		row1.put("key2", "100,3");

		assertThat(result.getHeader()).containsExactlyElementsOf(requiredHeaders);
		assertThat(result.getRows()).containsExactly(row1);
	}

	@Test
	void readCsv_trimEmptyLinesAtEnd() throws CsvReadException {
		// arrange
		String csv = "key1;key2\n 2; 100,3 \n   \n\n";

		List<String> requiredHeaders = List.of("key1", "key2");
		CsvData result = csvRepository.read(csv.getBytes(), requiredHeaders);

		// assert
		Map<String, String> row1 = new HashMap<>();
		row1.put("key1", "2");
		row1.put("key2", "100,3");

		assertThat(result.getHeader()).containsExactlyElementsOf(requiredHeaders);
		assertThat(result.getRows()).containsExactly(row1);
	}
}
