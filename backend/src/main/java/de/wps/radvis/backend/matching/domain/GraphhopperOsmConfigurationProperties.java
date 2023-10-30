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

package de.wps.radvis.backend.matching.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import lombok.Getter;

@ConfigurationProperties("radvis.graphhopper.osm")
@Getter
public class GraphhopperOsmConfigurationProperties {

	private final String cacheVerzeichnis;

	private final Double measurementErrorSigma;

	private final String mappingCacheVerzeichnis;

	@ConstructorBinding
	public GraphhopperOsmConfigurationProperties(
		String cacheVerzeichnis,
		String mappingCacheVerzeichnis,
		Double measurementErrorSigma) {
		require(isValidDateipfad(cacheVerzeichnis), "cacheVerzeichnis muss Dateipfadstruktur haben");
		require(measurementErrorSigma, notNullValue());
		require(measurementErrorSigma > 0, "measurementErrorSigma muss positiv sein");
		require(isValidDateipfad(mappingCacheVerzeichnis), "mappingCacheVerzeichnis muss Dateipfadstruktur haben");
		this.cacheVerzeichnis = cacheVerzeichnis;
		this.measurementErrorSigma = measurementErrorSigma;
		this.mappingCacheVerzeichnis = mappingCacheVerzeichnis;
	}

	public static boolean isValidDateipfad(String value) {
		try {
			Paths.get(value);
			return true;
		} catch (InvalidPathException e) {
			return false;
		}
	}
}
