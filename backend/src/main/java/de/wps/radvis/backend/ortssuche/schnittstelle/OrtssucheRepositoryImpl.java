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

package de.wps.radvis.backend.ortssuche.schnittstelle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.web.client.RestTemplate;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.ortssuche.domain.OrtsSucheConfigurationProperties;
import de.wps.radvis.backend.ortssuche.domain.OrtssucheRepository;
import de.wps.radvis.backend.ortssuche.domain.entity.OrtsSucheErgebnis;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrtssucheRepositoryImpl implements OrtssucheRepository {
	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	private String url;
	private int anzahlSuchergebnisse;
	private String bBox;

	private RestTemplate suchRestTemplate;

	public OrtssucheRepositoryImpl(CoordinateReferenceSystemConverter coordinateReferenceSystemConverter,
		RestTemplate suchRestTemplate,
		OrtsSucheConfigurationProperties ortsSucheConfigurationProperties) {
		this.coordinateReferenceSystemConverter = coordinateReferenceSystemConverter;
		this.suchRestTemplate = suchRestTemplate;
		this.url = String.format("%s/%s/%s",
			ortsSucheConfigurationProperties.getBaseUrl(),
			ortsSucheConfigurationProperties.getToken(),
			ortsSucheConfigurationProperties.getFile());
		this.anzahlSuchergebnisse = ortsSucheConfigurationProperties.getAnzahlSuchergebnisse();
		this.bBox = ortsSucheConfigurationProperties.getBBox();
	}

	@Override
	public List<OrtsSucheErgebnis> find(String suchString) {
		Pattern pattern = Pattern.compile("([+\\-!(){}\\[\\]^\"~*?:\\\\]|(&&)|(\\|\\|))");
		Matcher matcher = pattern.matcher(suchString);
		suchString = matcher.replaceAll("\\\\$1");

		List<OrtsSucheErgebnis> result = new ArrayList<>();
		try {
			// Using StringBuilder instead of UriBuilder to prevent redundant encoding leading to bad requests
			String urlWithParameters = url +
				"?count=" + anzahlSuchergebnisse +
				"&bbox=" + bBox +
				"&query=" + suchString +
				"&filter=-qualitaet:c";

			BkgSuchErgebnisDto suchErgebnisse = suchRestTemplate
				.getForObject(urlWithParameters, BkgSuchErgebnisDto.class);

			List<BkgOrtsSucheDto> bkgSuchErgebnisse = suchErgebnisse.getFeatures()
				.stream()
				.sorted(
					Comparator.comparing(ergebnis -> ergebnis.getProperties().getScore(), Comparator.reverseOrder()))
				.collect(Collectors.toList());

			result = bkgSuchErgebnisse
				.stream()
				.map(this::convertBkgOrtsSucheToOrtsSucheErgebnis)
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("BKG Suche konnte nicht durchgeführt werden: {}", e.getMessage());
		}
		return result;
	}

	private OrtsSucheErgebnis convertBkgOrtsSucheToOrtsSucheErgebnis(BkgOrtsSucheDto bkgDto) {
		Coordinate srcCoordinate = bkgDto.getGeometry().getCoordinate();
		Coordinate switchedCoordinate = new Coordinate(srcCoordinate.getOrdinate(1),
			srcCoordinate.getOrdinate(0));
		Coordinate transformedCoordinate = this.coordinateReferenceSystemConverter.transformCoordinateUnsafe(
			switchedCoordinate,
			KoordinatenReferenzSystem.WGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);

		Polygon bbox = bkgDto.getProperties().getBbox();

		double[] extent = null;

		if (bbox != null) {
			Arrays.stream(bbox.getCoordinates()).forEach(coor -> {
				double ordinate0 = coor.getOrdinate(0);
				coor.setOrdinate(0, coor.getOrdinate(1));
				coor.setOrdinate(1, ordinate0);
			});
			bbox.setSRID(KoordinatenReferenzSystem.WGS84.getSrid());

			Geometry bboxTransformed = this.coordinateReferenceSystemConverter.transformGeometry(
				bbox,
				KoordinatenReferenzSystem.ETRS89_UTM32_N);

			Envelope bboxTransformedEnvelope = bboxTransformed.getEnvelopeInternal();
			extent = new double[] {
				bboxTransformedEnvelope.getMinX(),
				bboxTransformedEnvelope.getMinY(),
				bboxTransformedEnvelope.getMaxX(),
				bboxTransformedEnvelope.getMaxY()
			};
		}

		OrtsSucheErgebnis dto = new OrtsSucheErgebnis();
		double[] coordinates = new double[] { transformedCoordinate.x, transformedCoordinate.y };
		dto.setCenterCoordinate(coordinates);
		dto.setName(bkgDto.getProperties().getText());
		dto.setExtent(extent);

		return dto;
	}

}
