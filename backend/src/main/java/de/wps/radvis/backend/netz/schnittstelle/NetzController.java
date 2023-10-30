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

package de.wps.radvis.backend.netz.schnittstelle;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.geojson.Feature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.entity.VersionedId;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometrien;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.schnittstelle.command.ChangeSeitenbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.CreateKanteCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveGeschwindigkeitAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteFahrtrichtungCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteVerlaufCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKnotenCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveZustaendigkeitAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.view.KanteDetailView;
import de.wps.radvis.backend.netz.schnittstelle.view.KanteEditView;
import de.wps.radvis.backend.netz.schnittstelle.view.KnotenDetailView;
import de.wps.radvis.backend.netz.schnittstelle.view.KnotenEditView;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/netz")
// Kein @Transactional am Controller, da der Service transactional ist.
public class NetzController {

	private final NetzService netzService;
	private final NetzGuard netzGuard;
	private final BenutzerResolver benutzerResolver;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final SaveKanteCommandConverter saveKanteCommandConverter;
	private final NetzToFeatureDetailsConverter netzToFeatureDetailsConverter;

	public NetzController(
		@NonNull NetzService netzService,
		@NonNull NetzGuard netzGuard,
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull ZustaendigkeitsService zustaendigkeitsService,
		@NonNull SaveKanteCommandConverter saveKanteCommandConverter,
		@NonNull NetzToFeatureDetailsConverter netzToFeatureDetailsConverter) {
		this.netzService = netzService;
		this.netzGuard = netzGuard;
		this.benutzerResolver = benutzerResolver;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.saveKanteCommandConverter = saveKanteCommandConverter;
		this.netzToFeatureDetailsConverter = netzToFeatureDetailsConverter;
	}

	@GetMapping("kante/{id}")
	public KanteDetailView getKanteByKantenId(@PathVariable("id") Long id,
		@RequestParam("position") Optional<Double[]> coordinatesParam,
		@RequestParam("seite") String seite) {

		Kante kante = netzService.getKante(id);

		Coordinate position = coordinatesParam.map(doubles -> new Coordinate(doubles[0], doubles[1]))
			.orElseGet(() -> kante.getGeometry().getStartPoint().getCoordinate());
		return netzToFeatureDetailsConverter.convertKantetoKanteDetailView(kante, position, seite);
	}

	@GetMapping("kante/{id}/edit")
	public KanteEditView getKanteEditView(@PathVariable("id") Long id, Authentication authentication) {
		Kante kante = netzService.getKante(id);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return new KanteEditView(kante, zustaendigkeitsService.istImZustaendigkeitsbereich(kante, benutzer));
	}

	@GetMapping("kanten/berechneNebenKante/{id}/{seite}")
	public Feature getKanteVerlauf(@PathVariable("id") Long id, @PathVariable("seite") KantenSeite seite) {
		Geometry kantenverlauf = netzService.getKante(id).getGeometry();
		Geometry nebenkantenVerlauf = netzService.berechneNebenkante(kantenverlauf, seite);
		return GeoJsonConverter.createFeature(nebenkantenVerlauf);
	}

	@GetMapping("knoten/{id}")
	public KnotenDetailView getKnotenFeatureByKnotenId(@PathVariable("id") Long id) {
		return netzToFeatureDetailsConverter.convertKnotenToKnotenDetailView(netzService.getKnoten(id),
			netzService.berechneOrtslage(netzService.getKnoten(id)));
	}

	@GetMapping("knoten/{id}/edit")
	public KnotenEditView getKnotenEditView(@PathVariable("id") Long id, Authentication authentication) {
		Knoten knoten = netzService.getKnoten(id);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return new KnotenEditView(knoten, netzService.berechneOrtslage(knoten),
			zustaendigkeitsService.istImZustaendigkeitsbereich(knoten, benutzer));
	}

	@PostMapping(path = "kanten/saveKanteAllgemein")
	@WithAuditing(context = AuditingContext.SAVE_KANTE_ATTRIBUTE_COMMAND)
	public List<KanteEditView> saveKanteAllgemein(
		Authentication authentication,
		@RequestBody List<@Valid SaveKanteAttributeCommand> commands) {
		netzGuard.saveKanteAllgemein(authentication, commands);

		Set<Long> kanteIds = commands.stream().map(SaveKanteAttributeCommand::getKanteId)
			.collect(Collectors.toSet());

		checkKeineRadnetzQuelle(kanteIds);

		final var kantenAttributGruppen = commands.stream().map(command -> KantenAttributGruppe.builder()
			.id(command.getGruppenId())
			.version(command.getGruppenVersion())
			.netzklassen(command.getNetzklassen())
			.istStandards(command.getIstStandards())
			.kantenAttribute(saveKanteCommandConverter.convertKantenAttributeCommand(command))
			.build()).collect(Collectors.toList());

		netzService.aktualisiereKantenAttribute(kantenAttributGruppen);

		return createKantenEditViews(authentication, kanteIds);
	}

	@PostMapping(path = "kanten/changeSeitenbezug")
	@WithAuditing(context = AuditingContext.CHANGE_SEITENBEZUG_COMMAND)
	public List<KanteEditView> changeSeitenbezug(
		Authentication authentication,
		@RequestBody List<@Valid ChangeSeitenbezugCommand> commands) {
		netzGuard.changeSeitenbezug(authentication, commands);

		Set<Long> kanteIds = commands.stream()
			.map(ChangeSeitenbezugCommand::getId)
			.collect(Collectors.toSet());

		checkKeineRadnetzQuelle(kanteIds);

		final var zweiseitigkeitMap = commands.stream().map(command -> {
				VersionedId versionedId = new VersionedId(command.getId(), command.getVersion());
				return new AbstractMap.SimpleEntry<>(versionedId, command.isZweiseitig());
			})
			.collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

		netzService.aktualisiereKantenZweiseitig(zweiseitigkeitMap);

		return netzService.getKanten(kanteIds)
			.stream()
			.map(kante -> new KanteEditView(kante,
				zustaendigkeitsService.istImZustaendigkeitsbereich(kante,
					benutzerResolver.fromAuthentication(authentication))))
			.collect(Collectors.toList());
	}

	@PostMapping(path = "kanten/saveVerlauf")
	@WithAuditing(context = AuditingContext.SAVE_KANTE_VERLAUF_COMMAND)
	public List<KanteEditView> saveKanteVerlauf(
		Authentication authentication,
		@RequestBody List<@Valid SaveKanteVerlaufCommand> commands) {
		netzGuard.saveKanteVerlauf(authentication, commands);

		Set<Long> kanteIds = commands.stream()
			.map(SaveKanteVerlaufCommand::getId)
			.collect(Collectors.toSet());

		final var kantenGeometrien = commands.stream()
			.map(command -> KanteGeometrien.builder()
				.id(command.getId())
				.version(command.getKantenVersion())
				.verlaufLinks((LineString) command.getVerlaufLinks())
				.verlaufRechts((LineString) command.getVerlaufRechts())
				.geometry((LineString) command.getGeometry())
				.build())
			.collect(Collectors.toList());

		netzService.aktualisiereVerlaeufeUndGeometrien(kantenGeometrien);

		return createKantenEditViews(authentication, kanteIds);
	}

	@PostMapping(path = "kanten/saveFuehrungsformAttributGruppe")
	@WithAuditing(context = AuditingContext.SAVE_FUEHRUNGSFORM_ATTRIBUT_GRUPPE_COMMAND)
	public List<KanteEditView> saveFuehrungsformAttributGruppen(
		Authentication authentication,
		@RequestBody List<@Valid SaveFuehrungsformAttributGruppeCommand> commands) {
		netzGuard.saveFuehrungsformAttributGruppen(authentication, commands);

		Set<Long> kanteIds = commands.stream()
			.map(SaveFuehrungsformAttributGruppeCommand::getKanteId)
			.collect(Collectors.toSet());

		checkKeineRadnetzQuelle(kanteIds);

		final var fuehrungsformAttributGruppen = commands.stream().map(command -> {
			List<FuehrungsformAttribute> fuehrungsFormAttributeLinks = saveKanteCommandConverter
				.convertFuehrungsformAtttributeCommands(
					command.getFuehrungsformAttributeLinks());
			List<FuehrungsformAttribute> fuehrungsFormAttributeRechts = saveKanteCommandConverter
				.convertFuehrungsformAtttributeCommands(
					command.getFuehrungsformAttributeRechts());

			return FuehrungsformAttributGruppe.builder()
				.isZweiseitig(true)
				.fuehrungsformAttributeLinks(fuehrungsFormAttributeLinks)
				.fuehrungsformAttributeRechts(fuehrungsFormAttributeRechts)
				.id(command.getGruppenID())
				.version(command.getGruppenVersion())
				.build();
		}).collect(Collectors.toList());

		netzService.aktualisiereFuehrungsformen(fuehrungsformAttributGruppen);

		return createKantenEditViews(authentication, kanteIds);
	}

	@PostMapping(path = "kanten/saveGeschwindigkeitAttributGruppe")
	@WithAuditing(context = AuditingContext.SAVE_GESCHWINDIGKEIT_ATTRIBUT_GRUPPE_COMMAND)
	public List<KanteEditView> saveGeschwindigkeitAttributGruppen(
		Authentication authentication,
		@RequestBody List<@Valid SaveGeschwindigkeitAttributGruppeCommand> commands) {
		netzGuard.saveGeschwindigkeitAttributGruppen(authentication, commands);

		Set<Long> kanteIds = commands.stream()
			.map(SaveGeschwindigkeitAttributGruppeCommand::getKanteId)
			.collect(Collectors.toSet());
		checkKeineRadnetzQuelle(kanteIds);

		Map<VersionedId, List<GeschwindigkeitAttribute>> attributeMap = commands.stream()
			.map(command -> {
				List<GeschwindigkeitAttribute> geschwindigkeitAttribute = command.getGeschwindigkeitAttribute()
					.stream()
					.map(saveKanteCommandConverter::convertGeschwindigkeitsAttributeCommand)
					.collect(Collectors.toList());
				VersionedId versionedId = new VersionedId(command.getGruppenID(), command.getGruppenVersion());
				return new AbstractMap.SimpleEntry<>(versionedId, geschwindigkeitAttribute);
			})
			.collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

		netzService.aktualisiereGeschwindigkeitAttribute(attributeMap);

		return createKantenEditViews(authentication, kanteIds);
	}

	@PostMapping(path = "kanten/saveZustaendigkeitAttributGruppe")
	@WithAuditing(context = AuditingContext.SAVE_ZUSTAENDIGKEIT_ATTRIBUT_GRUPPE_COMMAND)
	public List<KanteEditView> saveZustaendigkeitAttributGruppen(
		Authentication authentication,
		@RequestBody List<@Valid SaveZustaendigkeitAttributGruppeCommand> commands) {
		netzGuard.saveZustaendigkeitAttributGruppen(authentication, commands);

		Set<Long> kanteIds = commands.stream()
			.map(SaveZustaendigkeitAttributGruppeCommand::getKanteId)
			.collect(Collectors.toSet());
		checkKeineRadnetzQuelle(kanteIds);

		Map<VersionedId, List<ZustaendigkeitAttribute>> zustaendigkeitMap = commands.stream()
			.map(command -> {
				List<ZustaendigkeitAttribute> zustaendigkeitAttribute = command.getZustaendigkeitAttribute()
					.stream()
					.map(saveKanteCommandConverter::convertZustaendigkeitsAttributeCommand)
					.collect(Collectors.toList());
				VersionedId versionedId = new VersionedId(command.getGruppenID(), command.getGruppenVersion());
				return new AbstractMap.SimpleEntry<>(versionedId, zustaendigkeitAttribute);
			})
			.collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

		netzService.aktualisiereZustaendigkeitsAttribute(zustaendigkeitMap);

		return createKantenEditViews(authentication, kanteIds);
	}

	@PostMapping(path = "kanten/saveFahrtrichtungAttributGruppe")
	@WithAuditing(context = AuditingContext.SAVE_KANTE_FAHRTRICHTUNG_COMMAND)
	public List<KanteEditView> saveFahrtrichtungAttributGruppe(
		Authentication authentication,
		@RequestBody List<@Valid SaveKanteFahrtrichtungCommand> commands) {
		netzGuard.saveFahrtrichtungAttributGruppe(authentication, commands);

		Set<Long> kanteIds = commands.stream()
			.map(SaveKanteFahrtrichtungCommand::getKanteId)
			.collect(Collectors.toSet());
		checkKeineRadnetzQuelle(kanteIds);

		Map<VersionedId, FahrtrichtungAttributGruppe> fahrtrichtungMap = commands.stream()
			.map(command -> {
				VersionedId versionedId = new VersionedId(command.getGruppenId(), command.getGruppenVersion());
				FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = FahrtrichtungAttributGruppe.builder()
					.fahrtrichtungLinks(command.getFahrtrichtungLinks())
					.fahrtrichtungRechts(command.getFahrtrichtungRechts())
					.isZweiseitig(true)
					.build();
				return new AbstractMap.SimpleEntry<>(versionedId, fahrtrichtungAttributGruppe);
			})
			.collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

		netzService.aktualisiereFahrtrichtung(fahrtrichtungMap);

		return createKantenEditViews(authentication, kanteIds);
	}

	@PostMapping(path = "kanten/create")
	@WithAuditing(context = AuditingContext.CREATE_KANTE_COMMAND)
	public KanteEditView createKante(@RequestBody @Valid CreateKanteCommand command, Authentication authentication) {
		this.netzGuard.createKante(command, authentication);

		Kante kante;
		if (Objects.isNull(command.getBisKnotenId())) {
			kante = netzService.createGrundnetzKanteWithNewBisKnoten(command.getVonKnotenId(),
				GeoJsonConverter.create3DJtsPointFromGeoJson(command.getBisKnotenCoor(),
					KoordinatenReferenzSystem.ETRS89_UTM32_N), Status.FIKTIV);
		} else {
			kante = netzService.createGrundnetzKante(command.getVonKnotenId(), command.getBisKnotenId(),
				Status.FIKTIV);
		}

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return new KanteEditView(kante, zustaendigkeitsService.istImZustaendigkeitsbereich(kante, benutzer));
	}

	@PostMapping(path = "knoten/save")
	@WithAuditing(context = AuditingContext.SAVE_KNOTEN_COMMAND)
	public KnotenEditView saveKnoten(
		Authentication authentication,
		@RequestBody @Valid SaveKnotenCommand command) {
		netzGuard.saveKnoten(authentication, command);

		netzService.aktualisiereKnoten(command.getId(), command.getKnotenVersion(), command.getGemeinde(),
			command.getKommentar(), command.getZustandsbeschreibung(), command.getKnotenForm());

		Knoten knoten = netzService.getKnoten(command.getId());
		return new KnotenEditView(knoten, netzService.berechneOrtslage(knoten),
			zustaendigkeitsService.istImZustaendigkeitsbereich(knoten,
				benutzerResolver.fromAuthentication(authentication)));
	}

	/*
	 * Dieser Endpunkt wird nicht im RadVIS-Frontend aufgerufen. Er ist zur Vereinfachung des Loeschens durch
	 * Entwickler:innen da und kann z.B. ueber curl angesprochen werden. Dabei muessen die Authorization, die SessionID
	 * und ein XSRF-TOKEN aus einer anderen Anfrage im Browser herausgefunden werden.
	 *
	 * curl "<baseUrl>/api/netz/kante/<id>" -X DELETE -H "X-XSRF-TOKEN: <xsrf-token>" -H "Authorization: <authorization>" -H "Cookie: JSESSIONID=<session id>; XSRF-TOKEN=<nochmal das xsrf-token>"
	 */
	@DeleteMapping("kante/{id}")
	@WithAuditing(context = AuditingContext.DELETE_KANTE)
	public void deleteRadVISKanteById(@PathVariable("id") Long id) {
		if (!FeatureTogglz.KANTE_LOESCHEN_ENDPUNKT.isActive()) {
			throw new AccessDeniedException(
				"Diese Funktion ist derzeit deaktiviert.");
		}

		Kante kante = netzService.getKante(id);
		if (kante.getQuelle().equals(QuellSystem.RadVis)) {
			netzService.deleteKante(kante);
			log.info("RadVIS-Kante {} wurde gelöscht", kante.getId());
		} else {
			log.warn("Die Kante mit der id {} konnte nicht gelöscht werden, da sie nicht Quellsystem \"RadVIS\" ist",
				kante.getId());
		}
	}

	/*
	 * Dieser Endpunkt wird nicht im RadVIS-Frontend aufgerufen. Er ist zur aktualisierung der Materialized Views
	 *  durch Entwickler:innen da und kann z.B. ueber curl angesprochen werden. Dabei muessen die Authorization, die SessionID
	 * und ein XSRF-TOKEN aus einer anderen Anfrage im Browser herausgefunden werden (analog zu Kante loeschen)
	 */
	@GetMapping(path = "refreshMatViews")
	public void refreshRadVisNetzMaterializedViews(Authentication authentication) {
		if (!FeatureTogglz.REFRESH_MATERIALIZED_VIEWS_ENDPUNKT.isActive()) {
			throw new AccessDeniedException(
				"Diese Funktion ist derzeit deaktiviert: refreshMatViews");
		}

		netzGuard.refreshRadVisNetzMaterializedViews(authentication);

		log.info("RefreshRadVisNetzMaterializedViews Endpunkt getriggert.");
		netzService.refreshRadVisNetzMaterializedViews();
		log.info("RefreshRadVisNetzMaterializedViews Endpunkt Done.");
	}

	private List<KanteEditView> createKantenEditViews(Authentication authentication, Set<Long> kanteIds) {
		return netzService
			.getKanten(kanteIds).stream()
			.map(kante -> new KanteEditView(kante,
				zustaendigkeitsService.istImZustaendigkeitsbereich(kante,
					benutzerResolver.fromAuthentication(authentication))))
			.collect(Collectors.toList());
	}

	private void checkKeineRadnetzQuelle(Set<Long> kanteIds) {
		if (netzService.getKanten(kanteIds).stream().anyMatch(k -> k.getQuelle() == QuellSystem.RadNETZ)) {
			throw new AccessDeniedException(
				"RadNETZ-Kanten sind bis zum Abschluss der Qualitätssicherung nicht bearbeitbar.");
		}
	}
}
