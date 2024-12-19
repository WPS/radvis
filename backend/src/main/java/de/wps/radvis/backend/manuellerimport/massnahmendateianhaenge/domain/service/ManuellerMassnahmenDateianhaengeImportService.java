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

package de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.service.ZipService;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportDatei;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.valueObject.MassnahmenDateianhaengeImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.valueObject.MassnahmenDateianhaengeMappingHinweis;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ManuellerMassnahmenDateianhaengeImportService {

	private final ManuellerImportService manuellerImportService;

	private final ZipService zipService;
	private final CsvRepository csvRepository;

	private final MassnahmeRepository massnahmenRepository;
	private final VerwaltungseinheitRepository verwaltungseinheitRepository;

	public Optional<MassnahmenDateianhaengeImportSession> getMassnahmenDateianhaengeImportSession(Benutzer benutzer) {
		return manuellerImportService.findImportSessionFromBenutzer(benutzer,
			MassnahmenDateianhaengeImportSession.class);
	}

	private MultiPolygon getVereinigtenBereich(List<Long> gebietskoerperschaftIds) {
		return verwaltungseinheitRepository.getVereintenBereich(gebietskoerperschaftIds);
	}

	public File createTemporaryZipCopy(MassnahmenDateianhaengeImportSession session, MultipartFile file) {
		try {
			return this.zipService.createTemporaryZipCopy("massnahmen_dateianhaenge", file);
		} catch (Exception e) {
			session.addLogEintrag(ImportLogEintrag.ofError(e.getMessage()));
			log.error("Fehler bei ManuellerMassnahmenDateianhaengeImport", e);
		}
		return null;
	}

	@Async
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public void ladeDateien(MassnahmenDateianhaengeImportSession session, File file) {
		require(session, notNullValue());
		require(file, notNullValue());

		session.setExecuting(true);

		try {
			File unzippedFile = zipService.unzip("massnahmen_dateianhaenge", file);

			File[] hauptEbeneDirs = unzippedFile.listFiles();
			if (hauptEbeneDirs == null || hauptEbeneDirs.length != 1 || hauptEbeneDirs[0].listFiles() == null) {
				throw new Exception("Das hochgeladene ZIP-Archiv hat eine ungültige Ordnerstruktur");
			}

			List<File> massnahmenDirs = Arrays.stream(hauptEbeneDirs[0].listFiles()).filter(File::isDirectory).toList();
			if (massnahmenDirs.isEmpty()) {
				throw new Exception("Das hochgeladene ZIP-Archiv enthält keine Maßnahmen");
			}

			Map<String, MassnahmenDateianhaengeImportZuordnung> zuordnungen = massnahmenDirs.stream()
				.map(
					massnahmenDir -> this.getMassnahmenDateianhaengeZuordnung(
						massnahmenDir,
						session.getBereich(),
						session.getKonzeptionsquelle(),
						session.getSollStandard()))
				.collect(Collectors.toMap(MassnahmenDateianhaengeImportZuordnung::getOrdnerName, Function.identity()));

			session.setZuordnungen(zuordnungen);
			session.setSchritt(MassnahmenDateianhaengeImportSession.FEHLER_UEBERPRUEFEN);
		} catch (Exception e) {
			session.addLogEintrag(ImportLogEintrag.ofError(e.getMessage()));
			log.error("Fehler bei ManuellerMassnahmenDateianhaengeImport", e);
		} finally {
			session.setExecuting(false);
		}
	}

	private MassnahmenDateianhaengeImportZuordnung getMassnahmenDateianhaengeZuordnung(
		File massnahmenDirectory,
		MultiPolygon bereich,
		Konzeptionsquelle konzeptionsquelle,
		SollStandard sollStandard) {
		require(massnahmenDirectory.isDirectory(), "Zuordnungen zu Maßnahmen können nur für Ordner erfolgen");

		String directoryName = massnahmenDirectory.getName();

		MassnahmenDateianhaengeImportZuordnung zuordnung = new MassnahmenDateianhaengeImportZuordnung(
			directoryName);
		List<File> dateien = this.getFilesInFolder(massnahmenDirectory);
		zuordnung.setDateien(dateien.stream().map(MassnahmenDateianhaengeImportDatei::new)
			.collect(Collectors.toMap(MassnahmenDateianhaengeImportDatei::getDateiname, Function.identity())));

		if (!MassnahmeKonzeptID.isValid(directoryName)) {
			// Ungültige KonzeptId
			zuordnung.setStatus(MassnahmenDateianhaengeImportZuordnungStatus.IGNORIERT);
			zuordnung.setHinweis(
				MassnahmenDateianhaengeMappingHinweis.ofError("Ordnername ist keine gültige Maßnahmen-ID"));
			return zuordnung;
		}

		if (dateien.isEmpty()) {
			// Ordner ist leer
			zuordnung.setStatus(MassnahmenDateianhaengeImportZuordnungStatus.IGNORIERT);
			zuordnung.setHinweis(
				MassnahmenDateianhaengeMappingHinweis.ofError("Ordner ist leer"));
			return zuordnung;
		}

		List<Massnahme> massnahmen = massnahmenRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(directoryName), konzeptionsquelle);

		if (sollStandard != null) {
			// Ist ein Soll-Standard angegeben, soll diese mitgefiltert werden zur eindeutigen Zuordnung
			massnahmen = massnahmen.stream().filter(m -> m.getSollStandard().equals(sollStandard)).toList();
		}

		if (massnahmen.isEmpty()) {
			// Keine passende Massnahme gefunden
			zuordnung.setStatus(MassnahmenDateianhaengeImportZuordnungStatus.FEHLERHAFT);
			zuordnung.setHinweis(
				MassnahmenDateianhaengeMappingHinweis.ofError("Maßnahme '" + directoryName + "' wurde nicht gefunden"));
			return zuordnung;
		}

		if (massnahmen.size() > 1) {
			// Keine eindeutige Zuordnung möglich
			zuordnung.setStatus(MassnahmenDateianhaengeImportZuordnungStatus.FEHLERHAFT);
			zuordnung.setHinweis(
				MassnahmenDateianhaengeMappingHinweis.ofError(
					"Keine eindeutige Zuordnung möglich, da " + massnahmen.size()
						+ " potentielle Maßnahmen gefunden wurden"));
			return zuordnung;
		}

		Massnahme massnahme = massnahmen.get(0);
		if (!massnahme.getNetzbezug().getGeometrie().intersects(bereich)) {
			// Liegt nicht im ausgewählten Bereich
			zuordnung.setStatus(MassnahmenDateianhaengeImportZuordnungStatus.FEHLERHAFT);
			zuordnung.setHinweis(
				MassnahmenDateianhaengeMappingHinweis.ofError("Liegt nicht im gewählten Bereich"));
			return zuordnung;
		}

		if (massnahme.isArchiviert()) {
			zuordnung.setStatus(MassnahmenDateianhaengeImportZuordnungStatus.FEHLERHAFT);
			zuordnung.setHinweis(
				MassnahmenDateianhaengeMappingHinweis.ofError("Maßnahme ist bereits archiviert"));
			return zuordnung;

		}

		this.markPotentialDuplicateFiles(massnahme, zuordnung);

		zuordnung.setMassnahmeId(massnahme.getId());
		zuordnung.setStatus(MassnahmenDateianhaengeImportZuordnungStatus.ZUGEORDNET);
		return zuordnung;
	}

	private List<File> getFilesInFolder(File directory) {
		File[] files = directory.listFiles();
		return files != null ? Arrays.stream(files)
			.filter(File::isFile)
			.toList() : new ArrayList<>();
	}

	private void markPotentialDuplicateFiles(Massnahme massnahme, MassnahmenDateianhaengeImportZuordnung zuordnung) {
		zuordnung.getDateien().values().forEach(datei -> {
			if (massnahme.getDokumentListe().getDokumente().stream()
				.anyMatch(dokument -> dokument.getDateiname().equalsIgnoreCase(datei.getDatei().getName()))) {
				datei.markAsDuplicate();
			} else {
				datei.setSelected(true);
			}
		});
	}

	private String getNamen(List<Long> gebietskoerperschaftIds) {
		return verwaltungseinheitRepository.findAllDbViewsById(gebietskoerperschaftIds)
			.stream()
			.map(v -> Verwaltungseinheit.combineNameAndArt(v.getName(), v.getOrganisationsArt()))
			.collect(Collectors.joining(","));
	}

	@Async
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public void saveSelectedDateianhaengeToMassnahme(MassnahmenDateianhaengeImportSession session, Benutzer benutzer) {
		List<MassnahmenDateianhaengeImportZuordnung> filteredZuordnungen = session.getZuordnungen()
			.values().stream()
			.filter(zuordnung -> zuordnung.getStatus().equals(MassnahmenDateianhaengeImportZuordnungStatus.ZUGEORDNET)
				&& zuordnung.hasAnySelectedDateien())
			.toList();

		filteredZuordnungen.forEach(zuordnung -> {
			massnahmenRepository.findById(zuordnung.getMassnahmeId()).ifPresentOrElse(
				massnahme -> zuordnung.getDateien().values().stream()
					.filter(MassnahmenDateianhaengeImportDatei::isSelected)
					.forEach(datei -> {
						try {
							byte[] fileBytes = Files.readAllBytes(datei.getDatei().toPath());
							massnahme.getDokumentListe()
								.addOrReplaceDokumentWithEqualDateiname(
									new Dokument(datei.getDateiname(), benutzer, fileBytes, LocalDateTime.now()),
									datei.isDuplicate());
							datei.setApplied(true);
						} catch (IOException e) {
							zuordnung.setHinweis(MassnahmenDateianhaengeMappingHinweis.ofError(
								"Dateien konnten nicht gelesen werden, bitte starten Sie den Import erneut"));
						}
					}),
				() -> zuordnung.setHinweis(MassnahmenDateianhaengeMappingHinweis.ofError(
					"Dateien konnten nicht geschrieben werden, da die Maßnahme zwischenzeitlich gelöscht wurde")));

		});
		session.setSchritt(MassnahmenDateianhaengeImportSession.FEHLERPROTOKOLL_HERUNTERLADEN);
		session.setExecuting(false);
	}

	public MassnahmenDateianhaengeImportSession createSession(Benutzer benutzer, List<Long> gebietskoerperschaftenIds,
		Konzeptionsquelle konzeptionsquelle, SollStandard sollStandard) {

		MultiPolygon vereinigterBereich = getVereinigtenBereich(gebietskoerperschaftenIds);
		String bereichName = getNamen(gebietskoerperschaftenIds);

		return new MassnahmenDateianhaengeImportSession(
			benutzer,
			vereinigterBereich,
			bereichName,
			gebietskoerperschaftenIds,
			konzeptionsquelle,
			sollStandard);
	}

	public byte[] downloadFehlerprotokoll(MassnahmenDateianhaengeImportSession session) throws IOException {
		List<Map<String, String>> rows = new ArrayList<>();
		session.getZuordnungen().values().forEach(zuordnung -> rows.addAll(
			zuordnung.getDateien().isEmpty() ? List.of(convertZuordnungDateiToCsvRow(zuordnung, Optional.empty()))
				: zuordnung.getDateien().values().stream()
					.map(datei -> convertZuordnungDateiToCsvRow(zuordnung, Optional.of(datei))).toList()));
		return csvRepository.write(CsvData.of(rows, MassnahmenDateianhaengeImportSession.CsvHeader.ALL));
	}

	private Map<String, String> convertZuordnungDateiToCsvRow(
		MassnahmenDateianhaengeImportZuordnung zuordnung,
		Optional<MassnahmenDateianhaengeImportDatei> datei) {
		Map<String, String> row = new HashMap<>();

		row.put(MassnahmenDateianhaengeImportSession.CsvHeader.ORDNERNAME, zuordnung.getOrdnerName());

		row.put(MassnahmenDateianhaengeImportSession.CsvHeader.ORDNERSTATUS, zuordnung.getStatus().toString());

		row.put(MassnahmenDateianhaengeImportSession.CsvHeader.HINWEIS, zuordnung.getHinweis()
			.map(MassnahmenDateianhaengeMappingHinweis::text)
			.orElse(""));

		row.put(MassnahmenDateianhaengeImportSession.CsvHeader.DATEINAME,
			datei.map(MassnahmenDateianhaengeImportDatei::getDateiname)
				.orElse(""));

		row.put(MassnahmenDateianhaengeImportSession.CsvHeader.DATEI_DUPLIKAT,
			datei.map(MassnahmenDateianhaengeImportDatei::isDuplicate)
				.map(this::convertBooleanToCsvRepresentation)
				.orElse(""));

		row.put(MassnahmenDateianhaengeImportSession.CsvHeader.DATEI_AUSGEWAEHLT,
			datei.map(MassnahmenDateianhaengeImportDatei::isSelected)
				.map(this::convertBooleanToCsvRepresentation)
				.orElse(""));

		row.put(MassnahmenDateianhaengeImportSession.CsvHeader.DATEI_IMPORTIERT,
			datei.map(MassnahmenDateianhaengeImportDatei::isApplied)
				.map(this::convertBooleanToCsvRepresentation)
				.orElse(""));

		return row;
	}

	private String convertBooleanToCsvRepresentation(boolean bool) {
		return bool ? "Ja" : "Nein";
	}
}
