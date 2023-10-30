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

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
public class Attribute {

	@Getter
	private Map<String, Object> attribute;

	public Attribute() {
		this.attribute = new HashMap<>();
	}

	public boolean hasAttribut(String key) {
		return attribute.containsKey(key);
	}

	public Object getAttributValue(String key) {
		require(hasAttribut(key));
		return attribute.get(key);
	}

	public void addAttribute(String key, Object value) {
		require(!hasAttribut(key));
		attribute.put(key, value);
	}

}
