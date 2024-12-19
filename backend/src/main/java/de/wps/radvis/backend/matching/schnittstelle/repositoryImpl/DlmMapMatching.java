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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmw.hmm.SequenceState;
import com.bmw.hmm.Transition;
import com.bmw.hmm.ViterbiAlgorithm;
import com.carrotsearch.hppc.IntHashSet;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.LMProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.HmmProbabilities;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.matching.ObservationWithCandidateStates;
import com.graphhopper.matching.State;
import com.graphhopper.routing.AStarBidirection;
import com.graphhopper.routing.BidirRoutingAlgorithm;
import com.graphhopper.routing.DijkstraBidirectionRef;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.lm.LMApproximator;
import com.graphhopper.routing.lm.LandmarkStorage;
import com.graphhopper.routing.lm.PrepareLandmarks;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.querygraph.VirtualEdgeIteratorState;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.DistancePlaneProjection;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.BBox;

public class DlmMapMatching {
	// Der Graphhopper schaut in einer immer größer werdenen Bounding box, ob er mindestens ein validen snap findet.
	// Sobald er diesen gefunden hat, bricht er aber ab. Indem man in größeren Schritten (höhere Faktor) geht, ist es
	// wahrscheinlicher dass er mehrere valide snaps findet, und darunter der "richtige" liegt.
	// Der algorithmus selbst versucht dann den besten auszuwählen.
	// Das ganze ist also primär ein accuracy/ performance trade-off.
	// Valide Werte: 1, 50 (50 nutzt gleich die maximal erlaubte bounding box range)
	private static final int QUERY_AREA_GROWTH_CYCLE_FACTOR = 10;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Graph graph;
	private final PrepareLandmarks landmarks;
	private final LocationIndexTree locationIndex;
	private double measurementErrorSigma = 50.0;
	private final double transitionProbabilityBeta = 0.01;
	private final int maxVisitedNodes;
	private final DistanceCalc distanceCalc = new DistancePlaneProjection();
	private final Weighting unwrappedWeighting;
	private final BooleanEncodedValue accessEnc;

	public DlmMapMatching(GraphHopper graphHopper, PMap hints) {
		this.locationIndex = (LocationIndexTree) graphHopper.getLocationIndex();

		if (hints.has("vehicle"))
			throw new IllegalArgumentException(
				"MapMatching hints may no longer contain a vehicle, use the profile parameter instead, see core/#1958");
		if (hints.has("weighting"))
			throw new IllegalArgumentException(
				"MapMatching hints may no longer contain a weighting, use the profile parameter instead, see core/#1958");

		if (graphHopper.getProfiles().isEmpty()) {
			throw new IllegalArgumentException(
				"No profiles found, you need to configure at least one profile to use map matching");
		}
		if (!hints.has("profile")) {
			throw new IllegalArgumentException("You need to specify a profile to perform map matching");
		}
		String profileStr = hints.getString("profile", "");
		Profile profile = graphHopper.getProfile(profileStr);
		if (profile == null) {
			List<Profile> profiles = graphHopper.getProfiles();
			List<String> profileNames = new ArrayList<>(profiles.size());
			for (Profile p : profiles) {
				profileNames.add(p.getName());
			}
			throw new IllegalArgumentException(
				"Could not find profile '" + profileStr + "', choose one of: " + profileNames);
		}

		boolean disableLM = hints.getBool(Parameters.Landmark.DISABLE, false);
		boolean disableCH = hints.getBool(Parameters.CH.DISABLE, false);

		// see map-matching/#177: both ch.disable and lm.disable can be used to force Dijkstra which is the better
		// (=faster) choice when the observations are close to each other
		boolean useDijkstra = disableLM || disableCH;

		if (graphHopper.getLMPreparationHandler().isEnabled() && !useDijkstra) {
			// using LM because u-turn prevention does not work properly with (node-based) CH
			List<String> lmProfileNames = new ArrayList<>();
			PrepareLandmarks lmPreparation = null;
			for (LMProfile lmProfile : graphHopper.getLMPreparationHandler().getLMProfiles()) {
				lmProfileNames.add(lmProfile.getProfile());
				if (lmProfile.getProfile().equals(profile.getName())) {
					lmPreparation = graphHopper.getLMPreparationHandler().getPreparation(
						lmProfile.usesOtherPreparation() ? lmProfile.getPreparationProfile() : lmProfile.getProfile()
					);
				}
			}
			if (lmPreparation == null) {
				throw new IllegalArgumentException(
					"Cannot find LM preparation for the requested profile: '" + profile.getName() + "'" +
						"\nYou can try disabling LM using " + Parameters.Landmark.DISABLE + "=true" +
						"\navailable LM profiles: " + lmProfileNames);
			}
			landmarks = lmPreparation;
		} else {
			landmarks = null;
		}
		graph = graphHopper.getGraphHopperStorage();
		unwrappedWeighting = graphHopper.createWeighting(profile, hints);
		this.accessEnc = graphHopper.getEncodingManager().getEncoder("bike").getAccessEnc();
		this.maxVisitedNodes = hints.getInt(Parameters.Routing.MAX_VISITED_NODES, Integer.MAX_VALUE);
	}

	/**
	 * Standard deviation of the normal distribution [m] used for modeling the
	 * GPS error.
	 */
	public void setMeasurementErrorSigma(double measurementErrorSigma) {
		this.measurementErrorSigma = measurementErrorSigma;
	}

	public MatchResult match(List<Observation> observations) {
		List<Observation> filteredObservations = filterObservations(observations);

		// Snap observations to links. Generates multiple candidate snaps per observation.
		List<Collection<Snap>> snapsPerObservation = filteredObservations.stream()
			.map(o -> findCandidateSnaps(o.getPoint().lat, o.getPoint().lon))
			.collect(Collectors.toList());

		// Create the query graph, containing split edges so that all the places where an observation might have happened
		// are a node. This modifies the Snap objects and puts the new node numbers into them.
		QueryGraph queryGraph = QueryGraph.create(graph,
			snapsPerObservation.stream().flatMap(Collection::stream).collect(Collectors.toList()));
		Weighting weighting = queryGraph.wrapWeighting(unwrappedWeighting);

		// Creates candidates from the Snaps of all observations (a candidate is basically a
		// Snap + direction).
		List<ObservationWithCandidateStates> timeSteps = createTimeSteps(filteredObservations, snapsPerObservation,
			queryGraph);

		// Compute the most likely sequence of map matching candidates:
		List<SequenceState<State, Observation, Path>> seq = computeViterbiSequence(timeSteps, queryGraph, weighting);

		List<EdgeIteratorState> path = seq.stream().filter(s1 -> s1.transitionDescriptor != null)
			.flatMap(s1 -> s1.transitionDescriptor.calcEdges().stream()).collect(Collectors.toList());

		MatchResult result = new MatchResult(prepareEdgeMatches(seq, queryGraph));
		result.setMergedPath(new DlmMapMatching.MapMatchedPath(queryGraph, weighting, path));
		result.setMatchMillis(
			seq.stream().filter(s -> s.transitionDescriptor != null).mapToLong(s -> s.transitionDescriptor.getTime())
				.sum());
		result.setMatchLength(seq.stream().filter(s -> s.transitionDescriptor != null)
			.mapToDouble(s -> s.transitionDescriptor.getDistance()).sum());
		// result.setGPXEntriesLength(gpxLength(observations));
		result.setGraph(queryGraph);
		result.setWeighting(weighting);
		return result;
	}

	/**
	 * Filters observations to only those which will be used for map matching (i.e. those which
	 * are separated by at least 2 * measurementErrorSigman
	 */
	private List<Observation> filterObservations(List<Observation> observations) {
		List<Observation> filtered = new ArrayList<>();
		Observation prevEntry = null;
		int last = observations.size() - 1;
		for (int i = 0; i <= last; i++) {
			Observation observation = observations.get(i);
			if (i == 0 || i == last || distanceCalc.calcDist(
				prevEntry.getPoint().getLat(), prevEntry.getPoint().getLon(),
				observation.getPoint().getLat(), observation.getPoint().getLon()) > 2 * measurementErrorSigma) {
				filtered.add(observation);
				prevEntry = observation;
			} else {
				logger.debug("Filter out observation: {}", i + 1);
			}
		}
		return filtered;
	}

	// Änderung: QUERY_AREA_GROWTH_CYCLE_FACTOR eingeführt
	public List<Snap> findCandidateSnaps(final double queryLat, final double queryLon) {
		double rLon = (measurementErrorSigma * 360.0 / DistanceCalcEarth.DIST_EARTH.calcCircumference(queryLat));
		double rLat = measurementErrorSigma / DistanceCalcEarth.METERS_PER_DEGREE;
		Envelope envelope = new Envelope(queryLon, queryLon, queryLat, queryLat);

		int start = 3;
		for (int j = 0; j < start; j++) {
			envelope.expandBy(QUERY_AREA_GROWTH_CYCLE_FACTOR * rLon, QUERY_AREA_GROWTH_CYCLE_FACTOR * rLat);
		}
		for (int i = start; i < 50 / QUERY_AREA_GROWTH_CYCLE_FACTOR; i++) {
			envelope.expandBy(QUERY_AREA_GROWTH_CYCLE_FACTOR * rLon, QUERY_AREA_GROWTH_CYCLE_FACTOR * rLat);
			List<Snap> snaps = findCandidateSnapsInBBox(queryLat, queryLon, BBox.fromEnvelope(envelope));
			if (!snaps.isEmpty()) {
				return snaps;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Analog zur originalen Implementation, nutzt jedoch einen eigenen Filter, da es sein kann, dass entlegene DLM-Kanten
	 * vom Graphhopper als "Subnetwork" klassifiziert und dann nicht betrachtet werden. Mindestens bei Matchings auf
	 * vielen separaten Kanten passiert das und macht auch fachlich keinen Sinn: Wir wollen auf alle möglichen Wege
	 * matchen können, egal ob entlegen oder nicht.
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

	/**
	 * Creates TimeSteps with candidates for the GPX entries but does not create emission or
	 * transition probabilities. Creates directed candidates for virtual nodes and undirected
	 * candidates for real nodes.
	 */
	private List<ObservationWithCandidateStates> createTimeSteps(List<Observation> filteredObservations,
		List<Collection<Snap>> splitsPerObservation, QueryGraph queryGraph) {
		if (splitsPerObservation.size() != filteredObservations.size()) {
			throw new IllegalArgumentException(
				"filteredGPXEntries and queriesPerEntry must have same size.");
		}

		final List<ObservationWithCandidateStates> timeSteps = new ArrayList<>();
		for (int i = 0; i < filteredObservations.size(); i++) {
			Observation observation = filteredObservations.get(i);
			Collection<Snap> splits = splitsPerObservation.get(i);
			List<State> candidates = new ArrayList<>();
			for (Snap split : splits) {
				if (queryGraph.isVirtualNode(split.getClosestNode())) {
					List<VirtualEdgeIteratorState> virtualEdges = new ArrayList<>();
					EdgeIterator iter = queryGraph.createEdgeExplorer().setBaseNode(split.getClosestNode());
					while (iter.next()) {
						if (!queryGraph.isVirtualEdge(iter.getEdge())) {
							throw new RuntimeException("Virtual nodes must only have virtual edges "
								+ "to adjacent nodes.");
						}
						virtualEdges.add((VirtualEdgeIteratorState) queryGraph.getEdgeIteratorState(iter.getEdge(),
							iter.getAdjNode()));
					}
					if (virtualEdges.size() != 2) {
						throw new RuntimeException("Each virtual node must have exactly 2 "
							+ "virtual edges (reverse virtual edges are not returned by the "
							+ "EdgeIterator");
					}

					// Create a directed candidate for each of the two possible directions through
					// the virtual node. We need to add candidates for both directions because
					// we don't know yet which is the correct one. This will be figured
					// out by the Viterbi algorithm.
					candidates.add(new State(observation, split, virtualEdges.get(0), virtualEdges.get(1)));
					candidates.add(new State(observation, split, virtualEdges.get(1), virtualEdges.get(0)));
				} else {
					// Create an undirected candidate for the real node.
					candidates.add(new State(observation, split));
				}
			}

			timeSteps.add(new ObservationWithCandidateStates(observation, candidates));
		}
		return timeSteps;
	}

	/**
	 * Computes the most likely state sequence for the observations.
	 */
	private List<SequenceState<State, Observation, Path>> computeViterbiSequence(
		List<ObservationWithCandidateStates> timeSteps, QueryGraph queryGraph, Weighting weighting) {
		final HmmProbabilities probabilities = new HmmProbabilities(measurementErrorSigma, transitionProbabilityBeta);
		final ViterbiAlgorithm<State, Observation, Path> viterbi = new ViterbiAlgorithm<>();

		int timeStepCounter = 0;
		ObservationWithCandidateStates prevTimeStep = null;
		for (ObservationWithCandidateStates timeStep : timeSteps) {
			final Map<State, Double> emissionLogProbabilities = new HashMap<>();
			Map<Transition<State>, Double> transitionLogProbabilities = new HashMap<>();
			Map<Transition<State>, Path> roadPaths = new HashMap<>();
			for (State candidate : timeStep.candidates) {
				// distance from observation to road in meters
				final double distance = candidate.getSnap().getQueryDistance();
				emissionLogProbabilities.put(candidate, probabilities.emissionLogProbability(distance));
			}

			if (prevTimeStep == null) {
				viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
					emissionLogProbabilities);
			} else {
				final double linearDistance = distanceCalc.calcDist(prevTimeStep.observation.getPoint().lat,
					prevTimeStep.observation.getPoint().lon, timeStep.observation.getPoint().lat,
					timeStep.observation.getPoint().lon);

				for (State from : prevTimeStep.candidates) {
					for (State to : timeStep.candidates) {
						final Path path = createRouter(queryGraph, weighting).calcPath(from.getSnap().getClosestNode(),
							to.getSnap().getClosestNode(),
							from.isOnDirectedEdge() ? from.getOutgoingVirtualEdge().getEdge() : EdgeIterator.ANY_EDGE,
							to.isOnDirectedEdge() ? to.getIncomingVirtualEdge().getEdge() : EdgeIterator.ANY_EDGE);
						if (path.isFound()) {
							double transitionLogProbability = probabilities.transitionLogProbability(path.getDistance(),
								linearDistance);
							Transition<State> transition = new Transition<>(from, to);
							roadPaths.put(transition, path);
							transitionLogProbabilities.put(transition, transitionLogProbability);
						}
					}
				}
				viterbi.nextStep(timeStep.observation, timeStep.candidates,
					emissionLogProbabilities, transitionLogProbabilities,
					roadPaths);
			}
			if (viterbi.isBroken()) {
				fail(timeStepCounter, prevTimeStep, timeStep);
			}

			timeStepCounter++;
			prevTimeStep = timeStep;
		}

		return viterbi.computeMostLikelySequence();
	}

	private void fail(int timeStepCounter, ObservationWithCandidateStates prevTimeStep,
		ObservationWithCandidateStates timeStep) {
		String likelyReasonStr = "";
		if (prevTimeStep != null) {
			double dist = distanceCalc.calcDist(prevTimeStep.observation.getPoint().lat,
				prevTimeStep.observation.getPoint().lon, timeStep.observation.getPoint().lat,
				timeStep.observation.getPoint().lon);
			if (dist > 2000) {
				likelyReasonStr = "Too long distance to previous measurement? "
					+ Math.round(dist) + "m, ";
			}
		}

		throw new IllegalArgumentException("Sequence is broken for submitted track at time step "
			+ timeStepCounter + ". "
			+ likelyReasonStr + "observation:" + timeStep.observation + ", "
			+ timeStep.candidates.size() + " candidates: "
			+ getSnappedCandidates(timeStep.candidates)
			+ ". If a match is expected consider increasing max_visited_nodes.");
	}

	private BidirRoutingAlgorithm createRouter(QueryGraph queryGraph, Weighting weighting) {
		BidirRoutingAlgorithm router;
		if (landmarks != null) {
			AStarBidirection algo = new AStarBidirection(queryGraph, weighting, TraversalMode.EDGE_BASED) {
				@Override
				protected void initCollections(int size) {
					super.initCollections(50);
				}
			};
			LandmarkStorage lms = landmarks.getLandmarkStorage();
			int activeLM = Math.min(8, lms.getLandmarkCount());
			algo.setApproximation(LMApproximator.forLandmarks(queryGraph, lms, activeLM));
			algo.setMaxVisitedNodes(maxVisitedNodes);
			router = algo;
		} else {
			router = new DijkstraBidirectionRef(queryGraph, weighting, TraversalMode.EDGE_BASED) {
				@Override
				protected void initCollections(int size) {
					super.initCollections(50);
				}
			};
			router.setMaxVisitedNodes(maxVisitedNodes);
		}
		return router;
	}

	private List<EdgeMatch> prepareEdgeMatches(List<SequenceState<State, Observation, Path>> seq,
		QueryGraph queryGraph) {
		// This creates a list of directed edges (EdgeIteratorState instances turned the right way),
		// each associated with 0 or more of the observations.
		// These directed edges are edges of the real street graph, where nodes are intersections.
		// So in _this_ representation, the path that you get when you just look at the edges goes from
		// an intersection to an intersection.

		// Implementation note: We have to look at both states _and_ transitions, since we can have e.g. just one state,
		// or two states with a transition that is an empty path (observations snapped to the same node in the query graph),
		// but these states still happen on an edge, and for this representation, we want to have that edge.
		// (Whereas in the ResponsePath representation, we would just see an empty path.)

		// Note that the result can be empty, even when the input is not. Observations can be on nodes as well as on
		// edges, and when all observations are on the same node, we get no edge at all.
		// But apart from that corner case, all observations that go in here are also in the result.

		// (Consider totally forbidding candidate states to be snapped to a point, and make them all be on directed
		// edges, then that corner case goes away.)
		List<EdgeMatch> edgeMatches = new ArrayList<>();
		List<State> states = new ArrayList<>();
		EdgeIteratorState currentDirectedRealEdge = null;
		for (SequenceState<State, Observation, Path> transitionAndState : seq) {
			// transition (except before the first state)
			if (transitionAndState.transitionDescriptor != null) {
				for (EdgeIteratorState edge : transitionAndState.transitionDescriptor.calcEdges()) {
					EdgeIteratorState newDirectedRealEdge = resolveToRealEdge(edge, queryGraph);
					if (currentDirectedRealEdge != null) {
						if (!equalEdges(currentDirectedRealEdge, newDirectedRealEdge)) {
							EdgeMatch edgeMatch = new EdgeMatch(currentDirectedRealEdge, states);
							edgeMatches.add(edgeMatch);
							states = new ArrayList<>();
						}
					}
					currentDirectedRealEdge = newDirectedRealEdge;
				}
			}
			// state
			if (transitionAndState.state.isOnDirectedEdge()) { // as opposed to on a node
				EdgeIteratorState newDirectedRealEdge = resolveToRealEdge(
					transitionAndState.state.getOutgoingVirtualEdge(), queryGraph);
				if (currentDirectedRealEdge != null) {
					if (!equalEdges(currentDirectedRealEdge, newDirectedRealEdge)) {
						EdgeMatch edgeMatch = new EdgeMatch(currentDirectedRealEdge, states);
						edgeMatches.add(edgeMatch);
						states = new ArrayList<>();
					}
				}
				currentDirectedRealEdge = newDirectedRealEdge;
			}
			states.add(transitionAndState.state);
		}
		if (currentDirectedRealEdge != null) {
			EdgeMatch edgeMatch = new EdgeMatch(currentDirectedRealEdge, states);
			edgeMatches.add(edgeMatch);
		}
		return edgeMatches;
	}

	private boolean equalEdges(EdgeIteratorState edge1, EdgeIteratorState edge2) {
		return edge1.getEdge() == edge2.getEdge()
			&& edge1.getBaseNode() == edge2.getBaseNode()
			&& edge1.getAdjNode() == edge2.getAdjNode();
	}

	private EdgeIteratorState resolveToRealEdge(EdgeIteratorState edgeIteratorState, QueryGraph queryGraph) {
		if (queryGraph.isVirtualNode(edgeIteratorState.getBaseNode()) || queryGraph.isVirtualNode(
			edgeIteratorState.getAdjNode())) {
			return graph.getEdgeIteratorStateForKey(
				((VirtualEdgeIteratorState) edgeIteratorState).getOriginalEdgeKey());
		} else {
			return edgeIteratorState;
		}
	}

	private String getSnappedCandidates(Collection<State> candidates) {
		String str = "";
		for (State gpxe : candidates) {
			if (!str.isEmpty()) {
				str += ", ";
			}
			str += "distance: " + gpxe.getSnap().getQueryDistance() + " to "
				+ gpxe.getSnap().getSnappedPoint();
		}
		return "[" + str + "]";
	}

	private static class MapMatchedPath extends Path {
		MapMatchedPath(Graph graph, Weighting weighting, List<EdgeIteratorState> edges) {
			super(graph);
			int prevEdge = EdgeIterator.NO_EDGE;
			for (EdgeIteratorState edge : edges) {
				addDistance(edge.getDistance());
				addTime(GHUtility.calcMillisWithTurnMillis(weighting, edge, false, prevEdge));
				addEdge(edge.getEdge());
				prevEdge = edge.getEdge();
			}
			if (edges.isEmpty()) {
				setFound(false);
			} else {
				setFromNode(edges.get(0).getBaseNode());
				setFound(true);
			}
		}
	}
}
