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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.organisation.domain.FalscherGeometrieTypException;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;

public abstract class AbstractVerwaltungseinheitImportRepository implements VerwaltungseinheitImportRepository {

	private final CoordinateReferenceSystemConverter coordinateConverter;

	public AbstractVerwaltungseinheitImportRepository(CoordinateReferenceSystemConverter coordinateConverter) {
		require(coordinateConverter, notNullValue());
		this.coordinateConverter = coordinateConverter;
	}

	@Override
	public List<Gebietskoerperschaft> getGebietskoerperschaften() throws IOException {
		GebietskoerperschaftsFeatures features = getFeaturesFromFiles();

		checkRequiredProperties(features);

		return getGebietskoerperschaftenFromFeatures(features);
	}

	protected abstract List<Gebietskoerperschaft> getGebietskoerperschaftenFromFeatures(
		GebietskoerperschaftsFeatures features);

	protected abstract void checkRequiredProperties(GebietskoerperschaftsFeatures features);

	protected abstract GebietskoerperschaftsFeatures getFeaturesFromFiles() throws IOException;

	protected static boolean hasProperties(SimpleFeature feature, String... propertyNames) {
		List<String> existingPropertyNames = feature.getProperties().stream().map(p -> p.getName().toString())
			.toList();
		return new HashSet<>(existingPropertyNames).containsAll(Arrays.asList(propertyNames));
	}

	protected MultiPolygon getPolygon(KoordinatenReferenzSystem sourceReferenzSystem, SimpleFeature feature)
		throws FalscherGeometrieTypException {
		Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();
		if (!sourceGeometry.getGeometryType().equals(Geometry.TYPENAME_MULTIPOLYGON)) {
			throw new FalscherGeometrieTypException(Geometry.TYPENAME_MULTIPOLYGON, sourceGeometry.getGeometryType());
		}

		sourceGeometry.setSRID(sourceReferenzSystem.getSrid());
		Geometry transformedGeometry = coordinateConverter.transformGeometry(sourceGeometry,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
		return (MultiPolygon) transformedGeometry;
	}

	protected KoordinatenReferenzSystem getKoordinatenReferenzSystem(SimpleFeatureCollection featureCollection)
		throws FactoryException {
		CoordinateReferenceSystem sourceCRS = featureCollection.getSchema().getCoordinateReferenceSystem();

		String srid = CRS.lookupIdentifier(sourceCRS, true).substring(5);

		return KoordinatenReferenzSystem.ofSrid(Integer.parseInt(srid));
	}

	protected static SimpleFeatureCollection readShapeFile(File shpFile, Charset charset) throws IOException {
		ShapefileDataStore dataStore = new ShapefileDataStore(shpFile.toURI().toURL());
		try {
			dataStore.setCharset(charset);
			return dataStore.getFeatureSource().getFeatures();
		} finally {
			dataStore.dispose();
		}
	}

	protected record GebietskoerperschaftsFeatures(
		Optional<SimpleFeatureCollection> staat,
		Optional<SimpleFeatureCollection> bundesland,
		Optional<SimpleFeatureCollection> regierungsbezirk,
		Optional<SimpleFeatureCollection> kreis,
		SimpleFeatureCollection gemeinde) {
	}

}
