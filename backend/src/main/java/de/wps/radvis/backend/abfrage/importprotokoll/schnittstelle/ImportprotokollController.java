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

package de.wps.radvis.backend.abfrage.importprotokoll.schnittstelle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.Importprotokoll;
import de.wps.radvis.backend.common.domain.service.ImportprotokollService;

@RestController
@RequestMapping("/api/importprotokoll")
public class ImportprotokollController {

	private final CommonConfigurationProperties commonConfigurationProperties;
	private final List<ImportprotokollService> importprotokollServices;

	public ImportprotokollController(@Autowired CommonConfigurationProperties commonConfigurationProperties, @Autowired List<ImportprotokollService> importprotokollServices) {
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.importprotokollServices = importprotokollServices;
	}

	@GetMapping("")
	public List<Importprotokoll> getAll() {
		LocalDateTime after = LocalDate.now().atStartOfDay()
			.minusDays(commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten());

		return this.importprotokollServices
			.stream()
			.flatMap(importprotokollService -> importprotokollService.getAllImportprotokolleAfter(after).stream())
			.sorted((o1, o2) -> o2.getStartZeit().compareTo(o1.getStartZeit()))
			.collect(Collectors.toList());
	}
}
