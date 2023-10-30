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

package de.wps.radvis.backend.netz.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;

public class KnotenIndex {
	public static double SNAPPING_DISTANCE = 1.0;
	private SpatialIndex index;
	private Envelope maxExtent;

	public KnotenIndex() {
		this(new Envelope(0, 0, 0, 0));
	}

	public KnotenIndex(Envelope maxExtent) {
		this.maxExtent = maxExtent;
		this.index = new Quadtree();
	}

	public void fuegeEin(Knoten knoten) {
		require(knoten, notNullValue());

		Point point = knoten.getPoint();
		Envelope envelope = createEnvelope(point, SNAPPING_DISTANCE);
		index.insert(envelope, knoten);
	}

	public Optional<Knoten> finde(Point point) {
		return finde(point, SNAPPING_DISTANCE);
	}

	public Optional<Knoten> finde(Point point, double radius) {
		require(point, notNullValue());

		Envelope envelope = createEnvelope(point, radius);
		@SuppressWarnings("unchecked")
		List<Knoten> queryResult = index.query(envelope);

		if (queryResult.isEmpty()) {
			return Optional.empty();
		}

		Knoten resultKnoten = queryResult.stream()
			.min(Comparator.comparing(knoten -> knoten.getPoint().distance(point))).get();

		if (!(point.distance(resultKnoten.getPoint()) <= radius)) {
			return Optional.empty();
		}

		ensure(resultKnoten, notNullValue());
		return Optional.of(resultKnoten);
	}

	public int size() {
		return index.query(new Envelope(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE))
			.size();
	}

	private Envelope createEnvelope(Point point, double radius) {
		Coordinate coordinate = point.getCoordinate();
		return new Envelope(coordinate.x - radius, coordinate.x + radius, coordinate.y - radius, coordinate.y + radius);
	}
}
