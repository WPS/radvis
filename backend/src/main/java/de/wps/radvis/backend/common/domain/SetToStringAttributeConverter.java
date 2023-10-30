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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;

public class SetToStringAttributeConverter implements AttributeConverter<Set<String>, String> {
	@Override
	public String convertToDatabaseColumn(Set<String> attribute) {
		return attribute == null ? null : String.join(",", attribute);
	}

	@Override
	public Set<String> convertToEntityAttribute(String dbData) {
		return dbData == null ? Collections.emptySet() : Arrays.stream(dbData.split(",")).collect(Collectors.toSet());
	}
}
