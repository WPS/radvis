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

package de.wps.radvis.backend.common;

import static org.valid4j.Assertive.require;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.linearref.LengthIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;

public class GeometryTestdataProvider {
	public static final Comparator<Coordinate> LENIENT_COORDINATE_COMPARATOR = (Coordinate c1, Coordinate c2) ->
		Math.abs(c1.x - c2.x) < 1.1 && Math.abs(c1.y - c2.y) < 1.1 ? 0 : -1;

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	public static LineString getLinestringVerschobenUmCoordinate(LineString kante, double x, double y) {
		Coordinate[] oldCoords = kante.getCoordinates();
		Coordinate[] newCoords = new Coordinate[oldCoords.length];

		for (int i = 0; i < oldCoords.length; i++) {
			newCoords[i] = new Coordinate(oldCoords[i].x + x, oldCoords[i].y + y);
		}
		return KanteTestDataProvider.FACTORY.createLineString(newCoords);
	}

	public static LineString createLineString() {
		return createLineString(new Coordinate(0, 1), new Coordinate(1, 2));
	}

	public static LineString createLineString(Coordinate... coordinates) {
		return GEO_FACTORY.createLineString(coordinates);
	}

	public static LineString createLineStringWithCoordinatesMovedToValidBounds(Coordinate... coordinates) {
		return GEO_FACTORY.createLineString(moveAllToValidBounds(coordinates));
	}

	public static Coordinate moveToValidBounds(Coordinate coordinate) {
		require(coordinate.x >= -100000 && coordinate.x <= 100000 && coordinate.y >= -100000
				&& coordinate.y <= 100000,
			"Test-Koordinate muss vor dem Verschieben im Bereich von (-100000,-100000,100000,10000) liegen");
		return new Coordinate(coordinate.x + 450000, coordinate.y + 5400000);
	}

	public static Coordinate[] moveAllToValidBounds(Coordinate... coordinates) {
		return Arrays.stream(coordinates).map(GeometryTestdataProvider::moveToValidBounds)
			.toArray(Coordinate[]::new);
	}

	public static MultiLineString createMultiLineString(LineString... lineStrings) {
		return GEO_FACTORY.createMultiLineString(lineStrings);
	}

	public static MultiLineString createMultiLineString(Coordinate[]... coorArrays) {
		return GEO_FACTORY.createMultiLineString(
			Arrays.stream(coorArrays).map(GEO_FACTORY::createLineString).toArray(LineString[]::new));
	}

	public static MultiPoint createMultiPoint(Coordinate... coordinates) {
		return GEO_FACTORY.createMultiPointFromCoords(coordinates);
	}

	public static Point createPoint(Coordinate coordinate) {
		return GEO_FACTORY.createPoint(coordinate);
	}

	public static MultiPolygon createQuadratischerBereich(double minX, double minY, double maxX, double maxY) {
		return GEO_FACTORY.createMultiPolygon(new Polygon[] {
			GEO_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(minX, minY),
				new Coordinate(minX, maxY),
				new Coordinate(maxX, maxY),
				new Coordinate(maxX, minY),
				new Coordinate(minX, minY)
			})
		});
	}

	public static MultiPolygon getQuadratischenBereichFuerLinestrings(Collection<LineString> lineStrings) {
		Envelope envelope = lineStrings.stream().map(LineString::getEnvelopeInternal)
			.reduce(lineStrings.iterator().next().getEnvelopeInternal(), (env1, env2) -> {
				env1.expandToInclude(env2);
				return env1;
			});
		return GEO_FACTORY.createMultiPolygon(new Polygon[] {
			EnvelopeAdapter.toPolygon(envelope, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()) }
		);
	}

	public static LineString getAbschnitt(LineString lineString, LinearReferenzierterAbschnitt of) {
		LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(lineString);
		return (LineString) lengthIndexedLine.extractLine(of.getVonValue() * lineString.getLength(),
			of.getBisValue() * lineString.getLength());
	}

	public static Geometry creatGeometryCollection() {
		return GEO_FACTORY.createGeometryCollection();
	}

	public static GeometryCollection creatGeometryCollection(Geometry... geometry) {
		return GEO_FACTORY.createGeometryCollection(geometry);
	}
}
