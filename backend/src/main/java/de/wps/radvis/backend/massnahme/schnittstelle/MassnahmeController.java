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

package de.wps.radvis.backend.massnahme.schnittstelle;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.schnittstelle.AddDokumentCommand;
import de.wps.radvis.backend.dokument.schnittstelle.view.DokumenteView;
import de.wps.radvis.backend.kommentar.domain.entity.Kommentar;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.kommentar.schnittstelle.AddKommentarCommand;
import de.wps.radvis.backend.kommentar.schnittstelle.view.KommentarView;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandabfrageService;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.schnittstelle.view.MassnahmeEditView;
import de.wps.radvis.backend.massnahme.schnittstelle.view.MassnahmeListenView;
import de.wps.radvis.backend.massnahme.schnittstelle.view.MassnahmeToolView;
import de.wps.radvis.backend.massnahme.schnittstelle.view.UmsetzungsstandAbfrageVorschauView;
import de.wps.radvis.backend.massnahme.schnittstelle.view.UmsetzungsstandEditView;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.NonNull;

@RestController
@RequestMapping("/api/massnahme")
@Validated
public class MassnahmeController {

	private final MassnahmeService massnahmeService;
	private final UmsetzungsstandabfrageService umsetzungsstandabfrageService;
	private final CreateMassnahmeCommandConverter createMassnahmeCommandConverter;
	private final SaveMassnahmeCommandConverter saveMassnahmeCommandConverter;
	private final SaveUmsetzungsstandCommandConverter saveUmsetzungsstandCommandConverter;
	private final MassnahmeGuard massnahmeGuard;
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final CsvRepository csvRepository;

	public MassnahmeController(
		@NonNull MassnahmeService massnahmeService,
		@NonNull UmsetzungsstandabfrageService umsetzungsstandabfrageService,
		@NonNull CreateMassnahmeCommandConverter createMassnahmeCommandConverter,
		@NonNull SaveMassnahmeCommandConverter saveMassnahmeCommandConverter,
		@NonNull SaveUmsetzungsstandCommandConverter saveUmsetzungsstandCommandConverter,
		@NonNull MassnahmeGuard massnahmeGuard,
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull VerwaltungseinheitService verwaltungseinheitService,
		@NonNull CsvRepository csvRepository) {
		this.csvRepository = csvRepository;
		this.massnahmeService = massnahmeService;
		this.umsetzungsstandabfrageService = umsetzungsstandabfrageService;
		this.createMassnahmeCommandConverter = createMassnahmeCommandConverter;
		this.saveMassnahmeCommandConverter = saveMassnahmeCommandConverter;
		this.saveUmsetzungsstandCommandConverter = saveUmsetzungsstandCommandConverter;
		this.massnahmeGuard = massnahmeGuard;
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	@GetMapping("{id}/edit")
	public MassnahmeEditView getMassnahmeForEdit(@PathVariable("id") Long id, Authentication authentication) {
		Massnahme massnahme = massnahmeService.get(id);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return new MassnahmeEditView(massnahme, massnahmeGuard.darfMassnahmeBearbeiten(benutzer, massnahme));
	}

	@WithAuditing(context = AuditingContext.DELETE_MASSNAHME_COMMAND)
	@DeleteMapping("{id}")
	public void deleteMassnahme(@PathVariable("id") Long id, Authentication authentication,
		@RequestBody @Valid DeleteMassnahmeCommand command) {
		Massnahme massnahme = massnahmeService.loadForModification(id, command.getVersion());
		massnahmeGuard.deleteMassnahme(authentication, massnahme);
		massnahme.alsGeloeschtMarkieren();
		this.massnahmeService.saveMassnahme(massnahme);
	}

	@GetMapping("{id}/editUmsetzungsstand")
	public UmsetzungsstandEditView getUmsetzungsstandForEdit(@PathVariable("id") Long id,
		Authentication authentication) {
		Massnahme massnahme = massnahmeService.get(id);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return new UmsetzungsstandEditView(massnahme, massnahmeGuard.darfMassnahmeBearbeiten(benutzer, massnahme));
	}

	@GetMapping("{id}/kommentarliste")
	public List<KommentarView> getKommentarListe(Authentication authentication, @PathVariable("id") Long id) {
		KommentarListe kommentarListe = massnahmeService.get(id).getKommentarListe();
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return KommentarView.convertAll(kommentarListe, benutzer);
	}

	@PostMapping("{massnahmeId}/kommentar")
	@Transactional
	public List<KommentarView> addKommentar(
		@PathVariable("massnahmeId") Long massnahmeId,
		Authentication authentication,
		@RequestBody @Valid AddKommentarCommand command) {
		Massnahme massnahme = massnahmeService.get(massnahmeId);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		massnahme.addKommentar(new Kommentar(command.getKommentarText(), benutzer));

		return KommentarView.convertAll(massnahme.getKommentarListe(), benutzer);
	}

	@GetMapping("{id}/dokumentliste")
	public DokumenteView getDokumentListe(@PathVariable("id") Long id, Authentication authentication) {
		Massnahme massnahme = massnahmeService.get(id);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return new DokumenteView(
			massnahme.getDokumentListe().getDokumente(),
			massnahmeGuard.darfMassnahmeBearbeiten(benutzer, massnahme) && !massnahme.isArchiviert());
	}

	@PostMapping(path = "{massnahmeId}/dokument", consumes = {
		MediaType.MULTIPART_FORM_DATA_VALUE,
	})
	@WithAuditing(context = AuditingContext.UPLOAD_DOKUMENT)
	public void uploadDatei(
		@PathVariable("massnahmeId") Long massnahmeId,
		@RequestPart AddDokumentCommand command,
		@RequestPart MultipartFile file,
		Authentication authentication) throws IOException {
		byte[] bytes = file.getBytes();
		if (!Dokument.isValid(bytes)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Die maximale Dateigröße ist überschritten.");
		}

		massnahmeGuard.uploadDatei(massnahmeId, command, file, authentication);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		if (massnahmeService.get(massnahmeId).isArchiviert()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Archivierte Dateien dürfen nicht bearbeitet werden.");
		}

		massnahmeService.haengeDateiAn(
			massnahmeId,
			new Dokument(
				command.getFilename(),
				benutzer,
				bytes,
				LocalDateTime.now()));
	}

	@GetMapping("{massnahmeId}/dokument/{dokumentId}")
	public ResponseEntity<byte[]> downloadDatei(
		@PathVariable("massnahmeId") Long massnahmeId,
		@PathVariable("dokumentId") Long dokumentId) {
		Dokument dokument = massnahmeService.getDokument(massnahmeId, dokumentId);

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

	@DeleteMapping("{massnahmeId}/dokument/{dokumentId}")
	@WithAuditing(context = AuditingContext.DELETE_DOKUMENT)
	public void deleteDatei(Authentication authentication,
		@PathVariable("massnahmeId") Long massnahmeId,
		@PathVariable("dokumentId") Long dokumentId) {
		massnahmeGuard.deleteDatei(authentication, massnahmeId);

		if (massnahmeService.get(massnahmeId).isArchiviert()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Archivierte Dateien dürfen nicht bearbeitet werden.");
		}

		massnahmeService.deleteDokument(massnahmeId, dokumentId);
	}

	@PostMapping("{id}/unarchivieren")
	@WithAuditing(context = AuditingContext.MASSNAHME_ARCHIVIEREN_COMMAND)
	public MassnahmeEditView unarchivieren(Authentication authentication, @PathVariable("id") Long id) {
		massnahmeGuard.unarchivieren(authentication, id);
		Massnahme massnahme = massnahmeService.archivierungAufheben(id);
		return new MassnahmeEditView(massnahme,
			massnahmeGuard.darfMassnahmeBearbeiten(benutzerResolver.fromAuthentication(authentication), massnahme));
	}

	@PostMapping("archivieren")
	@WithAuditing(context = AuditingContext.MASSNAHME_ARCHIVIEREN_COMMAND)
	public void archivieren(Authentication authentication, @RequestBody MassnahmenArchivierenCommand command) {
		massnahmeGuard.archivieren(authentication, command);
		massnahmeService.massnahmenArchivieren(command.getIds());
	}

	@GetMapping("{id}/hasUmsetzungsstand")
	public boolean hasUmsetzungsstand(@PathVariable("id") Long id) {
		return massnahmeService.get(id).getUmsetzungsstand().isPresent();
	}

	@PostMapping("/umsetzungsstand/auswertung")
	public ResponseEntity<byte[]> getUmsetzungsstandAuswertung(
		Authentication authentication,
		@RequestBody List<Long> massnahmenIds) throws IOException {
		massnahmeGuard.getUmsetzungsstandAuswertung(authentication);

		String timestamp = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
		HttpHeaders headers = new HttpHeaders();
		String dateiname = timestamp + "_Umsetzungsstand-Export.csv";
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + dateiname);
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		byte[] datei = csvRepository.write(massnahmeService.getUmsetzungsstandAuswertung(massnahmenIds));

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
			.contentLength(datei.length)
			.headers(headers)
			.body(datei);
	}

	@PostMapping("/save")
	@WithAuditing(context = AuditingContext.SAVE_MASSNAHME_COMMAND)
	public MassnahmeEditView saveMassnahme(Authentication authentication,
		@RequestBody @Valid SaveMassnahmeCommand command) {
		massnahmeGuard.saveMassnahme(authentication, command);

		Massnahme massnahme = massnahmeService.loadForModification(command.getId(), command.getVersion());

		if (massnahme.isArchiviert()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Archivierte Dateien dürfen nicht bearbeitet werden.");
		}

		saveMassnahmeCommandConverter.apply(authentication, command, massnahme);
		return new MassnahmeEditView(massnahmeService.saveMassnahme(massnahme), true);
	}

	@PostMapping("/saveUmsetzungsstand")
	@WithAuditing(context = AuditingContext.SAVE_UMSETZUNGSSTAND_COMMAND)
	public UmsetzungsstandEditView saveUmsetzungsstand(Authentication authentication,
		@RequestBody @Valid SaveUmsetzungsstandCommand command) {
		massnahmeGuard.saveUmsetzungsstand(authentication, command);

		Umsetzungsstand umsetzungsstand = massnahmeService.loadUmsetzungsstandForModification(command.getId(),
			command.getVersion());
		Massnahme massnahme = massnahmeService.getMassnahmeByUmsetzungsstand(umsetzungsstand);

		if (massnahme.isArchiviert()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Archivierte Dateien dürfen nicht bearbeitet werden.");
		}

		saveUmsetzungsstandCommandConverter.apply(authentication, command, umsetzungsstand, massnahme);
		massnahmeService.saveUmsetzungsstand(umsetzungsstand);

		return new UmsetzungsstandEditView(massnahme, true);
	}

	@PostMapping(path = "create")
	@WithAuditing(context = AuditingContext.CREATE_MASSNAHME_COMMAND)
	public Long createMassnahme(Authentication authentication, @RequestBody @Valid CreateMassnahmeCommand command) {
		massnahmeGuard.createMassnahme(authentication, command);

		Massnahme massnahme = createMassnahmeCommandConverter.convert(authentication, command);
		return massnahmeService.saveMassnahme(massnahme).getId();
	}

	@PostMapping("/list")
	public List<MassnahmeListenView> getAlleMassnahme(@RequestBody @Valid FilterMassnahmenCommand command) {
		Optional<Verwaltungseinheit> verwaltungseinheit = command.getVerwaltungseinheitId().map(
			verwaltungseinheitId -> {
				return verwaltungseinheitService.findById(verwaltungseinheitId)
					.orElseThrow(() -> new EntityNotFoundException(String.format(
						"Eine Verwaltungseinheit mit der ID '%d' existiert nicht.", verwaltungseinheitId)));
			});

		return massnahmeService.getAlleMassnahmenListenViews(command.getHistorischeMassnahmenAnzeigen(),
			verwaltungseinheit, command.getFahrradroutenIds()).stream()
			.map(MassnahmeListenView::new).collect(Collectors.toList());
	}

	@PostMapping("/umsetzungsstandsabfrage/start")
	@WithAuditing(context = AuditingContext.UMSETZUNGSSTANDSABFRAGE_STARTEN)
	public void starteUmsetzungsstandsabfrage(Authentication authentication, @RequestBody List<Long> massnahmeIds) {
		massnahmeGuard.starteUmsetzungsstandsabfrage(authentication);
		List<Massnahme> angepassteMassnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			massnahmeIds);
		umsetzungsstandabfrageService.benachrichtigeNutzer(angepassteMassnahmen);
	}

	@PostMapping("/umsetzungsstandsabfrage/vorschau")
	public UmsetzungsstandAbfrageVorschauView getUmsetzungsstandsabfrageVorschau(Authentication authentication,
		@RequestBody List<Long> massnahmeIds) {
		massnahmeGuard.getUmsetzungsstandsabfrageVorschau(authentication);

		String emailVorschau = umsetzungsstandabfrageService.getUmsetzungsstandAbfrageEmailVorschau();
		List<Benutzer> zuBenachrichtigendeBenutzer = umsetzungsstandabfrageService
			.getZuBenachrichtigendeBenutzer(massnahmeIds);

		return new UmsetzungsstandAbfrageVorschauView(emailVorschau, zuBenachrichtigendeBenutzer);
	}

	@GetMapping("{id}/benachrichtigung")
	public boolean getBenachrichtigung(Authentication authentication, @PathVariable("id") Long id) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return this.massnahmeService.get(id).sollBenutzerBenachrichtigtWerden(benutzer);
	}

	@PostMapping("{id}/stelleBenachrichtigungsFunktionEin")
	public boolean getBenachrichtigung(Authentication authentication, @PathVariable("id") Long id,
		@RequestBody boolean aktiv) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		Massnahme massnahme = this.massnahmeService.get(id);
		if (aktiv) {
			massnahme.fuegeZuBenachrichtigendenBenutzerHinzu(benutzer);
		} else {
			massnahme.entferneZuBenachrichtigendenBenutzer(benutzer);
		}
		this.massnahmeService.saveMassnahme(massnahme);
		return massnahme.sollBenutzerBenachrichtigtWerden(benutzer);
	}

	@GetMapping("{id}/massnahmeToolView")
	public MassnahmeToolView getMassnahmeToolView(Authentication authentication, @PathVariable("id") Long id) {
		Massnahme massnahme = massnahmeService.get(id);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return new MassnahmeToolView(massnahmeService.get(id),
			massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahme));
	}
}
