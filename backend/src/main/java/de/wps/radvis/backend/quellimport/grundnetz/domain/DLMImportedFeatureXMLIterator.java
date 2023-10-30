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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import de.wps.radvis.backend.quellimport.grundnetz.domain.valueObject.Strasse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DLMImportedFeatureXMLIterator implements Iterator<ImportedFeature> {
	private XMLEventReader reader;

	public DLMImportedFeatureXMLIterator(XMLEventReader reader) {
		this.reader = reader;
		try {
			forwardReaderToNextStrasseOrDocumentEnd();
		} catch (XMLStreamException e) {
			log.error("Fehler beim Einlesen der DLM Daten");
			throw new RuntimeException(e);
		}
	}

	public boolean hasNext() {
		try {
			return reader.hasNext() && (reader.peek().isStartDocument() || isStrassenStartElement(reader.peek()));
		} catch (XMLStreamException e) {
			log.error("Fehler beim Einlesen der DLM Daten");
			throw new RuntimeException(e);
		}
	}

	private boolean isStrassenStartElement(XMLEvent event) {
		return event.isStartElement() && (event.asStartElement().getName().getLocalPart().equals("v_at_strasse")
			|| event.asStartElement().getName().getLocalPart().equals("v_at_weg"));
	}

	public ImportedFeature next() {
		try {
			Strasse strasse = new Strasse();

			while (reader.hasNext()) {
				XMLEvent nextEvent;
				nextEvent = reader.nextEvent();

				if (nextEvent.isStartElement()) {
					StartElement startElement = nextEvent.asStartElement();
					switch (startElement.getName().getLocalPart()) {
					case "gml_id":
						strasse.setGmlId(createFullStringFromReaderEvents(reader));
						break;
					case "posList":
						strasse.setGeom(buildLineString(createFullStringFromReaderEvents(reader)));
						break;
					case "eigenname":
						strasse.setEigenname(createFullStringFromReaderEvents(reader));
						break;
					case "bezeichnung":
						strasse.setBezeichnung(createFullStringFromReaderEvents(reader));
						break;
					}
				}

				if (isStrassenEndElement(nextEvent)) {
					forwardReaderToNextStrasseOrDocumentEnd();
					return buildImportedFeature(strasse);
				}
			}
			throw new IllegalStateException("Beim Einlesen der DLM-Daten wurde ein ungültiger Zustand erreicht");
		} catch (XMLStreamException e) {
			log.error("Fehler beim Einlesen der DLM Daten");
			throw new RuntimeException(e);
		}

	}

	private String createFullStringFromReaderEvents(XMLEventReader reader) throws XMLStreamException {
		XMLEvent nextEvent = reader.nextEvent();

		StringBuilder fullString = new StringBuilder();
		while (!nextEvent.isEndElement()) {
			String partialString = nextEvent.asCharacters().getData();
			if (!partialString.trim().isEmpty()) {
				fullString.append(partialString);
			}
			nextEvent = reader.nextEvent();
		}

		return fullString.toString().trim();
	}

	private LineString buildLineString(String posListAsString) {
		// beim Einlesen können durch den Zeilenumbruch mehrere Leerzeichen in der posList auftreten. Der Regex ersetzt
		// alle multiplen Leerzeichen durch ein Leerzeichen.
		String[] split = posListAsString.replaceAll("\\s{2,}", " ").split(" ");
		List<Coordinate> coordinates = new ArrayList<>();
		for (int i = 0; i + 1 < split.length; i = i + 2) {
			coordinates.add(new Coordinate(Double.valueOf(split[i]), Double.valueOf(split[i + 1])));
		}
		return KoordinatenReferenzSystem.ETRS89_UTM32_N
			.getGeometryFactory()
			.createLineString(coordinates.toArray(new Coordinate[0]));
	}

	private boolean isStrassenEndElement(XMLEvent nextEvent) {
		if (!nextEvent.isEndElement()) {
			return false;
		}

		String localPart = nextEvent.asEndElement().getName().getLocalPart();
		return localPart.equals("v_at_strasse") || localPart.equals("v_at_weg");
	}

	private void forwardReaderToNextStrasseOrDocumentEnd() throws XMLStreamException {
		while (reader.hasNext() && !(isStrassenStartElement(reader.peek()) || reader.peek().isEndDocument())) {
			reader.nextEvent();
		}
	}

	private ImportedFeature buildImportedFeature(Strasse strasse) {
		if (strasse.getGmlId() == null) {
			throw new IllegalStateException(
				"DLM-Feature konnte nicht gelesen werden. Es konnte keine gml_id gefunden werden.");
		} else if (strasse.getGeom() == null) {
			throw new IllegalStateException(
				"DLM-Feature konnte nicht gelesen werden. Es konnte keine Geometrie gefunden werden.");
		}

		Map<String, Object> attribute = new HashMap<>();

		if (strasse.getEigenname() != null) {
			attribute.put("eigenname", strasse.getEigenname());
		}

		if (strasse.getBezeichnung() != null) {
			attribute.put("bezeichnung", strasse.getBezeichnung());
		}

		return new ImportedFeature(strasse.getGmlId(), strasse.getGeom(), attribute,
			LocalDateTime.now(),
			QuellSystem.DLM, Art.Strecke);
	}
}
