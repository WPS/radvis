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

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

public class CoordinateReferenceSystemConverter {

	private final Envelope obersteGebietskoerperschaftEnvelope;

	public CoordinateReferenceSystemConverter(Envelope obersteGebietskoerperschaftEnvelope) {
		this.obersteGebietskoerperschaftEnvelope = obersteGebietskoerperschaftEnvelope;
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

	public boolean sindKoordinatenPlausibel(Geometry geometry) {
		return obersteGebietskoerperschaftEnvelope.contains(geometry.getEnvelopeInternal());
	}
}
