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

package de.wps.radvis.backend.abfrage.signatur.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.netz.domain.valueObject.AttributGruppe;

class AttributGruppenServiceTest {

	@Test
	void testGetAttributGruppe_einAttribut_EineAttributGruppe() throws Exception {
		assertThat(AttributGruppenService.getAttributGruppe(List.of("ortslage")))
			.isEqualTo(AttributGruppe.GESCHWINDIGKEITATTRIBUTE);
		assertThat(AttributGruppenService.getAttributGruppe(List.of("beleuchtung")))
			.isEqualTo(AttributGruppe.KANTENATTRIBUTE);
		assertThat(AttributGruppenService.getAttributGruppe(List.of("umfeld")))
			.isEqualTo(AttributGruppe.FUEHRUNGSFORMATTRIBUTE);
	}

	@Test
	void testGetAttributGruppe_mehrereGleichgruppigeAttribut_EineAttributGruppe() throws Exception {
		assertThat(AttributGruppenService.getAttributGruppe(List.of("belag_art", "bordstein")))
			.isEqualTo(AttributGruppe.FUEHRUNGSFORMATTRIBUTE);
	}

	@Test
	void testGetAttributGruppe_keinAttribut_ThrowsException() {
		assertThatThrownBy(() -> AttributGruppenService.getAttributGruppe(List.of()))
			.isInstanceOf(AttributGruppeNichtBestimmbarException.class)
			.hasMessageContaining("Mindestens ein Attribut muss zur Bestimmung der AttributGruppe gesetzt sein");
	}

	@Test
	void testGetAttributGruppe_nichtVorhandenesAttribut_ThrowsException() {
		assertThatThrownBy(() -> AttributGruppenService.getAttributGruppe(List.of("käsefüße")))
			.isInstanceOf(AttributGruppeNichtBestimmbarException.class)
			.hasMessageContaining("Attribut käsefüße konnte keiner AttributGruppe zugeordnet werden");
	}

	@Test
	void testGetAttributGruppe_mehrereVerschiedengruppigeAttribute_ThrowsException() {
		assertThatThrownBy(() -> AttributGruppenService.getAttributGruppe(List.of("belag_art", "ortslage")))
			.isInstanceOf(AttributGruppeNichtBestimmbarException.class)
			.hasMessageContaining("Alle Attribute müssen derselben AttributGruppe angehören."
				+ "Gefunden: belag_art(FUEHRUNGSFORMATTRIBUTE) und ortslage(GESCHWINDIGKEITATTRIBUTE)");
	}

	@Test
	void testGetAttributGruppe_vorhandenesUndNichtVorhandenedAttribut_ThrowsException() {
		assertThatThrownBy(() -> AttributGruppenService.getAttributGruppe(List.of("ortslage", "abc")))
			.isInstanceOf(AttributGruppeNichtBestimmbarException.class)
			.hasMessageContaining("Attribut abc konnte keiner AttributGruppe zugeordnet werden");
	}

}
