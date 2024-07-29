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

package de.wps.radvis.backend.servicestation.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import de.wps.radvis.backend.common.domain.service.ExporterService;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.valueObject.Marke;
import de.wps.radvis.backend.servicestation.domain.valueObject.Oeffnungszeiten;

public class ServicestationExporterService implements ExporterService {

	private final ServicestationRepository servicestationRepository;

	public ServicestationExporterService(ServicestationRepository servicestationRepository) {
		this.servicestationRepository = servicestationRepository;
	}

	@Override
	public List<ExportData> export(List<Long> ids) {
		return StreamSupport.stream(servicestationRepository.findAllById(ids).spliterator(), false)
			.map(servicestation -> {
				Map<String, String> attribute = new LinkedHashMap<>();

				attribute.put(Servicestation.CsvHeader.RAD_VIS_ID, String.valueOf(servicestation.getId()));
				attribute.put(Servicestation.CsvHeader.POSITION_X_UTM32_N,
					String.valueOf(servicestation.getGeometrie().getCoordinate().x).replace('.', ','));
				attribute.put(Servicestation.CsvHeader.POSITION_Y_UTM32_N,
					String.valueOf(servicestation.getGeometrie().getCoordinate().y).replace('.', ','));
				attribute.put(Servicestation.CsvHeader.NAME, servicestation.getName().getValue());
				attribute.put(Servicestation.CsvHeader.GEBUEHREN,
					servicestation.getGebuehren().getValueAsString());
				attribute.put(Servicestation.CsvHeader.OEFFNUNGSZEITEN,
					servicestation.getOeffnungszeiten().map(Oeffnungszeiten::getValue).orElse(""));
				attribute.put(Servicestation.CsvHeader.BETREIBER, servicestation.getBetreiber().getValue());
				attribute.put(Servicestation.CsvHeader.MARKE,
					servicestation.getMarke().map(Marke::getValue).orElse(""));
				attribute.put(Servicestation.CsvHeader.LUFTPUMPE,
					servicestation.getLuftpumpe().getValueAsString());
				attribute.put(Servicestation.CsvHeader.KETTENWERKZEUG,
					servicestation.getKettenwerkzeug().getValueAsString());
				attribute.put(Servicestation.CsvHeader.WERKZEUG,
					servicestation.getWerkzeug().getValueAsString());
				attribute.put(Servicestation.CsvHeader.FAHRRADHALTERUNG,
					servicestation.getFahrradhalterung().getValueAsString());
				attribute.put(Servicestation.CsvHeader.BESCHREIBUNG, servicestation.getBeschreibung().isPresent()
					? servicestation.getBeschreibung().get().getValue() : "");
				attribute.put(Servicestation.CsvHeader.ZUSTAENDIG_IN_RAD_VIS,
					servicestation.getOrganisation().getDisplayText());
				attribute.put(Servicestation.CsvHeader.TYP, servicestation.getTyp().toString());
				attribute.put(Servicestation.CsvHeader.STATUS, servicestation.getStatus().toString());
				attribute.put(Servicestation.CsvHeader.QUELL_SYSTEM, servicestation.getQuellSystem().toString());

				return new ExportData(servicestation.getGeometrie(), attribute);
			}).collect(Collectors.toList());
	}

	@Override
	public String getDateinamenPrefix() {
		return "servicestationen";
	}

}
