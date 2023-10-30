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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidatorsTest {
	@Test
	public void isValidUrl() {
		assertThat(Validators.isValidURL("http://foo.com")).isTrue();
		assertThat(Validators.isValidURL("https://foo.com")).isTrue();
		assertThat(Validators.isValidURL("https://foo.com/coole/datei.pdf")).isTrue();
		assertThat(Validators.isValidURL("https://foo.com?cool=yes&file=datei.pdf&mail=foo@bar.com")).isTrue();
		assertThat(Validators.isValidURL("http://foo.com:8080")).isTrue();
		assertThat(Validators.isValidURL("https://f00.com")).isTrue();
		assertThat(Validators.isValidURL("https://f#00_what-ever.com")).isTrue();
		assertThat(Validators.isValidURL("https://meine-website.homepage-baukasten.de.tl")).isTrue();

		assertThat(Validators.isValidURL(null)).isFalse();
		assertThat(Validators.isValidURL("https://foo.com/ümläutö123")).isFalse();
		assertThat(Validators.isValidURL("https://foo.com/ÜMLÄUTÖ123")).isFalse();
		assertThat(Validators.isValidURL("ftp://foo.com")).isFalse();
		assertThat(Validators.isValidURL("file://pfad/zu/datei")).isFalse();
		assertThat(Validators.isValidURL("file:///pfad/zu/datei")).isFalse();
		assertThat(Validators.isValidURL("foo")).isFalse();
		assertThat(Validators.isValidURL("foo . com")).isFalse();
		assertThat(Validators.isValidURL("foo→bar.com")).isFalse();
		assertThat(Validators.isValidURL("foo-bar.commmmmmmm")).isFalse();
	}

	@Test
	public void isValidEmail() {
		assertThat(Validators.isValidEmail("foo@bar.com")).isTrue();
		assertThat(Validators.isValidEmail("f00-bar@bar-foo.de")).isTrue();
		assertThat(Validators.isValidEmail("#foo.bar_whatever@blubb.pl4tsch")).isTrue();
		assertThat(Validators.isValidEmail("foo@bar")).isTrue();

		assertThat(Validators.isValidEmail(null)).isFalse();
		assertThat(Validators.isValidEmail("")).isFalse();
		assertThat(Validators.isValidEmail("foo")).isFalse();
		assertThat(Validators.isValidEmail("foo@")).isFalse();
		assertThat(Validators.isValidEmail("@bar.de")).isFalse();
		assertThat(Validators.isValidEmail("foo[at]bar.de")).isFalse();
		assertThat(Validators.isValidEmail("foo at bar.de")).isFalse();
	}

	@Test
	public void isValidDateipfad() {
		assertThat(Validators.isValidDateipfad("/pfad/zu/datei.pdf")).isTrue();

		assertThat(Validators.isValidDateipfad("/pfad/z\"u/datei.pdf")).isFalse();
		assertThat(Validators.isValidDateipfad("/pfad/z*u/datei.pdf")).isFalse();
		assertThat(Validators.isValidDateipfad("/pfad/z?u/datei.pdf")).isFalse();
		assertThat(Validators.isValidDateipfad("/pfad/z<u/datei.pdf")).isFalse();
		assertThat(Validators.isValidDateipfad("/pfad/z>u/datei.pdf")).isFalse();
		assertThat(Validators.isValidDateipfad("/pfad/z|u/datei.pdf")).isFalse();
	}
}