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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.BikeFlagEncoder;
import com.graphhopper.routing.util.FootFlagEncoder;

import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopper;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchingCacheRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmMatchedGraphhopperTestdataProvider {

	private final static CoordinateReferenceSystemConverter COORDINATE_REFERENCE_SYSTEM_CONVERTER = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95))
	);

	public static DlmMatchingRepositoryImpl reimportWithKanten(List<List<Kante>> kanten, String cacheDirName,
		File tempDir) throws IOException {

		DlmMatchedGraphHopperFactory graphHopperFactoryMock = mock(DlmMatchedGraphHopperFactory.class);

		PbfErstellungsRepositoryImpl pbfErstellungsRepository = new PbfErstellungsRepositoryImpl(
			COORDINATE_REFERENCE_SYSTEM_CONVERTER, mock(BarriereRepository.class), mock(KantenRepository.class));

		File pbfFile = new File(tempDir, "test-fahrradrouten-import-toubiz.osm.pbf");
		pbfErstellungsRepository.writePbf(
			PbfErstellungsTestDataProvider.getEnvelopeToKantenStreamMap(kanten),
			pbfFile);
		when(graphHopperFactoryMock.getDlmGraphHopper()).thenReturn(
			dlmMatchedGraphHopper(pbfFile, cacheDirName, tempDir));

		return new DlmMatchingRepositoryImpl(
			graphHopperFactoryMock,
			COORDINATE_REFERENCE_SYSTEM_CONVERTER,
			0.6);
	}

	private static DlmMatchedGraphHopper dlmMatchedGraphHopper(File pbfFile, String cacheDirName, File tempDir) {
		File mappingDir = new File(tempDir, "target/dlm/mapping-cache");
		if (!mappingDir.exists()) {
			mappingDir.mkdirs();
		}
		DlmMatchedGraphHopper graphHopper = new DlmMatchedGraphHopper(
			new DlmMatchingCacheRepositoryImpl(mappingDir.getAbsolutePath()));
		graphHopper.setOSMFile(pbfFile.getAbsolutePath());
		File routingDir = new File(tempDir, "target/" + cacheDirName + "/routing-cache");
		if (!routingDir.exists()) {
			routingDir.mkdirs();
		}
		graphHopper.setGraphHopperLocation(routingDir.getAbsolutePath());
		graphHopper.getEncodingManagerBuilder().add(new BikeFlagEncoder());
		graphHopper.getEncodingManagerBuilder().add(new FootFlagEncoder());
		File directory = new File(new File(tempDir, "target/" + cacheDirName + "/matching-cache").getAbsolutePath());
		if (!directory.exists()) {
			directory.mkdirs();
		}
		Profile profileBike = new Profile("bike").setVehicle("bike").setWeighting("fastest").setTurnCosts(false);
		Profile profileFoot = new Profile("foot").setVehicle("foot").setWeighting("fastest").setTurnCosts(false);
		graphHopper.setProfiles(profileBike, profileFoot);
		graphHopper.setMinNetworkSize(0);
		graphHopper.importOrLoad();

		log.info("DLM-Graphopper ist initialisiert.");
		return graphHopper;
	}

	public static DlmMatchedGraphHopperFactory initializeFactoryForPBFFile(String pbfFile, String temp) {
		File tiffTile = new File(temp, "/test-tiff-tiles");
		tiffTile.mkdir();

		GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties = new GraphhopperDlmConfigurationProperties(
			pbfFile,
			new File(temp, "test-routing-graph-cache").getAbsolutePath(),
			new File(temp, "test-mapping-graph-cache").getAbsolutePath(), 0.6,
			new File(temp, "/test-elevation-cache").getAbsolutePath(),
			tiffTile.getAbsolutePath());

		String dlmPbfPath = graphhopperDlmConfigurationProperties.getDlmBasisDaten();
		Profile profileBike = new Profile("bike").setVehicle("bike").setWeighting("fastest").setTurnCosts(false);
		Profile profileFoot = new Profile("foot").setVehicle("foot").setWeighting("fastest").setTurnCosts(false);
		int minNetworkSize = 0;

		return new DlmMatchedGraphHopperFactory(dlmPbfPath,
			graphhopperDlmConfigurationProperties.getCacheVerzeichnis(),
			graphhopperDlmConfigurationProperties.getMappingCacheVerzeichnis(),
			List.of(profileBike, profileFoot),
			minNetworkSize,
			graphhopperDlmConfigurationProperties.getElevationCacheVerzeichnis(),
			tiffTile.getAbsolutePath()
		);
	}
}
