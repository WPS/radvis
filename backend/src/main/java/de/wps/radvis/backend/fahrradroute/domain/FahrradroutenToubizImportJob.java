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

package de.wps.radvis.backend.fahrradroute.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteNetzbezugResult;
import de.wps.radvis.backend.fahrradroute.domain.entity.ImportedToubizRoute;
import de.wps.radvis.backend.fahrradroute.domain.entity.ToubizImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.ToubizRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithFehlercode(Fehlercode.FAHRRADROUTE_TOUBIZ_IMPORT)
/**
 * Import Fahrradrouten von Toubiz. Aktualisiert bereits importierte, erstellt neue oder löscht die in Quelle nicht mehr
 * vorhandenen.
 * 
 * Umgang mit Landesradfernwegen: Es werden Informationen aus Toubiz nur für bereits im System befindliche LRFW mit
 * Toubiz-ID gezogen. Es werden alle nicht von der Route abhängigen Informationen aktualisiert. Dabei werden
 * Nutzerinformationen ggf. überschrieben.
 */
public class FahrradroutenToubizImportJob extends AbstractJob {
	// Dieser Job Name sollte sich nicht mehr aendern, weil Controller und DB Eintraege den Namen verwenden
	public static final String JOB_NAME = "FahrradroutenToubizImportJob";

	private final ToubizRepository toubizRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;

	private final FahrradrouteRepository fahrradrouteRepository;
	private final FahrradroutenMatchingService fahrradroutenMatchingService;

	private Duration netzbezugTimeout;

	private List<String> toubizIgnoreList;

	public FahrradroutenToubizImportJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		ToubizRepository toubizRepository,
		VerwaltungseinheitService verwaltungseinheitService, FahrradrouteRepository fahrradrouteRepository,
		FahrradroutenMatchingService fahrradroutenMatchingService, Duration netzbezugTimeout,
		List<String> toubizIgnoreList) {
		super(jobExecutionDescriptionRepository);

		require(toubizIgnoreList, notNullValue());
		this.toubizRepository = toubizRepository;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.fahrradrouteRepository = fahrradrouteRepository;
		this.fahrradroutenMatchingService = fahrradroutenMatchingService;
		this.netzbezugTimeout = netzbezugTimeout;
		this.toubizIgnoreList = toubizIgnoreList;
	}

	@Override
	public String getName() {
		return FahrradroutenToubizImportJob.JOB_NAME;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB)
	public JobExecutionDescription run() {
		return super.run();
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		log.info("FahrradroutenToubizImportJob gestartet");
		log.info("Timeout für Erstellung Netzbezug: {} ms", netzbezugTimeout.toMillis());
		log.info("ToubizIds to be ignored: {}", toubizIgnoreList);
		ToubizImportStatistik toubizImportStatistik = new ToubizImportStatistik();

		List<ImportedToubizRoute> importedToubizRouten = toubizRepository.importRouten();
		Set<ToubizId> frischeIds = new HashSet<>();
		Set<ToubizId> bestehendeLandesradfernwegIds = fahrradrouteRepository.findAllToubizIdsOfLandesradfernwege();

		AtomicInteger progressCount = new AtomicInteger();
		importedToubizRouten.stream()
			.filter(importedToubizRoute -> {
				if (toubizIgnoreList.contains(importedToubizRoute.getToubizId().getToubizId())) {
					toubizImportStatistik.beimImportIgnoriert.add(importedToubizRoute.getToubizId());
					return false;
				}
				return true;
			})
			.filter(importedToubizRoute -> !importedToubizRoute.isLandesradfernweg() ||
				bestehendeLandesradfernwegIds.contains(importedToubizRoute.getToubizId()))
			.map(importedToubizRoute -> createFahrradroute(importedToubizRoute,
				toubizImportStatistik))
			.peek(fahrradroute -> this.logProgressInPercent(importedToubizRouten.size(), progressCount, 10))
			.forEach(fahrradroute -> {
				frischeIds.add(fahrradroute.getToubizId());
				fahrradrouteRepository.save(
					fahrradrouteRepository.findByToubizId(fahrradroute.getToubizId())
						.map((route) -> route.getKategorie() == Kategorie.LANDESRADFERNWEG
							? route.mergeNonRouteDependentAttribute(fahrradroute)
							: route.merge(fahrradroute))
						.orElse(fahrradroute));
			});

		// Alle entfernen, die in Toubiz nicht mehr existieren und kein Landesradfernweg sind
		Set<ToubizId> toubizIdsToRemove = fahrradrouteRepository.findAllToubizIdsWithoutLandesradfernwege();
		toubizIdsToRemove.removeAll(frischeIds);
		// ältere Importe ignorierter Toubiz-Routen sollen bestehen bleiben, sofern die Datensätze noch vorhanden sind
		toubizIdsToRemove.removeAll(toubizImportStatistik.beimImportIgnoriert);
		fahrradrouteRepository.deleteAllByToubizIdIn(toubizIdsToRemove);

		toubizImportStatistik.anzahlAlterFahrradroutenGeloescht = toubizIdsToRemove.size();
		log.info("Es wurden {} alte Fahrradrouten gelöscht.", toubizImportStatistik.anzahlAlterFahrradroutenGeloescht);

		toubizImportStatistik.anzahlFahrradroutenErstellt = progressCount.get();

		log.info(toubizImportStatistik.toString());
		log.info("Anzahl importierter ToubizFahrradrouten mit erfolgreich erstellten Netzbezügen: "
			+ (toubizImportStatistik.fahrradrouteMatchingStatistik.anzahlMatchingErfolgreich
				+ toubizImportStatistik.fahrradrouteMatchingStatistik.anzahlRoutingErfolgreich));
		log.info("Für {} Fahrradrouten konnte kein Netzbezug erstellt werden",
			toubizImportStatistik.fahrradrouteMatchingStatistik.anzahlRoutingFehlgeschlagen);
		int erfolgreich = toubizImportStatistik.fahrradrouteMatchingStatistik.anzahlMatchingErfolgreich
			+ toubizImportStatistik.fahrradrouteMatchingStatistik.anzahlRoutingErfolgreich;
		int gesamtZahl = toubizImportStatistik.anzahlFahrradroutenErstellt
			- toubizImportStatistik.anzahlRoutenOhneGeometrie;
		log.info("Erfolgsquote: " + (((int) ((erfolgreich) / ((double) gesamtZahl) * 10000)) / 100.));

		return Optional.of(toubizImportStatistik);
	}

	private Fahrradroute createFahrradroute(ImportedToubizRoute importedToubizRoute,
		ToubizImportStatistik toubizImportStatistik) {

		log.info("Erstelle Route für Toubiz-ID {}", importedToubizRoute.getToubizId());

		if (importedToubizRoute.getOriginalGeometrie() == null ||
			!importedToubizRoute.getOriginalGeometrie().getGeometryType().equals(Geometry.TYPENAME_LINESTRING)) {
			toubizImportStatistik.anzahlRoutenOhneGeometrie++;
			return createFahradrouteFromImportedToubizRoute(importedToubizRoute, new ArrayList<>(), null,
				new ArrayList<>(),
				FahrradroutenMatchingAndRoutingInformation.builder().build());
		}

		FahrradroutenMatchingAndRoutingInformation.FahrradroutenMatchingAndRoutingInformationBuilder fahrradroutenMatchingAndRoutingInformationBuilder = FahrradroutenMatchingAndRoutingInformation
			.builder()
			// Hier initial auf false setzten, da erst Abbildung durch Matching versucht wird. Schlaegt das fehl und
			// das anschließende Routing klappt, wird dieser Wert noch auf "true" gesetzt.
			.abbildungDurchRouting(false);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Optional<FahrradrouteNetzbezugResult>> future = executor.submit(() -> fahrradroutenMatchingService
			.getFahrradrouteNetzbezugResult(
				(LineString) importedToubizRoute.getOriginalGeometrie(),
				toubizImportStatistik.fahrradrouteMatchingStatistik,
				fahrradroutenMatchingAndRoutingInformationBuilder, true));
		Optional<FahrradrouteNetzbezugResult> netzbezugResult = Optional.empty();
		try {
			netzbezugResult = future.get(netzbezugTimeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			toubizImportStatistik.routenMitTimeoutBeimNetzbezugErstellen.add(importedToubizRoute.getToubizId());
			future.cancel(true);
		} catch (Exception e) {
			log.error("Unerwarteter Fehler beim Erstellen des Netzbezugs", e);
		} finally {
			executor.shutdownNow();
		}

		return createFahradrouteFromImportedToubizRoute(
			importedToubizRoute,
			netzbezugResult.map(FahrradrouteNetzbezugResult::getAbschnittsweiserKantenBezug).orElse(new ArrayList<>()),
			netzbezugResult.map(FahrradrouteNetzbezugResult::getGeometry).orElse(null),
			netzbezugResult.map(FahrradrouteNetzbezugResult::getProfilEigenschaften).orElse(new ArrayList<>()),
			fahrradroutenMatchingAndRoutingInformationBuilder.build());
	}

	private Fahrradroute createFahradrouteFromImportedToubizRoute(ImportedToubizRoute importedToubizRoute,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		LineString netzbezugLineString,
		List<LinearReferenzierteProfilEigenschaften> profilEigenschaften,
		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation) {

		Geometry iconLocation = null;
		if (importedToubizRoute.getOriginalGeometrie() != null) {
			iconLocation = ((LineString) importedToubizRoute.getOriginalGeometrie()).getStartPoint();
			iconLocation.setSRID(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		}

		return new Fahrradroute(
			importedToubizRoute.getToubizId(),
			FahrradrouteTyp.TOUBIZ_ROUTE,
			importedToubizRoute.getName(),
			importedToubizRoute.getBeschreibung(),
			importedToubizRoute.getKurzbezeichnung(),
			importedToubizRoute.getInfo(),
			importedToubizRoute.getOffizielleLaenge(),
			importedToubizRoute.getTourenkategorie(),
			Kategorie.TOURISTISCHE_ROUTE,
			importedToubizRoute.getHomepage(),
			verwaltungseinheitService.getToubiz(),
			importedToubizRoute.getEmailAnsprechpartner(),
			importedToubizRoute.getLizenz(),
			importedToubizRoute.getZuletztBearbeitet(),
			importedToubizRoute.getLinksZuWeiterenMedien(),
			importedToubizRoute.getLizenzNamensnennung(),
			importedToubizRoute.getOriginalGeometrie(),
			iconLocation,
			abschnittsweiserKantenBezug,
			netzbezugLineString,
			profilEigenschaften,
			fahrradroutenMatchingAndRoutingInformation);
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Importiert Farradrouten aus Toubiz und matchst diese auf das Netz. Bestehende Routen werden aktualisiert, nicht mehr vorhandene entfernt. Bei Landesradfernwegen werden nur Attribute aktualisiert, keine Geometrien.",
			"Fahrradrouten mit Toubiz-ID verändern sich im Netzbezug und ihren Attributen.",
			"Profilinformationen sind hiernach ggf. veraltet. Sollte nach allen netzverändernden Jobs laufen.",
			JobExecutionDurationEstimate.SHORT);
	}
}
