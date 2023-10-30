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

package de.wps.radvis.backend.shapetransformation.schnittstelle.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opengis.feature.simple.SimpleFeature;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.common.domain.exception.ZipFileRequiredFilesMissingException;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.shapetransformation.domain.TransformationsKonfigurationsRepository;
import de.wps.radvis.backend.shapetransformation.domain.TransformationsService;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeTransformationException;
import de.wps.radvis.backend.shapetransformation.domain.valueObject.TransformationsKonfiguration;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/transformation")
@Validated
@Slf4j
public class ShapeTransformationController {

	private final ShapeZipService shapeZipService;

	private final ShapeFileRepository shapeFileRepository;

	private final TransformationsService transformationsService;

	private final TransformationsKonfigurationsRepository transformationsKonfigurationsRepository;

	public ShapeTransformationController(ShapeZipService shapeZipService,
		ShapeFileRepository shapeFileRepository,
		TransformationsService transformationsService,
		TransformationsKonfigurationsRepository transformationsKonfigurationsRepository) {
		this.shapeZipService = shapeZipService;
		this.shapeFileRepository = shapeFileRepository;
		this.transformationsService = transformationsService;
		this.transformationsKonfigurationsRepository = transformationsKonfigurationsRepository;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public void transformShp(@RequestPart MultipartFile shpFile, @RequestPart MultipartFile transformationFile,
		HttpServletResponse response)
		throws IOException, ShapeTransformationException, ShapeProjectionException,
		ZipFileRequiredFilesMissingException {

		try {
			TransformationsKonfiguration konfiguration = transformationsKonfigurationsRepository
				.readKonfigurationFromCsv(transformationFile.getBytes());

			File unzipped = shapeZipService.unzip(shpFile.getBytes());

			File transformedDir = null;
			File transformedShpFile = null;
			try (Stream<SimpleFeature> simpleFeatureStream = shapeFileRepository.readShape(
				shapeZipService.getShapeFileFromDirectory(unzipped)
					.orElseThrow(() -> new ShapeTransformationException("Shape-Zip enthält keine .shp-Datei")))) {

				Stream<SimpleFeature> result = transformationsService.transformiere(simpleFeatureStream,
					konfiguration);

				transformedDir = Files.createTempDirectory("transformed").toFile();
				transformedDir.deleteOnExit();
				transformedShpFile = new File(transformedDir, "transformed.shp");
				transformedShpFile.deleteOnExit();
				shapeFileRepository.writeShape(transformedDir, transformedShpFile, result.collect(Collectors.toList()));
				shapeZipService.zip(response.getOutputStream(), transformedDir);
			} finally {
				if (transformedShpFile != null) {
					transformedShpFile.delete();
				}
				if (transformedDir != null) {
					transformedDir.delete();
				}
			}
		} catch (ShapeTransformationException | ShapeProjectionException | ZipFileRequiredFilesMissingException e) {
			throw e;
		} catch (Throwable t) {
			log.error("Ein unerwarteter Fehler ist beim Transformieren der Shapefile aufgetreten", t);
			throw new ShapeTransformationException(
				"Ein unerwarteter Fehler ist beim Transformieren der Shapefile aufgetreten.");
		}

		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=\"transformed.zip\"");
		response.flushBuffer();
	}
}
