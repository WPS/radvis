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

package de.wps.radvis.backend.abfrage.export.schnittstelle;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.abfrage.export.domain.InfrastrukturTyp;
import de.wps.radvis.backend.abfrage.export.domain.InfrastrukturenExporterFactory;
import de.wps.radvis.backend.abfrage.export.domain.service.ExportFromViewService;
import de.wps.radvis.backend.common.domain.service.ExporterService;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.schnittstelle.ExportConverter;
import de.wps.radvis.backend.common.schnittstelle.ExportConverterFactory;
import de.wps.radvis.backend.common.schnittstelle.ExportFormat;
import de.wps.radvis.backend.common.schnittstelle.GeoPackageExportConverter;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/export/")
public class ExportInfrastrukturenController {

	private final InfrastrukturenExporterFactory infrastrukturenExporterFactory;
	private final ExportConverterFactory converterFactory;
	private final ExportFromViewService exportFromViewService;

	public ExportInfrastrukturenController(InfrastrukturenExporterFactory infrastrukturenExporterFactory,
		ExportConverterFactory converterFactory, ExportFromViewService exportFromViewService) {
		this.infrastrukturenExporterFactory = infrastrukturenExporterFactory;
		this.converterFactory = converterFactory;
		this.exportFromViewService = exportFromViewService;
	}

	@PostMapping("{format}/infrastruktur/{typ}")
	public ResponseEntity<byte[]> export(@PathVariable("typ") InfrastrukturTyp infrastrukturTyp,
		@PathVariable("format") ExportFormat format, @RequestBody ExportInfrastrukturCommand command) {
		ExporterService exporter = infrastrukturenExporterFactory.getExporter(infrastrukturTyp);
		List<ExportData> exportData = exporter.export(command.getIds());
		ExportConverter converter = converterFactory.getConverter(format);

		HttpHeaders headers = new HttpHeaders();
		String dateiname = exporter.getDateinamenPrefix() + converter.getDateinamenSuffix();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + dateiname);
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		byte[] datei = converter.convert(exportData);

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
			.contentLength(datei.length)
			.headers(headers)
			.body(datei);
	}

	@GetMapping("balm")
	@Transactional
	public ResponseEntity<byte[]> exportBalm() {
		log.info("Starte Balm-Export als Geopackage");
		Map<String, List<ExportData>> exportData = exportFromViewService.exportBalm();

		log.debug("Schreibe GeoPackage Datei");
		GeoPackageExportConverter geoPackageExportConverter = new GeoPackageExportConverter();
		byte[] datei = geoPackageExportConverter.convertMulti(exportData);

		log.info("Balm-Export als Geopackage beendet.");

		String dateiname = "balm.gpkg";

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + dateiname);
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
			.contentLength(datei.length)
			.headers(headers)
			.body(datei);
	}
}
