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

package de.wps.radvis.backend.abfrage.netzausschnitt;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.BuildNetzklassenStreckenSignaturViewJob;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.BuildRadNETZNetzViewCacheJob;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.KantenAbfrageRepository;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzausschnittService;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzklassenStreckenSignaturView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzklassenStreckenSignaturViewRepository;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.RadNETZNetzViewCacheRepository;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.StreckeViewCacheRepository;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle.NetzToGeoJsonConverter;
import de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle.NetzausschnittGuard;
import de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle.repositoryImpl.KantenAbfrageRepositoryImpl;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.netz.domain.entity.NetzklassenStreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzklassenStreckenViewService;
import de.wps.radvis.backend.netz.domain.service.StreckenViewService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class AbfrageNetzausschnittConfiguration {

	@Autowired
	private FeatureToggleProperties featureToggleProperties;

	@Autowired
	private DLMConfigurationProperties dlmConfigurationProperties;

	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@Autowired
	private KnotenRepository knotenRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@PersistenceContext
	private EntityManager entityManager;

	private final BenutzerResolver benutzerResolver;

	private final NetzklassenStreckenViewService netzklassenStreckenViewService;

	private final StreckenViewService streckenViewService;

	public AbfrageNetzausschnittConfiguration(@NonNull BenutzerResolver benutzerResolver,
		@NonNull StreckenViewService streckenViewService,
		@NonNull NetzklassenStreckenViewService netzklassenStreckenViewService) {
		this.benutzerResolver = benutzerResolver;
		this.streckenViewService = streckenViewService;
		this.netzklassenStreckenViewService = netzklassenStreckenViewService;
	}

	@Bean
	public NetzToGeoJsonConverter netzToGeoJsonConverterService() {
		return new NetzToGeoJsonConverter();
	}

	@Bean
	public KantenAbfrageRepository kantenAbfrageRepository() {
		return new KantenAbfrageRepositoryImpl(featureToggleProperties);
	}

	@Bean
	public StreckeViewCacheRepository<NetzMapView, StreckeVonKanten> radNETZNetzViewCacheRepository() {
		return new RadNETZNetzViewCacheRepository();
	}

	@Bean
	public StreckeViewCacheRepository<List<NetzklassenStreckenSignaturView>, NetzklassenStreckeVonKanten> netzklassenStreckenSignaturViewRepository() {
		return new NetzklassenStreckenSignaturViewRepository();
	}

	@Bean
	public NetzausschnittService netzausschnittService() {
		return new NetzausschnittService(netzfehlerRepository, kantenAbfrageRepository(), knotenRepository,
			radNETZNetzViewCacheRepository(), netzklassenStreckenSignaturViewRepository());
	}

	@Bean
	public NetzausschnittGuard netzausschnittGuard() {
		return new NetzausschnittGuard(benutzerResolver);
	}

	@Bean
	public BuildRadNETZNetzViewCacheJob buildRadNETZNetzViewCacheJob() {
		return new BuildRadNETZNetzViewCacheJob(jobExecutionDescriptionRepository, kantenRepository,
			streckenViewService, radNETZNetzViewCacheRepository(), entityManager, dlmConfigurationProperties);
	}

	@Bean
	public BuildNetzklassenStreckenSignaturViewJob buildNetzklassenStreckenSignaturViewJob() {
		return new BuildNetzklassenStreckenSignaturViewJob(jobExecutionDescriptionRepository, kantenRepository,
			netzklassenStreckenViewService, netzklassenStreckenSignaturViewRepository(), entityManager,
			dlmConfigurationProperties);
	}
}
