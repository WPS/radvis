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

package de.wps.radvis.backend.leihstation.schnittstelle;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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
import de.wps.radvis.backend.leihstation.domain.LeihstationImportService;
import de.wps.radvis.backend.leihstation.domain.LeihstationRepository;
import de.wps.radvis.backend.leihstation.domain.LeihstationService;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/leihstation")
@AllArgsConstructor
public class LeihstationController {
	private final LeihstationRepository repository;
	private final LeihstationService service;
	private final CsvRepository csvRepository;
	private final LeihstationImportService leihstationImportService;
	private final LeihstationGuard leihstationGuard;
	private final BenutzerResolver benutzerResolver;

	@PostMapping("new")
	@WithAuditing(context = AuditingContext.SAVE_LEIHSTATION_COMMAND)
	public Long create(@RequestBody @Valid SaveLeihstationCommand command, Authentication authentication) {
		leihstationGuard.create(command, authentication);

		Leihstation leihstation = new Leihstation(
			(Point) command.getGeometrie(),
			command.getBetreiber(),
			command.getAnzahlFahrraeder(),
			command.getAnzahlPedelecs(),
			command.getAnzahlAbstellmoeglichkeiten(),
			command.isFreiesAbstellen(),
			command.getBuchungsUrl(),
			command.getStatus(),
			LeihstationQuellSystem.RADVIS,
			null);
		return repository.save(leihstation).getId();
	}

	@PostMapping("{id}")
	@WithAuditing(context = AuditingContext.SAVE_LEIHSTATION_COMMAND)
	public LeihstationView save(@PathVariable Long id, @RequestBody @Valid SaveLeihstationCommand command,
		Authentication authentication) {
		leihstationGuard.save(id, command, authentication);

		Leihstation leihstation = service.loadForModification(id, command.getVersion());
		if (!leihstation.getQuellSystem().equals(LeihstationQuellSystem.RADVIS)) {
			throw new AccessDeniedException("Es dürfen nur Leihstationen mit Quellsystem RadVIS bearbeitet werden.");
		}
		leihstation.update(
			(Point) command.getGeometrie(),
			command.getBetreiber(),
			command.getAnzahlFahrraeder(),
			command.getAnzahlPedelecs(),
			command.getAnzahlAbstellmoeglichkeiten(),
			command.isFreiesAbstellen(),
			command.getBuchungsUrl(),
			command.getStatus());
		return new LeihstationView(repository.save(leihstation),
			service.darfBenutzerBearbeiten(authentication, leihstation));
	}

	@GetMapping("")
	public Stream<LeihstationView> getAll(Authentication authentication) {
		return StreamSupport.stream(repository.findAll().spliterator(), false)
			.map(leihstation -> new LeihstationView(leihstation,
				service.darfBenutzerBearbeiten(authentication, leihstation)));
	}

	@GetMapping("{id}")
	public LeihstationView get(@PathVariable Long id, Authentication authentication) {
		Leihstation leihstation = repository.findById(id).orElseThrow(EntityNotFoundException::new);
		return new LeihstationView(leihstation, service.darfBenutzerBearbeiten(authentication, leihstation));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@WithAuditing(context = AuditingContext.LEIHSTATION_CSV_IMPORT)
	public void importCsv(@RequestPart @Valid MultipartFile file, HttpServletResponse httpServletResponse,
		Authentication authentication)
		throws CsvReadException, IOException, CsvImportException {
		CsvData csvData = csvRepository.read(file.getBytes(), Leihstation.CsvHeader.ALL);
		CsvData protokoll = leihstationImportService.importCsv(csvData,
			benutzerResolver.fromAuthentication(authentication));

		httpServletResponse.setContentType("text/csv;charset=utf-8");
		httpServletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=leihstation_import_protokoll.csv");
		httpServletResponse.getOutputStream().write(csvRepository.write(protokoll));
		httpServletResponse.flushBuffer();
	}

	@WithAuditing(context = AuditingContext.DELETE_LEIHSTATION_COMMAND)
	@DeleteMapping("{id}")
	public void delete(@PathVariable Long id, @RequestBody @Valid DeleteLeihstationCommand command,
		Authentication authentication) {
		leihstationGuard.delete(id, command, authentication);

		Leihstation leihstation = service.loadForModification(id, command.getVersion());
		repository.delete(leihstation);
	}
}
