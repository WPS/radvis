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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import com.graphhopper.ResponsePath;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.routing.querygraph.VirtualEdgeIteratorState;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;

import de.wps.radvis.backend.common.domain.valueObject.FractionIndexedLine;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmMatchingRepositoryImpl implements DlmMatchingRepository {

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;
	@Getter
	private DlmMapMatching mapMatchingBike;

	@Getter
	private DlmMapMatching mapMatchingFoot;

	private final GeometryFactory geometryFactory;

	private DlmMatchedGraphHopper graphHopper;
	private DlmMatchedGraphHopperFactory graphHopperFactory;
	private Double measurementErrorSigma;

	public DlmMatchingRepositoryImpl(DlmMatchedGraphHopperFactory graphHopperFactory,
		CoordinateReferenceSystemConverter coordinateReferenceSystemConverter,
		Double measurementErrorSigma) {
		this.measurementErrorSigma = measurementErrorSigma;
		require(graphHopperFactory, notNullValue());
		require(coordinateReferenceSystemConverter, notNullValue());
		require(measurementErrorSigma, notNullValue());

		this.coordinateReferenceSystemConverter = coordinateReferenceSystemConverter;
		this.geometryFactory = new GeometryFactory(new PrecisionModel(), KoordinatenReferenzSystem.WGS84.getSrid());

		this.graphHopperFactory = graphHopperFactory;
		this.graphHopper = this.graphHopperFactory.getDlmGraphHopper();

		PMap hintsBike = new PMap();
		hintsBike.putObject("profile", "bike");
		mapMatchingBike = new DlmMapMatching(graphHopper, hintsBike);
		mapMatchingBike.setMeasurementErrorSigma(measurementErrorSigma);

		PMap hintsFoot = new PMap();
		hintsFoot.putObject("profile", "foot");
		mapMatchingFoot = new DlmMapMatching(graphHopper, hintsFoot);
		mapMatchingFoot.setMeasurementErrorSigma(measurementErrorSigma);
	}

	@Override
	public OsmMatchResult matchGeometry(LineString geometrie, String profile)
		throws KeinMatchGefundenException {
		MatchResult matchResult = this.matcheGeometrie(geometrie, profile);
		List<OsmWayId> osmWayIds = extrahiereWayIds(matchResult);
		return new OsmMatchResult(extrahiereLineString(matchResult.getMergedPath().calcPoints(),
			KoordinatenReferenzSystem.ETRS89_UTM32_N), osmWayIds);
	}

	@Override
	public ProfilMatchResult matchGeometryUndDetails(LineString geometrie, String profile)
		throws KeinMatchGefundenException {
		MatchResult matchResult = this.matcheGeometrie(geometrie, profile);
		LineString gematchedteGeometrie = extrahiereLineString(matchResult.getMergedPath().calcPoints(),
			KoordinatenReferenzSystem.ETRS89_UTM32_N);

		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften = getLinearReferenzierteProfilEigenschaften(
			matchResult);
		List<OsmWayId> osmWayIds = extrahiereWayIds(matchResult);

		return new ProfilMatchResult(gematchedteGeometrie, osmWayIds, linearReferenzierteProfilEigenschaften);
	}

	private List<LinearReferenzierteProfilEigenschaften> getLinearReferenzierteProfilEigenschaften(
		MatchResult matchResult) {
		// Die Graphhopper WGS84Geometrien haben lat and lon vertauscht in Graphhopper und
		// ProfilEigenschaftenService.createLinearReferenzierteProfilEigenschaften arbeitet mit Graphhoppergeometrien,
		// weshalb wir nicht die extrahiereLinestring Logik hierfür verwenden (die lat und lon umdrehen)
		PointList pointList = matchResult.getMergedPath().calcPoints();
		LineString matchedGeometryWSG84 = pointList.toLineString(false);

		// Die FractionIndexedLine transformiert die Geometrie in eine planare Projektion (utm32)
		// Damit das geht müssen hier die Koordinaten korrekt herum getauscht werden
		FractionIndexedLine fractionIndexedLine = new FractionIndexedLine(
			extrahiereLineString(pointList, KoordinatenReferenzSystem.WGS84));

		ResponsePath responsePath = getResponsePath(matchResult);

		return responsePath
			.getPathDetails()
			.get(Parameters.Details.EDGE_ID)
			.stream()
			.map(pathDetail -> ProfilEigenschaftenCreator.createLinearReferenzierteProfilEigenschaften(
				graphHopper, matchedGeometryWSG84, fractionIndexedLine,
				pathDetail))
			.filter(Objects::nonNull)
			.collect(ProfilEigenschaftenCreator.fasseAbschnitteMitGleichenEigenschaftenZusammen());
	}

	// Hier werden die PathDetails entlang der Geometrie des MatchResults ermittelt und mit dieser
	// als ResponePath zusammengeführt. (Das MatchResult bietet nur einen Path, der aber keine Details enthält)
	private ResponsePath getResponsePath(MatchResult matchResult) {
		PathMerger pathMerger = new PathMerger(matchResult.getGraph(), matchResult.getWeighting())
			.setEnableInstructions(false).setPathDetailsBuilders(graphHopper.getPathDetailsBuilderFactory(),
				List.of(Parameters.Details.EDGE_ID)).setSimplifyResponse(false);
		return pathMerger
			.doWork(PointList.EMPTY, Collections.singletonList(matchResult.getMergedPath()),
				graphHopper.getEncodingManager(), null);
	}

	@Override
	public void swapGraphHopper() {
		log.info("Swapping Graphhopper for DLM-Matching");

		// darf nur mir graphHopperFactory.getDlmGraphHopper überschrieben werden, da die factory das clean-up handelt
		graphHopper = graphHopperFactory.getDlmGraphHopper();

		PMap hintsBike = new PMap();
		hintsBike.putObject("profile", "bike");
		mapMatchingBike = new DlmMapMatching(graphHopper, hintsBike);
		mapMatchingBike.setMeasurementErrorSigma(measurementErrorSigma);

		PMap hintsFoot = new PMap();
		hintsFoot.putObject("profile", "foot");
		mapMatchingFoot = new DlmMapMatching(graphHopper, hintsFoot);
		mapMatchingFoot.setMeasurementErrorSigma(measurementErrorSigma);
	}

	private MatchResult matcheGeometrie(LineString lineStringInUtm, String profile) throws KeinMatchGefundenException {
		require(lineStringInUtm, notNullValue());
		require(lineStringInUtm.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
				+ "' entspricht nicht lineStringInUtm.getSRID() '" + lineStringInUtm.getSRID() + "'");
		require(profile, notNullValue());

		LineString lineStringInWgs84 = (LineString) coordinateReferenceSystemConverter
			.transformGeometry(lineStringInUtm,
				KoordinatenReferenzSystem.WGS84);

		List<Observation> observations = new ArrayList<>();
		Coordinate[] coordinates = lineStringInWgs84.getCoordinates();

		for (Coordinate coordinate : coordinates) {
			GHPoint ghStart = new GHPoint(coordinate.getX(), coordinate.getY());
			observations.add(new Observation(ghStart));
		}

		try {
			if ("bike".equals(profile)) {
				return mapMatchingBike.match(observations);
			} else if ("foot".equals(profile)) {
				return mapMatchingFoot.match(observations);
			}
			throw new RuntimeException("Profile typ '" + profile + "' nicht unterstützt");
		} catch (IllegalArgumentException e) {
			throw new KeinMatchGefundenException(
				"Der LineString konnte nicht gematched werden. Dies liegt beispielsweise daran, dass die Geometrie oder"
					+ " Teile nicht Teil der importierten OsmBasiskarte sind oder dass die Geometrie keine passendes"
					+ " Pendant in den OSM-Daten hat",
				e);
		}
	}

	private LineString extrahiereLineString(PointList pointList, KoordinatenReferenzSystem koordinatenReferenzSystem) {
		List<Coordinate> jtsPoints = new ArrayList<>();
		for (GHPoint3D ghPoint3D : pointList) {
			Coordinate coordinate = new Coordinate(ghPoint3D.getLat(), ghPoint3D.getLon(), ghPoint3D.getEle());
			jtsPoints.add(coordinate);
		}

		LineString result = geometryFactory.createLineString(jtsPoints.toArray(new Coordinate[0]));
		if (KoordinatenReferenzSystem.ETRS89_UTM32_N.equals(koordinatenReferenzSystem))
			return (LineString) coordinateReferenceSystemConverter
				.transformGeometry(result, KoordinatenReferenzSystem.ETRS89_UTM32_N);
		else
			return result;
	}

	public List<OsmWayId> extrahiereWayIds(MatchResult matchResult) {
		List<OsmWayId> wayIds = new ArrayList<>();

		OsmWayId previousOsmWayId = null;
		boolean previousWasReverse = false;

		for (EdgeIteratorState edge : matchResult.getMergedPath().calcEdges()) {
			boolean currentIsReverse = edge.getReverse(EdgeIteratorState.REVERSE_STATE);

			Pair<Integer, Boolean> edgeIdUndIstVirtuell = extrahiereEdgeId(edge);
			Integer edgeId = edgeIdUndIstVirtuell.getLeft();
			OsmWayId osmWayId = graphHopper.getGraphHopperEdgesAufOsmWays().get(edgeId);

			if (!osmWayId.equals(previousOsmWayId) || previousWasReverse != currentIsReverse) {
				wayIds.add(osmWayId);
			}

			previousOsmWayId = osmWayId;
			previousWasReverse = currentIsReverse;
		}

		return wayIds;
	}

	private Pair<Integer, Boolean> extrahiereEdgeId(EdgeIteratorState edgeIteratorState) {
		if (edgeIteratorState instanceof VirtualEdgeIteratorState) {
			// first, via and last edges can be virtual
			VirtualEdgeIteratorState virtualEdge = (VirtualEdgeIteratorState) edgeIteratorState;
			// Warum?
			int edgeId = virtualEdge.getOriginalEdgeKey() / 2;
			log.debug("Edge {} ist virtuell.", edgeId);
			return Pair.of(edgeId, true);
		}
		return Pair.of(edgeIteratorState.getEdge(), false);
	}
}
