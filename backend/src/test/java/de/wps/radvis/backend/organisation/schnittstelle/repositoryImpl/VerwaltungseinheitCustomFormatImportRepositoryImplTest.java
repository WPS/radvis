package de.wps.radvis.backend.organisation.schnittstelle.repositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
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

class VerwaltungseinheitCustomFormatImportRepositoryImplTest {

	private static final String OBERSTE_GEBIETSKOERPERSCHAFT = "Baden-Württemberg";
	VerwaltungseinheitCustomFormatImportRepositoryImpl organisationenImportRepositoryImpl;

	@BeforeEach
	void setUp() {
		CoordinateReferenceSystemConverter coordinateConverter = new CoordinateReferenceSystemConverter(
			new Envelope(
				new Coordinate(0, 0),
				new Coordinate(100, 100)));

		File verzeichnisMock = mock(File.class);
		when(verzeichnisMock.exists()).thenReturn(true);
		when(verzeichnisMock.isDirectory()).thenReturn(true);

		organisationenImportRepositoryImpl = new VerwaltungseinheitCustomFormatImportRepositoryImpl(
			coordinateConverter, OBERSTE_GEBIETSKOERPERSCHAFT, OrganisationsArt.BUNDESLAND, verzeichnisMock);
	}

	@Test
	void testGetGebietskoerperschaftenFromShapeFiles_bundesland() throws IOException {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundesland = new Gebietskoerperschaft(0, OBERSTE_GEBIETSKOERPERSCHAFT, null,
			OrganisationsArt.BUNDESLAND, geometry, true);
		SimpleFeatureCollection featureCollection = new ListFeatureCollection(
			createFeature(bundesland).getFeatureType(),
			List.of(createFeature(bundesland)));

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			featureCollection, new ListFeatureCollection(createFeature(bundesland).getFeatureType(), List.of()),
			new ListFeatureCollection(createFeature(bundesland).getFeatureType(), List.of()),
			new ListFeatureCollection(createFeature(bundesland).getFeatureType(), List.of()));

		// Assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringFields("id", "version", "letzteAenderung", "benutzerLetzteAenderung")
			.isEqualTo(bundesland);
	}

	@Test
	void testGetGebietskoerperschaftenFromShapeFiles_RP() throws IOException {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundeslandOrg = new Gebietskoerperschaft(0, OBERSTE_GEBIETSKOERPERSCHAFT, null,
			OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft RP1Org = new Gebietskoerperschaft(5, "RP1", bundeslandOrg,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft RP2Org = new Gebietskoerperschaft(10, "RP2", bundeslandOrg,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);

		SimpleFeatureType featureType = createFeature(bundeslandOrg).getFeatureType();
		SimpleFeatureCollection bundeslandFeatures = new ListFeatureCollection(
			featureType, List.of(createFeature(bundeslandOrg)));

		SimpleFeatureCollection rpFeatures = new ListFeatureCollection(featureType,
			List.of(createFeature(RP1Org), createFeature(RP2Org)));

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslandFeatures, rpFeatures, new ListFeatureCollection(featureType, List.of()),
			new ListFeatureCollection(featureType, List.of()));

		// Assert
		assertThat(result).hasSize(3);
		assertThat(result)
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "version", "letzteAenderung",
				"benutzerLetzteAenderung", "bereich", "uebergeordneteOrganisation.bereich")
			.containsExactlyInAnyOrder(bundeslandOrg, RP1Org, RP2Org);
	}

	@Test
	void testGetGebietskoerperschaftenFromShapeFiles_kreis() throws IOException {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundeslandOrg = new Gebietskoerperschaft(0, OBERSTE_GEBIETSKOERPERSCHAFT, null,
			OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft RP1Org = new Gebietskoerperschaft(5, "RP1", bundeslandOrg,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft RP2Org = new Gebietskoerperschaft(10, "RP2", bundeslandOrg,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft kreis1 = new Gebietskoerperschaft(12, "Kreis1", RP1Org,
			OrganisationsArt.KREIS, geometry, true);
		Gebietskoerperschaft kreis2 = new Gebietskoerperschaft(14, "Kreis2", RP2Org,
			OrganisationsArt.KREIS, geometry, true);

		SimpleFeatureType featureType = createFeature(bundeslandOrg).getFeatureType();
		SimpleFeatureCollection bundeslandFeatures = new ListFeatureCollection(
			featureType, List.of(createFeature(bundeslandOrg)));

		SimpleFeatureCollection rpFeatures = new ListFeatureCollection(featureType,
			List.of(createFeature(RP1Org), createFeature(RP2Org)));

		SimpleFeatureCollection kreisFeatures = new ListFeatureCollection(featureType,
			List.of(createFeature(kreis1), createFeature(kreis2)));

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslandFeatures, rpFeatures, kreisFeatures, new ListFeatureCollection(featureType, List.of()));

		// Assert
		assertThat(result).hasSize(5);
		assertThat(result)
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "version", "letzteAenderung",
				"benutzerLetzteAenderung", "bereich", "uebergeordneteOrganisation.bereich",
				"uebergeordneteOrganisation.uebergeordneteOrganisation")
			.containsExactlyInAnyOrder(bundeslandOrg, RP1Org, RP2Org, kreis1, kreis2);
	}

	@Test
	void testGetGebietskoerperschaftenFromShapeFiles_gemeinde() throws IOException {
		// Arrange
		MultiPolygon geometry = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		Gebietskoerperschaft bundeslandOrg = new Gebietskoerperschaft(0, OBERSTE_GEBIETSKOERPERSCHAFT, null,
			OrganisationsArt.BUNDESLAND, geometry, true);
		Gebietskoerperschaft RP1Org = new Gebietskoerperschaft(5, "RP1", bundeslandOrg,
			OrganisationsArt.REGIERUNGSBEZIRK, geometry, true);
		Gebietskoerperschaft kreis1 = new Gebietskoerperschaft(12, "Kreis1", RP1Org,
			OrganisationsArt.KREIS, geometry, true);
		Gebietskoerperschaft kreis2 = new Gebietskoerperschaft(14, "Kreis2", RP1Org,
			OrganisationsArt.KREIS, geometry, true);
		Gebietskoerperschaft gemeinde1 = new Gebietskoerperschaft(16, "Gemeinde1", kreis1,
			OrganisationsArt.GEMEINDE, geometry, true);
		Gebietskoerperschaft gemeinde2 = new Gebietskoerperschaft(25, "Gemeinde2", kreis2,
			OrganisationsArt.GEMEINDE, geometry, true);

		SimpleFeatureType featureType = createFeature(bundeslandOrg).getFeatureType();
		SimpleFeatureCollection bundeslandFeatures = new ListFeatureCollection(
			featureType, List.of(createFeature(bundeslandOrg)));

		SimpleFeatureCollection rpFeatures = new ListFeatureCollection(featureType,
			List.of(createFeature(RP1Org)));

		SimpleFeatureCollection kreisFeatures = new ListFeatureCollection(featureType,
			List.of(createFeature(kreis1), createFeature(kreis2)));

		SimpleFeatureCollection gemeindeFeatures = new ListFeatureCollection(featureType,
			List.of(createFeature(gemeinde1), createFeature(gemeinde2)));

		// Act
		List<Gebietskoerperschaft> result = getGebietskoerperschaftenFromFeatures(
			bundeslandFeatures, rpFeatures, kreisFeatures, gemeindeFeatures);

		// Assert
		assertThat(result).hasSize(6);
		assertThat(result)
			.usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "version", "letzteAenderung",
				"benutzerLetzteAenderung", "bereich", "uebergeordneteOrganisation.bereich",
				"uebergeordneteOrganisation.uebergeordneteOrganisation")
			.containsExactlyInAnyOrder(bundeslandOrg, RP1Org, kreis1, kreis2, gemeinde1, gemeinde2);
	}

	private List<Gebietskoerperschaft> getGebietskoerperschaftenFromFeatures(SimpleFeatureCollection bundeslandFeature,
		SimpleFeatureCollection regierungsbezirkFeatures, SimpleFeatureCollection landkreisFeatures,
		SimpleFeatureCollection gemeindeFeatures) {

		GebietskoerperschaftsFeatures features = new GebietskoerperschaftsFeatures(Optional.empty(),
			Optional.of(bundeslandFeature), Optional.of(regierungsbezirkFeatures), Optional.of(landkreisFeatures),
			gemeindeFeatures);

		return organisationenImportRepositoryImpl.getGebietskoerperschaftenFromFeatures(features);
	}

	private SimpleFeature createFeature(Gebietskoerperschaft gebietskoerperschaft) {
		Map<String, String> attribute = new HashMap<>();
		String name = gebietskoerperschaft.getName();
		String fachId = gebietskoerperschaft.getFachId().toString();

		switch (gebietskoerperschaft.getOrganisationsArt()) {
		case REGIERUNGSBEZIRK:
			attribute.put("regb_id", fachId);
			attribute.put("regb_nam", name);
			break;
		case KREIS:
			attribute.put("regb_id",
				((Gebietskoerperschaft) gebietskoerperschaft.getUebergeordneteVerwaltungseinheit().get()).getFachId()
					.toString());
			attribute.put("kreis_id", fachId);
			attribute.put("kreis_nam", name);
			break;
		case GEMEINDE:

			attribute.put("kreis_id",
				((Gebietskoerperschaft) gebietskoerperschaft.getUebergeordneteVerwaltungseinheit().get()).getFachId()
					.toString());
			attribute.put("gem_id", fachId);
			attribute.put("gem_nam", name);
			break;
		default:
			break;
		}
		MultiPolygon geometry = gebietskoerperschaft.getBereich().get();
		return SimpleFeatureTestDataProvider.withGeometryAndAttributes(attribute,
			geometry);
	}
}