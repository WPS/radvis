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
import java.util.Optional;
import java.util.function.BinaryOperator;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.linearref.LengthIndexedLine;

import com.graphhopper.ResponsePath;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.details.PathDetail;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.OsmMatchingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.LinearReferenziertesOsmMatchResult;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsmMatchingRepositoryImpl implements OsmMatchingRepository {

	private CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;
	@Getter
	private OsmMapMatching mapMatchingBike;
	@Getter
	private OsmMapMatching mapMatchingFoot;
	@Getter
	private OsmMapMatching mapMatchingCar;
	private GeometryFactory geometryFactory;
	private OsmMatchedGraphHopper graphHopper;

	public OsmMatchingRepositoryImpl(OsmMatchedGraphHopper graphHopper,
		CoordinateReferenceSystemConverter coordinateReferenceSystemConverter,
		Double measurementErrorSigma) {
		require(graphHopper, notNullValue());
		require(coordinateReferenceSystemConverter, notNullValue());
		require(measurementErrorSigma, notNullValue());

		this.graphHopper = graphHopper;
		this.coordinateReferenceSystemConverter = coordinateReferenceSystemConverter;
		this.geometryFactory = new GeometryFactory(new PrecisionModel(), KoordinatenReferenzSystem.WGS84.getSrid());

		// Für Bedeutung und Effekt bei Veränderung des measurementErrorSigma siehe:
		// https://bis2wps.atlassian.net/wiki/spaces/WI/pages/99352577/Graphhopper#Anpassung-des-Matchings
		initializeMapMatching(measurementErrorSigma);

	}

	private void initializeMapMatching(Double measurementErrorSigma) {
		PMap hintsBike = new PMap();
		hintsBike.putObject("profile", "bike");
		mapMatchingBike = new OsmMapMatching(graphHopper, hintsBike);
		mapMatchingBike.setMeasurementErrorSigma(measurementErrorSigma);
		PMap hintsFoot = new PMap();
		hintsFoot.putObject("profile", "foot");
		mapMatchingFoot = new OsmMapMatching(graphHopper, hintsFoot);
		mapMatchingFoot.setMeasurementErrorSigma(measurementErrorSigma);
		PMap hintsCar = new PMap();
		hintsCar.putObject("profile", "car");
		mapMatchingCar = new OsmMapMatching(graphHopper, hintsCar);
		mapMatchingCar.setMeasurementErrorSigma(measurementErrorSigma);
	}

	@Override
	public LineString matchGeometry(LineString geometrie, String profile) throws KeinMatchGefundenException {
		MatchResult matchResult = this.matcheGeometrie(geometrie, profile);
		return extrahiereLineString(matchResult.getMergedPath().calcPoints(),
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
	}

	@Override
	public LinearReferenziertesOsmMatchResult matchGeometryLinearReferenziert(LineString geometrie, String profile)
		throws KeinMatchGefundenException {
		MatchResult matchResult = this.matcheGeometrie(geometrie, profile);
		List<LinearReferenzierteOsmWayId> osmWayIds = extrahiereWayIdsWithLR(matchResult);
		return new LinearReferenziertesOsmMatchResult(extrahiereLineString(matchResult.getMergedPath().calcPoints(),
			KoordinatenReferenzSystem.ETRS89_UTM32_N), osmWayIds);
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
			switch (profile) {
			case "bike":
				return mapMatchingBike.match(observations);
			case "foot":
				return mapMatchingFoot.match(observations);
			case "car":
				return mapMatchingCar.match(observations);
			default:
				throw new RuntimeException("Profile typ '" + profile + "' nicht unterstützt");
			}
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
			jtsPoints.add(getCoordinate(ghPoint3D));
		}

		LineString result = geometryFactory.createLineString(jtsPoints.toArray(new Coordinate[0]));
		if (KoordinatenReferenzSystem.ETRS89_UTM32_N.equals(koordinatenReferenzSystem))
			return (LineString) coordinateReferenceSystemConverter
				.transformGeometry(result, KoordinatenReferenzSystem.ETRS89_UTM32_N);
		else
			return result;
	}

	private List<LinearReferenzierteOsmWayId> extrahiereWayIdsWithLR(MatchResult matchResult) {
		PointList matchedPointListWGS84 = matchResult.getMergedPath().calcPoints();

		ResponsePath responsePath = getResponsePath(matchResult);

		List<PathDetail> pathDetails = responsePath
			.getPathDetails()
			.get(Parameters.Details.EDGE_ID);

		if (pathDetails == null) {
			log.info("Path Details are null!!! MatchResult: {}\nmatchedPointListWGS84:{}", matchResult,
				matchedPointListWGS84);
			return new ArrayList<>();
		}

		return pathDetails
			.stream()
			.map(pathDetail -> createOsmWayIdWithLR(matchedPointListWGS84, pathDetail))
			.filter(Objects::nonNull)
			// wir fassen benachbarte Abschnitte mit derselben OsmWayId zusammen, um ein schlankeres Ergebnis zu erhalten.
			// Ist nicht unbendingt nötig, erleichtert aber den späteren Umgang ein wenig.
			.map(a -> {
				List<LinearReferenzierteOsmWayId> list = new ArrayList<>();
				list.add(a);
				return list;
			}).reduce(new ArrayList<>(), fasseAbschnitteMitGleicherOsmWayIdZusammen());
	}

	// Hier werden die PathDetails entlang der Geometrie des MatchResults ermittelt und mit dieser
	// als ResponePath zusammengeführt. (Das MatchResult bietet nur einen Path, der aber keine Details enthält)
	private ResponsePath getResponsePath(MatchResult matchResult) {
		PathMerger pathMerger = new PathMerger(matchResult.getGraph(), matchResult.getWeighting()).
			setEnableInstructions(false).
			setPathDetailsBuilders(graphHopper.getPathDetailsBuilderFactory(),
				List.of(Parameters.Details.EDGE_ID)).
			setSimplifyResponse(false);

		return pathMerger
			.doWork(PointList.EMPTY, Collections.singletonList(matchResult.getMergedPath()),
				graphHopper.getEncodingManager(), null);
	}

	private static BinaryOperator<List<LinearReferenzierteOsmWayId>> fasseAbschnitteMitGleicherOsmWayIdZusammen() {
		// Empirisch bei Tests auf Prod-Daten ermittelt. Alle sinnvoll zusammenfassbaren Abschnitte wurden mit
		// diesem Wert zusammengefasst
		double maxDistanzBeiDerBenachbartenAbschnitteNochZusammengefasstWerden = 0.0001;
		// TODO mit Blick auf ProfilEigenschaftenCreator.fasseAbschnitteMitGleichenEigenschaftenZusammen()
		// ggf. vereinheitlichen und gemeinsame Struktur auslagern
		return (a, b) -> {
			if (a.isEmpty()) {
				return b;
			}
			if (a.get(a.size() - 1).getValue().equals(b.get(0).getValue())) {
				LinearReferenzierteOsmWayId last = a.remove(a.size() - 1);

				Optional<LinearReferenzierterAbschnitt> union = last.getLinearReferenzierterAbschnitt()
					.union(b.get(0).getLinearReferenzierterAbschnitt(),
						maxDistanzBeiDerBenachbartenAbschnitteNochZusammengefasstWerden);

				if (union.isEmpty()) {
					// Das passiert gelegntlich durch Kreis-Geometrie im Osm, selten durch Routing/Matching über Multiploygone
					log.debug(
						"Benachbarte OsmWayIdWithLRs ( {} / {} ) mit gleicher WayId konnten nicht zusammengefasst werden",
						last, b.get(0));
					a.add(last);
					a.add(b.get(0));
					return a;
				}
				a.add(LinearReferenzierteOsmWayId.of(last.getValue(), union.get()));

				return a;
			} else {
				a.addAll(b);
				return a;
			}
		};
	}

	private LinearReferenzierteOsmWayId createOsmWayIdWithLR(PointList matchedPointListWGS84, PathDetail pathDetail) {
		Integer edgeId = (Integer) pathDetail.getValue();

		GHPoint3D matchedGeometryVon = matchedPointListWGS84.get(pathDetail.getFirst());
		GHPoint3D matchedGeometryBis = matchedPointListWGS84.get(pathDetail.getLast());

		// Dies ist der Fall wenn eine einfache Kehrtwende (ggf.ein Matching-Artefact) vorliegt,
		// also eine Edge zum Teil (oder in Gänze) hin- und zurückdurchlaufen wird.
		// Das PathDetail enthält dann als first & last dann Indices mit derselben Coordinate.
		// Da Wir keine Geometrien mit Kehrtwenden im DLM haben (sollten), wollen wir auch in den
		// lin. Referenzen auf OsmWays keine Kehrtwenden berücksichtigen.
		if (matchedGeometryVon.equals(matchedGeometryBis)) {
			log.debug("Doppelte Koordinaten (von: {}, bis:{}) in gematchter Geometrie {}. Evtl. eine Kehrtwende.",
				matchedGeometryVon, matchedGeometryBis, matchedPointListWGS84);
			return null;
		}

		// Der Anfang und das Ende der gematchten Geometrien können mitten auf einer Edge liegen.
		// Für die korrekt Berechnung der lin. Referenzen benötigen wir in diesem Fall also
		// die gesamte Geometrie der Edge.
		// Den Teil der gematchten Geometrie, der auf der Edge liegt, könnten wir auch über einen
		// Sublinestring (pathDetail.getFirst() - pathDetail.getLast()) der gematchten Geometrie emittlen.
		// Falls keine Kehrtwende/Anfang/Ende vorliegt entspricht dieser Teil der gesamten Edge.
		// Wir können das aber nicht ohne viel Aufwand prüfen...
		PointList pointListEdge = graphHopper.getGraphHopperStorage()
			.getEdgeIteratorState(edgeId, Integer.MIN_VALUE)
			.fetchWayGeometry(FetchMode.ALL);

		// lin. Ref. Edge auf OsmWay
		LinearReferenzierteOsmWayId linearReferenzierteOsmWayId = graphHopper.getGraphHopperEdgesAufLinRefOsmWaysIds()
			.get(edgeId);

		if (linearReferenzierteOsmWayId == null) {
			log.warn("linearReferenzierteOsmWayId not found for edgeID {}", edgeId);
			return null;
		}

		GHPoint3D edgeVon = pointListEdge.get(0);
		GHPoint3D edgeBis = pointListEdge.get(pointListEdge.size() - 1);
		if (
			(matchedGeometryVon.equals(edgeVon) && matchedGeometryBis.equals(edgeBis))
				|| (matchedGeometryVon.equals(edgeBis) && matchedGeometryBis.equals(edgeVon))
		) {
			// Der betrachtete Abschnitt der gematchten Geometrie entspricht der gesamten Edge:
			// Wir können daher einfach die lin. Ref. der Edge auf den gesammten OsmWay zurückgeben
			return linearReferenzierteOsmWayId;
		}

		// Da entweder die von- oder die bisCoordinate mitten auf der Edge liegen,
		// müssen wir jetzt die LR des betrachteten Abschnitts der gematchten Geometrie auf
		// die Edge ermitteln. Anhand dieser kann dann die LR des betrachteten Abschnitts
		// auf den gesammten OsmWay berechnet werden

		// LengthIndexLine muss auf Geometrien in planarer Projektion angwendet werden
		LineString lineStringEdgeUtm32 = this.extrahiereLineString(pointListEdge,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);

		Coordinate coordinateVonUtm32 = this.coordinateReferenceSystemConverter.transformCoordinateUnsafe(
			getCoordinate(matchedGeometryVon),
			KoordinatenReferenzSystem.WGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);

		Coordinate coordinateBisUmt32 = this.coordinateReferenceSystemConverter.transformCoordinateUnsafe(
			getCoordinate(matchedGeometryBis),
			KoordinatenReferenzSystem.WGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);

		LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(lineStringEdgeUtm32);
		double vonValueGematchterAbschnittAufEdge =
			lengthIndexedLine.indexOf(coordinateVonUtm32) / lineStringEdgeUtm32.getLength();
		double bisValueGematchterAbschnittAufEdge =
			lengthIndexedLine.indexOf(coordinateBisUmt32) / lineStringEdgeUtm32.getLength();

		if (vonValueGematchterAbschnittAufEdge == bisValueGematchterAbschnittAufEdge) {
			log.warn("von == bis");
			return null;
		}

		double temp = vonValueGematchterAbschnittAufEdge;
		vonValueGematchterAbschnittAufEdge = Math.min(vonValueGematchterAbschnittAufEdge,
			bisValueGematchterAbschnittAufEdge);
		bisValueGematchterAbschnittAufEdge = Math.max(temp, bisValueGematchterAbschnittAufEdge);

		LinearReferenzierterAbschnitt linRefAbschnittEdgeAufOsmWay = linearReferenzierteOsmWayId.getLinearReferenzierterAbschnitt();
		double vonValueEdgeAufOsmWay = linRefAbschnittEdgeAufOsmWay.getVonValue();
		double fractionLengthEdgeAufOsmWay = linRefAbschnittEdgeAufOsmWay.getBisValue()
			- vonValueEdgeAufOsmWay;

		double vonValueGematchterAbschnittAufOsmWay =
			fractionLengthEdgeAufOsmWay * vonValueGematchterAbschnittAufEdge + vonValueEdgeAufOsmWay;
		double bisValueGematchterAbschnittAufOsmWay =
			fractionLengthEdgeAufOsmWay * bisValueGematchterAbschnittAufEdge + vonValueEdgeAufOsmWay;

		return LinearReferenzierteOsmWayId.of(linearReferenzierteOsmWayId.getValue(),
			LinearReferenzierterAbschnitt.of(
				vonValueGematchterAbschnittAufOsmWay,
				bisValueGematchterAbschnittAufOsmWay));
	}

	private Coordinate getCoordinate(GHPoint3D matchedGeometryVon) {
		return new Coordinate(matchedGeometryVon.getLat(), matchedGeometryVon.getLon());
	}
}
