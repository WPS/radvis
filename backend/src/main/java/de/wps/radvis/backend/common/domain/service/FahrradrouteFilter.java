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

package de.wps.radvis.backend.common.domain.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.index.strtree.STRtree;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

public class FahrradrouteFilter {
	private double erlaubterAbstand;
	private STRtree strTree;

	public FahrradrouteFilter(List<Geometry> fahrradroutenGeometrien, double erlaubterAbstand) {
		require(fahrradroutenGeometrien, notNullValue());
		require(!fahrradroutenGeometrien.isEmpty());

		this.erlaubterAbstand = erlaubterAbstand;
		// Die einzelnen MultiLineStrings aufteilen und jedes einzelne Segment (also Strecke zwischen zwei
		// Koordinaten) in den Index einfügen, statt ganze (Multi)LineStrings einzufügen. Diese Segmente sind
		// natürlich wesentlich kleiner als die Fahrradroute selbst. Weiter unten nutzen wir die Envelopes von
		// Routen und Maßnahmen um diejenigen Maßnahmen in er Nähe von Routen zu finden. Ist nun eine gesamte
		// Fahrradroute mit ihrem riesigen Envelope (bei LRFW teilweise halb BW) im Index, dann ist so ziemlich
		// jede Maßnahme "in der Nähe", was die Idee von räumlichen Queries irgendwo absurd macht. Denn für jede
		// Maßnahme muss man nochmal exakt die Distanz berechnen um wirklich sicherzugehen, dass sie innerhalb
		// des konfigurierten Radius ist. Diese Distanzberechnung ist aber sehr teuer und wir wollen so wenig
		// davon wie möglich machen.
		// Deswegen fügen wir die kleineren Segmente ein. Die Query ist dann ein Stück weit genauer, heißt ganz
		// viele Maßnahmen werden direkt aussortiert, weil sie weit abseits der angefragten Fahrradrouten sind.
		// Wir brauchen dann also nur noch wenige exakte Distanzberechnungen, was uns viel zeit spart. Es mag
		// kontraintuitiv sein mehr Objekte in den Index einzufügen um schneller zu sein, aber so ist das
		// nunmal.
		strTree = new STRtree();
		fahrradroutenGeometrien.stream()
			.flatMap(geom -> unwrap(geom).stream())
			.forEach(geom -> {
				Coordinate[] coordinates = geom.getCoordinates();

				for (int i = 0; i < coordinates.length - 1; i++) {
					Coordinate[] segmentCoords = new Coordinate[2];
					segmentCoords[0] = coordinates[i];
					segmentCoords[1] = coordinates[i + 1];

					LineString segment = KoordinatenReferenzSystem.ETRS89_UTM32_N
						.getGeometryFactory()
						.createLineString(segmentCoords);

					strTree.insert(segment.getEnvelopeInternal(), segment);
				}
			});
	}

	public boolean contains(Geometry geometry) {
		return contains(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createGeometryCollection(new Geometry[] { geometry }));
	}

	@SuppressWarnings("unchecked")
	public boolean contains(GeometryCollection geometryCollection) {
		require(geometryCollection, notNullValue());
		// Zunächst holen wir alle Fahrradrouten-Segmente, deren Envelopes mit dem erweiterten Envelope
		// der Maßnahme überlappen. Das ist eine sehr schnelle aber auch sehr ungenaue Operation. Daher
		// müssen alle Segmente nochmal durchgegangen werden, ob die Maßnahme wirklich innerhalb von
		// x Metern an einem der Segmente dran ist. Das ist eine verhältnißmäßig teure Operation,
		// deswegen die Vorfilterung nach Envelope auf den kleinen Segmenten, um möglichst wenig exakte
		// aber teure Berechnungen der Distanz machen zu müssen.
		Envelope bufferedMassnahmeEnvelope = geometryCollection.getEnvelopeInternal();
		bufferedMassnahmeEnvelope.expandBy(erlaubterAbstand);
		return strTree.query(bufferedMassnahmeEnvelope)
			.stream()
			.anyMatch(routeSegment -> {
				return ((Geometry) routeSegment).isWithinDistance(geometryCollection,
					erlaubterAbstand);
			});
	}

	private List<LineString> unwrap(Geometry multiLineString) {
		ArrayList<LineString> result = new ArrayList<>();

		for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
			Geometry childGeometry = multiLineString.getGeometryN(i);
			if (childGeometry instanceof MultiLineString) {
				result.addAll(unwrap((MultiLineString) childGeometry));
			} else if (childGeometry instanceof LineString) {
				result.add((LineString) childGeometry);
			} else {
				throw new RuntimeException("Unsupported geometry type " + childGeometry.getGeometryType()
					+ " during unwrap of MultiLineString.");
			}
		}

		return result;
	}
}
