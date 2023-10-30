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

package de.wps.radvis.backend.weitereKartenebenen.schnittstelle.repositoryImpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.DateiLayerImportException;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenConfigurationProperties;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.DatastoreFeatureTypesResponseDto;
import de.wps.radvis.backend.weitereKartenebenen.domain.exception.SldValidationException;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.GeoserverRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.DateiLayerFormat;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverDatastoreName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverLayerName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverStyleName;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class GeoserverRepositoryImpl implements GeoserverRepository {

	private final WeitereKartenebenenConfigurationProperties weitereKartenebenenConfigurationProperties;

	private final GeoJsonImportRepository geoJsonImportRepository;

	@Override
	public GeoserverLayerName createDataStoreAndLayer(GeoserverDatastoreName datastoreName,
		DateiLayerFormat dateiLayerFormat, MultipartFile file)
		throws IOException, InterruptedException {

		String datastoreEndpoint;
		byte[] fileBytes;
		String contentType;

		switch (dateiLayerFormat) {
		case SHAPE -> {
			datastoreEndpoint = "/file.shp";
			contentType = "application/zip";
			fileBytes = file.getBytes();
		}
		case GEOJSON -> {
			fileBytes = geoJsonToGeoPackage(file);
			datastoreEndpoint = "/file.gpkg";
			contentType = "application/x-gpkg";
		}
		case GEOPACKAGE -> {
			datastoreEndpoint = "/file.gpkg";
			contentType = "application/x-gpkg";
			fileBytes = file.getBytes();
		}
		default -> throw new DateiLayerImportException("Dateiformat '" + dateiLayerFormat + "' wird nicht unterstützt");
		}

		HttpRequest request = getAuthenticatedRequestBuilderForDatastore(datastoreName, datastoreEndpoint)
			.headers("Content-Type", contentType)
			.PUT(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
			.build();

		log.info("Erstelle Datastore '{}' und entsprechenden Layer am Datei-Layer Geoserver", datastoreName);
		log.info("PUT {}", request.uri());
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		// Status-Code 201 heißt "Created". Andere Status (auch 2xx-er) bedeuten, dass
		// irgendwas geklappt hat, was
		// wir nicht unterstützen/erwarten.
		if (response.statusCode() != 201) {
			String message = String.format(
				"Erstellen vom Datastore %s' mit Status-Code %d fehlgeschlagen",
				datastoreName,
				response.statusCode());
			log.error("{}: {}", message, response.body());
			throw new RuntimeException(message);
		}

		return getLayerNameFromDatastore(datastoreName);
	}

	private byte[] geoJsonToGeoPackage(MultipartFile file) {
		try {
			List<SimpleFeature> features = geoJsonImportRepository
				.readFeaturesFromGeojsonFile(file)
				.toList();

			SimpleFeatureType featureType = features.get(0).getFeatureType();
			ListFeatureCollection collection = new ListFeatureCollection(featureType);
			collection.addAll(features);

			File exportGeoPkgFile = null;
			try {
				exportGeoPkgFile = Files.createTempFile("export", "gpkg").toFile();
				exportGeoPkgFile.deleteOnExit();
			} catch (IOException e) {
				throw new RuntimeException("Es konnte keine temporäre Datei erstellt werden", e);
			}
			try (GeoPackage geopkg = new GeoPackage(exportGeoPkgFile)) {
				geopkg.init();
				FeatureEntry entry = new FeatureEntry();
				geopkg.add(entry, collection);
				geopkg.createSpatialIndex(entry);
				return FileUtils.readFileToByteArray(exportGeoPkgFile);
			} catch (IOException e) {
				throw new RuntimeException("Fehler beim Erstellen der Geopackage Datei", e);
			} finally {
				exportGeoPkgFile.delete();
			}
		} catch (ReadGeoJSONException e) {
			throw new RuntimeException("GeoJSON konnte nicht eingelesen werden", e);
		}
	}

	/**
	 * Ermittelt den ersten Layer des angegebenen Datastores. Theoretisch kann es
	 * mehrere pro store geben, wir erstellen
	 * aber maximal einen. Weitere Layer werden daher ignoriert.
	 */
	@Override
	public GeoserverLayerName getLayerNameFromDatastore(GeoserverDatastoreName datastoreName)
		throws IOException, InterruptedException {

		HttpRequest request = getAuthenticatedRequestBuilderForDatastore(datastoreName, "/featuretypes")
			.headers("Accept", "application/json")
			.GET()
			.build();

		log.info("Hole Layer-Namen vom Datastore '{}' am Datei-Layer Geoserver", datastoreName);
		log.debug("GET {}", request.uri());
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			String message = String.format(
				"Holen der Layer-Namen am Geoserver für Datastore '%s' mit Status-Code %d fehlgeschlagen",
				datastoreName,
				response.statusCode());
			log.error("{}: {}", message, response.body());
			throw new RuntimeException(message);
		}

		DatastoreFeatureTypesResponseDto responseDto = new ObjectMapper().readValue(
			response.body(), DatastoreFeatureTypesResponseDto.class);

		if (responseDto.getFeatureTypes() == null ||
			responseDto.getFeatureTypes().getFeatureType() == null ||
			responseDto.getFeatureTypes().getFeatureType().isEmpty()) {
			throw new RuntimeException("Datastore" + datastoreName + " hat keine Layer.");
		}

		return GeoserverLayerName.of(responseDto.getFeatureTypes().getFeatureType().get(0).getName());
	}

	/**
	 * Löscht den angegebenen Datastore und rekursiv auch alles, was da dran hängt
	 * (also z.B. alle Layer, den denen
	 * es aber nur einen geben sollte).
	 */
	@Override
	public void removeDatastoreAndLayer(GeoserverDatastoreName datastoreName)
		throws IOException, InterruptedException {

		HttpRequest request = getAuthenticatedRequestBuilderForDatastore(datastoreName, "?recurse=true")
			.DELETE()
			.build();

		log.info("Entferne Datastore '{}' am Datei-Layer Geoserver", datastoreName);
		log.debug("DELETE {}", request.uri());
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			log.error(
				"HTTP-Call zum Entfernen vom Datastore '{}' am Datei-Layer Geoserver mit Status-Code {} fehlgeschlagen. Der Datei-Layer wird dennoch aus der Datenbank entfernt.\nHTTP-Response:\n{}",
				datastoreName,
				response.statusCode(),
				response.body());
			// Keine Exception, da der Nutzer da nichts machen kann. Stattdessen loggen wir
			// diesen Fehler, sodass
			// manuell die Daten aus dem Geoserver entfernt werden können. Da der Layer aus
			// der DB entfernt wird,
			// ist er für den Nutzer in RadVIS nicht mehr sichtbar, sodass die Operation aus
			// Nutzer-Sicht geklappt
			// hat.
		}
	}

	@Override
	public void addStyleToLayer(GeoserverLayerName geoserverLayerName, GeoserverStyleName geoserverStyleName,
		boolean makeDefault)
		throws IOException, InterruptedException {

		URI dateiLayerGeoserverUrl = URI.create(
			String.format(
				"%s/geoserver/rest/layers/%s/styles?default=%b",
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerHost(),
				geoserverLayerName,
				makeDefault));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(dateiLayerGeoserverUrl)
			.headers("Authorization", "Basic " + getAuthenticationEncoding())
			.headers("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(
				"{ style:"
					+ "{\n"
					+ "  \"name\": \"" + geoserverStyleName.toString() + "\",\n"
					+ "  \"filename\": \"" + geoserverStyleName + ".sld\"\n"
					+ "}}"))
			.build();

		log.info("Setze Style {} als Default an Layer {}", geoserverStyleName, geoserverLayerName);
		log.info("POST {}", request.uri());

		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 201) {
			log.error(
				"HTTP-Call zum Setzen des Default-Styles an Layer '{}' am Datei-Layer Geoserver mit Status-Code {} fehlgeschlagen. \nHTTP-Response:\n{}",
				geoserverLayerName,
				response.statusCode(),
				response.body());
			return;
		}

		log.info(response.body());

	}

	@Override
	public GeoserverStyleName createStyle(GeoserverStyleName geoserverStyleName, MultipartFile sldFile)
		throws IOException, InterruptedException {
		URI dateiLayerGeoserverUrl = URI.create(
			String.format(
				"%s/geoserver/rest/styles?name=%s",
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerHost(),
				geoserverStyleName
			));

		byte[] byteArray = new String(sldFile.getBytes()).replace("SvgParameter", "CssParameter").getBytes();

		HttpRequest request = HttpRequest.newBuilder()
			.uri(dateiLayerGeoserverUrl)
			.headers("Authorization", "Basic " + getAuthenticationEncoding())
			.headers("Content-Type", "application/vnd.ogc.sld+xml")
			.POST(HttpRequest.BodyPublishers.ofByteArray(byteArray))
			.build();

		log.info("POST {}", request.uri());

		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 201) {
			log.error(
				"HTTP-Call zum Upload des Styles mit Status-Code {} fehlgeschlagen.\nHTTP-Response:\n{}",
				response.statusCode(),
				response.body());
			throw new SldValidationException("Technischer Grund:\n" + response.body());
		}

		return GeoserverStyleName.of(response.body());
	}

	@Override
	public void deleteStyle(GeoserverStyleName geoserverStyleName) throws IOException, InterruptedException {
		URI dateiLayerGeoserverUrl = URI.create(
			String.format(
				"%s/geoserver/rest/styles/%s?recurse=true&purge=true",
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerHost(),
				geoserverStyleName
			));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(dateiLayerGeoserverUrl)
			.headers("Authorization", "Basic " + getAuthenticationEncoding())
			.DELETE()
			.build();

		log.info("DELETE {}", request.uri());

		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			log.error(
				"HTTP-Call zum Löschen des Styles mit Status-Code {} fehlgeschlagen.\nHTTP-Response:\n{}",
				response.statusCode(),
				response.body());
			// Keine Exception, da der Nutzer da nichts machen kann. Stattdessen loggen wir
			// diesen Fehler, sodass
			// manuell die Daten aus dem Geoserver entfernt werden können. Da der Layer aus
			// der DB entfernt wird,
			// ist er für den Nutzer in RadVIS nicht mehr sichtbar, sodass die Operation aus
			// Nutzer-Sicht geklappt
			// hat.
		}
	}

	@Override
	public void validateStyleForLayer(GeoserverLayerName geoserverLayerName,
		GeoserverStyleName geoserverStyleName) throws IOException, InterruptedException {

		URI dateiLayerGeoserverWMSUrl = URI.create(
			String.format(
				"%s/geoserver/%s/wms?LAYERS=datei-layer:%s&TILED=true&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&projection=EPSG:25832&WIDTH=256&HEIGHT=256&CRS=EPSG:25832&STYLES=%s&BBOX=469103.37588850036,5316504.926736319,547287.2718699168,5394688.822717736",
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerHost(),
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerWorkspace(),
				geoserverLayerName,
				geoserverStyleName
			));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(dateiLayerGeoserverWMSUrl)
			.headers("Authorization", "Basic " + getAuthenticationEncoding())
			.GET()
			.build();

		log.info("GET {}", request.uri());

		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new SldValidationException("WMS konnte mit dem Style nicht abgerufen werden. " + response.body());
		}

		Optional<String> contentType = response.headers().firstValue("Content-Type");
		if (contentType.isPresent() && contentType.get().startsWith("text/xml")) {
			String serviceException;
			try {
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = null;
				doc = builder.parse(new ByteArrayInputStream(response.body().getBytes()));
				doc.getDocumentElement().normalize();
				serviceException = doc.getElementsByTagName("ServiceException").item(0).getTextContent();

			} catch (SAXException | ParserConfigurationException | IndexOutOfBoundsException e) {
				throw new RuntimeException("Es ist ein unbekannter Fehler aufgetreten");
			}
			throw new SldValidationException(serviceException);
		}
	}

	private HttpRequest.Builder getAuthenticatedRequestBuilderForDatastore(GeoserverDatastoreName datastoreName,
		String datastoreEndpoint) {

		// Aufbau des Requests orientiert an folgendem cURL-Befehl:
		// curl -u admin:geoserver -X PUT --data-binary @../daten.gpkg
		// http://localhost:181/geoserver/rest/workspaces/<workspace>/datastores/<name>/file.gpkg
		URI dateiLayerGeoserverUrl = URI.create(
			String.format(
				"%s/geoserver/rest/workspaces/%s/datastores/%s%s",
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerHost(),
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerWorkspace(),
				datastoreName,
				datastoreEndpoint));

		return HttpRequest.newBuilder()
			.uri(dateiLayerGeoserverUrl)
			.headers("Authorization", "Basic " + getAuthenticationEncoding());
	}

	private String getAuthenticationEncoding() {
		String encoding = Base64.getEncoder().encodeToString(
			String.format(
				"%s:%s",
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerUsername(),
				weitereKartenebenenConfigurationProperties.getGeoserverDateiLayerPassword()).getBytes());
		return encoding;
	}
}
