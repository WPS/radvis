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

package de.wps.radvis.backend.abstellanlage.domain;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage.AbstellanlageBuilder;
import de.wps.radvis.backend.abstellanlage.domain.entity.AbstellanlageBRImportStatistik;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBeschreibung;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBetreiber;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenOrt;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenStatus;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenWeitereInformation;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlStellplaetze;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.ExterneAbstellanlagenId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.exception.CsvReadException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithFehlercode(Fehlercode.ABSTELLANLAGEN_IMPORT)
public class AbstellanlageBRImportJob extends AbstractJob {
	public static final String JOB_NAME = "AbstellanlageBRImportJob";

	public static class CsvHeader {
		public static final String ID = "ID";
		public static final String DATENQUELLE = "Datenquelle";
		public static final String LONGITUDE = "Longitude";
		public static final String LATITUDE = "Latitude";
		public static final String ANLAGENTYP = "Anlagentyp";
		public static final String STELLPLATZANZAHL = "Stellplatzanzahl";
		public static final String UEBERDACHT = "ueberdacht";
		public static final String NOTIZEN = "Notizen";
		public static final String ANLAGE_FOTO = "Anlage_Foto";
		public static final String WEGZUR_ANLAGE_FOTO = "WegzurAnlage_Foto";
		public static final String HINDERNISZUFAHRT_FOTO = "Hinderniszufahrt_Foto";
		public static final String BESONDERHEITEN_FOTO = "Besonderheiten_Foto";
		public static final List<String> ALL = List.of(
			ID, DATENQUELLE, LONGITUDE, LATITUDE, ANLAGENTYP, STELLPLATZANZAHL, UEBERDACHT, NOTIZEN, ANLAGE_FOTO,
			WEGZUR_ANLAGE_FOTO, HINDERNISZUFAHRT_FOTO, BESONDERHEITEN_FOTO);
	}

	private final JobConfigurationProperties jobConfigurationProperties;
	private final CsvRepository csvRepository;
	private final CoordinateReferenceSystemConverter converter;
	private final AbstellanlageRepository abstellanlageRepository;

	public AbstellanlageBRImportJob(JobExecutionDescriptionRepository repository,
		JobConfigurationProperties jobConfigurationProperties, CsvRepository csvRepository,
		CoordinateReferenceSystemConverter converter, AbstellanlageRepository abstellanlageRepository) {
		super(repository);
		this.jobConfigurationProperties = jobConfigurationProperties;
		this.csvRepository = csvRepository;
		this.converter = converter;
		this.abstellanlageRepository = abstellanlageRepository;
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.ABSTELLANLAGE_BR_IMPORT)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.ABSTELLANLAGE_BR_IMPORT)
	protected Optional<JobStatistik> doRun() {
		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = new AbstellanlageBRImportStatistik();

		log.info("AbstellanlageBRImportJob gestartet.");
		Set<ExterneAbstellanlagenId> aktuelleAbstellanlagen = new HashSet<>();

		List<URL> urlsToImport = jobConfigurationProperties.getAbstellanlageBRImportUrlList().stream()
			.map(s -> {
				try {
					return new URL(s);
				} catch (MalformedURLException e) {
					log.error("Folgende Datei kann aufgrund einer Fehlerhaften URL nicht importiert werden: " + s);
					abstellanlageBRImportStatistik.anzahlUrlsOderDateienFehlerhaft++;
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		log.info("Zu importierende Abstellanlagen B+R Csv Dateien: " + urlsToImport);

		Set<ExterneAbstellanlagenId> bereitsImportierteAbstellanlagen = new HashSet<>();
		for (URL url : urlsToImport) {
			CsvData csvData;
			try (InputStream is = url.openStream()) {
				byte[] bytes = IOUtils.toByteArray(is);
				csvData = csvRepository.read(bytes, CsvHeader.ALL, ';', true);

			} catch (IOException | CsvReadException e) {
				log.error("Die Datei mit der URL {} konnte aus folgendem Grund nicht eingelesen werden: ", url, e);
				abstellanlageBRImportStatistik.anzahlUrlsOderDateienFehlerhaft++;
				continue;
			}

			abstellanlageBRImportStatistik.anzahlBeimCsvImportUebersprungenerZeilen = csvData
				.getAnzahlUebersprungenerZeilen();
			log.info("Anzahl zeilen in der csvData: " + csvData.getRows().size());

			for (Map<String, String> csvDataRow : csvData.getRows()) {
				ExterneAbstellanlagenId externeId = ExterneAbstellanlagenId.of(csvDataRow.get(CsvHeader.ID));
				if (bereitsImportierteAbstellanlagen.contains(externeId)) {
					log.error("Duplicate ID found: " + externeId);
					abstellanlageBRImportStatistik.anzahlAbstellanlagenIdNichtEindeutig++;
					continue;
				}
				if (csvDataRow.get(CsvHeader.DATENQUELLE)
					.equalsIgnoreCase(AbstellanlagenQuellSystem.RADVIS.toString())) {
					abstellanlageBRImportStatistik.anzahlRadVISAbstellanlagenUebersprungen++;
					continue;
				}
				bereitsImportierteAbstellanlagen.add(externeId);
				AbstellanlageBuilder abstellanlageBuilder = getExistingOrNewAbstellanlagenBuilder(externeId);
				addDefaultBRAttributes(abstellanlageBuilder);
				try {
					mapAttributesAndAddToBuilder(abstellanlageBuilder, csvDataRow);
				} catch (AbstellanlageAttributMappingException e) {
					log.warn(
						"Die Abstellanlage mit der ID " + externeId.getValue()
							+ " kann nicht importiert werden, aufgrund von: "
							+ e);
					abstellanlageBRImportStatistik.anzahlAbstellanlagenAttributmappingFehlerhaft++;
					continue;
				}
				Abstellanlage abstellanlage = abstellanlageBuilder.build();
				if (abstellanlage.getId() == null) {
					abstellanlageBRImportStatistik.anzahlNeuErstellt++;
				} else {
					abstellanlageBRImportStatistik.anzahlGeupdated++;
				}
				abstellanlageRepository.save(abstellanlage);
				aktuelleAbstellanlagen.add(externeId);
			}
		}
		if (!urlsToImport.isEmpty()) {
			int anzahlGeloeschteMobiDataAbstellanlagen = abstellanlageRepository
				.deleteAllByQuellSystemAndExterneIdNotIn(
					AbstellanlagenQuellSystem.MOBIDATABW, aktuelleAbstellanlagen);
			abstellanlageBRImportStatistik.anzahlGeloescht = anzahlGeloeschteMobiDataAbstellanlagen;
		}

		log.info("JobStatistik: " + abstellanlageBRImportStatistik);
		return Optional.of(abstellanlageBRImportStatistik);
	}

	private AbstellanlageBuilder getExistingOrNewAbstellanlagenBuilder(ExterneAbstellanlagenId externeId) {
		Optional<Abstellanlage> abstellanlageOpt = abstellanlageRepository.findByExterneIdAndQuellSystem(externeId,
			AbstellanlagenQuellSystem.MOBIDATABW);
		return abstellanlageOpt.map(Abstellanlage::toBuilder)
			.orElse(Abstellanlage.builder().dokumentListe(new DokumentListe()).externeId(externeId));
	}

	private void addDefaultBRAttributes(AbstellanlageBuilder abstellanlageBuilder) {
		abstellanlageBuilder.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
			.zustaendig(null)
			.ueberwacht(Ueberwacht.UNBEKANNT)
			.abstellanlagenOrt(AbstellanlagenOrt.BIKE_AND_RIDE)
			.groessenklasse(null)
			.status(AbstellanlagenStatus.AKTIV);
	}

	private void mapAttributesAndAddToBuilder(AbstellanlageBuilder abstellanlageBuilder, Map<String, String> csvDataRow)
		throws AbstellanlageAttributMappingException {
		// Geometrie
		try {
			abstellanlageBuilder.geometrie(extractGeometryAsUtm32Point(csvDataRow));
		} catch (ParseException e) {
			throw new AbstellanlageAttributMappingException(
				"Die Felder Latitude und Longitude müssen Kommazahlen sein.");
		} catch (FactoryException | TransformException e) {
			throw new AbstellanlageAttributMappingException(
				"Die angegebenen Koordinaten können nicht in valide UTM32-Koordinaten transformiert werden.");
		}

		// Betreiber
		String betreiberString = csvDataRow.get(CsvHeader.DATENQUELLE);
		if (betreiberString.isEmpty()) {
			throw new AbstellanlageAttributMappingException(
				"Es muss ein Betreiber angegeben sein. (Spaltenname: \"" + CsvHeader.DATENQUELLE + "\").");
		} else {
			abstellanlageBuilder.betreiber(AbstellanlagenBetreiber.of(betreiberString));
		}

		// Anzahl Stellplaetze
		String stellplatzanzahlString = csvDataRow.get(CsvHeader.STELLPLATZANZAHL);
		if (stellplatzanzahlString.isEmpty()) {
			throw new AbstellanlageAttributMappingException(
				"Es muss eine Anzahl an Stellplätzen angegeben sein. (Spaltenname: \"" + CsvHeader.STELLPLATZANZAHL
					+ "\").");
		} else {
			abstellanlageBuilder.anzahlStellplaetze(AnzahlStellplaetze.of(stellplatzanzahlString));
		}

		// Stellplatzart
		abstellanlageBuilder.stellplatzart(mapAnlagentypAufStellplatzart(csvDataRow.get(CsvHeader.ANLAGENTYP)));

		// Ueberdacht
		AbstellanlageImportService.pruefeBooleanInput("Überdacht", csvDataRow.get(CsvHeader.UEBERDACHT));
		abstellanlageBuilder.ueberdacht(Ueberdacht.of(csvDataRow.get(CsvHeader.UEBERDACHT).equals("ja")));

		// Beschreibung
		String notizenString = csvDataRow.get(CsvHeader.NOTIZEN);
		if (notizenString.isEmpty()) {
			abstellanlageBuilder.beschreibung(null);
		} else {
			abstellanlageBuilder.beschreibung(AbstellanlagenBeschreibung.of(notizenString));
		}

		// Weitere Informationen
		abstellanlageBuilder.weitereInformation(generateWeitereInformationen(csvDataRow));
	}

	private Point extractGeometryAsUtm32Point(Map<String, String> csvDataRow) throws ParseException,
		TransformException, FactoryException {
		NumberFormat format = NumberFormat.getInstance(Locale.GERMAN);
		double lat = format.parse(csvDataRow.get(CsvHeader.LATITUDE)).doubleValue();
		double lon = format.parse(csvDataRow.get(CsvHeader.LONGITUDE)).doubleValue();

		Coordinate coordinateWGS84 = new Coordinate(lat, lon);
		Coordinate coordinateUTM32 = converter.transformCoordinate(coordinateWGS84,
			KoordinatenReferenzSystem.WGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(coordinateUTM32);
	}

	private Stellplatzart mapAnlagentypAufStellplatzart(String anlagentyp)
		throws AbstellanlageAttributMappingException {
		switch (anlagentyp) {
		case "Vorderradhalter":
			return Stellplatzart.VORDERRADANSCHLUSS;
		case "Fahrradboxen":
			return Stellplatzart.FAHRRADBOX;
		case "Anlehnbuegel":
			return Stellplatzart.ANLEHNBUEGEL;
		case "doppelstoeckig":
			return Stellplatzart.DOPPELSTOECKIG;
		case "Fahrradsammelanlage":
			return Stellplatzart.SAMMELANLAGE;
		case "automatisches Parksystem":
			return Stellplatzart.AUTOMATISCHES_PARKSYSTEM;
		case "Fahrradparkhaus":
			return Stellplatzart.FAHRRADPARKHAUS;
		case "Sonstiges":
			return Stellplatzart.SONSTIGE;
		}
		throw new AbstellanlageAttributMappingException(
			"Es muss ein gültiger Anlagentyp (Stellplatzart) angegeben sein. Falscher Wert: " + anlagentyp);
	}

	private AbstellanlagenWeitereInformation generateWeitereInformationen(Map<String, String> csvDataRow) {
		String anlageFotoUrl = csvDataRow.get(CsvHeader.ANLAGE_FOTO);
		String wegZurAnlageUrl = csvDataRow.get(CsvHeader.WEGZUR_ANLAGE_FOTO);
		String hindernisZufahrtUrl = csvDataRow.get(CsvHeader.HINDERNISZUFAHRT_FOTO);
		String besonderheitenUrl = csvDataRow.get(CsvHeader.BESONDERHEITEN_FOTO);

		if (anlageFotoUrl.isEmpty() && wegZurAnlageUrl.isEmpty() && hindernisZufahrtUrl.isEmpty()
			&& besonderheitenUrl.isEmpty()) {
			return null;
		}

		StringBuilder htmlText = new StringBuilder();
		htmlText.append("<ul>\n")
			.append(generateHtmlLinkIfUrlNotEmpty("Anlage", anlageFotoUrl))
			.append(generateHtmlLinkIfUrlNotEmpty("Weg zur Anlage", wegZurAnlageUrl))
			.append(generateHtmlLinkIfUrlNotEmpty("Hinderniszufahrt", hindernisZufahrtUrl))
			.append(generateHtmlLinkIfUrlNotEmpty("Besonderheiten", besonderheitenUrl))
			.append("</ul>");
		return AbstellanlagenWeitereInformation.of(htmlText.toString());
	}

	private String generateHtmlLinkIfUrlNotEmpty(String linkName, String linkUrl) {
		return linkUrl.isEmpty() ? ""
			: String.format("<li><a href=\"%s\" target=\"_blank\">%s</a></li>\n", linkUrl, linkName);
	}
}
