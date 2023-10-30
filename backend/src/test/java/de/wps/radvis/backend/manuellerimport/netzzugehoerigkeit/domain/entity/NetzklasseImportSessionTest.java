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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AutomatischerImportSchritt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class NetzklasseImportSessionTest {

	@Test
	public void constructor() {
		// Act
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build();
		NetzklasseImportSession importSession = new NetzklasseImportSession(benutzer, organisation,
			Netzklasse.RADVORRANGROUTEN);

		// Assert
		assertThat(importSession.getBenutzer()).isEqualTo(benutzer);
		assertThat(importSession.getNetzklasse()).isEqualTo(Netzklasse.RADVORRANGROUTEN);
		assertThat(importSession.getOrganisation()).isEqualTo(organisation);
		assertThat(importSession.getAktuellerImportSchritt()).isEqualTo(
			AutomatischerImportSchritt.IMPORT_DER_DATEN);
		assertThat(importSession.hatFehler()).isFalse();
		assertThat(importSession.getKanteIds()).isEmpty();
	}

	@Test
	public void addNetzklasse_RequireViolation() {
		// Act
		final var benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		final var organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build();

		assertThatThrownBy(() -> new NetzklasseImportSession(benutzer, organisation, Netzklasse.RADNETZ_ALLTAG))
			.isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> new NetzklasseImportSession(benutzer, organisation, Netzklasse.RADNETZ_FREIZEIT))
			.isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> new NetzklasseImportSession(benutzer, organisation, Netzklasse.RADNETZ_ZIELNETZ))
			.isInstanceOf(RequireViolation.class);
	}

	@Test
	public void equals_false() {
		// Arrange
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer().build()).netzklasse(Netzklasse.RADVORRANGROUTEN)
			.build();
		NetzklasseImportSession netzklasseImportSession2 = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer().build()).netzklasse(Netzklasse.RADVORRANGROUTEN)
			.build();

		// Act
		boolean result = netzklasseImportSession.equals(netzklasseImportSession2);

		// Assert
		assertThat(result).isFalse();
	}

	@Test
	public void hashcode_true() {
		// Arrange
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer().build()).netzklasse(Netzklasse.RADVORRANGROUTEN)
			.build();

		// Act
		boolean result = netzklasseImportSession.hashCode() == netzklasseImportSession.hashCode();

		// Assert
		assertThat(result).isTrue();
	}

	@Test
	public void hashcode_false() {
		// Arrange
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer().build()).netzklasse(Netzklasse.RADVORRANGROUTEN)
			.build();
		NetzklasseImportSession netzklasseImportSession2 = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer().build()).netzklasse(Netzklasse.RADVORRANGROUTEN)
			.build();

		// Act
		boolean result = netzklasseImportSession.hashCode() == netzklasseImportSession2.hashCode();

		// Assert
		assertThat(result).isFalse();
	}

	@Test
	public void addFehler() {
		// Arrange
		NetzklasseImportSession importSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer().build()).netzklasse(Netzklasse.RADVORRANGROUTEN)
			.build();
		ImportLogEintrag fehler = ImportLogEintrag.ofError("Das ist ein Fehler");

		// Act
		importSession.addLogEintrag(fehler);

		// Assert
		assertThat(importSession.hatFehler()).isTrue();
		assertThat(importSession.getLog()).hasSize(1);
		assertThat(importSession.getLog().get(0)).isEqualTo(fehler);
	}

	@Test
	public void toggleNetzklassenzugehoerigkeit() {
		// Arrange
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer().build()).netzklasse(Netzklasse.RADVORRANGROUTEN)
			.build();

		// Act
		netzklasseImportSession.toggleNetzklassenzugehoerigkeit(1L);

		// Assert
		assertThat(netzklasseImportSession.getKanteIds()).containsExactly(1L);

		// Act
		netzklasseImportSession.toggleNetzklassenzugehoerigkeit(1L);

		// Assert
		assertThat(netzklasseImportSession.getKanteIds()).isEmpty();

		// Act
		netzklasseImportSession.toggleNetzklassenzugehoerigkeit(3L);
		netzklasseImportSession.toggleNetzklassenzugehoerigkeit(4L);

		// Assert
		assertThat(netzklasseImportSession.getKanteIds()).containsExactly(3L, 4L);
	}
}
