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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.details.PathDetail;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;

import de.wps.radvis.backend.common.domain.valueObject.FractionIndexedLine;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.matching.domain.CustomRoutingProfileRepository;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.entity.CustomRoutingProfile;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilRoutingResult;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphhopperRoutingRepositoryImpl implements GraphhopperRoutingRepository {
	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;
	private final GeometryFactory geometryFactory;

	private DlmMatchedGraphHopper graphHopper;
	private final DlmMatchedGraphHopperFactory graphHopperFactory;
	private final CustomRoutingProfileRepository customRoutingProfileRepository;

	public GraphhopperRoutingRepositoryImpl(DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory,
		CoordinateReferenceSystemConverter coordinateReferenceSystemConverter,
		CustomRoutingProfileRepository customRoutingProfileRepository) {

		this.coordinateReferenceSystemConverter = coordinateReferenceSystemConverter;
		this.graphHopperFactory = dlmMatchedGraphHopperFactory;
		this.customRoutingProfileRepository = customRoutingProfileRepository;
		this.graphHopper = graphHopperFactory.getDlmGraphHopper();
		this.geometryFactory = new GeometryFactory(new PrecisionModel(), KoordinatenReferenzSystem.WGS84.getSrid());
	}

	@Override
	public RoutingResult route(List<Coordinate> routeSteps,
		long customProfileId,
		boolean fahrtrichtungBeruecksichtigen) throws KeineRouteGefundenException {
		GHResponse response = this.findRoute(routeSteps, GraphhopperRoutingRepository.DEFAULT_PROFILE_ID,
			fahrtrichtungBeruecksichtigen);
		return extractRoutingResult(response.getBest());
	}

	@Override
	public ProfilRoutingResult routeMitProfileigenschaften(List<Coordinate> routeSteps, long customProfileId,
		boolean fahrtrichtungBeruecksichtigen) throws KeineRouteGefundenException {

		GHResponse response = this.findRoute(routeSteps, customProfileId, fahrtrichtungBeruecksichtigen);
		RoutingResult routingResult = extractRoutingResult(response.getBest());

		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften = extractLinearReferenzierteProfilEigenschaften(
			response.getBest());

		return new ProfilRoutingResult(routingResult, linearReferenzierteProfilEigenschaften);
	}

	private RoutingResult extractRoutingResult(ResponsePath bestResponse) {
		LineString routedGeometry = extrahiereLineString(bestResponse.getPoints(),
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
		List<PathDetail> pathDetailsEdgeIds = bestResponse.getPathDetails().get(Parameters.Details.EDGE_ID);

		Hoehenunterschied anstieg = Hoehenunterschied.of(bestResponse.getAscend());
		Hoehenunterschied abstieg = Hoehenunterschied.of(bestResponse.getDescend());

		List<Long> wayIDs = pathDetailsEdgeIds.stream().map(PathDetail::getValue)
			.map(value -> (Integer) value)
			.map(graphhopperID -> graphHopper.getGraphHopperEdgesAufOsmWays().get(graphhopperID))
			.map(OsmWayId::getValue)
			.collect(Collectors.toList());

		return new RoutingResult(wayIDs, routedGeometry, anstieg, abstieg);
	}

	private List<LinearReferenzierteProfilEigenschaften> extractLinearReferenzierteProfilEigenschaften(
		ResponsePath bestResponse) {
		// Die Graphhopper WGS84Geometrien haben lat and lon vertauscht in Graphhopper und
		// createLinearReferenzierteProfilEigenschaften arbeitet mit Graphhoppergeometrien, weshalb wir nicht
		// die extrahiereLinestring Logik hierfür verwenden
		PointList pointList = bestResponse.getPoints();
		LineString wgs84Geometrie = pointList.toLineString(false);

		// Die FractionIndexedLine transformiert die Geometrie in eine planare Projektion (utm32)
		// Damit das geht müssen hier die Koordinaten korrekt herum getauscht werden
		FractionIndexedLine fractionIndexedLine = new FractionIndexedLine(
			extrahiereLineString(pointList, KoordinatenReferenzSystem.WGS84));

		return bestResponse
			.getPathDetails()
			.get(Parameters.Details.EDGE_ID)
			.stream()
			.map(pathDetail -> ProfilEigenschaftenCreator.createLinearReferenzierteProfilEigenschaften(graphHopper,
				wgs84Geometrie,
				fractionIndexedLine,
				pathDetail))
			.filter(Objects::nonNull)
			.collect(ProfilEigenschaftenCreator.fasseAbschnitteMitGleichenEigenschaftenZusammen());
	}

	private LineString extrahiereLineString(PointList pointList, KoordinatenReferenzSystem koordinatenReferenzSystem) {
		final var jtsPoints = new ArrayList<Coordinate>();
		for (GHPoint3D ghPoint3D : pointList) {
			final var coordinate = new Coordinate(ghPoint3D.getLat(), ghPoint3D.getLon(), ghPoint3D.getEle());
			jtsPoints.add(coordinate);
		}

		LineString result = geometryFactory.createLineString(jtsPoints.toArray(new Coordinate[0]));
		if (koordinatenReferenzSystem.equals(KoordinatenReferenzSystem.ETRS89_UTM32_N))
			return (LineString) coordinateReferenceSystemConverter
				.transformGeometry(result, KoordinatenReferenzSystem.ETRS89_UTM32_N);
		else
			return result;
	}

	private GHResponse findRoute(List<Coordinate> routeSteps, long customProfileId,
		boolean fahrtrichtungBeruecksichtigen) throws KeineRouteGefundenException {
		require(this.graphHopper, notNullValue());
		final List<GHPoint> points = routeSteps.stream()
			.map(utmCoord -> coordinateReferenceSystemConverter.transformCoordinateUnsafe(utmCoord,
				KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84))
			.map(wgsCoord -> new GHPoint(wgsCoord.x, wgsCoord.y))
			.collect(Collectors.toList());

		final var request = new GHRequest(points);
		String profileName = fahrtrichtungBeruecksichtigen ? "bike" : "foot";
		if (customProfileId != GraphhopperRoutingRepository.DEFAULT_PROFILE_ID) {
			profileName = "bike_custom";
		}
		checkProfileExists(profileName);
		request.setProfile(profileName);
		request.setPathDetails(List.of(Parameters.Details.EDGE_ID));

		if (customProfileId != GraphhopperRoutingRepository.DEFAULT_PROFILE_ID) {
			try {
				String jsonProfile = getCustomRoutingProfile(customProfileId);
				CustomModel model = CustomRoutingProfile.parseCustomModel(jsonProfile);

				request.setCustomModel(model);
			} catch (IOException e) {
				String msg = "Parsen des Custom-Model JSONs fehlgeschlagen";
				log.error(msg, e);
				throw new RuntimeException(msg, e);
			}
		}

		final var response = graphHopper.route(request);

		if (response.hasErrors()) {
			log.info(response.getErrors().stream().map(Throwable::toString).collect(Collectors.joining()));
			throw new KeineRouteGefundenException(
				"Die Routing-Response muss fehlerfrei sein: " +
					response.getErrors().stream().map(Throwable::toString).collect(Collectors.joining()));
		}

		return response;
	}

	@NotNull
	private String getCustomRoutingProfile(long customRoutingProfileId) {
		return customRoutingProfileRepository.findById(customRoutingProfileId).orElseThrow(EntityNotFoundException::new)
			.getProfilJson();
	}

	private void checkProfileExists(String profileName) {
		Profile profile = graphHopper.getProfile(profileName);
		if (profile == null) {
			List<Profile> profiles = graphHopper.getProfiles();
			List<String> profileNames = new ArrayList<>(profiles.size());
			for (Profile p : profiles) {
				profileNames.add(p.getName());
			}
			throw new IllegalArgumentException(
				"Could not find profile '" + profileName + "', choose one of: " + profileNames);
		}
	}

	@Override
	public void swapGraphHopper() {
		log.info("Swapping Graphhopper for DLM-Routing");
		// darf nur mir graphHopperFactory.getDlmGraphHopper überschrieben werden, da die factory das clean-up handelt
		this.graphHopper = graphHopperFactory.getDlmGraphHopper();
	}
}
