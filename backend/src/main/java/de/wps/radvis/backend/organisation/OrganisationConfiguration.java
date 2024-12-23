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

package de.wps.radvis.backend.organisation;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.WahlkreisRepository;
import de.wps.radvis.backend.organisation.domain.WahlkreisService;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitImportRepositoryFactory;
import jakarta.annotation.Nonnull;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class OrganisationConfiguration {

	private final GeoConverterConfiguration geoConverterConfiguration;

	@Autowired
	private VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private OrganisationRepository organisationRepository;

	@Autowired
	private WahlkreisRepository wahlkreisRepository;

	private final CommonConfigurationProperties commonConfigurationProperties;

	private final OrganisationConfigurationProperties organisationConfigurationProperties;

	public OrganisationConfiguration(@NonNull GeoConverterConfiguration geoConverterConfiguration,
		@NonNull CommonConfigurationProperties commonConfigurationProperties,
		@Nonnull OrganisationConfigurationProperties organisationConfigurationProperties) {
		this.organisationConfigurationProperties = organisationConfigurationProperties;
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.geoConverterConfiguration = geoConverterConfiguration;
	}

	@Bean
	@Lazy
	public VerwaltungseinheitImportRepository organisationenImportRepository() {
		File verwaltungsgrenzenShapeFileRoot = new File(
			commonConfigurationProperties.getExterneResourcenBasisPfad(),
			organisationConfigurationProperties.getVerwaltungsgrenzenShapeFilesPath());
		return verwaltungseinheitImportRepositoryFactory().getImportRepository(verwaltungsgrenzenShapeFileRoot);
	}

	@Bean
	public VerwaltungseinheitImportRepositoryFactory verwaltungseinheitImportRepositoryFactory() {
		return new VerwaltungseinheitImportRepositoryFactory(
			geoConverterConfiguration.coordinateReferenceSystemConverter(),
			commonConfigurationProperties.getObersteGebietskoerperschaftOrganisationsArt(),
			commonConfigurationProperties.getObersteGebietskoerperschaftName());
	}

	@Bean
	@Primary
	public VerwaltungseinheitService verwaltungseinheitService() {
		return new VerwaltungseinheitService(verwaltungseinheitRepository, gebietskoerperschaftRepository,
			organisationRepository, commonConfigurationProperties.getObersteGebietskoerperschaftOrganisationsArt(),
			commonConfigurationProperties.getObersteGebietskoerperschaftName());
	}

	@Bean
	public VerwaltungseinheitResolver verwaltungseinheitResolver() {
		return verwaltungseinheitService();
	}

	@Bean
	public WahlkreisService wahlkreisService() {
		return new WahlkreisService(wahlkreisRepository);
	}
}
