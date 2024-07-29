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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.FileUtils;

import de.wps.radvis.backend.common.domain.exception.ShapeZipInvalidException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShapeZipService {

	private static final Set<String> SUPPORTED_FILEENDINGS = Set.of(".shp", ".shx", ".dbf", ".prj", ".cpg", ".xml",
		".sbn", ".sbx");
	private static final Set<String> REQUIRED_FILEENDINGS = Set.of(".shp", ".dbf", ".prj", ".shx");

	public File unzip(byte[] zipfileContent)
		throws IOException, ShapeZipInvalidException {
		File shpDirectory = Files
			.createTempDirectory("manueller_import")
			.toFile();
		shpDirectory.deleteOnExit();
		try {
			unzip(zipfileContent, shpDirectory);
			validateDirectoryHasRequiredFiles(shpDirectory);
		} catch (Exception e) {
			deleteUploadedFiles(shpDirectory);
			throw e;
		}
		log.info("ZipFile nach {} entpackt", shpDirectory.getAbsolutePath());

		return shpDirectory;
	}

	public void deleteUploadedFiles(File destShpDirectory) {
		try {
			FileUtils.deleteDirectory(destShpDirectory);
			log.info("Das temporäre Verzeichnis {} wurde gelöscht", destShpDirectory.getAbsolutePath());
		} catch (Exception e) {
			if (destShpDirectory != null) {
				log.error("Das Temp Verzeichnis {} konnte nicht gelöscht werden", destShpDirectory.getAbsolutePath(),
					e);
			} else {
				log.error("Das Temp Verzeichnis konnte nicht gelöscht werden, weil es nicht existiert");
			}
		}
	}

	public void unzip(byte[] zipfileContent, File destShpDirectory) throws IOException, ShapeZipInvalidException {
		ZipFile.Builder zipFileBuilder = ZipFile
			.builder()
			.setSeekableByteChannel(new SeekableInMemoryByteChannel(zipfileContent));

		try (ZipFile zipFile = zipFileBuilder.get()) {
			Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries();
			while (enumeration.hasMoreElements()) {
				ZipArchiveEntry zipEntry = enumeration.nextElement();
				if (zipEntry.isDirectory() || zipEntry.getName().contains("/")) {
					// Ordner in Zip werden samt enthaltener Files ignoriert
					continue;
				}

				if (!shouldExtractZipEntry(zipEntry.getName())) {
					// Dateien ohne Shapefile-Bezug werden ignoriert
					continue;
				}

				if (zipEntry.getName().contains("?")) {
					throw new ShapeZipInvalidException(
						"Dateinamen innerhalb des Zip-Archivs dürfen keine Umlaute oder Sonderzeichen enthalten.");
				}

				File newFile = newFile(destShpDirectory, zipEntry);
				newFile.deleteOnExit();

				// write file content
				try (FileOutputStream fos = new FileOutputStream(newFile)) {
					zipFile.getInputStream(zipEntry).transferTo(fos);
				}
			}
		}
	}

	public Optional<File> getShapeFileFromDirectory(File unzippedDir) throws IOException {
		try (Stream<Path> stream = Files.list(Paths.get(unzippedDir.getAbsolutePath()))) {
			return stream.filter(p -> p.toString().endsWith(".shp"))
				.findAny()
				.map(Path::toFile);
		}
	}

	private boolean shouldExtractZipEntry(String zipEntryName) {
		return SUPPORTED_FILEENDINGS.stream().anyMatch(zipEntryName::endsWith);
	}

	private void validateDirectoryHasRequiredFiles(File shpDirectory)
		throws ShapeZipInvalidException, IOException {

		long anzahlDateien = Files.list(Paths.get(shpDirectory.getAbsolutePath())).count();
		if (anzahlDateien == 0) {
			throw new ShapeZipInvalidException(REQUIRED_FILEENDINGS,
				"Es ist entweder keine dieser Dateien vorhanden, oder sie befinden sich fälschlicherweise in einem Unterordner.");
		}

		List<String> missingFiles = new ArrayList<>();
		List<String> multibleFiles = new ArrayList<>();
		for (String requiredFileEnding : REQUIRED_FILEENDINGS) {
			long countFilesWithEnding = countFilesWithEnding(shpDirectory, requiredFileEnding);
			if (countFilesWithEnding == 0) {
				missingFiles.add(requiredFileEnding);
			} else if (countFilesWithEnding > 1) {
				multibleFiles.add(requiredFileEnding);
			}
		}
		if (missingFiles.size() > 0) {
			throw new ShapeZipInvalidException(REQUIRED_FILEENDINGS,
				"Es fehlen folgende Dateien: '" + String.join(", ", missingFiles) + "'");
		}

		if (multibleFiles.size() > 0) {
			throw new ShapeZipInvalidException(REQUIRED_FILEENDINGS,
				"Folgende Dateien sind mehrfach vorhanden: '" + String.join(", ", multibleFiles) + "'");
		}

		List<Path> shapefileComponents = Files.list(Paths.get(shpDirectory.getAbsolutePath())).filter(
			fileName -> {
				return SUPPORTED_FILEENDINGS.stream()
					.anyMatch(supportedEnding -> fileName.toString().endsWith(supportedEnding));
			})
			.collect(Collectors.toList());

		String shpName = shapefileComponents.get(0).getFileName().toString().split("\\.")[0];
		if (!shapefileComponents.stream().allMatch(fileName -> {
			return fileName.getFileName().toString().split("\\.")[0].equals(shpName);
		})) {
			throw new ShapeZipInvalidException("Alle Komponenten des Shapefiles müssen den gleichen Dateinamen haben.");
		}
	}

	private long countFilesWithEnding(File directory, String ending) throws IOException {
		return Files.list(Paths.get(directory.getAbsolutePath()))
			.filter(p -> p.toString().endsWith(ending))
			.count();
	}

	private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	public void zip(OutputStream out, File file) throws IOException {
		ZipOutputStream zipOutputStream = new ZipOutputStream(out);
		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			ZipEntry zipEntry = new ZipEntry(files[i].getName());
			zipOutputStream.putNextEntry(zipEntry);
			zipOutputStream.write(Files.readAllBytes(files[i].toPath()));
			zipOutputStream.closeEntry();
		}
		zipOutputStream.close();
	}

}
