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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import de.wps.radvis.backend.common.domain.exception.ZipFileExtractException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZipServiceTest {

	private ZipService service;

	@BeforeEach
	public void setup() {
		service = new ZipService();
	}

	@Test
	public void testUnzip_noFilter_extractsAllContent() throws IOException, ZipFileExtractException {
		// arrange
		File testZip = new ClassPathResource("massnahmen-dateianhaenge-test.zip").getFile();

		// act
		File unzippedFile = service.unzip("testunzip", testZip);

		// assert
		assertThat(unzippedFile).isDirectory();
		assertThat(unzippedFile).exists();
		File[] filesAufHauptebene = unzippedFile.listFiles();
		assertThat(filesAufHauptebene.length).isEqualTo(1);
		File[] filesAufZweiterEbene = filesAufHauptebene[0].listFiles();
		assertThat(filesAufZweiterEbene.length).isEqualTo(5);
		List<String> pathList = Files.walk(unzippedFile.toPath(), 10)
			.map(p -> p.toString().replace(unzippedFile.getAbsolutePath(), ""))
			.toList();
		assertThat(pathList.size()).isEqualTo(14);
	}

}
