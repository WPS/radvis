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

package de.wps.radvis.backend.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.ImmutableList;
import com.graphhopper.config.Profile;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.impl.Entity;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.matching.domain.repository.OsmMatchingCacheRepository;
import de.wps.radvis.backend.matching.domain.service.OsmAuszeichnungsService;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchedGraphHopper;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import jakarta.validation.constraints.NotNull;

@Tag("group5")
@ContextConfiguration(classes = {
	CommonConfiguration.class,
	OsmAuszeichnungsServiceTestIT.TestConfiguration.class,
	GeoConverterConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	CommonConfigurationProperties.class,
	PostgisConfigurationProperties.class,
})
class OsmAuszeichnungsServiceTestIT extends DBIntegrationTestIT {
	@EnableEnversRepositories(basePackages = { "de.wps.radvis.backend.matching", "de.wps.radvis.backend.netz" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.matching", "de.wps.radvis.backend.netz" })
	public static class TestConfiguration {
	}

	@TempDir
	public File temp;

	@Mock
	private OsmMatchingCacheRepository osmMatchingCacheRepository;

	@Autowired
	private KantenRepository kantenRepository;

	private OsmAuszeichnungsService osmAuszeichnungsService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		osmAuszeichnungsService = new OsmAuszeichnungsService(kantenRepository, 0.8);
	}

	@Test
	void testReicherePbfAn() throws IOException {
		// Arrange
		File input = new File("src/test/resources/test_small.osm.pbf");
		File output = new File(temp, "result.pbf");

		Laenge breite = Laenge.of(12.34);
		Radverkehrsfuehrung radverkehrsfuehrung = Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG;
		Oberflaechenbeschaffenheit oberflaechenbeschaffenheit = Oberflaechenbeschaffenheit.NEUWERTIG;
		BelagArt belagArt = BelagArt.UNGEBUNDENE_DECKE;
		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ,
			Netzklasse.RADNETZ_FREIZEIT);
		Status status = Status.KONZEPTION;

		Kante kante = addKante(breite, radverkehrsfuehrung, oberflaechenbeschaffenheit, belagArt, netzklassen, status);

		int wayIdWithTags = 416541605;
		int wayIdWithoutTags = 25817668;
		List<KanteOsmWayIdsInsert> osmWayIdMapping = List.of(
			new KanteOsmWayIdsInsert(
				kante.getId(),
				List.of(
					LinearReferenzierteOsmWayId.of(wayIdWithTags, LinearReferenzierterAbschnitt.of(0.0, 0.8)),
					LinearReferenzierteOsmWayId.of(wayIdWithoutTags, LinearReferenzierterAbschnitt.of(0.5, 1.0))
				)
			)
		);

		kantenRepository.insertOsmWayIds(osmWayIdMapping);

		refreshRadVisNetzMaterializedViews();

		// Act
		osmAuszeichnungsService.reicherePbfAn(input, output);

		// Assert
		try (FileInputStream inputStreamInput = new FileInputStream(input);
			FileInputStream inputStreamOutput = new FileInputStream(output)) {
			List<EntityContainer> outputList = getOutputEntityContainers(inputStreamInput, inputStreamOutput);

			outputList.forEach(entityContainer -> {
				Entity entity = (Entity) entityContainer.getEntity();

				if (entity.getType() != EntityType.Way) {
					return;
				}

				String[] expectedNetzklassen = { Netzklasse.RADNETZ_ALLTAG.name(),
					Netzklasse.RADNETZ_FREIZEIT.name() };
				String expectedBreite = "12.34";
				String expectedRadverkehrsfuehrung = radverkehrsfuehrung.name();
				String expectedOberflaechenbeschaffenheit = oberflaechenbeschaffenheit.name();
				String expectedStatus = status.name();
				String expectedBelagArt = "unpaved";

				boolean wirdAngereichert = entity.getId() == wayIdWithTags;
				List<? extends OsmTag> tags = entity.getTags();

				assertTags(tags, wirdAngereichert, expectedNetzklassen, expectedBreite,
					expectedRadverkehrsfuehrung, expectedOberflaechenbeschaffenheit, expectedStatus,
					expectedBelagArt);
			});

			assertGraphHopperCanLoadPbf(output);
		}
	}

	@Test
	void testAlleWerteUnbekannt() throws IOException {
		// Arrange
		File input = new File("src/test/resources/test_small.osm.pbf");
		File output = new File(temp, "result.pbf");

		Laenge breite = null;
		Radverkehrsfuehrung radverkehrsfuehrung = Radverkehrsfuehrung.UNBEKANNT;
		Oberflaechenbeschaffenheit oberflaechenbeschaffenheit = Oberflaechenbeschaffenheit.UNBEKANNT;
		BelagArt belagArt = BelagArt.UNBEKANNT;
		Set<Netzklasse> netzklassen = Set.of();
		Status status = Status.UNTER_VERKEHR;

		Kante kante = addKante(breite, radverkehrsfuehrung, oberflaechenbeschaffenheit, belagArt, netzklassen, status);

		int wayIdWithTags = 416541605;
		int wayIdWithoutTags = 25817668;
		List<KanteOsmWayIdsInsert> osmWayIdMapping = List.of(
			new KanteOsmWayIdsInsert(
				kante.getId(),
				List.of(
					LinearReferenzierteOsmWayId.of(wayIdWithTags, LinearReferenzierterAbschnitt.of(0.0, 0.8)),
					LinearReferenzierteOsmWayId.of(wayIdWithoutTags, LinearReferenzierterAbschnitt.of(0.5, 1.0))
				)
			)
		);

		kantenRepository.insertOsmWayIds(osmWayIdMapping);

		refreshRadVisNetzMaterializedViews();

		// Act
		osmAuszeichnungsService.reicherePbfAn(input, output);

		// Assert
		try (FileInputStream inputStreamInput = new FileInputStream(input);
			FileInputStream inputStreamOutput = new FileInputStream(output)) {
			List<EntityContainer> outputList = getOutputEntityContainers(inputStreamInput, inputStreamOutput);

			outputList.forEach(entityContainer -> {
				Entity entity = (Entity) entityContainer.getEntity();

				if (entity.getType() != EntityType.Way) {
					return;
				}

				boolean wirdAngereichert = entity.getId() == wayIdWithTags;
				List<? extends OsmTag> tags = entity.getTags();

				Optional<? extends OsmTag> netzklasseTag = getTag(tags, "radvis:netzklassen");
				Optional<? extends OsmTag> breiteTag = getTag(tags, "radvis:width");
				Optional<? extends OsmTag> radverkehrsfuehrungTag = getTag(tags, "radvis:cycleway");
				Optional<? extends OsmTag> oberflaechenbeschaffenheitTag = getTag(tags, "radvis:surface:condition");
				Optional<? extends OsmTag> statusTag = getTag(tags, "radvis:status");
				Optional<? extends OsmTag> belagartTag = getTag(tags, "radvis:surface");

				assertThat(netzklasseTag).isNotPresent();
				assertThat(breiteTag).isNotPresent();
				assertThat(radverkehrsfuehrungTag).isNotPresent();
				assertThat(oberflaechenbeschaffenheitTag).isNotPresent();
				assertThat(belagartTag).isNotPresent();

				assertTagValue(statusTag, status.name(), wirdAngereichert);
			});

			assertGraphHopperCanLoadPbf(output);
		}
	}

	@Test
	void reicherePbfAn_MehrereKanten() throws IOException {
		// Arrange
		File input = new File("src/test/resources/test_small.osm.pbf");
		File output = new File(temp, "result.pbf");

		// Kante 1
		Laenge breite1 = Laenge.of(12.34);
		Radverkehrsfuehrung radverkehrsfuehrung1 = Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG;
		Oberflaechenbeschaffenheit oberflaechenbeschaffenheit1 = Oberflaechenbeschaffenheit.NEUWERTIG;
		BelagArt belagArt1 = BelagArt.UNGEBUNDENE_DECKE;
		Set<Netzklasse> netzklassen1 = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ,
			Netzklasse.RADNETZ_FREIZEIT);
		Status status1 = Status.KONZEPTION;

		Kante kante1 = addKante(breite1, radverkehrsfuehrung1, oberflaechenbeschaffenheit1, belagArt1, netzklassen1,
			status1);

		// Kante 2
		Laenge breite2 = Laenge.of(2.34);
		Radverkehrsfuehrung radverkehrsfuehrung2 = Radverkehrsfuehrung.SCHUTZSTREIFEN;
		Oberflaechenbeschaffenheit oberflaechenbeschaffenheit2 = Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND;
		BelagArt belagArt2 = BelagArt.ASPHALT;
		Set<Netzklasse> netzklassen2 = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADVORRANGROUTEN);
		Status status2 = Status.IN_BAU;

		Kante kante2 = addKante(breite2, radverkehrsfuehrung2, oberflaechenbeschaffenheit2, belagArt2, netzklassen2,
			status2);

		// wayIdForKante1 erhält Attribute von kante1, da kante1 den way zu >=80% matched.
		// wayIdForKante2 erhält Attribute von kante2, da kante1 und kante2 diesen zusammen zu >=80% matchen, aber kante2
		// den größeren Eigenanteil hat.
		int wayIdForKante1 = 416541605;
		int wayIdForKante2 = 25817668;
		List<KanteOsmWayIdsInsert> osmWayIdMapping = List.of(
			new KanteOsmWayIdsInsert(
				kante1.getId(),
				List.of(
					LinearReferenzierteOsmWayId.of(wayIdForKante1, LinearReferenzierterAbschnitt.of(0.0, 0.8)),
					LinearReferenzierteOsmWayId.of(wayIdForKante2, LinearReferenzierterAbschnitt.of(0.5, 0.9))
				)
			),
			new KanteOsmWayIdsInsert(
				kante2.getId(),
				List.of(
					LinearReferenzierteOsmWayId.of(wayIdForKante1, LinearReferenzierterAbschnitt.of(0.8, 1)),
					LinearReferenzierteOsmWayId.of(wayIdForKante2, LinearReferenzierterAbschnitt.of(0.0, 0.5))
				)
			)
		);

		kantenRepository.insertOsmWayIds(osmWayIdMapping);

		refreshRadVisNetzMaterializedViews();

		// Act
		osmAuszeichnungsService.reicherePbfAn(input, output);

		// Assert
		try (FileInputStream inputStreamInput = new FileInputStream(input);
			FileInputStream inputStreamOutput = new FileInputStream(output)) {
			List<EntityContainer> outputList = getOutputEntityContainers(inputStreamInput, inputStreamOutput);

			outputList.forEach(entityContainer -> {
				Entity entity = (Entity) entityContainer.getEntity();

				if (entity.getType() != EntityType.Way) {
					return;
				}

				boolean wirdAngereichert = entity.getId() == wayIdForKante1 || entity.getId() == wayIdForKante2;
				List<? extends OsmTag> tags = entity.getTags();

				String[] expectedNetzklassen = { Netzklasse.RADNETZ_ALLTAG.name(),
					Netzklasse.RADNETZ_FREIZEIT.name() };
				String expectedBreite = "12.34";
				String expectedRadverkehrsfuehrung = radverkehrsfuehrung1.name();
				String expectedOberflaechenbeschaffenheit = oberflaechenbeschaffenheit1.name();
				String expectedStatus = status1.name();
				String expectedBelagArt = "unpaved";

				if (entity.getId() == wayIdForKante2) {
					expectedNetzklassen = new String[] { Netzklasse.RADNETZ_ALLTAG.name() };
					expectedBreite = "2.34";
					expectedRadverkehrsfuehrung = radverkehrsfuehrung2.name();
					expectedOberflaechenbeschaffenheit = oberflaechenbeschaffenheit2.name();
					expectedStatus = status2.name();
					expectedBelagArt = "asphalt";
				}

				assertTags(tags, wirdAngereichert, expectedNetzklassen, expectedBreite,
					expectedRadverkehrsfuehrung, expectedOberflaechenbeschaffenheit, expectedStatus,
					expectedBelagArt);
			});

			assertGraphHopperCanLoadPbf(output);
		}
	}

	@Test
	void reicherePbfAn_MehrereKanten_GleichGrosseAnteile() throws IOException {
		// Arrange
		File input = new File("src/test/resources/test_small.osm.pbf");
		File output = new File(temp, "result.pbf");

		// Kante 1
		Laenge breite1 = Laenge.of(12.34);
		Radverkehrsfuehrung radverkehrsfuehrung1 = Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG;
		Oberflaechenbeschaffenheit oberflaechenbeschaffenheit1 = Oberflaechenbeschaffenheit.NEUWERTIG;
		BelagArt belagArt1 = BelagArt.UNGEBUNDENE_DECKE;
		Set<Netzklasse> netzklassen1 = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ,
			Netzklasse.RADNETZ_FREIZEIT);
		Status status1 = Status.KONZEPTION;

		Kante kante1 = addKante(breite1, radverkehrsfuehrung1, oberflaechenbeschaffenheit1, belagArt1, netzklassen1,
			status1);

		// Kante 2
		Laenge breite2 = Laenge.of(2.34);
		Radverkehrsfuehrung radverkehrsfuehrung2 = Radverkehrsfuehrung.SCHUTZSTREIFEN;
		Oberflaechenbeschaffenheit oberflaechenbeschaffenheit2 = Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND;
		BelagArt belagArt2 = BelagArt.ASPHALT;
		Set<Netzklasse> netzklassen2 = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADVORRANGROUTEN);
		Status status2 = Status.IN_BAU;

		Kante kante2 = addKante(breite2, radverkehrsfuehrung2, oberflaechenbeschaffenheit2, belagArt2, netzklassen2,
			status2);

		int wayIdWithTags = 416541605;
		int wayIdWithoutTags = 25817668;
		List<KanteOsmWayIdsInsert> osmWayIdMapping = List.of(
			new KanteOsmWayIdsInsert(
				kante1.getId(),
				List.of(
					LinearReferenzierteOsmWayId.of(wayIdWithTags, LinearReferenzierterAbschnitt.of(0.0, 1.0)),
					LinearReferenzierteOsmWayId.of(wayIdWithoutTags, LinearReferenzierterAbschnitt.of(0.5, 1.0))
				)
			),
			new KanteOsmWayIdsInsert(
				kante2.getId(),
				List.of(
					LinearReferenzierteOsmWayId.of(wayIdWithTags, LinearReferenzierterAbschnitt.of(0.0, 1.0))
				)
			)
		);

		kantenRepository.insertOsmWayIds(osmWayIdMapping);

		refreshRadVisNetzMaterializedViews();

		// Act
		osmAuszeichnungsService.reicherePbfAn(input, output);

		// Assert
		try (FileInputStream inputStreamInput = new FileInputStream(input);
			FileInputStream inputStreamOutput = new FileInputStream(output)) {
			List<EntityContainer> outputList = getOutputEntityContainers(inputStreamInput, inputStreamOutput);

			outputList.forEach(entityContainer -> {
				Entity entity = (Entity) entityContainer.getEntity();

				if (entity.getType() != EntityType.Way) {
					return;
				}

				String[] expectedNetzklassen = { Netzklasse.RADNETZ_ALLTAG.name(),
					Netzklasse.RADNETZ_FREIZEIT.name() };
				String expectedBreite = "12.34";
				String expectedRadverkehrsfuehrung = radverkehrsfuehrung1.name();
				String expectedOberflaechenbeschaffenheit = oberflaechenbeschaffenheit1.name();
				String expectedStatus = status1.name();
				String expectedBelagArt = "unpaved";

				boolean wirdAngereichert = entity.getId() == wayIdWithTags;
				List<? extends OsmTag> tags = entity.getTags();

				// Anhand der BelagArt finden wir heraus, welche der gleich lang gematchten Kanten genommen wurde. Das
				// Anreichern ist leider nicht deterministisch (wegen der Streams und Sets) und die Kanten-ID taggen wir
				// auch nicht, daher gibt es sonst keine Möglichkeit diesen Rückschluss zu ziehen.
				Optional<? extends OsmTag> optionalSurfaceTag = getTag(tags, "radvis:surface");

				if (wirdAngereichert && optionalSurfaceTag.isPresent() && optionalSurfaceTag.get().getValue()
					.equals("asphalt")) {
					expectedNetzklassen = new String[] { Netzklasse.RADNETZ_ALLTAG.name() };
					expectedBreite = "2.34";
					expectedRadverkehrsfuehrung = radverkehrsfuehrung2.name();
					expectedOberflaechenbeschaffenheit = oberflaechenbeschaffenheit2.name();
					expectedStatus = status2.name();
					expectedBelagArt = "asphalt";
				}

				assertTags(tags, wirdAngereichert, expectedNetzklassen, expectedBreite,
					expectedRadverkehrsfuehrung, expectedOberflaechenbeschaffenheit, expectedStatus,
					expectedBelagArt);
			});

			assertGraphHopperCanLoadPbf(output);
		}
	}

	private static void assertTags(List<? extends OsmTag> tags, boolean expectedWirdAngereichert,
		String[] expectedNetzklassen, String expectedBreite, String expectedRadverkehrsfuehrung,
		String expectedOberflaechenbeschaffenheit, String expectedStatus,
		String expectedBelagArt) {
		Optional<? extends OsmTag> netzklasseTag = getTag(tags, "radvis:netzklassen");
		Optional<? extends OsmTag> breiteTag = getTag(tags, "radvis:width");
		Optional<? extends OsmTag> radverkehrsfuehrungTag = getTag(tags, "radvis:cycleway");
		Optional<? extends OsmTag> oberflaechenbeschaffenheitTag = getTag(tags, "radvis:surface:condition");
		Optional<? extends OsmTag> statusTag = getTag(tags, "radvis:status");
		Optional<? extends OsmTag> belagartTag = getTag(tags, "radvis:surface");

		if (expectedWirdAngereichert) {
			assertThat(netzklasseTag).isPresent();
			assertThat(netzklasseTag.get().getValue().split(";")).containsExactlyInAnyOrder(expectedNetzklassen);

		} else {
			assertThat(netzklasseTag).isNotPresent();
		}

		assertTagValue(breiteTag, expectedBreite, expectedWirdAngereichert);
		assertTagValue(radverkehrsfuehrungTag, expectedRadverkehrsfuehrung, expectedWirdAngereichert);
		assertTagValue(oberflaechenbeschaffenheitTag, expectedOberflaechenbeschaffenheit, expectedWirdAngereichert);
		assertTagValue(statusTag, expectedStatus, expectedWirdAngereichert);
		assertTagValue(belagartTag, expectedBelagArt, expectedWirdAngereichert);
	}

	@NotNull
	private Kante addKante(Laenge breite, Radverkehrsfuehrung radverkehrsfuehrung,
		Oberflaechenbeschaffenheit oberflaechenbeschaffenheit, BelagArt belagArt, Set<Netzklasse> netzklassen,
		Status status) {
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.breite(breite)
				.radverkehrsfuehrung(radverkehrsfuehrung)
				.oberflaechenbeschaffenheit(oberflaechenbeschaffenheit)
				.belagArt(belagArt)
				.build());
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.netzklassen(netzklassen)
					.kantenAttribute(KantenAttribute.builder().status(status).build())
					.build()
			)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(fuehrungsformAttribute)
					.fuehrungsformAttributeRechts(fuehrungsformAttribute)
					.build()
			)
			.build();
		kantenRepository.save(kante);
		return kante;
	}

	private void assertGraphHopperCanLoadPbf(File output) throws IOException {
		OsmMatchedGraphHopper osmMatchedGraphHopper = new OsmMatchedGraphHopper(osmMatchingCacheRepository);
		osmMatchedGraphHopper.setOSMFile(output.getCanonicalPath());
		Profile profile = new Profile("bike").setVehicle("bike").setWeighting("fastest").setTurnCosts(false);
		osmMatchedGraphHopper.setProfiles(profile);
		osmMatchedGraphHopper.setGraphHopperLocation(temp.getCanonicalPath());

		assertThatNoException().isThrownBy(() -> osmMatchedGraphHopper.importOrLoad());
	}

	@NotNull
	private static List<EntityContainer> getOutputEntityContainers(FileInputStream inputStreamInput,
		FileInputStream inputStreamOutput) {
		PbfIterator iteratorInput = new PbfIterator(inputStreamInput, true);
		PbfIterator iteratorOutput = new PbfIterator(inputStreamOutput, true);

		List<EntityContainer> inputList = ImmutableList.copyOf(iteratorInput.iterator());
		List<EntityContainer> outputList = ImmutableList.copyOf(iteratorOutput.iterator());

		assertThat(inputList.stream().map(container -> container.getEntity().getId()))
			.containsExactlyInAnyOrderElementsOf(
				outputList.stream().map(container -> container.getEntity().getId()).collect(
					Collectors.toSet()));

		return outputList;
	}

	private static void assertTagValue(Optional<? extends OsmTag> tag, String expected, boolean wirdAngereichert) {
		if (wirdAngereichert) {
			assertThat(tag).isPresent();
			assertThat(tag.get().getValue()).isEqualTo(expected);
		} else {
			assertThat(tag).isNotPresent();
		}
	}

	@NotNull
	private static Optional<? extends OsmTag> getTag(List<? extends OsmTag> tags, String key) {
		return tags.stream()
			.filter(tag -> tag.getKey().equals(key))
			.findFirst();
	}

	private void refreshRadVisNetzMaterializedViews() {
		kantenRepository.refreshNetzMaterializedViews();
	}
}