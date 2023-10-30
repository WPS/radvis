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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.organisation.domain.FalscherGeometrieTypException;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

public class OrganisationenImportRepositoryImpl implements VerwaltungseinheitImportRepository {

	private CoordinateReferenceSystemConverter transformationService;

	public OrganisationenImportRepositoryImpl(
		CoordinateReferenceSystemConverter transformationService) {
		require(transformationService, notNullValue());

		this.transformationService = transformationService;
	}

	@Override
	public List<Gebietskoerperschaft> getGebietskoerperschaftenFromShapeFiles(
		File bundeslandFile, File regierungsbezirkFile,
		File landkreisFile, File gemeindeFile) throws IOException {

		Map<Integer, Gebietskoerperschaft> fachIdToOrganisation = new HashMap<>();

		// Bundesland
		SimpleFeatureCollection bundeslandFeature = readShapeFile(bundeslandFile);

		if (bundeslandFeature.size() != 1) {
			throw new RuntimeException("Bundesland ShapeFile enthält nicht genau ein Bundesland.");
		}

		KoordinatenReferenzSystem bundeslandSourceReferenzSystem;

		try {
			bundeslandSourceReferenzSystem = getKoordinatenReferenzSystem(bundeslandFeature);
		} catch (FactoryException e) {
			throw new RuntimeException("Ermittlung der SRID für Bundesland Shapefile fehlgeschlagen.");
		}

		try (SimpleFeatureIterator bundeslandIterator = bundeslandFeature.features()) {
			while (bundeslandIterator.hasNext()) {
				SimpleFeature feature = bundeslandIterator.next();

				Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();
				if (!sourceGeometry.getGeometryType().equals(Geometry.TYPENAME_MULTIPOLYGON)) {
					throw new RuntimeException("Bundesland Shapefile enthält falschen GeometrieTyp. Erwartet: "
						+ Geometry.TYPENAME_MULTIPOLYGON + " / Enthalten: " + sourceGeometry.getGeometryType());
				}

				sourceGeometry.setSRID(bundeslandSourceReferenzSystem.getSrid());
				Geometry transformedGeometry = transformationService
					.transformGeometry(sourceGeometry,
						KoordinatenReferenzSystem.ETRS89_UTM32_N);

				Gebietskoerperschaft bundesland = new Gebietskoerperschaft(0, "Baden-Württemberg", null,
					OrganisationsArt.BUNDESLAND,
					(MultiPolygon) transformedGeometry, true);

				fachIdToOrganisation.put(0, bundesland);
			}
		}

		// Regierungsbezirke
		SimpleFeatureCollection regierungsbezirkFeatures = readShapeFile(regierungsbezirkFile);

		KoordinatenReferenzSystem regierungsbezirkSourceReferenzSystem;
		try {
			regierungsbezirkSourceReferenzSystem = getKoordinatenReferenzSystem(regierungsbezirkFeatures);
		} catch (FactoryException e) {
			throw new RuntimeException("Ermittlung der SRID für Regierungsbezirk Shapefile fehlgeschlagen.", e);
		}

		try (SimpleFeatureIterator featureIterator = regierungsbezirkFeatures.features()) {
			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				Gebietskoerperschaft regierungsBezirk = createGebietskoerperschaft(feature,
					regierungsbezirkSourceReferenzSystem,
					"regb_id", "regb_nam", fachIdToOrganisation.get(0), OrganisationsArt.REGIERUNGSBEZIRK);

				fachIdToOrganisation.put(regierungsBezirk.getFachId(), regierungsBezirk);
			}
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Regierungsbezirk Shapefile enthält falschen GeometrieTyp. " + e.getMessage());
		}

		// Landkreise
		SimpleFeatureCollection landkreisFeatures = readShapeFile(landkreisFile);

		KoordinatenReferenzSystem landkreisSourceReferenzSystem;
		try {
			landkreisSourceReferenzSystem = getKoordinatenReferenzSystem(landkreisFeatures);
		} catch (FactoryException e) {
			throw new RuntimeException("Ermittlung der SRID für Landkreis Shapefile fehlgeschlagen.", e);
		}

		try (SimpleFeatureIterator featureIterator = landkreisFeatures.features()) {
			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();

				int regbId = Integer.parseInt(feature.getProperty("regb_id").getValue().toString());
				Verwaltungseinheit regierungsBezirk = fachIdToOrganisation.get(
					regbId);

				Gebietskoerperschaft landkreis = createGebietskoerperschaft(feature, landkreisSourceReferenzSystem,
					"kreis_id",
					"kreis_nam", regierungsBezirk, OrganisationsArt.KREIS);

				if (regierungsBezirk == null) {
					throw new RuntimeException(
						"Regierungsbezirk mit FachId " + regbId + " für Landkreis " + landkreis.getName()
							+ " mit FachId " + landkreis.getFachId() + "existiert nicht!");
				}

				fachIdToOrganisation.put(landkreis.getFachId(), landkreis);
			}
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Kreis Shapefile enthält falschen GeometrieTyp. " + e.getMessage());
		}

		// Gemeinden
		SimpleFeatureCollection gemeindeFeatures = readShapeFile(gemeindeFile);

		KoordinatenReferenzSystem gemeindeSourceReferenzSystem;
		try {
			gemeindeSourceReferenzSystem = getKoordinatenReferenzSystem(gemeindeFeatures);
		} catch (FactoryException e) {
			throw new RuntimeException("Ermittlung der SRID für Landkreis Shapefile fehlgeschlagen.", e);
		}

		try (SimpleFeatureIterator featureIterator = gemeindeFeatures.features()) {
			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();

				int kreisId = Integer.parseInt(feature.getProperty("kreis_id").getValue().toString());
				Verwaltungseinheit kreis = fachIdToOrganisation.get(kreisId);

				Gebietskoerperschaft gemeinde = createGebietskoerperschaft(feature, gemeindeSourceReferenzSystem,
					"gem_id",
					"gem_nam", kreis, OrganisationsArt.GEMEINDE);

				if (kreis == null) {
					throw new RuntimeException(
						"Landkreis mit FachId " + kreisId + " für Gemeinde " + gemeinde.getName()
							+ " mit FachId " + gemeinde.getFachId() + "existiert nicht!");
				}

				fachIdToOrganisation.put(gemeinde.getFachId(), gemeinde);
			}
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Gemeinde Shapefile enthält falschen GeometrieTyp. " + e.getMessage());
		}

		List<Gebietskoerperschaft> result = new ArrayList<>(fachIdToOrganisation.values());
		result.sort(Comparator.comparing(Gebietskoerperschaft::getFachId));

		return result;
	}

	@Override
	public List<Organisation> getCustomAdditionalOrganisationen() {
		return List.of(
			new Organisation("Bundesrepublik Deutschland", null, OrganisationsArt.SONSTIGES, Set.of(), true),
			new Organisation("Deutsche Bahn", null, OrganisationsArt.SONSTIGES, Set.of(), true),
			new Organisation("Wasserstraßen- und Schifffahrtsverwaltung", null, OrganisationsArt.SONSTIGES, Set.of(),
				true),
			new Organisation("Landesforstverwaltung BW", null, OrganisationsArt.SONSTIGES, Set.of(), true),
			new Organisation("Dritter / Sonstiger", null, OrganisationsArt.SONSTIGES, Set.of(), true),
			new Organisation("Unbekannt", null, OrganisationsArt.SONSTIGES, Set.of(), true));
	}

	private Gebietskoerperschaft createGebietskoerperschaft(SimpleFeature feature,
		KoordinatenReferenzSystem sourceReferenzSystem,
		String fachIdKey, String nameKey, Verwaltungseinheit uebergeordneteOrganisation, OrganisationsArt art)
		throws FalscherGeometrieTypException {
		Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();

		if (!sourceGeometry.getGeometryType().equals(Geometry.TYPENAME_MULTIPOLYGON)) {
			throw new FalscherGeometrieTypException(Geometry.TYPENAME_MULTIPOLYGON, sourceGeometry.getGeometryType());
		}

		sourceGeometry.setSRID(sourceReferenzSystem.getSrid());
		Geometry transformedGeometry = transformationService
			.transformGeometry(sourceGeometry,
				KoordinatenReferenzSystem.ETRS89_UTM32_N);

		int fachId = Integer.parseInt(feature.getProperty(fachIdKey).getValue().toString());
		String name = feature.getProperty(nameKey).getValue().toString();

		return new Gebietskoerperschaft(fachId, name, uebergeordneteOrganisation, art,
			(MultiPolygon) transformedGeometry, true);
	}

	private KoordinatenReferenzSystem getKoordinatenReferenzSystem(SimpleFeatureCollection featureCollection)
		throws FactoryException {
		CoordinateReferenceSystem sourceCRS = featureCollection.getSchema().getCoordinateReferenceSystem();

		String srid = CRS.lookupIdentifier(sourceCRS, true).substring(5);

		return KoordinatenReferenzSystem.ofSrid(Integer.parseInt(srid));
	}

	private SimpleFeatureCollection readShapeFile(File shpFile) throws IOException {
		ShapefileDataStore dataStore = new ShapefileDataStore(shpFile.toURI().toURL());
		try {
			dataStore.setCharset(StandardCharsets.ISO_8859_1);
			return dataStore.getFeatureSource().getFeatures();
		} finally {
			dataStore.dispose();
		}
	}
}
