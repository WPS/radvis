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

package de.wps.radvis.backend.shapetransformation.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TransformationsKonfiguration {

	private final Map<String, AttributTransformation> attributTransformationen;

	public TransformationsKonfiguration(Map<String, AttributTransformation> attributTransformationen) {
		require(attributTransformationen, notNullValue());

		this.attributTransformationen = attributTransformationen;
	}

	public boolean hasQuellAttributName(String quellAttributName) {
		return attributTransformationen.containsKey(quellAttributName);
	}

	public String getZielAttributName(String quellAttributName) {
		require(hasQuellAttributName(quellAttributName), "QuellAttributName muss vorhanden sein");

		return attributTransformationen.get(quellAttributName).getZielAttributName();
	}

	public boolean hasQuellAttributWert(String quellAttributName, String quellAttributWert) {
		require(hasQuellAttributName(quellAttributName), "QuellAttributName muss vorhanden sein");

		return attributTransformationen.get(quellAttributName).hasQuellAttributWert(quellAttributWert);
	}

	public String getZielAttributWert(String quellAttributName, String quellAttributWert) {
		require(hasQuellAttributWert(quellAttributName, quellAttributWert), "QuellAttributWert muss vorhanden sein");

		AttributTransformation attributTransformation = attributTransformationen.get(quellAttributName);

		require(attributTransformation.hasQuellAttributWert(quellAttributWert));
		return attributTransformation.getZielAttributWert(quellAttributWert);
	}

	public Set<String> getZielAttributNamen() {
		return attributTransformationen.values().stream().map(tr -> tr.getZielAttributName())
			.collect(Collectors.toSet());
	}

	public Set<String> getQuellAttributNamen() {
		return attributTransformationen.keySet();
	}
}
