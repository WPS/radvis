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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import lombok.Getter;

@Audited
@Entity
public class Umsetzungsstand extends VersionierteEntity {

	@Enumerated(EnumType.STRING)
	@Getter
	private UmsetzungsstandStatus umsetzungsstandStatus = UmsetzungsstandStatus.NEU_ANGELEGT;

	@Getter
	private LocalDateTime letzteAenderung;

	@ManyToOne
	@Getter
	@Audited(targetAuditMode = NOT_AUDITED)
	private Benutzer benutzerLetzteAenderung;

	@Getter
	private boolean umsetzungGemaessMassnahmenblatt;

	@Enumerated(EnumType.STRING)
	@Getter
	private GrundFuerAbweichungZumMassnahmenblatt grundFuerAbweichungZumMassnahmenblatt;

	@Enumerated(EnumType.STRING)
	@Getter
	private PruefungQualitaetsstandardsErfolgt pruefungQualitaetsstandardsErfolgt;

	@Getter
	private String beschreibungAbweichenderMassnahme;

	@Getter
	private Long kostenDerMassnahme;

	@Enumerated(EnumType.STRING)
	@Getter
	private GrundFuerNichtUmsetzungDerMassnahme grundFuerNichtUmsetzungDerMassnahme;

	@Getter
	private String anmerkung;

	public void update(
		boolean umsetzungGemaessMassnahmenblatt,
		LocalDateTime letzteAenderung,
		Benutzer benutzerLetzteAenderung,
		GrundFuerAbweichungZumMassnahmenblatt grundFuerAbweichungZumMassnahmenblatt,
		PruefungQualitaetsstandardsErfolgt pruefungQualitaetsstandardsErfolgt,
		String beschreibungAbweichenderMassnahme,
		Long kostenDerMassnahme,
		GrundFuerNichtUmsetzungDerMassnahme grundFuerNichtUmsetzungDerMassnahme,
		String anmerkung,
		Umsetzungsstatus umsetzungsstatusDerMassnahme
	) {
		require(letzteAenderung, notNullValue());
		require(benutzerLetzteAenderung, notNullValue());
		require(pruefungQualitaetsstandardsErfolgt, notNullValue());
		require(grundFuerNichtUmsetzungGesetztWennInBestimmtenZustand(this.umsetzungsstandStatus,
				grundFuerNichtUmsetzungDerMassnahme, umsetzungsstatusDerMassnahme),
			"Grund für Nicht-Umsetzung ist nicht gesetzt, obwohl die Maßnahme in Zustand Idee oder storniert ist und die Aktualisierung angefordert ist");

		this.umsetzungsstandStatus = UmsetzungsstandStatus.AKTUALISIERT;
		this.letzteAenderung = letzteAenderung;
		this.benutzerLetzteAenderung = benutzerLetzteAenderung;
		this.umsetzungGemaessMassnahmenblatt = umsetzungGemaessMassnahmenblatt;
		this.grundFuerAbweichungZumMassnahmenblatt = grundFuerAbweichungZumMassnahmenblatt;
		this.pruefungQualitaetsstandardsErfolgt = pruefungQualitaetsstandardsErfolgt;
		this.beschreibungAbweichenderMassnahme = beschreibungAbweichenderMassnahme;
		this.kostenDerMassnahme = kostenDerMassnahme;
		this.grundFuerNichtUmsetzungDerMassnahme = grundFuerNichtUmsetzungDerMassnahme;
		this.anmerkung = anmerkung;
	}

	public void updateFromImport(
		boolean umsetzungGemaessMassnahmenblatt,
		LocalDateTime letzteAenderung,
		Benutzer benutzerLetzteAenderung,
		GrundFuerAbweichungZumMassnahmenblatt grundFuerAbweichungZumMassnahmenblatt,
		PruefungQualitaetsstandardsErfolgt pruefungQualitaetsstandardsErfolgt,
		String beschreibungAbweichenderMassnahme,
		Long kostenDerMassnahme,
		GrundFuerNichtUmsetzungDerMassnahme grundFuerNichtUmsetzungDerMassnahme,
		String anmerkung
	) {
		this.umsetzungsstandStatus = UmsetzungsstandStatus.IMPORTIERT;
		this.letzteAenderung = letzteAenderung;
		this.benutzerLetzteAenderung = benutzerLetzteAenderung;
		this.umsetzungGemaessMassnahmenblatt = umsetzungGemaessMassnahmenblatt;
		this.grundFuerAbweichungZumMassnahmenblatt = grundFuerAbweichungZumMassnahmenblatt;
		this.pruefungQualitaetsstandardsErfolgt = pruefungQualitaetsstandardsErfolgt;
		this.beschreibungAbweichenderMassnahme = beschreibungAbweichenderMassnahme;
		this.kostenDerMassnahme = kostenDerMassnahme;
		this.grundFuerNichtUmsetzungDerMassnahme = grundFuerNichtUmsetzungDerMassnahme;
		this.anmerkung = anmerkung;
	}

	public void fordereAktualisierungAn() {
		this.umsetzungsstandStatus = UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT;
	}

	public static boolean isUmsetzungsstandBearbeitungGesperrt(Umsetzungsstand umsetzungsstand, Massnahme massnahme) {
		return (massnahme.getUmsetzungsstatus() == Umsetzungsstatus.STORNIERT
			|| massnahme.getUmsetzungsstatus() == Umsetzungsstatus.UMGESETZT)
			&& umsetzungsstand.umsetzungsstandStatus != UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT;
	}

	private static boolean grundFuerNichtUmsetzungGesetztWennInBestimmtenZustand(
		UmsetzungsstandStatus umsetzungsstandStatus,
		GrundFuerNichtUmsetzungDerMassnahme grundFuerNichtUmsetzungDerMassnahme,
		Umsetzungsstatus umsetzungsstatusDerMassnahme) {
		if ((umsetzungsstatusDerMassnahme == Umsetzungsstatus.IDEE
			|| umsetzungsstatusDerMassnahme == Umsetzungsstatus.STORNIERT)
			&& umsetzungsstandStatus == UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT) {
			return grundFuerNichtUmsetzungDerMassnahme != null;
		}
		return true;
	}
}
