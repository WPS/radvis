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

import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.exception.MassnahmenAttributWertValidierungsException;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.exception.VerwaltungseinheitNichtGefundenException;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehler;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehlermeldung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportAttribute;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
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
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManuellerMassnahmenImportService {

	public static final String NETZ_BEZUG_ATTRIBUTENAME = "NetzBezug";
	public static final String MASSNAHME_ID_ATTRIBUTENAME = "Massnahme-ID";
	public static final String GELOESCHT_ATTRIBUTENAME = "geloescht";
	private final ManuellerImportService manuellerImportService;
	private final GeoJsonImportRepository geoJsonImportRepository;
	private final VerwaltungseinheitRepository verwaltungseinheitRepository;
	private final MassnahmeRepository massnahmenRepostory;
	private final EntityManager entityManager;
	private final double minimaleDistanzFuerAbweichungsWarnung;

	public ManuellerMassnahmenImportService(ManuellerImportService manuellerImportService,
		GeoJsonImportRepository geoJsonImportRepository,
		VerwaltungseinheitRepository verwaltungseinheitRepository,
		MassnahmeRepository massnahmenRepostory,
		EntityManager entityManager,
		double minimaleDistanzFuerAbweichungsWarnung
	) {
		this.manuellerImportService = manuellerImportService;
		this.geoJsonImportRepository = geoJsonImportRepository;
		this.verwaltungseinheitRepository = verwaltungseinheitRepository;
		this.massnahmenRepostory = massnahmenRepostory;
		this.entityManager = entityManager;
		this.minimaleDistanzFuerAbweichungsWarnung = minimaleDistanzFuerAbweichungsWarnung;
	}

	public Optional<MassnahmenImportSession> getMassnahmenImportSession(Benutzer benutzer) {
		return manuellerImportService.findImportSessionFromBenutzer(benutzer, MassnahmenImportSession.class);
	}

	private MultiPolygon getVereinigtenBereich(List<Long> gebietskoerperschaftIds) {
		return verwaltungseinheitRepository.getVereintenBereich(gebietskoerperschaftIds);
	}

	@Async
	@Transactional
	public void ladeFeatures(MassnahmenImportSession session, byte[] file) {
		require(session.getSchritt().equals(MassnahmenImportSession.DATEI_HOCHLADEN) && !session.isExecuting()
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
				// Duplikate heausfiltern
				.filter(e -> e.getValue() > 1)
				// Häufigkeit verwerfen, nur ID aufsammeln
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		} catch (ReadGeoJSONException e) {
			session.addLogEintrag(ImportLogEintrag.ofError(e.getMessage()));
			log.error(
				"Maßnahmenimport für Benutzer {}: Fehler beim Einlesen der GeoJSON",
				session.getBenutzer().getId(),
				e
			);
			session.setExecuting(false);
			return;
		} catch (Exception e) {
			session.addLogEintrag(
				ImportLogEintrag.ofError("Es ist ein unbekannter Fehler beim Einlesen der GeoJSON aufgetreten."));
			log.error(
				"Maßnahmenimport für Benutzer {}: Unbekannter Fehler beim Einlesen der GeoJSON",
				session.getBenutzer().getId(),
				e
			);
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
				.map(simpleFeature -> getMassnahmenZuordnung(session, simpleFeature, doppelteIds))
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
				e
			);
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

	private static void logSummary(MassnahmenImportSession session) {
		log.info(
			"Maßnahmenimport für Benutzer {}: Hinweise an der Session (pro Status inkl. Häufigkeit)",
			session.getBenutzer().getId()
		);

		List<MassnahmenImportZuordnung> importZuordnungen = session.getZuordnungen();
		Arrays.stream(MassnahmenImportZuordnungStatus.values()).forEach(status -> {
			log.info("Zuordnungen {}: {}", status.name(),
				importZuordnungen.stream().filter(z -> z.getStatus().equals(status))
					.count());
			Map<MappingFehler, Long> mappingHinweisFrequencyMap = importZuordnungen.stream()
				.filter(z -> z.getStatus().equals(status))
				.flatMap(z -> z.getMappingFehler().stream())
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			try {
				log.info("  => Hinweise: {}",
					new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
						mappingHinweisFrequencyMap));
			} catch (JsonProcessingException e) {
				log.info("  => Hinweise: {}",
					mappingHinweisFrequencyMap);
			}
		});
	}

	private MassnahmenImportZuordnung getMassnahmenZuordnung(MassnahmenImportSession session,
		SimpleFeature simpleFeature, Set<String> doppelteIds) {
		Geometry zuImportierendeGeometrie = (Geometry) simpleFeature.getDefaultGeometry();

		Object massnahmenKonzeptIDAttribute = simpleFeature.getAttribute(MASSNAHME_ID_ATTRIBUTENAME);
		// Keine Id am Feature -> Fehler
		if (Objects.isNull(massnahmenKonzeptIDAttribute) || massnahmenKonzeptIDAttribute.toString().isBlank()) {
			MassnahmenImportZuordnung massnahmenImportZuordnung = new MassnahmenImportZuordnung(
				null,
				simpleFeature,
				null,
				MassnahmenImportZuordnungStatus.FEHLERHAFT);
			massnahmenImportZuordnung.addMappingFehler(
				MappingFehler.of(
					MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_KEINE_ID.getText()
				)
			);
			return massnahmenImportZuordnung;
		}

		String massnahmenKonzeptIDString = massnahmenKonzeptIDAttribute.toString();
		MassnahmeKonzeptID massnahmeKonzeptID = MassnahmeKonzeptID.of(massnahmenKonzeptIDString);

		// Massnahmen-Id in der Quelle doppelt vorhanden
		if (doppelteIds.contains(massnahmenKonzeptIDString)) {
			MassnahmenImportZuordnung massnahmenImportZuordnung = new MassnahmenImportZuordnung(
				massnahmeKonzeptID,
				simpleFeature,
				null,
				MassnahmenImportZuordnungStatus.FEHLERHAFT);
			massnahmenImportZuordnung.addMappingFehler(
				MappingFehler.of(
					MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_MEHRFACH.getText()
				)
			);
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
				geloescht ? MassnahmenImportZuordnungStatus.GELOESCHT : MassnahmenImportZuordnungStatus.GEMAPPT);
			massnahmenImportZuordnung.addMappingFehler(
				MappingFehler.of(
					MASSNAHME_ID_ATTRIBUTENAME,
					MappingFehlermeldung.MASSNAHME_NICHT_EINDEUTIG.getText(massnahmen.size())
				)
			);
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
						MappingFehlermeldung.MASSNAHME_NICHT_GEFUNDEN.getText()
					)
				);
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
			// Löschen Oder Update
			Massnahme massnahme = massnahmen.get(0);
			if (geloescht) {
				// LÖSCHEN
				return new MassnahmenImportZuordnung(
					massnahmeKonzeptID,
					simpleFeature,
					massnahme,
					MassnahmenImportZuordnungStatus.GELOESCHT);
			} else {
				// UPDATE
				MassnahmenImportZuordnung massnahmenImportZuordnungGemappt = new MassnahmenImportZuordnung(
					massnahmeKonzeptID,
					simpleFeature,
					massnahme,
					MassnahmenImportZuordnungStatus.GEMAPPT);

				// PRÜFUNG AUF KORREKTEN GEOMTYPE
				pruefeGeometrieTyp(zuImportierendeGeometrie, massnahmenImportZuordnungGemappt);

				// PRÜFUNG AUF ABWEICHENDE GEOMETRIEN
				pruefeAbweichendeGeometrien(zuImportierendeGeometrie, massnahme, massnahmenImportZuordnungGemappt);

				return massnahmenImportZuordnungGemappt;
			}
		}

	}

	private void pruefeGeometrieTyp(Geometry zuImportierendeGeometrie,
		MassnahmenImportZuordnung massnahmenImportZuordnung) {
		if (!isGeometrieTypZulaessig(zuImportierendeGeometrie)) {
			massnahmenImportZuordnung.addNetzbezugHinweis(
				NetzbezugHinweis.ofWarnung(
					MappingFehlermeldung.FALSCHER_GEOMETRIE_TYP.getText(),
					MappingFehlermeldung.FALSCHER_GEOMETRIE_TYP.getText()
				)
			);
		}
	}

	private boolean isGeometrieTypZulaessig(Geometry zuImportierendeGeometrie) {
		Set<String> allowedBaseGeometryTypes = Set.of(Geometry.TYPENAME_MULTILINESTRING,
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
				NetzbezugHinweis.ofWarnung(
					MappingFehlermeldung.GEOMETRIEN_ABWEICHEND.getText(),
					MappingFehlermeldung.GEOMETRIEN_ABWEICHEND.getText()
				)
			);
		}
	}

	private List<Massnahme> getPassendeMassnahmen(MassnahmenImportSession session,
		MassnahmeKonzeptID massnahmeKonzeptID) {
		return session.getSollStandard().isPresent() ?
			massnahmenRepostory.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndSollStandardAndGeloeschtFalse(
				massnahmeKonzeptID,
				session.getKonzeptionsquelle(), session.getSollStandard().get()) :
			massnahmenRepostory.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
				massnahmeKonzeptID,
				session.getKonzeptionsquelle());
	}

	private String getNamen(List<Long> gebietskoerperschaftIds) {
		return verwaltungseinheitRepository.findAllDbViewsById(gebietskoerperschaftIds)
			.stream()
			.map(gK -> String.format("%s (%s)", gK.getName(), gK.getOrganisationsArt()))
			.collect(Collectors.joining(","));
	}

	public MassnahmenImportSession createSession(Benutzer benutzer, List<Long> gebietskoerperschaftenIds,
		Konzeptionsquelle konzeptionsquelle, SollStandard sollStandard) {

		MultiPolygon vereinigterBereich = getVereinigtenBereich(gebietskoerperschaftenIds);
		String bereichName = getNamen(gebietskoerperschaftenIds);

		return new MassnahmenImportSession(benutzer,
			vereinigterBereich, bereichName, gebietskoerperschaftenIds, konzeptionsquelle, sollStandard);
	}

	@Async
	@Transactional
	public void attributeValidieren(MassnahmenImportSession session, List<MassnahmenImportAttribute> attribute) {
		require(session.getSchritt().equals(MassnahmenImportSession.ATTRIBUTE_AUSWAEHLEN) && !session.isExecuting()
				&& !session.hatFehler(),
			"Das Validieren der Attribute kann nur in Schritt 2 und nur einmal passieren.");
		log.info("Maßnahmenimport für Benutzer {}: Starte Validieren der Attribute", session.getBenutzer().getId());
		session.setExecuting(true);

		try {
			session.setAttribute(attribute);
			session.getZuordnungen().stream()
				// Bei fehlerhaften und löschenden Zuordnungen müssen die Attribute nicht validiert werden
				.filter(zuordnung -> zuordnung.getStatus() == MassnahmenImportZuordnungStatus.GEMAPPT ||
					zuordnung.getStatus() == MassnahmenImportZuordnungStatus.NEU)
				.forEach(zuordnung -> validiereAttributeDerZuordnung(session, zuordnung));
			logSummary(session);
			session.setSchritt(MassnahmenImportSession.ATTRIBUTFEHLER_UEBERPRUEFEN);
		} catch (Exception e) {
			session.addLogEintrag(
				ImportLogEintrag.ofError(
					"Es ist ein unbekannter Fehler bei der Validierung der Attribute aufgetreten."));
			log.error(
				"Maßnahmenimport für Benutzer {}: Unbekannter Fehler beim Validieren der Attribute",
				session.getBenutzer().getId(),
				e
			);
		} finally {
			session.setExecuting(false);
		}
	}

	private void validiereAttributeDerZuordnung(MassnahmenImportSession session, MassnahmenImportZuordnung zuordnung) {
		// Attribute für Quervalidierung
		Optional<Umsetzungsstatus> umsetzungsstatus = Optional.empty();
		Set<Massnahmenkategorie> massnahmenkategorien = Collections.emptySet();
		Optional<Durchfuehrungszeitraum> durchfuehrungszeitraum = Optional.empty();
		Optional<Verwaltungseinheit> baulastZustaendiger = Optional.empty();
		Optional<Handlungsverantwortlicher> handlungsverantwortlicher = Optional.empty();

		if (zuordnung.getStatus().equals(MassnahmenImportZuordnungStatus.GEMAPPT) &&
			zuordnung.getMassnahme().isPresent()) {
			// Attribute bestehender Maßnahmen sollen unabhängig von den ausgewählten Attributen
			// für die Quervalidierung genutzt werden können
			Massnahme massnahme = entityManager.merge(zuordnung.getMassnahme().get());
			umsetzungsstatus = Optional.of(massnahme.getUmsetzungsstatus());
			massnahmenkategorien = massnahme.getMassnahmenkategorien();
			durchfuehrungszeitraum = massnahme.getDurchfuehrungszeitraum();
			baulastZustaendiger = massnahme.getBaulastZustaendiger();
			handlungsverantwortlicher = massnahme.getHandlungsverantwortlicher();
		} else {
			// Für neu angelegte Maßnahmen müssen alle Pflichtattribute auswählt sein.
			List<MassnahmenImportAttribute> nichtAusgewaehltePflichtattribute = MassnahmenImportAttribute.getPflichtAttribute()
				.stream()
				.filter(pflichtAttribut -> !session.getAttribute().contains(pflichtAttribut))
				.toList();

			nichtAusgewaehltePflichtattribute.forEach(
				pflichtAttribut -> zuordnung.addMappingFehler(
					MappingFehler.of(
						pflichtAttribut.toString(),
						MappingFehlermeldung.PFLICHTATTRIBUT_NICHT_AUSGEWAEHLT.getText()
					)
				)
			);
		}

		// Validiere Attribute einzeln
		// Attribute für die Quervalidierung werden durch die neuen Werte überschrieben
		for (MassnahmenImportAttribute attribut : session.getAttribute()) {
			String attributName = attribut.toString();
			Object attributWert = zuordnung.getFeature().getAttribute(attributName);
			if (Objects.isNull(attributWert)) {
				if (MassnahmenImportAttribute.getPflichtAttribute().contains(attribut)) {
					zuordnung.addMappingFehler(
						MappingFehler.of(
							attributName,
							MappingFehlermeldung.PFLICHTATTRIBUT_NICHT_GESETZT.getText(attributName)
						)
					);
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
					case DURCHFUEHRUNGSZEITRAUM ->
						durchfuehrungszeitraum = Optional.ofNullable(mapToDurchfuehrungszeitraum(attributString));
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
					default -> throw new RuntimeException(
						String.format("Das MassnahmenImportAttribut '%s' fehlt bei der Validierung!", attribut));
					}
				} catch (MassnahmenAttributWertValidierungsException e) {
					zuordnung.addMappingFehler(
						MappingFehler.of(
							attributName,
							MappingFehlermeldung.ATTRIBUT_WERT_UNGUELTIG.getText(attributString)
						)
					);
				} catch (VerwaltungseinheitNichtGefundenException e) {
					zuordnung.addMappingFehler(
						MappingFehler.of(
							attributName,
							MappingFehlermeldung.VERWALTUNGSEINHEIT_NICHT_GEFUNDEN.getText(attributString)
						)
					);
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
							umsetzungsstatus.get()
						)
					)
				);
			}
			if (baulastZustaendiger.isEmpty()) {
				zuordnung.addMappingFehler(
					MappingFehler.of(
						MassnahmenImportAttribute.BAULASTTRAEGER.toString(),
						MappingFehlermeldung.QUERVALIDIERUNG_PFLICHTATTRIBUTE.getText(
							umsetzungsstatus.get()
						)
					)
				);
			}
			if (handlungsverantwortlicher.isEmpty()) {
				zuordnung.addMappingFehler(
					MappingFehler.of(
						MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER.toString(),
						MappingFehlermeldung.QUERVALIDIERUNG_PFLICHTATTRIBUTE.getText(
							umsetzungsstatus.get()
						)
					)
				);
			}
		}

		if (!Massnahme.hatNurEineMassnahmenkategorieProOberkategorie(massnahmenkategorien)) {
			zuordnung.addMappingFehler(
				MappingFehler.of(
					MassnahmenImportAttribute.KATEGORIEN.toString(),
					MappingFehlermeldung.QUERVALIDIERUNG_MASSNAHMENKATEGORIE.getText()
				)
			);
		}
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
		throws VerwaltungseinheitNichtGefundenException {
		if (attributString.isBlank()) {
			return null;
		}
		return mapToVerwaltungseinheit(attributString);
	}

	private Verwaltungseinheit mapToVerwaltungseinheit(String attributString)
		throws VerwaltungseinheitNichtGefundenException {
		Pair<String, OrganisationsArt> parsed;
		try {
			parsed = Verwaltungseinheit.parseBezeichnung(attributString);
		} catch (Exception e) {
			throw new VerwaltungseinheitNichtGefundenException();
		}
		return verwaltungseinheitRepository.findByNameAndOrganisationsArt(parsed.getFirst(),
			parsed.getSecond()).orElseThrow(VerwaltungseinheitNichtGefundenException::new);
	}

	private Set<Netzklasse> mapToNetzklassen(String s) throws MassnahmenAttributWertValidierungsException {
		if (s.isBlank()) {
			return Collections.emptySet();
		}

		String[] split = s.split(";");
		if (Arrays.stream(split).anyMatch(nk -> !isValidEnumConstant(Netzklasse.class, nk))) {
			throw new MassnahmenAttributWertValidierungsException();
		}

		return Arrays.stream(split)
			.map(Netzklasse::valueOf)
			.collect(Collectors.toSet());
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
}
