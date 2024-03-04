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

package de.wps.radvis.backend.massnahme.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RandomMassnahmenGenerierenJob extends AbstractJob {
	private static final int ANZAHL_RANDOM_MASSNAHMEN = 100;

	private final KantenRepository kantenRepository;
	private final VerwaltungseinheitRepository verwaltungseinheitRepository;
	private final MassnahmeService massnahmeService;
	private final BenutzerResolver benutzerResolver;
	private final Random random;

	private List<Long> alleKantenIds;
	private List<Verwaltungseinheit> alleOrganisationen;

	public RandomMassnahmenGenerierenJob(MassnahmeService massnahmeService,
		KantenRepository kantenRepository,
		VerwaltungseinheitRepository verwaltungseinheitRepository,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		BenutzerResolver benutzerResolver) {
		super(jobExecutionDescriptionRepository);
		this.massnahmeService = massnahmeService;
		this.random = new Random();
		this.benutzerResolver = benutzerResolver;
		this.kantenRepository = kantenRepository;
		this.verwaltungseinheitRepository = verwaltungseinheitRepository;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MASSNAHME_IMPORT_JOB)
	public JobExecutionDescription run() {
		return super.run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MASSNAHME_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	private String createRandomString() {
		return Long.toHexString(Double.doubleToLongBits(random.nextDouble()));
	}

	private Set<Massnahmenkategorie> createRandomMassnahmenkategorie() {
		Massnahmenkategorie[] alleMassnahmenkategories = Massnahmenkategorie.values();
		int randomSize = random.nextInt(2) + 1; // 0 geht nicht weil Pflichtfeld

		// Etwas unschoene Umsetzung, dass es nur eine Massnahmekategorie pro Oberkategorie geben darf,
		// aber dieser Job ist ja eh nur zum Erstellen von Testmassnahmen
		Set<Massnahmenkategorie> randomMassnahmenkategorien = new HashSet<>();
		do {
			randomMassnahmenkategorien.clear();
			for (int i = 0; i < randomSize; i++) {
				randomMassnahmenkategorien.add(
					alleMassnahmenkategories[random.nextInt(alleMassnahmenkategories.length)]);
			}
		} while (!Massnahme.hatNurEineMassnahmenkategorieProOberkategorie(randomMassnahmenkategorien));
		return randomMassnahmenkategorien;
	}

	private MassnahmeNetzBezug createRandomNetzbezug() {
		Long randomKanteId = alleKantenIds.get(random.nextInt(alleKantenIds.size()));
		Kante randomKante = kantenRepository.findById(randomKanteId).get();

		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
			randomKante,
			LinearReferenzierterAbschnitt.of(0.0, 1.0), Seitenbezug.LINKS);
		return new MassnahmeNetzBezug(Set.of(abschnittsweiserKantenSeitenBezug),
			Set.of(),
			Set.of(randomKante.getVonKnoten(), randomKante.getNachKnoten()));
	}

	private Umsetzungsstatus createRandomUmsetzungsstatus() {
		Umsetzungsstatus[] alleStatusse = Umsetzungsstatus.values();
		return alleStatusse[random.nextInt(alleStatusse.length)];
	}

	private Durchfuehrungszeitraum createRandomDurchfuehrungszeitraum() {
		Integer jahr = 2000 + random.nextInt(999);

		return Durchfuehrungszeitraum.of(jahr);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		alleKantenIds = StreamSupport
			.stream(kantenRepository.getAllKanteIds().spliterator(), false)
			.collect(Collectors.toList());

		alleOrganisationen = verwaltungseinheitRepository.findAll();

		for (int i = 0; i < ANZAHL_RANDOM_MASSNAHMEN; i++) {
			Konzeptionsquelle randomKonzeptionsquelle = createRandomKonzeptionsquelle();

			// Nur Massnahmen mit Konzeptionsquelle 'RadNETZ-Maßnahme' koennen einen Umsetzungsstand haben
			Umsetzungsstand umsetzungsstand = null;
			if (randomKonzeptionsquelle == Konzeptionsquelle.RADNETZ_MASSNAHME) {
				// leeren (nicht ausgefuellten) Umsetzungsstand erzeugen
				umsetzungsstand = new Umsetzungsstand();
			}

			Massnahme massnahme = Massnahme.builder()
				.bezeichnung(Bezeichnung.of(createRandomString()))
				.massnahmenkategorien(createRandomMassnahmenkategorie())
				.netzbezug(createRandomNetzbezug())
				.umsetzungsstatus(createRandomUmsetzungsstatus())
				.veroeffentlicht(random.nextBoolean())
				.planungErforderlich(random.nextBoolean())
				.durchfuehrungszeitraum(createRandomDurchfuehrungszeitraum())
				.baulastZustaendiger(createRandomOrganisation())
				.letzteAenderung(LocalDateTime.now())
				.benutzerLetzteAenderung(benutzer)
				.sollStandard(createRandomSollStandard())
				.handlungsverantwortlicher(createRandomHandlungsverantwortlicher())
				.konzeptionsquelle(randomKonzeptionsquelle)
				.umsetzungsstand(umsetzungsstand)
				.sonstigeKonzeptionsquelle(createRandomString())
				.netzklassen(createRandomNetzklassen(random))
				.dokumentListe(new DokumentListe())
				.kommentarListe(new KommentarListe())
				.build();

			massnahmeService.saveMassnahme(massnahme);
		}

		log.info("RandomMassnahmenGenerierenJob abgeschlossen.");
		alleKantenIds.clear();
		return Optional.empty();
	}

	static Set<Netzklasse> createRandomNetzklassen(Random random) {
		Netzklasse[] alleNetzklassen = Netzklasse.values();
		int randomSize = random.nextInt(3);

		// wenn wir keine setzen, dann 50/50 setzen wir entweder eine leere Liste oder null
		if (randomSize == 0 && random.nextBoolean()) {
			return new HashSet<>();
		}

		Set<Netzklasse> randomNetzklassen = new HashSet<>();
		for (int i = 0; i < randomSize; i++) {
			randomNetzklassen.add(alleNetzklassen[random.nextInt(alleNetzklassen.length)]);
		}

		return randomNetzklassen;
	}

	private Verwaltungseinheit createRandomOrganisation() {
		return alleOrganisationen.get(random.nextInt(alleOrganisationen.size()));
	}

	private SollStandard createRandomSollStandard() {
		SollStandard[] alleStatusse = SollStandard.values();
		return alleStatusse[random.nextInt(alleStatusse.length)];
	}

	private Handlungsverantwortlicher createRandomHandlungsverantwortlicher() {
		Handlungsverantwortlicher[] alleStatusse = Handlungsverantwortlicher.values();
		return alleStatusse[random.nextInt(alleStatusse.length)];
	}

	private Konzeptionsquelle createRandomKonzeptionsquelle() {
		Konzeptionsquelle[] alleStatusse = Konzeptionsquelle.values();
		return alleStatusse[random.nextInt(alleStatusse.length)];
	}
}
