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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import static org.valid4j.Assertive.require;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributProjektionsJobStatistik;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.Attributprojektionsbeschreibung;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KanteDublette;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AttributProjektionsJob extends AbstractJob {

	private final AttributProjektionsService attributProjektionsService;
	private final AttributeAnreicherungsService attributAnreicherungsService;
	private final AttributProjektionsStatistikService attributProjektionsStatistikService;
	private final KantenDublettenPruefungService kantenDublettenPruefungService;
	private final NetzService netzService;
	private final ImportedFeaturePersistentRepository importedFeaturePersistentRepository;

	private final DLMConfigurationProperties config;

	private final NetzfehlerRepository netzfehlerRepository;

	private final EntityManager entityManager;

	private final QuellSystem quellSystem;

	public AttributProjektionsJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		AttributProjektionsService attributProjektionsService,
		AttributeAnreicherungsService attributAnreicherungsService,
		AttributProjektionsStatistikService attributProjektionsStatistikService,
		NetzfehlerRepository netzfehlerRepository,
		KantenDublettenPruefungService kantenDublettenPruefungService,
		NetzService netzService,
		ImportedFeaturePersistentRepository importedFeaturePersistentRepository,
		DLMConfigurationProperties config, EntityManager entityManager, QuellSystem quellSystem) {
		super(jobExecutionDescriptionRepository);

		require(attributProjektionsService, Matchers.notNullValue());
		require(attributProjektionsStatistikService, Matchers.notNullValue());
		require(netzfehlerRepository, Matchers.notNullValue());
		require(kantenDublettenPruefungService, Matchers.notNullValue());
		require(netzService, Matchers.notNullValue());
		require(importedFeaturePersistentRepository, Matchers.notNullValue());
		require(config, Matchers.notNullValue());
		require(quellSystem, Matchers.notNullValue());
		require(quellSystem != QuellSystem.DLM);

		this.attributProjektionsService = attributProjektionsService;
		this.attributAnreicherungsService = attributAnreicherungsService;
		this.attributProjektionsStatistikService = attributProjektionsStatistikService;
		this.netzfehlerRepository = netzfehlerRepository;
		this.kantenDublettenPruefungService = kantenDublettenPruefungService;
		this.netzService = netzService;
		this.importedFeaturePersistentRepository = importedFeaturePersistentRepository;
		this.config = config;
		this.entityManager = entityManager;
		this.quellSystem = quellSystem;
	}

	@Override
	public String getName() {
		return super.getName() + " " + quellSystem;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.ATTRIBUT_PROJEKTION_JOB)
	public JobExecutionDescription run() {
		return super.run();
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.ATTRIBUT_PROJEKTION_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	@Transactional
	protected Optional<JobStatistik> doRun() {
		log.info("Start Projektionsjob für Quellnetz {}", quellSystem);

		netzfehlerRepository.deleteAllByjobZuordnung(getName());
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();

		statistik.reset();

		// Ermöglicht, dass wir jede DLM Kante nur einmal abarbeiten, auch wenn sie in mehreren Partitionen ist
		Set<Long> dlmKantenBereitsAbgearbeitet = new HashSet<>();
		AtomicInteger numberPartitionsDone = new AtomicInteger();
		numberPartitionsDone.set(0);

		List<Envelope> partitionen = getPartitionen(this.config.getExtentProperty(), this.config.getPartitionenX());

		partitionen.forEach(
			partition -> {
				log.info("Starting partition number {}...", numberPartitionsDone.get() + 1);
				Envelope biggerExtentForQuellnetz = partition.copy();
				biggerExtentForQuellnetz.expandBy(1000);

				Set<Kante> DLMKanten = netzService
					.getKantenInBereichNachQuelle(partition, QuellSystem.DLM)
					.filter(dlmKante -> !dlmKantenBereitsAbgearbeitet.contains(dlmKante.getId()))
					.collect(Collectors.toSet());

				List<KanteDublette> dubletten = kantenDublettenPruefungService.findDubletten(
					DLMKanten,
					netzService
						.getKantenInBereichNachQuelleUndIsAbgebildet(biggerExtentForQuellnetz, quellSystem));

				dlmKantenBereitsAbgearbeitet.addAll(DLMKanten.stream().map(Kante::getId).collect(Collectors.toSet()));
				log.info("Current free memory: {} / {}", Runtime.getRuntime().freeMemory(),
					Runtime.getRuntime().totalMemory());

				if (dubletten.isEmpty()) {
					log.info("Partition nummer {} hatte keine Dubletten ", numberPartitionsDone.getAndIncrement() + 1);
					statistik.dlmKantenAbgearbeitet = dlmKantenBereitsAbgearbeitet.size();
					return;
				}

				statistik.dubletten += dubletten.size();
				statistik.grundnetzKantenInDubletten += dubletten.stream().map(KanteDublette::getZielnetzKante)
					.collect(Collectors.toSet()).size();
				statistik.laengeDerDlmUebereinstimmungInMeter += dubletten.stream()
					.map(KanteDublette::getZielnetzUeberschneidung)
					.mapToDouble(LineString::getLength).sum();

				log.info("Es wird versucht aus {} Kantendubletten AttributProjektionen zu erstellen.",
					dubletten.size());

				Collection<Attributprojektionsbeschreibung> attributprojektionsbeschreibungen = attributProjektionsService
					.projiziereAttributeAufGrundnetzKanten(dubletten, statistik, getName());

				// Wir brauchen die Dubletten ab diesen Schritt nicht mehr
				dubletten.clear();
				dubletten = null;

				fuelleStatistikMitAttributprojektionsbeschreibungen(statistik, attributprojektionsbeschreibungen);

				log.info("Es wird versucht GrundnetzKanten aus {} AttributProjektionen anzureichern.",
					attributprojektionsbeschreibungen.size());

				Map<Long, Double> quellnetzIDsMitAnreicherungsanteil = attributAnreicherungsService
					.reichereGrundnetzKantenMitAttributenAn(attributprojektionsbeschreibungen, quellSystem,
						statistik, getName());

				log.info("Schreibe Projizierten Anteil auf {} Quellnetzfeatures",
					quellnetzIDsMitAnreicherungsanteil.size());
				reichereFeaturesMitAnteilAn(quellnetzIDsMitAnreicherungsanteil);

				statistik.dlmKantenAbgearbeitet = dlmKantenBereitsAbgearbeitet.size();

				entityManager.flush();
				entityManager.clear();
				log.info("Finished partition number {} ", numberPartitionsDone.getAndIncrement() + 1);
			});

		if (quellSystem.equals(QuellSystem.RadNETZ)) {
			// Nach der initialen Projektion des RadNETZ auf DLM haben zunächst alle RadNETZ-Kanten Grundnetzstatus
			setzeRadNETZKantenAlsGrundnetzKanten();
		}

		Set<Long> radNETZGrundnetzKantenBereitsAbgearbeitet = new HashSet<>();
		partitionen.forEach(partition -> attributProjektionsStatistikService
			.ueberpruefeTopologieDerNetzklassen(partition, radNETZGrundnetzKantenBereitsAbgearbeitet, statistik,
				getName()));

		statistik.laengeDerUebernommenenQuelleDieProjiziertWurde = attributProjektionsStatistikService
			.berechneAnteilFeaturesProjiziert(quellSystem);

		log.info(statistik.toString());
		return Optional.of(statistik);
	}

	void setzeRadNETZKantenAlsGrundnetzKanten() {
		entityManager.createQuery("UPDATE Kante SET isGrundnetz = true WHERE quelle = :quelleRadNETZ")
			.setParameter("quelleRadNETZ", QuellSystem.RadNETZ)
			.executeUpdate();
	}

	private void reichereFeaturesMitAnteilAn(Map<Long, Double> quellnetzIDsMitAnreicherungsanteil) {
		HashMap<String, Double> quellTechnischeIDs = new HashMap<>();
		netzService.findKanteByQuelle(quellSystem)
			.filter(kante -> quellnetzIDsMitAnreicherungsanteil.containsKey(kante.getId()))
			.forEach(kante -> quellTechnischeIDs.put(kante.getUrsprungsfeatureTechnischeID(),
				quellnetzIDsMitAnreicherungsanteil.get(kante.getId())));

		String featureType = getFeatureType(quellSystem);

		importedFeaturePersistentRepository.getAllByQuelleAndArtAndGeometryType(quellSystem,
			Art.Strecke, featureType)
			.filter(feature -> quellTechnischeIDs.containsKey(feature.getTechnischeId()))
			.forEach(feature -> {
				feature.setAnteilProjiziert(quellTechnischeIDs.get(feature.getTechnischeId()));
			});
	}

	private String getFeatureType(QuellSystem quellSystem) {
		if (quellSystem.equals(QuellSystem.RadNETZ)) {
			return ImportedFeaturePersistentRepository.LINESTRING;
		} else if (quellSystem.equals(QuellSystem.RadwegeDB)) {
			return ImportedFeaturePersistentRepository.MULTILINESTRING;
		} else {
			return ImportedFeaturePersistentRepository.LINESTRING;
		}
	}

	private void fuelleStatistikMitAttributprojektionsbeschreibungen(AttributProjektionsJobStatistik statistik,
		Collection<Attributprojektionsbeschreibung> attributprojektionsbeschreibungen) {
		statistik.projektionen += attributprojektionsbeschreibungen.size();
		for (Attributprojektionsbeschreibung attributprojektionsbeschreibung : attributprojektionsbeschreibungen) {

			Collection<Double> anteile = attributprojektionsbeschreibung
				.getPotentiellInkonsistenteProjizierteKantenattributeZuAnteil().values();

			if (anteile.size() == 1) {
				statistik.projektionenMitNurEinemSegment++;
			}

			if (anteile.size() > 2) {
				statistik.projektionenMitMehrAls2Segmenten++;
			}

			Double maxAnteil = attributprojektionsbeschreibung
				.getPotentiellInkonsistenteProjizierteKantenattributeZuAnteil()
				.entrySet().stream()
				.max((entry1, entry2) -> (int) (entry1.getValue() - entry2.getValue()))
				.get().getValue();

			statistik.addProjektionMitAnteilGleicherKantenAttribute(maxAnteil);

			statistik.laengeDesDLMAufDasProjiziertWurdeInMeter += berechneLaengeDesDLMAufDasProjiziertWurdeInMeter(
				attributprojektionsbeschreibung.getZielnetzKante().getGeometry().getLength(),
				attributprojektionsbeschreibung.getPotentiellInkonsistenteProjizierteNetzklassen());
		}
	}

	double berechneLaengeDesDLMAufDasProjiziertWurdeInMeter(Double geometrylength,
		Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> projizierteNetzklassen) {

		List<LinearReferenzierterAbschnitt> anteileDerKanteMitProjizierterNetzklasse = projizierteNetzklassen.values()
			.stream()
			.flatMap(List::stream)
			.sorted(LinearReferenzierterAbschnitt.vonZuerst)
			.collect(Collectors.toList());

		double relativeLength = 0;
		if (anteileDerKanteMitProjizierterNetzklasse.size() == 1) {
			LinearReferenzierterAbschnitt current = anteileDerKanteMitProjizierterNetzklasse.get(0);
			relativeLength = current.relativeLaenge();
		} else if (anteileDerKanteMitProjizierterNetzklasse.size() > 1) {
			LinearReferenzierterAbschnitt current = anteileDerKanteMitProjizierterNetzklasse.get(0);

			for (int i = 1; i < anteileDerKanteMitProjizierterNetzklasse.size(); i++) {
				LinearReferenzierterAbschnitt next = anteileDerKanteMitProjizierterNetzklasse.get(i);
				if (current.union(next).isPresent()) {
					current = current.union(next).get();
				} else {
					relativeLength += current.relativeLaenge();
					current = next;
				}
				if (i == anteileDerKanteMitProjizierterNetzklasse.size() - 1) {
					relativeLength += current.relativeLaenge();
				}
			}
		}

		return geometrylength * relativeLength;
	}
}
