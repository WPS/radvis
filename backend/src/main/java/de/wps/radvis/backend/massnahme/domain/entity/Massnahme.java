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

package de.wps.radvis.backend.massnahme.domain.entity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.OptimisticLock;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

import com.google.common.base.Objects;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.kommentar.domain.entity.Kommentar;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.massnahme.domain.event.MassnahmeChangedEvent;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Kostenannahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.Realisierungshilfe;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.VerbaID;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.AbstractEntityWithNetzbezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
@Entity
@ToString
public class Massnahme extends AbstractEntityWithNetzbezug {

	@Enumerated(EnumType.STRING)
	private Realisierungshilfe realisierungshilfe;

	@Getter
	private boolean geloescht;

	@Getter
	private MassnahmenPaketId massnahmenPaketId;

	@Getter
	private Geometry originalRadNETZGeometrie;

	@Getter
	private Bezeichnung bezeichnung;

	@Getter
	@ElementCollection(fetch = FetchType.LAZY)
	@Enumerated(EnumType.STRING)
	private Set<Massnahmenkategorie> massnahmenkategorien;

	@Getter
	@Embedded
	private MassnahmeNetzBezug netzbezug;

	@Embedded
	private Durchfuehrungszeitraum durchfuehrungszeitraum;

	@Getter
	@Enumerated(EnumType.STRING)
	private Umsetzungsstatus umsetzungsstatus;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@Getter
	@NotAudited
	private DokumentListe dokumentListe;

	@Getter
	private Boolean veroeffentlicht;

	@Getter
	private Boolean planungErforderlich;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@Getter
	@NotAudited
	private KommentarListe kommentarListe;

	private MaViSID maViSID;

	private VerbaID verbaID;

	private LGVFGID lgvfgid;

	private Prioritaet prioritaet;

	@Embedded
	private Kostenannahme kostenannahme;

	@ElementCollection
	@Getter
	@Enumerated(EnumType.STRING)
	private Set<Netzklasse> netzklassen;

	@ManyToOne
	@Getter
	@Audited(targetAuditMode = NOT_AUDITED)
	private Benutzer benutzerLetzteAenderung;

	@Getter
	@NotAudited
	private LocalDateTime letzteAenderung;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit baulastZustaendiger;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit unterhaltsZustaendiger;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit zustaendiger;

	private MassnahmeKonzeptID massnahmeKonzeptId;

	@Getter
	@Enumerated(EnumType.STRING)
	private SollStandard sollStandard;

	@Enumerated(EnumType.STRING)
	private Handlungsverantwortlicher handlungsverantwortlicher;

	@Getter
	@Enumerated(EnumType.STRING)
	private Konzeptionsquelle konzeptionsquelle;
	private String sonstigeKonzeptionsquelle;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Umsetzungsstand umsetzungsstand;

	@OneToMany
	@NotAudited
	@Getter
	@OptimisticLock(excluded = true)
	private Set<Benutzer> zuBenachrichtigendeBenutzer;

	@Getter
	private boolean archiviert;

	@Getter(value = AccessLevel.PACKAGE)
	private MultiPoint netzbezugSnapshotPoints;
	@Getter(value = AccessLevel.PACKAGE)
	private MultiLineString netzbezugSnapshotLines;

	@Builder(builderMethodName = "privateBuilder", toBuilder = true)
	private Massnahme(Long id, Long version, Bezeichnung bezeichnung,
		Set<Massnahmenkategorie> massnahmenkategorien, MassnahmeNetzBezug netzbezug,
		Durchfuehrungszeitraum durchfuehrungszeitraum, Umsetzungsstatus umsetzungsstatus,
		DokumentListe dokumentListe, Boolean veroeffentlicht, Boolean planungErforderlich,
		KommentarListe kommentarListe, MaViSID maViSID, VerbaID verbaID, LGVFGID lgvfgid, Prioritaet prioritaet,
		Kostenannahme kostenannahme, Set<Netzklasse> netzklassen, Benutzer benutzerLetzteAenderung,
		LocalDateTime letzteAenderung, Verwaltungseinheit baulastZustaendiger,
		Verwaltungseinheit unterhaltsZustaendiger, Verwaltungseinheit zustaendiger,
		MassnahmeKonzeptID massnahmeKonzeptId, SollStandard sollStandard,
		Handlungsverantwortlicher handlungsverantwortlicher, Konzeptionsquelle konzeptionsquelle,
		String sonstigeKonzeptionsquelle, Geometry originalRadNETZGeometrie, MassnahmenPaketId massnahmenPaketId,
		Set<Benutzer> zuBenachrichtigendeBenutzer, boolean geloescht, Realisierungshilfe realisierungshilfe) {
		super(id, version);
		require(bezeichnung, notNullValue());
		require(massnahmenkategorien, notNullValue());
		require(massnahmenkategorien, is(not(empty())));
		require(areKategorienValidForKonzeptionsquelle(konzeptionsquelle, massnahmenkategorien),
			"Nicht alle Kategorien sind für die gewählte Konzeptionsquelle erlaubt.");
		require(zustaendiger, notNullValue());
		require(hatNurEineMassnahmenkategorieProOberkategorie(massnahmenkategorien),
			"Nur eine Maßnahmenkategorie pro Oberkategorie erlaubt");
		require(netzbezug, notNullValue());
		require(umsetzungsstatus, notNullValue());
		require(dokumentListe, notNullValue());
		require(kommentarListe, notNullValue());
		require(veroeffentlicht, notNullValue());
		require(planungErforderlich, notNullValue());
		require(letzteAenderung, notNullValue());
		require(benutzerLetzteAenderung, notNullValue());
		require(pflichtFelderAbPlanung(umsetzungsstatus, baulastZustaendiger, durchfuehrungszeitraum,
			handlungsverantwortlicher),
			"Durchführungszeitraum, Baulastträger und Handlungsverantwortlicher sind ab Status 'Planung' Pflichtfelder.");
		require(sonstigeKonzeptionsquelleNichtLeerWennSonstigeKonzeptionsquelle(konzeptionsquelle,
			sonstigeKonzeptionsquelle),
			"Sonstige Konzeptionsquelle ist ein Pflichtfeld, wenn Konzeptionsquelle 'Sonstige' ist.");

		this.bezeichnung = bezeichnung;
		this.massnahmenkategorien = new HashSet<>(massnahmenkategorien);
		this.netzbezug = netzbezug;
		this.durchfuehrungszeitraum = durchfuehrungszeitraum;
		this.dokumentListe = dokumentListe;
		this.veroeffentlicht = veroeffentlicht;
		this.planungErforderlich = planungErforderlich;
		this.kommentarListe = kommentarListe;
		this.maViSID = maViSID;
		this.verbaID = verbaID;
		this.lgvfgid = lgvfgid;
		this.prioritaet = prioritaet;
		this.kostenannahme = kostenannahme;
		this.netzklassen = new HashSet<>(netzklassen);
		this.benutzerLetzteAenderung = benutzerLetzteAenderung;
		this.letzteAenderung = letzteAenderung;
		this.baulastZustaendiger = baulastZustaendiger;
		this.unterhaltsZustaendiger = unterhaltsZustaendiger;
		this.zustaendiger = zustaendiger;
		this.massnahmeKonzeptId = massnahmeKonzeptId;
		this.sollStandard = sollStandard;
		this.handlungsverantwortlicher = handlungsverantwortlicher;
		this.sonstigeKonzeptionsquelle = sonstigeKonzeptionsquelle;
		this.originalRadNETZGeometrie = originalRadNETZGeometrie;
		this.massnahmenPaketId = massnahmenPaketId;
		this.zuBenachrichtigendeBenutzer = zuBenachrichtigendeBenutzer;
		if (this.zuBenachrichtigendeBenutzer == null) {
			this.zuBenachrichtigendeBenutzer = new HashSet<>();
		}
		this.geloescht = geloescht;
		this.realisierungshilfe = realisierungshilfe;
		this.archiviert = false;

		aktualisiereKonzeptionsquelleUndUmsetzungsstand(konzeptionsquelle, umsetzungsstatus);

		ensure(umsetzungsstandValidForKonzeptionsquelle());
	}

	/**
	 * Dieser fachliche Konstruktor ist für die Erstellung der Massnahmen aus dem FE heraus gedacht. Nix anderes.
	 */
	public Massnahme(Bezeichnung bezeichnung,
		Set<Massnahmenkategorie> massnahmenkategorien,
		MassnahmeNetzBezug netzbezug,
		Umsetzungsstatus umsetzungsstatus,
		Boolean veroeffentlicht,
		Boolean planungErforderlich,
		Durchfuehrungszeitraum durchfuehrungszeitraum,
		Verwaltungseinheit baulastZustaendiger,
		Verwaltungseinheit zustaendiger,
		LocalDateTime letzteAenderung,
		Benutzer benutzerLetzteAenderung,
		SollStandard sollStandard,
		Handlungsverantwortlicher handlungsverantwortlicher,
		Konzeptionsquelle konzeptionsquelle,
		String sonstigeKonzeptionsquelle) {
		this(null, null, bezeichnung, massnahmenkategorien, netzbezug, durchfuehrungszeitraum, umsetzungsstatus,
			new DokumentListe(), veroeffentlicht, planungErforderlich, new KommentarListe(), null, null, null,
			null, null, new HashSet<>(), benutzerLetzteAenderung, letzteAenderung, baulastZustaendiger, null,
			zustaendiger, null, sollStandard, handlungsverantwortlicher, konzeptionsquelle, sonstigeKonzeptionsquelle,
			null, null, new HashSet<>(), false, null);

		require(!konzeptionsquelle.equals(Konzeptionsquelle.RADNETZ_MASSNAHME), "Für Konzeptionsquelle "
			+ Konzeptionsquelle.RADNETZ_MASSNAHME + " dürfen keine neuen Maßnahmen angelegt werden.");
	}

	public static MassnahmeBuilder builder() {
		return privateBuilder();
	}

	public void update(Bezeichnung bezeichnung,
		Set<Massnahmenkategorie> massnahmenkategorien, MassnahmeNetzBezug netzbezug,
		Durchfuehrungszeitraum durchfuehrungszeitraum, Umsetzungsstatus umsetzungsstatus,
		Boolean veroeffentlicht, Boolean planungErforderlich,
		MaViSID maViSID, VerbaID verbaID, LGVFGID lgvfgid, Prioritaet prioritaet, Kostenannahme kostenannahme,
		Set<Netzklasse> netzklassen, Benutzer benutzerLetzteAenderung, LocalDateTime letzteAenderung,
		Verwaltungseinheit baulastZustaendiger, Verwaltungseinheit unterhaltsZustaendiger,
		Verwaltungseinheit zustaendiger,
		MassnahmeKonzeptID massnahmeKonzeptID, SollStandard sollStandard,
		Handlungsverantwortlicher handlungsverantwortlicher, Konzeptionsquelle konzeptionsquelle,
		String sonstigeKonzeptionsquelle, Realisierungshilfe realisierungshilfe) {
		require(bezeichnung, notNullValue());
		require(massnahmenkategorien, notNullValue());
		require(massnahmenkategorien, is(not(empty())));
		require(areKategorienValidForKonzeptionsquelle(konzeptionsquelle, massnahmenkategorien),
			"Nicht alle Kategorien sind für die gewählte Konzeptionsquelle erlaubt.");
		require(hatNurEineMassnahmenkategorieProOberkategorie(massnahmenkategorien),
			"Nur eine Massnahmenkategorie pro Oberkategorie erlaubt");
		require(netzbezug, notNullValue());
		require(umsetzungsstatus, notNullValue());
		require(veroeffentlicht, notNullValue());
		require(planungErforderlich, notNullValue());
		require(letzteAenderung, notNullValue());
		require(benutzerLetzteAenderung, notNullValue());
		require(pflichtFelderAbPlanung(umsetzungsstatus, baulastZustaendiger, durchfuehrungszeitraum,
			handlungsverantwortlicher),
			"Durchführungszeitpunkt und Zuständiger-Baulast sind ab Status 'Planung' ein Pflichtfeld.");
		require(sonstigeKonzeptionsquelleNichtLeerWennSonstigeKonzeptionsquelle(konzeptionsquelle,
			sonstigeKonzeptionsquelle),
			"Sonstige Konzeptionsquelle ist ein Pflichtfeld, wenn Konzeptionsquelle 'Sonstige' ist.");
		require(!archiviert);

		aktualisiereKonzeptionsquelleUndUmsetzungsstand(konzeptionsquelle, umsetzungsstatus);

		this.bezeichnung = bezeichnung;
		this.massnahmenkategorien = massnahmenkategorien;
		this.netzbezug = netzbezug;
		this.durchfuehrungszeitraum = durchfuehrungszeitraum;
		this.veroeffentlicht = veroeffentlicht;
		this.planungErforderlich = planungErforderlich;
		this.maViSID = maViSID;
		this.verbaID = verbaID;
		this.lgvfgid = lgvfgid;
		this.prioritaet = prioritaet;
		this.kostenannahme = kostenannahme;
		this.netzklassen = netzklassen;
		this.benutzerLetzteAenderung = benutzerLetzteAenderung;
		this.letzteAenderung = letzteAenderung;
		this.baulastZustaendiger = baulastZustaendiger;
		this.unterhaltsZustaendiger = unterhaltsZustaendiger;
		this.zustaendiger = zustaendiger;
		this.massnahmeKonzeptId = massnahmeKonzeptID;
		this.sollStandard = sollStandard;
		this.handlungsverantwortlicher = handlungsverantwortlicher;
		this.sonstigeKonzeptionsquelle = sonstigeKonzeptionsquelle;
		this.realisierungshilfe = realisierungshilfe;

		ensure(umsetzungsstandValidForKonzeptionsquelle());

		RadVisDomainEventPublisher.publish(new MassnahmeChangedEvent(this.id));
	}

	private boolean umsetzungsstandValidForKonzeptionsquelle() {
		if (Konzeptionsquelle.isRadNetzKonzeptionsquelle(konzeptionsquelle)) {
			return umsetzungsstand != null;
		}

		return umsetzungsstand == null;
	}

	private void aktualisiereKonzeptionsquelleUndUmsetzungsstand(Konzeptionsquelle konzeptionsquelle,
		Umsetzungsstatus neuerUmsetzungsstatus) {
		require(canUpdateKonzeptionsquelle(konzeptionsquelle));

		this.passeUmsetzungsstandAnNeueKonzeptionsquelleAn(konzeptionsquelle);

		this.getUmsetzungsstand().ifPresent(umsetzungsstand -> {
			if (neuerUmsetzungsstatus != this.umsetzungsstatus &&
				(neuerUmsetzungsstatus == Umsetzungsstatus.STORNIERT ||
					neuerUmsetzungsstatus == Umsetzungsstatus.UMGESETZT)) {
				umsetzungsstand.fordereAktualisierungAn();
			}
		});

		this.umsetzungsstatus = neuerUmsetzungsstatus;
		this.konzeptionsquelle = konzeptionsquelle;
	}

	public Optional<GeometryCollection> getNetzbezugSnapshot() {
		if (netzbezugSnapshotLines == null && netzbezugSnapshotPoints == null) {
			return Optional.empty();
		}

		List<Geometry> netzbezugSnapshots = new ArrayList<>();
		if (netzbezugSnapshotLines != null) {
			for (int i = 0; i < netzbezugSnapshotLines.getNumGeometries(); i++) {
				netzbezugSnapshots.add(netzbezugSnapshotLines.getGeometryN(i));
			}
		}
		if (netzbezugSnapshotPoints != null) {
			for (int i = 0; i < netzbezugSnapshotPoints.getNumGeometries(); i++) {
				netzbezugSnapshots.add(netzbezugSnapshotPoints.getGeometryN(i));
			}
		}

		return Optional.of(new GeometryCollection(netzbezugSnapshots.toArray(new Geometry[] {}),
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()));
	}

	public boolean canUpdateKonzeptionsquelle(Konzeptionsquelle neueKonzeptionsquelle) {
		if (Objects.equal(this.konzeptionsquelle, Konzeptionsquelle.RADNETZ_MASSNAHME)) {
			return neueKonzeptionsquelle.equals(Konzeptionsquelle.RADNETZ_MASSNAHME);
		}

		if (Objects.equal(this.konzeptionsquelle, Konzeptionsquelle.RADNETZ_MASSNAHME_2024)) {
			return neueKonzeptionsquelle.equals(Konzeptionsquelle.RADNETZ_MASSNAHME_2024);
		}

		return true;
	}

	public Optional<Durchfuehrungszeitraum> getDurchfuehrungszeitraum() {
		if (durchfuehrungszeitraum != null && durchfuehrungszeitraum.getGeplanterUmsetzungsstartJahr() == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(durchfuehrungszeitraum);
	}

	public Optional<String> getSonstigeKonzeptionsquelle() {
		if (this.konzeptionsquelle == Konzeptionsquelle.SONSTIGE) {
			return Optional.ofNullable(this.sonstigeKonzeptionsquelle);
		}
		return Optional.empty();
	}

	public Optional<Handlungsverantwortlicher> getHandlungsverantwortlicher() {
		return Optional.ofNullable(handlungsverantwortlicher);
	}

	public Optional<MassnahmeKonzeptID> getMassnahmeKonzeptID() {
		return Optional.ofNullable(massnahmeKonzeptId);
	}

	public Optional<LGVFGID> getLGVFGID() {
		return Optional.ofNullable(lgvfgid);
	}

	public Optional<MaViSID> getMaViSID() {
		return Optional.ofNullable(maViSID);
	}

	public Optional<VerbaID> getVerbaID() {
		return Optional.ofNullable(verbaID);
	}

	public Optional<Prioritaet> getPrioritaet() {
		return Optional.ofNullable(prioritaet);
	}

	public Optional<Kostenannahme> getKostenannahme() {
		return Optional.ofNullable(kostenannahme);
	}

	public Optional<Verwaltungseinheit> getBaulastZustaendiger() {
		return Optional.ofNullable(baulastZustaendiger);
	}

	public Optional<Verwaltungseinheit> getunterhaltsZustaendiger() {
		return Optional.ofNullable(unterhaltsZustaendiger);
	}

	public Optional<Verwaltungseinheit> getZustaendiger() {
		return Optional.ofNullable(zustaendiger);
	}

	public Optional<Umsetzungsstand> getUmsetzungsstand() {
		return Optional.ofNullable(umsetzungsstand);
	}

	public Optional<Realisierungshilfe> getRealisierungshilfe() {
		return Optional.ofNullable(realisierungshilfe);
	}

	private void passeUmsetzungsstandAnNeueKonzeptionsquelleAn(Konzeptionsquelle konzeptionsquelle) {
		if (Konzeptionsquelle.isRadNetzKonzeptionsquelle(konzeptionsquelle)
			&& !Konzeptionsquelle.isRadNetzKonzeptionsquelle(this.konzeptionsquelle)) {
			this.umsetzungsstand = new Umsetzungsstand();
		}
	}

	public static boolean pflichtFelderAbPlanung(Umsetzungsstatus umsetzungsstatus,
		Verwaltungseinheit baulastZustaendiger,
		Durchfuehrungszeitraum durchfuehrungszeitraum, Handlungsverantwortlicher handlungsverantwortlicher) {
		Long baulastZustaendigerId = baulastZustaendiger == null ? null : baulastZustaendiger.getId();
		return pflichtFelderAbPlanung(umsetzungsstatus, baulastZustaendigerId, durchfuehrungszeitraum,
			handlungsverantwortlicher);
	}

	public static boolean pflichtFelderAbPlanung(Umsetzungsstatus umsetzungsstatus, Long baulastZustaendigerId,
		Durchfuehrungszeitraum durchfuehrungszeitraum, Handlungsverantwortlicher handlungsverantwortlicher) {

		if (Umsetzungsstatus.isAbPlanung(umsetzungsstatus)) {
			return handlungsverantwortlicher != null && baulastZustaendigerId != null && durchfuehrungszeitraum != null
				&& durchfuehrungszeitraum.getGeplanterUmsetzungsstartJahr() != null;
		}
		return true;
	}

	public static boolean sonstigeKonzeptionsquelleNichtLeerWennSonstigeKonzeptionsquelle(
		Konzeptionsquelle konzeptionsquelle, String sonstigeKonzeptionsquelle) {
		return konzeptionsquelle != Konzeptionsquelle.SONSTIGE
			|| (sonstigeKonzeptionsquelle != null && !sonstigeKonzeptionsquelle.isEmpty());
	}

	public static boolean areKategorienValidForKonzeptionsquelle(
		Konzeptionsquelle konzeptionsquelle, Set<Massnahmenkategorie> massnahmenkategorien) {
		require(massnahmenkategorien, notNullValue());

		if (konzeptionsquelle == null) {
			return true;
		}

		return massnahmenkategorien.stream()
			.allMatch(kategorie -> kategorie.isValidForKonzeptionsquelle(konzeptionsquelle));
	}

	public static boolean hatNurEineMassnahmenkategorieProOberkategorie(Set<Massnahmenkategorie> massnahmenkategorien) {
		return massnahmenkategorien.stream()
			.map(Massnahmenkategorie::getMassnahmenOberkategorie)
			.collect(Collectors.toSet()).size() == massnahmenkategorien.size();
	}

	public void updateMassnahmeUmgesetztFuerUmsetzungsstandabfrageImport() {
		require(pflichtFelderAbPlanung(Umsetzungsstatus.UMGESETZT, baulastZustaendiger, durchfuehrungszeitraum,
			handlungsverantwortlicher));
		this.umsetzungsstatus = Umsetzungsstatus.UMGESETZT;
	}

	public void updateBaulastZustaendiger(Verwaltungseinheit baulastZustaendiger) {
		require(baulastZustaendiger, notNullValue());
		this.baulastZustaendiger = baulastZustaendiger;
	}

	public Optional<Geometry> berechneMittelpunkt() {
		Set<AbschnittsweiserKantenSeitenBezug> immutableSeitenabschnittsKantenSeitenAbschnitte = this.getNetzbezug()
			.getImmutableKantenAbschnittBezug();
		if (!immutableSeitenabschnittsKantenSeitenAbschnitte.isEmpty()) {
			return Optional.of(
				immutableSeitenabschnittsKantenSeitenAbschnitte.stream().findFirst().get().getKante().getGeometry());
		}

		Set<Knoten> immutableKnotenBezug = this.getNetzbezug().getImmutableKnotenBezug();
		if (!immutableKnotenBezug.isEmpty()) {
			return Optional.of(immutableKnotenBezug.stream().findFirst().get().getPoint());
		}

		if (!this.getNetzbezug().getImmutableKantenPunktBezug().isEmpty()) {
			PunktuellerKantenSeitenBezug punktuellerKantenSeitenBezug = this.getNetzbezug()
				.getImmutableKantenPunktBezug()
				.stream().findFirst().get();
			return Optional.of(punktuellerKantenSeitenBezug.getPointGeometry());
		}

		return Optional.empty();
	}

	public void addDokument(Dokument dokument) {
		require(!archiviert);

		if (this.dokumentListe == null) {
			this.dokumentListe = new DokumentListe();
		}
		this.dokumentListe.addDokument(dokument);
		RadVisDomainEventPublisher.publish(new MassnahmeChangedEvent(this.id));
	}

	public void addKommentar(Kommentar kommentar) {
		this.kommentarListe.addKommentar(kommentar);
		RadVisDomainEventPublisher.publish(new MassnahmeChangedEvent(this.id));
	}

	public void deleteDokument(long dokumentId) {
		require(!archiviert);

		this.dokumentListe.deleteDokument(dokumentId);
	}

	public boolean sollBenutzerBenachrichtigtWerden(Benutzer benutzer) {
		return zuBenachrichtigendeBenutzer.contains(benutzer);
	}

	public void fuegeZuBenachrichtigendenBenutzerHinzu(Benutzer benutzer) {
		this.zuBenachrichtigendeBenutzer.add(benutzer);
	}

	public void entferneZuBenachrichtigendenBenutzer(Benutzer benutzer) {
		this.zuBenachrichtigendeBenutzer.remove(benutzer);
	}

	public void alsGeloeschtMarkieren() {
		this.geloescht = true;
	}

	public boolean isRadNETZMassnahme() {
		return Konzeptionsquelle.isRadNetzKonzeptionsquelle(konzeptionsquelle);
	}

	public void stornieren(Benutzer benutzerLetzteAenderung, LocalDateTime letzteAenderung) {
		this.umsetzungsstatus = Umsetzungsstatus.STORNIERT;
		this.benutzerLetzteAenderung = benutzerLetzteAenderung;
		this.letzteAenderung = letzteAenderung;
	}

	public void archivieren() {
		require(!archiviert);
		this.archiviert = true;

		List<LineString> lineStrings = this.netzbezug.getLineStrings();
		if (!lineStrings.isEmpty()) {
			this.netzbezugSnapshotLines = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createMultiLineString(lineStrings.toArray(LineString[]::new));
		}

		List<Point> points = this.netzbezug.getPoints();
		if (!points.isEmpty()) {
			this.netzbezugSnapshotPoints = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createMultiPoint(points.toArray(Point[]::new));
		}
	}

	public void archivierungAufheben() {
		require(archiviert);

		this.archiviert = false;
		this.netzbezugSnapshotLines = null;
		this.netzbezugSnapshotPoints = null;
	}

	public void removeKanteFromNetzbezug(Collection<Long> kantenIds) {
		require(kantenIds, notNullValue());
		netzbezug = netzbezug.withoutKanten(new HashSet<>(kantenIds));
	}

	@Override
	public void removeKnotenFromNetzbezug(Collection<Long> knotenIds) {
		require(knotenIds, notNullValue());
		netzbezug = netzbezug.withoutKnoten(new HashSet<>(knotenIds));
	}

	public void ersetzeKanteInNetzbezug(Kante zuErsetzendeKante, Set<Kante> zuErsetzenDurch,
		double erlaubteAbweichung) {
		require(zuErsetzenDurch, notNullValue());
		require(!zuErsetzenDurch.isEmpty());

		netzbezug = netzbezug.withKanteErsetzt(zuErsetzendeKante, zuErsetzenDurch, erlaubteAbweichung);
	}

	/**
	 * Ersetzt Knoten anhand übergebener Abbildung
	 * 
	 * @param ersatzKnoten:
	 *     ID des zu ersetzenden Knoten -> Ersatz-Knoten
	 */
	public void ersetzeKnotenInNetzbezug(Map<Long, Knoten> ersatzKnoten) {
		netzbezug = netzbezug.withKnotenErsetzt(ersatzKnoten);
	}
}
