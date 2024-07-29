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

package de.wps.radvis.backend.abstellanlage.schnittstelle;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageImportService;
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageRepository;
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageService;
import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.exception.CsvImportException;
import de.wps.radvis.backend.common.domain.exception.CsvReadException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.dokument.schnittstelle.AddDokumentCommand;
import de.wps.radvis.backend.dokument.schnittstelle.view.DokumenteView;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/abstellanlage")
@AllArgsConstructor
@Transactional
public class AbstellanlageController {
	private final AbstellanlageRepository repository;
	private final AbstellanlageService service;
	private final AbstellanlageImportService importService;
	private final AbstellanlageGuard abstellanlageGuard;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final BenutzerResolver benutzerResolver;
	private final CsvRepository csvRepository;

	@PostMapping("new")
	@WithAuditing(context = AuditingContext.SAVE_ABSTELLANLAGE_COMMAND)
	public Long create(@RequestBody @Valid SaveAbstellanlageCommand command, Authentication authentication) {
		abstellanlageGuard.create(command, authentication);

		Verwaltungseinheit zustaendig = command.getZustaendigId() != null ? verwaltungseinheitResolver.resolve(command
			.getZustaendigId()) : null;
		Abstellanlage abstellanlage = new Abstellanlage(
			(Point) command.getGeometrie(),
			command.getBetreiber(),
			command.getExterneId(),
			AbstellanlagenQuellSystem.RADVIS,
			zustaendig,
			command.getAnzahlStellplaetze(),
			command.getAnzahlSchliessfaecher(),
			command.getAnzahlLademoeglichkeiten(),
			command.getUeberwacht(),
			command.getAbstellanlagenOrt(),
			command.getGroessenklasse(),
			command.getStellplatzart(),
			command.getUeberdacht(),
			command.getGebuehrenProTag(),
			command.getGebuehrenProMonat(),
			command.getGebuehrenProJahr(),
			command.getBeschreibung(),
			command.getWeitereInformation(),
			command.getStatus(),
			new DokumentListe());
		return repository.save(abstellanlage).getId();
	}

	@PostMapping("{id}")
	@WithAuditing(context = AuditingContext.SAVE_ABSTELLANLAGE_COMMAND)
	public AbstellanlageView save(@PathVariable Long id, @RequestBody @Valid SaveAbstellanlageCommand command,
		Authentication authentication) {
		abstellanlageGuard.save(id, command, authentication);

		Verwaltungseinheit zustaendig = command.getZustaendigId() != null ? verwaltungseinheitResolver.resolve(command
			.getZustaendigId()) : null;
		Abstellanlage abstellanlage = service.loadForModification(id, command.getVersion());
		abstellanlage.update(
			(Point) command.getGeometrie(),
			command.getBetreiber(),
			command.getExterneId(),
			zustaendig,
			command.getAnzahlStellplaetze(),
			command.getAnzahlSchliessfaecher(),
			command.getAnzahlLademoeglichkeiten(),
			command.getUeberwacht(),
			command.getAbstellanlagenOrt(),
			command.getGroessenklasse(),
			command.getStellplatzart(),
			command.getUeberdacht(),
			command.getGebuehrenProTag(),
			command.getGebuehrenProMonat(),
			command.getGebuehrenProJahr(),
			command.getBeschreibung(),
			command.getWeitereInformation(),
			command.getStatus()
		);
		return new AbstellanlageView(repository.save(abstellanlage),
			service.darfBenutzerBearbeiten(authentication, abstellanlage));
	}

	@GetMapping("")
	public Stream<AbstellanlageView> getAll(Authentication authentication) {
		return StreamSupport.stream(repository.findAll().spliterator(), false)
			.map(abstellanlage -> new AbstellanlageView(abstellanlage,
				service.darfBenutzerBearbeiten(authentication, abstellanlage)));
	}

	@GetMapping("{id}")
	public AbstellanlageView get(@PathVariable Long id, Authentication authentication) {
		Abstellanlage abstellanlage = repository.findById(id).orElseThrow(EntityNotFoundException::new);
		return new AbstellanlageView(abstellanlage,
			service.darfBenutzerBearbeiten(authentication, abstellanlage));
	}

	@GetMapping("{id}/dokumentliste")
	public DokumenteView getDokumentListe(@PathVariable("id") Long id) {
		return new DokumenteView(
			repository.findById(id).orElseThrow(EntityNotFoundException::new).getDokumentListe().getDokumente(), true);
	}

	@GetMapping("{id}/dokument/{dokumentId}")
	public ResponseEntity<byte[]> getDokument(@PathVariable("id") Long id,
		@PathVariable("dokumentId") Long dokumentId) {
		Dokument dokument = service.getDokument(id, dokumentId);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dokument.getDateiname() + "\"");
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
			.contentLength(dokument.getDateigroesseInBytes())
			.headers(headers)
			.body(dokument.getDatei());
	}

	@PostMapping(path = "{id}/dokument", consumes = {
		MediaType.MULTIPART_FORM_DATA_VALUE,
	})
	@WithAuditing(context = AuditingContext.UPLOAD_DOKUMENT)
	public void addDokument(
		@PathVariable("id") Long id,
		@RequestPart AddDokumentCommand command,
		@RequestPart MultipartFile file,
		Authentication authentication) throws IOException {
		service.addDokument(
			id,
			new Dokument(
				command.getFilename(),
				benutzerResolver.fromAuthentication(authentication),
				file.getBytes(),
				LocalDateTime.now())
		);
	}

	@DeleteMapping("{id}/dokument/{dokumentId}")
	@WithAuditing(context = AuditingContext.DELETE_DOKUMENT)
	public void deleteDokument(@PathVariable("id") Long id,
		@PathVariable("dokumentId") Long dokumentId) {
		service.deleteDokument(id, dokumentId);
	}

	@WithAuditing(context = AuditingContext.DELETE_ABSTELLANLAGE_COMMAND)
	@DeleteMapping("{id}")
	public void delete(@PathVariable Long id, @RequestBody @Valid DeleteAbstellanlageCommand command,
		Authentication authentication) {
		abstellanlageGuard.delete(id, command, authentication);

		Abstellanlage abstellanlage = service.loadForModification(id, command.getVersion());
		repository.delete(abstellanlage);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@WithAuditing(context = AuditingContext.ABSTELLANLAGE_CSV_IMPORT)
	public void importCsv(@RequestPart @Valid MultipartFile file, HttpServletResponse httpServletResponse,
		Authentication authentication)
		throws CsvReadException, IOException, CsvImportException {
		CsvData csvData = csvRepository.read(file.getBytes(), Abstellanlage.CsvHeader.ALL);
		CsvData protokoll = importService.importCsv(csvData, benutzerResolver.fromAuthentication(authentication));

		httpServletResponse.setContentType("text/csv;charset=utf-8");
		httpServletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=abstellanlage_import_protokoll.csv");
		httpServletResponse.getOutputStream().write(csvRepository.write(protokoll));
		httpServletResponse.flushBuffer();
	}
}
