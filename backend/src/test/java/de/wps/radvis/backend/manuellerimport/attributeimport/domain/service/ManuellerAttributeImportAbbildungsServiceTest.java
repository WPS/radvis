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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportSession;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMappingTestDataProvider;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Severity;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class ManuellerAttributeImportAbbildungsServiceTest {

	private ManuellerAttributeImportAbbildungsService manuellerAttributeImportAbbildungsService;

	@Mock
	private InMemoryKantenRepository inMemoryKantenRepository;

	@Mock
	private SimpleMatchingService matchingService;

	@BeforeEach
	void setUp() {
		openMocks(this);

		InMemoryKantenRepositoryFactory factory = mock(InMemoryKantenRepositoryFactory.class);

		when(factory.create(any(MultiPolygon.class))).thenReturn(inMemoryKantenRepository);
		when(factory.create(any(Envelope.class), any(MultiPolygon.class))).thenReturn(inMemoryKantenRepository);

		this.manuellerAttributeImportAbbildungsService = new ManuellerAttributeImportAbbildungsService(factory,
			matchingService);
	}

	@Test
	void noLinestring_warn() {
		// arrange
		Polygon polygon = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(1, 1),
				new Coordinate(1, 0), new Coordinate(0, 0) });
		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(Polygon.class).buildFeatureType());
		f1.add(polygon);
		List<SimpleFeature> featuresInBereich = List.of(f1.buildFeature("id"));
		AbstractImportSession sessionMock = mock(AttributeImportSession.class);

		// act
		List<FeatureMapping> result = manuellerAttributeImportAbbildungsService.bildeFeaturesAb(featuresInBereich,
			sessionMock);

		// assert
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
		assertThat(result).isEmpty();
	}

	@Test
	void multilinestring_multiple_warn() {
		// arrange
		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(MultiLineString.class).buildFeatureType());
		LineString linestring = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(0, 1), new Coordinate(1, 1) });

		MultiLineString multiLineStringSingle = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiLineString(new LineString[] { linestring });
		f1.add(multiLineStringSingle);
		String keepId = "keep";
		SimpleFeature featureKeep = f1.buildFeature(keepId);

		String filteredId = "filter";
		MultiLineString multiLineStringMultiple = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiLineString(new LineString[] { linestring, linestring });
		f1.add(multiLineStringMultiple);
		SimpleFeature featureFiltered = f1.buildFeature(filteredId);

		List<SimpleFeature> featuresInBereich = List.of(featureFiltered, featureKeep);
		AbstractImportSession sessionMock = mock(AttributeImportSession.class);

		// act
		List<FeatureMapping> result = manuellerAttributeImportAbbildungsService.bildeFeaturesAb(featuresInBereich,
			sessionMock);

		// assert
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
		assertThat(result).hasSize(1);
	}

	@Test
	void testBildeAttibuteAb() {
		// arrange
		// diagonale verbundene Strecke
		Kante kante0 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 10, 10, QuellSystem.DLM)
			.id(0L)
			.build();

		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 20, QuellSystem.DLM)
			.id(1L)
			.build();

		Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(20, 20, 30, 30, QuellSystem.DLM)
			.id(2L)
			.build();

		SimpleFeature importedFeature1SchneidetKanten0Und1 = SimpleFeatureTestDataProvider.withLineString(
			new Coordinate(10, 0),
			new Coordinate(5, 5), // ab hier diagonale
			new Coordinate(20, 20));

		SimpleFeature importedFeature2SchneidetKanten2 = SimpleFeatureTestDataProvider.withLineString(
			new Coordinate(20, 20),
			new Coordinate(40, 40));

		SimpleFeature importedFeature3NichtGematcht = SimpleFeatureTestDataProvider.withLineString(
			new Coordinate(100, 100),
			new Coordinate(200, 200));

		List<SimpleFeature> features = List.of(
			importedFeature1SchneidetKanten0Und1,
			importedFeature2SchneidetKanten2,
			importedFeature3NichtGematcht);

		LineString ueberschneidung1 = GeometryTestdataProvider.createLineString(
			new Coordinate(5, 5),
			new Coordinate(20, 20));

		LineString ueberschneidung2 = GeometryTestdataProvider.createLineString(
			new Coordinate(20, 20),
			new Coordinate(30, 30));

		when(matchingService.matche(eq((LineString) importedFeature1SchneidetKanten0Und1.getDefaultGeometry()),
			any())).thenReturn(
				Optional
					.of(new OsmMatchResult(ueberschneidung1,
						List.of(OsmWayId.of(0L), OsmWayId.of(1L)))));

		when(inMemoryKantenRepository.findKantenById(Set.of(0L, 1L))).thenReturn(
			List.of(kante0, kante1));

		when(matchingService.matche(eq((LineString) importedFeature2SchneidetKanten2.getDefaultGeometry()),
			any())).thenReturn(
				Optional.of(new OsmMatchResult(ueberschneidung2, List.of(OsmWayId.of(2L)))));

		when(inMemoryKantenRepository.findKantenById(Set.of(2L))).thenReturn(
			List.of(kante2));
		when(matchingService.matche(eq((LineString) importedFeature3NichtGematcht.getDefaultGeometry()), any()))
			.thenReturn(
				Optional.empty());

		// act
		List<FeatureMapping> featureMappings = this.manuellerAttributeImportAbbildungsService.bildeFeaturesAb(
			features,
			new AttributeImportSession(BenutzerTestDataProvider.defaultBenutzer().build(),
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), List.of(),
				AttributeImportFormat.LUBW));

		// assert
		assertThat(featureMappings).hasSize(3);
		FeatureMapping featureMapping0 = featureMappings.get(0);

		assertThat(featureMapping0.getImportedLineString().getCoordinates())
			.containsExactly(((LineString) importedFeature1SchneidetKanten0Und1.getDefaultGeometry()).getCoordinates());

		assertThat(featureMapping0.getKantenAufDieGemappedWurde()).hasSize(2);
		assertThat(featureMapping0.getKantenAufDieGemappedWurde()).extracting(MappedGrundnetzkante::getKanteId)
			.containsExactly(0L, 1L);
		assertThat(featureMapping0.getKantenAufDieGemappedWurde())
			.extracting(MappedGrundnetzkante::getLinearReferenzierterAbschnitt)
			.containsExactly(LinearReferenzierterAbschnitt.of(0.5, 1),
				LinearReferenzierterAbschnitt.of(0, 1));

		FeatureMapping featureMapping1 = featureMappings.get(1);

		assertThat(featureMapping1.getImportedLineString().getCoordinates())
			.containsExactly(((LineString) importedFeature2SchneidetKanten2.getDefaultGeometry()).getCoordinates());

		assertThat(featureMapping1.getKantenAufDieGemappedWurde()).hasSize(1);
		assertThat(featureMapping1.getKantenAufDieGemappedWurde()).extracting(MappedGrundnetzkante::getKanteId)
			.containsExactly(2L);
		assertThat(featureMapping1.getKantenAufDieGemappedWurde())
			.extracting(MappedGrundnetzkante::getLinearReferenzierterAbschnitt)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 1));

		verify(inMemoryKantenRepository, times(2)).findKantenById(any());

		FeatureMapping featureMapping2 = featureMappings.get(2);

		assertThat(featureMapping2.getImportedLineString().getCoordinates())
			.containsExactly(((LineString) importedFeature3NichtGematcht.getDefaultGeometry()).getCoordinates());

		assertThat(featureMapping2.getKantenAufDieGemappedWurde()).isEmpty();
	}

	@Test
	void testeRematchFeaturemapping() {
		// arrange
		// diagonale verbundene Strecke
		Kante kante0 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 10, 10, QuellSystem.DLM)
			.id(0L)
			.build();

		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 20, QuellSystem.DLM)
			.id(1L)
			.build();

		FeatureMapping featureMapping = FeatureMappingTestDataProvider
			.withCoordinates(new Coordinate(5, 6),
				new Coordinate(17, 18))
			.build();

		LineString ueberschneidung1 = GeometryTestdataProvider.createLineString(
			new Coordinate(5, 5),
			new Coordinate(17, 17));

		when(matchingService.matche(eq(featureMapping.getImportedLineString()),
			any())).thenReturn(
				Optional.of(new OsmMatchResult(ueberschneidung1,
					List.of(OsmWayId.of(0L), OsmWayId.of(1L), OsmWayId.of(2L)))));

		when(inMemoryKantenRepository.findKantenById(
			Set.of(0L, 1L, 2L))).thenReturn(
				Stream.of(kante0, kante1, null).collect(Collectors.toList()));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100)).id(1L).build();

		// act
		FeatureMapping result = this.manuellerAttributeImportAbbildungsService.rematchFeaturemapping(featureMapping,
			organisation);

		// assert
		assertThat(result.getImportedLineString().getCoordinates())
			.containsExactly(featureMapping.getImportedLineString().getCoordinates());

		assertThat(result.getKantenAufDieGemappedWurde()).hasSize(2);
		assertThat(result.getKantenAufDieGemappedWurde()).extracting(MappedGrundnetzkante::getKanteId)
			.containsExactly(0L, 1L);
		assertThat(result.getKantenAufDieGemappedWurde())
			.extracting(MappedGrundnetzkante::getLinearReferenzierterAbschnitt)
			.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0.5, 1),
				LinearReferenzierterAbschnitt.of(0, 0.7));

		verify(inMemoryKantenRepository, times(1)).findKantenById(
			Set.of(0L, 1L, 2L));
	}

	@Test
	void testeRematchFeaturemapping_featureMussLeerSein() {

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100)).id(1L).build();

		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 20, QuellSystem.DLM)
			.id(1L)
			.build();

		FeatureMapping featureMapping = FeatureMappingTestDataProvider
			.withCoordinates(new Coordinate(5, 6),
				new Coordinate(17, 18))
			.build();

		featureMapping.add(new MappedGrundnetzkante(kante1.getGeometry(), kante1.getId(), kante1.getGeometry()));

		// act + assert
		assertThatThrownBy(
			() -> this.manuellerAttributeImportAbbildungsService.rematchFeaturemapping(featureMapping, organisation))
				.isInstanceOf(RequireViolation.class);
	}

	@Nested
	class InMemoryRepositoryIntegrationTest {
		private InMemoryKantenRepositoryFactory factory;

		private ManuellerAttributeImportAbbildungsService manuellerAttributeImportAbbildungsService;

		@Mock
		private KantenRepository kantenRepository;

		@Mock
		private SimpleMatchingService simpleMatchingService;

		@BeforeEach
		void setUp() {
			MockitoAnnotations.openMocks(this);

			factory = new InMemoryKantenRepositoryFactory(kantenRepository);

			manuellerAttributeImportAbbildungsService = new ManuellerAttributeImportAbbildungsService(
				factory, simpleMatchingService);
		}

		@Test
		void testeRematcheOnlyKantenImOrganisationsbereich() {
			// arrange
			// diagonale verbundene Strecke
			Kante kante0 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 10, 10, QuellSystem.DLM)
				.id(0L)
				.build();

			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 20, QuellSystem.DLM)
				.id(1L)
				.build();

			Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(20, 20, 30, 30, QuellSystem.DLM)
				.id(2L)
				.build();

			FeatureMapping featureMapping = FeatureMappingTestDataProvider.withCoordinates(new Coordinate(5, 6),
				new Coordinate(17, 18),
				new Coordinate(25, 26)).build();

			LineString ueberschneidung1 = GeometryTestdataProvider.createLineString(
				new Coordinate(5, 5),
				new Coordinate(17, 17),
				new Coordinate(25, 25));

			when(simpleMatchingService.matche(
				eq(featureMapping.getImportedLineString()),
				any())).thenReturn(
					Optional.of(new OsmMatchResult(ueberschneidung1,
						List.of(OsmWayId.of(0L), OsmWayId.of(1L), OsmWayId.of(2L)))));

			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 18, 18)).id(1L).build();

			Envelope searchEnvelope = featureMapping.getImportedLineString().getEnvelopeInternal();
			searchEnvelope.expandBy(MatchingKorrekturService.MAX_DISTANCE_TO_MATCHED_GEOMETRY);
			when(kantenRepository.getKantenimBereich(searchEnvelope)).thenReturn(Set.of(kante0, kante1, kante2));

			// Env[5.0 : 25.0, 6.0 : 26.0]

			// act
			FeatureMapping result = this.manuellerAttributeImportAbbildungsService.rematchFeaturemapping(featureMapping,
				organisation);

			// assert
			assertThat(result.getImportedLineString().getCoordinates())
				.containsExactly(featureMapping.getImportedLineString().getCoordinates());

			assertThat(result.getKantenAufDieGemappedWurde()).hasSize(2);
			assertThat(result.getKantenAufDieGemappedWurde()).extracting(MappedGrundnetzkante::getKanteId)
				.containsExactly(0L, 1L);
			assertThat(result.getKantenAufDieGemappedWurde())
				.extracting(MappedGrundnetzkante::getLinearReferenzierterAbschnitt)
				.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
				.containsExactly(LinearReferenzierterAbschnitt.of(0.5, 1),
					LinearReferenzierterAbschnitt.of(0, 1));
		}

	}

}
