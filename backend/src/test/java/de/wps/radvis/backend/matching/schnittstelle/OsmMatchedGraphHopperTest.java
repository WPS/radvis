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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.OsmMatchingCacheRepository;

class OsmMatchedGraphHopperTest {

	private MatchingConfiguration graphhopperServiceConfiguration;

	private CommonConfigurationProperties commonConfigurationProperties;

	private GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties;

	@Mock
	private GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties;
	private OsmMatchingCacheRepository osmMatchingCacheRepository;

	private String mappingCacheVerzeichnis = "target/test-mapping-cache";

	@BeforeEach
	public void setUp() throws Exception {
		deleteRecursively(mappingCacheVerzeichnis);

		MockitoAnnotations.openMocks(this);
		ExtentProperty extent = new ExtentProperty(492846.960, 500021.252, 5400410.543, 5418644.476);
		commonConfigurationProperties = new CommonConfigurationProperties(
			"src/test/resources/",
			60,
			extent,
			null,
			"test",
			"https://radvis-dev.landbw.de/",
			"DLM", "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources");
		graphhopperOsmConfigurationProperties = new GraphhopperOsmConfigurationProperties(
			"target/test-routing-graph-cache", mappingCacheVerzeichnis, 1d);
		graphhopperServiceConfiguration = new MatchingConfiguration(
			new GeoConverterConfiguration(commonConfigurationProperties),
			commonConfigurationProperties,
			graphhopperOsmConfigurationProperties,
			new OsmPbfConfigurationProperties("test_angereichert.osm.pbf", "test_osm-basisnetz.osm.pbf",
				"src/test/resources/test_small.osm.pbf",
				"https://someurl.com", 0.0),
			graphhopperDlmConfigurationProperties);
		osmMatchingCacheRepository = graphhopperServiceConfiguration.osmMatchingCacheRepository();
		osmMatchingCacheRepository.deleteAll();
		deleteGraphhopperCache();
	}

	@AfterEach
	public void tearDown() {
		osmMatchingCacheRepository.deleteAll();
		deleteRecursively(mappingCacheVerzeichnis);
		deleteGraphhopperCache();
	}

	private void deleteGraphhopperCache() {
		File graphhopperCacheVerzeichnis = new File(graphhopperOsmConfigurationProperties.getCacheVerzeichnis());
		if (graphhopperCacheVerzeichnis.exists()) {
			Arrays.stream(graphhopperCacheVerzeichnis.listFiles()).forEach(File::delete);
			graphhopperCacheVerzeichnis.delete();
		}
	}

	private void deleteRecursively(String verzeichnisPfad) {
		File graphhopperCacheVerzeichnis = new File(verzeichnisPfad);
		if (graphhopperCacheVerzeichnis.exists()) {
			Arrays.stream(graphhopperCacheVerzeichnis.listFiles()).forEach(File::delete);
			graphhopperCacheVerzeichnis.delete();
		}
	}

	@Test
	void wederGraphhopperCacheNochMappingCache_beidesWirdErzeugt() {
		graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository);

		// assert
		assertThat(Files.exists(Path.of(graphhopperOsmConfigurationProperties.getCacheVerzeichnis()))).isTrue();
		assertThat(osmMatchingCacheRepository.hasCache()).isTrue();
		assertThat(osmMatchingCacheRepository.get()).isNotEmpty();
	}

	@Test
	void keinGraphhopperCacheAberSchonMappingCache_MappingCacheWirdUeberschrieben() throws IOException {
		// arrange
		graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository);
		LocalDateTime timestampBefore = osmMatchingCacheRepository.getTimestamp();
		deleteGraphhopperCache();

		// act
		graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository);

		// assert
		assertThat(osmMatchingCacheRepository.hasCache()).isTrue();
		assertThat(osmMatchingCacheRepository.getTimestamp()).isAfter(timestampBefore);
	}

	@Test
	void graphhopperCacheJedochKeinMappingCache_GraphhopperBrichtAb() {
		// arrange
		graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository);
		osmMatchingCacheRepository.deleteAll();

		// assert
		assertThrows(RuntimeException.class, () -> {
			// act
			graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository);
		});
	}

	@Test
	void graphhopperCacheUndMappingCache_GraphhopperLaedtCacheUnveraendert() {
		// arrange
		graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository);
		LocalDateTime timestampBefore = osmMatchingCacheRepository.getTimestamp();

		// act
		graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository);

		// assert
		assertThat(osmMatchingCacheRepository.hasCache()).isTrue();
		assertThat(osmMatchingCacheRepository.get()).isNotEmpty();
		assertThat(osmMatchingCacheRepository.getTimestamp()).isEqualTo(timestampBefore);
	}
}
