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

package de.wps.radvis.backend.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.common.schnittstelle.CSVExportConverter;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.ExportConverterFactory;
import de.wps.radvis.backend.common.schnittstelle.GeoPackageExportConverter;
import de.wps.radvis.backend.common.schnittstelle.ShpExportConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.CsvRepositoryImpl;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.GeoJsonImportRepositoryImpl;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.ShapeFileRepositoryImpl;

@Configuration
@EnableJpaRepositories
@EntityScan
public class CommonConfiguration {

	@Autowired
	ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	@Bean
	public ShapeZipService shapeZipService() {
		return new ShapeZipService();
	}

	@Bean
	public ShapeFileRepository shapeFileRepository() {
		return new ShapeFileRepositoryImpl(coordinateReferenceSystemConverter);
	}

	@Bean
	public GeoJsonImportRepository geoJsonImportRepository() {
		return new GeoJsonImportRepositoryImpl(coordinateReferenceSystemConverter);
	}

	@Bean
	public RadVisDomainEventPublisher radVISDomainEventPublisher() {
		return new RadVisDomainEventPublisher(applicationEventPublisher);
	}

	@Bean
	public CsvRepository csvRepository() {
		return new CsvRepositoryImpl();
	}

	@Bean
	public ShpExportConverter shpExportConverter() {
		return new ShpExportConverter(shapeFileRepository(), shapeZipService());
	}

	@Bean
	public CSVExportConverter csvExportConverter() {
		return new CSVExportConverter(csvRepository());
	}

	@Bean
	public ExportConverterFactory exportConverterFactory() {
		return new ExportConverterFactory(shpExportConverter(), csvExportConverter());
	}

	@Bean
	public GeoPackageExportConverter geoPackageExportConverter() {
		return new GeoPackageExportConverter();
	}
}
