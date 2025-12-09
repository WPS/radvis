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
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import de.wps.radvis.backend.abstellanlage.domain.valueObject.MobiDataQuellId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithFehlercode(Fehlercode.ABSTELLANLAGEN_IMPORT)
public class AbstellanlageBRImportJob extends AbstractJob {
	public static final String JOB_NAME = "AbstellanlageBRImportJob";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(
		DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private final CoordinateReferenceSystemConverter converter;
	private final AbstellanlageRepository abstellanlageRepository;
	private final String abstellanlageBRDatenquellenImportUrl;
	private final String abstellanlageBRImportUrl;

	public AbstellanlageBRImportJob(
		JobExecutionDescriptionRepository repository,
		CoordinateReferenceSystemConverter converter,
		AbstellanlageRepository abstellanlageRepository,
		String abstellanlageBRDatenquellenImportUrl,
		String abstellanlageBRImportUrl) {
		super(repository);
		this.converter = converter;
		this.abstellanlageRepository = abstellanlageRepository;
		this.abstellanlageBRDatenquellenImportUrl = abstellanlageBRDatenquellenImportUrl;
		this.abstellanlageBRImportUrl = abstellanlageBRImportUrl;
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Importiert Abstellanlagen aus einem JSON-Endpunkt der ParkApi Daten. Duplikate und Abstellanlagen mit der Quelle 'RadVIS' werden übersprungen und bereits existierende aktualisiert.",
			"Abstellanlagen werden erstellt und in der Datenbank gespeichert.",
			JobExecutionDurationEstimate.SHORT);
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
		Set<Long> aktuelleAbstellanlagenIds = new HashSet<>();

		Optional<Integer> radvisParkApiId = tryToGetRadvisParkApiId(abstellanlageBRImportStatistik);

		if (radvisParkApiId.isEmpty()) {
			return Optional.of(abstellanlageBRImportStatistik);
		}

		log.info("Zu importierende Abstellanlagen B+R GeoJson Url: {}", this.abstellanlageBRImportUrl);

		Optional<ParkApiAbstellanlagenJson> parkApiAbstellanlagenJson = tryToGetAbstellanlagenJson(
			this.abstellanlageBRImportUrl, abstellanlageBRImportStatistik);

		if (parkApiAbstellanlagenJson.isEmpty()) {
			return Optional.of(abstellanlageBRImportStatistik);
		}

		List<ParkApiAbstellanlageJson> zuImportierendeDatensaetze = parkApiAbstellanlagenJson.get().getItems();
		log.info("Anzahl Datensätze im Json: {}", zuImportierendeDatensaetze.size());

		Set<Pair<ExterneAbstellanlagenId, MobiDataQuellId>> bereitsImportierteAbstellanlagen = new HashSet<>();
		for (ParkApiAbstellanlageJson abstellanlageJson : zuImportierendeDatensaetze) {
			// Keine in RadVIS gepflegten Anlagen importieren
			if (abstellanlageJson.getSource_id().equals(radvisParkApiId.get())) {
				abstellanlageBRImportStatistik.anzahlRadVISAbstellanlagenUebersprungen++;
				continue;
			}

			ExterneAbstellanlagenId externeId = ExterneAbstellanlagenId.of(abstellanlageJson.getOriginal_uid());
			MobiDataQuellId mobiDataQuellId = MobiDataQuellId.of(abstellanlageJson.getSource_id());

			// Keine Duplikate importieren
			Pair<ExterneAbstellanlagenId, MobiDataQuellId> mobiDataIdentifier = Pair.of(externeId, mobiDataQuellId);
			if (bereitsImportierteAbstellanlagen.contains(mobiDataIdentifier)) {
				log.error("Duplicate ID {} found for MobiDataQuellId {}!", externeId, mobiDataQuellId);
				abstellanlageBRImportStatistik.anzahlAbstellanlagenIdNichtEindeutig++;
				continue;
			}
			bereitsImportierteAbstellanlagen.add(mobiDataIdentifier);

			// Auf Abstellanlage mappen und - wenn Mapping möglich - persistieren
			mapToAbstellanlage(
				abstellanlageJson,
				externeId,
				mobiDataQuellId,
				abstellanlageBRImportStatistik).ifPresent(abstellanlage -> {
					abstellanlageRepository.save(abstellanlage);
					aktuelleAbstellanlagenIds.add(abstellanlage.getId());
				});
		}
		// noinspection UnnecessaryLocalVariable
		int anzahlGeloeschteMobiDataAbstellanlagen = abstellanlageRepository
			.deleteAllByQuellSystemAndIdNotIn(AbstellanlagenQuellSystem.MOBIDATABW, aktuelleAbstellanlagenIds);
		abstellanlageBRImportStatistik.anzahlGeloescht = anzahlGeloeschteMobiDataAbstellanlagen;

		log.info("JobStatistik: {}", abstellanlageBRImportStatistik);
		return Optional.of(abstellanlageBRImportStatistik);
	}

	private Optional<ParkApiAbstellanlagenJson> tryToGetAbstellanlagenJson(String urlString,
		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik) {
		try (InputStream is = new URL(urlString).openStream()) {
			byte[] bytes = IOUtils.toByteArray(is);
			return Optional.of(OBJECT_MAPPER.readValue(bytes, ParkApiAbstellanlagenJson.class));
		} catch (MalformedURLException e) {
			log.error(
				"Folgende Datei kann aufgrund einer Fehlerhaften URL nicht importiert werden: {}",
				urlString, e);
			abstellanlageBRImportStatistik.urlOderGeojsonFehlerhaft = true;
			return Optional.empty();
		} catch (IOException e) {
			log.error("Die Datei mit der URL {} konnte aus folgendem Grund nicht eingelesen werden: ", urlString, e);
			abstellanlageBRImportStatistik.urlOderGeojsonFehlerhaft = true;
			return Optional.empty();
		}
	}

	private Optional<Integer> tryToGetRadvisParkApiId(AbstellanlageBRImportStatistik abstellanlageBRImportStatistik) {
		Integer radvisParkApiId = null;

		try (
			InputStream is = new URL(this.abstellanlageBRDatenquellenImportUrl).openStream()) {
			ParkApiDatenquellenJson parkApiDatenquellenJson = OBJECT_MAPPER.readValue(
				is,
				ParkApiDatenquellenJson.class);
			Optional<ParkApiDatenquelleJson> radvisQuelle = parkApiDatenquellenJson.getItems().stream()
				.filter(quelle -> quelle.getUid().equalsIgnoreCase("radvis_bw")).findFirst();
			radvisParkApiId = radvisQuelle.map(ParkApiDatenquelleJson::getId).orElse(null);
		} catch (IOException e) {
			log.error(
				"Folgende Datei mit Datenquellen kann aufgrund einer Fehlerhaften URL nicht ausgelesen werden: {}",
				this.abstellanlageBRDatenquellenImportUrl, e);
			abstellanlageBRImportStatistik.urlOderGeojsonFehlerhaft = true;
		}
		return Optional.ofNullable(radvisParkApiId);
	}

	private Optional<Abstellanlage> mapToAbstellanlage(
		ParkApiAbstellanlageJson abstellanlageJson,
		ExterneAbstellanlagenId externeId,
		MobiDataQuellId mobiDataQuellId,
		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik) {
		AbstellanlageBuilder abstellanlageBuilder = getExistingOrNewAbstellanlagenBuilder(externeId, mobiDataQuellId);
		try {
			mapAttributesAndAddToBuilder(abstellanlageBuilder, abstellanlageJson);
		} catch (AbstellanlageAttributMappingException e) {
			log.warn("Die Abstellanlage mit der ID {} kann nicht importiert werden.", externeId.getValue(), e);
			abstellanlageBRImportStatistik.anzahlAbstellanlagenAttributmappingFehlerhaft++;
			return Optional.empty();
		}
		Abstellanlage abstellanlage = abstellanlageBuilder.build();
		if (abstellanlage.getId() == null) {
			abstellanlageBRImportStatistik.anzahlNeuErstellt++;
		} else {
			abstellanlageBRImportStatistik.anzahlGeupdated++;
		}
		return Optional.of(abstellanlage);
	}

	private AbstellanlageBuilder getExistingOrNewAbstellanlagenBuilder(ExterneAbstellanlagenId externeId,
		MobiDataQuellId mobiDataQuellId) {
		Optional<Abstellanlage> abstellanlageOpt = abstellanlageRepository
			.findByExterneIdAndQuellSystemAndMobiDataQuellId(
				externeId,
				AbstellanlagenQuellSystem.MOBIDATABW,
				mobiDataQuellId);
		AbstellanlageBuilder abstellanlageBuilder = abstellanlageOpt.map(Abstellanlage::toBuilder)
			.orElse(Abstellanlage.builder().dokumentListe(new DokumentListe()).externeId(externeId).mobiDataQuellId(
				mobiDataQuellId));
		// Add Default Attributes
		return abstellanlageBuilder
			.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
			.zustaendig(null)
			.ueberwacht(Ueberwacht.UNBEKANNT)
			.abstellanlagenOrt(AbstellanlagenOrt.BIKE_AND_RIDE)
			.groessenklasse(null)
			.status(AbstellanlagenStatus.AKTIV);
	}

	private void mapAttributesAndAddToBuilder(
		AbstellanlageBuilder abstellanlageBuilder,
		ParkApiAbstellanlageJson abstellanlageJson)
		throws AbstellanlageAttributMappingException {
		// Geometrie
		try {
			abstellanlageBuilder.geometrie(extractGeometryAsUtm32Point(abstellanlageJson));
		} catch (ParseException e) {
			throw new AbstellanlageAttributMappingException(
				"Die Felder Latitude und Longitude müssen Kommazahlen im Format 1.234 sein.");
		} catch (FactoryException | TransformException e) {
			throw new AbstellanlageAttributMappingException(
				"Die angegebenen Koordinaten können nicht in valide UTM32-Koordinaten transformiert werden.");
		}

		// Betreiber
		String betreiberString = abstellanlageJson.getOperator_name();
		if (betreiberString == null || betreiberString.isEmpty()) {
			betreiberString = "Unbekannt";
		}

		abstellanlageBuilder.betreiber(AbstellanlagenBetreiber.of(betreiberString));

		// Anzahl Stellplaetze
		Integer stellplatzanzahl = abstellanlageJson.getCapacity();
		if (stellplatzanzahl == null) {
			throw new AbstellanlageAttributMappingException(
				"Es muss eine Anzahl an Stellplätzen angegeben sein. (Attribut: \"capacity\").");
		} else {
			abstellanlageBuilder.anzahlStellplaetze(AnzahlStellplaetze.of(stellplatzanzahl));
		}

		// Stellplatzart
		abstellanlageBuilder.stellplatzart(mapAnlagentypAufStellplatzart(abstellanlageJson.getType()));

		// Ueberdacht
		abstellanlageBuilder.ueberdacht(Ueberdacht.of(abstellanlageJson.isCovered().orElse(false)));

		// Beschreibung
		String beschreibungString = abstellanlageJson.getDescription();
		if (beschreibungString == null || beschreibungString.isEmpty()) {
			abstellanlageBuilder.beschreibung(null);
		} else {
			abstellanlageBuilder.beschreibung(AbstellanlagenBeschreibung.of(beschreibungString));
		}

		// Weitere Informationen
		abstellanlageBuilder.weitereInformation(generateWeitereInformationen(abstellanlageJson));
	}

	private Point extractGeometryAsUtm32Point(ParkApiAbstellanlageJson json) throws ParseException,
		TransformException, FactoryException {
		NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
		double lat = format.parse(json.getLat()).doubleValue();
		double lon = format.parse(json.getLon()).doubleValue();

		Coordinate coordinateWGS84 = new Coordinate(lat, lon);
		Coordinate coordinateUTM32 = converter.transformCoordinate(
			coordinateWGS84,
			KoordinatenReferenzSystem.WGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(coordinateUTM32);
	}

	private Stellplatzart mapAnlagentypAufStellplatzart(String anlagentyp)
		throws AbstellanlageAttributMappingException {
		return switch (anlagentyp) {
		case "WALL_LOOPS", "SAFE_WALL_LOOPS" -> Stellplatzart.VORDERRADANSCHLUSS;
		case "LOCKBOX", "LOCKERS" -> Stellplatzart.FAHRRADBOX;
		case "STANDS" -> Stellplatzart.ANLEHNBUEGEL;
		case "TWO_TIER" -> Stellplatzart.DOPPELSTOECKIG;
		case "SHED" -> Stellplatzart.SAMMELANLAGE;
		case "BUILDING" -> Stellplatzart.FAHRRADPARKHAUS;
		case "OTHER", "FLOOR" -> Stellplatzart.SONSTIGE;
		default -> throw new AbstellanlageAttributMappingException(
			"Es muss ein gültiger Anlagentyp (Stellplatzart) angegeben sein. Falscher Wert: " + anlagentyp);
		};
	}

	private AbstellanlagenWeitereInformation generateWeitereInformationen(ParkApiAbstellanlageJson abstellanlageJson) {
		String anlageFotoUrl = abstellanlageJson.getPhoto_url();
		String anlageInfoUrl = abstellanlageJson.getPublic_url();
		if ((anlageFotoUrl == null || anlageFotoUrl.isEmpty()) && (anlageInfoUrl == null || anlageInfoUrl.isEmpty())) {
			return null;
		}

		String htmlText = "<ul>\n"
			+ generateHtmlLinkIfUrlNotEmpty("Weitere Informationen zur Anlage", anlageInfoUrl)
			+ generateHtmlLinkIfUrlNotEmpty("Foto der Anlage", anlageFotoUrl)
			+ "</ul>";
		return AbstellanlagenWeitereInformation.of(htmlText);
	}

	private String generateHtmlLinkIfUrlNotEmpty(String linkName, String linkUrl) {
		return linkUrl == null || linkUrl.isEmpty() ? ""
			: String.format("<li><a href=\"%s\" target=\"_blank\">%s</a></li>\n", linkUrl, linkName);
	}

	@Getter
	@Setter
	public static class ParkApiAbstellanlageJson {
		private Integer source_id;
		private String original_uid;
		private String operator_name;
		private String lat;
		private String lon;

		private String type;
		private Integer capacity;
		private Boolean is_covered;
		private String description;
		private String photo_url;
		private String public_url;

		private Optional<Boolean> isCovered() {
			return Optional.ofNullable(is_covered);
		}
	}

	@Getter
	@Setter
	public static class ParkApiAbstellanlagenJson {
		private List<ParkApiAbstellanlageJson> items;
	}

	@Getter
	@Setter
	public static class ParkApiDatenquelleJson {
		private Integer id;
		private String uid;
		private String name;
	}

	@Getter
	@Setter
	public static class ParkApiDatenquellenJson {
		private List<ParkApiDatenquelleJson> items;
	}
}
