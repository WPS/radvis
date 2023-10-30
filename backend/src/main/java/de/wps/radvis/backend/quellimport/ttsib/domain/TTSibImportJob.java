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

package de.wps.radvis.backend.quellimport.ttsib.domain;

import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.common.domain.FileBasedInputSummarySupplier;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.Attribute;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.quellimport.ttsib.domain.entity.TtSibAbschnittOderAst;
import de.wps.radvis.backend.quellimport.ttsib.domain.entity.TtSibFahrradweg;
import de.wps.radvis.backend.quellimport.ttsib.domain.entity.TtSibQuerschnitt;
import de.wps.radvis.backend.quellimport.ttsib.domain.entity.TtSibStreifen;
import de.wps.radvis.backend.quellimport.ttsib.domain.entity.TtSibTeilabschnitt;
import de.wps.radvis.backend.quellimport.ttsib.domain.generated.CoordinatesType;
import de.wps.radvis.backend.quellimport.ttsib.domain.generated.DotquerType;
import de.wps.radvis.backend.quellimport.ttsib.domain.generated.GeometryPropertyType;
import de.wps.radvis.backend.quellimport.ttsib.domain.generated.LineStringType;
import de.wps.radvis.backend.quellimport.ttsib.domain.generated.ObjektrefType;
import de.wps.radvis.backend.quellimport.ttsib.domain.generated.VISTRASSENNETZType;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibEinordnung;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibQuerschnittArt;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TTSibImportJob extends AbstractJob {

	private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

	XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

	TtSibRepository ttSibRepository;

	private final TtSibFahrradwegRepository ttSibFahrradwegRepository;

	private final HashMap<String, File> ttSibXmlFiles;

	private List<VISTRASSENNETZType> viStrassennetzList;

	private final GeometryFactory etrs89Utm32NGeometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N
		.getGeometryFactory();

	// Wird benötigt um bestehende gespeicherte Objekte wieder zu finden,
	// falls Einträge in der XML nicht nacheinander gehen.
	private final Map<String, Long> abschnittOderAstIndex = new HashMap<>();

	// Hält ein AoA fest um bei jeden Streifen-Durchlauf den wiederzufinden und die Relation korrekt zu befüllen.
	// Das ist effektiv, weil die Reihenfolge in der XML Datei sortiert ist.
	private TtSibAbschnittOderAst bufferedAoA;

	private final EntityManager entityManager;

	public TTSibImportJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		TtSibRepository ttSibRepository,
		TtSibFahrradwegRepository ttSibFahrradwegRepository,
		File ttSibFolder,
		EntityManager entityManager) {
		super(jobExecutionDescriptionRepository);

		require(ttSibFolder.exists(), "TT-SIB Quellordner existiert nicht: " + ttSibFolder.getAbsolutePath());

		ttSibXmlFiles = getTtSibXmlFiles(ttSibFolder);

		this.ttSibRepository = ttSibRepository;
		this.ttSibFahrradwegRepository = ttSibFahrradwegRepository;
		this.entityManager = entityManager;

		setInputSummarySupplier(FileBasedInputSummarySupplier.of(ttSibFolder));
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		log.info("Lösche alte TT-SIB-Daten");
		// Fuer ein deleteAll sind die Tables zu groß -> Dauert ewig und führt evtl. zu Überlauf des UNDO-Space auf der DB
		this.ttSibRepository.truncateCascadeAllAoAs();
		this.ttSibFahrradwegRepository.truncateCascadeAllFahrradwege();

		log.info("TT-Sib-Daten werden importiert.");

		viStrassennetzList = parseViStrassennetz();
		log.info("{} Elemente aus ViStrassennetz gelesen. Parse Dotquer", viStrassennetzList.size());

		parseDotquer();

		log.info("TT-Sib-Daten importiert");

		this.calculateTtSibFahrradwege();

		return Optional.empty();
	}

	private List<VISTRASSENNETZType> parseViStrassennetz() {
		XMLEventReader reader = createReader("VI_STRASSENNETZ.xml");

		List<VISTRASSENNETZType> result = new ArrayList<>();

		try {
			VISTRASSENNETZType entry = new VISTRASSENNETZType();

			// Einträge mit <PROJEKT>0</PROJEKT> müssen ignoriert werden
			boolean hasNoProjekt = false;

			while (reader.hasNext()) {
				XMLEvent nextEvent = reader.nextEvent();

				if (nextEvent.isStartElement()) {
					StartElement startElement = nextEvent.asStartElement();
					switch (startElement.getName().getLocalPart()) {
					case "PROJEKT":
						nextEvent = reader.nextEvent();
						String projekt = nextEvent.asCharacters().getData();
						if (projekt.equals("0")) {
							hasNoProjekt = true;
						}
						break;
					case "ABSCHNITT_ID":
						entry.setABSCHNITTID(this.createFullStringFromReaderEvents(reader));
						break;
					case "KLASSE":
						entry.setKLASSE(this.createFullStringFromReaderEvents(reader));
						break;
					case "NUMMER":
						entry.setNUMMER(
							BigInteger.valueOf(Integer.parseInt(this.createFullStringFromReaderEvents(reader))));
						break;
					case "VNP":
						entry.setVNP(this.createFullStringFromReaderEvents(reader));
						break;
					case "NNP":
						entry.setNNP(this.createFullStringFromReaderEvents(reader));
						break;
					case "BUCHSTABE":
						entry.setBUCHSTABE(this.createFullStringFromReaderEvents(reader));
						break;
					case "KREIS":
						entry.setKREIS(this.createFullStringFromReaderEvents(reader));
						break;
					case "AKZ":
						entry.setAKZ(this.createFullStringFromReaderEvents(reader));
						break;
					case "SBA":
						entry.setSBA(this.createFullStringFromReaderEvents(reader));
						break;
					case "VNK_NAME":
						entry.setVNKNAME(this.createFullStringFromReaderEvents(reader));
						break;
					case "NNK_NAME":
						entry.setNNKNAME(this.createFullStringFromReaderEvents(reader));
						break;
					case "VNP_ART":
						entry.setVNPART(this.createFullStringFromReaderEvents(reader));
						break;
					case "NNP_ART":
						entry.setNNPART(this.createFullStringFromReaderEvents(reader));
						break;
					case "LEN":
						entry.setLEN(new BigInteger(this.createFullStringFromReaderEvents(reader)));
						break;
					case "STRASSENBEZEICHNUNG":
						entry.setSTRASSENBEZEICHNUNG(this.createFullStringFromReaderEvents(reader));
						break;
					case "SBA_NAME":
						entry.setSBANAME(this.createFullStringFromReaderEvents(reader));
						break;
					case "SM_NAME":
						entry.setSMNAME(this.createFullStringFromReaderEvents(reader));
						break;
					case "coordinates":
						GeometryPropertyType geometryPropertyType = new GeometryPropertyType();

						String coordinates = this.createFullStringFromReaderEvents(reader);

						CoordinatesType coordinatesType = new CoordinatesType();
						coordinatesType.setValue(coordinates);

						LineStringType lineStringType = new LineStringType();
						lineStringType.setCoordinates(coordinatesType);

						JAXBElement<LineStringType> lineStringTypeJAXBElement = new JAXBElement<>(
							new QName("coordinates"),
							LineStringType.class, lineStringType);

						geometryPropertyType.setGeometry(lineStringTypeJAXBElement);

						entry.setGEOMETRY(geometryPropertyType);

						break;
					case "STAND":
						nextEvent = reader.nextEvent();
						entry.setSTAND(
							DatatypeFactory.newInstance().newXMLGregorianCalendar(
								GregorianCalendar.from(
									LocalDateTime.parse(
										nextEvent.asCharacters().getData(),
										DateTimeFormatter.ISO_DATE_TIME).atZone(ZONE_ID_BERLIN))));
						break;
					}
				}

				if (nextEvent.isEndElement() && nextEvent.asEndElement().getName().getLocalPart()
					.equals("VI_STRASSENNETZ")) {
					if (hasNoProjekt) {
						hasNoProjekt = false;
						result.add(entry);
					}
					entry = new VISTRASSENNETZType();
				}
			}
		} catch (XMLStreamException | DatatypeConfigurationException e) {
			log.error("Fehler beim Einlesen der TT-SIB-Daten VI_STRASSENNETZ.xml");
			throw new RuntimeException(e);
		}

		return result;
	}

	private void parseDotquer() {
		XMLEventReader reader = createReader("Dotquer.xml");

		try {
			DotquerType entry = new DotquerType();

			// Es gibt Einträge, die sich nur im Wert "luk" im Tag "projekt" unterscheiden.
			// Alle Einträge mit luk != 0 müssen verworfen werden.
			boolean projektIsValid = false;

			int zaehler = 1;
			while (reader.hasNext()) {
				XMLEvent nextEvent = reader.nextEvent();

				if (nextEvent.isStartElement()) {
					StartElement startElement = nextEvent.asStartElement();
					switch (startElement.getName().getLocalPart()) {
					case "projekt":
						nextEvent = reader.nextEvent();
						String luk = startElement.getAttributeByName(new QName("luk")).getValue();
						if (luk.equals("0")) {
							projektIsValid = true;
						}
						break;
					case "vst":
						entry.setVst(
							BigInteger.valueOf(Integer.parseInt(this.createFullStringFromReaderEvents(reader))));
						break;
					case "bst":
						entry.setBst(
							BigInteger.valueOf(Integer.parseInt(this.createFullStringFromReaderEvents(reader))));
						break;
					case "streifen":
						entry.setStreifen(this.createFullStringFromReaderEvents(reader));
						break;
					case "streifennr":
						entry.setStreifennr(
							BigInteger.valueOf(Integer.parseInt(this.createFullStringFromReaderEvents(reader))));
						break;
					case "art":
						nextEvent = reader.nextEvent();
						ObjektrefType objektrefTypeArt = new ObjektrefType();
						objektrefTypeArt.setHref(
							startElement.getAttributeByName(new QName("http://www.w3.org/1999/xlink", "href"))
								.getValue());
						entry.setArt(objektrefTypeArt);
						break;
					case "breite":
						entry.setBreite(
							BigInteger.valueOf(Integer.parseInt(this.createFullStringFromReaderEvents(reader))));
						break;
					case "bisBreite":
						entry.setBisBreite(
							BigInteger.valueOf(Integer.parseInt(this.createFullStringFromReaderEvents(reader))));
						break;
					case "abschnittId":
						entry.setAbschnittId(this.createFullStringFromReaderEvents(reader));
						break;
					}
				}

				if (nextEvent.isEndElement()) {
					EndElement endElement = nextEvent.asEndElement();
					if (endElement.getName().getLocalPart().equals("Dotquer")) {
						if (projektIsValid) {
							projektIsValid = false;
							convertToAoaAndPersist(entry);
						} else {
							log.debug("Projekt hat Nummer {} != 0 und wird nicht importiert.", entry.getProjekt());
						}
						entry = new DotquerType();

						if (zaehler % 10000 == 0) {
							log.info("TT-SIB Streifen {} (Zähler) wird verarbeitet.", zaehler);
						}

						zaehler++;
					}
				}
			}
			// letzten Eintrag speichern
			ttSibRepository.save(bufferedAoA);
		} catch (XMLStreamException e) {
			log.error("Fehler beim Einlesen der TT-SIB-Daten Dotquer.xml");
			throw new RuntimeException(e);
		}
	}

	private String createFullStringFromReaderEvents(XMLEventReader reader) throws XMLStreamException {
		XMLEvent nextEvent = reader.nextEvent();

		StringBuilder fullString = new StringBuilder();
		while (!nextEvent.isEndElement()) {
			String partialString = nextEvent.asCharacters().getData();
			if (!partialString.trim().isEmpty()) {
				fullString.append(" ");
				fullString.append(partialString);
			}
			nextEvent = reader.nextEvent();
		}

		return fullString.toString().trim();
	}

	private void convertToAoaAndPersist(DotquerType dotquerType) {
		try {
			// jede DotquerType enthält Daten für alle TT-SIB Entitäten von AoA bis TtSibStreifen

			if (dotquerType.getArt() == null) {
				log.warn(
					"TT-Sib-Abschnitt mit der Objekt-Id {} hat keine 'Art' und wurde daher nicht hinzugefügt.",
					dotquerType.getObjektId());
				return;
			}

			// Ein neuer Streifen wird grundsätzlich gebraucht
			TtSibStreifen streifen = new TtSibStreifen(
				dotquerType.getBreite() != null ? dotquerType.getBreite().intValue() : null,
				dotquerType.getBisBreite() != null ? dotquerType.getBisBreite().intValue() : null,
				TtSibEinordnung.fromLMRChar(dotquerType.getStreifen()), dotquerType.getStreifennr().intValue(),
				TtSibQuerschnittArt.fromArtHref(dotquerType.getArt().getHref()));

			TtSibAbschnittOderAst aoA = aoAfindenOderErstellen(dotquerType);

			Set<TtSibTeilabschnitt> abschnitte = aoA.getTeilabschnitte();

			TtSibQuerschnitt querschnitt = new TtSibQuerschnitt();
			querschnitt.addStreifen(streifen);

			TtSibTeilabschnitt ttSibTeilabschnitt = new TtSibTeilabschnitt(
				dotquerType.getVst().intValue(),
				dotquerType.getBst().intValue(),
				querschnitt);

			// AoA ist neu
			if (abschnitte.isEmpty()) {
				aoA.addTeilabschnitt(ttSibTeilabschnitt);
			} else {
				abschnitte.stream().filter(a -> a.equals(ttSibTeilabschnitt)).findFirst().ifPresentOrElse(

					// Passender Teilabschnitt für Stationen gefunden -> Streifen einhängen
					passenderAbschnitt -> passenderAbschnitt.getQuerschnitt().addStreifen(streifen),

					// Oder kein passender Teilabschnitt für Stationen gefunden -> Neuer hinzufügen
					() -> aoA.addTeilabschnitt(ttSibTeilabschnitt));
			}

			// EntityManager kennt den AoA noch nicht.
			// An dieser Stelle kann auch unser Index gefüllt werden.
			if (aoA.getId() == null) {
				ttSibRepository.save(aoA);
				abschnittOderAstIndex.put(aoA.getAbschnittOderAstId(), aoA.getId());
			}

			// buffer ersetzen (oder initial setzen)
			bufferedAoA = aoA;
		} catch (Exception e) {
			log.error(
				"TT-Sib-Abschnitt mit der Objekt-Id {} konnte nicht hinzugefügt werden.",
				dotquerType.getObjektId(),
				e);
		}
	}

	private TtSibAbschnittOderAst aoAfindenOderErstellen(
		DotquerType dotquerType) {

		String aoAId = dotquerType.getAbschnittId();
		Long id = abschnittOderAstIndex.get(aoAId);
		if (id != null && id.equals(bufferedAoA.getId())) {
			return bufferedAoA;
		}

		// Abschnitt oder Ast ist vollständig gespeichert und kann zur DB gesendet werden.
		if (bufferedAoA != null) {
			entityManager.flush();
			entityManager.clear();
			log.debug("AbschnittOderAst mit der fachlichen ID {} wurde gespeichert.", aoAId);
		}

		Optional<TtSibAbschnittOderAst> existingAoA;
		if (id != null) {
			existingAoA = ttSibRepository.findById(id);
			log.debug(
				"AbschnittOderAst mit der fachlichen ID {} wurde aus der Datenbank gelesen für die weitere Anreicherung",
				aoAId);
		} else {
			log.debug("AbschnittOderAst mit der fachlichen ID {} ist neu", aoAId);
			existingAoA = Optional.empty();
		}

		TtSibAbschnittOderAst aoA;
		if (existingAoA.isEmpty()) {
			aoA = new TtSibAbschnittOderAst();

			aoA.setAbschnittOderAstId(aoAId);

			Optional<VISTRASSENNETZType> relatedNetz = this.viStrassennetzList.stream()
				.filter(v -> aoAId.equals(v.getABSCHNITTID()))
				.findFirst();

			relatedNetz.ifPresent((VISTRASSENNETZType vistrassennetzType) -> {
				aoA.setAttribute(this.buildAttributeVOFrom(vistrassennetzType));

				// LineStringType stammt aus XML Schema und muss konvertiert werden zu geom.LineString
				String coordinatesFromXML = ((LineStringType) vistrassennetzType.getGEOMETRY().getGeometry().getValue())
					.getCoordinates().getValue();

				List<Coordinate> coordinateList = new ArrayList<>();
				// "1.1,2.2 3.3,4.4 5.5,6.6" -> ["1.1,2.2", "3.3,4.4", "5.5,6.6"]
				for (String coordinateFromXML : coordinatesFromXML.split(" ")) {
					// "1.1,2.2" -> ["1.1", "2.2"]
					String[] langAndLong = coordinateFromXML.split(",");
					coordinateList
						.add(new Coordinate(Double.parseDouble(langAndLong[0]), Double.parseDouble(langAndLong[1])));
				}

				LineString lineString = etrs89Utm32NGeometryFactory.createLineString(
					coordinateList.toArray(new Coordinate[0]));

				aoA.setGeometry(lineString);
			});
		} else {
			aoA = existingAoA.get();
		}

		return aoA;
	}

	private XMLEventReader createReader(String filename) {
		try {
			return xmlInputFactory.createXMLEventReader(new FileInputStream(ttSibXmlFiles.get(filename)));
		} catch (XMLStreamException | FileNotFoundException e) {
			log.error("Fehler beim Einlesen der TT-SIB-Daten " + filename);
			throw new RuntimeException(e);
		}
	}

	private HashMap<String, File> getTtSibXmlFiles(File xmlFolder) {
		File[] fileArray = xmlFolder.listFiles();

		require(fileArray != null,
			String.format("TT-SIB-Root %s muss Dateien enthalten: ", xmlFolder.getPath()));

		List<File> files = Arrays.stream(fileArray)
			.filter(file -> file.getName().endsWith(".xml"))
			.collect(Collectors.toList());

		require(!files.isEmpty(),
			String.format("TT-SIB-Root %s muss XML Dateien enthalten: ", xmlFolder.getPath()));

		HashMap<String, File> xmlFiles = new HashMap<>();

		for (File file : files) {
			xmlFiles.put(file.getName(), file);
		}

		return xmlFiles;
	}

	@SuppressWarnings("unchecked")
	private Attribute buildAttributeVOFrom(VISTRASSENNETZType viStrassennetz) {
		Map<String, Object> map = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
			.convertValue(viStrassennetz, Map.class);
		// Manche Daten sollen nicht als Attribute gespeichert werden
		map.remove("geometry");
		map.remove("abschnittid");
		return Attribute.of(map);
	}

	private void calculateTtSibFahrradwege() {
		log.info("Berechne Tt-Sib-Fahrradwege");
		List<TtSibFahrradweg> ttSibFahrradwege = new LinkedList<>();
		AtomicInteger count = new AtomicInteger();
		ttSibRepository.findAll().forEach(aoa -> {
			if (count.get() % 100 == 0) {
				log.info("Fortschritt Tt-Sib-Fahrradwege: " + count.get());
			}
			aoa.ermittleRadwegverlaeufe().forEach((LineString radwegverlauf) -> {
				ttSibFahrradwege.add(new TtSibFahrradweg(radwegverlauf, aoa.getAttribute()));
			});
			count.incrementAndGet();
		});
		log.info("Speichere Tt-Sib-Fahrradwege");
		this.ttSibFahrradwegRepository.saveAll(ttSibFahrradwege);
		log.info("Fertig (Tt-Sib-Fahrradwege)");
	}

}
