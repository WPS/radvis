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

package de.wps.radvis.backend.manuellerimport.massnahmenimport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.MassnahmenImportConfigurationProperties;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service.ManuellerMassnahmenImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service.MassnahmeNetzbezugService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.MassnahmenImportNetzbezugAktualisierenCommandConverter;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.controller.ManuellerMassnahmenImportGuard;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class MassnahmenImportConfiguration {

	@Autowired
	ManuellerImportService manuellerImportService;

	@Autowired
	SimpleMatchingService simpleMatchingService;

	@Autowired
	NetzService netzService;

	@Autowired
	GeoJsonImportRepository geoJsonImportRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	BenutzerResolver benutzerResolver;

	@Autowired
	MassnahmeRepository massnahmenRepostory;

	@Autowired
	CsvRepository csvRepository;

	@PersistenceContext
	private EntityManager entityManager;

	private final MassnahmenImportConfigurationProperties massnahmenImportConfigurationProperties;

	public MassnahmenImportConfiguration(
		MassnahmenImportConfigurationProperties massnahmenImportConfigurationProperties) {
		this.massnahmenImportConfigurationProperties = massnahmenImportConfigurationProperties;
	}

	@Bean
	public ManuellerMassnahmenImportService manuellerMassnahmenImportService() {
		return new ManuellerMassnahmenImportService(
			manuellerImportService,
			massnahmeNetzbezugService(),
			geoJsonImportRepository,
			verwaltungseinheitRepository,
			massnahmenRepostory,
			entityManager,
			csvRepository,
			massnahmenImportConfigurationProperties.getMinimaleDistanzFuerAbweichungsWarnung()
		);
	}

	@Bean
	public MassnahmeNetzbezugService massnahmeNetzbezugService() {
		return new MassnahmeNetzbezugService(
			simpleMatchingService,
			netzService
		);
	}

	@Bean
	public ManuellerMassnahmenImportGuard manuellerMassnahmenImportGuard() {
		return new ManuellerMassnahmenImportGuard(benutzerResolver, verwaltungseinheitService);
	}

	@Bean
	public MassnahmenImportNetzbezugAktualisierenCommandConverter massnahmenImportNetzbezugAktualisierenCommandConverter() {
		return new MassnahmenImportNetzbezugAktualisierenCommandConverter(netzService, netzService);
	}
}
