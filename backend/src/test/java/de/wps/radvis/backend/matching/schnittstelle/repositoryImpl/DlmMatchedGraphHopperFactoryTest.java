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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.spy;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;

class DlmMatchedGraphHopperFactoryTest {
	private DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory;
	private GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties;

	@Mock
	private GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties;

	@Mock
	private OsmPbfConfigurationProperties osmPbfConfigurationProperties;

	@Mock
	private CommonConfigurationProperties commonConfigurationProperties;

	private MatchingConfiguration graphhopperServiceConfiguration;

	@TempDir
	public File temp;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		Mockito.when(commonConfigurationProperties.getExterneResourcenBasisPfad()).thenReturn("");

		File tiffTile = new File(temp, "/test-tiff-tiles");
		tiffTile.mkdir();

		graphhopperDlmConfigurationProperties = new GraphhopperDlmConfigurationProperties(
			"src/test/resources/test_freiburg.osm.pbf",
			new File(temp, "test-routing-graph-cache").getAbsolutePath(),
			new File(temp, "test-mapping-graph-cache").getAbsolutePath(), 1d,
			new File(temp, "/test-elevation-cache").getAbsolutePath(),
			tiffTile.getAbsolutePath());

		graphhopperServiceConfiguration = new MatchingConfiguration(
			new GeoConverterConfiguration(commonConfigurationProperties),
			commonConfigurationProperties,
			graphhopperOsmConfigurationProperties,
			osmPbfConfigurationProperties,
			graphhopperDlmConfigurationProperties);

		dlmMatchedGraphHopperFactory = spy(graphhopperServiceConfiguration.dlmMatchedGraphHopperFactory());
	}

	@Test
	void test_getReturnsSameEntity() {
		DlmMatchedGraphHopper reference1 = dlmMatchedGraphHopperFactory.getDlmGraphHopper();
		DlmMatchedGraphHopper reference2 = dlmMatchedGraphHopperFactory.getDlmGraphHopper();

		assertThat(reference1).isNotNull();
		assertThat(reference2).isNotNull();
		assertThat(reference1).isSameAs(reference2);
	}

	@Test
	void test_getReturnsDifferentEntityAfterUpdate() {
		DlmMatchedGraphHopper reference1 = dlmMatchedGraphHopperFactory.getDlmGraphHopper();
		dlmMatchedGraphHopperFactory.updateDlmGraphHopper();
		DlmMatchedGraphHopper reference2 = dlmMatchedGraphHopperFactory.getDlmGraphHopper();

		assertThat(reference1).isNotNull();
		assertThat(reference2).isNotNull();
		assertThat(reference1).isNotSameAs(reference2);
	}

	@Test
	void test_initialGet_erstelltCache() {
		dlmMatchedGraphHopperFactory.getDlmGraphHopper();

		File routingCache = new File(graphhopperDlmConfigurationProperties.getCacheVerzeichnis());
		assertThat(routingCache.exists()).isTrue();
		File mappingCache = new File(graphhopperDlmConfigurationProperties.getMappingCacheVerzeichnis());
		assertThat(mappingCache.exists()).isTrue();
	}
}
