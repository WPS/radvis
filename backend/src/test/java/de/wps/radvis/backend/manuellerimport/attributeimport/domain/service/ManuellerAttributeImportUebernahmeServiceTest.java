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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMappingTestDataProvider;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.KantenMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributes;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryTestProvider;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

class ManuellerAttributeImportUebernahmeServiceTest {

	private static final Comparator<LinearReferenzierterAbschnitt> LR_COMPARATOR_WITH_TOLERANCE = LineareReferenzTestProvider
		.comparatorWithTolerance(
			0.003);

	private ManuellerAttributeImportUebernahmeService manuellerAttributeImportUebernahmeService;

	@Mock
	private InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory;

	@Mock
	private MappingService mappingService;

	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;

	@Mock
	private EntityManager entityManager;

	@BeforeEach
	void setup() {
		openMocks(this);
		this.manuellerAttributeImportUebernahmeService = new ManuellerAttributeImportUebernahmeService(
			inMemoryKantenRepositoryFactory, mappingService, entityManager);
	}

	@Test
	void testInvertMapping() {
		// Arrange
		FeatureMapping featureMapping1 = FeatureMappingTestDataProvider
			.withCoordinates(new Coordinate(0.1, 0), new Coordinate(120.1, 120))
			.build();

		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0.2, 80, 80., QuellSystem.DLM)
			.id(1L)
			.build();
		LineString feature1MatchedLineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(100, 100));

		featureMapping1.add(new MappedGrundnetzkante(kante1.getGeometry(), kante1.getId(), feature1MatchedLineString));

		Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(2L)
			.build();
		featureMapping1.add(new MappedGrundnetzkante(kante2.getGeometry(), kante2.getId(), feature1MatchedLineString));

		FeatureMapping featureMapping2 = FeatureMappingTestDataProvider
			.withCoordinates(new Coordinate(0.2, 0), new Coordinate(120.2, 120))
			.build();
		LineString feature2MatchedLineString = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(140, 140));

		featureMapping2.add(new MappedGrundnetzkante(kante2.getGeometry(), kante2.getId(), feature2MatchedLineString));

		Kante kante3 = KanteTestDataProvider.withCoordinatesAndQuelle(120, 120.2, 180, 180.2, QuellSystem.DLM)
			.id(3L)
			.build();
		featureMapping2.add(new MappedGrundnetzkante(kante3.getGeometry(), kante3.getId(), feature2MatchedLineString));

		FeatureMapping featureMapping3 = FeatureMappingTestDataProvider
			.withCoordinates(new Coordinate(200.2, 0), new Coordinate(120.2, 120))
			.build();

		List<FeatureMapping> featureMappings = List.of(
			featureMapping1,
			featureMapping2,
			featureMapping3);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante1, kante2, kante3));

		// act
		List<KantenMapping> result = manuellerAttributeImportUebernahmeService.invertMappingAndCreateMappedFeatures(
			featureMappings,
			inMemoryKantenRepository, new LUBWMapper());

		// assert
		assertThat(result).hasSize(3);
		assertThat(result).extracting(KantenMapping::getKante).containsExactly(kante1, kante2, kante3);
		assertThat(result.get(0).getMappedAttributes()).extracting(MappedAttributes::getLinearReferenzierterAbschnitt)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 1));

		assertThat(result.get(1).getMappedAttributes()).extracting(MappedAttributes::getLinearReferenzierterAbschnitt)
			.usingComparatorForType(LR_COMPARATOR_WITH_TOLERANCE, LinearReferenzierterAbschnitt.class)
			.containsExactlyInAnyOrder(LinearReferenzierterAbschnitt.of(0, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 1));

		assertThat(result.get(2).getMappedAttributes()).extracting(MappedAttributes::getLinearReferenzierterAbschnitt)
			.usingComparatorForType(LR_COMPARATOR_WITH_TOLERANCE, LinearReferenzierterAbschnitt.class)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.33));
	}

	@Test
	void testInvertMapping_attributierterSeitenbezug_linksPropertyGesetzt_getLinks() {
		// Arrange
		FeatureMapping featureMapping1 = FeatureMappingTestDataProvider
			.withCoordinates(new Coordinate(0.1, 0), new Coordinate(120.1, 120))
			.properties(Map.of(
				"seite", "LINKS"
			))
			.build();

		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0.2, 80, 80., QuellSystem.DLM)
			.id(1L)
			.build();
		LineString feature1MatchedLineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(100, 100));

		featureMapping1.add(new MappedGrundnetzkante(kante1.getGeometry(), kante1.getId(), feature1MatchedLineString));

		List<FeatureMapping> featureMappings = List.of(featureMapping1);

		InMemoryKantenRepository inMemoryKantenRepository = InMemoryKantenRepositoryTestProvider.withKanten(
			Set.of(kante1));

		// act
		List<KantenMapping> result = manuellerAttributeImportUebernahmeService.invertMappingAndCreateMappedFeatures(
			featureMappings,
			inMemoryKantenRepository, new RadVISMapper(verwaltungseinheitService));

		// assert
		assertThat(result).hasSize(1);
		assertThat(result).extracting(KantenMapping::getKante).containsExactly(kante1);
		List<MappedAttributes> normalizedMappedAttributesLinks = result.get(0).getNormalizedMappedAttributesLinks();
		assertThat(normalizedMappedAttributesLinks).hasSize(1);
		assertThat(normalizedMappedAttributesLinks).extracting(MappedAttributes::getSeitenbezug)
			.containsExactly(Seitenbezug.LINKS);
		List<MappedAttributes> normalizedMappedAttributesRechts = result.get(0).getNormalizedMappedAttributesRechts();
		assertThat(normalizedMappedAttributesRechts).isEmpty();
	}
}