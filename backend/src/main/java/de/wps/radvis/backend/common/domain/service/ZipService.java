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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.common.domain.exception.ZipFileExtractException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZipService {

	public File createTemporaryZipCopy(String tempFilePrefix, MultipartFile file) throws IOException {
		File tempZipCopy = Files
			.createTempFile(tempFilePrefix, ".zip")
			.toFile();

		tempZipCopy.deleteOnExit();

		file.transferTo(tempZipCopy);

		return tempZipCopy;
	}

	public File unzip(String tempDirectoryPrefix, File tempZipFile)
		throws IOException, ZipFileExtractException {
		if (tempZipFile == null) {
			throw new ZipFileExtractException("Das gegebene ZIP-File konnte nicht gelesen werden");
		}
		
		File tempDir = this.createTempDirectory(tempDirectoryPrefix);

		log.info("ZipFile {} wird nach {} entpackt...", tempZipFile.getName(), tempDir.getName());
		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(tempZipFile))) {
			ZipEntry entry;
			while ((entry = zipIn.getNextEntry()) != null) {
				File tempFile = this.createTempFile(tempDir, entry.getName());
				if (!entry.isDirectory()) {
					// Wenn der Eintrag kein Verzeichnis ist, extrahiere ihn
					this.extractFile(zipIn, tempFile);
				} else {
					// Wenn der Eintrag ein Verzeichnis ist, erstelle es
					tempFile.mkdirs();
				}
				zipIn.closeEntry();
			}
		} catch (IOException e) {
			log.error("ZipFile konnte nicht entpackt werden: ", e);
		}

		log.info("ZipFile nach {} entpackt", tempDir.getAbsolutePath());
		return tempDir;
	}

	private void extractFile(ZipInputStream zipIn, File targetFile) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));
		byte[] buffer = new byte[4096];
		int read = 0;
		while ((read = zipIn.read(buffer)) != -1) {
			bos.write(buffer, 0, read);
		}
		bos.close();
	}

	private File createTempDirectory(String tempDirectoryPrefix) throws IOException {
		File tempDir = Files
			.createTempDirectory(tempDirectoryPrefix)
			.toFile();

		tempDir.deleteOnExit();
		return tempDir;
	}

	private File createTempFile(File destinationDir, String fileName) throws IOException {
		File file = new File(destinationDir, fileName);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		file.deleteOnExit();
		return file;
	}
}
