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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.valid4j.Assertive.require;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode
public class CsvData {

	@NonNull
	private final List<Map<String, String>> rows;
	@NonNull
	private final List<String> header;

	private CsvData(List<Map<String, String>> rows, List<String> header) {
		require(rowsEnthaltenGenauHeader(rows, header),
			"Die Keys in den Zeilen müssen genau mit den Headern übereinstimmen");
		this.header = Collections.unmodifiableList(header);
		this.rows = rows.stream().map(r -> Collections.unmodifiableMap(r)).collect(Collectors.toUnmodifiableList());
	}

	private boolean rowsEnthaltenGenauHeader(List<Map<String, String>> rows, List<String> header) {
		for (Map<String, String> row : rows) {
			Set<String> keys = row.keySet();
			if (keys.size() != header.size() || !keys.containsAll(header)) {
				return false;
			}
		}
		return true;
	}

	public static CsvData of(List<Map<String, String>> rows, List<String> header) {
		return new CsvData(rows, header);
	}
}
