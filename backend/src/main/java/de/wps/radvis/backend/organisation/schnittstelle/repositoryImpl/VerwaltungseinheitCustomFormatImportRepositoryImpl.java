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

import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.FactoryException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.organisation.domain.FalscherGeometrieTypException;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerwaltungseinheitCustomFormatImportRepositoryImpl extends AbstractVerwaltungseinheitImportRepository {

	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private final String obersteGebietskoerperschaftName;
	private final OrganisationsArt obersteGebietskoerperschaftArt;
	private final File verwaltungsgrenzenVerzeichnis;
	private Gebietskoerperschaft obersteGebietskoerperschaft;

	public VerwaltungseinheitCustomFormatImportRepositoryImpl(
		CoordinateReferenceSystemConverter coordinateConverter, @NonNull String obersteGebietskoerperschaftName,
		@NonNull OrganisationsArt obersteGebietskoerperschaftArt, File verwaltungsgrenzenVerzeichnis) {
		super(coordinateConverter);
		this.verwaltungsgrenzenVerzeichnis = verwaltungsgrenzenVerzeichnis;

		require(verwaltungsgrenzenVerzeichnis.exists(),
			"Verwaltungsgrenzenverzeichnis existiert nicht: " + verwaltungsgrenzenVerzeichnis.getAbsolutePath());
		require(verwaltungsgrenzenVerzeichnis.isDirectory(),
			"Verwaltungsgrenzenverzeichnis ist kein Verzeichnis: " + verwaltungsgrenzenVerzeichnis.getAbsolutePath());

		this.obersteGebietskoerperschaftName = obersteGebietskoerperschaftName;
		this.obersteGebietskoerperschaftArt = obersteGebietskoerperschaftArt;
	}

	protected GebietskoerperschaftsFeatures getFeaturesFromFiles() throws IOException {
		File bundeslandFile = new File(verwaltungsgrenzenVerzeichnis, "v_at_land.shp");
		File regierungsbezirkFile = new File(verwaltungsgrenzenVerzeichnis, "v_at_regierungsbezirk.shp");
		File landkreisFile = new File(verwaltungsgrenzenVerzeichnis, "v_at_kreis.shp");
		File gemeindeFile = new File(verwaltungsgrenzenVerzeichnis, "v_at_gemeinde.shp");

		require(bundeslandFile.exists(), "Bundesland File existiert nicht: " + bundeslandFile.getAbsolutePath());
		require(regierungsbezirkFile.exists(),
			"Regierungsbezirk File existiert nicht: " + regierungsbezirkFile.getAbsolutePath());
		require(landkreisFile.exists(), "Landkreis File existiert nicht: " + landkreisFile.getAbsolutePath());
		require(gemeindeFile.exists(), "Gemeinde File existiert nicht: " + gemeindeFile.getAbsolutePath());

		SimpleFeatureCollection bundeslandFeature = readShapeFile(bundeslandFile, CHARSET);
		SimpleFeatureCollection regierungsbezirkFeatures = readShapeFile(regierungsbezirkFile, CHARSET);
		SimpleFeatureCollection landkreisFeatures = readShapeFile(landkreisFile, CHARSET);
		SimpleFeatureCollection gemeindeFeatures = readShapeFile(gemeindeFile, CHARSET);

		return new GebietskoerperschaftsFeatures(Optional.empty(),
			Optional.of(bundeslandFeature), Optional.of(regierungsbezirkFeatures), Optional.of(landkreisFeatures),
			gemeindeFeatures);
	}

	protected void checkRequiredProperties(GebietskoerperschaftsFeatures features) {
		if (features.bundesland().isEmpty() || features.regierungsbezirk().isEmpty() || features.kreis().isEmpty()
			|| features.staat().isPresent()) {
			throw new RuntimeException("Nur für Bundesländer mit Regierungsbezirken und Kreisen implementiert");
		}

		SimpleFeatureCollection regierungsbezirkFeatures = features.regierungsbezirk().get();
		SimpleFeatureCollection landkreisFeatures = features.kreis().get();
		SimpleFeatureCollection gemeindeFeatures = features.gemeinde();

		try (SimpleFeatureIterator iterator = regierungsbezirkFeatures.features()) {
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				if (!hasProperties(feature, "regb_id", "regb_nam")) {
					throw new RuntimeException(
						"Feature für Kreis muss key regb_id, regb_nam enthalten.");
				}
			}
		}

		try (SimpleFeatureIterator iterator = landkreisFeatures.features()) {
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				if (!hasProperties(feature, "regb_id", "kreis_nam", "kreis_id")) {
					throw new RuntimeException(
						"Feature für Kreis muss key regb_id, kreis_id, kreis_nam enthalten.");
				}
			}
		}

		try (SimpleFeatureIterator iterator = gemeindeFeatures.features()) {
			if (iterator.hasNext()) {
				if (!areGemeindeFeaturesValid(gemeindeFeatures)) {
					throw new RuntimeException(
						"Feature für Gemeinde muss key gem_id, kreis_id, gem_nam enthalten.");
				}
			}
		}

	}

	private static boolean areGemeindeFeaturesValid(SimpleFeatureCollection gemeindeFeatures) {
		try (SimpleFeatureIterator iterator = gemeindeFeatures.features()) {
			SimpleFeature feature = iterator.next();
			return hasProperties(feature, "kreis_id", "gem_id", "gem_nam");
		}
	}

	protected List<Gebietskoerperschaft> getGebietskoerperschaftenFromFeatures(GebietskoerperschaftsFeatures features) {
		SimpleFeatureCollection bundeslandFeature = features.bundesland().get();
		SimpleFeatureCollection regierungsbezirkFeatures = features.regierungsbezirk().get();
		SimpleFeatureCollection landkreisFeatures = features.kreis().get();
		SimpleFeatureCollection gemeindeFeatures = features.gemeinde();

		if (!obersteGebietskoerperschaftArt.equals(OrganisationsArt.BUNDESLAND)) {
			throw new UnsupportedOperationException(
				"Dieser Import ist nur für Bundesland als oberste Gebietskörperschafts-Art implementiert.");
		}

		// Bundesland
		if (bundeslandFeature.size() != 1) {
			throw new RuntimeException(
				"Bundesland ShapeFile muss genau ein Feature enthalten, wenn oberste Gebietskörperschafts-Art BUNDESLAND ist.");
		}

		Map<Integer, Gebietskoerperschaft> fachIdToOrganisation = new HashMap<>();
		KoordinatenReferenzSystem bundeslandSourceReferenzSystem;

		try {
			bundeslandSourceReferenzSystem = getKoordinatenReferenzSystem(bundeslandFeature);
		} catch (FactoryException e) {
			throw new RuntimeException("Ermittlung der SRID für Bundesland Shapefile fehlgeschlagen.");
		}

		try (SimpleFeatureIterator bundeslandIterator = bundeslandFeature.features()) {
			while (bundeslandIterator.hasNext()) {
				SimpleFeature feature = bundeslandIterator.next();

				MultiPolygon geometry = getPolygon(bundeslandSourceReferenzSystem, feature);

				Gebietskoerperschaft bundesland = new Gebietskoerperschaft(0, obersteGebietskoerperschaftName, null,
					OrganisationsArt.BUNDESLAND, geometry, true);

				fachIdToOrganisation.put(0, bundesland);

				obersteGebietskoerperschaft = bundesland;
			}
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Bundesland Shapefile enthält falschen GeometrieTyp.", e);
		}

		// Regierungsbezirke
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
					regierungsbezirkSourceReferenzSystem, "regb_id", "regb_nam", fachIdToOrganisation.get(0),
					OrganisationsArt.REGIERUNGSBEZIRK);

				fachIdToOrganisation.put(regierungsBezirk.getFachId(), regierungsBezirk);
			}
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Regierungsbezirk Shapefile enthält falschen GeometrieTyp.", e);
		}

		// Landkreise
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
					"kreis_id", "kreis_nam", regierungsBezirk, OrganisationsArt.KREIS);

				if (regierungsBezirk == null) {
					throw new RuntimeException(
						"Regierungsbezirk mit FachId " + regbId + " für Landkreis " + landkreis.getName()
							+ " mit FachId " + landkreis.getFachId() + "existiert nicht!");
				}

				fachIdToOrganisation.put(landkreis.getFachId(), landkreis);
			}
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Kreis Shapefile enthält falschen GeometrieTyp.", e);
		}

		// Gemeinden
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
					"gem_id", "gem_nam", kreis, OrganisationsArt.GEMEINDE);

				if (kreis == null) {
					throw new RuntimeException(
						"Landkreis mit FachId " + kreisId + " für Gemeinde " + gemeinde.getName()
							+ " mit FachId " + gemeinde.getFachId() + "existiert nicht!");
				}

				fachIdToOrganisation.put(gemeinde.getFachId(), gemeinde);
			}
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Gemeinde Shapefile enthält falschen GeometrieTyp.", e);
		}

		List<Gebietskoerperschaft> result = new ArrayList<>(fachIdToOrganisation.values());
		result.sort(Comparator.comparing(Gebietskoerperschaft::getFachId));

		return result;
	}

	private Gebietskoerperschaft createGebietskoerperschaft(SimpleFeature feature,
		KoordinatenReferenzSystem sourceReferenzSystem, String fachIdKey, String nameKey,
		Verwaltungseinheit uebergeordneteOrganisation, OrganisationsArt art)
		throws FalscherGeometrieTypException {
		MultiPolygon bereich = getPolygon(sourceReferenzSystem, feature);

		int fachId = Integer.parseInt(feature.getProperty(fachIdKey).getValue().toString());
		String name = feature.getProperty(nameKey).getValue().toString();

		return new Gebietskoerperschaft(fachId, name, uebergeordneteOrganisation, art,
			bereich, true);
	}

	public static boolean checkShapeFiles(File gebietskoerperschaftShpVerzeichnis) {
		File gemeindeFile = new File(gebietskoerperschaftShpVerzeichnis, "v_at_gemeinde.shp");
		SimpleFeatureCollection featureCollection;
		try {
			featureCollection = readShapeFile(gemeindeFile, CHARSET);
		} catch (IOException e) {
			log.info("Die Datei {} konnte nicht eingelesen werden.", gemeindeFile.getAbsolutePath());
			return false;
		}

		try (SimpleFeatureIterator iterator = featureCollection.features()) {
			if (iterator.hasNext()) {
				return areGemeindeFeaturesValid(featureCollection);
			}
		}

		return false;
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
			new Organisation("Unbekannt", null, OrganisationsArt.SONSTIGES, Set.of(), true),

			new Organisation("Toubiz", null, OrganisationsArt.SONSTIGES, Set.of(obersteGebietskoerperschaft), true));
	}
}
