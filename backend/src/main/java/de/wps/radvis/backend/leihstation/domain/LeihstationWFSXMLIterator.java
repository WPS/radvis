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

import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.ExterneLeihstationenId;
import de.wps.radvis.backend.leihstation.domain.entity.LeihstationMobidataWFSElement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LeihstationWFSXMLIterator implements Iterator<LeihstationMobidataWFSElement> {

	private static final String BIKESTATION_XML_ELEMENT = "bikesharingStations";
	private static final String BIKESTATION_POSITION_XML_ELEMENT = "pos";
	private static final String BIKESTATION_COUNT_XML_ELEMENT = "COUNT";
	private static final String BIKESTATION_ID_XML_ELEMENT = "ID";

	private XMLEventReader reader;

	public LeihstationWFSXMLIterator(XMLEventReader reader) {
		this.reader = reader;
		try {
			forwardReaderToNextBikeStationOrDocumentEnd();
		} catch (XMLStreamException e) {
			log.error("Fehler beim Einlesen der MobiDataBW Leihstationen");
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() {
		try {
			return reader.hasNext() && (reader.peek().isStartDocument() || isBikeStationStartElement(reader.peek()));
		} catch (XMLStreamException e) {
			log.error("Fehler beim Einlesen der MobiDataBW Leihstationen");
			throw new RuntimeException(e);
		}
	}

	@Override
	public LeihstationMobidataWFSElement next() {
		try {
			LeihstationMobidataWFSElement dto = new LeihstationMobidataWFSElement();

			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();

				if (event.isStartElement()) {
					switch (event.asStartElement().getName().getLocalPart()) {
					case BIKESTATION_ID_XML_ELEMENT:
						dto.setId(ExterneLeihstationenId.of(createFullStringFromReaderEvents(reader.nextEvent())));
						break;
					case BIKESTATION_POSITION_XML_ELEMENT:
						dto.setPosition(buildPoint(createFullStringFromReaderEvents(reader.nextEvent())));
						break;
					case BIKESTATION_COUNT_XML_ELEMENT:
						dto.setAnzahlFahrraeder(buildCount(createFullStringFromReaderEvents(reader.nextEvent())));
						break;
					}
				}

				if (isBikeStationEndElement(event)) {
					forwardReaderToNextBikeStationOrDocumentEnd();
					return dto;
				}
			}
		} catch (XMLStreamException e) {
			log.error("Fehler beim Einlesen der MobiDataBW Leihstationen");
			throw new RuntimeException(e);
		}
		throw new IllegalStateException(
			"Beim Einlesen der MobiDataBW Leihstationen wurde ein ungültiger Zustand erreicht");
	}

	private void forwardReaderToNextBikeStationOrDocumentEnd() throws XMLStreamException {
		while (reader.hasNext() && !(isBikeStationStartElement(reader.peek()) || reader.peek().isEndDocument())) {
			reader.nextEvent();
		}
	}

	private boolean isBikeStationStartElement(XMLEvent event) {
		return event.isStartElement() && event.asStartElement().getName().getLocalPart()
			.equals(BIKESTATION_XML_ELEMENT);
	}

	private boolean isBikeStationEndElement(XMLEvent event) {
		return event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(BIKESTATION_XML_ELEMENT);
	}

	private String createFullStringFromReaderEvents(XMLEvent event) throws XMLStreamException {
		StringBuilder fullString = new StringBuilder();
		while (!event.isEndElement()) {
			String partialString = event.asCharacters().getData();
			if (!partialString.trim().isEmpty()) {
				fullString.append(partialString);
			}
			event = reader.nextEvent();
		}

		return fullString.toString().trim();
	}

	private Point buildPoint(String posAsString) {
		String[] split = posAsString.split(" ");
		return KoordinatenReferenzSystem.WGS84
			.getGeometryFactory()
			.createPoint(new Coordinate(Double.valueOf(split[0]), Double.valueOf(split[1])));
	}

	private int buildCount(String countAsString) {
		try {
			return Integer.parseInt(countAsString);
		} catch (NumberFormatException e) {
			log.error("Illegal Symbol in 'COUNT' vom MobiDataBW Leihstation WFS: " + countAsString);
		}
		return 0;
	}
}
