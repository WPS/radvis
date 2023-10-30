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

@ConfigurationProperties("radvis.graphhopper.dlm")
public class GraphhopperDlmConfigurationProperties {

	@Getter
	private final String dlmBasisDaten;

	@Getter
	private final String cacheVerzeichnis;    // auch routingCacheVerzeichnis genannt

	@Getter
	private final String mappingCacheVerzeichnis;

	@Getter
	private final Double measurementErrorSigma;

	@Getter
	private final String elevationCacheVerzeichnis;

	@Getter
	private final String tiffTilesVerzeichnis;

	@ConstructorBinding
	public GraphhopperDlmConfigurationProperties(
		String dlmBasisDaten, String cacheVerzeichnis, String mappingCacheVerzeichnis,
		Double measurementErrorSigma, String elevationCacheVerzeichnis, String tiffTilesVerzeichnis) {
		require(isValidDateipfad(dlmBasisDaten), "dlmBasisDaten muss Dateipfadstruktur haben");
		require(isValidDateipfad(cacheVerzeichnis), "cacheVerzeichnis muss Dateipfadstruktur haben");
		require(isValidDateipfad(mappingCacheVerzeichnis), "mappingCacheVerzeichnis muss Dateipfadstruktur haben");
		require(isValidDateipfad(elevationCacheVerzeichnis), "elevationCacheVerzeichnis muss Dateipfadstruktur haben");
		require(isValidDateipfad(tiffTilesVerzeichnis), "tiffTilesVerzeichnis muss Dateipfadstruktur haben");
		require(measurementErrorSigma, notNullValue());
		require(measurementErrorSigma > 0, "measurementErrorSigma muss positiv sein");

		this.mappingCacheVerzeichnis = mappingCacheVerzeichnis;
		this.dlmBasisDaten = dlmBasisDaten;
		this.cacheVerzeichnis = cacheVerzeichnis;
		this.measurementErrorSigma = measurementErrorSigma;
		this.elevationCacheVerzeichnis = elevationCacheVerzeichnis;
		this.tiffTilesVerzeichnis = tiffTilesVerzeichnis;
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
