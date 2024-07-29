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

package de.wps.radvis.backend.servicestation.schnittstelle;

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
import de.wps.radvis.backend.servicestation.domain.ServicestationImportService;
import de.wps.radvis.backend.servicestation.domain.ServicestationRepository;
import de.wps.radvis.backend.servicestation.domain.ServicestationService;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/servicestation")
@AllArgsConstructor
@Transactional
public class ServicestationController {
	private final ServicestationRepository repository;
	private final ServicestationService service;
	private final CsvRepository csvRepository;
	private final ServicestationImportService servicestationImportService;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final BenutzerResolver benutzerResolver;
	private final ServicestationGuard servicestationGuard;

	@PostMapping("new")
	@WithAuditing(context = AuditingContext.SAVE_SERVICESTATION_COMMAND)
	public Long create(@RequestBody @Valid SaveServicestationCommand command, Authentication authentication) {
		servicestationGuard.create(command, authentication);

		Servicestation servicestation = new Servicestation(
			(Point) command.getGeometrie(),
			command.getName(),
			command.getGebuehren(),
			command.getOeffnungszeiten(),
			command.getBetreiber(),
			command.getMarke(),
			command.getLuftpumpe(),
			command.getKettenwerkzeug(),
			command.getWerkzeug(),
			command.getFahrradhalterung(),
			command.getBeschreibung(),
			verwaltungseinheitResolver.resolve(command.getOrganisationId()),
			command.getTyp(),
			command.getStatus(),
			new DokumentListe());
		return repository.save(servicestation).getId();
	}

	@PostMapping("{id}")
	@WithAuditing(context = AuditingContext.SAVE_SERVICESTATION_COMMAND)
	public ServicestationView save(@PathVariable Long id, @RequestBody @Valid SaveServicestationCommand command,
		Authentication authentication) {
		servicestationGuard.save(id, command, authentication);

		Servicestation servicestation = service.loadForModification(id, command.getVersion());
		servicestation.updateAttribute(
			(Point) command.getGeometrie(),
			command.getName(),
			command.getGebuehren(),
			command.getOeffnungszeiten(),
			command.getBetreiber(),
			command.getMarke(),
			command.getLuftpumpe(),
			command.getKettenwerkzeug(),
			command.getWerkzeug(),
			command.getFahrradhalterung(),
			command.getBeschreibung(),
			verwaltungseinheitResolver.resolve(command.getOrganisationId()),
			command.getTyp(),
			command.getStatus()
		);
		return new ServicestationView(repository.save(servicestation),
			service.darfBenutzerBearbeiten(authentication, servicestation));
	}

	@GetMapping("")
	public Stream<ServicestationView> getAll(Authentication authentication) {
		return StreamSupport.stream(repository.findAll().spliterator(), false)
			.map(servicestation -> new ServicestationView(servicestation,
				service.darfBenutzerBearbeiten(authentication, servicestation)));
	}

	@GetMapping("{id}")
	public ServicestationView get(@PathVariable Long id, Authentication authentication) {
		Servicestation servicestation = repository.findById(id).orElseThrow(EntityNotFoundException::new);
		return new ServicestationView(servicestation, service.darfBenutzerBearbeiten(authentication, servicestation));
	}

	@GetMapping("{servicestationId}/dokumentliste")
	public DokumenteView getDokumentListe(@PathVariable("servicestationId") Long servicestationId) {
		return new DokumenteView(
			repository.findById(servicestationId).orElseThrow(EntityNotFoundException::new).getDokumentListe()
				.getDokumente(), true);
	}

	@GetMapping("{servicestationId}/dokument/{dokumentId}")
	public ResponseEntity<byte[]> getDokument(@PathVariable("servicestationId") Long servicestationId,
		@PathVariable("dokumentId") Long dokumentId) {
		Dokument dokument = service.getDokument(servicestationId, dokumentId);

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

	@PostMapping(path = "{servicestationId}/dokument", consumes = {
		MediaType.MULTIPART_FORM_DATA_VALUE,
	})
	@WithAuditing(context = AuditingContext.UPLOAD_DOKUMENT)
	public void addDokument(
		@PathVariable("servicestationId") Long servicestationId,
		@RequestPart AddDokumentCommand command,
		@RequestPart MultipartFile file,
		Authentication authentication) throws IOException {
		service.addDokument(
			servicestationId,
			new Dokument(
				command.getFilename(),
				benutzerResolver.fromAuthentication(authentication),
				file.getBytes(),
				LocalDateTime.now())
		);
	}

	@DeleteMapping("{servicestationId}/dokument/{dokumentId}")
	@WithAuditing(context = AuditingContext.DELETE_DOKUMENT)
	public void deleteDokument(@PathVariable("servicestationId") Long servicestationId,
		@PathVariable("dokumentId") Long dokumentId) {
		service.deleteDokument(servicestationId, dokumentId);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@WithAuditing(context = AuditingContext.SERVICESTATION_CSV_IMPORT)
	public void importCsv(@RequestPart @Valid String file, HttpServletResponse httpServletResponse,
		Authentication authentication)
		throws IOException, CsvReadException, CsvImportException {
		CsvData csvData = csvRepository.read(file.getBytes(), Servicestation.CsvHeader.ALL);
		CsvData protokoll = servicestationImportService.importCsv(csvData,
			benutzerResolver.fromAuthentication(authentication));

		httpServletResponse.setContentType("text/csv;charset=utf-8");
		httpServletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=servicestation_import_protokoll.csv");
		httpServletResponse.getOutputStream().write(csvRepository.write(protokoll));
		httpServletResponse.flushBuffer();
	}

	@WithAuditing(context = AuditingContext.DELETE_SERVICESTATION_COMMAND)
	@DeleteMapping("{id}")
	public void delete(@PathVariable Long id, @RequestBody @Valid DeleteServicestationCommand command,
		Authentication authentication) {
		Servicestation servicestation = service.loadForModification(id, command.getVersion());
		servicestationGuard.delete(id, command, authentication);

		repository.delete(servicestation);
	}
}
