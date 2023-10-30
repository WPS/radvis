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

package de.wps.radvis.backend.quellimport.grundnetz.domain;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DLMWFSImportRepository {

	private final CommonConfigurationProperties commonConfigurationProperties;
	private final DLMConfigurationProperties dlmConfigurationProperties;

	public DLMWFSImportRepository(
		CommonConfigurationProperties commonConfigurationProperties,
		DLMConfigurationProperties dlmConfigurationProperties) {
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.dlmConfigurationProperties = dlmConfigurationProperties;
	}

	public List<Envelope> getPartitionen() {
		double minX = this.dlmConfigurationProperties.getExtentProperty().getMinX();
		double maxX = this.dlmConfigurationProperties.getExtentProperty().getMaxX();
		double minY = this.dlmConfigurationProperties.getExtentProperty().getMinY();
		double maxY = this.dlmConfigurationProperties.getExtentProperty().getMaxY();

		int numberOfPartitionsX = this.dlmConfigurationProperties.getPartitionenX();
		double partitionWidth = (maxX - minX) / numberOfPartitionsX;
		List<Envelope> partitions = new ArrayList<>();

		for (int x = 0; x < numberOfPartitionsX; x++) {
			partitions.add(new Envelope(minX + (x * partitionWidth), minX + ((x + 1) * partitionWidth),
				minY, maxY));
		}

		return partitions;
	}

	public Stream<ImportedFeature> readStrassenFeatures(Envelope extent) {
		String typeNames = "nora:v_at_strasse";
		String propertyNames = "gml_id,geom,eigenname,bezeichnung";

		return readFeatures(extent, typeNames, propertyNames);
	}

	public Stream<ImportedFeature> readWegeFeatures(Envelope extent) {
		String typeNames = "nora:v_at_weg";
		String propertyNames = "gml_id,geom,eigenname";

		return readFeatures(extent, typeNames, propertyNames);
	}

	@NotNull
	private Stream<ImportedFeature> readFeatures(Envelope extent, String typeNames, String propertyNames) {
		Double minX = extent.getMinX();
		Double maxX = extent.getMaxX();
		Double minY = extent.getMinY();
		Double maxY = extent.getMaxY();

		Polygon envelopePolygon = EnvelopeAdapter.toPolygon(extent, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		try {
			Stream<ImportedFeature> features = readURL(
				String.format(
					"%s?user=%s&password=%s&version=2.0.0&service=WFS&request=GetFeature&typeName=%s&bbox=%s,%s,%s,%s,EPSG:%d&propertyName=%s",
					dlmConfigurationProperties.getBasisUrl(),
					dlmConfigurationProperties.getUsername(),
					dlmConfigurationProperties.getPassword(),
					typeNames,
					minX, minY, maxX, maxY, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
					propertyNames
				));
			if (FeatureTogglz.DLM_REIMPORT_FIX.isActive()) {
				log.info("{} ist aktiviert. Es werden die Features nach envelope intersection gefiltert.",
					FeatureTogglz.DLM_REIMPORT_FIX.name());
				return features.filter(feature -> envelopePolygon.intersects(feature.getGeometrie()));
			} else {
				log.info("{} ist deaktiviert. Es werden die Features NICHT nach envelope intersection gefiltert.",
					FeatureTogglz.DLM_REIMPORT_FIX.name());
				return features;
			}
		} catch (IOException | XMLStreamException e) {
			log.error("Basis-DLM Features konnten nicht aus WFS gelesen werden", e);
			throw new RuntimeException(e);
		}
	}

	private Stream<ImportedFeature> readURL(String urlString) throws IOException, XMLStreamException {
		URL url = new URL(urlString);
		URLConnection urlConnection;

		if (commonConfigurationProperties.getProxyAdress() != null) {
			urlConnection = url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
				commonConfigurationProperties.getProxyAdress(),
				commonConfigurationProperties.getProxyPort())));
		} else {
			urlConnection = url.openConnection();
		}

		urlConnection.setReadTimeout(0);
		urlConnection.setConnectTimeout(0);
		InputStream inputStream = urlConnection.getInputStream();

		return readStream(inputStream);
	}

	private Stream<ImportedFeature> readStream(InputStream inputStream) throws XMLStreamException {
		XMLEventReader reader = XMLInputFactory.newInstance()
			.createXMLEventReader(inputStream);
		DLMImportedFeatureXMLIterator iterator = new DLMImportedFeatureXMLIterator(reader);
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}
}
