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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Severity;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.exception.MassnahmenAttributWertValidierungsException;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.exception.VerwaltungseinheitNichtGefundenException;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehler;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehlermeldung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportAttribute;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweisText;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.BegruendungStornierungsanfrage;
import de.wps.radvis.backend.massnahme.domain.valueObject.BegruendungZurueckstellung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Kostenannahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.Realisierungshilfe;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.VerbaID;
import de.wps.radvis.backend.massnahme.domain.valueObject.ZurueckstellungsGrund;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.OrganisationsartUndNameNichtEindeutigException;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManuellerMassnahmenImportService {

	public static final String MASSNAHME_ID_ATTRIBUTENAME = "Massnahme-ID";
	public static final String GELOESCHT_ATTRIBUTENAME = "geloescht";
	private final ManuellerImportService manuellerImportService;
	private final MassnahmeNetzbezugService massnahmeNetzbezugService;
	private final GeoJsonImportRepository geoJsonImportRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final MassnahmeRepository massnahmenRepostory;
	private final EntityManager entityManager;
	private final CsvRepository csvRepository;
	private final double minimaleDistanzFuerAbweichungsWarnung;

	public ManuellerMassnahmenImportService(ManuellerImportService manuellerImportService,
		MassnahmeNetzbezugService massnahmeNetzbezugService,
		GeoJsonImportRepository geoJsonImportRepository,
		VerwaltungseinheitService verwaltungseinheitService,
		MassnahmeRepository massnahmenRepostory,
		EntityManager entityManager,
		CsvRepository csvRepository,
		double minimaleDistanzFuerAbweichungsWarnung) {
		this.manuellerImportService = manuellerImportService;
		this.massnahmeNetzbezugService = massnahmeNetzbezugService;
		this.geoJsonImportRepository = geoJsonImportRepository;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.massnahmenRepostory = massnahmenRepostory;
		this.entityManager = entityManager;
		this.csvRepository = csvRepository;
		this.minimaleDistanzFuerAbweichungsWarnung = minimaleDistanzFuerAbweichungsWarnung;
	}

	public Optional<MassnahmenImportSession> getMassnahmenImportSession(Benutzer benutzer) {
		return manuellerImportService.findImportSessionFromBenutzer(benutzer, MassnahmenImportSession.class);
	}

	@Async
	@Transactional
	public void ladeFeatures(MassnahmenImportSession session, byte[] file) {
		require(
			session.getSchritt().equals(MassnahmenImportSession.DATEI_HOCHLADEN)
				&& !session.isExecuting()
				&& !session.hatFehler(),
			"Das Zuordnen der Features kann nur in Schritt 1 und nur einmal passieren.");
		log.info("Maßnahmenimport für Benutzer {}: Starte Zuordnen der Features", session.getBenutzer().getId());
		session.setExecuting(true);

		// 1.Pass:
		// - Einlesen & Validierung GeoJSON
		// - doppelte Ids in der Quelle ermitteln
		// - Quelldaten auf Bereich filtern
		Set<String> doppelteIds;
		List<SimpleFeature> featuresInBereich = new ArrayList<>();
		final AtomicLong anzahlFeaturesInsgesamt = new AtomicLong();
		try (Stream<SimpleFeature> featureStream = this.geoJsonImportRepository.readFeaturesFromByteArray(
			file)) {
			doppelteIds = featureStream
				.peek(f -> anzahlFeaturesInsgesamt.incrementAndGet())
				// Features auf den Bereich filtern
				.filter(
					simpleFeature -> ((Geometry) simpleFeature.getDefaultGeometry()).intersects(session.getBereich()))
				// Für die spätere Zuordnung aufsammeln (damit wir die File nicht zweimal einlesen müssen)
				.peek(featuresInBereich::add)
				// auf id mappen (null-safe)
				.map(simpleFeature -> {
					Object attribute = simpleFeature.getAttribute(MASSNAHME_ID_ATTRIBUTENAME);
					return Objects.isNull(attribute) ? null : attribute.toString();
				})
				// null und blank ids sind gesonderter Fehler (fehlende ID) und keine Duplikate
				.filter(Objects::nonNull)
				.filter(s -> !s.isBlank())
				// Duplikate filtern:
				// Alle IDs in einer Map mit der Frequency aufsammeln
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
				.entrySet()
				.stream()
				// Duplikate herausfiltern
				.filter(e -> e.getValue() > 1)
				// Häufigkeit verwerfen, nur ID aufsammeln
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		} catch (ReadGeoJSONException e) {
			session.addLogEintrag(ImportLogEintrag.ofError(e.getMessage()));
			log.warn(
				"Maßnahmenimport für Benutzer {}: Fehler beim Einlesen der GeoJSON",
				session.getBenutzer().getId(),
				e);
			session.setExecuting(false);
			return;
		} catch (Exception e) {
			session.addLogEintrag(
				ImportLogEintrag.ofError("Es ist ein unbekannter Fehler beim Einlesen der GeoJSON aufgetreten."));
			log.error(
				"Maßnahmenimport für Benutzer {}: Unbekannter Fehler beim Einlesen der GeoJSON",
				session.getBenutzer().getId(),
				e);
			session.setExecuting(false);
			return;
		}

		// 2. Pass:
		// - grundlegende Validierung der Features (Massnahmen-ID vorhanden & nicht doppelt)
		// - Massnahmen zuordnen
		// - Zuordnung prüfen
		// - ggf. Geometrie prüfen
		try {
			// Zuordnung ermitteln (2. pass)
			List<MassnahmenImportZuordnung> importZuordnungen = featuresInBereich
				.stream()
				.map(simpleFeature -> {
					MassnahmenImportZuordnung zuordnung = getMassnahmenZuordnung(
						session, simpleFeature,
						doppelteIds);

					if (!zuordnung.hasNetzbezugHinweisFehler() && !zuordnung.hasMappingFehler()) {
						zuordnung.select();
					}

					return zuordnung;
				})
				.toList();

			session.setZuordnungen(importZuordnungen);
			session.setSchritt(MassnahmenImportSession.ATTRIBUTE_AUSWAEHLEN);
			logSummary(session, anzahlFeaturesInsgesamt.get(), featuresInBereich.size());
		} catch (Exception e) {
			session.addLogEintrag(
				ImportLogEintrag.ofError("Es ist ein unbekannter Fehler bei der Verarbeitung aufgetreten."));
			log.error(
				"Maßnahmenimport für Benutzer {}: Unbekannter Fehler beim Erstellen der Zuordnungen",
				session.getBenutzer().getId(),
				e);
		} finally {
			session.setExecuting(false);
		}
	}

	private static void logSummary(MassnahmenImportSession session, long anzahlFeaturesInsgesamt,
		long anzahlFeaturesInBereich) {
		logSummary(session);
		log.info("Features in GeoJSON Insgesamt: {}", anzahlFeaturesInsgesamt);
		log.info("Features in Bereich: {}", anzahlFeaturesInBereich);
	}

	public static void logSummary(MassnahmenImportSession session) {
		log.info(
			"Maßnahmenimport für Benutzer {}: Fehler an der Session (pro Status inkl. Häufigkeit)",
			session.getBenutzer().getId());

		List<MassnahmenImportZuordnung> importZuordnungen = session.getZuordnungen();
		Arrays.stream(MassnahmenImportZuordnungStatus.values()).forEach(status -> {
			log.info(
				"Zuordnungen {}: {}", status.name(),
				importZuordnungen.stream().filter(z -> z.getZuordnungStatus().equals(status))
					.count());
			Map<MappingFehler, Long> mappingFehlerFrequencyMap = importZuordnungen.stream()
				.filter(z -> z.getZuordnungStatus().equals(status))
				.flatMap(z -> z.getMappingFehler().stream())
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			try {
				log.info(
					"  => Fehler: {}",
					new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
						mappingFehlerFrequencyMap));
			} catch (JsonProcessingException e) {
				log.info(
					"  => Fehler: {}",
					mappingFehlerFrequencyMap);
			}
		});
	}

	private MassnahmenImportZuordnung getMassnahmenZuordnung(MassnahmenImportSession session,
		SimpleFeature simpleFeature, Set<String> doppelteIds) {
		Geometry zuImportierendeGeometrie = (Geometry) simpleFeature.getDefaultGeometry();

		Object massnahmenKonzeptIDAttribute = simpleFeature.getAttribute(MASSNAHME_ID_ATTRIBUTENAME);
		// Keine Id am Feature -> Fehler
		boolean keineKonzeptIdGefunden = Objects.isNull(massnahmenKonzeptIDAttribute)
			|| massnahmenKonzeptIDAttribute.toString().isBlank();
		boolean konzeptIdInvalid = massnahmenKonzeptIDAttribute != null
			&& !MassnahmeKonzeptID.isValid(massnahmenKonzeptIDAttribute.toString());
		if (keineKonzeptIdGefunden || konzeptIdInvalid) {
			MassnahmenImportZuordnung massnahmenImportZuordnung = new MassnahmenImportZuordnung(
				null,
				simpleFeature,
				null,
				MassnahmenImportZuordnungStatus.FEHLERHAFT);
			massnahmenImportZuordnung.addMappingFehler(
				MappingFehler.of(
					MASSNAHME_ID_ATTRIBUTENAME,
					keineKonzeptIdGefunden ? MappingFehlermeldung.MASSNAHME_KEINE_ID.getText()
						: MappingFehlermeldung.MASSNAHME_ID_INVALID.getText(massnahmenKonzeptIDAttribute.toString())));
			return massnahmenImportZuordnung;
		}

		MassnahmeKonzeptID massnahmeKonzeptID = MassnahmeKonzeptID.of(massnahmenKonzeptIDAttribute.toString());

		// Massnahmen-Id in der Quelle doppelt vorhanden
		if (doppelteIds.contains(massnahmenKonzeptIDAttribute.toString())) {
			MassnahmenImportZuordnung massnahmenImportZuordnung = new MassnahmenImportZuordnung(
				massnahmeKonzeptID,
				simpleFeature,
				null,
				MassnahmenImportZuordnungStatus.FEHLERHAFT);
			massnahmenImportZuordnung.addMappingFehler(
				MappingFehler.of(
					MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_MEHRFACH.getText()));
			return massnahmenImportZuordnung;
		}

		// MASSNAHMEN ZUORDNEN - Mapping anhand der Massnahmen-ID
		List<Massnahme> massnahmen = getPassendeMassnahmen(session, massnahmeKonzeptID);

		// zugeordnete Massnahmen auf den Bereich filtern
		massnahmen = massnahmen.stream()
			.filter(massnahme -> massnahme.getNetzbezug().getGeometrie().intersects(session.getBereich()))
			.toList();

		// PRÜFUNG AUF LÖSCHUNG
		boolean geloescht = isFlaggedAsGeloescht(simpleFeature);

		// ZUORDNUNG PRÜFEN
		if (massnahmen.size() > 1) {
			// Zuordnung nicht eindeutig
			MassnahmenImportZuordnung massnahmenImportZuordnung = new MassnahmenImportZuordnung(
				massnahmeKonzeptID,
				simpleFeature,
				null,
				geloescht ? MassnahmenImportZuordnungStatus.GELOESCHT : MassnahmenImportZuordnungStatus.ZUGEORDNET);
			massnahmenImportZuordnung.addMappingFehler(
				MappingFehler.of(
					MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_NICHT_EINDEUTIG.getText(massnahmen.size())));
			return massnahmenImportZuordnung;
		} else if (massnahmen.isEmpty()) {
			// Keine Zuordnung
			if (geloescht) {
				// Löschen nicht möglich -> Fehler
				MassnahmenImportZuordnung massnahmenImportZuordnung = new MassnahmenImportZuordnung(
					massnahmeKonzeptID,
					simpleFeature,
					null,
					MassnahmenImportZuordnungStatus.GELOESCHT);

				massnahmenImportZuordnung.addMappingFehler(
					MappingFehler.of(
						MASSNAHME_ID_ATTRIBUTENAME,
						MappingFehlermeldung.MASSNAHME_NICHT_GEFUNDEN.getText()));
				return massnahmenImportZuordnung;
			} else {
				// Neue Massnahme
				MassnahmenImportZuordnung massnahmenImportZuordnungNeu = new MassnahmenImportZuordnung(
					massnahmeKonzeptID,
					simpleFeature,
					null,
					MassnahmenImportZuordnungStatus.NEU);

				// PRÜFUNG AUF KORREKTEN GEOMTYPE
				pruefeGeometrieTyp(zuImportierendeGeometrie, massnahmenImportZuordnungNeu);

				return massnahmenImportZuordnungNeu;
			}
		} else {
			// Löschversuch Oder Update
			Massnahme massnahme = massnahmen.get(0);
			if (geloescht) {
				// NICHT LÖSCHEN WENN ES EINE RADNETZ MASSNAHME IST
				if (Konzeptionsquelle.isRadNetzKonzeptionsquelle(massnahme.getKonzeptionsquelle())) {
					MassnahmenImportZuordnung massnahmenImportZuordnung = new MassnahmenImportZuordnung(
						massnahmeKonzeptID,
						simpleFeature,
						null,
						MassnahmenImportZuordnungStatus.FEHLERHAFT);
					massnahmenImportZuordnung.addMappingFehler(
						MappingFehler.of(
							MASSNAHME_ID_ATTRIBUTENAME,
							MappingFehlermeldung.LOESCHUNG_QUELLE_RADNETZ_UNGUELTIG.getText()));
					return massnahmenImportZuordnung;
				} else {
					// LÖSCHEN
					return new MassnahmenImportZuordnung(
						massnahmeKonzeptID,
						simpleFeature,
						massnahme,
						MassnahmenImportZuordnungStatus.GELOESCHT);
				}
			} else {
				// UPDATE
				if (massnahme.isArchiviert()) {
					MassnahmenImportZuordnung massnahmenImportZuordnung = new MassnahmenImportZuordnung(
						massnahmeKonzeptID,
						simpleFeature,
						null,
						MassnahmenImportZuordnungStatus.FEHLERHAFT);
					massnahmenImportZuordnung.addMappingFehler(
						MappingFehler.of(
							MASSNAHME_ID_ATTRIBUTENAME,
							MappingFehlermeldung.UPDATE_ARCHIVIERT_NICHT_MOEGLICH.getText()));
					return massnahmenImportZuordnung;
				} else {
					MassnahmenImportZuordnung massnahmenImportZuordnungGemappt = new MassnahmenImportZuordnung(
						massnahmeKonzeptID,
						simpleFeature,
						massnahme,
						MassnahmenImportZuordnungStatus.ZUGEORDNET);

					// PRÜFUNG AUF KORREKTEN GEOMTYPE
					pruefeGeometrieTyp(zuImportierendeGeometrie, massnahmenImportZuordnungGemappt);

					// PRÜFUNG AUF ABWEICHENDE GEOMETRIEN
					pruefeAbweichendeGeometrien(zuImportierendeGeometrie, massnahme, massnahmenImportZuordnungGemappt);

					return massnahmenImportZuordnungGemappt;
				}
			}
		}
	}

	private void pruefeGeometrieTyp(Geometry zuImportierendeGeometrie,
		MassnahmenImportZuordnung massnahmenImportZuordnung) {
		if (!isGeometrieTypZulaessig(zuImportierendeGeometrie)) {
			massnahmenImportZuordnung.addNetzbezugHinweis(
				NetzbezugHinweis.ofError(NetzbezugHinweisText.FALSCHER_GEOMETRIE_TYP));
		}
	}

	private boolean isGeometrieTypZulaessig(Geometry zuImportierendeGeometrie) {
		Set<String> allowedBaseGeometryTypes = Set.of(
			Geometry.TYPENAME_MULTILINESTRING,
			Geometry.TYPENAME_MULTIPOINT, Geometry.TYPENAME_LINESTRING, Geometry.TYPENAME_POINT);

		if (zuImportierendeGeometrie.getGeometryType().equals(Geometry.TYPENAME_GEOMETRYCOLLECTION)) {
			GeometryCollection geometryCollection = (GeometryCollection) zuImportierendeGeometrie;

			for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
				Geometry geometry = geometryCollection.getGeometryN(i);
				if (!allowedBaseGeometryTypes.contains(geometry.getGeometryType())) {
					return false;
				}
			}
			return true;
		} else {
			return allowedBaseGeometryTypes.contains(zuImportierendeGeometrie.getGeometryType());
		}
	}

	private boolean isFlaggedAsGeloescht(SimpleFeature simpleFeature) {
		Object geloeschtAttribute = simpleFeature.getAttribute(GELOESCHT_ATTRIBUTENAME);
		return Objects.nonNull(geloeschtAttribute) && geloeschtAttribute.toString().equalsIgnoreCase("ja");
	}

	private void pruefeAbweichendeGeometrien(Geometry zuImportierendeGeometrie, Massnahme massnahme,
		MassnahmenImportZuordnung massnahmenImportZuordnungGemappt) {
		GeometryCollection alteGeometrie = massnahme.getNetzbezug().getGeometrie();
		if (alteGeometrie.distance(zuImportierendeGeometrie) > this.minimaleDistanzFuerAbweichungsWarnung) {
			massnahmenImportZuordnungGemappt.addNetzbezugHinweis(
				NetzbezugHinweis.ofWarnung(NetzbezugHinweisText.GEOMETRIEN_ABWEICHEND));
		}
	}

	private List<Massnahme> getPassendeMassnahmen(MassnahmenImportSession session,
		MassnahmeKonzeptID massnahmeKonzeptID) {
		return session.getSollStandard().isPresent() ? massnahmenRepostory
			.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndSollStandardAndGeloeschtFalse(
				massnahmeKonzeptID,
				session.getKonzeptionsquelle(), session.getSollStandard().get())
			: massnahmenRepostory
				.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
					massnahmeKonzeptID,
					session.getKonzeptionsquelle());
	}

	public MassnahmenImportSession createSession(Benutzer benutzer, List<Long> gebietskoerperschaftenIds,
		Konzeptionsquelle konzeptionsquelle, SollStandard sollStandard) {
		MultiPolygon vereinigterBereich = verwaltungseinheitService.getVereintenBereich(gebietskoerperschaftenIds);
		String bereichName = verwaltungseinheitService.getAllNames(gebietskoerperschaftenIds);

		return new MassnahmenImportSession(benutzer, vereinigterBereich, bereichName, gebietskoerperschaftenIds,
			konzeptionsquelle, sollStandard);
	}

	@Async
	@Transactional
	public void attributeValidieren(MassnahmenImportSession session, List<MassnahmenImportAttribute> attribute) {
		require(
			session.getSchritt().equals(MassnahmenImportSession.ATTRIBUTE_AUSWAEHLEN)
				&& !session.isExecuting()
				&& !session.hatFehler(),
			"Das Validieren der Attribute kann nur in Schritt 2 und nur einmal passieren.");
		log.info("Maßnahmenimport für Benutzer {}: Starte Validieren der Attribute", session.getBenutzer().getId());
		session.setExecuting(true);

		try {
			session.setAttribute(attribute);
			session.getZuordnungen().stream()
				// Bei fehlerhaften und löschenden Zuordnungen müssen die Attribute nicht validiert werden
				.filter(
					zuordnung -> zuordnung.getZuordnungStatus() == MassnahmenImportZuordnungStatus.NEU
						|| zuordnung.getZuordnungStatus() == MassnahmenImportZuordnungStatus.ZUGEORDNET
							&& zuordnung.getMassnahme().isPresent())
				.forEach(zuordnung -> validiereAttributeDerZuordnung(session.getAttribute(), zuordnung,
					session.getKonzeptionsquelle()));
			logSummary(session);
			session.setSchritt(MassnahmenImportSession.ATTRIBUTFEHLER_UEBERPRUEFEN);
		} catch (Exception e) {
			session.addLogEintrag(
				ImportLogEintrag.ofError(
					"Es ist ein unbekannter Fehler bei der Validierung der Attribute aufgetreten."));
			log.error(
				"Maßnahmenimport für Benutzer {}: Unbekannter Fehler beim Validieren der Attribute",
				session.getBenutzer().getId(),
				e);
		} finally {
			session.setExecuting(false);
		}
	}

	private void validiereAttributeDerZuordnung(List<MassnahmenImportAttribute> attribute,
		MassnahmenImportZuordnung zuordnung, Konzeptionsquelle konzeptionsquelle) {
		require(
			zuordnung.getZuordnungStatus() == MassnahmenImportZuordnungStatus.NEU
				|| zuordnung.getZuordnungStatus() == MassnahmenImportZuordnungStatus.ZUGEORDNET
					&& zuordnung.getMassnahme().isPresent());

		// Attribute für Quervalidierung
		Optional<Umsetzungsstatus> umsetzungsstatus = Optional.empty();
		Set<Massnahmenkategorie> massnahmenkategorien = Collections.emptySet();
		Optional<Durchfuehrungszeitraum> durchfuehrungszeitraum = Optional.empty();
		Optional<Verwaltungseinheit> baulastZustaendiger = Optional.empty();
		Optional<Handlungsverantwortlicher> handlungsverantwortlicher = Optional.empty();
		Optional<ZurueckstellungsGrund> zurueckstellungsgrund = Optional.empty();
		Optional<BegruendungStornierungsanfrage> begruendungStornierungsanfrage = Optional.empty();
		Optional<BegruendungZurueckstellung> begruendungZurueckstellung = Optional.empty();

		if (zuordnung.getZuordnungStatus() == MassnahmenImportZuordnungStatus.ZUGEORDNET) {
			// Attribute bestehender Maßnahmen sollen unabhängig von den ausgewählten Attributen
			// für die Quervalidierung genutzt werden können
			Massnahme massnahme = entityManager.merge(zuordnung.getMassnahme().get());
			umsetzungsstatus = Optional.of(massnahme.getUmsetzungsstatus());
			massnahmenkategorien = massnahme.getMassnahmenkategorien();
			durchfuehrungszeitraum = massnahme.getDurchfuehrungszeitraum();
			baulastZustaendiger = massnahme.getBaulastZustaendiger();
			handlungsverantwortlicher = massnahme.getHandlungsverantwortlicher();
			zurueckstellungsgrund = massnahme.getZurueckstellungsGrund();
			begruendungStornierungsanfrage = massnahme.getBegruendungStornierungsanfrage();
			begruendungZurueckstellung = massnahme.getBegruendungZurueckstellung();
		} else {
			// Für neu angelegte Maßnahmen müssen alle Pflichtattribute auswählt sein.
			List<MassnahmenImportAttribute> nichtAusgewaehltePflichtattribute = MassnahmenImportAttribute
				.getPflichtAttribute()
				.stream()
				.filter(pflichtAttribut -> !attribute.contains(pflichtAttribut))
				.toList();

			nichtAusgewaehltePflichtattribute.forEach(
				pflichtAttribut -> zuordnung.addMappingFehler(
					MappingFehler.of(
						pflichtAttribut.toString(),
						MappingFehlermeldung.PFLICHTATTRIBUT_NICHT_AUSGEWAEHLT.getText())));
		}

		// Validiere Attribute einzeln
		// Attribute für die Quervalidierung werden durch die neuen Werte überschrieben
		for (MassnahmenImportAttribute attribut : attribute) {
			String attributName = attribut.toString();
			Object attributWert = zuordnung.getFeature().getAttribute(attributName);
			if (Objects.isNull(attributWert)) {
				if (MassnahmenImportAttribute.getPflichtAttribute().contains(attribut)) {
					zuordnung.addMappingFehler(
						MappingFehler.of(
							attributName,
							MappingFehlermeldung.PFLICHTATTRIBUT_NICHT_GESETZT.getText(attributName)));
				}
			} else {
				String attributString = attributWert.toString();
				try {
					switch (attribut) {
					case UMSETZUNGSSTATUS -> umsetzungsstatus = Optional.of(
						mapFromDisplayTextToEnumConstant(Umsetzungsstatus.class, attributString));
					case BEZEICHNUNG -> validiereBezeichnung(attributString);
					case KATEGORIEN -> massnahmenkategorien = mapToMassnahmenkategorien(attributString);
					case ZUSTAENDIGER -> mapToVerwaltungseinheit(attributString);
					case SOLL_STANDARD -> mapFromDisplayTextToEnumConstant(SollStandard.class, attributString);
					case DURCHFUEHRUNGSZEITRAUM -> durchfuehrungszeitraum = Optional.ofNullable(
						mapToDurchfuehrungszeitraum(attributString));
					case BAULASTTRAEGER -> baulastZustaendiger = Optional.ofNullable(
						mapToVerwaltungseinheitEmptyAllowed(attributString));
					case HANDLUNGSVERANTWORTLICHER -> handlungsverantwortlicher = Optional.ofNullable(
						mapToEnumConstantAllowEmpty(Handlungsverantwortlicher.class, attributString));
					case PRIORITAET -> validierePrioritaet(attributString);
					case KOSTENANNAHME -> validiereKostenannahme(attributString);
					case UNTERHALTSZUSTAENDIGER -> mapToVerwaltungseinheitEmptyAllowed(attributString);
					case MAVIS_ID -> validiereMaViSID(attributString);
					case VERBA_ID -> validiereVerbaID(attributString);
					case LGVFG_ID -> validiereLGVFGID(attributString);
					case REALISIERUNGSHILFE -> mapToEnumConstantAllowEmpty(Realisierungshilfe.class, attributString);
					case NETZKLASSEN -> mapToNetzklassen(attributString);
					case PLANUNG_ERFORDERLICH, VEROEFFENTLICHT -> validiereJaNein(attributString);
					case ZURUECKSTELLUNGS_GRUND -> zurueckstellungsgrund = Optional
						.ofNullable(mapToEnumConstantAllowEmpty(ZurueckstellungsGrund.class,
							attributString));
					case BEGRUENDUNG_STORNIERUNGSANFRAGE -> begruendungStornierungsanfrage = Optional
						.ofNullable(validiereBegruendungStornierungsanfrage(attributString));
					case BEGRUENDUNG_ZURUECKSTELLUNG -> begruendungZurueckstellung = Optional
						.ofNullable(validiereBegruendungZurueckstellung(attributString));
					default -> throw new RuntimeException(
						String.format("Das MassnahmenImportAttribut '%s' fehlt bei der Validierung!", attribut));
					}
				} catch (MassnahmenAttributWertValidierungsException e) {
					zuordnung.addMappingFehler(
						MappingFehler.of(
							attributName,
							MappingFehlermeldung.ATTRIBUT_WERT_UNGUELTIG.getText(attributString)));
				} catch (VerwaltungseinheitNichtGefundenException e) {
					zuordnung.addMappingFehler(
						MappingFehler.of(
							attributName,
							MappingFehlermeldung.VERWALTUNGSEINHEIT_NICHT_GEFUNDEN.getText(attributString)));
				} catch (OrganisationsartUndNameNichtEindeutigException e) {
					zuordnung.addMappingFehler(MappingFehler.of(attributName, e.getMessage()));
				}
			}
		}

		// Quervalidierung
		if (umsetzungsstatus.isPresent() && Umsetzungsstatus.isAbPlanung(umsetzungsstatus.get())) {
			if (durchfuehrungszeitraum.isEmpty()) {
				zuordnung.addMappingFehler(
					MappingFehler.of(
						MassnahmenImportAttribute.DURCHFUEHRUNGSZEITRAUM.toString(),
						MappingFehlermeldung.QUERVALIDIERUNG_PFLICHTATTRIBUTE.getText(
							umsetzungsstatus.get())));
			}
			if (baulastZustaendiger.isEmpty()) {
				zuordnung.addMappingFehler(
					MappingFehler.of(
						MassnahmenImportAttribute.BAULASTTRAEGER.toString(),
						MappingFehlermeldung.QUERVALIDIERUNG_PFLICHTATTRIBUTE.getText(
							umsetzungsstatus.get())));
			}
			if (handlungsverantwortlicher.isEmpty()) {
				zuordnung.addMappingFehler(
					MappingFehler.of(
						MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER.toString(),
						MappingFehlermeldung.QUERVALIDIERUNG_PFLICHTATTRIBUTE.getText(
							umsetzungsstatus.get())));
			}
		}

		if (umsetzungsstatus.isPresent()) {
			if (!Massnahme.isUmsetzungsstatusValidForKonzeptionsquelle(konzeptionsquelle, umsetzungsstatus.get())) {
				zuordnung.addMappingFehler(MappingFehler.of(MassnahmenImportAttribute.UMSETZUNGSSTATUS.toString(),
					"Umsetzungsstatus nicht valid für Konzeptionsquelle der Maßnahme."));
			}

			if (!Massnahme.isBegruendungStornierungsanfrageValidForUmsetzungsstatus(umsetzungsstatus.get(),
				begruendungStornierungsanfrage.orElse(null))) {
				zuordnung.addMappingFehler(
					MappingFehler.of(MassnahmenImportAttribute.BEGRUENDUNG_STORNIERUNGSANFRAGE.toString(),
						"Begründung Stornierungsanfrage nicht valid für Umsetzungsstatus der Maßnahme."));
			}

			if (!Massnahme.isZurueckstellungsGrundValidForUmsetzungsstatus(umsetzungsstatus.get(),
				zurueckstellungsgrund.orElse(null))) {
				zuordnung.addMappingFehler(MappingFehler.of(MassnahmenImportAttribute.ZURUECKSTELLUNGS_GRUND.toString(),
					"Zurückstellungsgrund nicht valid für Umsetzungsstatus der Maßnahme."));
			}
		}

		if (!Massnahme.hatNurEineMassnahmenkategorieProOberkategorie(massnahmenkategorien)) {
			zuordnung.addMappingFehler(
				MappingFehler.of(
					MassnahmenImportAttribute.KATEGORIEN.toString(),
					MappingFehlermeldung.QUERVALIDIERUNG_MASSNAHMENKATEGORIE_OBERKATEGORIE.getText()));
		}

		if (!Massnahme.areKategorienValidForKonzeptionsquelle(konzeptionsquelle, massnahmenkategorien)) {
			zuordnung.addMappingFehler(
				MappingFehler.of(
					MassnahmenImportAttribute.KATEGORIEN.toString(),
					MappingFehlermeldung.QUERVALIDIERUNG_MASSNAHMENKATEGORIE_KONZEPTIONSQUELLE.getText()));

		}

		if (!Massnahme.isBegruendungZurueckstellungValidForZurueckstellungsgrund(zurueckstellungsgrund.orElse(null),
			begruendungZurueckstellung.orElse(null))) {
			zuordnung.addMappingFehler(
				MappingFehler.of(MassnahmenImportAttribute.BEGRUENDUNG_ZURUECKSTELLUNG.toString(),
					"Begründung Zurückstellung nicht valid für Zurückstellungsgrund der Maßnahme."));
		}
	}

	private static BegruendungZurueckstellung validiereBegruendungZurueckstellung(
		String attributString) throws MassnahmenAttributWertValidierungsException {
		if (attributString.isBlank()) {
			return null;
		}

		if (!BegruendungZurueckstellung.isValid(attributString)) {
			throw new MassnahmenAttributWertValidierungsException();
		}

		return BegruendungZurueckstellung.of(attributString);
	}

	private static BegruendungStornierungsanfrage validiereBegruendungStornierungsanfrage(
		String attributString) throws MassnahmenAttributWertValidierungsException {
		if (attributString.isBlank()) {
			return null;
		}

		if (!BegruendungStornierungsanfrage.isValid(attributString)) {
			throw new MassnahmenAttributWertValidierungsException();
		}

		return BegruendungStornierungsanfrage.of(attributString);
	}

	private static void validiereJaNein(String attributString) throws MassnahmenAttributWertValidierungsException {
		if (!("ja".equalsIgnoreCase(attributString) || "nein".equalsIgnoreCase(attributString))) {
			throw new MassnahmenAttributWertValidierungsException();
		}
	}

	private void validiereLGVFGID(String attributString) throws MassnahmenAttributWertValidierungsException {
		if (!attributString.isBlank() && !LGVFGID.isValid(attributString)) {
			throw new MassnahmenAttributWertValidierungsException();
		}
	}

	private void validiereVerbaID(String attributString) throws MassnahmenAttributWertValidierungsException {
		if (!attributString.isBlank() && !VerbaID.isValid(attributString)) {
			throw new MassnahmenAttributWertValidierungsException();
		}
	}

	private void validiereMaViSID(String attributString) throws MassnahmenAttributWertValidierungsException {
		if (!attributString.isBlank() && !MaViSID.isValid(attributString)) {
			throw new MassnahmenAttributWertValidierungsException();
		}
	}

	private void validiereKostenannahme(String attributString) throws MassnahmenAttributWertValidierungsException {
		if (!attributString.isBlank() && !Kostenannahme.isValid(attributString)) {
			throw new MassnahmenAttributWertValidierungsException();
		}
	}

	private void validierePrioritaet(String attributString) throws MassnahmenAttributWertValidierungsException {
		if (!attributString.isBlank() && !Prioritaet.isValid(attributString)) {
			throw new MassnahmenAttributWertValidierungsException();
		}
	}

	private void validiereBezeichnung(String attributString) throws MassnahmenAttributWertValidierungsException {
		if (attributString.isBlank() || !Bezeichnung.isValid(attributString)) {
			throw new MassnahmenAttributWertValidierungsException();
		}
	}

	private Durchfuehrungszeitraum mapToDurchfuehrungszeitraum(String attributString)
		throws MassnahmenAttributWertValidierungsException {
		if (attributString.isBlank()) {
			return null;
		}
		if (Durchfuehrungszeitraum.isValid(attributString)) {
			return Durchfuehrungszeitraum.of(attributString);
		} else {
			throw new MassnahmenAttributWertValidierungsException();
		}
	}

	private <E extends Enum<E>> E mapToEnumConstantAllowEmpty(Class<E> eClass, String value)
		throws MassnahmenAttributWertValidierungsException {
		if (value.isBlank()) {
			return null;
		}
		return mapFromDisplayTextToEnumConstant(eClass, value);
	}

	private <E extends Enum<E>> E mapFromDisplayTextToEnumConstant(Class<E> eClass, String value)
		throws MassnahmenAttributWertValidierungsException {
		return Arrays.stream(eClass.getEnumConstants()).filter(e -> e.toString().equals(value)).findFirst()
			.orElseThrow(MassnahmenAttributWertValidierungsException::new);
	}

	private <E extends Enum<E>> boolean isValidEnumConstant(Class<E> eClass, String value) {
		return Arrays.stream(eClass.getEnumConstants()).map(Enum::name).anyMatch(value::equals);
	}

	private Verwaltungseinheit mapToVerwaltungseinheitEmptyAllowed(String attributString)
		throws VerwaltungseinheitNichtGefundenException, OrganisationsartUndNameNichtEindeutigException {
		if (attributString.isBlank()) {
			return null;
		}
		return mapToVerwaltungseinheit(attributString);
	}

	private Verwaltungseinheit mapToVerwaltungseinheit(String attributString)
		throws VerwaltungseinheitNichtGefundenException, OrganisationsartUndNameNichtEindeutigException {
		Pair<String, OrganisationsArt> parsed;
		try {
			parsed = Verwaltungseinheit.parseBezeichnung(attributString);
		} catch (Exception e) {
			throw new VerwaltungseinheitNichtGefundenException();
		}
		return verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(
			parsed.getFirst(), parsed.getSecond()).orElseThrow(VerwaltungseinheitNichtGefundenException::new);
	}

	private Set<Netzklasse> mapToNetzklassen(String s) throws MassnahmenAttributWertValidierungsException {
		if (s.isBlank()) {
			return Collections.emptySet();
		}

		String[] split = s.split(";");
		if (Arrays.stream(split).anyMatch(nk -> !isValidEnumConstant(Netzklasse.class, nk))) {
			throw new MassnahmenAttributWertValidierungsException();
		}

		return new HashSet<>(
			Arrays.stream(split)
				.map(Netzklasse::valueOf)
				.collect(Collectors.toSet()));
	}

	private Set<Massnahmenkategorie> mapToMassnahmenkategorien(String s)
		throws MassnahmenAttributWertValidierungsException {
		if (s.isBlank()) {
			throw new MassnahmenAttributWertValidierungsException();
		}

		String[] split = s.split(";");
		if (Arrays.stream(split).anyMatch(nk -> !isValidEnumConstant(Massnahmenkategorie.class, nk))) {
			throw new MassnahmenAttributWertValidierungsException();
		}

		return Arrays.stream(split)
			.map(Massnahmenkategorie::valueOf)
			.collect(Collectors.toSet());
	}

	@Async
	@Transactional
	public void erstelleNetzbezuege(MassnahmenImportSession session) {
		require(
			session.getSchritt().equals(MassnahmenImportSession.ATTRIBUTFEHLER_UEBERPRUEFEN)
				&& !session.isExecuting()
				&& !session.hatFehler(),
			"Das Erstellen der Netzbezüge kann nur in Schritt 3 und nur einmal passieren.");
		log.info("Maßnahmenimport für Benutzer {}: Starte Erstellen der Netzbezüge", session.getBenutzer().getId());
		session.setExecuting(true);

		MatchingStatistik matchingStatistik = new MatchingStatistik();

		try {
			session.getZuordnungen().stream()
				// Bei löschenden Zuordnungen und solche, die eh nicht gespeichert werden können, müssen keine
				// Netzbezüge erstellt werden
				.filter(
					zuordnung -> zuordnung.getZuordnungStatus() != MassnahmenImportZuordnungStatus.GELOESCHT
						&& zuordnung
							.canBeSaved())
				.forEach(
					zuordnung -> massnahmeNetzbezugService.bestimmeNetzbezugDerZuordnung(
						zuordnung, matchingStatistik));

			log.info("Matchingstatistik:");
			log.info(matchingStatistik.toString());
			logNetzbezugHinweise(session);
			session.setSchritt(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN);
		} catch (Exception e) {
			session.addLogEintrag(
				ImportLogEintrag.ofError(
					"Es ist ein unbekannter Fehler bei der Erstellung der Netzbezüge aufgetreten."));
			log.error(
				"Maßnahmenimport für Benutzer {}: Unbekannter Fehler beim Erstellen der Netzbezüge",
				session.getBenutzer().getId(),
				e);
		} finally {
			session.setExecuting(false);
		}
	}

	private static void logNetzbezugHinweise(MassnahmenImportSession session) {
		log.info(
			"Maßnahmenimport für Benutzer {}: Hinweise beim Erstellen der Netzbezüge (pro Hinweis-Typ inkl. Häufigkeit)",
			session.getBenutzer().getId());

		List<MassnahmenImportZuordnung> zuordnungen = session.getZuordnungen();

		Map<NetzbezugHinweis, Long> netzbezugHinweisFrequencyMap = zuordnungen.stream()
			.flatMap(z -> z.getNetzbezugHinweise().stream())
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		try {
			log.info(
				"  => Hinweise: {}",
				new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(netzbezugHinweisFrequencyMap));
		} catch (JsonProcessingException e) {
			log.info("  => Hinweise: {}", netzbezugHinweisFrequencyMap);
		}
	}

	public void aktualisiereNetzbezug(MassnahmenImportSession session, int zuordnungId, MassnahmeNetzBezug netzbezug) {
		require(
			session.getSchritt().equals(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN)
				&& !session.isExecuting()
				&& !session.hatFehler(),
			"Das Aktualisieren der Netzbezüge kann nur in Schritt 4 passieren.");
		log.info(
			"Maßnahmenimport für Benutzer {}: Aktualisiere Netzbezug in Zuordnung {}",
			session.getBenutzer().getId(), zuordnungId);

		session.getZuordnungen().stream()
			.filter(z -> z.getId() == zuordnungId)
			.findAny() // MassnahmenImportZuordnung stellt sicher, dass es keine zwei Zuordnungen mit derselben ID gibt.
			.orElseThrow(() -> new RuntimeException("Die ID der zu aktualisierenden Zuordnung existiert nicht"))
			.aktualisiereNetzbezug(netzbezug, true);
	}

	@Async
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	@WithAuditing(context = AuditingContext.MANUELLER_MASSNAHMEN_IMPORT)
	public void speichereMassnahmenDerZuordnungen(MassnahmenImportSession session,
		List<Integer> zuSpeicherndeZuordnungenIds) {
		require(zuSpeicherndeZuordnungenIds, notNullValue());
		require(
			session.getSchritt().equals(MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN)
				&& !session.isExecuting()
				&& !session.hatFehler(),
			"Das Speichern der Maßnahmen kann nur in Schritt "
				+ MassnahmenImportSession.IMPORT_UEBERPRUEFEN_UND_SPEICHERN + " und nur einmal passieren.");
		log.info("Maßnahmenimport für Benutzer {}: Starte Speichern der Maßnahmen", session.getBenutzer().getId());

		List<MassnahmenImportZuordnung> zuSpeicherndeZuordnungen = session.getZuordnungen()
			.stream()
			.peek(zuordnung -> {
				if (zuSpeicherndeZuordnungenIds.contains(zuordnung.getId())) {
					if (zuordnung.canBeSaved()) {
						zuordnung.select();
					} else {
						session.addLogEintrag(
							ImportLogEintrag.ofError(
								"Die Zuordnung mit ID " + zuordnung.getId()
									+ " enthält Fehler und kann nicht gespeichert werden."));
						log.warn(
							"Maßnahmenimport für Benutzer {}: Maßnahme {} beim Import geskipped, da fehlerhaft",
							session.getBenutzer().getId(), zuordnung.getId());
					}
				} else {
					zuordnung.deselect();
				}
			})
			.filter(zuordnung -> zuordnung.isSelected())
			.toList();

		// 1. Finale Prüfung ob die Maßnahmen soweit korrekt sind
		require(
			zuSpeicherndeZuordnungen.stream()
				.allMatch(zuordnung -> zuordnung.getZuordnungStatus() != MassnahmenImportZuordnungStatus.FEHLERHAFT),
			"Der Status der Zuordnungen darf nicht '" + MassnahmenImportZuordnungStatus.FEHLERHAFT.name() + "' sein.");
		require(zuSpeicherndeZuordnungen.stream().allMatch(zuordnung -> {
			return zuordnung.getMassnahmeKonzeptId().isPresent();
		}), "MassnahmeKonzeptId muss bei nicht-fehlerhaften Maßnahmen vorhanden sein.");
		require(
			zuSpeicherndeZuordnungen.stream().allMatch(zuordnung -> zuordnung.getMappingFehler().isEmpty()),
			"Zu speicherne Zuordnungen dürfen keine Mapping-Fehler haben.");
		require(
			zuSpeicherndeZuordnungen.stream().allMatch(
				zuordnung -> zuordnung.getNetzbezugHinweise().stream()
					.allMatch(hinweis -> !hinweis.getSeverity().equals(Severity.ERROR))),
			"Zu speicherne Zuordnungen dürfen keine Netzbezughinweis-Fehler haben.");
		require(
			zuSpeicherndeZuordnungen.stream()
				.allMatch(zuordnung -> {
					boolean hasNetzbezug = zuordnung.getNetzbezug().isPresent();
					boolean geloescht = zuordnung.getZuordnungStatus() == MassnahmenImportZuordnungStatus.GELOESCHT;
					return geloescht && !hasNetzbezug || !geloescht && hasNetzbezug;
				}),
			"Ungültiger Netzbezug vorhanden: Bearbeitete/neue Maßnahmen müssen einen Netzbezug haben, gelöschte Maßnahmen dürfen keinen Netzbezug haben.");

		// 2. Übernehmen der Änderungen
		session.setExecuting(true);

		for (MassnahmenImportZuordnung zuordnung : zuSpeicherndeZuordnungen) {
			try {
				switch (zuordnung.getZuordnungStatus()) {
				case GELOESCHT -> {
					Massnahme massnahme = zuordnung.getMassnahme().orElseThrow();
					massnahme.alsGeloeschtMarkieren();
					massnahmenRepostory.save(massnahme);
				}
				case NEU -> {
					// Default-Werte setzen, da es sein kann, dass nicht alle Attribute aus dem Feature übernommen
					// werden, aber der Maßnahmen-Konstruktor manche Felder zwingend braucht.
					Massnahme.MassnahmeBuilder builder = Massnahme.builder()
						.benutzerLetzteAenderung(session.getBenutzer())
						.netzbezug(zuordnung.getNetzbezug().orElseThrow())
						.netzklassen(Set.of())
						.dokumentListe(new DokumentListe())
						.kommentarListe(new KommentarListe())
						.letzteAenderung(LocalDateTime.now())
						.planungErforderlich(false)
						.veroeffentlicht(false);

					addAttributesToBuilder(
						session.getAttribute(), zuordnung.getFeature(),
						session.getKonzeptionsquelle(), zuordnung.getMassnahmeKonzeptId().orElseThrow(), builder);

					if (session.getKonzeptionsquelle() == Konzeptionsquelle.SONSTIGE) {
						String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
						builder.sonstigeKonzeptionsquelle("Manueller Import vom " + today);
					}

					Massnahme massnahme = builder.build();
					massnahmenRepostory.save(massnahme);
				}
				case ZUGEORDNET -> {
					// Wir machen hier ein .merge() Aufruf, damit die Maßnahme aus der Zuordnung wieder einen
					// Persistence-Context bekommt. Sonst schlagen manche Operationen an der Enity fehl (z.B. Setzen
					// der Netzklassen über den Builder, wodurch in den Hibernate Proxy-Objekten eine
					// LazyInitializationException fliegt), weil der alte Context, als die Entity in einem vorherigen
					// Schritt aus der DB geholt wurde, bereits geschlossen ist. Mit dem .merge() Aufruf wird der
					// Context erneuert und Änderungen laufen durch.
					Massnahme massnahme = entityManager.merge(zuordnung.getMassnahme().get());

					// Builder benutzen um Attribute zu übernehmen. Hier wird der Mechanismus für neue Maßnahmen
					// wiederverwendet um viele neue setter oder einen umständlichen ".update()"-Call zu vermeiden.
					Massnahme.MassnahmeBuilder builder = massnahme.toBuilder();
					addAttributesToBuilder(
						session.getAttribute(), zuordnung.getFeature(),
						session.getKonzeptionsquelle(), zuordnung.getMassnahmeKonzeptId().orElseThrow(), builder);

					builder.netzbezug(zuordnung.getNetzbezug().orElseThrow());

					Massnahme massnahmeToSave = builder.build();
					massnahmenRepostory.save(massnahmeToSave);
				}
				}
			} catch (OptimisticLockException e) {
				session.addLogEintrag(
					ImportLogEintrag.ofError(
						"Die Maßnahme mit ID " + zuordnung.getMassnahme().get().getId()
							+ " wurde seit dem Start des Imports verändert und kann daher nicht gespeichert werden."));
				log.error(
					"Maßnahmenimport für Benutzer {}: Optimistic-Locking für Maßnahme " + zuordnung.getMassnahme()
						.get().getId(),
					session.getBenutzer().getId(), e);
			} catch (Throwable e) {
				session.addLogEintrag(
					ImportLogEintrag.ofError(
						"Es ist ein unbekannter Fehler beim Speichern der Maßnahmen aufgetreten."));
				log.error(
					"Maßnahmenimport für Benutzer {}: Unbekannter Fehler beim Speichern der Maßnahmen",
					session.getBenutzer().getId(), e);
			}
		}

		entityManager.flush();
		entityManager.clear();

		session.setExecuting(false);
		session.setSchritt(MassnahmenImportSession.FEHLERPROTOKOLL_HERUNTERLADEN);
	}

	private void addAttributesToBuilder(List<MassnahmenImportAttribute> attribute, SimpleFeature feature,
		Konzeptionsquelle konzeptionsquelle, MassnahmeKonzeptID massnahmeKonzeptId,
		Massnahme.MassnahmeBuilder builder) {

		builder.konzeptionsquelle(konzeptionsquelle);

		if (massnahmeKonzeptId != null) {
			builder.massnahmeKonzeptId(massnahmeKonzeptId);
		}

		for (MassnahmenImportAttribute attribut : attribute) {
			String attributName = attribut.toString();
			Object featureAttribute = feature.getAttribute(attributName);
			if (featureAttribute == null) {
				continue;
			}
			String attributWert = featureAttribute.toString();

			try {
				switch (attribut) {
				case UMSETZUNGSSTATUS -> builder.umsetzungsstatus(
					mapFromDisplayTextToEnumConstant(
						Umsetzungsstatus.class, attributWert));
				case BEZEICHNUNG -> builder.bezeichnung(Bezeichnung.of(attributWert));
				case KATEGORIEN -> builder.massnahmenkategorien(mapToMassnahmenkategorien(attributWert));
				case ZUSTAENDIGER -> builder.zustaendiger(mapToVerwaltungseinheit(attributWert));
				case SOLL_STANDARD -> builder.sollStandard(
					mapFromDisplayTextToEnumConstant(
						SollStandard.class,
						attributWert));
				case DURCHFUEHRUNGSZEITRAUM -> builder.durchfuehrungszeitraum(
					mapToDurchfuehrungszeitraum(
						attributWert));
				case BAULASTTRAEGER -> builder.baulastZustaendiger(mapToVerwaltungseinheitEmptyAllowed(attributWert));
				case HANDLUNGSVERANTWORTLICHER -> builder.handlungsverantwortlicher(
					mapToEnumConstantAllowEmpty(Handlungsverantwortlicher.class, attributWert));
				case PRIORITAET -> builder.prioritaet(Prioritaet.of(attributWert));
				case KOSTENANNAHME -> builder.kostenannahme(Kostenannahme.of(attributWert));
				case UNTERHALTSZUSTAENDIGER -> builder.unterhaltsZustaendiger(
					mapToVerwaltungseinheitEmptyAllowed(
						attributWert));
				case MAVIS_ID -> builder.maViSID(MaViSID.of(attributWert));
				case VERBA_ID -> builder.verbaID(VerbaID.of(attributWert));
				case LGVFG_ID -> builder.lgvfgid(LGVFGID.of(attributWert));
				case REALISIERUNGSHILFE -> builder.realisierungshilfe(
					mapToEnumConstantAllowEmpty(
						Realisierungshilfe.class, attributWert));
				case NETZKLASSEN -> builder.netzklassen(mapToNetzklassen(attributWert));
				case PLANUNG_ERFORDERLICH -> builder.planungErforderlich(jaNeinToBool(attributWert));
				case VEROEFFENTLICHT -> builder.veroeffentlicht(jaNeinToBool(attributWert));
				case BEGRUENDUNG_STORNIERUNGSANFRAGE -> builder.begruendungStornierungsanfrage(
					attributWert.isBlank() ? null : BegruendungStornierungsanfrage.of(attributWert));
				case ZURUECKSTELLUNGS_GRUND -> builder
					.zurueckstellungsGrund(mapToEnumConstantAllowEmpty(ZurueckstellungsGrund.class, attributWert));
				case BEGRUENDUNG_ZURUECKSTELLUNG -> builder.begruendungZurueckstellung(
					attributWert.isBlank() ? null : BegruendungZurueckstellung.of(attributWert));

				default -> throw new RuntimeException(
					String.format("Das MassnahmenImportAttribut '%s' fehlt bei der Speicherung!", attribut));
				}
			} catch (MassnahmenAttributWertValidierungsException e) {
				log.error(
					"Fehler beim Erstellen einer neuen Maßnahme (Konzept-ID '{}') wegen ungültigem Attribut {} mit Wert {}",
					massnahmeKonzeptId != null ? massnahmeKonzeptId.getValue() : "", attributName, attributWert);
				throw new RuntimeException(e);
			} catch (VerwaltungseinheitNichtGefundenException e) {
				log.error(
					"Verwaltungseinheit {} beim Erstellen einer neuen Maßnahme (Konzept-ID '{}') für Attribut {} nicht gefunden",
					attributWert, massnahmeKonzeptId != null ? massnahmeKonzeptId.getValue() : "", attributName);
				throw new RuntimeException(e);
			} catch (OrganisationsartUndNameNichtEindeutigException e) {
				log.error(
					"Verwaltungseinheit {} beim Erstellen einer neuen Maßnahme (Konzept-ID '{}') für Attribut {} nicht eindeutig",
					attributWert, massnahmeKonzeptId != null ? massnahmeKonzeptId.getValue() : "", attributName);
				throw new RuntimeException(e);
			}
		}
	}

	private Boolean jaNeinToBool(String attributWert) {
		return "ja".equalsIgnoreCase(attributWert);
	}

	public byte[] downloadFehlerprotokoll(MassnahmenImportSession session) throws IOException {
		List<Map<String, String>> rows = session.getZuordnungen().stream()
			.map(zuordnung -> convertZuordnungToCsvRow(zuordnung))
			.collect(Collectors.toList());
		return csvRepository.write(CsvData.of(rows, MassnahmenImportSession.CsvHeader.ALL));
	}

	private Map<String, String> convertZuordnungToCsvRow(MassnahmenImportZuordnung zuordnung) {
		Map<String, String> row = new HashMap<>();

		row.put(
			MassnahmenImportSession.CsvHeader.MASSNAHME_ID, zuordnung.getMassnahmeKonzeptId()
				.map(id -> id.getValue()).orElse(""));
		row.put(
			MassnahmenImportSession.CsvHeader.IMPORTIERT, convertBooleanToCsvRepresentation(
				zuordnung
					.isSelected()));

		MassnahmenImportZuordnungStatus status = zuordnung.getZuordnungStatus();
		if (!zuordnung.canBeSaved()) {
			status = MassnahmenImportZuordnungStatus.FEHLERHAFT;
		}

		row.put(MassnahmenImportSession.CsvHeader.STATUS, status.getDisplayText());

		List<String> hinweise = new ArrayList<>();
		hinweise.addAll(zuordnung.getNetzbezugHinweise().stream().map(hinweis -> hinweis.getDisplayText()).toList());
		hinweise.addAll(zuordnung.getMappingFehler().stream().map(mappingFehler -> mappingFehler.getText()).toList());
		String hinweisRowContent = hinweise.stream().distinct().collect(Collectors.joining("\n\n"));
		row.put(MassnahmenImportSession.CsvHeader.HINWEIS, hinweisRowContent);

		return row;
	}

	private String convertBooleanToCsvRepresentation(boolean bool) {
		return bool ? "Ja" : "Nein";
	}
}
