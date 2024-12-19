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

package de.wps.radvis.backend.application.domain;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

public class RadVISInfoService {

	private final String radVisVersion;
	private final JdbcTemplate jdbcTemplate;

	public RadVISInfoService(String radVisVersion, JdbcTemplate jdbcTemplate) {
		this.radVisVersion = radVisVersion;
		this.jdbcTemplate = jdbcTemplate;
	}

	public Map<String, String> getVersion() {
		String postGisVersion = jdbcTemplate.queryForObject("SELECT postgis_lib_version()", String.class);
		String postgreSqlVersion = jdbcTemplate.queryForObject("SELECT version();", String.class)
			.replaceAll("PostgreSQL (\\d+\\.\\d+).*", "$1");

		Map<String, String> jsonMap = new HashMap<>();
		jsonMap.put("RadVIS-Version", radVisVersion);
		jsonMap.put("PostGIS-Version", postGisVersion);
		jsonMap.put("PostgreSQL-Version", postgreSqlVersion);

		return jsonMap;
	}
}
