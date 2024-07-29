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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import static com.graphhopper.util.DistancePlaneProjection.DIST_PLANE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.locationtech.jts.geom.Envelope;

import com.carrotsearch.hppc.IntHashSet;
import com.graphhopper.GraphHopper;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import com.graphhopper.util.shapes.BBox;

public class OsmMapMatching extends MapMatching {
	// Der Graphhopper schaut in einer immer größer werdenden Bounding box, ob er mindestens ein validen snap findet.
	// Sobald er diesen gefunden hat, bricht er aber ab. Indem man in größeren Schritten (höherer Faktor) geht, ist es
	// wahrscheinlicher, dass er mehrere valide snaps findet, und darunter der "richtige" liegt.
	// Der Algorithmus selbst versucht dann den besten auszuwählen.
	// Das Ganze ist also primär ein accuracy/ performance trade-off.
	// Valide Werte: 1, 50 (50 nutzt gleich die maximal erlaubte bounding box range)
	private static final int QUERY_AREA_GROWTH_CYCLE_FACTOR = 10;

	private final Graph graph;
	private final LocationIndexTree locationIndex;
	private double measurementErrorSigma = 50.0;
	private final BooleanEncodedValue accessEnc;

	public OsmMapMatching(GraphHopper graphHopper, PMap hints, Double measurementErrorSigma) {
		super(graphHopper, hints);

		this.measurementErrorSigma = measurementErrorSigma;
		this.locationIndex = (LocationIndexTree) graphHopper.getLocationIndex();
		this.graph = graphHopper.getGraphHopperStorage();
		this.accessEnc = graphHopper.getEncodingManager().getEncoder("bike").getAccessEnc();

		setTransitionProbabilityBeta(0.2);
		setMeasurementErrorSigma(this.measurementErrorSigma);
	}

	/**
	 * Analog zur originalen Implementation, bricht jedoch nicht ab, sobald ein snap gefunden wurde, sondern versucht
	 * weitere zu finden. Zudem wird hier die eigene Implementierung von {@code findCandidateSnapsInBBox} genutzt.
	 */
	public List<Snap> findCandidateSnaps(final double queryLat, final double queryLon) {
		double rLon = (measurementErrorSigma * 360.0 / DistanceCalcEarth.DIST_EARTH.calcCircumference(queryLat));
		double rLat = measurementErrorSigma / DistanceCalcEarth.METERS_PER_DEGREE;
		Envelope envelope = new Envelope(queryLon, queryLon, queryLat, queryLat);

		envelope.expandBy(QUERY_AREA_GROWTH_CYCLE_FACTOR * rLon, QUERY_AREA_GROWTH_CYCLE_FACTOR * rLat);
		for (int i = 1; i < 50 / QUERY_AREA_GROWTH_CYCLE_FACTOR; i++) {
			envelope.expandBy(QUERY_AREA_GROWTH_CYCLE_FACTOR * rLon, QUERY_AREA_GROWTH_CYCLE_FACTOR * rLat);
			List<Snap> snaps = findCandidateSnapsInBBox(queryLat, queryLon, BBox.fromEnvelope(envelope));
			if (!snaps.isEmpty()) {
				return snaps;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Analog zur originalen Implementation, nutzt jedoch einen eigenen Filter, da es sein kann, dass entlegene OSM-Ways
	 * vom Graphhopper als "Subnetwork" klassifiziert und dann nicht betrachtet werden. Mindestens in Tests passiert das
	 * und macht auch fachlich keinen Sinn: Wir wollen auf alle möglichen Wege matchen, egal ob subnetwork oder nicht.
	 */
	private List<Snap> findCandidateSnapsInBBox(double queryLat, double queryLon, BBox queryShape) {
		EdgeFilter edgeAccessFilter = edgeState -> {
			return edgeState.get(accessEnc) || edgeState.getReverse(accessEnc);
		};
		List<Snap> snaps = new ArrayList<>();
		IntHashSet seenEdges = new IntHashSet();
		IntHashSet seenNodes = new IntHashSet();
		locationIndex.query(queryShape, edgeId -> {
			EdgeIteratorState edge = graph.getEdgeIteratorStateForKey(edgeId * 2);
			if (seenEdges.add(edgeId) && edgeAccessFilter.accept(edge)) {
				Snap snap = new Snap(queryLat, queryLon);
				locationIndex.traverseEdge(queryLat, queryLon, edge, (node, normedDist, wayIndex, pos) -> {
					if (normedDist < snap.getQueryDistance()) {
						snap.setQueryDistance(normedDist);
						snap.setClosestNode(node);
						snap.setWayIndex(wayIndex);
						snap.setSnappedPosition(pos);
					}
				});
				double dist = DIST_PLANE.calcDenormalizedDist(snap.getQueryDistance());
				snap.setClosestEdge(edge);
				snap.setQueryDistance(dist);
				if (snap.isValid() && (snap.getSnappedPosition() != Snap.Position.TOWER || seenNodes.add(
					snap.getClosestNode()))) {
					snap.calcSnappedPoint(DistanceCalcEarth.DIST_EARTH);
					if (queryShape.contains(snap.getSnappedPoint().lat, snap.getSnappedPoint().lon)) {
						snaps.add(snap);
					}
				}
			}
		});
		return snaps;
	}
}
