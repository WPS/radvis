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

package de.wps.radvis.backend.matching.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;

import de.topobyte.osm4j.core.model.impl.Node;
import lombok.Getter;
import lombok.Setter;

public class NodeIndex {

	public static final double PRECISION = 0.000001;

	@Setter
	@Getter
	private double precision;

	private final SpatialIndex index;

	public NodeIndex() {
		this.precision = PRECISION;
		this.index = new Quadtree();
	}

	public void fuegeEin(Node node) {
		require(node, notNullValue());

		Envelope envelope = createEnvelope(node);
		index.insert(envelope, node);
	}

	public Optional<Node> finde(Node node) {
		require(node, notNullValue());

		Envelope envelope = createEnvelope(node);
		@SuppressWarnings("unchecked")
		List<Node> queryResult = index.query(envelope);

		if (queryResult.isEmpty()) {
			return Optional.empty();
		}
		Coordinate coordinate = getCoordinate(node);

		Node resultNode = queryResult.stream()
			.min(Comparator.comparing(n -> getCoordinate(n).distance(coordinate))).get();

		Coordinate resultCoordinate = getCoordinate(node);

		if (!(coordinate.distance(resultCoordinate) <= precision)) {
			return Optional.empty();
		}

		ensure(resultNode, notNullValue());
		return Optional.of(resultNode);
	}

	public NodeIndex getIndexForCoordinatesIn(Envelope envelope) {
		NodeIndex nodeIndex = new NodeIndex();
		nodeIndex.setPrecision(this.precision);

		@SuppressWarnings("unchecked")
		List<Node> allInEnvelope = index.query(envelope);

		allInEnvelope.forEach(nodeIndex::fuegeEin);
		return nodeIndex;
	}

	private Envelope createEnvelope(Node node) {
		Coordinate coordinate = getCoordinate(node);
		return new Envelope(coordinate.x - precision, coordinate.x + precision, coordinate.y - precision,
			coordinate.y + precision);
	}

	private Coordinate getCoordinate(Node node) {
		return new Coordinate(node.getLatitude(), node.getLongitude());
	}
}
