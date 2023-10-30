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

package de.wps.radvis.backend.shapetransformation.schnittstelle.repositoryImpl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import com.opencsv.exceptions.CsvException;

import de.wps.radvis.backend.shapetransformation.domain.TransformationsKonfigurationsRepository;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeTransformationException;
import de.wps.radvis.backend.shapetransformation.domain.valueObject.AttributTransformation;
import de.wps.radvis.backend.shapetransformation.domain.valueObject.TransformationsKonfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class TransformationsKonfigurationsRepositoryImpl implements TransformationsKonfigurationsRepository {
	static String[] HEADER = new String[] { "Quell-Attribut-Name", "Ziel-Attribut-Name", "Quell-Attribut-Wert",
		"Ziel-Attribut-Wert" };

	@Override
	public TransformationsKonfiguration readKonfigurationFromCsv(byte[] data) {
		List<AttributTranformationCsvZeile> zeilen = readCsv(data);

		return new TransformationsKonfiguration(parseTransformations(zeilen));
	}

	private List<AttributTranformationCsvZeile> readCsv(byte[] data) {
		List<AttributTranformationCsvZeile> zeilen = new ArrayList<>();
		List<String[]> lines;

		Charset charset = findCharset(data);
		char delimiter = findDelimiter(data, charset);

		try (InputStreamReader reader = new InputStreamReader(new BOMInputStream(new ByteArrayInputStream(data)),
			charset);
			CSVReader csvReader = new CSVReaderBuilder(reader)
				.withCSVParser(
					new CSVParserBuilder()
						.withSeparator(delimiter)
						.build())
				.build()) {
			lines = csvReader.readAll();
		} catch (IOException | UnsupportedOperationException | CsvException e) {
			throw new ShapeTransformationException("Die Konfigurationsdatei kann nicht eingelesen werden");
		}

		if (!Arrays.equals(lines.get(0), HEADER)) {
			throw new ShapeTransformationException(
				"Die Konfigurationsdatei muss als erste Zeile die Überschrift enthalten: "
					+ String.join(", ", HEADER));
		}

		for (int i = lines.size() - 1; i >= 0; i--) {
			String[] line = lines.get(i);
			if (line.length == 0 || line[0].isBlank()) {
				lines.remove(i);
			} else {
				break;
			}
		}

		for (int i = 1; i < lines.size(); i++) {
			String[] values = Arrays.stream(lines.get(i)).map(String::trim)
				.toArray(String[]::new);

			if (values.length != 4) {
				throw new ShapeTransformationException("Zeile Nr. " + (i + 1) + " hat nicht genau 4 Spalten.");
			}
			zeilen.add(new AttributTranformationCsvZeile(values[0], values[1], values[2],
				values[3]));
		}
		return zeilen;
	}

	private static BufferedReader createBufferedReader(byte[] data, Charset charset) {
		return new BufferedReader(new InputStreamReader(new BOMInputStream(new ByteArrayInputStream(data)), charset));
	}

	private Charset findCharset(byte[] data) {

		try (BufferedReader reader = createBufferedReader(data, StandardCharsets.UTF_8)) {
			String content = reader.lines().collect(Collectors.joining(""));
			if (content.contains("�")) {
				throw new IOException();
			}
			return StandardCharsets.UTF_8;
		} catch (IOException e) {
			try (BufferedReader reader = createBufferedReader(data, StandardCharsets.ISO_8859_1)) {
				reader.lines().collect(Collectors.toList());
				return StandardCharsets.ISO_8859_1;
			} catch (IOException ioException) {
				throw new ShapeTransformationException(
					"Ungültiges Encoding der Konfigurationsdatei. Erlaubte Encodings: "
						+ StandardCharsets.ISO_8859_1.displayName() + ", " + StandardCharsets.UTF_8.displayName());
			}
		}

	}

	private char findDelimiter(byte[] data, Charset charset) {
		try (BufferedReader reader = createBufferedReader(data, charset)) {
			String firstLine = reader.readLine();
			if (firstLine == null) {
				throw new ShapeTransformationException("Die Konfigurationsdatei enthält keine Zeilen.");
			}
			int firstHeaderFieldlength = HEADER[0].length();
			char delimiter = ';';
			if (firstLine.startsWith("\"") && firstLine.length() >= firstHeaderFieldlength + 2) {
				delimiter = firstLine.charAt(firstHeaderFieldlength + 2);
			} else if (firstLine.length() > firstHeaderFieldlength) {
				delimiter = firstLine.charAt(firstHeaderFieldlength);
			}
			return delimiter;
		} catch (IOException e) {
			throw new ShapeTransformationException("Die Konfigurationsdatei kann nicht eingelesen werden");
		}
	}

	private Map<String, AttributTransformation> parseTransformations(List<AttributTranformationCsvZeile> zeilen) {
		Map<String, List<AttributTranformationCsvZeile>> attributNameToZeilen = zeilen.stream()
			.collect(Collectors.groupingBy(AttributTranformationCsvZeile::getQuellAttributName));

		Map<String, AttributTransformation> attributTransformationen = new HashMap<>();

		attributNameToZeilen.keySet().forEach(quellName -> {
			List<AttributTranformationCsvZeile> abbildung = attributNameToZeilen.get(quellName);
			if (!abbildung.stream()
				.allMatch(abb -> abb.getZielAttributName().equals(abbildung.get(0).getZielAttributName()))) {
				throw new ShapeTransformationException("Für denselben Quell-Attribut-Namen '" + quellName
					+ "' gibt es mehrere Ziel-Attribut-Namen. Dadurch ist nicht eindeutig, auf welchen Ziel-Attribut-Namen transformiert werden soll");
			}

			Map<String, String> quellToZielAttributwert = new HashMap<>();

			for (AttributTranformationCsvZeile abb : abbildung) {
				if (quellToZielAttributwert.containsKey(abb.getQuellAttributWert())) {
					throw new ShapeTransformationException(
						"Für denselben Quell-Attribut-Wert '" + abb.getQuellAttributWert()
							+ "' für das Quell-Attribut'" + quellName
							+ "' gibt es mehrere Ziel-Attribut-Werte. Dadurch ist nicht eindeutig, auf welchen Ziel-Attribut-Wert transformiert werden soll");
				}
				quellToZielAttributwert.put(abb.getQuellAttributWert(), abb.getZielAttributWert());
			}

			attributTransformationen.put(quellName,
				new AttributTransformation(abbildung.get(0).getZielAttributName(), quellToZielAttributwert));
		});
		return attributTransformationen;
	}

	@AllArgsConstructor
	@Getter
	private class AttributTranformationCsvZeile {

		private String quellAttributName;

		private String zielAttributName;

		private String quellAttributWert;

		private String zielAttributWert;

	}
}
