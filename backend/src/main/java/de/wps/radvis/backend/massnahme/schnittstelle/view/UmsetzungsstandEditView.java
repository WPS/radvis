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

package de.wps.radvis.backend.massnahme.schnittstelle.view;

import java.time.LocalDateTime;
import java.util.Optional;

import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import lombok.Getter;

@Getter
public class UmsetzungsstandEditView {
	private final Long id;
	private final Long version;

	private final UmsetzungsstandStatus umsetzungsstandStatus;

	private final Umsetzungsstatus massnahmeUmsetzungsstatus;

	private final LocalDateTime letzteAenderung;

	private final boolean umsetzungGemaessMassnahmenblatt;

	private final GrundFuerAbweichungZumMassnahmenblatt grundFuerAbweichungZumMassnahmenblatt;

	private final PruefungQualitaetsstandardsErfolgt pruefungQualitaetsstandardsErfolgt;

	private final String beschreibungAbweichenderMassnahme;

	private final Long kostenDerMassnahme;

	private final Optional<GrundFuerNichtUmsetzungDerMassnahme> grundFuerNichtUmsetzungDerMassnahme;

	private final String anmerkung;
	private final boolean canEdit;

	public UmsetzungsstandEditView(Massnahme massnahme, boolean isBerechtigtZuBearbeiten) {
		this.canEdit = isBerechtigtZuBearbeiten && !massnahme.isArchiviert();
		Umsetzungsstand umsetzungsstand = massnahme.getUmsetzungsstand().orElseThrow();

		this.id = umsetzungsstand.getId();
		this.version = umsetzungsstand.getVersion();

		this.massnahmeUmsetzungsstatus = massnahme.getUmsetzungsstatus();
		this.umsetzungsstandStatus = umsetzungsstand.getUmsetzungsstandStatus();
		this.letzteAenderung = umsetzungsstand.getLetzteAenderung();
		this.umsetzungGemaessMassnahmenblatt = umsetzungsstand.isUmsetzungGemaessMassnahmenblatt();
		this.grundFuerAbweichungZumMassnahmenblatt = umsetzungsstand.getGrundFuerAbweichungZumMassnahmenblatt();
		this.pruefungQualitaetsstandardsErfolgt = umsetzungsstand.getPruefungQualitaetsstandardsErfolgt();
		this.beschreibungAbweichenderMassnahme = umsetzungsstand.getBeschreibungAbweichenderMassnahme();
		this.kostenDerMassnahme = umsetzungsstand.getKostenDerMassnahme();
		this.grundFuerNichtUmsetzungDerMassnahme = umsetzungsstand.getGrundFuerNichtUmsetzungDerMassnahme();
		this.anmerkung = umsetzungsstand.getAnmerkung();
	}
}
