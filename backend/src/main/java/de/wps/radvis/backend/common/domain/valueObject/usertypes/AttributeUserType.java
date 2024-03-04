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

package de.wps.radvis.backend.common.domain.valueObject.usertypes;

import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.common.domain.valueObject.Attribute;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AttributeUserType implements AttributeConverter<Attribute, String> {

	@Override
	public String convertToDatabaseColumn(Attribute attribute) {
		try {
			return new ObjectMapper().writeValueAsString(attribute.getAttribute());
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Map konnte für die Serialisierung in die DB nicht in JSON konvertiert werden",
				e);
		}
	}

	@Override
	public Attribute convertToEntityAttribute(String databaseValue) {
		try {
			// noinspection unchecked
			return Attribute.of(new ObjectMapper().readValue(databaseValue,
				new TypeReference<HashMap<String, Object>>() {
				}));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON-Daten aus der DB konnte nicht in Map konvertiert werden", e);
		}
	}
}
