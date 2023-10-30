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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class MappedAttributesProperties {

	private final Map<String, Object> propertyMap;

	private MappedAttributesProperties(Map<String, Object> propertyMap) {
		require(propertyMap, notNullValue());

		this.propertyMap = new HashMap<>();
		propertyMap.forEach((k, v) -> this.propertyMap.put(k.toLowerCase(), v));
	}

	static public MappedAttributesProperties of(Map<String, Object> properties) {
		return new MappedAttributesProperties(properties);
	}

	public String getProperty(String attributName) {
		require(propertyMap.containsKey(attributName.toLowerCase()), "Kein Wert für '" + attributName + "' vorhanden!");
		Object propertyValue = propertyMap.get(attributName.toLowerCase());
		return propertyValue == null ? null : propertyValue.toString();
	}

	public String getPropertyOrElse(String attributName, String elseValue) {
		Object propertyValue = propertyMap.getOrDefault(attributName.toLowerCase(), elseValue);
		// "null" ist in der Shapefile das Signal "Attribut ist nicht gesetzt", was wir hier als "Attribut ist nicht
		// vorhanden" interpretieren und der Einfachheit halber den default-Wert zurückgeben.
		return propertyValue == null ? elseValue : propertyValue.toString();
	}

	public Map<String, Object> getPropertyMap() {
		return this.propertyMap;
	}

}
