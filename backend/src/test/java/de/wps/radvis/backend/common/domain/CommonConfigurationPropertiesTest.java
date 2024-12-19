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

package de.wps.radvis.backend.common.domain;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;

class CommonConfigurationPropertiesTest {

	private String pfad;
	private Integer anzahlTageImportprotokolleVorhalten;
	private ExtentProperty extent;
	private String proxy;
	private String version;
	private String basisURL;
	private String basisnetzImportSource;

	@BeforeEach
	void setup() {
		pfad = "foo/bar";
		anzahlTageImportprotokolleVorhalten = 60;
		extent = new ExtentProperty(0L, 100L, 0L, 100L);
		proxy = null;
		version = null;
		basisURL = "http://foo.bar";
		basisnetzImportSource = "DLM";
	}

	@Test
	void testePfadIstGueltig_keineException() {
		pfad = "F:/WPS_Files/nextCloud/Radvis-Intern/Ressourcen";

		assertThatNoException().isThrownBy(() -> new CommonConfigurationProperties(
			pfad,
			anzahlTageImportprotokolleVorhalten,
			extent,
			proxy,
			version,
			basisURL,
			basisnetzImportSource, "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources", 1.0, 1.0));
	}

	@Test
	void testeErstelleProperties_fail_externeUrlUngueltig() {
		String pfad = "http://Eigen?tch#//MURKS!";

		assertThatThrownBy(() -> new CommonConfigurationProperties(
			pfad,
			anzahlTageImportprotokolleVorhalten,
			extent,
			proxy,
			version,
			basisURL,
			basisnetzImportSource, "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources", 1.0, 1.0))
				.hasMessageContaining("externeResourcenBasisPfad muss Dateipfadstruktur haben");
	}

	@Test
	void testeErstelleProperties_anzahlTageImportprotokolleVorhalten_null() {
		Integer anzahlTageImportprotokolleVorhalten = null;

		assertThatThrownBy(() -> new CommonConfigurationProperties(
			pfad,
			anzahlTageImportprotokolleVorhalten,
			extent,
			proxy,
			version,
			basisURL,
			basisnetzImportSource, "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources", 1.0, 1.0))
				.isInstanceOf(org.valid4j.errors.RequireViolation.class)
				.hasMessageContaining("expected: not null");
	}

	@Test
	void testeErstelleProperties_extent_null() {
		ExtentProperty extent = null;

		assertThatThrownBy(() -> new CommonConfigurationProperties(
			pfad,
			anzahlTageImportprotokolleVorhalten,
			extent,
			proxy,
			version,
			basisURL,
			basisnetzImportSource, "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources", 1.0, 1.0))
				.isInstanceOf(org.valid4j.errors.RequireViolation.class)
				.hasMessageContaining("expected: not null");
	}

	@Test
	void testeErstelleProperties_proxy_ungueltig() {
		String proxy = "irgendsonquatsch";

		assertThatThrownBy(() -> new CommonConfigurationProperties(
			pfad,
			anzahlTageImportprotokolleVorhalten,
			extent,
			proxy,
			version,
			basisURL,
			basisnetzImportSource, "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources", 1.0, 1.0));
	}

	@Test
	void testeErstelleProperties_fail_basisURL_null() {
		String basisURL = null;

		assertThatThrownBy(() -> new CommonConfigurationProperties(
			pfad,
			anzahlTageImportprotokolleVorhalten,
			extent,
			proxy,
			version,
			basisURL,
			basisnetzImportSource, "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources", 1.0, 1.0))
				.hasMessageContaining("basisUrl muss URL-Struktur haben");
	}

	@Test
	void testeBasisnetzQuelleUngueltig() {
		String basisnetzSource = "Without any remorse, I assign an invalid source.";

		assertThatThrownBy(() -> new CommonConfigurationProperties(
			pfad,
			anzahlTageImportprotokolleVorhalten,
			extent,
			proxy,
			version,
			basisURL,
			basisnetzSource, "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources", 1.0, 1.0))
				.hasMessage("basisnetzImportSource muss einen der folgenden Wert enthalten: DLM, OSM");
	}
}
