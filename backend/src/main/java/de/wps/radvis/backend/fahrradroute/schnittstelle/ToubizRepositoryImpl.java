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

package de.wps.radvis.backend.fahrradroute.schnittstelle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.fahrradroute.domain.entity.ImportedToubizRoute;
import de.wps.radvis.backend.fahrradroute.domain.repository.ToubizRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToubizRepositoryImpl implements ToubizRepository {
	private static final String LANDESRADFERNWEG = "landesradfernweg";

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	private final String urlPage1;

	private final ToubizConfigurationProperties toubizConfigurationProperties;

	private final RestTemplate restTemplate;

	private final GeometryFactory geometryFactory;

	public ToubizRepositoryImpl(
		CoordinateReferenceSystemConverter coordinateReferenceSystemConverter,
		RestTemplate restTemplate,
		ToubizConfigurationProperties toubizConfigurationProperties) {
		this.coordinateReferenceSystemConverter = coordinateReferenceSystemConverter;
		this.restTemplate = restTemplate;
		this.geometryFactory = new GeometryFactory(new PrecisionModel(), KoordinatenReferenzSystem.WGS84.getSrid());
		this.toubizConfigurationProperties = toubizConfigurationProperties;

		urlPage1 = String.format(
			"%s?language=de&unlicensed=1&api_token=%s&filter[category]=%s&include[]=articleTypeAttributes&include[]=contactInformation",
			toubizConfigurationProperties.getBaseUrl(), toubizConfigurationProperties.getToken(),
			toubizConfigurationProperties.getFilterCategory());
	}

	private static Coordinate mapPointsToCoordinates(List<Float> floats) {
		// discard third value, because it is not needed
		return new Coordinate(floats.get(0), floats.get(1));
	}

	@Override
	public List<ImportedToubizRoute> importRouten() {
		List<ImportedToubizRoute> fahrradrouten = new ArrayList<>();
		Optional<String> url = Optional.of(urlPage1);
		while (url.isPresent()) {
			logApiCall(url.get());

			ToubizFahrradrouteErgebnisDto ergebnis = restTemplate.getForObject(url.get(),
				ToubizFahrradrouteErgebnisDto.class);
			if (ergebnis != null) {
				fahrradrouten.addAll(ergebnis.getArticles().stream()
					.map(this::mapDtoToToubizRoute)
					.filter(Objects::nonNull)
					.collect(Collectors.toList()));

				url = ergebnis.getNextPageWithoutURLEncoding();
			} else {
				break;
			}
		}

		return fahrradrouten;
	}

	private void logApiCall(String url) {
		log.info("REST GET Toubiz: {}",
			UriComponentsBuilder.fromHttpUrl(url).replaceQueryParam("api_token", "***").build());
	}

	private boolean isLandesradfernweg(ToubizFahrradroutePayloadDto toubizFahrradroutePayloadDto) {
		String url = this.getTagsUrlForToubizRoute(toubizFahrradroutePayloadDto);
		logApiCall(url);

		try {
			ToubizFahrradrouteTagsErgebnisDto ergebnis = restTemplate.getForObject(url,
				ToubizFahrradrouteTagsErgebnisDto.class);
			if (ergebnis == null) {
				log.warn("Das Ergebnis des restTemplate.getForObject für isLandesradfernweg ist null");
				log.warn("-> Die Route wird als nicht-LRFW importiert");
				return false;
			}
			return ergebnis.getTags().contains(LANDESRADFERNWEG);
		} catch (RestClientException e) {
			log.error(
				"RestClientException: Das Abrufen der Tags ist fehlgeschlagen. Es konnte somit nicht festgestellt werden, ob es sich um einen LRFW handelt");
			log.error("-> Die Route wird als nicht-LRFW importiert");
			log.error(e.getMessage());
			return false;
		}
	}

	private String getTagsUrlForToubizRoute(ToubizFahrradroutePayloadDto toubizFahrradroutePayloadDto) {

		return String.format("%s/%s/tags?language=de&unlicensed=1&api_token=%s",
			toubizConfigurationProperties.getBaseUrl(),
			toubizFahrradroutePayloadDto.getId(),
			toubizConfigurationProperties.getToken());
	}

	private ImportedToubizRoute mapDtoToToubizRoute(ToubizFahrradroutePayloadDto article) {

		Geometry originalGeometrie = null;

		List<List<Float>> points = article.getPoints();
		// Wenn eine Fahrradroute keine Punkte hat -> ohne Geometrie importieren
		if (!points.isEmpty()) {
			AtomicReference<Coordinate> previous = new AtomicReference<>(null);
			LineString lineString = this.geometryFactory.createLineString(
				points.stream()
					.map(ToubizRepositoryImpl::mapPointsToCoordinates)
					// wir filtern direkt nacheinander auftauchende Dopplungen heraus
					.filter(coordinate -> !coordinate.equals(previous.getAndSet(coordinate)))
					.toArray(Coordinate[]::new));

			lineString.setSRID(KoordinatenReferenzSystem.WGS84.getSrid());

			originalGeometrie = this.coordinateReferenceSystemConverter.transformGeometry(
				lineString,
				KoordinatenReferenzSystem.ETRS89_UTM32_N);
		}
		Tourenkategorie tourenkategorie = null;
		try {
			tourenkategorie = Tourenkategorie.fromDisplaytext(article.getPrimaryCategory().getName());
		} catch (RuntimeException e) {
			log.warn(e.getMessage());
		}

		// ist De-fakto immer null
		List<String> linksZuWeiterenMedien = new ArrayList<>();
		if (article.getWebMediaLinks() != null && article.getWebMediaLinks().getUrl() != null) {
			linksZuWeiterenMedien.add(article.getWebMediaLinks().getUrl());
		} else {
			log.warn("getWebMediaLinks konnte nicht eingelesen werden");
		}

		String _abstract = article.get_abstract();

		if (_abstract != null && _abstract.length() > 500) {
			log.warn("Kurzbeschreibung zu lang und wird abgeschnitten");
			_abstract = _abstract.substring(0, 499);
		}

		if (originalGeometrie == null) {
			log.info("Toubiz ID: {} konnte nicht importiert werden: Fehlende Geometrie",
				ToubizId.of(article.getId()));
			return null;
		}

		String homepage = getHompage(article);

		String email = getEmail(article);

		return ImportedToubizRoute.builder()
			.name(FahrradrouteName.of(article.getName()))
			.toubizId(ToubizId.of(article.getId()))
			.originalGeometrie(originalGeometrie)
			.kurzbezeichnung(_abstract)
			.beschreibung(article.getDescription())
			.info(article.getTour().getMoreInformation())
			.offizielleLaenge(Laenge.of(article.getTour().getTrackInformation().getDistance()))
			.tourenkategorie(tourenkategorie)
			.homepage(homepage)
			.emailAnsprechpartner(email)
			.lizenz(article.getLicense())
			.zuletztBearbeitet(article.getUpdatedAt())
			.linksZuWeiterenMedien(linksZuWeiterenMedien)
			.lizenzNamensnennung(article.getCopyright())
			.landesradfernweg(isLandesradfernweg(article))
			.build();
	}

	private String getHompage(ToubizFahrradroutePayloadDto payloadDto) {
		String url = this.getContactInformationUrlForToubizRoute(payloadDto);
		logApiCall(url);
		ToubizFahrradrouteContactInformationErgebnisDto ergebnis = null;
		try {
			ergebnis = restTemplate.getForObject(url,
				ToubizFahrradrouteContactInformationErgebnisDto.class);
		} catch (RestClientException e) {
			log.error(
				"RestClientException: Das Abrufen der Homepage ist fehlgeschlagen. Die Route wird ohne Homepage importiert");
			log.error(e.getMessage());
		}
		return ergebnis != null && ergebnis.getWebsite() != null ? ergebnis.getWebsite() : "";
	}

	private String getContactInformationUrlForToubizRoute(ToubizFahrradroutePayloadDto toubizFahrradroutePayloadDto) {

		return String.format("%s/%s/contactInformation?language=de&unlicensed=1&api_token=%s",
			toubizConfigurationProperties.getBaseUrl(),
			toubizFahrradroutePayloadDto.getId(),
			toubizConfigurationProperties.getToken());
	}

	private String getEmail(ToubizFahrradroutePayloadDto payloadDto) {
		String url = this.getEmailsUrlForToubizRoute(payloadDto);
		logApiCall(url);
		ToubizFahrradrouteEmailsErgebnisDto ergebnis = null;
		try {
			ergebnis = restTemplate.getForObject(url, ToubizFahrradrouteEmailsErgebnisDto.class);
		} catch (RestClientException e) {
			log.error(
				"RestClientException: Das Abrufen der Email ist fehlgeschlagen. Die Route wird ohne Mailadresse importiert");
			log.error(e.getMessage());
		}

		return ergebnis != null ? ergebnis.getFirstEmail() : null;
	}

	private String getEmailsUrlForToubizRoute(ToubizFahrradroutePayloadDto toubizFahrradroutePayloadDto) {

		return String.format("%s/%s/emails?language=de&unlicensed=1&api_token=%s",
			toubizConfigurationProperties.getBaseUrl(),
			toubizFahrradroutePayloadDto.getId(),
			toubizConfigurationProperties.getToken());
	}
}
