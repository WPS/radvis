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
import static org.valid4j.Assertive.require;

import java.util.HashMap;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;

import de.topobyte.osm4j.core.model.impl.Node;

/**
 * Der NodeIndex ist im wesentlichen eine Map von Koordinate auf node-ID (nicht auf Knoten-ID).
 *
 * Wichtig ist hierbei, dass es aus Performance- und Speichergründen KEIN spatialer Index ist (also kein QuadTree o.Ä.),
 * der mit Ungenauigkeiten bei angefragten Koordinaten umgehen kann. Kleinste Abweichungen bei den angefragten
 * Koordinaten führen also zu leeren Suchergebnissen.
 */
public class NodeIndex {

	private final HashMap<Coordinate, Long> index;

	public NodeIndex() {
		this.index = new HashMap<>();
	}

	public void fuegeEin(Node node) {
		require(node, notNullValue());
		index.put(getCoordinate(node), node.getId());
	}

	public Optional<Long> finde(Node node) {
		require(node, notNullValue());
		return Optional.ofNullable(index.get(getCoordinate(node)));
	}

	private Coordinate getCoordinate(Node node) {
		return new Coordinate(node.getLatitude(), node.getLongitude());
	}
}
