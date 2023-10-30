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

package de.wps.radvis.backend.quellimport.common;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.quellimport.common.domain.FeatureImportRepository;
import de.wps.radvis.backend.quellimport.common.schnittstelle.ImportedFeatureToGeoJsonConverter;
import de.wps.radvis.backend.quellimport.common.schnittstelle.repositoryImpl.FeatureImportRepositoryImpl;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class ImportsCommonConfiguration {

	private final CommonConfigurationProperties commonConfigurationProperties;
	private final GeoConverterConfiguration geoConverterConfiguration;

	public ImportsCommonConfiguration(@NonNull GeoConverterConfiguration geoConverterConfiguration,
		@NonNull CommonConfigurationProperties commonConfigurationProperties) {
		this.geoConverterConfiguration = geoConverterConfiguration;
		this.commonConfigurationProperties = commonConfigurationProperties;
	}

	@Bean
	public FeatureImportRepository featureImportRepository() {
		ExtentProperty extentProperty = commonConfigurationProperties.getExtentProperty();
		return new FeatureImportRepositoryImpl(geoConverterConfiguration.coordinateReferenceSystemConverter(),
			new Envelope(new Coordinate(extentProperty.getMinX(), extentProperty.getMinY()),
				new Coordinate(extentProperty.getMaxX(), extentProperty.getMaxY())));
	}

	@Bean
	public ImportedFeatureToGeoJsonConverter importedFeatureToGeoJsonConverterService() {
		return new ImportedFeatureToGeoJsonConverter();
	}
}
