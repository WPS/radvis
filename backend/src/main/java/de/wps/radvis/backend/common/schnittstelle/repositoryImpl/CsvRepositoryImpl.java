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

package de.wps.radvis.backend.common.schnittstelle.repositoryImpl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.input.BOMInputStream;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import de.wps.radvis.backend.common.domain.CSVEncodingUtility;
import de.wps.radvis.backend.common.domain.exception.CsvReadException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvRepositoryImpl implements CsvRepository {

	@Override
	public CsvData read(byte[] csvFile, List<String> requiredHeaders) throws CsvReadException {
		return this.read(csvFile, requiredHeaders, ';', false);
	}

	@Override
	public CsvData read(byte[] csvFileData, List<String> requiredHeaders, char delimiter, boolean ignoreQuotations)
		throws CsvReadException {
		List<String[]> lines;

		Charset charset = findCharset(csvFileData);

		BOMInputStream.Builder inputStreamBuilder = BOMInputStream
			.builder()
			.setInputStream(new ByteArrayInputStream(csvFileData));

		try (InputStreamReader reader = new InputStreamReader(inputStreamBuilder.get(), charset);
			CSVReader csvReader = new CSVReaderBuilder(reader)
				.withCSVParser(
					new CSVParserBuilder()
						.withSeparator(delimiter)
						.withIgnoreQuotations(ignoreQuotations)
						.build())
				.build()) {
			lines = csvReader.readAll();
		} catch (IOException | UnsupportedOperationException | CsvException e) {
			String message = "Die CSV-Datei kann nicht eingelesen werden";
			log.error(message, e);
			throw new CsvReadException(message, e);
		}

		List<String> headersFromFile = Arrays.asList(lines.get(0));
		if (!headersFromFile.containsAll(requiredHeaders)) {
			throw new CsvReadException(
				"Die Csv-Datei muss als erste Zeile die Überschrift enthalten. Die Spalten müssen durch " + delimiter
					+ " getrennt sein.\n"
					+ "Expected: " + String.join(delimiter + " ", requiredHeaders) + "\n"
					+ "Found:    " + headersFromFile.stream().collect(Collectors.joining(delimiter + " ")));
		}

		List<Map<String, String>> rows = new ArrayList<>();
		int anzahlUebersprungenerZeilen = 0;
		for (int i = 1; i < lines.size(); i++) {
			String[] row = lines.get(i);

			// Wir ueberspringen Zeilen, die nicht zum Header in der Datei passen
			// (z.B. zusaetzliche oder fehlende Semikolons)
			if (row.length != headersFromFile.size()) {
				log.warn("Zeile " + i
					+ " hat nicht die gleiche Anzahl Spalten wie die Headerzeile. Zeile wird uebersprungen.");
				log.warn("CsvZeile: " + row.toString());
				anzahlUebersprungenerZeilen++;
				continue;
			}

			Map<String, String> properties = new HashMap<>();
			for (int j = 0; j < row.length; j++) {
				String value = row[j];

				if (requiredHeaders.contains(headersFromFile.get(j))) {
					properties.put(headersFromFile.get(j), value.trim());
				}
			}
			rows.add(properties);
		}
		return CsvData.of(rows, requiredHeaders, anzahlUebersprungenerZeilen);
	}

	@Override
	public byte[] write(CsvData csvData) throws IOException {
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
			OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
			CSVWriter writer = new CSVWriter(streamWriter, ';', '"', '"', "\n")) {

			CSVEncodingUtility.writeBOMEncoding(stream);

			String[] header = csvData.getHeader().toArray(String[]::new);
			writer.writeNext(header);

			csvData.getRows().forEach(row -> {
				String[] nextLine = new String[header.length];
				for (int i = 0; i < header.length; i++) {
					nextLine[i] = row.get(header[i]);
				}
				writer.writeNext(nextLine);
			});

			streamWriter.flush();
			return stream.toByteArray();
		}
	}

	private static BufferedReader createBufferedReader(byte[] data, Charset charset) throws IOException {
		BOMInputStream.Builder inputStreamBuilder = BOMInputStream
			.builder()
			.setInputStream(new ByteArrayInputStream(data));
		return new BufferedReader(new InputStreamReader(inputStreamBuilder.get(), charset));
	}

	private Charset findCharset(byte[] data) throws CsvReadException {
		try (BufferedReader reader = createBufferedReader(data, StandardCharsets.UTF_8)) {
			String content = reader.lines().collect(Collectors.joining(""));
			if (content.contains("�")) {
				throw new IOException("Data contains invalid characters according to UTF-8 encoding");
			}
			return StandardCharsets.UTF_8;
		} catch (IOException e) {
			log.warn("Lesen von CSV-Daten mit {} Encoding fehlgeschlagen. Versuche {}",
				StandardCharsets.UTF_8.displayName(), StandardCharsets.ISO_8859_1.displayName(), e);
			try (BufferedReader reader = createBufferedReader(data, StandardCharsets.ISO_8859_1)) {
				reader.lines().collect(Collectors.toList());
				return StandardCharsets.ISO_8859_1;
			} catch (IOException ioException) {
				log.error("CSV-Daten sind nicht nach {} und nicht nach {} kodiert. Breche ab.",
					StandardCharsets.UTF_8.displayName(), StandardCharsets.ISO_8859_1.displayName(), ioException);
				throw new CsvReadException(
					"Ungültiges Encoding der CSV-Datei. Erlaubte Encodings: "
						+ StandardCharsets.ISO_8859_1.displayName() + ", "
						+ StandardCharsets.UTF_8.displayName(),
					ioException);
			}
		}
	}
}
