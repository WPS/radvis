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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.graphhopper.config.Profile;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezug;
import de.wps.radvis.backend.barriere.domain.entity.provider.BarriereTestDataProvider;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.barriere.domain.valueObject.BarrierenForm;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.repository.CustomRoutingProfileRepository;
import de.wps.radvis.backend.matching.domain.repository.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.repository.OsmMatchingCacheRepository;
import de.wps.radvis.backend.matching.domain.repository.PbfErstellungsRepository;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;

class PbfErstellungsRepositoryImplTest {

	private final CoordinateReferenceSystemConverter converter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95))
	);

	PbfErstellungsRepository pbfErstellungsRepository;

	@Mock
	private BarriereRepository barriereRepository;

	@Mock
	private KantenRepository kantenRepository;

	@TempDir
	public File tempDir;

	@Mock
	private OsmMatchingCacheRepository osmMatchingCacheRepository;

	private final Comparator<Coordinate> lenientCoordinateComparator = (coor1, coor2) -> coor1.distance(coor2)
		< 0.000002 ? 0 : -1;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		pbfErstellungsRepository = new PbfErstellungsRepositoryImpl(converter, barriereRepository, kantenRepository);
	}

	@Test
	public void testWritePbf() throws IOException {
		//arrange
		Stream<Kante> kanten = Stream.of(
			KanteTestDataProvider
				.withCoordinatesAndQuelle(452164.8506, 5390542.25685, 453231.8178, 5391128.56334, QuellSystem.DLM)
				.id(111111L)
				.build(),
			KanteTestDataProvider
				.withCoordinatesAndQuelle(452100.8506, 5390542.25685, 453200.8178, 5391128.56334, QuellSystem.DLM)
				.id(222222L)
				.build()
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		//act
		pbfErstellungsRepository.writePbf(Map.of(new Envelope(452164, 453232, 5390542, 5391129), kanten), pbfFile);

		//assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());
			// Wir haben 2 Ways und 4 Nodes
			assertThat(list).hasSize(6);
			// Die Ids an den Ways sollen die Ids der Kanten sein
			assertThat(list).filteredOn(
				entityContainer -> entityContainer.getType().equals(EntityType.Way))
				.extracting(entityContainer -> entityContainer.getEntity().getId())
				.containsExactlyInAnyOrder(111111L, 222222L);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testWritePbf_zweiKantenMitGleichemInnerenStuetzpunkt_KeineGemeinsamenNodes() throws IOException {
		//arrange

		Coordinate[] coordinatesKante1 = new Coordinate[] {
			new Coordinate(452100.8506, 5390542.25685),
			new Coordinate(452102.55, 5390544.6),
			new Coordinate(452104.5, 5390546.2)
		};

		Coordinate[] coordinatesKante2 = new Coordinate[] {
			new Coordinate(452110.8506, 5390532.25685),
			new Coordinate(452102.55, 5390544.6),
			new Coordinate(452094.5, 5390536.2)
		};

		Stream<Kante> kanten = Stream.of(
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante1))
				.id(111111L)
				.build(),
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante2))
				.id(222222L)
				.build()
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		//act
		pbfErstellungsRepository.writePbf(
			Map.of(GeometryTestdataProvider.createMultiLineString(
				coordinatesKante1, coordinatesKante2).getEnvelopeInternal(),
				kanten),
			pbfFile);

		//assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());
			// Wir haben 2 Ways und 6 Nodes
			assertThat(list).hasSize(8);
			assertThat(list).filteredOn(entityContainer -> entityContainer.getType().equals(EntityType.Node))
				.extracting(this::getCoordinateOfNode)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyInAnyOrderElementsOf(
					Stream.concat(
						Arrays.stream(coordinatesKante1),
						Arrays.stream(coordinatesKante2)
					)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			// Die Ids an den Ways sollen die Ids der Kanten sein
			assertThat(list).filteredOn(
				entityContainer -> entityContainer.getType().equals(EntityType.Way))
				.extracting(entityContainer -> entityContainer.getEntity().getId())
				.containsExactlyInAnyOrder(111111L, 222222L);

			// Die Nodes der Ways sollen korrekt sein
			List<Coordinate> coordinatesOfWayFromKante1 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 111111L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante1)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante1)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			List<Coordinate> coordinatesOfWayFromKante2 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 222222L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante2)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante2)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testWritePbf_zweiKantenMitGleichemAeusseremStuetzpunkt_EineGemeinsameNode() throws IOException {
		//arrange

		Coordinate[] coordinatesKante1 = new Coordinate[] {
			new Coordinate(452105.8506, 5390532.25685),
			new Coordinate(452102.55, 5390544.6),
			new Coordinate(452104.5, 5390546.2)
		};

		Coordinate[] coordinatesKante2 = new Coordinate[] {
			new Coordinate(452105.8506, 5390532.25685),
			new Coordinate(452100.55, 5390534.6),
			new Coordinate(452094.5, 5390536.2)
		};

		Stream<Kante> kanten = Stream.of(
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante1))
				.id(111111L)
				.build(),
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante2))
				.id(222222L)
				.build()
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		//act
		pbfErstellungsRepository.writePbf(
			Map.of(GeometryTestdataProvider.createMultiLineString(coordinatesKante1, coordinatesKante2)
				.getEnvelopeInternal(),
				kanten),
			pbfFile);

		//assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());
			// Wir haben 2 Ways und 5 Nodes
			assertThat(list).hasSize(7);
			assertThat(list).filteredOn(entityContainer -> entityContainer.getType().equals(EntityType.Node))
				.extracting(this::getCoordinateOfNode)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyInAnyOrderElementsOf(
					Stream.concat(
						Arrays.stream(coordinatesKante1),
						// der erste Stützpunkt ist beiden Kanten gemeinsam
						Arrays.stream(Arrays.copyOfRange(coordinatesKante2, 1, 3))
					)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			// Die Ids an den Ways sollen die Ids der Kanten sein
			assertThat(list).filteredOn(
				entityContainer -> entityContainer.getType().equals(EntityType.Way))
				.extracting(entityContainer -> entityContainer.getEntity().getId())
				.containsExactlyInAnyOrder(111111L, 222222L);

			// Die Nodes der Ways sollen korrekt sein
			List<Coordinate> coordinatesOfWayFromKante1 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 111111L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante1)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante1)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			List<Coordinate> coordinatesOfWayFromKante2 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 222222L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante2)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante2)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testWritePbf_zweiKantenStreckeUeber2PartitionenMitVerbindungInErsterPartition() throws IOException {
		//arrange

		Coordinate[] coordinatesKante1 = new Coordinate[] {
			new Coordinate(450000, 5390000),
			new Coordinate(450100, 5390000),
			new Coordinate(450200, 5390000)
		};

		Coordinate[] coordinatesKante2 = new Coordinate[] {
			new Coordinate(450200, 5390000),
			new Coordinate(450300, 5390000),
			new Coordinate(450400, 5390000)
		};

		Envelope partition1 = new Envelope(440000, 450250, 5380000, 5400000);
		Envelope partition2 = new Envelope(450250, 460000, 5380000, 5400000);

		Kante kante1 = KanteTestDataProvider
			.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.geometry(GeometryTestdataProvider.createLineString(coordinatesKante1))
			.id(111111L)
			.build();
		Kante kante2 = KanteTestDataProvider
			.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.geometry(GeometryTestdataProvider.createLineString(coordinatesKante2))
			.id(222222L)
			.build();

		Stream<Kante> kantenPartition1 = Stream.of(
			kante1,
			kante2
		);

		Stream<Kante> kantenPartition2 = Stream.of(
			kante2
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		//act
		pbfErstellungsRepository.writePbf(
			Map.of(partition1, kantenPartition1, partition2, kantenPartition2),
			pbfFile);

		//assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());
			// Wir haben 2 Ways und 5 Nodes
			assertThat(list).hasSize(7);
			assertThat(list).filteredOn(entityContainer -> entityContainer.getType().equals(EntityType.Node))
				.extracting(this::getCoordinateOfNode)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyInAnyOrderElementsOf(
					Stream.concat(
						Arrays.stream(coordinatesKante1),
						// der erste Stützpunkt ist beiden Kanten gemeinsam
						Arrays.stream(Arrays.copyOfRange(coordinatesKante2, 1, 3))
					)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			// Die Ids an den Ways sollen die Ids der Kanten sein
			assertThat(list).filteredOn(
				entityContainer -> entityContainer.getType().equals(EntityType.Way))
				.extracting(entityContainer -> entityContainer.getEntity().getId())
				.containsExactlyInAnyOrder(111111L, 222222L);

			// Die Nodes der Ways sollen korrekt sein
			List<Coordinate> coordinatesOfWayFromKante1 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 111111L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante1)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante1)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			List<Coordinate> coordinatesOfWayFromKante2 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 222222L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante2)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante2)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testWritePbf_zweiKantenStreckeUeber2PartitionenMitVerbindungAufPartitionsgrenze() throws IOException {
		//arrange

		Coordinate[] coordinatesKante1 = new Coordinate[] {
			new Coordinate(450000, 5390000),
			new Coordinate(450100, 5390000),
			new Coordinate(450200, 5390000)
		};

		Coordinate[] coordinatesKante2 = new Coordinate[] {
			new Coordinate(450200, 5390000),
			new Coordinate(450300, 5390000),
			new Coordinate(450400, 5390000)
		};

		Envelope partition1 = new Envelope(440000, 450200, 5380000, 5400000);
		Envelope partition2 = new Envelope(450200, 460000, 5380000, 5400000);

		Kante kante1 = KanteTestDataProvider
			.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.geometry(GeometryTestdataProvider.createLineString(coordinatesKante1))
			.id(111111L)
			.build();
		Kante kante2 = KanteTestDataProvider
			.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.geometry(GeometryTestdataProvider.createLineString(coordinatesKante2))
			.id(222222L)
			.build();

		Stream<Kante> kantenPartition1 = Stream.of(
			kante1,
			kante2
		);

		Stream<Kante> kantenPartition2 = Stream.of(
			kante2
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		//act
		pbfErstellungsRepository.writePbf(
			Map.of(partition1, kantenPartition1, partition2, kantenPartition2),
			pbfFile);

		//assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());
			// Wir haben 2 Ways und 5 Nodes
			assertThat(list).hasSize(7);
			assertThat(list).filteredOn(entityContainer -> entityContainer.getType().equals(EntityType.Node))
				.extracting(this::getCoordinateOfNode)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyInAnyOrderElementsOf(
					Stream.concat(
						Arrays.stream(coordinatesKante1),
						// der erste Stützpunkt ist beiden Kanten gemeinsam
						Arrays.stream(Arrays.copyOfRange(coordinatesKante2, 1, 3))
					)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			// Die Ids an den Ways sollen die Ids der Kanten sein
			assertThat(list).filteredOn(
				entityContainer -> entityContainer.getType().equals(EntityType.Way))
				.extracting(entityContainer -> entityContainer.getEntity().getId())
				.containsExactlyInAnyOrder(111111L, 222222L);

			// Die Nodes der Ways sollen korrekt sein
			List<Coordinate> coordinatesOfWayFromKante1 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 111111L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante1)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante1)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			List<Coordinate> coordinatesOfWayFromKante2 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 222222L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante2)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante2)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testWritePbf_zweiKantenStreckeUeber3PartitionenMitVerbindungInDritterPartition() throws IOException {
		//arrange

		Coordinate[] coordinatesKante1 = new Coordinate[] {
			new Coordinate(450000, 5390000),
			new Coordinate(450100, 5390000),
			new Coordinate(450600, 5390000)
		};

		Coordinate[] coordinatesKante2 = new Coordinate[] {
			new Coordinate(450600, 5390000),
			new Coordinate(450700, 5390000),
			new Coordinate(450800, 5390000)
		};

		Envelope partition1 = new Envelope(440000, 450200, 5380000, 5400000);
		Envelope partition2 = new Envelope(450200, 450400, 5380000, 5400000);
		Envelope partition3 = new Envelope(450400, 460000, 5380000, 5400000);

		Kante kante1 = KanteTestDataProvider
			.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.geometry(GeometryTestdataProvider.createLineString(coordinatesKante1))
			.id(111111L)
			.build();
		Kante kante2 = KanteTestDataProvider
			.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.geometry(GeometryTestdataProvider.createLineString(coordinatesKante2))
			.id(222222L)
			.build();

		Stream<Kante> kantenPartition1 = Stream.of(
			kante1
		);

		Stream<Kante> kantenPartition2 = Stream.of(
			kante1
		);

		Stream<Kante> kantenPartition3 = Stream.of(
			kante1,
			kante2
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		//act
		pbfErstellungsRepository.writePbf(
			Map.of(
				partition1, kantenPartition1,
				partition2, kantenPartition2,
				partition3, kantenPartition3),
			pbfFile);

		//assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());
			// Wir haben 2 Ways und 5 Nodes
			assertThat(list).hasSize(7);
			assertThat(list).filteredOn(entityContainer -> entityContainer.getType().equals(EntityType.Node))
				.extracting(this::getCoordinateOfNode)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyInAnyOrderElementsOf(
					Stream.concat(
						Arrays.stream(coordinatesKante1),
						// der erste Stützpunkt ist beiden Kanten gemeinsam
						Arrays.stream(Arrays.copyOfRange(coordinatesKante2, 1, 3))
					)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			// Die Ids an den Ways sollen die Ids der Kanten sein
			assertThat(list).filteredOn(
				entityContainer -> entityContainer.getType().equals(EntityType.Way))
				.extracting(entityContainer -> entityContainer.getEntity().getId())
				.containsExactlyInAnyOrder(111111L, 222222L);

			// Die Nodes der Ways sollen korrekt sein
			List<Coordinate> coordinatesOfWayFromKante1 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 111111L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante1)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante1)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			List<Coordinate> coordinatesOfWayFromKante2 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 222222L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante2)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante2)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testWritePbf_zweiKantenStreckeUeber2PartitionenMitVerbindungInZweiterPartition() throws IOException {
		//arrange

		Coordinate[] coordinatesKante1 = new Coordinate[] {
			new Coordinate(450000, 5390000),
			new Coordinate(450100, 5390000),
			new Coordinate(450200, 5390000)
		};

		Coordinate[] coordinatesKante2 = new Coordinate[] {
			new Coordinate(450200, 5390000),
			new Coordinate(450300, 5390000),
			new Coordinate(450400, 5390000)
		};

		Envelope partition1 = new Envelope(440000, 450150, 5380000, 5400000);
		Envelope partition2 = new Envelope(450150, 460000, 5380000, 5400000);

		Kante kante1 = KanteTestDataProvider
			.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.geometry(GeometryTestdataProvider.createLineString(coordinatesKante1))
			.id(111111L)
			.build();
		Kante kante2 = KanteTestDataProvider
			.withDefaultValues()
			.quelle(QuellSystem.DLM)
			.geometry(GeometryTestdataProvider.createLineString(coordinatesKante2))
			.id(222222L)
			.build();

		Stream<Kante> kantenPartition1 = Stream.of(
			kante1
		);

		Stream<Kante> kantenPartition2 = Stream.of(
			kante1,
			kante2
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		//act
		pbfErstellungsRepository.writePbf(
			Map.of(partition1, kantenPartition1, partition2, kantenPartition2),
			pbfFile);

		//assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());
			// Wir haben 2 Ways und 5 Nodes
			assertThat(list).hasSize(7);
			assertThat(list).filteredOn(entityContainer -> entityContainer.getType().equals(EntityType.Node))
				.extracting(this::getCoordinateOfNode)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyInAnyOrderElementsOf(
					Stream.concat(
						Arrays.stream(coordinatesKante1),
						// der erste Stützpunkt ist beiden Kanten gemeinsam
						Arrays.stream(Arrays.copyOfRange(coordinatesKante2, 1, 3))
					)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			// Die Ids an den Ways sollen die Ids der Kanten sein
			assertThat(list).filteredOn(
				entityContainer -> entityContainer.getType().equals(EntityType.Way))
				.extracting(entityContainer -> entityContainer.getEntity().getId())
				.containsExactlyInAnyOrder(111111L, 222222L);

			// Die Nodes der Ways sollen korrekt sein
			List<Coordinate> coordinatesOfWayFromKante1 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 111111L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante1)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante1)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

			List<Coordinate> coordinatesOfWayFromKante2 = list.stream()
				.filter(entityContainer -> entityContainer.getType().equals(EntityType.Way)
					&& entityContainer.getEntity().getId() == 222222L)
				.map(entityContainer -> getCoordinatesOfWay(list, entityContainer))
				.findFirst().get();

			assertThat(coordinatesOfWayFromKante2)
				.usingComparatorForType(lenientCoordinateComparator, Coordinate.class)
				.containsExactlyElementsOf(
					Arrays.stream(coordinatesKante2)
						.map(coordinate -> converter.transformCoordinateUnsafe(coordinate,
							KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
						.collect(Collectors.toList())
				);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testWritePbf_onewayTagsKorrektUndVonRoutingRespektiert() throws IOException,
		KeineRouteGefundenException {
		//arrange

		Coordinate[] coordinatesKante1 = new Coordinate[] {
			new Coordinate(452105.8506, 5390532.25685),
			new Coordinate(452007.55, 5390524.6),
			new Coordinate(451184.5, 5390506.2)
		};

		Coordinate[] coordinatesKante2 = new Coordinate[] {
			new Coordinate(452105.8506, 5390532.25685),
			new Coordinate(452100.55, 5390534.6),
			new Coordinate(452094.5, 5390536.2)
		};

		Coordinate[] coordinatesKante3 = new Coordinate[] {
			new Coordinate(452094.5, 5390536.2),
			new Coordinate(452104.5, 5390536.2),
			new Coordinate(452124.5, 5390536.2),
		};

		Coordinate[] coordinatesKante4 = new Coordinate[] {
			new Coordinate(452124.5, 5390536.2),
			new Coordinate(452224.5, 5390236.2),
			new Coordinate(452424.5, 5390536.2),
		};

		Coordinate[] coordinatesKante5 = new Coordinate[] {
			new Coordinate(452124.5, 5390536.2),
			new Coordinate(452324.5, 5390836.2),
			new Coordinate(452424.5, 5390536.2),
		};

		List<Kante> kanten = List.of(
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante1))
				.fahrtrichtungAttributGruppe(
					FahrtrichtungAttributGruppe.builder()
						.fahrtrichtungLinks(Richtung.GEGEN_RICHTUNG)
						.fahrtrichtungRechts(Richtung.GEGEN_RICHTUNG)
						.build()
				)
				.id(111111L)
				.build(),
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante2))
				.fahrtrichtungAttributGruppe(
					FahrtrichtungAttributGruppe.builder()
						.fahrtrichtungLinks(Richtung.BEIDE_RICHTUNGEN)
						.fahrtrichtungRechts(Richtung.BEIDE_RICHTUNGEN)
						.build()
				)
				.id(222222L)
				.build(),
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante3))
				.isZweiseitig(true)
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().isZweiseitig(true).build())
				.fahrtrichtungAttributGruppe(
					FahrtrichtungAttributGruppe.builder()
						.isZweiseitig(true)
						.fahrtrichtungLinks(Richtung.GEGEN_RICHTUNG)
						.fahrtrichtungRechts(Richtung.IN_RICHTUNG)
						.build()
				)
				.id(333333L)
				.build(),
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante4))
				.fahrtrichtungAttributGruppe(
					FahrtrichtungAttributGruppe.builder()
						.fahrtrichtungLinks(Richtung.IN_RICHTUNG)
						.fahrtrichtungRechts(Richtung.IN_RICHTUNG)
						.build()
				)
				.id(444444L)
				.build(),
			KanteTestDataProvider
				.withDefaultValues()
				.quelle(QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(coordinatesKante5))
				.fahrtrichtungAttributGruppe(
					FahrtrichtungAttributGruppe.builder()
						.fahrtrichtungLinks(Richtung.GEGEN_RICHTUNG)
						.fahrtrichtungRechts(Richtung.GEGEN_RICHTUNG)
						.build()
				)
				.id(555555L)
				.build()
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		//act
		pbfErstellungsRepository.writePbf(
			Map.of(GeometryTestdataProvider.createMultiLineString(
				coordinatesKante1, coordinatesKante2, coordinatesKante3,
				coordinatesKante4, coordinatesKante5).getEnvelopeInternal(),
				kanten.stream()),
			pbfFile);

		//assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());
			// Wir haben 5 Ways und 10 Nodes
			assertThat(list).hasSize(15);
			assertThat(list).filteredOn(
				entityContainer -> entityContainer.getType().equals(EntityType.Way))
				.extracting(
					entityContainer -> entityContainer.getEntity().getId(), this::getTags)
				.usingRecursiveFieldByFieldElementComparator()
				.containsExactlyInAnyOrder(
					tuple(111111L, getDefaultTagsWithOneway("-1")),
					tuple(222222L, getDefaultTagsWithOneway("no")),
					tuple(333333L, getDefaultTagsWithOneway("no")),
					tuple(444444L, getDefaultTagsWithOneway("yes")),
					tuple(555555L, getDefaultTagsWithOneway("-1"))
				);

			openMocks(this);
			DlmMatchedGraphHopper dlmMatchedGraphHopper = new DlmMatchedGraphHopper(
				new DlmMatchingCacheRepositoryImpl(tempDir.getAbsolutePath()));
			dlmMatchedGraphHopper.setOSMFile(pbfFile.getCanonicalPath());
			dlmMatchedGraphHopper.setMinNetworkSize(0);
			dlmMatchedGraphHopper.setGraphHopperLocation(tempDir.getCanonicalPath());
			dlmMatchedGraphHopper.setProfiles(
				new Profile("bike").setVehicle("bike").setWeighting("fastest").setTurnCosts(false));
			dlmMatchedGraphHopper.importOrLoad();
			DlmMatchedGraphHopperFactory factory = mock(DlmMatchedGraphHopperFactory.class);

			when(factory.getDlmGraphHopper()).thenReturn(dlmMatchedGraphHopper);

			GraphhopperRoutingRepository graphhopperRoutingRepository = new GraphhopperRoutingRepositoryImpl(factory,
				converter, mock(CustomRoutingProfileRepository.class));

			List<Coordinate> routeSteps = List.of(
				new Coordinate(451184.5, 5390506.2),
				new Coordinate(452105.8506, 5390532.25685),
				new Coordinate(452094.5, 5390536.2),
				new Coordinate(452124.5, 5390536.2),
				new Coordinate(452424.0, 5390536.1)
			);

			RoutingResult result = graphhopperRoutingRepository.route(routeSteps,
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, true);

			assertThat(result.getKantenIDs()).containsExactly(111111L, 222222L, 333333L, 444444L);

			List<Coordinate> routeSteps2 = List.of(
				new Coordinate(452094.5, 5390536.2),
				new Coordinate(452105.8506, 5390532.25685),
				new Coordinate(451184.5, 5390506.2)
			);

			assertThatExceptionOfType(KeineRouteGefundenException.class).isThrownBy(
				() -> graphhopperRoutingRepository.route(routeSteps2, GraphhopperRoutingRepository.DEFAULT_PROFILE_ID,
					true));
		}
	}

	@Test
	public void testWritePbf_barrierenTagsKorrekt() throws IOException {
		// Alle Kombinationen an Netzbezügen und Kanten-Konstellationen wären zu viele, daher nur ein paar use-cases um
		// zumindest alle Arten von Netzbezügen und ein paar Kombinationen zu behandeln.

		// Arrange
		Coordinate[] coordinatesKante1 = new Coordinate[] {
			new Coordinate(450010, 5390000),
			new Coordinate(450011, 5390001),
		};
		Coordinate[] coordinatesKante2 = new Coordinate[] {
			new Coordinate(450020, 5390000),
			new Coordinate(450021, 5390001),
		};
		Coordinate[] coordinatesKante3 = new Coordinate[] {
			new Coordinate(450030, 5390000),
			new Coordinate(450031, 5390001),
		};
		Coordinate[] coordinatesKante4 = new Coordinate[] {
			new Coordinate(450040, 5390000),
			new Coordinate(450041, 5390001),
		};
		Coordinate[] coordinatesKante5 = new Coordinate[] {
			new Coordinate(450050, 5390000),
			new Coordinate(450051, 5390001),
		};
		Coordinate[] coordinatesKante6 = new Coordinate[] {
			coordinatesKante5[1], // Kante 6 und 5 berühren sich hier (gemeinsamer Knoten)
			new Coordinate(450061, 5390002),
		};

		Kante kante1 = KanteTestDataProvider.withCoordinates(coordinatesKante1).id(1L).build();
		Kante kante2 = KanteTestDataProvider.withCoordinates(coordinatesKante2).id(2L).build();
		Kante kante3 = KanteTestDataProvider.withCoordinates(coordinatesKante3).id(3L).build();
		Kante kante4 = KanteTestDataProvider.withCoordinates(coordinatesKante4).id(4L).build();
		Kante kante5 = KanteTestDataProvider.withCoordinates(coordinatesKante5).id(5L).build();
		Kante kante6 = KanteTestDataProvider.withCoordinates(coordinatesKante6)
			.id(6L)
			.vonKnoten(kante5.getNachKnoten())
			.build();

		List<Kante> kanten = List.of(kante1, kante2, kante3, kante4, kante5, kante6);
		kanten.forEach(kante -> {
			when(kantenRepository.getAdjazenteKanten(eq(kante.getVonKnoten()))).thenReturn(List.of(kante));
			when(kantenRepository.getAdjazenteKanten(eq(kante.getNachKnoten()))).thenReturn(List.of(kante));
		});
		// Geteilter Knoten zwischen Kante 5 und 6:
		when(kantenRepository.getAdjazenteKanten(eq(kante6.getVonKnoten()))).thenReturn(List.of(kante5, kante6));

		final BarriereNetzBezug netzbezug2 = new BarriereNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(kante2, LinearReferenzierterAbschnitt.of(0, 0.5),
				Seitenbezug.BEIDSEITIG)),
			Collections.emptySet(),
			Collections.emptySet());

		final BarriereNetzBezug netzbezug3abschnitt = new BarriereNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(kante3, LinearReferenzierterAbschnitt.of(0, 0.5),
				Seitenbezug.RECHTS)),
			Collections.emptySet(),
			Collections.emptySet());
		final BarriereNetzBezug netzbezug3punkt = new BarriereNetzBezug(
			Collections.emptySet(),
			Set.of(new PunktuellerKantenSeitenBezug(kante3, LineareReferenz.of(0.75), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());

		final BarriereNetzBezug netzbezug4punktVorne = new BarriereNetzBezug(
			Collections.emptySet(),
			Set.of(new PunktuellerKantenSeitenBezug(kante4, LineareReferenz.of(0.1), Seitenbezug.RECHTS)),
			Collections.emptySet());
		final BarriereNetzBezug netzbezug4punktHinten = new BarriereNetzBezug(
			Collections.emptySet(),
			Set.of(new PunktuellerKantenSeitenBezug(kante4, LineareReferenz.of(0.9), Seitenbezug.LINKS)),
			Collections.emptySet());

		final BarriereNetzBezug netzbezug5 = new BarriereNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(kante5, LinearReferenzierterAbschnitt.of(0, 0.5),
				Seitenbezug.RECHTS)),
			Collections.emptySet(),
			Set.of(kante5.getNachKnoten()));

		final BarriereNetzBezug netzbezug6 = new BarriereNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(kante6.getVonKnoten())); // == kante5.getNachKnoten()

		when(barriereRepository.findAll()).thenReturn(List.of(
			// Kante 1: Keine Barriere
			// Kante 2: Linear referenzierte Barriere (beidseitig)
			BarriereTestDataProvider.withDefaultValues().id(200L)
				.netzbezug(netzbezug2)
				.barrierenForm(BarrierenForm.ANORDNUNG_VZ_220)
				.build(),
			// Kante 3: Linear referenzierte Barriere (einseitig) + punktuelle Barriere (beidseitig)
			BarriereTestDataProvider.withDefaultValues().id(301L)
				.netzbezug(netzbezug3abschnitt)
				.barrierenForm(BarrierenForm.SONSTIGE_BARRIERE)
				.build(),
			BarriereTestDataProvider.withDefaultValues().id(302L)
				.netzbezug(netzbezug3punkt)
				.barrierenForm(BarrierenForm.STEILE_RAMPE)
				.build(),
			// Kante 4: Zwei Barrieren mit punktuellem Netzbezug (einseitig entgegengesetzt = nicht in Konflikt)
			BarriereTestDataProvider.withDefaultValues().id(401L)
				.netzbezug(netzbezug4punktVorne)
				.barrierenForm(BarrierenForm.BAUM_BAUMSCHEIBE)
				.build(),
			BarriereTestDataProvider.withDefaultValues().id(402L)
				.netzbezug(netzbezug4punktHinten)
				.barrierenForm(BarrierenForm.VERKEHRSZEICHEN)
				.build(),
			// Kante 5: Abschnitt einseitig + Knotenbezug
			BarriereTestDataProvider.withDefaultValues().id(500L)
				.netzbezug(netzbezug5)
				.barrierenForm(BarrierenForm.SCHRANKE)
				.build(),
			// Kante 6: Knotenbezug (gleicher Knoten wie bei Kante 5)
			BarriereTestDataProvider.withDefaultValues().id(600L)
				.netzbezug(netzbezug6)
				.barrierenForm(BarrierenForm.VERSENKBARE_SPERRPFOSTEN)
				.build()
		));

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		// Act
		pbfErstellungsRepository.writePbf(
			Map.of(
				GeometryTestdataProvider.createMultiLineString(coordinatesKante1, coordinatesKante2, coordinatesKante3,
					coordinatesKante4, coordinatesKante5, coordinatesKante6).getEnvelopeInternal(),
				kanten.stream()
			),
			pbfFile
		);

		// Assert
		try (InputStream input = new FileInputStream(pbfFile)) {

			Iterable<EntityContainer> iterable = new PbfIterator(input, true);

			List<EntityContainer> list = StreamSupport.stream(iterable.spliterator(), false)
				.collect(Collectors.toList());

			// Wir haben 6 Ways und 11 Nodes (ein Knoten geteilt zwischen Kante 5 und 6)
			assertThat(list).hasSize(17);

			AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> listAssert = assertThat(
				list).filteredOn(
					entityContainer -> entityContainer.getType().equals(EntityType.Way))
					.extracting(entityContainer -> entityContainer.getEntity().getId(), this::getTags)
					.usingRecursiveFieldByFieldElementComparator();
			listAssert.hasSize(6);
			listAssert.contains(
				// 1: Keine Barriere = keine tags
				tuple(1L, getDefaultTags())
			);
			listAssert.contains(
				// 2: Beidseitiger Abschnitt ohne Konflikte mit anderen Barrieren = beidseitige tags
				tuple(2L, getDefaultTagsWithBarriere(
					BarrierenForm.ANORDNUNG_VZ_220.name(),
					BarrierenForm.ANORDNUNG_VZ_220.name()
				))
			);
			listAssert.contains(
				// 3: Einseitiger Abschnitt + beidseitiger Punkt = Abschnitt überschreibt Punkt auf einer Seite
				tuple(3L, getDefaultTagsWithBarriere(
					BarrierenForm.SONSTIGE_BARRIERE.name(),
					BarrierenForm.STEILE_RAMPE.name()
				))
			);
			listAssert.contains(
				// 4: Punkte auf entgegengesetzten Seiten = Kein Konflikt
				tuple(4L, getDefaultTagsWithBarriere(
					BarrierenForm.BAUM_BAUMSCHEIBE.name(),
					BarrierenForm.VERKEHRSZEICHEN.name()
				))
			);
			listAssert.contains(
				// 5: Knotenbezug + einseitiger Abschnitt = Abschnitt überschreibt Knoten-Barriere
				tuple(5L, getDefaultTagsWithBarriere(
					BarrierenForm.SCHRANKE.name(),
					BarrierenForm.VERSENKBARE_SPERRPFOSTEN.name()
				))
			);
			listAssert.contains(
				// 6: Geteilter Knoten mit Kante 5, sonst kein weiterer Netzbezug
				tuple(6L, getDefaultTagsWithBarriere(
					BarrierenForm.VERSENKBARE_SPERRPFOSTEN.name(),
					BarrierenForm.VERSENKBARE_SPERRPFOSTEN.name()
				))
			);
		}
	}

	@Test
	public void testWritePbf_outputKannVonGraphhopperEingelesenWerden() throws IOException {
		//arrange
		openMocks(this);
		Kante kante = KanteTestDataProvider
			.withCoordinatesAndQuelle(452164.8506, 5390542.25685, 453231.8178, 5391128.56334, QuellSystem.DLM)
			.id(1L)
			.build();
		Stream<Kante> kanten = Stream.of(
			kante
		);

		File pbfFile = new File(tempDir, "dlm-test.osm.pbf");

		// act
		pbfErstellungsRepository.writePbf(Map.of(kante.getGeometry().getEnvelopeInternal(), kanten), pbfFile);

		// assert
		assertThatNoException().isThrownBy(() -> {
			OsmMatchedGraphHopper osmMatchedGraphHopper = new OsmMatchedGraphHopper(osmMatchingCacheRepository);
			osmMatchedGraphHopper.setOSMFile(pbfFile.getCanonicalPath());
			Profile profile = new Profile("bike").setVehicle("bike").setWeighting("fastest").setTurnCosts(false);
			osmMatchedGraphHopper.setProfiles(profile);
			osmMatchedGraphHopper.setGraphHopperLocation(tempDir.getCanonicalPath());
			osmMatchedGraphHopper.importOrLoad();
		});
	}

	private Coordinate getCoordinateOfNode(EntityContainer entityContainer) {
		OsmNode node = (OsmNode) entityContainer.getEntity();
		return new Coordinate(node.getLatitude(), node.getLongitude());
	}

	private List<Coordinate> getCoordinatesOfWay(List<EntityContainer> list, EntityContainer entityContainer) {
		OsmWay way = (OsmWay) entityContainer.getEntity();

		return IntStream.range(0, way.getNumberOfNodes()).mapToObj(way::getNodeId)
			.map(id -> {
				EntityContainer eC = list.stream().filter(container -> container.getEntity().getId() == id)
					.findFirst()
					.get();
				return this.getCoordinateOfNode(eC);
			}).collect(Collectors.toList());
	}

	private List<Tag> getTags(EntityContainer entityContainer) {
		OsmEntity entity = entityContainer.getEntity();
		return IntStream.range(0, entity.getNumberOfTags()).mapToObj(entity::getTag).map(osmTag -> (Tag) osmTag)
			.collect(Collectors.toList());
	}

	private Set<Tag> getDefaultTags() {
		return toTags(getDefaultTagMap());
	}

	private Set<Tag> getDefaultTagsWithOneway(String onewayTagValue) {
		Map<String, String> defaultTags = getDefaultTagMap();
		defaultTags.put("oneway", onewayTagValue);
		return toTags(defaultTags);
	}

	private Set<Tag> getDefaultTagsWithBarriere(String barriereRechtsValue, String barriereLinksValue) {
		Map<String, String> defaultTags = getDefaultTagMap();
		defaultTags.put("barriere:right", barriereRechtsValue);
		defaultTags.put("barriere:left", barriereLinksValue);
		return toTags(defaultTags);
	}

	@NotNull
	private static Map<String, String> getDefaultTagMap() {
		Map<String, String> defaultTags = new HashMap<>();
		defaultTags.put("oneway", "no");
		defaultTags.put("highway", "track");
		defaultTags.put("belagart:left", "UNBEKANNT");
		defaultTags.put("belagart:right", "UNBEKANNT");
		defaultTags.put("fuehrung:left", "UNBEKANNT");
		defaultTags.put("fuehrung:right", "UNBEKANNT");
		defaultTags.put("radvis:netzklasse:radnetz_alltag", "false");
		defaultTags.put("radvis:netzklasse:radnetz_freizeit", "false");
		defaultTags.put("radvis:netzklasse:radnetz_zielnetz", "false");
		defaultTags.put("radvis:netzklasse:kreisnetz_alltag", "false");
		defaultTags.put("radvis:netzklasse:kreisnetz_freizeit", "false");
		defaultTags.put("radvis:netzklasse:kommunalnetz_alltag", "false");
		defaultTags.put("radvis:netzklasse:kommunalnetz_freizeit", "false");
		defaultTags.put("radvis:netzklasse:radschnellverbindung", "false");
		defaultTags.put("radvis:netzklasse:radvorrangrouten", "false");
		defaultTags.put("oberflaeche:left", "UNBEKANNT");
		defaultTags.put("oberflaeche:right", "UNBEKANNT");
		defaultTags.put("beleuchtung", "UNBEKANNT");
		return defaultTags;
	}

	private Set<Tag> toTags(Map<String, String> keyValueMap) {
		return keyValueMap.entrySet()
			.stream()
			.map(entry -> new Tag(entry.getKey(), entry.getValue()))
			.collect(Collectors.toSet());
	}
}
