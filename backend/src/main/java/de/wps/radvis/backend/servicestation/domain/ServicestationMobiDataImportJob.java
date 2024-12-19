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

package de.wps.radvis.backend.servicestation.domain;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Point;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.entity.ServicestationMobiDataImportStatistik;
import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;
import de.wps.radvis.backend.servicestation.domain.valueObject.Fahrradhalterung;
import de.wps.radvis.backend.servicestation.domain.valueObject.Gebuehren;
import de.wps.radvis.backend.servicestation.domain.valueObject.Kettenwerkzeug;
import de.wps.radvis.backend.servicestation.domain.valueObject.Luftpumpe;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationName;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationStatus;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationTyp;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationenQuellSystem;
import de.wps.radvis.backend.servicestation.domain.valueObject.Werkzeug;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Importiert die Servicestationen von MobiDataBW. Die Stationen werden über den Namen (in Verbindung mit dem
 * Quellsystem MobiData) und die Verortung (ein Versatz von max. 10 Metern wird toleriert) identifiziert und
 * entsprechend geupdatet oder neu angelegt. Wenn in einem Datensatz ein Name doppelt vorhanden ist und beide Datensätze
 * auf dieselbe vorhandene Station gemappt werden, dann wir die zweite Station neu angelegt. In jedem Fall entspricht
 * der Datenstand nach dem Update den importierten Daten. Wenn die Identität der Stationen beim Import tatsächlich
 * komplett stabil bleiben soll, bräuchten wir in den Daten von MobiDataBW eine ID. Diese fehlt dort allerdings.
 */
@Slf4j
@WithFehlercode(Fehlercode.SERVICESTATIONEN_IMPORT)
public class ServicestationMobiDataImportJob extends AbstractJob {
	public static final String JOB_NAME = "ServicestationMobiDataImportJob";
	static final double MAX_VERSCHIEBUNG_VORHANDENER_STATION = 10.0;

	private final GeoJsonImportRepository geoJsonImportRepository;

	private final ServicestationRepository servicestationRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final VerwaltungseinheitRepository verwaltungseinheitRepository;
	private final String servicestationenMobiDataImportUrl;
	private final ObjectMapper objectMapper;

	static class Fields {
		public static final String NAME = "name";
		public static final String ADDRESS = "address";
		public static final String OPERATOR = "operator";
		public static final String SUPPLIER = "supplier";
		public static final String SIZE = "size";

		public static final String GEOMETRY = "geometry";
	}

	@Getter
	static class ServicestationSupplier {
		private String name;
	}

	@Getter
	static class ServicestationAddress {
		private String street;
	}

	public ServicestationMobiDataImportJob(
		JobExecutionDescriptionRepository repository,
		GeoJsonImportRepository geoJsonImportRepository,
		ServicestationRepository servicestationRepository,
		VerwaltungseinheitService verwaltungseinheitService,
		VerwaltungseinheitRepository verwaltungseinheitRepository,
		String servicestationenMobiDataImportUrl) {
		super(repository);
		this.geoJsonImportRepository = geoJsonImportRepository;
		this.servicestationRepository = servicestationRepository;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.verwaltungseinheitRepository = verwaltungseinheitRepository;
		this.servicestationenMobiDataImportUrl = servicestationenMobiDataImportUrl;
		this.objectMapper = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.SERVICESTATION_MOBIDATA_IMPORT)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.SERVICESTATION_MOBIDATA_IMPORT)
	protected Optional<JobStatistik> doRun() {
		ServicestationMobiDataImportStatistik statistik = new ServicestationMobiDataImportStatistik();

		log.info("ServicestationMobiDataImportJob gestartet.");
		URL url;
		try {
			url = URI.create(servicestationenMobiDataImportUrl).toURL();
		} catch (MalformedURLException e) {
			log.error("Von folgender URL kann nicht importiert werden: " + servicestationenMobiDataImportUrl, e);
			return Optional.empty();
		}

		List<SimpleFeature> features;

		try {
			String fileContentAsString = geoJsonImportRepository.getFileContentAsString(url);
			features = geoJsonImportRepository.getSimpleFeatures(fileContentAsString,
				getMobiDataServicestationFeatureType());
		} catch (IOException ex) {
			log.error("Von folgender URL kann nicht importiert werden: " + servicestationenMobiDataImportUrl, ex);
			return Optional.empty();
		} catch (ReadGeoJSONException ex) {
			log.error("GeoJson von folgender URL kann nicht eingelesen werden: " + servicestationenMobiDataImportUrl,
				ex);
			return Optional.empty();
		}

		Set<Long> idsAktuellerServicestationen = new HashSet<>();
		for (SimpleFeature feature : features) {

			// Name
			Optional<ServicestationName> servicestationNameOptional = getServicestationName(feature);

			if (servicestationNameOptional.isEmpty()) {
				log.warn("Attribut 'name' fehlt. Das feature kann nicht importiert werden: " + feature);
				statistik.anzahlIgnoriert++;
				continue;
			}

			ServicestationName servicestationName = servicestationNameOptional.get();

			// Geometrie
			Point geometrie = extractGeometryAsUtm32Point(feature);

			// Wir nehmen die am nächsten gelegene, vorhandene MobiData Station mit demselben Namen
			// in einem Umkreis von ca. 10m
			Servicestation.ServicestationBuilder builder = getExistingOrNewServicestationBuilder(
				servicestationName, geometrie);

			mapAttributesAndAddToBuilder(builder, feature);

			// Zustaendig
			builder.organisation(getZustaendigeVerwaltungseinheit(feature, geometrie));

			// Prüfung auf Ids, auf die zwei Datensätze gemappt wurden
			Servicestation servicestation = builder.build();
			if (!idsAktuellerServicestationen.contains(servicestation.getId())) {
				if (Objects.isNull(servicestation.getId())) {
					statistik.anzahlNeuErstellt++;
				} else {
					statistik.anzahlGeupdated++;
				}
			} else {
				log.info("Zwei Datensätze mit demselben Namen '" + servicestationName
					+ "' gefunden und derselben bestehenden Servicestation zugeordnet. "
					+ "Nur der erste Datensatz wird zur Aktualisierung herangezogen. Für den zweiten "
					+ "wird eine neue Servicestation erstellt.");
				statistik.anzahlNeuErstellt++;
				servicestation = servicestation.toBuilder().id(null).build();
			}

			servicestationRepository.save(servicestation);
			idsAktuellerServicestationen.add(servicestation.getId());
		}

		statistik.anzahlGeloescht = servicestationRepository.deleteByIdNotInAndQuellSystem(
			idsAktuellerServicestationen, ServicestationenQuellSystem.MOBIDATABW);

		log.info("JobStatistik: " + statistik);
		return Optional.of(statistik);
	}

	@NotNull
	private Optional<ServicestationName> getServicestationName(SimpleFeature feature) {
		Object nameAttribute = feature.getAttribute(Fields.NAME);

		if (Objects.isNull(nameAttribute) || nameAttribute.toString().isBlank()) {
			return Optional.empty();
		}

		String name = nameAttribute.toString();
		return Optional.of(ServicestationName.of(name));
	}

	private static SimpleFeatureType getMobiDataServicestationFeatureType() {
		SimpleFeatureTypeBuilder ftb1 = new SimpleFeatureTypeBuilder();
		ftb1.setName("ServiceStationMobiDataJson");
		// Die Achsen der WGS84-Projektion sind im GeoJson-Standard vertauscht,
		// wir können also nicht KoordinatenReferenzSystem.WGS84.getGeotoolsCRS() nutzen
		ftb1.setCRS(DefaultGeographicCRS.WGS84);
		ftb1.add(Fields.GEOMETRY, Point.class);
		ftb1.nillable(true).add(Fields.SUPPLIER, Object.class);
		ftb1.add(Fields.NAME, String.class);
		ftb1.add(Fields.OPERATOR, String.class);
		ftb1.add(Fields.ADDRESS, Object.class);
		ftb1.add(Fields.SIZE, String.class);
		return ftb1.buildFeatureType();
	}

	private Servicestation.ServicestationBuilder getExistingOrNewServicestationBuilder(
		ServicestationName servicestationName, Point point) {
		Optional<Servicestation> servicestationOptional = servicestationRepository
			.findNearestByNameAndQuellSystemAndPosition(
				servicestationName,
				ServicestationenQuellSystem.MOBIDATABW,
				point,
				MAX_VERSCHIEBUNG_VORHANDENER_STATION);
		return servicestationOptional.map(Servicestation::toBuilder).orElse(
			Servicestation.builder()
				.dokumentListe(new DokumentListe())
				.quellSystem(ServicestationenQuellSystem.MOBIDATABW)
				.name(servicestationName))
			.geometrie(point);
	}

	private void mapAttributesAndAddToBuilder(Servicestation.ServicestationBuilder servicestationBuilder,
		SimpleFeature feature) {
		// Defaults
		servicestationBuilder
			.luftpumpe(Luftpumpe.of(false))
			.kettenwerkzeug(Kettenwerkzeug.of(false))
			.fahrradhalterung(Fahrradhalterung.of(false))
			.werkzeug(Werkzeug.of(false))
			.status(ServicestationStatus.AKTIV)
			.gebuehren(Gebuehren.of(false));

		// Betreiber
		Object supplierAttribute = feature.getAttribute(Fields.SUPPLIER);
		Optional<ServicestationSupplier> supplier = Optional.empty();
		if (Objects.nonNull(supplierAttribute) && !supplierAttribute.toString().equalsIgnoreCase("null")) {
			String supplierJson = supplierAttribute.toString();
			try {
				ServicestationSupplier value = objectMapper.readValue(supplierJson, ServicestationSupplier.class);
				supplier = Optional.ofNullable(value);
			} catch (JsonProcessingException e) {
				log.warn("Konnte Supplier mit JSON '" + supplierJson + "' nicht deserialisieren: " + e);
				supplier = Optional.empty();
			}
		}
		servicestationBuilder.betreiber(Betreiber.of(supplier.map(ServicestationSupplier::getName).orElse("")));

		// Typ
		Optional<ServicestationTyp> servicestationTyp = Optional.empty();
		Object sizeAttribute = feature.getAttribute(Fields.SIZE);
		if (Objects.nonNull(sizeAttribute)) {
			String string = sizeAttribute.toString();
			if (string.equals("Kleine Station")) {
				servicestationTyp = Optional.of(ServicestationTyp.RADSERVICE_PUNKT_KLEIN);
			} else if (string.equals("Große Station")) {
				servicestationTyp = Optional.of(ServicestationTyp.RADSERVICE_PUNKT_GROSS);
			}
		}
		servicestationBuilder.typ(servicestationTyp.orElse(ServicestationTyp.SONSTIGER));
	}

	private Verwaltungseinheit getZustaendigeVerwaltungseinheit(SimpleFeature feature, Point geometrie) {
		Optional<Verwaltungseinheit> verwaltungseinheit = Optional.empty();
		Object operatorAttribute = feature.getAttribute(Fields.OPERATOR);

		if (Objects.nonNull(operatorAttribute)) {
			String operatorString = operatorAttribute.toString().trim();

			String[] operatorStringParts = operatorString.split(" ");

			List<Verwaltungseinheit> passendeVerwaltungseinheiten = Collections.emptyList();

			// Kreis
			if (Arrays.stream(operatorStringParts).anyMatch(ServicestationMobiDataImportJob::isKreisIndicator)) {
				String operatorName = Arrays.stream(operatorStringParts)
					.filter(part -> !isKreisIndicator(part))
					.collect(Collectors.joining(" "));
				if (!operatorName.isBlank()) {
					passendeVerwaltungseinheiten = verwaltungseinheitRepository
						.findAllByNameContainingAndOrganisationsArt(
							operatorName,
							OrganisationsArt.KREIS);
				}
				// Gemeinde
			} else if (Arrays.stream(operatorStringParts)
				.anyMatch(ServicestationMobiDataImportJob::isGemeindeIndicator)) {
				String operatorName = Arrays.stream(operatorStringParts)
					.filter(part -> !isGemeindeIndicator(part))
					.collect(Collectors.joining(" "));
				if (!operatorName.isBlank()) {
					passendeVerwaltungseinheiten = verwaltungseinheitRepository
						.findAllByNameContainingAndOrganisationsArt(
							operatorName,
							OrganisationsArt.GEMEINDE);
				}
				// 1. operatorString ist Exakter Name , 2. Name enthält den operatorString, 3.operatorString mit
				// Bindestrich -> Name entspricht dem Teil VOR dem Bindestrich
			} else if (!operatorString.isBlank()) {
				passendeVerwaltungseinheiten = verwaltungseinheitRepository.findAllByName(operatorString);

				if (passendeVerwaltungseinheiten.isEmpty()) {
					passendeVerwaltungseinheiten = verwaltungseinheitRepository.findAllByNameContaining(
						operatorString);
				}

				if (passendeVerwaltungseinheiten.isEmpty() && operatorString.contains("-")) {
					passendeVerwaltungseinheiten = verwaltungseinheitRepository.findAllByName(
						operatorString.split("-")[0]);
				}
			}
			// Bei mehreren passenden Treffern nehmen wir die mit dem geringsten Abstand gemäß ogc sfs.
			// (Also eine VE, die eine Grenze mit den geringsten minimalen(!) Abstand zum Punkt hat).
			// Da die Grenzen von Kreisen/Gemeinden teilw. denkungsgleich sind
			// (Landkreisgrenze ist auch immer Gemeindegrenze), kann es durchaus vorkommen, dass dies nicht
			// eindeutig ist!
			verwaltungseinheit = findNearestVerwaltungseinheit(geometrie, passendeVerwaltungseinheiten);
			log.debug(
				"Operator '" + operatorString + "' wurde auf Verwaltungseinheit " + verwaltungseinheit.orElse(null)
					+ " gemappt");
		} else {
			log.info("Operator nicht gesetzt. Setze Zuständigkeit auf Unbekannt");
		}

		// Fallback Unbekannt
		return verwaltungseinheit.orElse(verwaltungseinheitService.getUnbekannteOrganisation());
	}

	@NotNull
	private static Optional<Verwaltungseinheit> findNearestVerwaltungseinheit(Point geometrie,
		List<Verwaltungseinheit> verwaltungseinheiten) {
		return verwaltungseinheiten.stream().min(
			Comparator.comparing(v -> v.getBereich()
				.map(bereich -> bereich.distance(geometrie))
				.orElse(Double.MAX_VALUE)));
	}

	private static boolean isGemeindeIndicator(String str) {
		List<String> gemeindeStrings = List.of("gemeinde", "stadt");
		return gemeindeStrings.stream()
			.anyMatch(gemeindeString -> str.replace("(", "").replace(")", "").trim().equalsIgnoreCase(gemeindeString));
	}

	private static boolean isKreisIndicator(String str) {
		List<String> kreisStrings = List.of("lk", "Kreis", "Landkreis", "Stadtkreis");
		return kreisStrings.stream()
			.anyMatch(kreisString -> str.replace("(", "").replace(")", "").trim().equalsIgnoreCase(kreisString));
	}

	private Point extractGeometryAsUtm32Point(SimpleFeature feature) {
		Point pointWGS84 = (Point) feature.getDefaultGeometry();
		return (Point) CoordinateReferenceSystemConverterUtility.transformGeometry(pointWGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
	}
}
