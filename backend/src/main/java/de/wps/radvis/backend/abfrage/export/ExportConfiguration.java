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

package de.wps.radvis.backend.abfrage.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.abfrage.export.domain.InfrastrukturenExporterFactory;
import de.wps.radvis.backend.abfrage.export.domain.service.ExportFromViewService;
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageExporterService;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenExporterService;
import de.wps.radvis.backend.leihstation.domain.LeihstationExporterService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenExporterService;
import de.wps.radvis.backend.servicestation.domain.ServicestationExporterService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class ExportConfiguration {
	@Autowired
	private MassnahmenExporterService massnahmenExporterService;

	@Autowired
	private FahrradroutenExporterService fahrradroutenExporterService;

	@Autowired
	private LeihstationExporterService leihstationExporterService;

	@Autowired
	private ServicestationExporterService servicestationExporterService;

	@Autowired
	private AbstellanlageExporterService abstellanlageExporterService;

	@PersistenceContext
	private EntityManager entityManager;

	@Bean
	public InfrastrukturenExporterFactory infrastrukturenExporterFactory() {
		return new InfrastrukturenExporterFactory(massnahmenExporterService, fahrradroutenExporterService,
			leihstationExporterService, servicestationExporterService, abstellanlageExporterService);
	}

	@Bean
	public ExportFromViewService exportFromViewService() {
		return new ExportFromViewService(entityManager);
	}
}
