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

package de.wps.radvis.backend.abfrage.intern.schnittstelle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;

@RestController
@RequestMapping("/api/extern/pbf")
public class PbfExportController {

	private final String pbfFilePath;

	public PbfExportController(OsmPbfConfigurationProperties osmPbfConfigurationProperties) {
		this.pbfFilePath = osmPbfConfigurationProperties.getOsmAngereichertDaten();
	}

	@GetMapping("/download")
	public ResponseEntity<Resource> downloadPbf() throws FileNotFoundException {
		File file = new File(pbfFilePath);

		InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
		header.add("Cache-Control", "no-cache, no-store, must-revalidate");
		header.add("Pragma", "no-cache");
		header.add("Expires", "0");

		return ResponseEntity.ok()
			.headers(header)
			.contentLength(file.length())
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(resource);
	}
}
