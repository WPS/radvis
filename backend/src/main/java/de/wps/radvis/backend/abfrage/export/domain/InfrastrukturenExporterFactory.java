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

package de.wps.radvis.backend.abfrage.export.domain;

import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageExporterService;
import de.wps.radvis.backend.common.domain.service.ExporterService;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenExporterService;
import de.wps.radvis.backend.leihstation.domain.LeihstationExporterService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenExporterService;
import de.wps.radvis.backend.servicestation.domain.ServicestationExporterService;

public class InfrastrukturenExporterFactory {
	private final MassnahmenExporterService massnahmenExporterService;
	private final FahrradroutenExporterService fahrradroutenExporterService;
	private final LeihstationExporterService leihstationExporterService;
	private final ServicestationExporterService servicestationExporterService;
	private final AbstellanlageExporterService abstellanlageExporterService;

	public InfrastrukturenExporterFactory(MassnahmenExporterService massnahmenExporterService,
		FahrradroutenExporterService fahrradroutenExporterService,
		LeihstationExporterService leihstationExporterService,
		ServicestationExporterService servicestationExporterService,
		AbstellanlageExporterService abstellanlageExporterService) {
		this.massnahmenExporterService = massnahmenExporterService;
		this.fahrradroutenExporterService = fahrradroutenExporterService;
		this.leihstationExporterService = leihstationExporterService;
		this.servicestationExporterService = servicestationExporterService;
		this.abstellanlageExporterService = abstellanlageExporterService;
	}

	public ExporterService getExporter(InfrastrukturTyp infrastrukturTyp) {
		switch (infrastrukturTyp) {
		case MASSNAHME:
			return massnahmenExporterService;
		case FAHRRADROUTE:
			return fahrradroutenExporterService;
		case LEIHSTATION:
			return leihstationExporterService;
		case SERVICESTATION:
			return servicestationExporterService;
		case ABSTELLANLAGE:
			return abstellanlageExporterService;
		default:
			return null;
		}
	}
}
