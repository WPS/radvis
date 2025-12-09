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

package de.wps.radvis.backend.abstellanlage.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.Optional;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBeschreibung;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBetreiber;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenOrt;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenStatus;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenWeitereInformation;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlLademoeglichkeiten;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlSchliessfaecher;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlStellplaetze;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.ExterneAbstellanlagenId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProJahr;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProMonat;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProTag;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Groessenklasse;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.MobiDataQuellId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
public class Abstellanlage extends VersionierteEntity {
	public static class CsvHeader {
		public static final String RAD_VIS_ID = "RadVIS-ID";
		public static final String POSITION_X_UTM32_N = "Position X (UTM32_N)";
		public static final String POSITION_Y_UTM32_N = "Position Y (UTM32_N)";
		public static final String BETREIBER = "Betreiber";
		public static final String EXTERNE_ID = "Externe ID";
		public static final String QUELLSYSTEM = "Quellsystem";
		public static final String ZUSTAENDIG_IN_RAD_VIS = "Zuständig in RadVIS";
		public static final String ANZAHL_STELLPLAETZE = "Anzahl Stellplätze";
		public static final String ANZAHL_SCHLIESSFAECHER = "Anzahl Schließfächer";
		public static final String ANZAHL_LADEMOEGLICHKEITEN = "Anzahl Lademöglichkeiten";
		public static final String UEBERWACHT = "Überwacht";
		public static final String ABSTELLANLAGEN_ORT = "Ort";
		public static final String GROESSENKLASSE = "Größenklasse";
		public static final String STELLPLATZART = "Stellplatzart";
		public static final String UEBERDACHT = "Überdacht";
		public static final String GEBUEHREN_PRO_TAG = "Gebühren pro Tag (Cent)";
		public static final String GEBUEHREN_PRO_MONAT = "Gebühren pro Monat (Cent)";
		public static final String GEBUEHREN_PRO_JAHR = "Gebühren pro Jahr (Cent)";
		public static final String BESCHREIBUNG = "Beschreibung";
		public static final String WEITERE_INFORMATION = "Weitere Information";
		public static final String ABSTELLANLAGEN_STATUS = "Status";

		public static final List<String> ALL = List.of(RAD_VIS_ID,
			POSITION_X_UTM32_N,
			POSITION_Y_UTM32_N,
			BETREIBER,
			EXTERNE_ID,
			QUELLSYSTEM,
			ZUSTAENDIG_IN_RAD_VIS,
			ANZAHL_STELLPLAETZE,
			ANZAHL_SCHLIESSFAECHER,
			ANZAHL_LADEMOEGLICHKEITEN,
			UEBERWACHT,
			ABSTELLANLAGEN_ORT,
			GROESSENKLASSE,
			STELLPLATZART,
			UEBERDACHT,
			GEBUEHREN_PRO_TAG,
			GEBUEHREN_PRO_MONAT,
			GEBUEHREN_PRO_JAHR,
			BESCHREIBUNG,
			WEITERE_INFORMATION,
			ABSTELLANLAGEN_STATUS);
	}

	@Getter
	private Point geometrie;

	@Getter
	private AbstellanlagenBetreiber betreiber;

	private ExterneAbstellanlagenId externeId;

	private MobiDataQuellId mobiDataQuellId;

	@Getter
	@Enumerated(EnumType.STRING)
	private AbstellanlagenQuellSystem quellSystem;

	@Audited(targetAuditMode = NOT_AUDITED)
	@ManyToOne
	private Verwaltungseinheit zustaendig;

	private AnzahlStellplaetze anzahlStellplaetze;

	private AnzahlSchliessfaecher anzahlSchliessfaecher;

	private AnzahlLademoeglichkeiten anzahlLademoeglichkeiten;

	@Getter
	@Enumerated(EnumType.STRING)
	private Ueberwacht ueberwacht;

	@Getter
	@Enumerated(EnumType.STRING)
	private AbstellanlagenOrt abstellanlagenOrt;

	@Enumerated(EnumType.STRING)
	private Groessenklasse groessenklasse;

	@Getter
	@Enumerated(EnumType.STRING)
	private Stellplatzart stellplatzart;

	@Getter
	private Ueberdacht ueberdacht;

	private GebuehrenProTag gebuehrenProTag;

	private GebuehrenProMonat gebuehrenProMonat;

	private GebuehrenProJahr gebuehrenProJahr;

	private AbstellanlagenBeschreibung beschreibung;

	private AbstellanlagenWeitereInformation weitereInformation;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@Getter
	@NotAudited
	private DokumentListe dokumentListe;

	@Getter
	@Enumerated(EnumType.STRING)
	private AbstellanlagenStatus status;

	@Builder(toBuilder = true)
	private Abstellanlage(
		Long id,
		Long version,
		Point geometrie,
		AbstellanlagenBetreiber betreiber,
		ExterneAbstellanlagenId externeId,
		AbstellanlagenQuellSystem quellSystem,
		MobiDataQuellId mobiDataQuellId,
		Verwaltungseinheit zustaendig,
		AnzahlStellplaetze anzahlStellplaetze,
		AnzahlSchliessfaecher anzahlSchliessfaecher,
		AnzahlLademoeglichkeiten anzahlLademoeglichkeiten,
		Ueberwacht ueberwacht,
		AbstellanlagenOrt abstellanlagenOrt,
		Groessenklasse groessenklasse,
		Stellplatzart stellplatzart,
		Ueberdacht ueberdacht,
		GebuehrenProTag gebuehrenProTag,
		GebuehrenProMonat gebuehrenProMonat,
		GebuehrenProJahr gebuehrenProJahr,
		AbstellanlagenBeschreibung beschreibung,
		AbstellanlagenWeitereInformation weitereInformation,
		AbstellanlagenStatus status,
		DokumentListe dokumentListe) {
		super(id, version);
		require(geometrie, notNullValue());
		require(betreiber, notNullValue());
		require(quellSystem, notNullValue());
		if (quellSystem.equals(AbstellanlagenQuellSystem.MOBIDATABW)) {
			require(mobiDataQuellId != null, "MobiDataQuellId muss bei Quellsystem MobiData gesetzt sein.");
		} else {
			require(mobiDataQuellId == null, "MobiDataQuellId darf nur bei Quellsystem MobiData gesetzt sein.");
		}
		require(ueberwacht, notNullValue());
		require(abstellanlagenOrt, notNullValue());
		require(stellplatzart, notNullValue());
		require(ueberdacht, notNullValue());
		require(status, notNullValue());
		require(dokumentListe, notNullValue());

		require(abstellanlagenOrt == AbstellanlagenOrt.BIKE_AND_RIDE || groessenklasse == null,
			"Größenklasse darf nur bei vorhandenem B+R gesetzt sein.");

		this.geometrie = geometrie;
		this.betreiber = betreiber;
		this.externeId = externeId;

		this.quellSystem = quellSystem;
		this.mobiDataQuellId = mobiDataQuellId;
		this.zustaendig = zustaendig;
		this.anzahlStellplaetze = anzahlStellplaetze;
		this.anzahlSchliessfaecher = anzahlSchliessfaecher;
		this.anzahlLademoeglichkeiten = anzahlLademoeglichkeiten;
		this.ueberwacht = ueberwacht;
		this.abstellanlagenOrt = abstellanlagenOrt;
		this.groessenklasse = groessenklasse;
		this.stellplatzart = stellplatzart;
		this.ueberdacht = ueberdacht;
		this.gebuehrenProTag = gebuehrenProTag;
		this.gebuehrenProMonat = gebuehrenProMonat;
		this.gebuehrenProJahr = gebuehrenProJahr;
		this.beschreibung = beschreibung;
		this.weitereInformation = weitereInformation;
		this.status = status;
		this.dokumentListe = dokumentListe;
	}

	/**
	 * Wird nur zum Erstellen neuer Anlagen aus dem FE (mit QuellSystem.RadVIS) genutzt
	 */
	public Abstellanlage(
		Point geometrie,
		AbstellanlagenBetreiber betreiber,
		ExterneAbstellanlagenId externeId,
		Verwaltungseinheit zustaendig,
		AnzahlStellplaetze anzahlStellplaetze,
		AnzahlSchliessfaecher anzahlSchliessfaecher,
		AnzahlLademoeglichkeiten anzahlLademoeglichkeiten,
		Ueberwacht ueberwacht,
		AbstellanlagenOrt abstellanlagenOrt,
		Groessenklasse groessenklasse,
		Stellplatzart stellplatzart,
		Ueberdacht ueberdacht,
		GebuehrenProTag gebuehrenProTag,
		GebuehrenProMonat gebuehrenProMonat,
		GebuehrenProJahr gebuehrenProJahr,
		AbstellanlagenBeschreibung beschreibung,
		AbstellanlagenWeitereInformation weitereInformation,
		AbstellanlagenStatus status) {
		this(
			null,
			null,
			geometrie,
			betreiber,
			externeId,
			AbstellanlagenQuellSystem.RADVIS,
			null,
			zustaendig,
			anzahlStellplaetze,
			anzahlSchliessfaecher,
			anzahlLademoeglichkeiten,
			ueberwacht,
			abstellanlagenOrt,
			groessenklasse,
			stellplatzart,
			ueberdacht,
			gebuehrenProTag,
			gebuehrenProMonat,
			gebuehrenProJahr,
			beschreibung,
			weitereInformation,
			status,
			new DokumentListe());
	}

	/**
	 * Nur für Updates von Anlagen mit Quellsystem RadVIS aus dem Frontend
	 */
	public void update(
		Point geometrie,
		AbstellanlagenBetreiber betreiber,
		ExterneAbstellanlagenId externeId,
		Verwaltungseinheit zustaendig,
		AnzahlStellplaetze anzahlStellplaetze,
		AnzahlSchliessfaecher anzahlSchliessfaecher,
		AnzahlLademoeglichkeiten anzahlLademoeglichkeiten,
		Ueberwacht ueberwacht,
		AbstellanlagenOrt abstellanlagenOrt,
		Groessenklasse groessenklasse,
		Stellplatzart stellplatzart,
		Ueberdacht ueberdacht,
		GebuehrenProTag gebuehrenProTag,
		GebuehrenProMonat gebuehrenProMonat,
		GebuehrenProJahr gebuehrenProJahr,
		AbstellanlagenBeschreibung beschreibung,
		AbstellanlagenWeitereInformation weitereInformation,
		AbstellanlagenStatus status) {
		require(this.quellSystem.equals(AbstellanlagenQuellSystem.RADVIS));
		require(geometrie, notNullValue());
		require(betreiber, notNullValue());
		require(anzahlStellplaetze, notNullValue());
		require(ueberwacht, notNullValue());
		require(abstellanlagenOrt, notNullValue());
		require(stellplatzart, notNullValue());
		require(ueberdacht, notNullValue());
		require(status, notNullValue());

		require(abstellanlagenOrt == AbstellanlagenOrt.BIKE_AND_RIDE || groessenklasse == null,
			"Größenklasse darf nur bei vorhandenem B+R gesetzt sein.");

		this.geometrie = geometrie;
		this.betreiber = betreiber;
		this.externeId = externeId;

		this.zustaendig = zustaendig;
		this.anzahlStellplaetze = anzahlStellplaetze;
		this.anzahlSchliessfaecher = anzahlSchliessfaecher;
		this.anzahlLademoeglichkeiten = anzahlLademoeglichkeiten;
		this.ueberwacht = ueberwacht;
		this.abstellanlagenOrt = abstellanlagenOrt;
		this.groessenklasse = groessenklasse;
		this.stellplatzart = stellplatzart;
		this.ueberdacht = ueberdacht;
		this.gebuehrenProTag = gebuehrenProTag;
		this.gebuehrenProMonat = gebuehrenProMonat;
		this.gebuehrenProJahr = gebuehrenProJahr;
		this.beschreibung = beschreibung;
		this.weitereInformation = weitereInformation;
		this.status = status;
	}

	public void addDokument(Dokument dokument) {
		this.dokumentListe.addDokument(dokument);
	}

	public void deleteDokument(Long dokumentId) {
		this.dokumentListe.deleteDokument(dokumentId);
	}

	public Optional<Verwaltungseinheit> getZustaendig() {
		return Optional.ofNullable(zustaendig);
	}

	public Optional<ExterneAbstellanlagenId> getExterneId() {
		return Optional.ofNullable(externeId);
	}

	public Optional<MobiDataQuellId> getMobiDataQuellId() {
		return Optional.ofNullable(mobiDataQuellId);
	}

	public Optional<AnzahlStellplaetze> getAnzahlStellplaetze() {
		return Optional.ofNullable(anzahlStellplaetze);
	}

	public Optional<AnzahlSchliessfaecher> getAnzahlSchliessfaecher() {
		return Optional.ofNullable(anzahlSchliessfaecher);
	}

	public Optional<AnzahlLademoeglichkeiten> getAnzahlLademoeglichkeiten() {
		return Optional.ofNullable(anzahlLademoeglichkeiten);
	}

	public Optional<Groessenklasse> getGroessenklasse() {
		return Optional.ofNullable(groessenklasse);
	}

	public Optional<GebuehrenProTag> getGebuehrenProTag() {
		return Optional.ofNullable(gebuehrenProTag);
	}

	public Optional<GebuehrenProMonat> getGebuehrenProMonat() {
		return Optional.ofNullable(gebuehrenProMonat);
	}

	public Optional<GebuehrenProJahr> getGebuehrenProJahr() {
		return Optional.ofNullable(gebuehrenProJahr);
	}

	public Optional<AbstellanlagenBeschreibung> getBeschreibung() {
		return Optional.ofNullable(beschreibung);
	}

	public Optional<AbstellanlagenWeitereInformation> getWeitereInformation() {
		return Optional.ofNullable(weitereInformation);
	}
}
