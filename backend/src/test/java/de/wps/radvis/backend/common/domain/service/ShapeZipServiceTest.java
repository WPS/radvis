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

package de.wps.radvis.backend.common.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import de.wps.radvis.backend.common.domain.exception.ZipFileRequiredFilesMissingException;

public class ShapeZipServiceTest {

	private ShapeZipService shapeZipService;

	@BeforeEach
	public void setup() {
		shapeZipService = new ShapeZipService();
	}

	@Test
	public void testGetShapeFileFromDirectory() throws IOException {
		// arrange
		File shpDirectory = new ClassPathResource("shp/test_attribute").getFile();

		// act
		Optional<File> shapeFile = shapeZipService.getShapeFileFromDirectory(shpDirectory);

		// assert
		assertThat(shapeFile).isPresent();
	}

	@Test
	public void testUnzipAndZip() throws IOException, ZipFileRequiredFilesMissingException {
		// arrange
		ClassPathResource zipFile = new ClassPathResource("shp/radwegedb_bodenseekreis.zip");
		byte[] bytes = Files.readAllBytes(zipFile.getFile().toPath());

		// act
		File file = shapeZipService.unzip(bytes);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		shapeZipService.zip(stream, file);
		stream.close();

		// assert
		try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(stream.toByteArray()))) {
			zipInputStream.read();

			List<String> entries = new ArrayList<>();

			ZipEntry nextEntry = zipInputStream.getNextEntry();
			while (nextEntry != null) {
				entries.add(nextEntry.getName());
				nextEntry = zipInputStream.getNextEntry();
			}

			assertThat(entries)
				.containsExactlyInAnyOrder("radwegedb_bodenseekreis2.cpg", "radwegedb_bodenseekreis2.dbf",
					"radwegedb_bodenseekreis2.prj", "radwegedb_bodenseekreis2.shp", "radwegedb_bodenseekreis2.shx");
		}
	}
}
