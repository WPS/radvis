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

package de.wps.radvis.backend.netz.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.netz.domain.valueObject.Richtung;

class FahrtrichtungAttributGruppeTest {

	@Test
	void testeKonstruktor() {
		assertThatThrownBy(() -> {
			new FahrtrichtungAttributGruppe(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG, false);
		}).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> {
			new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, Richtung.GEGEN_RICHTUNG, false);
		}).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> {
			new FahrtrichtungAttributGruppe(null, Richtung.GEGEN_RICHTUNG, false);
		}).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> {
			new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, null, false);
		}).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> {
			new FahrtrichtungAttributGruppe(null, false);
		}).isInstanceOf(RequireViolation.class);

		new FahrtrichtungAttributGruppe(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG, true);

		new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, Richtung.BEIDE_RICHTUNGEN, false);

		new FahrtrichtungAttributGruppe(Richtung.IN_RICHTUNG, false);
		new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, false);
		new FahrtrichtungAttributGruppe(Richtung.GEGEN_RICHTUNG, false);
		new FahrtrichtungAttributGruppe(Richtung.GEGEN_RICHTUNG, true);
		new FahrtrichtungAttributGruppe(Richtung.IN_RICHTUNG, true);
		new FahrtrichtungAttributGruppe(Richtung.BEIDE_RICHTUNGEN, true);
	}

	@Test
	void testeSetter() {
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = new FahrtrichtungAttributGruppe(
			Richtung.BEIDE_RICHTUNGEN, false);

		fahrtrichtungAttributGruppe.setRichtung(Richtung.GEGEN_RICHTUNG);
		fahrtrichtungAttributGruppe.setRichtung(Richtung.IN_RICHTUNG);
		fahrtrichtungAttributGruppe.setRichtung(Richtung.BEIDE_RICHTUNGEN);

		fahrtrichtungAttributGruppe.setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.GEGEN_RICHTUNG);

		assertThatThrownBy(() -> {
			fahrtrichtungAttributGruppe.setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.IN_RICHTUNG);
		}).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> {
			fahrtrichtungAttributGruppe.setRichtung(null, Richtung.IN_RICHTUNG);
		}).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> {
			fahrtrichtungAttributGruppe.setRichtung(Richtung.GEGEN_RICHTUNG, null);
		}).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> {
			fahrtrichtungAttributGruppe.setRichtung(null);
		}).isInstanceOf(RequireViolation.class);

		fahrtrichtungAttributGruppe.changeSeitenbezug(true);

		fahrtrichtungAttributGruppe.setRichtung(Richtung.GEGEN_RICHTUNG);
		fahrtrichtungAttributGruppe.setRichtung(Richtung.IN_RICHTUNG);
		fahrtrichtungAttributGruppe.setRichtung(Richtung.BEIDE_RICHTUNGEN);

		fahrtrichtungAttributGruppe.setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.IN_RICHTUNG);
		fahrtrichtungAttributGruppe.setRichtung(Richtung.IN_RICHTUNG, Richtung.BEIDE_RICHTUNGEN);
		fahrtrichtungAttributGruppe.setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.BEIDE_RICHTUNGEN);

		fahrtrichtungAttributGruppe.changeSeitenbezug(false);
		assertThat(fahrtrichtungAttributGruppe.getFahrtrichtungLinks())
			.isEqualTo(fahrtrichtungAttributGruppe.getFahrtrichtungRechts())
			.isEqualTo(Richtung.GEGEN_RICHTUNG);
	}

}