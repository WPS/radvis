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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.input.BOMInputStream;

import com.opencsv.bean.CsvToBeanBuilder;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.entity.UmsetzungsstandImportStatistik;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandCsvZeile;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UmsetzungsstandImportJob extends AbstractJob {

	private final Path csvFilePath;
	private final MassnahmeService massnahmeService;
	private final UmsetzungsstandCsvZeileMapper umsetzungsstandCsvZeileMapper;
	private final BenutzerService benutzerService;
	private final List<Verwaltungseinheit> verwaltungseinheiten;

	public UmsetzungsstandImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		Path csvFilePath, MassnahmeService massnahmeService, BenutzerService benutzerService,
		VerwaltungseinheitService verwaltungseinheitService) {
		super(jobExecutionDescriptionRepository);
		this.csvFilePath = csvFilePath;
		this.massnahmeService = massnahmeService;
		this.umsetzungsstandCsvZeileMapper = new UmsetzungsstandCsvZeileMapper();
		this.benutzerService = benutzerService;
		this.verwaltungseinheiten = verwaltungseinheitService.getAll();
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.UMSETZUNGSSTAND_IMPORT_JOB)
	public JobExecutionDescription run() {
		return super.run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.UMSETZUNGSSTAND_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		log.info("Lese CSV-Datei von " + this.csvFilePath);
		List<UmsetzungsstandCsvZeile> umsetzungsstandCsvZeilen = this.leseCsvEin();
		return this.processCsvZeilen(umsetzungsstandCsvZeilen);
	}

	Optional<JobStatistik> processCsvZeilen(List<UmsetzungsstandCsvZeile> umsetzungsstandCsvZeilen) {
		UmsetzungsstandImportStatistik statistik = new UmsetzungsstandImportStatistik();
		statistik.setAnzahlAllerCsvZeilen(umsetzungsstandCsvZeilen.size());

		umsetzungsstandCsvZeilen = this.filtereNurUnbeantworteteFragen(umsetzungsstandCsvZeilen);
		statistik.setAnzahlGefilterterCsvZeilen(umsetzungsstandCsvZeilen.size());

		this.reformatiereMassnahmenIds(umsetzungsstandCsvZeilen);
		umsetzungsstandCsvZeilen = this.entferneDuplikate(umsetzungsstandCsvZeilen);
		statistik.setAnzahlDeduplizierterCsvZeilen(umsetzungsstandCsvZeilen.size());

		umsetzungsstandCsvZeilen.forEach(umsetzungsstandCsvZeile -> {
			String massnahmenPaketId = umsetzungsstandCsvZeile.getMassnahmennummer();
			// Es kann pro PaketId bis zu zwei Maßnahmen geben: Eine für Start- und eine für Zielstandard
			List<Massnahme> massnahmen = this.massnahmeService.getMassnahmeByPaketId(massnahmenPaketId);
			if (massnahmen.size() == 0) {
				statistik.addNichtGefundeneMassnahmenId(massnahmenPaketId);
				return;
			}
			statistik.incrementAnzahlGefundenerZugehoerigerMassnahmenIds();
			massnahmen.forEach(massnahme -> {
				statistik.incrementAnzahlGefundenerZugehoerigerMassnahmen();
				Umsetzungsstand umsetzungsstand = massnahme.getUmsetzungsstand()
					.orElseThrow(() -> new RuntimeException(
						"Datenfehler: Kein Umsetzungsstand für RadNETZ-Massnahme gefunden. Paket-ID "
							+ massnahmenPaketId));
				UmsetzungsstandStatus status = umsetzungsstand.getUmsetzungsstandStatus();
				if (status != null && status != UmsetzungsstandStatus.NEU_ANGELEGT
					&& status != UmsetzungsstandStatus.IMPORTIERT) {
					// Der Umsetzungsstand wurde bereits per Hand aktualisiert oder ist zumindest angefordert und wird
					// daher vom Import nicht mehr überschrieben
					return;
				}
				this.pruefeUndAendereUmsetzungErfolgt(umsetzungsstandCsvZeile, massnahme, statistik);
				this.updateMassnahmeBaulastZustaendigerIfKorrigiert(umsetzungsstandCsvZeile, massnahme, statistik);
				this.updateUmsetzungsstand(umsetzungsstand, umsetzungsstandCsvZeile, statistik);
				statistik.incrementAnzahlAktualisierterUmsetzungsstaende();

			});
		});
		log.info(statistik.toString());
		log.info("Für folgende Maßnahmen-IDs aus den Quelldaten wurde keine Maßnahme in der DB gefunden: "
			+ statistik.getNichtGefundeneMassnahmenIds());
		log.warn(
			"Konnte Umsetzungsstatus folgender Maßnahmen nicht auf \"Umgesetzt\" setzen, da zwischenzeitlich manuelle"
				+ " Änderungen an der Maßnahme durchgeführt wurden: "
				+ statistik.getStatusNichtGeaendertWegenManuellerAenderung());
		log.warn("Konnte Umsetzungsstatus folgender Maßnahmen nicht auf \"Umgesetzt\" setzen, da Pflichtfelder "
			+ "(Baulastträger, Durchführungszeitraum, Handlungsverantwortlicher) in der Maßnahme fehlen: "
			+ statistik.getStatusNichtGeaendertWegenInkonsistenz());
		log.warn("Konnte Kosten von folgenden Maßnahmen nicht parsen: \n"
			+ statistik.getKostenMappingFehlerFormattedString());
		log.warn("Konnte UmsetzungErfolgt von folgenden Maßnahmen nicht parsen: \n"
			+ statistik.getUmsetzungErfolgtMappingFehlerFormattedString());
		return Optional.of(statistik);
	}

	private void updateMassnahmeBaulastZustaendigerIfKorrigiert(UmsetzungsstandCsvZeile umsetzungsstandCsvZeile,
		Massnahme massnahme,
		UmsetzungsstandImportStatistik statistik) {
		if (!massnahme.getBenutzerLetzteAenderung().getId().equals(benutzerService.getTechnischerBenutzer().getId())) {
			return;
		}

		umsetzungsstandCsvZeileMapper.mapKorrigierterBaulastZustaendiger(umsetzungsstandCsvZeile,
			this.verwaltungseinheiten)
			.ifPresent((Verwaltungseinheit neuerBaulastZustaendiger) -> {
				if (massnahme.getBaulastZustaendiger().isPresent() && massnahme.getBaulastZustaendiger().get()
					.equals(neuerBaulastZustaendiger)) {
					return;
				}

				massnahme.updateBaulastZustaendiger(neuerBaulastZustaendiger);
				statistik.incrementAnzahlAktualisierteBaulastZustaendige();
			});
	}

	private List<UmsetzungsstandCsvZeile> leseCsvEin() {
		try {
			String path = this.csvFilePath.toString();
			BOMInputStream.Builder inputStreamBuilder = BOMInputStream
				.builder()
				.setInputStream(new FileInputStream(path));
			InputStreamReader reader = new InputStreamReader(inputStreamBuilder.get(), StandardCharsets.UTF_8);
			return new CsvToBeanBuilder<UmsetzungsstandCsvZeile>(
				reader).withType(UmsetzungsstandCsvZeile.class).withSeparator(';').withQuoteChar('"').build().parse();
		} catch (FileNotFoundException e) {
			throw new RuntimeException("CSV-Datei an Pfad " + this.csvFilePath + " nicht gefunden", e);
		} catch (IOException e) {
			throw new RuntimeException("Einlesen der Datei " + this.csvFilePath + " fehlgeschlagen", e);
		}
	}

	private List<UmsetzungsstandCsvZeile> filtereNurUnbeantworteteFragen(
		List<UmsetzungsstandCsvZeile> umsetzungsstandCsvZeilen) {
		return umsetzungsstandCsvZeilen.stream().filter(u -> !u.isAlleFragenUnbeantwortet()).collect(
			Collectors.toList());
	}

	private void reformatiereMassnahmenIds(List<UmsetzungsstandCsvZeile> umsetzungsstandCsvZeilen) {
		umsetzungsstandCsvZeilen.forEach(
			u -> u.setMassnahmennummer(MassnahmenPaketIdExtractor.normalize(u.getMassnahmennummer())));
	}

	private List<UmsetzungsstandCsvZeile> entferneDuplikate(List<UmsetzungsstandCsvZeile> umsetzungsstandCsvZeilen) {
		Map<String, UmsetzungsstandCsvZeile> massnahmenIdMap = new HashMap<>();
		umsetzungsstandCsvZeilen.forEach(u -> {
			if (!massnahmenIdMap.containsKey(u.getMassnahmennummer())) {
				massnahmenIdMap.put(u.getMassnahmennummer(), u);
			} else {
				if (u.getAnzahlBeantworteterFragen() > massnahmenIdMap.get(u.getMassnahmennummer())
					.getAnzahlBeantworteterFragen()) {
					massnahmenIdMap.put(u.getMassnahmennummer(), u);
				}
			}
		});
		return new ArrayList<>(massnahmenIdMap.values());
	}

	private void pruefeUndAendereUmsetzungErfolgt(UmsetzungsstandCsvZeile umsetzungsstandCsvZeile, Massnahme massnahme,
		UmsetzungsstandImportStatistik statistik) {
		boolean umsetzungErfolgt = this.umsetzungsstandCsvZeileMapper.mapUmsetzungErfolgt(
			umsetzungsstandCsvZeile, statistik);
		if (umsetzungErfolgt) {
			boolean letzteAenderungAnMassnahmeDurchImport = massnahme.getBenutzerLetzteAenderung().getId()
				.equals(benutzerService.getTechnischerBenutzer().getId());
			if (!letzteAenderungAnMassnahmeDurchImport) {
				statistik.addStatusNichtGeaendertWegenManuellerAenderung(massnahme.getMassnahmenPaketId().getValue(),
					massnahme.getId());
				return;
			}
			try {
				massnahme.updateMassnahmeUmgesetztFuerUmsetzungsstandabfrageImport();
				statistik.incrementAnzahlMassnahmenAufUmgesetztGesetzt();
			} catch (Throwable e) {
				statistik.addStatusNichtGeaendertWegenInkonsistenz(massnahme.getMassnahmenPaketId().getValue(),
					massnahme.getId());
			}
		}
	}

	private void updateUmsetzungsstand(Umsetzungsstand umsetzungsstand, UmsetzungsstandCsvZeile zeile,
		UmsetzungsstandImportStatistik statistik) {
		LocalDateTime letzteAenderung = LocalDateTime.now();
		Benutzer technischerBenutzer = benutzerService.getTechnischerBenutzer();
		umsetzungsstand.updateFromImport(
			this.umsetzungsstandCsvZeileMapper.mapUmsetzungGemaessMassnahmenblatt(zeile),
			letzteAenderung,
			technischerBenutzer,
			this.umsetzungsstandCsvZeileMapper.mapGrundFuerAbweichungZumMassnahmenblatt(zeile),
			this.umsetzungsstandCsvZeileMapper.mapPruefungQualitaetsstandardsErfolgt(zeile),
			this.umsetzungsstandCsvZeileMapper.mapBeschreibungAbweichenderMassnahme(zeile),
			this.umsetzungsstandCsvZeileMapper.mapKostenDerMassnahme(zeile, statistik),
			this.umsetzungsstandCsvZeileMapper.mapGrundFuerNichtUmsetzungDerMassnahme(zeile),
			this.umsetzungsstandCsvZeileMapper.mapAnmerkung(zeile));
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Aktualisiert Umsetzungsstand und einige Attribute an Maßnahmen aus konfigurierbarer CSV-Datei",
			"",
			"",
			"Nicht ausführen, wird nicht mehr produktiv benötigt.",
			JobExecutionDurationEstimate.UNKNOWN
		);
	}
}
