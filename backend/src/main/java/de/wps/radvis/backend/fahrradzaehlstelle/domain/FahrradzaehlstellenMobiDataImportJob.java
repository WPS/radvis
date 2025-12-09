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

package de.wps.radvis.backend.fahrradzaehlstelle.domain;

import static org.valid4j.Assertive.require;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.exception.CsvReadException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Channel;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.FahrradzaehlDatenEintrag;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.FahrradzaehlDatenEintrag.FahrradzaehlDatenEintragBuilder;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Fahrradzaehlstelle;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.FahrradzaehlstellenMobiDataImportStatistik;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.MessDatenEintrag;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.MessDatenEintrag.MessDatenEintragBuilder;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.BetreiberEigeneId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleGebietskoerperschaft;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Seriennummer;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlintervall;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstand;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstatus;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithFehlercode(Fehlercode.FAHRRADZAEHLSTELLEN_IMPORT)
public class FahrradzaehlstellenMobiDataImportJob extends AbstractJob {
	public static class CsvHeader {

		public static final String ISO_TIMESTAMP = "iso_timestamp";
		public static final String ZAEHLSTAND = "zählstand";
		public static final String STAND = "stand";
		public static final String CHANNEL_NAME = "channel_name";
		public static final String CHANNEL_ID = "channel_id";
		public static final String COUNTER_SITE = "counter_site";
		public static final String COUNTER_SITE_ID = "counter_site_id";
		public static final String DOMAIN_NAME = "domain_name";
		public static final String LONGITUDE = "longitude";
		public static final String LATITUDE = "latitude";
		public static final String INTERVAL = "interval";
		public static final String COUNTER_SERIAL = "counter_serial";
		public static final List<String> ALL = List.of(ISO_TIMESTAMP, ZAEHLSTAND, STAND, CHANNEL_NAME, CHANNEL_ID,
			COUNTER_SITE, COUNTER_SITE_ID, DOMAIN_NAME, LONGITUDE, LATITUDE, INTERVAL, COUNTER_SERIAL);
	}

	private final JobConfigurationProperties jobConfigurationProperties;
	private final FahrradzaehlstelleRepository fahrradzaehlstelleRepository;
	private final CsvRepository csvRepository;
	private final CoordinateReferenceSystemConverter converter;

	public FahrradzaehlstellenMobiDataImportJob(
		JobExecutionDescriptionRepository repository,
		JobConfigurationProperties jobConfigurationProperties,
		FahrradzaehlstelleRepository fahrradzaehlstelleRepository,
		CsvRepository csvRepository,
		CoordinateReferenceSystemConverter converter) {
		super(repository);
		this.jobConfigurationProperties = jobConfigurationProperties;
		this.fahrradzaehlstelleRepository = fahrradzaehlstelleRepository;
		this.csvRepository = csvRepository;
		this.converter = converter;
	}

	@Override
	@Transactional
	protected Optional<JobStatistik> doRun() {
		FahrradzaehlstellenMobiDataImportStatistik statistik = new FahrradzaehlstellenMobiDataImportStatistik();

		log.info("FahrradzaehlstellenMobiDataImportJob gestartet.");

		DateTimeFormatter datumsFormatInApplicationProperties = DateTimeFormatter.ofPattern("yyyyMM");
		YearMonth ersterMonat = fahrradzaehlstelleRepository.findeLetztesImportDatum()
			.map(Zeitstempel::toYearMonth)
			.orElseGet(() -> {
				log.info("Es wurden in der Datenbank keine bereits importierten Fahrradzaehldaten gefunden, "
					+ "daher wird das in der application.yml konfigurierte Startdatum verwendet.");
				return YearMonth.parse(
					jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportStartDate(),
					datumsFormatInApplicationProperties);
			});
		YearMonth letzerMonat = YearMonth.from(LocalDate.now());
		log.info("Es wird versucht die Monate von {} bis {} zu importieren", ersterMonat, letzerMonat);

		List<URL> urlsToImport = getUrlsToImport(ersterMonat, letzerMonat, statistik);

		try {
			urlsToImport.add(new URL(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportBaseUrl()
				+ "eco_counter_fahrradzaehler.csv"));
			log.info("Es wird zusätzlich versucht die Daten der letzten 7 Tage zu importieren");
		} catch (MalformedURLException e) {
			log.error(
				"Die Datei der aktuellen Fahrradzähldaten kann aufgrund einer Fehlerhaften URL nicht importiert werden.");
			statistik.anzahlUrlsOderDateiFehlerhaft++;
		}

		for (URL url : urlsToImport) {

			log.info("Die Datei mit folgender URL wird importiert: " + url);

			CsvData csvData;
			try (InputStream is = url.openStream();
				InputStream gis = url.toString().endsWith(".gz") ? new GzipCompressorInputStream(is) : is) {

				byte[] bytes = IOUtils.toByteArray(gis);
				csvData = csvRepository.read(bytes, CsvHeader.ALL, ',', false);

				log.info("Fertig mit einlesen der Csv. Jetzt werden die einzelnen Zeilen in Java Objekte gemapped.");

			} catch (IOException | CsvReadException e) {
				log.error("Die Datei mit der URL {} konnte aus folgendem Grund nicht eingelesen werden: ", url, e);
				statistik.anzahlUrlsOderDateiFehlerhaft++;
				continue;
			}
			Map<BetreiberEigeneId, List<MessDatenEintrag>> betreiberEigeneIdListMap = csvData.getRows().stream()
				.map(csvDataRow -> mapCSVDataRowToMessDatenEintrag(csvDataRow, statistik))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.groupingBy(MessDatenEintrag::getBetreiberEigeneId));

			Map<BetreiberEigeneId, Map<ChannelId, List<MessDatenEintrag>>> result = new HashMap<>();

			betreiberEigeneIdListMap.forEach(
				(key, eintraege) -> result.put(key, eintraege.stream()
					.collect(Collectors.groupingBy(MessDatenEintrag::getChannelId))));

			List<Fahrradzaehlstelle> neueFahrradzaehlstellen = result.values().stream()
				.map(this::mapMessDatenEintragToFahrradzaehlstelle).collect(Collectors.toList());

			log.info(
				"Importierte Fahrradzählstellen werden gemerged und gespeichert: "
					+ neueFahrradzaehlstellen.size());
			List<Fahrradzaehlstelle> alleFahrradzaehlstellen = mergeMitBekanntenFahrradzaehlstellen(
				neueFahrradzaehlstellen, statistik);
			fahrradzaehlstelleRepository.saveAll(alleFahrradzaehlstellen);
			log.info("Speichern abgeschlossen.");
		}

		log.info("JobStatistik: " + statistik);

		return Optional.of(statistik);
	}

	private List<URL> getUrlsToImport(YearMonth start,
		YearMonth letzterMonat, FahrradzaehlstellenMobiDataImportStatistik statistik) {

		String baseUrl = jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportBaseUrl()
			+ "eco_counter_fahrradzaehler_";
		DateTimeFormatter datumsFormatVonImportDatei = DateTimeFormatter.ofPattern("yyyyMM");

		return start.atDay(1)
			.datesUntil(letzterMonat.atEndOfMonth(), Period.ofMonths(1))
			.map(datumsFormatVonImportDatei::format)
			.map(formatedDate -> baseUrl + formatedDate + ".csv.gz")
			.map(s -> {
				try {
					return new URL(s);
				} catch (MalformedURLException e) {
					log.error("Folgende Datei kann aufgrund einer Fehlerhaften URL nicht importiert werden: " + s);
					statistik.anzahlUrlsOderDateiFehlerhaft++;
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private List<Fahrradzaehlstelle> mergeMitBekanntenFahrradzaehlstellen(
		List<Fahrradzaehlstelle> neuImportierteFahrradzaehlstellen,
		FahrradzaehlstellenMobiDataImportStatistik statistik) {
		require(neuImportierteFahrradzaehlstellen.size() < Short.MAX_VALUE,
			"Aufgrund von Postgres wird in der findAllBy... Methode nur eine maximale Anzahl von " + Short.MAX_VALUE
				+ " unterstützt.");

		Map<BetreiberEigeneId, Fahrradzaehlstelle> neuImportierteFahrradzaehlstellenMap = neuImportierteFahrradzaehlstellen
			.stream()
			.collect(Collectors.toMap(Fahrradzaehlstelle::getBetreiberEigeneId, Function.identity()));

		List<Fahrradzaehlstelle> bekannteFahrradzaehlstellen = fahrradzaehlstelleRepository
			.findAllByBetreiberEigeneIdIn(
				neuImportierteFahrradzaehlstellenMap.keySet());

		bekannteFahrradzaehlstellen.forEach(bekannteFahrradzaehlstelle -> {
			bekannteFahrradzaehlstelle.merge(
				neuImportierteFahrradzaehlstellenMap.get(bekannteFahrradzaehlstelle.getBetreiberEigeneId()));
			neuImportierteFahrradzaehlstellenMap.remove(bekannteFahrradzaehlstelle.getBetreiberEigeneId());
		});

		statistik.anzahlFahrradzaehlstellenNeuErstellt += neuImportierteFahrradzaehlstellenMap.values().size();
		bekannteFahrradzaehlstellen.addAll(neuImportierteFahrradzaehlstellenMap.values());

		return bekannteFahrradzaehlstellen;
	}

	private Optional<MessDatenEintrag> mapCSVDataRowToMessDatenEintrag(Map<String, String> csvDataRow,
		FahrradzaehlstellenMobiDataImportStatistik fahrradzaehlstellenMobiDataImportStatistik) {

		MessDatenEintragBuilder messDatenEintragBuilder = MessDatenEintrag.builder();

		// Wenn entweder BetreiberEigeneId, ChannelId, Zaehlstand oder Zeitstempel nicht
		// importiert werden kann,
		// koennen wir den Zaehldateneintrag nicht zuordnen und somit nicht
		// abspeichern/importieren
		try {
			messDatenEintragBuilder.betreiberEigeneId(BetreiberEigeneId.of(csvDataRow.get(CsvHeader.COUNTER_SITE_ID)));
		} catch (NumberFormatException e) {
			log.warn("Es konnte keine Betreiber eigene Id eingelesen werden (" + CsvHeader.COUNTER_SITE_ID + "): \""
				+ csvDataRow.get(CsvHeader.COUNTER_SITE_ID) + "\"\nGrund: " + e);
			fahrradzaehlstellenMobiDataImportStatistik.anzahlMessdatenNichtImportiert_requiredAttributNichtParsebar++;
			return Optional.empty();
		}
		try {
			messDatenEintragBuilder.channelId(ChannelId.of(csvDataRow.get(CsvHeader.CHANNEL_ID)));
		} catch (NumberFormatException e) {
			log.warn("Es konnte keine ChannelId eingelesen werden (" + CsvHeader.CHANNEL_ID + "): \""
				+ csvDataRow.get(CsvHeader.CHANNEL_ID) + "\"\nGrund: " + e);
			fahrradzaehlstellenMobiDataImportStatistik.anzahlMessdatenNichtImportiert_requiredAttributNichtParsebar++;
			return Optional.empty();
		}
		try {
			messDatenEintragBuilder.zaehlstand(Zaehlstand.of(csvDataRow.get(CsvHeader.ZAEHLSTAND)));
		} catch (NumberFormatException | RequireViolation e) {
			log.warn("Es konnte kein Zaehlstand eingelesen werden (" + CsvHeader.ZAEHLSTAND + "): \""
				+ csvDataRow.get(CsvHeader.ZAEHLSTAND) + "\"\nGrund: " + e);
			fahrradzaehlstellenMobiDataImportStatistik.anzahlMessdatenNichtImportiert_requiredAttributNichtParsebar++;
			return Optional.empty();
		}
		try {
			messDatenEintragBuilder.zeitstempel(Zeitstempel.of(csvDataRow.get(CsvHeader.ISO_TIMESTAMP)));
		} catch (DateTimeParseException | RequireViolation e) {
			log.warn("Es konnte kein Zeitstempel eingelesen werden (" + CsvHeader.ISO_TIMESTAMP + "): \""
				+ csvDataRow.get(CsvHeader.ISO_TIMESTAMP) + "\"\nGrund: " + e);
			fahrradzaehlstellenMobiDataImportStatistik.anzahlMessdatenNichtImportiert_requiredAttributNichtParsebar++;
			return Optional.empty();
		}

		// Ohne geometrie ist es auch nicht sinnvoll zu importieren
		try {
			messDatenEintragBuilder.geometrie(extractGeometryAsUtm32Point(csvDataRow));
		} catch (ParseException e) {
			log.warn("Das Trennzeichen der Nachkommastellen von Latitude und Longitude muss ein Punkt sein.");
			fahrradzaehlstellenMobiDataImportStatistik.anzahlMessdatenNichtImportiert_requiredAttributNichtParsebar++;
			return Optional.empty();
		} catch (FactoryException | TransformException e) {
			log.warn("Die angegebenen Koordinaten können nicht in valide UTM32-Koordinaten transformiert werden.");
			fahrradzaehlstellenMobiDataImportStatistik.anzahlMessdatenNichtImportiert_requiredAttributNichtParsebar++;
			return Optional.empty();
		}

		// Diese Attribute sind nullable -> wenn eins nicht Importiert werden kann, wird
		// es null gesetzt und
		// bei den anderen weiterversucht
		String logMeldungFormatString = "Das Attribut {} konnte nicht importiert werden. Nicht importierbarer Wert: \"{}\"";
		try {
			messDatenEintragBuilder.fahrradzaehlstelleGebietskoerperschaft(
				FahrradzaehlstelleGebietskoerperschaft.of(csvDataRow.get(CsvHeader.DOMAIN_NAME)));
		} catch (RequireViolation e) {
			log.debug(logMeldungFormatString, "FahrradzaehlstelleGebietskoerperschaft",
				csvDataRow.get(CsvHeader.DOMAIN_NAME));
			fahrradzaehlstellenMobiDataImportStatistik.anzahlAttributmappingFehlerhaft_trotzdemEingelesen++;
		}
		try {
			messDatenEintragBuilder.fahrradzaehlstelleBezeichnung(
				FahrradzaehlstelleBezeichnung.of(csvDataRow.get(CsvHeader.COUNTER_SITE)));
		} catch (RequireViolation e) {
			log.debug(logMeldungFormatString, "FahrradzaehlstelleBezeichnung", csvDataRow.get(CsvHeader.COUNTER_SITE));
			fahrradzaehlstellenMobiDataImportStatistik.anzahlAttributmappingFehlerhaft_trotzdemEingelesen++;
		}
		try {
			messDatenEintragBuilder.channelBezeichnung(ChannelBezeichnung.of(csvDataRow.get(CsvHeader.CHANNEL_NAME)));
		} catch (RequireViolation e) {
			log.debug(
				logMeldungFormatString, "ChannelBezeichnung", csvDataRow.get(CsvHeader.CHANNEL_NAME));
			fahrradzaehlstellenMobiDataImportStatistik.anzahlAttributmappingFehlerhaft_trotzdemEingelesen++;
		}
		try {
			messDatenEintragBuilder.seriennummer(Seriennummer.of(csvDataRow.get(CsvHeader.COUNTER_SERIAL)));
		} catch (RequireViolation e) {
			log.debug(logMeldungFormatString, "Seriennummer", csvDataRow.get(CsvHeader.COUNTER_SERIAL));
			fahrradzaehlstellenMobiDataImportStatistik.anzahlAttributmappingFehlerhaft_trotzdemEingelesen++;
		}
		try {
			messDatenEintragBuilder.zaehlintervall(Zaehlintervall.of(csvDataRow.get(CsvHeader.INTERVAL)));
		} catch (RequireViolation e) {
			log.debug(logMeldungFormatString, "Zaehlintervall", csvDataRow.get(CsvHeader.INTERVAL));
			fahrradzaehlstellenMobiDataImportStatistik.anzahlAttributmappingFehlerhaft_trotzdemEingelesen++;
		}
		try {
			messDatenEintragBuilder.zaehlstatus(Zaehlstatus.of(csvDataRow.get(CsvHeader.STAND)));
		} catch (NumberFormatException | RequireViolation e) {
			log.debug(logMeldungFormatString, "Zaehlstatus", csvDataRow.get(CsvHeader.STAND));
			fahrradzaehlstellenMobiDataImportStatistik.anzahlAttributmappingFehlerhaft_trotzdemEingelesen++;
		}
		return Optional.of(messDatenEintragBuilder.build());
	}

	private Fahrradzaehlstelle mapMessDatenEintragToFahrradzaehlstelle(
		Map<ChannelId, List<MessDatenEintrag>> messDatenEintraege) {

		MessDatenEintrag messDatenEintrag = messDatenEintraege.values().stream()
			.flatMap(Collection::stream).findFirst()
			.orElseThrow(() -> new RuntimeException(
				"Zu einer Fahrradzählstelle liegen keine Daten vor. Dieser Fall dürfte eigentlich nicht auftreten, da wir sonst keine Zeile in den Messdaten hätten."));

		Zeitstempel neusterZeitstempel = messDatenEintraege.values().stream()
			.flatMap(List::stream).map(MessDatenEintrag::getZeitstempel)
			.max(Comparator.comparing(Zeitstempel::getValue)).orElseThrow(() -> new RuntimeException(
				"Zu einer Fahrradzählstelle liegt kein aktueller Messwert vor. Dieser Fall dürfte eigentlich nicht auftreten, da wir sonst keine Zeile in den Messdaten hätten."));

		return Fahrradzaehlstelle.builder()
			.geometrie(messDatenEintrag.getGeometrie())
			.betreiberEigeneId(messDatenEintrag.getBetreiberEigeneId())
			.neusterZeitstempel(neusterZeitstempel)
			.fahrradzaehlstelleGebietskoerperschaft(
				messDatenEintrag.getFahrradzaehlstelleGebietskoerperschaft().orElse(null))
			.fahrradzaehlstelleBezeichnung(messDatenEintrag.getFahrradzaehlstelleBezeichnung().orElse(null))
			.seriennummer(messDatenEintrag.getSeriennummer().orElse(null))
			.zaehlintervall(messDatenEintrag.getZaehlintervall().orElse(null))
			.channels(
				messDatenEintraege.values().stream()
					.map(this::mapMessDatenEintragToChannel)
					.collect(Collectors.toList()))
			.build();
	}

	private Channel mapMessDatenEintragToChannel(List<MessDatenEintrag> messDatenEintraege) {
		MessDatenEintrag messDatenEintrag = messDatenEintraege.stream().findFirst()
			.orElseThrow(() -> new RuntimeException(
				"Zu einem Channel liegen keine Daten vor. Dieser Fall dürfte eigentlich nicht auftreten, da wir sonst keine Zeile in den Messdaten haetten."));

		return Channel.builder()
			.channelId(messDatenEintrag.getChannelId())
			.channelBezeichnung(messDatenEintrag.getChannelBezeichnung().orElse(null))
			.fahrradzaehlDaten(
				messDatenEintraege.stream().collect(Collectors.toMap(
					MessDatenEintrag::getZeitstempel,
					this::mapMessDatenEintragToFahrradzaehlDatenEintrag,
					// Definieren was passieren soll, wenn ein Key doppelt vorhanden ist
					// Dies tritt bei Zeitumstellungen (meist im maerz) auf, wo es zwei Messdaten zu
					// demselben
					// Zeitpunkt in EpochSecs gibt
					(schonVorhandenerWert, neuerWert) -> {
						if (neuerWert.getZaehlstand().equals(Zaehlstand.of(0L))) {
							return schonVorhandenerWert;
						} else {
							return neuerWert;
						}
					})))
			.build();
	}

	private FahrradzaehlDatenEintrag mapMessDatenEintragToFahrradzaehlDatenEintrag(MessDatenEintrag messDatenEintrag) {
		FahrradzaehlDatenEintragBuilder fahrradzaehlDatenEintragBuilder = FahrradzaehlDatenEintrag.builder();

		fahrradzaehlDatenEintragBuilder.zaehlstatus(messDatenEintrag.getZaehlstatus().orElse(null));
		fahrradzaehlDatenEintragBuilder.zaehlstand(messDatenEintrag.getZaehlstand());

		return fahrradzaehlDatenEintragBuilder.build();
	}

	private Point extractGeometryAsUtm32Point(Map<String, String> csvDataRow) throws ParseException,
		TransformException, FactoryException {
		NumberFormat format = NumberFormat.getInstance(Locale.US);
		double lat = format.parse(csvDataRow.get(FahrradzaehlstellenMobiDataImportJob.CsvHeader.LATITUDE))
			.doubleValue();
		double lon = format.parse(csvDataRow.get(FahrradzaehlstellenMobiDataImportJob.CsvHeader.LONGITUDE))
			.doubleValue();

		Coordinate coordinateWGS84 = new Coordinate(lat, lon);
		Coordinate coordinateUTM32 = converter.transformCoordinate(coordinateWGS84,
			KoordinatenReferenzSystem.WGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(coordinateUTM32);
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Importiert alle Fahrradzählstellen von MobiData seit dem letzten Import-Datum. Es werden dabei die monatlichen CSV-Dateien importiert. Neue Fahrradzählstellen werden ergänzt, alte bleiben bestehen.",
			"Fahrradzählstellen und Messergebnisse werden in der DB gespeichert.",
			JobExecutionDurationEstimate.MEDIUM
		);
	}
}
