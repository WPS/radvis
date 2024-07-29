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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeTransformationException;
import de.wps.radvis.backend.shapetransformation.domain.valueObject.TransformationsKonfiguration;

class TransformationsKonfigurationsRepositoryTest {

	private TransformationsKonfigurationsRepositoryImpl transformationsKonfigurationsRepositoryImpl;

	@BeforeEach
	void setup() {
		this.transformationsKonfigurationsRepositoryImpl = new TransformationsKonfigurationsRepositoryImpl();
	}

	private final Stream<String> delimiters = Stream.of(",", ";", "|");

	private final List<Boolean> withOuotes = List.of(true, false);

	private final List<Charset> charsets = List.of(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8);

	@TestFactory
	public Stream<DynamicTest> readCsv() {
		return delimiters.flatMap(delimiter -> charsets.stream().flatMap(charset -> withOuotes.stream().map(quotes -> {
			String quote = quotes ? "\"" : "";
			return DynamicTest.dynamicTest(
				"Read csv (encoding: " + charset.displayName() + ") with delimiter '" + delimiter + "' and with"
					+ (quotes ? "" : "out") + " quotes",
				() -> {
					String csv = quote + String.join(quote + delimiter + quote,
						TransformationsKonfigurationsRepositoryImpl.HEADER)
						+ quote + "\n" +
						quote + String.join(quote + delimiter + quote, "ä", "b", "c", "d") + quote + "\n"
						+
						quote + String.join(quote + delimiter + quote, "ä", "b", "e", "f") + quote + "\n";

					// act

					TransformationsKonfiguration konfiguration = transformationsKonfigurationsRepositoryImpl
						.readKonfigurationFromCsv(csv.getBytes(charset));

					// assert
					assertThat(
						konfiguration.hasQuellAttributName(
							TransformationsKonfigurationsRepositoryImpl.HEADER[0]))
								.isFalse();

					assertThat(konfiguration.hasQuellAttributName("ä")).isTrue();
					assertThat(konfiguration.getZielAttributName("ä")).isEqualTo("b");

					assertThat(konfiguration.hasQuellAttributWert("ä", "c")).isTrue();
					assertThat(konfiguration.getZielAttributWert("ä", "c")).isEqualTo("d");

					assertThat(konfiguration.hasQuellAttributWert("ä", "e")).isTrue();
					assertThat(konfiguration.getZielAttributWert("ä", "e")).isEqualTo("f");
				});
		})));
	}

	@Test
	void readCsv_inputEnthaeltZuVieleSpalten_throws() {
		// arrange
		String csv = String.join(",", TransformationsKonfigurationsRepositoryImpl.HEADER) + "\n" +
			"a,b,c,d\n" +
			"a,b,e,f,g";

		// act & assert
		assertThrows(ShapeTransformationException.class,
			() -> transformationsKonfigurationsRepositoryImpl.readKonfigurationFromCsv(csv.getBytes()));
	}

	@Test
	void readCsv_inputEnthaeltZuWenigeSpalten_throws() {
		// arrange
		String csv = String.join(",", TransformationsKonfigurationsRepositoryImpl.HEADER) + "\n" +
			"a,b,c,d\n" +
			"a,b,e";

		// act & assert
		assertThrows(ShapeTransformationException.class,
			() -> transformationsKonfigurationsRepositoryImpl.readKonfigurationFromCsv(csv.getBytes()));
	}

	@Test
	void readCsv_inputKorrektOhneKopfzeile_throws() {
		// arrange
		String csv = "a,b,c,d\n" +
			"a,b,e,f";

		// act

		// act & assert
		assertThrows(ShapeTransformationException.class,
			() -> transformationsKonfigurationsRepositoryImpl.readKonfigurationFromCsv(csv.getBytes()));
	}

	@Test
	void readCsv_forbiddenDelimiter_throwsReadableException() {
		String csv = "a,b\"";

		// act + assert
		assertThatThrownBy(() -> {
			transformationsKonfigurationsRepositoryImpl.readKonfigurationFromCsv(csv.getBytes());
		}).isInstanceOf(ShapeTransformationException.class)
			.hasMessage("Die Konfigurationsdatei kann nicht eingelesen werden");
	}

	@Test
	void readCsv_onlyFirstHeaderKey_throwsReadableException() {
		String csv = "Quell-Attribut-Name";

		// act + assert
		assertThatThrownBy(() -> {
			transformationsKonfigurationsRepositoryImpl.readKonfigurationFromCsv(csv.getBytes());
		}).isInstanceOf(ShapeTransformationException.class)
			.hasMessage(
				"Die Konfigurationsdatei muss als erste Zeile die Überschrift enthalten: Quell-Attribut-Name, Ziel-Attribut-Name, Quell-Attribut-Wert, Ziel-Attribut-Wert");
	}

	@Test
	void readCsv_inputKorrektGleicherQuellUndZielWertBeiVerschiedenenAttributen_wirdKorrektGeparsed() {
		// arrange
		String csv = String.join(",", TransformationsKonfigurationsRepositoryImpl.HEADER) + "\n" +
			"a,b,c,d\n" +
			"g,h,c,d";

		// act
		TransformationsKonfiguration konfiguration = transformationsKonfigurationsRepositoryImpl
			.readKonfigurationFromCsv(csv.getBytes());

		// assert
		assertThat(konfiguration.hasQuellAttributName("a")).isTrue();
		assertThat(konfiguration.getZielAttributName("a")).isEqualTo("b");

		assertThat(konfiguration.hasQuellAttributWert("a", "c")).isTrue();
		assertThat(konfiguration.getZielAttributWert("a", "c")).isEqualTo("d");

		assertThat(konfiguration.hasQuellAttributName("g")).isTrue();
		assertThat(konfiguration.getZielAttributName("g")).isEqualTo("h");

		assertThat(konfiguration.hasQuellAttributWert("g", "c")).isTrue();
		assertThat(konfiguration.getZielAttributWert("g", "c")).isEqualTo("d");
	}

	@Test
	void readCsv_quellNameMitVerschiedenenZielNamen_throws() {
		// arrange
		String csv = String.join(",", TransformationsKonfigurationsRepositoryImpl.HEADER) + "\n" +
			"a,b,c,d\n" +
			"a,h,e,f";

		// act & assert
		assertThrows(ShapeTransformationException.class,
			() -> transformationsKonfigurationsRepositoryImpl.readKonfigurationFromCsv(csv.getBytes()));
	}

	@Test
	void readCsv_quellWertMitVerschiedenenZielWerten_throws() {
		// arrange
		String csv = String.join(",", TransformationsKonfigurationsRepositoryImpl.HEADER) + "\n" +
			"a,b,c,d\n" +
			"a,b,c,f";

		// act & assert

		assertThrows(ShapeTransformationException.class,
			() -> transformationsKonfigurationsRepositoryImpl.readKonfigurationFromCsv(csv.getBytes()));
	}

	@Test
	void readCsv_nurAttributUmbenennung() {
		// arrange
		String csv = String.join(",", TransformationsKonfigurationsRepositoryImpl.HEADER) + "\n" +
			"a,b,,\n" +
			"g,h,,";

		// act
		TransformationsKonfiguration konfiguration = transformationsKonfigurationsRepositoryImpl
			.readKonfigurationFromCsv(csv.getBytes());

		// assert
		assertThat(konfiguration.hasQuellAttributName("a")).isTrue();
		assertThat(konfiguration.getZielAttributName("a")).isEqualTo("b");

		assertThat(konfiguration.hasQuellAttributWert("a", "")).isTrue();
		assertThat(konfiguration.getZielAttributWert("a", "")).isEqualTo("");

		assertThat(konfiguration.hasQuellAttributName("g")).isTrue();
		assertThat(konfiguration.getZielAttributName("g")).isEqualTo("h");

		assertThat(konfiguration.hasQuellAttributWert("g", "")).isTrue();
		assertThat(konfiguration.getZielAttributWert("g", "")).isEqualTo("");
	}

	@Test
	void readCsv_trimSpaces() {
		//arrange
		String csv = String.join(",", TransformationsKonfigurationsRepositoryImpl.HEADER) + "\n" +
			"breite , breite, 2, 100 \n";

		// act
		TransformationsKonfiguration konfiguration = transformationsKonfigurationsRepositoryImpl
			.readKonfigurationFromCsv(csv.getBytes());

		//assert
		assertThat(konfiguration.hasQuellAttributName("breite")).isTrue();
		assertThat(konfiguration.getZielAttributName("breite")).isEqualTo("breite");

		assertThat(konfiguration.hasQuellAttributWert("breite", "2")).isTrue();
		assertThat(konfiguration.getZielAttributWert("breite", "2")).isEqualTo("100");
	}

	@Test
	void readCsv_trimEmptyLinesAtEnd() {
		//arrange
		String csv = String.join(",", TransformationsKonfigurationsRepositoryImpl.HEADER) + "\n" +
			"breite , breite, 2, 100 \n" + "   \n" + "\n";

		// act
		TransformationsKonfiguration konfiguration = transformationsKonfigurationsRepositoryImpl
			.readKonfigurationFromCsv(csv.getBytes());

		//assert
		assertThat(konfiguration.hasQuellAttributName("breite")).isTrue();
		assertThat(konfiguration.getZielAttributName("breite")).isEqualTo("breite");

		assertThat(konfiguration.hasQuellAttributWert("breite", "2")).isTrue();
		assertThat(konfiguration.getZielAttributWert("breite", "2")).isEqualTo("100");
	}
}