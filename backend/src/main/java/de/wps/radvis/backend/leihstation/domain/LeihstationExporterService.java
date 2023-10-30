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

package de.wps.radvis.backend.leihstation.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import de.wps.radvis.backend.common.domain.service.ExporterService;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;

public class LeihstationExporterService implements ExporterService {

	private LeihstationRepository leihstationRepository;

	public LeihstationExporterService(LeihstationRepository leihstationRepository) {
		this.leihstationRepository = leihstationRepository;
	}

	@Override
	public List<ExportData> export(List<Long> ids) {
		return StreamSupport.stream(leihstationRepository.findAllById(ids).spliterator(), false).map(leihstation -> {
			Map<String, String> attribute = new HashMap<>();

			attribute.put(Leihstation.CsvHeader.RAD_VIS_ID,
				String.valueOf(leihstation.getId()));
			attribute.put(Leihstation.CsvHeader.POSITION_X_UTM32_N,
				String.valueOf(leihstation.getGeometrie().getCoordinate().x).replace('.', ','));
			attribute.put(Leihstation.CsvHeader.POSITION_Y_UTM32_N,
				String.valueOf(leihstation.getGeometrie().getCoordinate().y).replace('.', ','));
			attribute.put(Leihstation.CsvHeader.BETREIBER,
				leihstation.getBetreiber());
			attribute.put(Leihstation.CsvHeader.ANZAHL_FAHRRAEDER,
				leihstation.getAnzahlFahrraeder().map(v -> v.toString()).orElse(""));
			attribute.put(Leihstation.CsvHeader.ANZAHL_PEDELECS,
				leihstation.getAnzahlPedelecs().map(v -> v.toString()).orElse(""));
			attribute.put(Leihstation.CsvHeader.ANZAHL_ABSTELLMOEGLICHKEITEN,
				leihstation.getAnzahlAbstellmoeglichkeiten().map(v -> v.toString()).orElse(""));
			attribute.put(Leihstation.CsvHeader.FREIES_ABSTELLEN_MOEGLICH,
				leihstation.isFreiesAbstellen() ? "ja" : "nein");
			attribute.put(Leihstation.CsvHeader.BUCHUNGS_URL,
				leihstation.getBuchungsUrl().map(url -> url.getValue()).orElse(""));
			attribute.put(Leihstation.CsvHeader.STATUS,
				leihstation.getStatus().toString());
			attribute.put(Leihstation.CsvHeader.QUELLSYSTEM,
				leihstation.getQuellSystem().toString());

			return new ExportData(leihstation.getGeometrie(), attribute, Leihstation.CsvHeader.ALL);
		}).collect(Collectors.toList());
	}

	@Override
	public String getDateinamenPrefix() {
		return "leihstationen";
	}

}
