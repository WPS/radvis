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

package de.wps.radvis.backend.matching;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.weighting.custom.CustomProfile;
import com.graphhopper.util.CustomModel;

import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.CustomRoutingProfileRepository;
import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.KanteUpdateElevationService;
import de.wps.radvis.backend.matching.domain.OsmAbbildungsFehlerRepository;
import de.wps.radvis.backend.matching.domain.OsmAbbildungsFehlerService;
import de.wps.radvis.backend.matching.domain.OsmMatchingCacheRepository;
import de.wps.radvis.backend.matching.domain.OsmMatchingRepository;
import de.wps.radvis.backend.matching.domain.PbfErstellungsRepository;
import de.wps.radvis.backend.matching.domain.service.CustomRoutingProfileService;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GraphhopperUpdateService;
import de.wps.radvis.backend.matching.domain.service.MatchingJobProtokollService;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.matching.domain.service.OsmAuszeichnungsService;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.schnittstelle.CustomRoutingProfileGuard;
import de.wps.radvis.backend.matching.schnittstelle.GraphhopperUpdateServiceImpl;
import de.wps.radvis.backend.matching.schnittstelle.KanteUpdateElevationServiceImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.GraphhopperRoutingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchedGraphHopper;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchingCacheRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsRepositoryImpl;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@EnableScheduling
@EnableJpaRepositories
@EntityScan
public class MatchingConfiguration {

	private final CommonConfigurationProperties commonConfigurationProperties;
	private final GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties;
	private final GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties;
	private final OsmPbfConfigurationProperties osmPbfConfigurationProperties;

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@Autowired
	private DLMConfigurationProperties dlmConfigurationProperties;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private OsmAbbildungsFehlerRepository osmAbbildungsFehlerRepository;

	@Autowired
	private CustomRoutingProfileRepository customRoutingProfileRepository;

	@Autowired
	private BarriereRepository barriereRepository;

	@Autowired
	private BenutzerResolver benutzerResolver;

	public MatchingConfiguration(@NonNull GeoConverterConfiguration geoConverterConfiguration,
		@NonNull CommonConfigurationProperties commonConfigurationProperties,
		@NonNull GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties,
		@NonNull OsmPbfConfigurationProperties osmPbfConfigurationProperties,
		@NonNull GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties) {

		this.coordinateReferenceSystemConverter = geoConverterConfiguration.coordinateReferenceSystemConverter();
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.graphhopperOsmConfigurationProperties = graphhopperOsmConfigurationProperties;
		this.graphhopperDlmConfigurationProperties = graphhopperDlmConfigurationProperties;
		this.osmPbfConfigurationProperties = osmPbfConfigurationProperties;
	}

	@Bean
	public OsmMatchingCacheRepository osmMatchingCacheRepository() {
		return new OsmMatchingCacheRepositoryImpl(graphhopperOsmConfigurationProperties.getMappingCacheVerzeichnis());
	}

	@Bean
	public MatchingJobProtokollService osmJobProtokollService() {
		return new MatchingJobProtokollService(netzfehlerRepository);
	}

	@Lazy
	@Bean
	public OsmMatchingRepository osmMatchingRepository(OsmMatchedGraphHopper graphhopper) {
		return new OsmMatchingRepositoryImpl(graphhopper, coordinateReferenceSystemConverter,
			graphhopperOsmConfigurationProperties.getMeasurementErrorSigma());
	}

	@Lazy
	@Bean
	public DlmMatchingRepository dlmMatchingRepository() {
		return new DlmMatchingRepositoryImpl(dlmMatchedGraphHopperFactory(),
			coordinateReferenceSystemConverter,
			graphhopperDlmConfigurationProperties.getMeasurementErrorSigma());
	}

	@Lazy
	@Bean
	public GraphhopperUpdateService graphhopperUpdateService() {
		return new GraphhopperUpdateServiceImpl(dlmMatchedGraphHopperFactory(), graphhopperRoutingRepository(),
			dlmMatchingRepository());
	}

	@Bean
	public MatchingKorrekturService osmMatchingKorrekturService() {
		return new MatchingKorrekturService();
	}

	@Lazy
	@Bean
	public OsmMatchedGraphHopper graphhopper(OsmMatchingCacheRepository osmMatchingCacheRepository) {
		log.info("Genutztes OSM-File: {}", osmPbfConfigurationProperties.getOsmBasisDaten());

		OsmMatchedGraphHopper graphHopper = new OsmMatchedGraphHopper(osmMatchingCacheRepository);
		graphHopper.setOSMFile(osmPbfConfigurationProperties.getOsmBasisDaten());
		graphHopper.setGraphHopperLocation(graphhopperOsmConfigurationProperties.getCacheVerzeichnis());

		File directory = new File(graphhopperOsmConfigurationProperties.getMappingCacheVerzeichnis());
		if (!directory.exists()) {
			directory.mkdirs();
		}

		Profile profile = new Profile("bike").setVehicle("bike").setWeighting("shortest").setTurnCosts(false);
		graphHopper.setProfiles(profile);
		graphHopper.importOrLoad();

		log.info("OSM-GraphHopper ist initialisiert.");
		return graphHopper;
	}

	@Lazy
	@Bean
	public DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory() {
		String dlmPbfPath = graphhopperDlmConfigurationProperties.getDlmBasisDaten();
		Profile profileBike = new Profile("bike").setVehicle("bike").setWeighting("fastest").setTurnCosts(false);
		Profile profileCustomBike = new CustomProfile("bike_custom").setCustomModel(new CustomModel())
			.setVehicle("bike");
		Profile profileFoot = new Profile("foot").setVehicle("foot").setWeighting("fastest").setTurnCosts(false);
		int minNetworkSize = 200;

		return new DlmMatchedGraphHopperFactory(dlmPbfPath,
			graphhopperDlmConfigurationProperties.getCacheVerzeichnis(),
			graphhopperDlmConfigurationProperties.getMappingCacheVerzeichnis(),
			List.of(profileBike, profileCustomBike, profileFoot),
			minNetworkSize,
			graphhopperDlmConfigurationProperties.getElevationCacheVerzeichnis(),
			commonConfigurationProperties.getExterneResourcenBasisPfad()
				+ graphhopperDlmConfigurationProperties.getTiffTilesVerzeichnis()
		);
	}

	@Bean
	public OsmAuszeichnungsService osmAuszeichnungsService() {
		return new OsmAuszeichnungsService(kantenRepository,
			osmPbfConfigurationProperties.getMinOsmWayCoverageForRadNETZ());
	}

	@Bean
	public SimpleMatchingService matchingFuerManuellerImportService() {
		return new SimpleMatchingService(
			org.springframework.data.util.Lazy.of(
				this::dlmMatchingRepository),
			osmMatchingKorrekturService());
	}

	@Bean
	PbfErstellungsRepository pbfErstellungsRepository() {
		return new PbfErstellungsRepositoryImpl(coordinateReferenceSystemConverter, entityManager, barriereRepository,
			kantenRepository);
	}

	@Bean
	public DlmPbfErstellungService dlmPbfErstellungService() {
		return new DlmPbfErstellungService(kantenRepository,
			pbfErstellungsRepository(),
			dlmConfigurationProperties,
			graphhopperDlmConfigurationProperties.getDlmBasisDaten());
	}

	@Lazy
	@Bean
	public GraphhopperRoutingRepository graphhopperRoutingRepository() {
		return new GraphhopperRoutingRepositoryImpl(dlmMatchedGraphHopperFactory(), coordinateReferenceSystemConverter,
			customRoutingProfileRepository);
	}

	@Bean
	public OsmAbbildungsFehlerService osmAbbildungsFehlerService() {
		return new OsmAbbildungsFehlerService(osmAbbildungsFehlerRepository);
	}

	@Bean
	public CustomRoutingProfileService customRoutingProfileService() {
		return new CustomRoutingProfileService(customRoutingProfileRepository);
	}

	@Bean
	public CustomRoutingProfileGuard customRoutingProfileGuard() {
		return new CustomRoutingProfileGuard(benutzerResolver);
	}

	@Bean
	public KanteUpdateElevationService updateElevationService() {
		return new KanteUpdateElevationServiceImpl(
			kantenRepository,
			graphhopperDlmConfigurationProperties.getElevationCacheVerzeichnis(),
			commonConfigurationProperties.getExterneResourcenBasisPfad()
				+ graphhopperDlmConfigurationProperties.getTiffTilesVerzeichnis()
		);
	}
}
