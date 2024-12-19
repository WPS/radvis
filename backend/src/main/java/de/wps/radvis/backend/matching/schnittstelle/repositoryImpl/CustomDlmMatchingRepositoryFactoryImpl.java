/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.beans.factory.BeanInitializationException;

import com.graphhopper.config.Profile;

import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.repository.CustomDlmMatchingRepositoryFactory;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;

public class CustomDlmMatchingRepositoryFactoryImpl implements CustomDlmMatchingRepositoryFactory {
	private GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties;
	private CommonConfigurationProperties commonConfigurationProperties;
	private CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	public CustomDlmMatchingRepositoryFactoryImpl(
		GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties,
		CommonConfigurationProperties commonConfigurationProperties,
		CoordinateReferenceSystemConverter coordinateReferenceSystemConverter) {
		this.graphhopperDlmConfigurationProperties = graphhopperDlmConfigurationProperties;
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.coordinateReferenceSystemConverter = coordinateReferenceSystemConverter;
	}

	@Override
	public DlmMatchingRepository createCustomMatchingRepository(File dlmPbfFile) {
		DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory = newGraphHopperFactory(dlmPbfFile);
		return new DlmMatchingRepositoryImpl(dlmMatchedGraphHopperFactory, coordinateReferenceSystemConverter,
			graphhopperDlmConfigurationProperties.getMeasurementErrorSigma());
	}

	private DlmMatchedGraphHopperFactory newGraphHopperFactory(File pbfFile) {
		Profile profileBike = new Profile("bike").setVehicle("bike").setWeighting("fastest").setTurnCosts(false);
		Profile profileFoot = new Profile("foot").setVehicle("foot").setWeighting("fastest").setTurnCosts(false);
		int minNetworkSize = 200;

		File tempRoutingCache;
		File tempMappingCache;
		try {
			tempRoutingCache = Files.createTempDirectory("dlm-reimport-graphhopper-routing-cache").toFile();
			tempMappingCache = Files.createTempDirectory("dlm-reimport-graphhopper-mapping-cache").toFile();
		} catch (IOException e) {
			throw new BeanInitializationException(
				"Temporäre Verzeichnisse für DLM-Reimport GraphHopper konnten nicht angelegt werden", e);
		}

		return new DlmMatchedGraphHopperFactory(
			pbfFile.getAbsolutePath(),
			tempRoutingCache.getAbsolutePath(),
			tempMappingCache.getAbsolutePath(),
			List.of(profileBike, profileFoot),
			minNetworkSize,
			graphhopperDlmConfigurationProperties.getElevationCacheVerzeichnis(),
			commonConfigurationProperties.getExterneResourcenBasisPfad() + graphhopperDlmConfigurationProperties
				.getTiffTilesVerzeichnis());
	}
}
