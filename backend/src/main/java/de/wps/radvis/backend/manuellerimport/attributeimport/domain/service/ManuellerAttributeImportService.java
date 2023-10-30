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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.scheduling.annotation.Async;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.exception.ZipFileRequiredFilesMissingException;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportSession;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.repository.ShapeFileAttributeRepository;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.ImportierbaresAttribut;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AutomatischerImportSchritt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionStatus;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManuellerAttributeImportService {

	private final ManuellerImportService manuellerImportService;
	private final ManuellerAttributeImportAbbildungsService manuellerAttributeImportAbbildungsService;
	private final ShapeZipService zipService;
	private final ShapeFileAttributeRepository shapeFileAttributeRepository;
	private final AttributMapperFactory attributMapperFactory;
	private final ShapeFileRepository shapeFileRepository;
	private final KantenRepository kantenRepository;
	private final ManuellerImportFehlerRepository manuellerImportFehlerRepository;
	private final ManuellerAttributeImportUebernahmeService manuellerAttributeImportUebernahmeService;

	public ManuellerAttributeImportService(ManuellerImportService manuellerImportService,
		ManuellerAttributeImportAbbildungsService manuellerAttributeImportAbbildungsService,
		ManuellerAttributeImportUebernahmeService manuellerAttributeImportUebernahmeService,
		ShapeZipService zipService, ShapeFileRepository shapeFileRepository,
		ShapeFileAttributeRepository shapeFileAttributeRepository, AttributMapperFactory attributMapperFactory,
		KantenRepository kantenRepository, ManuellerImportFehlerRepository manuellerImportFehlerRepository) {
		this.shapeFileRepository = shapeFileRepository;
		this.shapeFileAttributeRepository = shapeFileAttributeRepository;
		this.zipService = zipService;
		this.manuellerImportService = manuellerImportService;
		this.manuellerAttributeImportAbbildungsService = manuellerAttributeImportAbbildungsService;
		this.manuellerAttributeImportUebernahmeService = manuellerAttributeImportUebernahmeService;
		this.attributMapperFactory = attributMapperFactory;
		this.kantenRepository = kantenRepository;
		this.manuellerImportFehlerRepository = manuellerImportFehlerRepository;
	}

	public AttributeImportSession getAttributeImportSession(Benutzer benutzer) {
		Optional<AbstractImportSession> importSession = manuellerImportService.findImportSessionFromBenutzer(benutzer);

		if (importSession.isEmpty() || !(importSession.get() instanceof AttributeImportSession)) {
			throw new RuntimeException("Keine AttributeImportSession für den Benutzer verfügbar");
		}

		return (AttributeImportSession) importSession.get();
	}

	@Async
	public void runAutomatischeAbbildung(AttributeImportSession session, File shpDirectory) {
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_RUNNING);

		try (Stream<SimpleFeature> features = this.shapeFileRepository
			.readShape(zipService.getShapeFileFromDirectory(shpDirectory).orElseThrow(
				() -> new RuntimeException("Directory " + shpDirectory.getName() + " enthält keine .shp Datei")))) {

			AtomicInteger numberOfNullGeometries = new AtomicInteger();
			List<SimpleFeature> featuresInBereich = features
				.filter(simpleFeature -> {
					if (simpleFeature.getDefaultGeometry() == null) {
						numberOfNullGeometries.getAndIncrement();
						return false;
					} else {
						return true;
					}
				})
				.filter(feature -> session.getOrganisation().getBereich()
					.map(bereich -> bereich.intersects(CoordinateReferenceSystemConverterUtility.transformGeometry(
						(Geometry) feature.getDefaultGeometry(),
						KoordinatenReferenzSystem.ETRS89_UTM32_N)))
					.orElse(false)
				).collect(Collectors.toList());

			if (numberOfNullGeometries.get() > 0) {
				session.addLogEintrag(
					ImportLogEintrag.ofWarnung(
						"Shapefile enthält " + numberOfNullGeometries.get()
							+ " Features ohne Geometrien, welche aus diesem Grund nicht importiert werden konnten."));
			}

			if (featuresInBereich.isEmpty()) {
				session.addLogEintrag(
					ImportLogEintrag.ofWarnung("Shapefile enthält keine Features im Bereich der Organisation "
						+ session.getOrganisation().getName()));
			}

			session.setAktuellerImportSchritt(AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ);

			log.info("Es wurden {} Features importiert", featuresInBereich.size());

			List<FeatureMapping> featureMappings = this.manuellerAttributeImportAbbildungsService
				.bildeFeaturesAb(featuresInBereich, session);

			log.info("Es wurden {} FeatureMappings erstellt", featureMappings.size());

			session.setFeatureMappings(featureMappings);
			session.setAktuellerImportSchritt(AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN);
		} catch (ShapeProjectionException e) {
			session.addLogEintrag(ImportLogEintrag.ofError(e.getMessage()));
			log.error("Fehler bei ManuellerAttributImport", e);
		} catch (Throwable e) {
			session.addLogEintrag(ImportLogEintrag.ofError("Es ist ein unerwarteter Fehler aufgetreten."));
			log.error("Fehler bei ManuellerAttributImport", e);
		} finally {
			if (shpDirectory != null) {
				zipService.deleteUploadedFiles(shpDirectory);
			}
			session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		}
	}

	@Async
	@WithAuditing(context = AuditingContext.MANUELLER_ATTRIBUTE_IMPORT)
	@Transactional
	public void runUpdate(AttributeImportSession session) {
		require(session.getStatus() == ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
			"Eine Session darf nur nach der automatischen Abbildung und nur einmal ausgeführt werden");
		session.setStatus(ImportSessionStatus.UPDATE_EXECUTING);
		LocalDateTime importZeitpunkt = LocalDateTime.now();

		AttributeImportKonfliktProtokoll attributeImportKonfliktProtokoll = new AttributeImportKonfliktProtokoll();
		session.setAttributeImportKonfliktProtokoll(attributeImportKonfliktProtokoll);
		try {
			this.manuellerAttributeImportUebernahmeService.attributeUebernehmen(session.getAttribute(),
				session.getOrganisation(), session.getFeatureMappings(),
				this.attributMapperFactory.createMapper(session.getAttributeImportFormat()),
				attributeImportKonfliktProtokoll);

			manuellerImportFehlerRepository.saveAll(
				session.getFeatureMappings().stream()
					.filter(fm -> fm.getKantenAufDieGemappedWurde().isEmpty())
					.map(fm -> new ManuellerImportFehler(
						CoordinateReferenceSystemConverterUtility.transformGeometry(
							fm.getImportedLineString(),
							KoordinatenReferenzSystem.ETRS89_UTM32_N)
						, ImportTyp.ATTRIBUTE_UEBERNEHMEN, importZeitpunkt,
						session.getBenutzer(),
						session.getOrganisation()))
					.collect(Collectors.toList()));
			manuellerImportFehlerRepository.saveAll(
				attributeImportKonfliktProtokoll.getKantenKonfliktProtokolle().stream()
					.filter(kKP -> kantenRepository.findById(kKP.getKanteId()).isPresent())
					.map(kantenKonfliktProtokoll -> kantenRepository.findById(
						kantenKonfliktProtokoll.getKanteId()).map(
						k -> new ManuellerImportFehler(k, importZeitpunkt, session.getBenutzer(),
							session.getOrganisation(),
							kantenKonfliktProtokoll.getKonflikte())))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.collect(Collectors.toList()));

			session.setStatus(ImportSessionStatus.UPDATE_DONE);
		} catch (OptimisticLockException e) {
			session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
			throw new OptimisticLockException(
				"Kanten in der Organisation wurden während des Speicherns aus einer anderen Quelle verändert."
					+ " Bitte versuchen Sie es erneut.");
		} catch (Throwable e) {
			session.addLogEintrag(
				ImportLogEintrag.ofError("Es ist ein unerwarteter Fehler aufgetreten."));
			session.setStatus(ImportSessionStatus.UPDATE_DONE);
			throw e;
		}
	}

	public List<ImportierbaresAttribut> validateAttribute(byte[] shpZip, AttributeImportFormat attributeImportFormat)
		throws IOException, ZipFileRequiredFilesMissingException {
		AttributeMapper mapper = attributMapperFactory.createMapper(attributeImportFormat);
		File shpDirectory = zipService.unzip(shpZip);
		Optional<File> shapeFileFromDirectory = zipService.getShapeFileFromDirectory(shpDirectory);
		if (shapeFileFromDirectory.isEmpty()) {
			return new ArrayList<>();
		}

		try {
			List<ImportierbaresAttribut> result = shapeFileAttributeRepository
				.getAttributnamen(shapeFileFromDirectory.get()).stream()
				.filter(mapper::isAttributNameValid).map(attrName -> {
					Stream<String> attributWerte = Stream.empty();
					try {
						attributWerte = shapeFileAttributeRepository
							.getAttributWerte(shapeFileFromDirectory.get(), attrName);

						Set<String> ungueltigeAttributWerte = attributWerte.filter(
								attrWert -> !mapper.isAttributWertValid(attrName, attrWert))
							.collect(Collectors.toSet());

						boolean areWerteValid = ungueltigeAttributWerte.isEmpty();

						// Hier zunächst alle Attribute einzeln, Gruppierung erfolgt im collect, deshalb nur
						// attributName & isValid interessant
						return ImportierbaresAttribut.of(attrName, mapper.getRadVisAttributName(attrName), attrName,
							areWerteValid, ungueltigeAttributWerte);
					} catch (IOException e) {
						throw new RuntimeException("Shape-File konnte nicht gelesen werden");
					} finally {
						attributWerte.close();
					}
				}).collect(Collectors.groupingBy(attr -> mapper.getImportGruppe(attr.getAttributName()),
					// Für alle ImportierbarenAttribute einer Gruppe, wollen wir die Namen konkatenieren und die Validität ver"und"en.
					// Am Ende haben wir nur ein Importierbares Attribut für jede Gruppe und holen uns nur für das "Hauptattribut" der ImportGruppe den Namen
					// (z.B.: "BREITST, ST, BREITST2" (displayName) -> "Sicherheitstrennstreifeninformationen" (RadVIS-Name).
					Collectors.reducing((next, curr) -> {
						String importGruppe = mapper.getImportGruppe(next.getAttributName());
						return ImportierbaresAttribut.of(
							importGruppe,
							mapper.getRadVisAttributName(importGruppe),
							next.getAttributDisplayName() + ", " + curr.getAttributDisplayName(),
							next.isValid() && curr.isValid(),
							Stream.concat(next.getUngueltigeWerte().stream(), curr.getUngueltigeWerte().stream())
								.collect(Collectors.toSet()));
					}))).values().stream().filter(Optional::isPresent).map(Optional::get).toList();

			return result;
		} finally {
			zipService.deleteUploadedFiles(shpDirectory);
		}
	}

	public FeatureMapping updateFeatureMapping(AttributeImportSession session, Long featuremappingID,
		LineString updatedLinestring) {
		require(session.getFeatureMappings().stream().anyMatch(mapping -> mapping.getId() == featuremappingID));

		FeatureMapping featureMapping = session.getFeatureMappings().stream()
			.filter(mapping -> mapping.getId() == featuremappingID).findFirst().get();

		featureMapping.updateLinestringAndResetMapping(updatedLinestring);

		manuellerAttributeImportAbbildungsService.rematchFeaturemapping(featureMapping, session.getOrganisation());

		return featureMapping;
	}
}
