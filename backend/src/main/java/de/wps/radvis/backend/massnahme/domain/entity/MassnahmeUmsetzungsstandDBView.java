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

import java.util.Set;

import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "massnahme_umsetzungsstand_view")
public class MassnahmeUmsetzungsstandDBView {

	@Id
	Long id;

	String bezeichnung;
	String massnahmeKonzeptId;
	String baulastOrganisationsArt;
	String gemeinde;
	String kreis;

	Integer laenge;

	@ElementCollection
	@CollectionTable(name = "massnahme_netzklassen", joinColumns = @JoinColumn(name = "massnahme_id", referencedColumnName = "id"))
	@Enumerated(EnumType.STRING)
	Set<Netzklasse> netzklassen;

	@Enumerated(EnumType.STRING)
	Umsetzungsstatus umsetzungsstatus;

	String istUmgesetzt;
	String umsetzungGemaessMassnahmenblatt;
	String grundFuerAbweichung;
	String pruefungQualitaetsstandardsErfolgt;
	String beschreibungAbweichenderMassnahme;
	String kostenDerMassnahme;
	String anmerkung;
	String benutzerKontaktdaten;
	String letzteAenderung;
}
