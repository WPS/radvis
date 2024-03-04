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

package de.wps.radvis.backend.common.domain;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.exception.CsvAttributMappingException;
import de.wps.radvis.backend.common.domain.exception.CsvImportException;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;

public abstract class AbstractCsvImportService<ENTITY, BUILDER> {
	public static final List<String> PROTOKOLL_HEADER = List.of("Aktion", "URL", "Fehler");

	private List<String> requiredHeaders;
	private String idHeader;

	protected AbstractCsvImportService(List<String> requiredHeaders, String idHeader) {
		this.requiredHeaders = requiredHeaders;
		this.idHeader = idHeader;
	}

	public CsvData importCsv(CsvData csvData, AbstractCsvAttributeMapper<ENTITY, BUILDER> attributeMapper)
		throws CsvImportException {
		if (csvData.getHeader().size() != requiredHeaders.size()
			|| !csvData.getHeader().containsAll(requiredHeaders)) {
			throw new CsvImportException(
				"Alle Spalten müssen vorhanden sein. [" + String.join(";", requiredHeaders) + "]");
		}

		List<Map<String, String>> protokollEintraege = new ArrayList<>();

		csvData.getRows().forEach(row -> {
			Map<String, String> protokollEintrag = new HashMap<>();
			protokollEintrag.put("Aktion", "Hinzugefügt");

			BUILDER builder = getDefaultBuilder();

			if (!row.get(idHeader).isEmpty()) {
				long id = Long.parseLong(row.get(idHeader));
				Optional<BUILDER> existing = getBuilderFromId(id);
				if (existing.isPresent()) {
					builder = existing.get();
					protokollEintrag.put("Aktion", "Aktualisiert");
				}
			} else {
				try {
					Optional<BUILDER> existing = getBuilderFromPosition(extractPosition(row));
					if (existing.isPresent()) {
						builder = existing.get();
						protokollEintrag.put("Aktion", "Aktualisiert");
					}
				} catch (ParseException e) {
					// fortfahren, Fehler taucht beim Mapping erneut auf und wird behandelt
				}
			}

			try {
				ENTITY mappedEntity = attributeMapper.mapAttributes(builder, row);
				ENTITY savedEntity = save(mappedEntity);
				protokollEintrag.put("Fehler", "");
				try {
					protokollEintrag.put("URL", createUrl(savedEntity));
				} catch (URISyntaxException e) {
					// Dann gibt es eben keine URL, sollte nicht auftreten außer durch Programmierfehler
				}
			} catch (CsvAttributMappingException e) {
				protokollEintrag.put("Aktion", "Ignoriert");
				protokollEintrag.put("Fehler", e.getMessage());
				protokollEintrag.put("URL", "");
			}

			protokollEintrag.putAll(row);
			protokollEintraege.add(protokollEintrag);
		});

		List<String> exportHeader = new ArrayList<>(requiredHeaders);
		exportHeader.addAll(PROTOKOLL_HEADER);

		return CsvData.of(protokollEintraege, exportHeader);
	}

	protected abstract String createUrl(ENTITY savedEntity) throws URISyntaxException;

	protected abstract Geometry extractPosition(Map<String, String> row) throws ParseException;

	protected abstract ENTITY save(ENTITY mappedEntity);

	protected abstract Optional<BUILDER> getBuilderFromPosition(Geometry extractedPosition);

	protected abstract Optional<BUILDER> getBuilderFromId(long id);

	protected abstract BUILDER getDefaultBuilder();
}
