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

import org.junit.jupiter.api.Test;

class CommonConfigurationPropertiesTest {

	@Test
	void testePfadIstGueltig_keineException() {
		String pfad = "F:/WPS_Files/nextCloud/Radvis-Intern/Ressourcen";
		ExtentProperty extent = new ExtentProperty(0L, 100L, 0L, 100L);
		String proxy = null;
		String version = null;
		String basisURL = "http://foo.bar";

		assertThatNoException().isThrownBy(
			() -> new CommonConfigurationProperties(pfad, 60, extent, proxy, version, basisURL));
	}

	@Test
	void testeErstelleProperties_fail_externeURLungueltig() {
		String pfad = "http://Eigen?tch#//MURKS!";
		ExtentProperty extent = new ExtentProperty(0L, 100L, 0L, 100L);
		String proxy = null;
		String version = null;
		String basisURL = "http://foo.bar";

		assertThatThrownBy(() -> {
			new CommonConfigurationProperties(pfad, 60, extent, proxy, version, basisURL);
		})
			.hasMessageContaining("externeResourcenBasisPfad muss Dateipfadstruktur haben");
	}

	@Test
	void testeErstelleProperties_extent_null() {
		String pfad = "foo/bar";
		ExtentProperty extent = null;
		String proxy = null;
		String version = null;
		String basisURL = "http://foo.bar";

		assertThatThrownBy(() -> {
			new CommonConfigurationProperties(pfad, 60, extent, proxy, version, basisURL);
		})
			.isInstanceOf(org.valid4j.errors.RequireViolation.class)
			.hasMessageContaining("expected: not null");
	}

	@Test
	void testeErstelleProperties_proxy_ungueltig() {
		String pfad = "foo/bar";
		ExtentProperty extent = new ExtentProperty(0L, 100L, 0L, 100L);
		String proxy = "irgendsonquatsch";
		String version = null;
		String basisURL = "http://foo.bar";

		assertThatThrownBy(() -> {
			new CommonConfigurationProperties(pfad, 60, extent, proxy, version, basisURL);
		});
	}

	@Test
	void testeErstelleProperties_fail_basisURL_null() {
		String pfad = "foo/bar";
		ExtentProperty extent = new ExtentProperty(0L, 100L, 0L, 100L);
		String proxy = null;
		String version = null;
		String basisURL = null;

		assertThatThrownBy(() -> {
			new CommonConfigurationProperties(pfad, 60, extent, proxy, version, basisURL);
		})
			.hasMessageContaining("basisUrl muss URL-Struktur haben");
	}

}
