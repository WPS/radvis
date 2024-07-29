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

package de.wps.radvis.backend.organisation.schnittstelle.repositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.schnittstelle.repositoryImpl.AbstractVerwaltungseinheitImportRepository.GebietskoerperschaftsFeatures;

class VerwaltungseinheitBkgFormatImportRepositoryImplTest {

	VerwaltungseinheitBkgFormatImportRepositoryImpl organisationenImportRepositoryImpl;
	private Gebietskoerperschaft staatOrg;
	private SimpleFeature staatFeature;
	private static int BUNDESLAND_MIT_REGIERUNGSBEZIRKEN_AGS = 8;
	private static int ANDERES_BUNDESLAND_MIT_REGIERUNGSBEZIRKEN_AGS = 9;
	private static int BUNDESLAND_OHNE_REGIERUNGSBEZIRKEN_AGS = 0;

	@BeforeEach
	void setUp() {
		CoordinateReferenceSystemConverter coordinateConverter = new CoordinateReferenceSystemConverter(
			new Envelope(
				new Coordinate(0, 0),
				new Coordinate(100, 100)));

		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		staatOrg = new Gebietskoerperschaft(0, "BunteRepublik", null, OrganisationsArt.STAAT, geometry, true);
		staatFeature = createFeature(staatOrg);

		File verzeichnisMock = mock(File.class);
		when(verzeichnisMock.exists()).thenReturn(true);
		when(verzeichnisMock.isDirectory()).thenReturn(true);

		organisationenImportRepositoryImpl = new VerwaltungseinheitBkgFormatImportRepositoryImpl(
			coordinateConverter, staatOrg.getOrganisationsArt(), staatOrg.getName(), verzeichnisMock);
	}

	@Test
	void filtereNachLandflaechen_filtertElementeMitGeofaktorVierNichtHeraus() {
		// Arrange
		SimpleFeatureCollection simpleFeatureCollection = new ListFeatureCollection(
			staatFeature.getFeatureType(),
			List.of(staatFeature));

		// Act
		SimpleFeatureCollection result = organisationenImportRepositoryImpl.filtereNachLandflaechen(
			simpleFeatureCollection);

		// Assert
		assertThat(result.size()).isEqualTo(1);
	}

	@Test
	void filtereNachLandflaechen_filtertElementeOhneGeofaktorVierHeraus() {
		// Arrange
		Map<String, String> attributes = new HashMap<>();
		attributes.put("GF", "1");
		SimpleFeature feature = SimpleFeatureTestDataProvider.withAttributes(attributes);

		SimpleFeatureCollection simpleFeatureCollection = new ListFeatureCollection(
			staatFeature.getFeatureType(),
			List.of(feature));

		// Act
		SimpleFeatureCollection result = organisationenImportRepositoryImpl.filtereNachLandflaechen(
			simpleFeatureCollection);

		// Assert
		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	void getGebietskoerperschaftenFromFeatures_staat() {
		// Arrange
		SimpleFeatureType featureType = staatFeature.getFeatureType();
		SimpleFeatureCollection bundeslaender = new ListFeatureCollection(
			featureType, List.of());
		SimpleFeatureCollection regierungsbezirke = new ListFeatureCollection(
			featureType, List.of());
		SimpleFeatureCollection kreise = new ListFeatureCollection(
			featureType, List.of());
		SimpleFeatureCollection gemeinden = new ListFeatureCollection(
			featureType, List.of());

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslaender, regierungsbezirke, kreise, gemeinden);

		// Assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringFields("id", "version", "letzteAenderung", "benutzerLetzteAenderung")
			.isEqualTo(staatOrg);
	}

	@Test
	void getGebietskoerperschaftenFromFeatures_bundesland() {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundesland1 = new Gebietskoerperschaft(BUNDESLAND_MIT_REGIERUNGSBEZIRKEN_AGS,
			"KunterBundesland1", staatOrg, OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft bundesland2 = new Gebietskoerperschaft(BUNDESLAND_OHNE_REGIERUNGSBEZIRKEN_AGS,
			"KunterBundesland2", staatOrg, OrganisationsArt.BUNDESLAND, geometry, true);
		SimpleFeatureType featureType = createFeature(bundesland1).getFeatureType();
		SimpleFeatureCollection bundeslaender = new ListFeatureCollection(
			featureType, List.of(createFeature(bundesland1), createFeature(bundesland2)));
		SimpleFeatureCollection regierungsbezirke = new ListFeatureCollection(
			featureType, List.of());
		SimpleFeatureCollection kreise = new ListFeatureCollection(
			featureType, List.of());
		SimpleFeatureCollection gemeinden = new ListFeatureCollection(
			featureType, List.of());

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslaender, regierungsbezirke, kreise, gemeinden);

		// Assert
		assertThat(result).hasSize(3);
		assertThat(result)
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "version", "letzteAenderung",
				"benutzerLetzteAenderung", "bereich", "uebergeordneteOrganisation.bereich")
			.containsExactlyInAnyOrder(staatOrg, bundesland1, bundesland2);
	}

	@Test
	void getGebietskoerperschaftenFromFeatures_regierungsbezirke() {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundesland1 = new Gebietskoerperschaft(BUNDESLAND_MIT_REGIERUNGSBEZIRKEN_AGS,
			"KunterBundesland1", staatOrg, OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft bundesland2 = new Gebietskoerperschaft(ANDERES_BUNDESLAND_MIT_REGIERUNGSBEZIRKEN_AGS,
			"KunterBundesland2", staatOrg, OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft regBez1 = new Gebietskoerperschaft(2, "regBez1", bundesland1,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft regBez2 = new Gebietskoerperschaft(8, "regBez2", bundesland2,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		SimpleFeatureType featureType = createFeature(bundesland1).getFeatureType();
		SimpleFeatureCollection bundeslaender = new ListFeatureCollection(
			featureType, List.of(createFeature(bundesland1), createFeature(bundesland2)));
		SimpleFeatureCollection regierungsbezirke = new ListFeatureCollection(
			featureType, List.of(createFeature(regBez1), createFeature(regBez2)));
		SimpleFeatureCollection kreise = new ListFeatureCollection(
			featureType, List.of());
		SimpleFeatureCollection gemeinden = new ListFeatureCollection(
			featureType, List.of());

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslaender, regierungsbezirke, kreise, gemeinden);

		// Assert
		assertThat(result).hasSize(5);
		assertThat(result)
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "version", "letzteAenderung",
				"benutzerLetzteAenderung", "bereich", "uebergeordneteOrganisation.bereich",
				"uebergeordneteOrganisation.uebergeordneteOrganisation")
			.containsExactlyInAnyOrder(staatOrg, bundesland1, bundesland2, regBez1, regBez2);
	}

	@Test
	void getGebietskoerperschaftenFromFeatures_kreise() {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundesland1 = new Gebietskoerperschaft(BUNDESLAND_MIT_REGIERUNGSBEZIRKEN_AGS,
			"KunterBundesland1", staatOrg, OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft regBez1 = new Gebietskoerperschaft(2, "regBez1", bundesland1,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft regBez2 = new Gebietskoerperschaft(8, "regBez2", bundesland1,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft kreis1 = new Gebietskoerperschaft(2, "kreis1", regBez1,
			OrganisationsArt.KREIS, geometry, true);
		Gebietskoerperschaft kreis2 = new Gebietskoerperschaft(34, "kreis2", regBez2,
			OrganisationsArt.KREIS, geometry, true);
		SimpleFeatureType featureType = createFeature(bundesland1).getFeatureType();
		SimpleFeatureCollection bundeslaender = new ListFeatureCollection(
			featureType, List.of(createFeature(bundesland1)));
		SimpleFeatureCollection regierungsbezirke = new ListFeatureCollection(
			featureType, List.of(createFeature(regBez1), createFeature(regBez2)));
		SimpleFeatureCollection kreise = new ListFeatureCollection(
			featureType, List.of(createFeature(kreis1), createFeature(kreis2)));
		SimpleFeatureCollection gemeinden = new ListFeatureCollection(
			featureType, List.of());

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslaender, regierungsbezirke, kreise, gemeinden);

		// Assert
		assertThat(result).hasSize(6);
		assertThat(result)
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "version", "letzteAenderung",
				"benutzerLetzteAenderung", "bereich", "uebergeordneteOrganisation.bereich",
				"uebergeordneteOrganisation.uebergeordneteOrganisation")
			.containsExactlyInAnyOrder(staatOrg, bundesland1, regBez1, regBez2, kreis1, kreis2);
	}

	@Test
	void getGebietskoerperschaftenFromFeatures_kreise_ohneRegierungsbezirk() {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundesland1 = new Gebietskoerperschaft(BUNDESLAND_OHNE_REGIERUNGSBEZIRKEN_AGS,
			"KunterBundesland1", staatOrg, OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft regBez2 = new Gebietskoerperschaft(8, "regBez2", bundesland1,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft kreis1 = new Gebietskoerperschaft(2, "kreis1", bundesland1,
			OrganisationsArt.KREIS, geometry, true);
		SimpleFeatureType featureType = createFeature(bundesland1).getFeatureType();
		SimpleFeatureCollection bundeslaender = new ListFeatureCollection(
			featureType, List.of(createFeature(bundesland1)));
		SimpleFeatureCollection regierungsbezirke = new ListFeatureCollection(
			featureType, List.of(createFeature(regBez2)));
		SimpleFeatureCollection kreise = new ListFeatureCollection(
			featureType, List.of(createFeature(kreis1)));
		SimpleFeatureCollection gemeinden = new ListFeatureCollection(
			featureType, List.of());

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslaender, regierungsbezirke, kreise, gemeinden);

		// Assert
		assertThat(result).hasSize(4);
		assertThat(result)
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "version", "letzteAenderung",
				"benutzerLetzteAenderung", "bereich", "uebergeordneteOrganisation.bereich",
				"uebergeordneteOrganisation.uebergeordneteOrganisation")
			.containsExactlyInAnyOrder(staatOrg, bundesland1, regBez2, kreis1);
	}

	@Test
	void getGebietskoerperschaftenFromFeatures_gemeinden() {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundesland1 = new Gebietskoerperschaft(BUNDESLAND_MIT_REGIERUNGSBEZIRKEN_AGS,
			"KunterBundesland1", staatOrg, OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft regBez1 = new Gebietskoerperschaft(2, "regBez1", bundesland1,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft kreis1 = new Gebietskoerperschaft(2, "kreis1", regBez1,
			OrganisationsArt.KREIS, geometry, true);
		Gebietskoerperschaft kreis2 = new Gebietskoerperschaft(34, "kreis2", regBez1,
			OrganisationsArt.KREIS, geometry, true);
		Gebietskoerperschaft gemeinde1 = new Gebietskoerperschaft(2, "gemeinde1", kreis1,
			OrganisationsArt.GEMEINDE, geometry, true);
		Gebietskoerperschaft gemeinde2 = new Gebietskoerperschaft(356, "gemeinde2", kreis2,
			OrganisationsArt.GEMEINDE, geometry, true);
		SimpleFeatureType featureType = createFeature(bundesland1).getFeatureType();
		SimpleFeatureCollection bundeslaender = new ListFeatureCollection(
			featureType, List.of(createFeature(bundesland1)));
		SimpleFeatureCollection regierungsbezirke = new ListFeatureCollection(
			featureType, List.of(createFeature(regBez1)));
		SimpleFeatureCollection kreise = new ListFeatureCollection(
			featureType, List.of(createFeature(kreis1), createFeature(kreis2)));
		SimpleFeatureCollection gemeinden = new ListFeatureCollection(
			featureType, List.of(createFeature(gemeinde1), createFeature(gemeinde2)));

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslaender, regierungsbezirke, kreise, gemeinden);

		// Assert
		assertThat(result).hasSize(7);
		assertThat(result)
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "version", "letzteAenderung",
				"benutzerLetzteAenderung", "bereich", "uebergeordneteOrganisation.bereich",
				"uebergeordneteOrganisation.uebergeordneteOrganisation")
			.containsExactlyInAnyOrder(staatOrg, bundesland1, regBez1, kreis1, kreis2, gemeinde1, gemeinde2);
	}

	private List<Gebietskoerperschaft> getGebietskoerperschaftenFromFeatures(SimpleFeatureCollection bundeslandFeature,
		SimpleFeatureCollection regierungsbezirkFeatures, SimpleFeatureCollection landkreisFeatures,
		SimpleFeatureCollection gemeindeFeatures) {
		GebietskoerperschaftsFeatures features = new GebietskoerperschaftsFeatures(
			Optional.of(new ListFeatureCollection(staatFeature.getFeatureType(), List.of(staatFeature))),
			Optional.of(bundeslandFeature), Optional.of(regierungsbezirkFeatures), Optional.of(landkreisFeatures),
			gemeindeFeatures);

		return organisationenImportRepositoryImpl.getGebietskoerperschaftenFromFeatures(features);
	}

	private SimpleFeature createFeature(Gebietskoerperschaft gebietskoerperschaft) {
		Map<String, String> attributes = new HashMap<>();
		String name = gebietskoerperschaft.getName();
		String ags = getAgs(gebietskoerperschaft);

		attributes.put("AGS", ags);
		attributes.put("GEN", name);
		attributes.put("GF", "4");
		MultiPolygon geometry = gebietskoerperschaft.getBereich().get();
		return SimpleFeatureTestDataProvider.withGeometryAndAttributes(attributes,
			geometry);
	}

	private String getAgs(Gebietskoerperschaft gebietskoerperschaft) {
		switch (gebietskoerperschaft.getOrganisationsArt()) {
		case GEMEINDE:
			return getAgs((Gebietskoerperschaft) gebietskoerperschaft.getUebergeordneteVerwaltungseinheit().get())
				+ String.format("%03d", gebietskoerperschaft.getFachId());
		case KREIS:
			String kreisAgs = String.format("%02d", gebietskoerperschaft.getFachId());
			if (!gebietskoerperschaft.getUebergeordneteVerwaltungseinheit().get().getOrganisationsArt()
				.equals(OrganisationsArt.REGIERUNGSBEZIRK)) {
				kreisAgs = "0" + kreisAgs;
			}
			return getAgs((Gebietskoerperschaft) gebietskoerperschaft.getUebergeordneteVerwaltungseinheit().get())
				+ kreisAgs;
		case REGIERUNGSBEZIRK:
			return getAgs((Gebietskoerperschaft) gebietskoerperschaft.getUebergeordneteVerwaltungseinheit().get())
				+ String.format("%01d", gebietskoerperschaft.getFachId());
		case BUNDESLAND:
			return String.format("%02d", gebietskoerperschaft.getFachId());
		default:
			return "";
		}
	}
}
