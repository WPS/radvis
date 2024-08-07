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

package de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.service.ZipService;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.service.ManuellerMassnahmenDateianhaengeImportService;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.controller.ManuellerMassnahmenDateianhaengeImportGuard;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.converter.SaveMassnahmenDateianhaengeCommandConverter;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

@Configuration
public class MassnahmenDateianhaengeImportConfiguration {
	@Autowired
	ManuellerImportService manuellerImportService;

	@Autowired
	MassnahmeRepository massnahmenRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	BenutzerResolver benutzerResolver;

	@Bean
	public ManuellerMassnahmenDateianhaengeImportService manuellerMassnahmenDateianhaengeImportService(
		ZipService zipService, CsvRepository csvRepository) {
		return new ManuellerMassnahmenDateianhaengeImportService(
			manuellerImportService,
			zipService,
			csvRepository,
			massnahmenRepository,
			verwaltungseinheitRepository);
	}

	@Bean
	public ManuellerMassnahmenDateianhaengeImportGuard manuellerMassnahmenDateianhaengeImportGuard() {
		return new ManuellerMassnahmenDateianhaengeImportGuard(benutzerResolver, verwaltungseinheitService);
	}

	@Bean
	public SaveMassnahmenDateianhaengeCommandConverter saveMassnahmenDateianhaengeCommandConverter() {
		return new SaveMassnahmenDateianhaengeCommandConverter();
	}
}
