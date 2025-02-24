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

import static org.valid4j.Assertive.require;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.IterableUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongIndexedContainer;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;

import de.wps.radvis.backend.common.domain.valueObject.FractionIndexedLine;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.SeitenbezogeneProfilEigenschaften;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsmWayReader extends OSMReader {

	private final DistanceCalc distCalc = DistanceCalcEarth.DIST_EARTH;

	private final Map<Integer, OsmWayId> graphHopperEdgesAufOsmWays;
	private final Map<Integer, SeitenbezogeneProfilEigenschaften> graphHopperEdgesAufProfilEigenschaften;
	private final Map<Integer, LinearReferenzierteOsmWayId> graphHopperEdgesAufOsmWaysWithLR;
	private final EncodingManager encodingManager;
	private final Method getRelFlagsMapMethod;
	private final Method getTmpLatitudeMethod;
	private final Method getTmpLongitudeMethod;
	private final Method getBarrierNodeMethod;
	private final Method addOSMWayMethod;
	private final Method addBarrierEdgeMethod;

	/*
	 * Die drei Maps, die in den Konstruktur mit reingegeben werden, werden beim Aufruf von processWay() innerhalb
	 * von readGraph() befuellt, wenn sie als != null uebergeben werden.
	 * Sie enthalten eine Zuordnung der Edges zu Eigenschaften, die nur zu dem Zeitpunkt zu ermitteln sind, an dem
	 * wir auch die gesamte OsmWay zur verfuegung haben.
	 * Aus diesem Grund und der schlechten Erweiterbarkeit des WayReaders entsteht diese unschoene Struktur.
	 */
	@SneakyThrows
	public OsmWayReader(
		GraphHopperStorage ghStorage,
		Map<Integer, OsmWayId> graphHopperEdgesAufOsmWays,
		Map<Integer, SeitenbezogeneProfilEigenschaften> graphHopperEdgesAufProfilEigenschaften,
		Map<Integer, LinearReferenzierteOsmWayId> graphHopperEdgesAufOsmWaysWithLR
	) {
		super(ghStorage);

		require(!Objects.isNull(graphHopperEdgesAufOsmWays) || !Objects.isNull(graphHopperEdgesAufOsmWaysWithLR),
			"Die OsmWayIds müssen entweder mit oder ohne lineare Referenzen gemappt werden");

		this.graphHopperEdgesAufOsmWays = graphHopperEdgesAufOsmWays;
		this.graphHopperEdgesAufProfilEigenschaften = graphHopperEdgesAufProfilEigenschaften;
		this.graphHopperEdgesAufOsmWaysWithLR = graphHopperEdgesAufOsmWaysWithLR;

		this.encodingManager = ghStorage.getEncodingManager();

		// Viele Methoden haben nur default-access, sind also nur innerhalb des entsprechenden GraphHopper Pakates aus
		// nutzber. Das betrifft uns hier auch, trotz Vererbung von OSMReader. Daher müssen wir hier auf Reflection
		// zurückgreifen, um nicht großflächig Code zu kopieren.
		Class<?> superclass = super.getClass().getSuperclass();

		getRelFlagsMapMethod = superclass.getDeclaredMethod("getRelFlagsMap", long.class);
		getRelFlagsMapMethod.setAccessible(true);

		getTmpLatitudeMethod = superclass.getDeclaredMethod("getTmpLatitude", int.class);
		getTmpLatitudeMethod.setAccessible(true);

		getTmpLongitudeMethod = superclass.getDeclaredMethod("getTmpLongitude", int.class);
		getTmpLongitudeMethod.setAccessible(true);

		getBarrierNodeMethod = superclass.getDeclaredMethod("addBarrierNode", long.class);
		getBarrierNodeMethod.setAccessible(true);

		addBarrierEdgeMethod = superclass.getDeclaredMethod("addBarrierEdge", long.class, long.class, IntsRef.class,
			long.class, long.class);
		addBarrierEdgeMethod.setAccessible(true);

		addOSMWayMethod = superclass.getDeclaredMethod("addOSMWay", LongIndexedContainer.class, IntsRef.class,
			long.class);
		addOSMWayMethod.setAccessible(true);
	}

	@SneakyThrows
	private IntsRef getRelFlagsMap(long osmId) {
		return (IntsRef) getRelFlagsMapMethod.invoke(this, osmId);
	}

	@SneakyThrows
	private double getTmpLatitude(int id) {
		return (double) getTmpLatitudeMethod.invoke(this, id);
	}

	@SneakyThrows
	private double getTmpLongitude(int id) {
		return (double) getTmpLongitudeMethod.invoke(this, id);
	}

	@SneakyThrows
	private long addBarrierNode(long nodeId) {
		return (long) getBarrierNodeMethod.invoke(this, nodeId);
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	private Collection<EdgeIteratorState> addOSMWay(final LongIndexedContainer osmNodeIds, final IntsRef flags,
		final long wayOsmId) {
		return (Collection<EdgeIteratorState>) addOSMWayMethod.invoke(this, osmNodeIds, flags, wayOsmId);
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	private Collection<EdgeIteratorState> addBarrierEdge(long fromId, long toId, IntsRef inEdgeFlags, long nodeFlags,
		long wayOsmId) {
		return (Collection<EdgeIteratorState>) addBarrierEdgeMethod.invoke(this, fromId, toId, inEdgeFlags, nodeFlags,
			wayOsmId);
	}

	/**
	 * Process properties, encode flags and create edges for the way.
	 */
	@Override
	protected void processWay(ReaderWay way) {
		if (way.getNodes().size() < 2)
			return;

		// ignore multipolygon geometry
		if (!way.hasTags())
			return;

		long wayOsmId = way.getId();

		EncodingManager.AcceptWay acceptWay = new EncodingManager.AcceptWay();
		if (!encodingManager.acceptWay(way, acceptWay))
			return;

		IntsRef relationFlags = getRelFlagsMap(way.getId());

		// TODO move this after we have created the edge and know the coordinates => encodingManager.applyWayTags
		LongArrayList osmNodeIds = way.getNodes();
		// Estimate length of ways containing a route tag e.g. for ferry speed calculation
		int first = getNodeMap().get(osmNodeIds.get(0));
		int last = getNodeMap().get(osmNodeIds.get(osmNodeIds.size() - 1));
		double firstLat = getTmpLatitude(first), firstLon = getTmpLongitude(first);
		double lastLat = getTmpLatitude(last), lastLon = getTmpLongitude(last);
		GHPoint estimatedCenter = null;
		if (!Double.isNaN(firstLat) && !Double.isNaN(firstLon) && !Double.isNaN(lastLat) && !Double.isNaN(lastLon)) {
			double estimatedDist = distCalc.calcDist(firstLat, firstLon, lastLat, lastLon);
			// Add artificial tag for the estimated distance and center
			way.setTag("estimated_distance", estimatedDist);
			estimatedCenter = new GHPoint((firstLat + lastLat) / 2, (firstLon + lastLon) / 2);
			way.setTag("estimated_center", estimatedCenter);
		}

		//
		// Manuelle Anpassung:
		//
		// * Betrachtung vom "duration" tag entfernt, da dieser für uns irrelevant ist, da er nur bei Rolltreppen verwendet wird.
		// * Betrachtung von Land-Flächen ("country" tag) entfernt, da für uns irrelevant.
		// * Betrachtung von generellen Flächen ("custom_areas" tag) entfernt, da wir generell keine Flächen betrachten.
		//

		IntsRef edgeFlags = encodingManager.handleWayTags(way, acceptWay, relationFlags);
		if (edgeFlags.isEmpty())
			return;

		List<EdgeIteratorState> createdEdges = new ArrayList<>();
		// look for barriers along the way
		final int size = osmNodeIds.size();
		int lastBarrier = -1;
		for (int i = 0; i < size; i++) {
			long nodeId = osmNodeIds.get(i);
			long nodeFlags = getNodeFlagsMap().get(nodeId);
			// barrier was spotted and the way is passable for that mode of travel
			if (nodeFlags > 0) {
				if (isOnePassable(encodingManager.getAccessEncFromNodeFlags(nodeFlags), edgeFlags)) {
					// remove barrier to avoid duplicates
					getNodeFlagsMap().put(nodeId, 0);

					// create shadow node copy for zero length edge
					long newNodeId = addBarrierNode(nodeId);
					if (i > 0) {
						// start at beginning of array if there was no previous barrier
						if (lastBarrier < 0)
							lastBarrier = 0;

						// add way up to barrier shadow node
						int length = i - lastBarrier + 1;
						LongArrayList partNodeIds = new LongArrayList();
						partNodeIds.add(osmNodeIds.buffer, lastBarrier, length);
						partNodeIds.set(length - 1, newNodeId);
						createdEdges.addAll(addOSMWay(partNodeIds, edgeFlags, wayOsmId));

						// create zero length edge for barrier
						createdEdges.addAll(addBarrierEdge(newNodeId, nodeId, edgeFlags, nodeFlags, wayOsmId));
					} else {
						// run edge from real first node to shadow node
						createdEdges.addAll(addBarrierEdge(nodeId, newNodeId, edgeFlags, nodeFlags, wayOsmId));

						// exchange first node for created barrier node
						osmNodeIds.set(0, newNodeId);
					}
					// remember barrier for processing the way behind it
					lastBarrier = i;
				}
			}
		}

		// just add remainder of way to graph if barrier was not the last node
		if (lastBarrier >= 0) {
			if (lastBarrier < size - 1) {
				LongArrayList partNodeIds = new LongArrayList();
				partNodeIds.add(osmNodeIds.buffer, lastBarrier, size - lastBarrier);
				createdEdges.addAll(addOSMWay(partNodeIds, edgeFlags, wayOsmId));
			}
		} else {
			// no barriers - simply add the whole way
			createdEdges.addAll(addOSMWay(way.getNodes(), edgeFlags, wayOsmId));
		}

		//
		// Manuelle Erweiterung:
		//

		PointList totalPointList = null;
		FractionIndexedLine fractionIndexedLine = null;
		// Falls die OsmWayIds mit LRs gemappt werden sollen, müssen wir hier
		// bereits die Gesamtgeometrie aus den neuen Edges extrahieren
		// und auf dieser Basis eine FractionIndexLine bauen.
		if (shouldMapOsmWayIdsWithLR()) {
			totalPointList = createTotalPointList(createdEdges, osmNodeIds.size());
			// Manchmal gibt es im PBF-File LineStrings mit nur einem Punkt (bzw. mit zweimal demselben Punkt).
			// Das führt in späteren Methoden zu Exceptions - daher ignorieren wir derartige "LineStrings"
			if (totalPointList.size() < 2) {
				return;
			}
			fractionIndexedLine = createFractionIndexedLine(totalPointList);
		}

		// Hier werden die drei dem Konstruktor uebergebenen Maps gefuellt mit den Zuordnungen von
		// den Edges des hier betrachteten OsmWays auf die LinearReferenzierteOsmWays, OsmWays und Profileigenschaften
		SeitenbezogeneProfilEigenschaften seitenbezogeneProfilEigenschaften = getProfilEigenschaften(way);
		for (EdgeIteratorState edge : createdEdges) {
			if (shouldMapOsmWayIdsWithLR() && totalPointList.size() > 1) {
				Optional<LinearReferenzierterAbschnitt> linearReferenzierterAbschnitt = berechneLineareReferenz(
					edge, totalPointList, fractionIndexedLine);
				linearReferenzierterAbschnitt.ifPresent(
					referenzierterAbschnitt -> graphHopperEdgesAufOsmWaysWithLR.put(edge.getEdge(),
						LinearReferenzierteOsmWayId.of(wayOsmId, referenzierterAbschnitt)));
			}
			if (shouldMapOsmWayIdsWithoutLR()) {
				graphHopperEdgesAufOsmWays.put(edge.getEdge(), OsmWayId.of(wayOsmId));
			}
			if (shouldMapProfilEigenschaften()) {
				graphHopperEdgesAufProfilEigenschaften.put(edge.getEdge(), seitenbezogeneProfilEigenschaften);
			}

			// Diese Zeile kommt noch aus dem original WayReader und hat nichts mit der Befuellung der EdgeAuf... Maps zu tun
			encodingManager.applyWayTags(way, edge);
		}
	}

	private SeitenbezogeneProfilEigenschaften getProfilEigenschaften(ReaderWay way) {
		if (!shouldMapProfilEigenschaften()) {
			return null;
		}

		BelagArt belagArtLinks = BelagArt.UNBEKANNT;
		BelagArt belagArtRechts = BelagArt.UNBEKANNT;
		Radverkehrsfuehrung fuehrungLinks = Radverkehrsfuehrung.UNBEKANNT;
		Radverkehrsfuehrung fuehrungRechs = Radverkehrsfuehrung.UNBEKANNT;

		if (way.hasTag("belagart:left")) {
			belagArtLinks = BelagArt.valueOf(way.getTag("belagart:left"));
		}

		if (way.hasTag("belagart:right")) {
			belagArtRechts = BelagArt.valueOf(way.getTag("belagart:right"));
		}

		if (way.hasTag("fuehrung:left")) {
			fuehrungLinks = Radverkehrsfuehrung.valueOf(way.getTag("fuehrung:left"));
		}

		if (way.hasTag("fuehrung:right")) {
			fuehrungRechs = Radverkehrsfuehrung.valueOf(way.getTag("fuehrung:right"));
		}

		return SeitenbezogeneProfilEigenschaften.of(belagArtLinks, belagArtRechts, fuehrungLinks, fuehrungRechs);
	}

	//
	// Manuelle Erweiterung:
	//
	private Optional<LinearReferenzierterAbschnitt> berechneLineareReferenz(
		EdgeIteratorState edgeIteratorState,
		PointList totalPointList,
		FractionIndexedLine fractionIndexedLine) {
		// Da wir durch die doppelten Coordinaten Probleme beim Bauen der LRs haben,
		// Entfernen wir diese sowohl aus der totalPointList als auch aus den edgePointLists
		PointList edgePointList = removeDuplicatePoints(edgeIteratorState.fetchWayGeometry(FetchMode.ALL));

		// Barrier-Edge
		if (edgePointList.size() < 2) {
			return Optional.empty();
		}

		// LR auf die Geometrie des OsmWays ermitteln
		int indexStart = Collections.indexOfSubList(
			IterableUtils.toList(totalPointList),
			IterableUtils.toList(edgePointList)
		);
		if (indexStart < 0) {
			log.warn(
				"Coordinatenfolge einer neuen Edge nicht in der Coordinatenfolge des Osmways enthalten: Konnte {} \n, nicht in {} \n finden",
				edgePointList, totalPointList);
			return Optional.empty();
		}
		int indexEnd = indexStart + edgePointList.size() - 1;

		double von = fractionIndexedLine.getFractionAtIndex(indexStart);
		double bis = fractionIndexedLine.getFractionAtIndex(indexEnd);

		if (von < bis) {
			return Optional.of(LinearReferenzierterAbschnitt.of(von, bis));
		} else {
			log.warn("von = {} >= {} = bis", von, bis);
			return Optional.empty();
		}
	}

	private FractionIndexedLine createFractionIndexedLine(PointList totalPointList) {
		LineString lineString = KoordinatenReferenzSystem.WGS84.getGeometryFactory()
			.createLineString(
				StreamSupport.stream(totalPointList.spliterator(), false)
					.map(ghPoint3D -> new Coordinate(ghPoint3D.getLat(), ghPoint3D.getLon()))
					.toArray(Coordinate[]::new));

		return new FractionIndexedLine(lineString);
	}

	private PointList createTotalPointList(List<EdgeIteratorState> edges, int size) {
		PointList totalPointList = new PointList(size, false);
		edges.stream()
			.map(edgeIteratorState -> edgeIteratorState.fetchWayGeometry(FetchMode.ALL))
			.forEach(totalPointList::add);

		return removeDuplicatePoints(totalPointList);
	}

	private boolean equalsStrict2D(GHPoint3D ghPoint3D, GHPoint3D other) {
		if (ghPoint3D == null || other == null)
			return false;
		return ghPoint3D.lat == other.lat && ghPoint3D.lon == other.lon;
	}

	private static boolean isOnePassable(List<BooleanEncodedValue> checkEncoders, IntsRef edgeFlags) {
		for (BooleanEncodedValue accessEnc : checkEncoders) {
			if (accessEnc.getBool(false, edgeFlags) || accessEnc.getBool(true, edgeFlags))
				return true;
		}
		return false;
	}

	//
	// Manuelle Erweiterung:
	//
	private PointList removeDuplicatePoints(PointList pointList) {
		GHPoint3D last = null;
		PointList temp = new PointList(pointList.size(), false);
		for (GHPoint3D ghPoint3D : pointList) {
			if (equalsStrict2D(ghPoint3D, last)) {
				continue;
			}
			temp.add(ghPoint3D);
			last = ghPoint3D;
		}

		return temp;
	}

	private boolean shouldMapProfilEigenschaften() {
		return !Objects.isNull(this.graphHopperEdgesAufProfilEigenschaften);
	}

	private boolean shouldMapOsmWayIdsWithoutLR() {
		return !Objects.isNull(this.graphHopperEdgesAufOsmWays);
	}

	private boolean shouldMapOsmWayIdsWithLR() {
		return !Objects.isNull(this.graphHopperEdgesAufOsmWaysWithLR);
	}
}
