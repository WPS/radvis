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

package de.wps.radvis.backend.matching.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingCacheRepository;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;

class DlmMatchedGraphHopperTest {

	@Mock
	private GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties;

	@Mock
	private OsmPbfConfigurationProperties osmPbfConfigurationProperties;

	@Mock
	private CommonConfigurationProperties commonConfigurationProperties;

	@InjectMocks
	private GeoConverterConfiguration geoConverterConfiguration;

	@TempDir
	public File temp;

	private MatchingConfiguration graphhopperServiceConfiguration;
	private GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties;

	private File mappingCacheVerzeichnis;
	private DlmMatchedGraphHopperFactory graphHopperFactory;

	@BeforeEach
	public void setUp() throws Exception {

		MockitoAnnotations.openMocks(this);

		final String absolutePath = new File(temp, "/dlm/test-routing-graph-cache").getAbsolutePath();
		mappingCacheVerzeichnis = new File(temp, "/dlm/test-mapping-graph-cache");
		Mockito.when(commonConfigurationProperties.getExterneResourcenBasisPfad()).thenReturn("");

		File tiffTile = new File(temp, "/test-tiff-tiles");
		tiffTile.mkdir();

		graphhopperDlmConfigurationProperties = new GraphhopperDlmConfigurationProperties(
			"src/test/resources/test_small.osm.pbf",
			absolutePath, mappingCacheVerzeichnis.getAbsolutePath(), 1d,
			new File(temp, "/test-elevation-cache").getAbsolutePath(),
			tiffTile.getAbsolutePath());
		graphhopperServiceConfiguration = new MatchingConfiguration(
			geoConverterConfiguration,
			commonConfigurationProperties,
			graphhopperOsmConfigurationProperties,
			osmPbfConfigurationProperties,
			graphhopperDlmConfigurationProperties);

		graphHopperFactory = graphhopperServiceConfiguration.dlmMatchedGraphHopperFactory();
	}

	@AfterEach
	public void tearDown() throws IOException {
		DlmMatchingCacheRepository repository = graphHopperFactory.getDlmMatchingCacheRepository();
		if (repository != null) {
			repository.deleteAll();
		}

		deleteRecursively(mappingCacheVerzeichnis.getAbsolutePath());
		deleteGraphhopperCache();
	}

	@Test
	void wederGraphhopperCacheNochMappingCache_beidesWirdErzeugt() {
		DlmMatchedGraphHopperFactory graphHopperFactory = graphhopperServiceConfiguration
			.dlmMatchedGraphHopperFactory();
		graphHopperFactory.getDlmGraphHopper();

		// assert
		assertThat(Files.exists(Path.of(graphhopperDlmConfigurationProperties.getCacheVerzeichnis()))).isTrue();
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().hasCache()).isTrue();
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().getWayIds()).isNotEmpty();
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().getProfilEigenschaften()).isNotEmpty();
	}

	@Test
	void keinGraphhopperCacheAberSchonMappingCache_MappingCacheWirdUeberschrieben() throws IOException {
		// arrange
		LocalDateTime timestampBefore = graphHopperFactory.getDlmMatchingCacheRepository().getTimestamp();
		deleteGraphhopperCache();

		// act
		graphHopperFactory.updateDlmGraphHopper();

		// assert
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().hasCache()).isTrue();
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().getTimestamp()).isAfter(timestampBefore);
	}

	@Test
	void graphhopperCacheUndMappingCache_GraphhopperLaedtCacheUnveraendert() {
		// arrange
		graphHopperFactory.getDlmGraphHopper();
		LocalDateTime timestampBefore = graphHopperFactory.getDlmMatchingCacheRepository().getTimestamp();
		// act
		graphHopperFactory.getDlmGraphHopper().close();

		// assert
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().hasCache()).isTrue();
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().getWayIds()).isNotEmpty();
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().getProfilEigenschaften()).isNotEmpty();
		assertThat(graphHopperFactory.getDlmMatchingCacheRepository().getTimestamp()).isEqualTo(timestampBefore);
	}

	private void deleteGraphhopperCache() throws IOException {
		File graphhopperCacheVerzeichnis = new File(graphhopperDlmConfigurationProperties.getCacheVerzeichnis());
		if (graphhopperCacheVerzeichnis.exists()) {
			Arrays.stream(graphhopperCacheVerzeichnis.listFiles()).forEach(File::delete);
			graphhopperCacheVerzeichnis.delete();
		}
	}

	private void deleteRecursively(String verzeichnisPfad) throws IOException {
		File graphhopperCacheVerzeichnis = new File(verzeichnisPfad);
		if (graphhopperCacheVerzeichnis.exists()) {
			Arrays.stream(graphhopperCacheVerzeichnis.listFiles()).forEach(File::delete);
			graphhopperCacheVerzeichnis.delete();
		}
	}
}
