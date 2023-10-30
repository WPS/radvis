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

package de.wps.radvis.backend.organisation.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

class VerwaltungseinheitTest {

	@Test
	void parseBezeichnung_erfolgreich() {
		assertThat(Verwaltungseinheit.parseBezeichnung("Entenhausen (Bundesland)").getFirst()).isEqualTo("Entenhausen");
		assertThat(Verwaltungseinheit.parseBezeichnung("Entenhausen (Bundesland)").getSecond()).isEqualTo(
			OrganisationsArt.BUNDESLAND);
		assertThat(Verwaltungseinheit.parseBezeichnung("Enten hausen (Landkreis)").getFirst()).isEqualTo(
			"Enten hausen");
		assertThat(Verwaltungseinheit.parseBezeichnung("Enten hausen (Landkreis)").getSecond()).isEqualTo(
			OrganisationsArt.KREIS);
		assertThat(Verwaltungseinheit.parseBezeichnung("Tour Is Muss (Tourismusverband)").getFirst()).isEqualTo(
			"Tour Is Muss");
		assertThat(Verwaltungseinheit.parseBezeichnung("Tour Is Muss (Tourismusverband)").getSecond()).isEqualTo(
			OrganisationsArt.TOURISMUSVERBAND);
		assertThat(Verwaltungseinheit.parseBezeichnung("Papenburg (Aschendorf) (Gemeinde)").getFirst()).isEqualTo(
			"Papenburg (Aschendorf)");
		assertThat(Verwaltungseinheit.parseBezeichnung("Papenburg (Aschendorf) (Gemeinde)").getSecond()).isEqualTo(
			OrganisationsArt.GEMEINDE);
		assertThat(Verwaltungseinheit.parseBezeichnung("Deutsche Bahn (Sonstiges)").getFirst()).isEqualTo(
			"Deutsche Bahn");
		assertThat(Verwaltungseinheit.parseBezeichnung("Deutsche Bahn (Sonstiges)").getSecond()).isEqualTo(
			OrganisationsArt.SONSTIGES);
	}

	@Test
	void parseBezeichnung_keineOrganisationsart() {
		assertThatThrownBy(() -> Verwaltungseinheit.parseBezeichnung("Entenhausen"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("No match found");
	}

	@Test
	void parseBezeichnung_organisationsartFalsch() {
		assertThatThrownBy(() -> Verwaltungseinheit.parseBezeichnung("Entenhausen (unsinn)"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("OrganisationsArt 'unsinn' kann nicht gelesen werden");
	}
}
