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

package de.wps.radvis.backend.fahrradroute.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.fahrradroute.domain.event.FahrradrouteCreatedEvent;
import de.wps.radvis.backend.fahrradroute.domain.event.FahrradrouteUpdatedEvent;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.DrouteId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Audited
@Getter
@Entity
public class Fahrradroute extends VersionierteEntity {

	@Embedded
	private ToubizId toubizId;

	@Enumerated(EnumType.STRING)
	@NotNull
	private FahrradrouteTyp fahrradrouteTyp;

	@Embedded
	private FahrradrouteName name;

	@Embedded
	private TfisId tfisId;

	@Embedded
	private DrouteId drouteId;

	private String kurzbeschreibung;
	private String beschreibung;
	private String info;

	@Enumerated(EnumType.STRING)
	private Kategorie kategorie;

	@Enumerated(EnumType.STRING)
	private Tourenkategorie tourenkategorie;

	private Laenge offizielleLaenge;
	private String homepage;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit verantwortlich;

	@Nullable
	private Hoehenunterschied anstieg;
	@Nullable
	private Hoehenunterschied abstieg;
	private String emailAnsprechpartner;
	private String lizenz;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "fahrradroute_linkszuweiterenmedien")
	@Column(name = "link")
	private Set<String> linksZuWeiterenMedien;

	private String lizenzNamensnennung;

	@NotAudited
	private LocalDateTime zuletztBearbeitet;

	@ElementCollection(fetch = FetchType.LAZY)
	@OrderColumn(name = "fahrradroute_kantenabschnitte_order")
	@CollectionTable(name = "fahrradroute_kantenabschnitte")
	private List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug;

	// koennen alles null sein, wenn es keine Geometrie gibt und somit auch keinen Netzbezug
	@Nullable
	private Geometry originalGeometrie;
	@Nullable
	private Geometry iconLocation;
	@Nullable
	private Geometry netzbezugLineString; // TODO: Das sollte eig. vom Datentyp LineString sein

	// Speichert die Liste von Wegpunkten (Linestring ist eine Liste von Coordinates)
	@Nullable
	private Geometry stuetzpunkte;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "fahrradroute_id")
	private List<FahrradrouteVariante> varianten;

	@Embedded
	@Getter(AccessLevel.NONE)
	private FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation;

	@NotAudited
	@ElementCollection(fetch = FetchType.LAZY)
	@OrderColumn(name = "fahrradroute_profil_eigenschaften_order")
	@CollectionTable(name = "fahrradroute_profil_eigenschaften")
	private List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften;

	private Long customProfileId;

	private boolean geloescht;

	private boolean veroeffentlicht;

	@Builder(access = AccessLevel.PUBLIC, toBuilder = true)
	private Fahrradroute(Long id,
		Long version,
		ToubizId toubizId,
		TfisId tfisId,
		DrouteId drouteId,
		FahrradrouteTyp fahrradrouteTyp,
		FahrradrouteName name,
		String kurzbeschreibung,
		String beschreibung,
		String info,
		Laenge offizielleLaenge,
		Tourenkategorie tourenkategorie,
		Kategorie kategorie,
		String homepage,
		Verwaltungseinheit verantwortlich,
		Hoehenunterschied anstieg,
		Hoehenunterschied abstieg,
		String emailAnsprechpartner,
		String lizenz,
		String lizenzNamensnennung,
		LocalDateTime zuletztBearbeitet,
		Set<String> linksZuWeiterenMedien,
		Geometry originalGeometrie,
		Geometry iconLocation,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		Geometry netzbezugLineString,
		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften,
		Geometry stuetzpunkte,
		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation,
		List<FahrradrouteVariante> varianten,
		boolean veroeffentlicht,
		Long customProfileId) {
		super(id, version);
		require(fahrradrouteTyp, notNullValue());
		require(name, notNullValue());
		require(kurzbeschreibung, notNullValue());
		require(beschreibung, notNullValue());
		require(info, notNullValue());
		require(kategorie, notNullValue());
		require(tourenkategorie, notNullValue());
		require(kurzbeschreibung.length() <= 500);
		require(homepage, notNullValue());
		// require(verantwortlich, notNullValue()); // darf erstmal null sein
		require(lizenz, notNullValue());
		require(lizenzNamensnennung, notNullValue());
		require(abschnittsweiserKantenBezug, notNullValue());
		require(zuletztBearbeitet, notNullValue());
		require(varianten, notNullValue());
		require(linearReferenzierteProfilEigenschaften, notNullValue());
		this.toubizId = toubizId;
		this.tfisId = tfisId;
		this.drouteId = drouteId;
		this.fahrradrouteTyp = fahrradrouteTyp;
		this.name = name;
		this.kurzbeschreibung = kurzbeschreibung;
		this.beschreibung = beschreibung;
		this.info = info;
		this.tourenkategorie = tourenkategorie;
		this.kategorie = kategorie;
		this.offizielleLaenge = offizielleLaenge;
		this.homepage = homepage;
		this.verantwortlich = verantwortlich;
		this.anstieg = anstieg;
		this.abstieg = abstieg;
		this.emailAnsprechpartner = emailAnsprechpartner;
		this.lizenz = lizenz;
		this.lizenzNamensnennung = lizenzNamensnennung;
		this.iconLocation = iconLocation;
		this.abschnittsweiserKantenBezug = abschnittsweiserKantenBezug;
		this.stuetzpunkte = stuetzpunkte;
		this.netzbezugLineString = netzbezugLineString;
		this.linearReferenzierteProfilEigenschaften = linearReferenzierteProfilEigenschaften;
		this.originalGeometrie = originalGeometrie;
		this.linksZuWeiterenMedien = linksZuWeiterenMedien != null ? new HashSet<>(linksZuWeiterenMedien)
			: new HashSet<>();
		this.fahrradroutenMatchingAndRoutingInformation = fahrradroutenMatchingAndRoutingInformation;
		this.zuletztBearbeitet = zuletztBearbeitet;
		this.varianten = varianten;
		this.veroeffentlicht = veroeffentlicht;
		this.customProfileId = customProfileId;

		RadVisDomainEventPublisher.publish(new FahrradrouteCreatedEvent(this));
	}

	/**
	 * Fachlicher Konstruktor zum Erstellen von Fahrradouten im FE
	 */
	public Fahrradroute(
		FahrradrouteName name,
		String beschreibung,
		Kategorie kategorie,
		Verwaltungseinheit verantwortlich,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		LineString netzbezugLineString,
		LineString stuetzpunkte,
		List<LinearReferenzierteProfilEigenschaften> profilEigenschaften,
		Long customProfileId) {
		this(
			null,
			null,
			null,
			null,
			null,
			FahrradrouteTyp.RADVIS_ROUTE,
			name,
			"",
			beschreibung,
			"",
			null,
			Tourenkategorie.RADTOUR,
			kategorie,
			"",
			verantwortlich,
			null,
			null,
			"",
			"",
			"",
			LocalDateTime.now(),
			Collections.emptySet(),
			null,
			stuetzpunkte.getStartPoint(),
			abschnittsweiserKantenBezug,
			netzbezugLineString,
			profilEigenschaften,
			stuetzpunkte,
			new FahrradroutenMatchingAndRoutingInformation(),
			new ArrayList<>(),
			false,
			customProfileId);
		require(stuetzpunkte, notNullValue());
	}

	/**
	 * Import Toubiz
	 */
	public Fahrradroute(
		ToubizId toubizId,
		FahrradrouteTyp fahrradrouteTyp,
		FahrradrouteName name,
		String beschreibung,
		String kurzbezeichnung,
		String info,
		Laenge offizielleLaenge,
		Tourenkategorie tourenkategorie,
		Kategorie kategorie,
		String homepage,
		Verwaltungseinheit verantwortlich,
		String emailAnsprechpartner,
		String lizenz,
		LocalDateTime zuletztBearbeitet,
		List<String> linksZuWeiterenMedien,
		String lizenzNamensnennung,
		Geometry originalGeometrie,
		Geometry iconLocation,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		LineString netzbezugLineString,
		List<LinearReferenzierteProfilEigenschaften> profilEigenschaften,
		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation) {
		this(
			null,
			null,
			toubizId,
			null,
			null,
			fahrradrouteTyp,
			name,
			kurzbezeichnung,
			beschreibung,
			info,
			offizielleLaenge,
			tourenkategorie,
			kategorie,
			homepage,
			verantwortlich,
			null,
			null,
			emailAnsprechpartner,
			lizenz,
			lizenzNamensnennung,
			zuletztBearbeitet,
			new HashSet<>(linksZuWeiterenMedien),
			originalGeometrie,
			iconLocation,
			abschnittsweiserKantenBezug,
			netzbezugLineString,
			profilEigenschaften,
			// Verlauf von Toubiz-Routen (nicht LRFW) koennen nicht in RadVis bearbeitet werden -> man braucht keine
			// Stuetzpunkte
			null,
			fahrradroutenMatchingAndRoutingInformation,
			new ArrayList<>(),
			false,
			null);
	}

	/**
	 * Initialer Import TFIS-LRFW
	 */
	@Builder(buildMethodName = "buildLandesradfernweg")
	private Fahrradroute(
		TfisId tfisId,
		FahrradrouteName name,
		Geometry originalGeometrie,
		Geometry iconLocation,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		Geometry netzbezugLineString,
		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften,
		LocalDateTime zuletztBearbeitet) {
		this(
			null,
			null,
			null,
			tfisId,
			null,
			FahrradrouteTyp.RADVIS_ROUTE,
			name,
			"",
			"",
			"",
			null,
			Tourenkategorie.RADFERNWEG,
			Kategorie.LANDESRADFERNWEG,
			"",
			null,
			null,
			null,
			"",
			"",
			"",
			zuletztBearbeitet,
			new HashSet<>(),
			originalGeometrie,
			iconLocation,
			abschnittsweiserKantenBezug,
			netzbezugLineString,
			linearReferenzierteProfilEigenschaften,
			netzbezugLineString != null && netzbezugLineString.getGeometryType().equals("LineString")
				? FahrradrouteVariante.createDefaultStuetzpunkte(abschnittsweiserKantenBezug,
					(LineString) netzbezugLineString)
				: null,
			new FahrradroutenMatchingAndRoutingInformation(),
			new ArrayList<>(),
			false,
			null);
	}

	/**
	 * Import TFIS-Routen
	 */
	@Builder(buildMethodName = "buildTfisRoute")
	private Fahrradroute(
		Long id,
		TfisId tfisId,
		FahrradrouteName name,
		String kurzbeschreibung,
		String beschreibung,
		String info,
		Geometry originalGeometrie,
		Geometry iconLocation,
		Laenge offizielleLaenge,
		Kategorie kategorie,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		Geometry netzbezugLineString,
		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften,
		LocalDateTime zuletztBearbeitet) {
		this(
			id,
			null,
			null,
			tfisId,
			null,
			FahrradrouteTyp.TFIS_ROUTE,
			name,
			kurzbeschreibung,
			beschreibung,
			info,
			offizielleLaenge,
			Tourenkategorie.RADFERNWEG,
			kategorie,
			"",
			null,
			null,
			null,
			"",
			"",
			"",
			zuletztBearbeitet,
			new HashSet<>(),
			originalGeometrie,
			iconLocation,
			abschnittsweiserKantenBezug,
			netzbezugLineString,
			linearReferenzierteProfilEigenschaften,
			// Verlauf von TFIS-Routen (nicht LRFW) koennen nicht bearbeitet werden -> man braucht keine Stuetzpunkte
			null,
			new FahrradroutenMatchingAndRoutingInformation(),
			new ArrayList<>(),
			false,
			null);
	}

	/**
	 * Initialer Import D-Route
	 */
	public Fahrradroute(
		FahrradrouteName name,
		DrouteId drouteId,
		Geometry originalGeometrie,
		Geometry iconLocation,
		LocalDateTime zuletztBearbeitet) {
		this(
			null,
			null,
			null,
			null,
			drouteId,
			FahrradrouteTyp.RADVIS_ROUTE,
			name,
			"",
			"",
			"",
			null,
			Tourenkategorie.RADFERNWEG,
			Kategorie.D_ROUTE,
			"",
			null,
			null,
			null,
			"",
			"",
			"",
			zuletztBearbeitet,
			new HashSet<>(),
			originalGeometrie,
			iconLocation,
			new ArrayList<>(),
			null,
			new ArrayList<>(),
			null,
			new FahrradroutenMatchingAndRoutingInformation(),
			new ArrayList<>(),
			false,
			null);
	}

	/**
	 * update aus dem FE
	 */
	public void updateAttribute(
		ToubizId toubizId,
		String name,
		String kurzbeschreibung,
		String beschreibung,
		Kategorie kategorie,
		Tourenkategorie tourenkategorie,
		Laenge offizielleLaenge,
		String website,
		Verwaltungseinheit verantwortlich,
		String emailAnsprechpartner,
		String lizenz,
		String lizenzNamensnennung,
		List<FahrradrouteVariante> varianten,
		List<AbschnittsweiserKantenBezug> netzbezug,
		LineString stuetzpunkte,
		LineString netzbezugLineString,
		List<LinearReferenzierteProfilEigenschaften> profilEigenschaften,
		Long customProfileId) {
		require(toubizId == null || kategorie == Kategorie.LANDESRADFERNWEG, "toubizId darf nur für LRFW gesetzt sein");
		this.toubizId = toubizId;
		this.name = FahrradrouteName.of(name);
		this.kurzbeschreibung = kurzbeschreibung;
		this.beschreibung = beschreibung;
		this.kategorie = kategorie;
		this.tourenkategorie = tourenkategorie;
		this.offizielleLaenge = offizielleLaenge;
		this.homepage = website;
		this.verantwortlich = verantwortlich;
		this.emailAnsprechpartner = emailAnsprechpartner;
		this.lizenz = lizenz;
		this.lizenzNamensnennung = lizenzNamensnennung;
		this.varianten.clear();
		this.varianten.addAll(varianten);
		this.stuetzpunkte = stuetzpunkte;
		if (!netzbezug.isEmpty()) {
			this.abschnittsweiserKantenBezug.clear();
			this.abschnittsweiserKantenBezug.addAll(netzbezug);
		}
		this.iconLocation = stuetzpunkte != null ? stuetzpunkte.getStartPoint() : null;
		this.zuletztBearbeitet = LocalDateTime.now();
		this.netzbezugLineString = netzbezugLineString;
		this.linearReferenzierteProfilEigenschaften = profilEigenschaften;
		this.customProfileId = customProfileId;

		RadVisDomainEventPublisher.publish(new FahrradrouteUpdatedEvent(this));
	}

	public void updateAbgeleiteteRoutenInformationen(Hoehenunterschied anstieg, Hoehenunterschied abstieg) {
		this.anstieg = anstieg;
		this.abstieg = abstieg;
		this.zuletztBearbeitet = LocalDateTime.now();
	}

	public void updateLinearReferenzierteProfilEigenschaften(
		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften) {
		this.linearReferenzierteProfilEigenschaften = linearReferenzierteProfilEigenschaften;
		this.zuletztBearbeitet = LocalDateTime.now();
	}

	public Fahrradroute merge(Fahrradroute route) {
		this.abschnittsweiserKantenBezug = route.abschnittsweiserKantenBezug;
		this.netzbezugLineString = route.netzbezugLineString;
		this.linearReferenzierteProfilEigenschaften = route.linearReferenzierteProfilEigenschaften;

		this.fahrradroutenMatchingAndRoutingInformation = route.fahrradroutenMatchingAndRoutingInformation;
		this.originalGeometrie = route.originalGeometrie;
		this.fahrradrouteTyp = route.fahrradrouteTyp;
		this.kategorie = route.kategorie;
		this.tfisId = route.tfisId;
		this.iconLocation = route.iconLocation;
		this.stuetzpunkte = route.stuetzpunkte;

		RadVisDomainEventPublisher.publish(new FahrradrouteUpdatedEvent(this));

		return mergeNonRouteDependentAttribute(route);
	}

	/**
	 * Bei Landesradfernwegen mergen wir nur Attribute von Toubiz ohne Verlauf
	 */
	public Fahrradroute mergeNonRouteDependentAttribute(Fahrradroute route) {
		this.toubizId = route.toubizId;
		this.name = route.name;
		this.offizielleLaenge = route.offizielleLaenge;
		this.info = route.info;
		this.zuletztBearbeitet = route.zuletztBearbeitet;
		this.kurzbeschreibung = route.kurzbeschreibung;
		this.beschreibung = route.beschreibung;
		this.tourenkategorie = route.tourenkategorie;
		this.linksZuWeiterenMedien = route.linksZuWeiterenMedien;
		this.homepage = route.homepage;
		this.verantwortlich = route.verantwortlich;
		this.emailAnsprechpartner = route.emailAnsprechpartner;
		this.lizenz = route.lizenz;
		this.lizenzNamensnennung = route.lizenzNamensnennung;
		return this;
	}

	public List<AbschnittsweiserKantenBezug> getAbschnittsweiserKantenBezug() {
		return Collections.unmodifiableList(abschnittsweiserKantenBezug);
	}

	public double getLaengeDerHauptstrecke() {
		if (netzbezugLineString != null) {
			return netzbezugLineString.getLength();
		}
		return abschnittsweiserKantenBezug.stream().mapToDouble(
			aKB -> aKB.getLinearReferenzierterAbschnitt().relativeLaenge() * aKB.getKante().getGeometry().getLength())
			.sum();
	}

	public Optional<Geometry> getOriginalGeometrie() {
		return Optional.ofNullable(originalGeometrie);
	}

	public Optional<Geometry> getIconLocation() {
		return Optional.ofNullable(iconLocation);
	}

	public Optional<Verwaltungseinheit> getVerantwortlich() {
		return Optional.ofNullable(verantwortlich);
	}

	public Optional<Hoehenunterschied> getAnstieg() {
		return Optional.ofNullable(this.anstieg);
	}

	public Optional<Hoehenunterschied> getAbstieg() {
		return Optional.ofNullable(this.abstieg);
	}

	public Optional<Laenge> getOffizielleLaenge() {
		return Optional.ofNullable(this.offizielleLaenge);
	}

	public Optional<Geometry> getStuetzpunkte() {
		return Optional.ofNullable(this.stuetzpunkte);
	}

	public Optional<Geometry> getNetzbezugLineString() {
		return Optional.ofNullable(this.netzbezugLineString);
	}

	public void removeKanteFromNetzbezug(Long kanteId) {
		require(kanteId, notNullValue());
		abschnittsweiserKantenBezug.removeIf(kantenBezug -> kantenBezug.getKante().getId().equals(kanteId));
		varianten.forEach(v -> v.removeKanteFromNetzbezug(kanteId));
	}

	public void alsGeloeschtMarkieren() {
		this.geloescht = true;
	}

	public void veroeffentlichen() {
		this.veroeffentlicht = true;
	}

	public void veroeffentlichungZuruecknehmen() {
		this.veroeffentlicht = false;
	}

	/*
	 * Beim Setzten des Netzbezuges werden auch die Stuetzpunkte aus dem abschnittsweiserKantenBezug berechnet und die
	 * alten ueberschrieben.
	 */
	public void updateNetzbezug(Optional<Geometry> netzbezugLineString,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften,
		Optional<Geometry> originalGeometrie) {
		require(netzbezugLineString, notNullValue());
		if (netzbezugLineString.isPresent()) {
			require(netzbezugLineString.get().getGeometryType().equals(Geometry.TYPENAME_LINESTRING));
		}
		require(abschnittsweiserKantenBezug, notNullValue());
		require(!abschnittsweiserKantenBezug.isEmpty());
		require(originalGeometrie, notNullValue());

		originalGeometrie.ifPresent(geometry -> this.originalGeometrie = geometry);
		this.netzbezugLineString = netzbezugLineString.orElse(null);
		this.abschnittsweiserKantenBezug.clear();
		this.abschnittsweiserKantenBezug.addAll(abschnittsweiserKantenBezug);
		this.linearReferenzierteProfilEigenschaften.clear();
		this.linearReferenzierteProfilEigenschaften.addAll(linearReferenzierteProfilEigenschaften);

		if (netzbezugLineString.isPresent()) {
			// Immer wenn der Netzbezug mit neuen Werten geupdated wird wollen wir auch die Stuetzpunkte
			// neu setzten, da die eventuell alten noch vorhandenen nicht mehr zu der jetzt gesetzten Route passen
			this.stuetzpunkte = FahrradrouteVariante.createDefaultStuetzpunkte(abschnittsweiserKantenBezug,
				(LineString) netzbezugLineString.get());
		} else {
			this.stuetzpunkte = null;
		}

		RadVisDomainEventPublisher.publish(new FahrradrouteUpdatedEvent(this));
	}

	public Optional<FahrradroutenMatchingAndRoutingInformation> getFahrradroutenMatchingAndRoutingInformation() {
		return Optional.ofNullable(this.fahrradroutenMatchingAndRoutingInformation);
	}

	public Optional<FahrradrouteVariante> findFahrradrouteVariante(TfisId varianteId) {
		return varianten.stream().filter(fV -> fV.getTfisId().equals(varianteId)).findFirst();
	}

	public Optional<Long> getCustomProfileId() {
		return Optional.ofNullable(this.customProfileId);
	}

	public void replaceFahrradrouteVarianten(List<FahrradrouteVariante> fahrradrouteVarianten) {
		this.varianten.clear();
		this.varianten.addAll(fahrradrouteVarianten);
		RadVisDomainEventPublisher.publish(new FahrradrouteUpdatedEvent(this));
	}

	public void setMatchingAndRoutingInformation(
		FahrradroutenMatchingAndRoutingInformation matchingAndRoutingInformation) {
		this.fahrradroutenMatchingAndRoutingInformation = matchingAndRoutingInformation;
	}
}
