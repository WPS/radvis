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

package de.wps.radvis.backend.common.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

public class CoordinateReferenceSystemConverterUtility {

	private static final Map<Pair<ReferenceIdentifier, ReferenceIdentifier>, MathTransform> crsPairToTransformMap = new HashMap<>();

	/**
	 * Achtung: Nicht diese Methode verwenden, sondern die drunterliegende transformCoordinate,
	 * da diese eine unspezifische RuntimeException wirft und so fehler verschluckt werden koennen
	 */
	public static Coordinate transformCoordinateUnsafe(final Coordinate coordinate,
		KoordinatenReferenzSystem quellReferenzSystem,
		KoordinatenReferenzSystem zielReferenzSystem) {
		try {
			return transformCoordinate(coordinate, quellReferenzSystem, zielReferenzSystem);
		} catch (FactoryException | TransformException e) {
			throw new RuntimeException(e);
		}
	}

	public static Coordinate transformCoordinate(final Coordinate coordinate,
		KoordinatenReferenzSystem quellReferenzSystem,
		KoordinatenReferenzSystem zielReferenzSystem) throws FactoryException, TransformException {

		CoordinateReferenceSystem sourceCrs = quellReferenzSystem.getGeotoolsCRS();
		CoordinateReferenceSystem zielCrs = zielReferenzSystem.getGeotoolsCRS();

		MathTransform transform = getMathTransform(sourceCrs, zielCrs);
		return JTS.transform(coordinate, new Coordinate(), transform);
	}

	public static Geometry transformGeometry(final Geometry geometry,
		KoordinatenReferenzSystem zielReferenzSystem) {
		require(KoordinatenReferenzSystem.isValid(geometry.getSRID()),
			"An der Geometrie muss ein valider SRID gesetzt sein");
		CoordinateReferenceSystem sourceCrs = KoordinatenReferenzSystem.ofSrid(geometry.getSRID()).getGeotoolsCRS();
		CoordinateReferenceSystem zielCrs = zielReferenzSystem.getGeotoolsCRS();

		try {
			MathTransform transform = getMathTransform(sourceCrs, zielCrs);
			Geometry transformedGeometry = JTS.transform(geometry, transform);
			Geometry result = zielReferenzSystem.getGeometryFactory().createGeometry(transformedGeometry);
			ensure(result.getSRID() == zielReferenzSystem.getSrid());
			return result;
		} catch (FactoryException | TransformException e) {
			throw new RuntimeException(e);
		}
	}

	public static SimpleFeature changeFeatureTypeCrsFromFeature(SimpleFeature feature,
		KoordinatenReferenzSystem zielCrs) throws SchemaException {
		setzteSRIDAnGeometrie(feature, zielCrs);
		SimpleFeatureType featureTypeWithZielCrs = FeatureTypes.transform(feature.getFeatureType(),
			zielCrs.getGeotoolsCRS());
		return setzteFeatureType(featureTypeWithZielCrs, feature);
	}

	public static SimpleFeature transformFeature(SimpleFeature feature, KoordinatenReferenzSystem zielCrs)
		throws FactoryException, SchemaException, TransformException {
		CoordinateReferenceSystem quellCrs = feature.getFeatureType().getCoordinateReferenceSystem();
		require(quellCrs, notNullValue());

		SimpleFeatureType featureTypeWithZielCrs = FeatureTypes.transform(feature.getFeatureType(),
			zielCrs.getGeotoolsCRS());

		MathTransform transform = getMathTransform(
			quellCrs,
			zielCrs.getGeotoolsCRS()
		);

		SimpleFeature transformedFeatureWithOldFeatureType = FeatureTypes.transform(feature, featureTypeWithZielCrs,
			transform);

		SimpleFeature transformedFeatureWithZielCrsFeatureType = setzteFeatureType(
			featureTypeWithZielCrs, transformedFeatureWithOldFeatureType);

		// Das transform von oben hat nur die Koordinaten der Geometrie und nicht die SRID veraendert.
		setzteSRIDAnGeometrie(transformedFeatureWithZielCrsFeatureType, zielCrs);

		return transformedFeatureWithZielCrsFeatureType;
	}

	private static SimpleFeature setzteFeatureType(SimpleFeatureType newFeatureType,
		SimpleFeature feature) {
		SimpleFeature featureWithNewFeatureType = SimpleFeatureBuilder.build(
			newFeatureType,
			feature.getAttributes(),
			feature.getID());
		return featureWithNewFeatureType;
	}

	private static void setzteSRIDAnGeometrie(SimpleFeature feature, KoordinatenReferenzSystem zielCrs) {
		Geometry geometry = (Geometry) feature.getDefaultGeometry();
		Geometry result = zielCrs.getGeometryFactory().createGeometry(geometry);
		ensure(result.getSRID() == zielCrs.getSrid());
		feature.setDefaultGeometry(result);
	}

	public static Envelope transformEnvelope(final Envelope envelope,
		KoordinatenReferenzSystem quellReferenzSystem,
		KoordinatenReferenzSystem zielReferenzSystem) {

		CoordinateReferenceSystem sourceCrs = quellReferenzSystem.getGeotoolsCRS();
		CoordinateReferenceSystem zielCrs = zielReferenzSystem.getGeotoolsCRS();

		try {
			MathTransform transform = getMathTransform(sourceCrs, zielCrs);
			return JTS.transform(envelope, transform);
		} catch (FactoryException | TransformException e) {
			throw new RuntimeException(e);
		}
	}

	public static Geometry tauscheLatLong(Geometry geometry) {
		Geometry copy = geometry.copy();

		for (Coordinate c : copy.getCoordinates()) {
			c.setCoordinate(new Coordinate(c.y, c.x, c.z));
		}

		return copy;
	}

	private static MathTransform getMathTransform(CoordinateReferenceSystem sourceCrs,
		CoordinateReferenceSystem zielCrs)
		throws FactoryException {
		Pair<ReferenceIdentifier, ReferenceIdentifier> crsPair = Pair.of(sourceCrs.getName(), zielCrs.getName());
		MathTransform transform = crsPairToTransformMap.get(crsPair);
		if (transform == null) {
			transform = CRS
				.findMathTransform(sourceCrs, zielCrs,
					true);
			crsPairToTransformMap.put(crsPair, transform);
		}
		return transform;
	}
}
