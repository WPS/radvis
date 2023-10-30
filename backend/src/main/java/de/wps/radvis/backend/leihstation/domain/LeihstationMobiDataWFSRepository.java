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

package de.wps.radvis.backend.leihstation.domain;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.leihstation.domain.entity.LeihstationMobidataWFSElement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LeihstationMobiDataWFSRepository {

	private final CommonConfigurationProperties commonConfigurationProperties;
	private final String leihstationMobiDataWFSUrl;

	public LeihstationMobiDataWFSRepository(
		CommonConfigurationProperties commonConfigurationProperties,
		String leihstationMobiDataWFSUrl) {
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.leihstationMobiDataWFSUrl = leihstationMobiDataWFSUrl;
	}

	public Stream<LeihstationMobidataWFSElement> readBikeStationFeatures() {
		try {
			URL url = new URL(leihstationMobiDataWFSUrl);
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
		} catch (IOException | XMLStreamException e) {
			log.error("Leihstation Features konnten nicht aus MobiDataBW WFS gelesen werden", e);
			throw new RuntimeException(e);
		}
	}

	private Stream<LeihstationMobidataWFSElement> readStream(InputStream inputStream) throws XMLStreamException {
		XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
		LeihstationWFSXMLIterator iterator = new LeihstationWFSXMLIterator(reader);
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}
}
