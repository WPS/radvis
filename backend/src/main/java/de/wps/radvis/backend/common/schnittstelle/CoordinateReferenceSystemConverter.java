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

package de.wps.radvis.backend.common.schnittstelle;

import static org.valid4j.Assertive.require;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.KoordinateAusserhalbDesUnterstuetztenBereichsException;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

public class CoordinateReferenceSystemConverter {

	private final Envelope badenWuerttembergEnvelope;

	public CoordinateReferenceSystemConverter(Envelope badenWuerttembergEnvelope) {
		this.badenWuerttembergEnvelope = badenWuerttembergEnvelope;
	}

	/**
	 * Achtung: Nicht diese Methode verwenden, sondern die drunterliegende transformCoordinate,
	 * da diese eine unspezifische RuntimeException wirft und so fehler verschluckt werden koennen
	 */
	public Coordinate transformCoordinateUnsafe(final Coordinate coordinate,
		KoordinatenReferenzSystem quellReferenzSystem,
		KoordinatenReferenzSystem zielReferenzSystem) {
		return CoordinateReferenceSystemConverterUtility.transformCoordinateUnsafe(coordinate, quellReferenzSystem,
			zielReferenzSystem);
	}

	public Coordinate transformCoordinate(final Coordinate coordinate,
		KoordinatenReferenzSystem quellReferenzSystem,
		KoordinatenReferenzSystem zielReferenzSystem) throws FactoryException, TransformException {

		return CoordinateReferenceSystemConverterUtility.transformCoordinate(coordinate, quellReferenzSystem,
			zielReferenzSystem);
	}

	public Geometry transformGeometry(final Geometry geometry,
		KoordinatenReferenzSystem zielReferenzSystem) {
		return CoordinateReferenceSystemConverterUtility.transformGeometry(geometry, zielReferenzSystem);
	}

	public SimpleFeature transformFeature(SimpleFeature feature, KoordinatenReferenzSystem zielCrs)
		throws SchemaException, FactoryException, TransformException {
		return CoordinateReferenceSystemConverterUtility.transformFeature(feature, zielCrs);
	}

	public SimpleFeature changeFeatureTypeCrsFromFeature(SimpleFeature feature,
		KoordinatenReferenzSystem zielCrs) throws SchemaException {
		return CoordinateReferenceSystemConverterUtility.changeFeatureTypeCrsFromFeature(feature, zielCrs);
	}

	public Envelope transformEnvelope(final Envelope envelope,
		KoordinatenReferenzSystem quellReferenzSystem,
		KoordinatenReferenzSystem zielReferenzSystem) {

		return CoordinateReferenceSystemConverterUtility.transformEnvelope(envelope, quellReferenzSystem,
			zielReferenzSystem);
	}

	public Geometry tauscheLatLong(Geometry geometry) {
		return CoordinateReferenceSystemConverterUtility.tauscheLatLong(geometry);
	}

	public boolean sindKoordinatenPlausibel(Geometry geometry) {
		return badenWuerttembergEnvelope.contains(geometry.getEnvelopeInternal());
	}

	public boolean mussLatLongGetauschtWerden(Geometry geom)
		throws KoordinateAusserhalbDesUnterstuetztenBereichsException {
		KoordinatenReferenzSystem koordinatenReferenzSystem = KoordinatenReferenzSystem.ofSrid(geom.getSRID());
		require(koordinatenReferenzSystem.equals(KoordinatenReferenzSystem.WGS84),
			"Diese Methode ist bisher nur fuer WGS84 entwickelt");

		try {
			Geometry transformedNonFlippedGeometry = transformGeometry(geom,
				KoordinatenReferenzSystem.ETRS89_UTM32_N);
			if (sindKoordinatenPlausibel(transformedNonFlippedGeometry)) {
				return false;
			}
		} catch (Exception ignored) {
			// ignorieren es wird nach Coordinaten-Flipping vielleicht klappen
		}

		try {
			Geometry getauscht = tauscheLatLong(geom);
			Geometry transformedFlippedGeometry = transformGeometry(getauscht,
				KoordinatenReferenzSystem.ETRS89_UTM32_N);
			if (sindKoordinatenPlausibel(transformedFlippedGeometry)) {
				return true;
			}
		} catch (Exception ignored) {
			// ignorieren, die KoordinateAusserhalbDesUnterstuetztenBereichsException wird geworfen
		}

		throw new KoordinateAusserhalbDesUnterstuetztenBereichsException(
			"Die Importkoordinaten keiner Geometrie konnten im Bereich von Baden-Württemberg verortet werden.");
	}
}
