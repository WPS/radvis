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

package de.wps.radvis.backend.common.schnittstelle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CSVExportConverter implements ExportConverter {

	private final CsvRepository csvRepository;

	@Override
	public byte[] convert(List<ExportData> data) {
		if (data.isEmpty()) {
			return "Keine Daten zum Exportieren".getBytes(StandardCharsets.UTF_8);
		}

		try {
			return csvRepository
				.write(CsvData.of(data.stream().map(exp -> exp.getProperties()).collect(Collectors.toList()),
					data.get(0).getHeaders()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getDateinamenSuffix() {
		return "_csv_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv";
	}

}
