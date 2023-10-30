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

package de.wps.radvis.backend.matching.domain.entity;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphhopper.jackson.Jackson;
import com.graphhopper.util.CustomModel;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class CustomRoutingProfile extends AbstractEntity {

	private String name;

	private String profilJson;

	@Builder
	private CustomRoutingProfile(Long id, String name, String profilJson) {
		super(id);
		this.name = name;
		this.profilJson = profilJson;
	}

	public static CustomModel parseCustomModel(String profilJson) throws JsonProcessingException {
		ObjectMapper jsonOM = Jackson.initObjectMapper(new ObjectMapper(new JsonFactory()));
		profilJson = profilJson
			.replaceAll("\"multiply_by\": \"(.*?)\"", "\"multiply_by\" : $1")
			.replaceAll("\"limit_to\": \"(.*?)\"", "\"limit_to\" : $1");

		return jsonOM.readValue(profilJson, CustomModel.class);
	}

}
