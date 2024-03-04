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

package de.wps.radvis.backend.manuellerimport.common.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklasseImportSession;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class ImportSessionRepositoryImplTest {

	private ImportSessionRepositoryImpl importSessionRepositoryImpl;

	@BeforeEach
	void beforeEach() {
		importSessionRepositoryImpl = new ImportSessionRepositoryImpl();
	}

	@Nested
	class WithBenutzerTests {
		private Benutzer benutzer;

		@BeforeEach
		void beforeEach() {
			benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		}

		@Nested
		class WithImportSessionTests {
			private NetzklasseImportSession netzklasseImportSession;

			@BeforeEach
			void beforeEach() {
				Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.build();
				netzklasseImportSession = new NetzklasseImportSession(benutzer, organisation,
					Netzklasse.RADVORRANGROUTEN);
			}

			@Test
			public void save() {
				// Act
				importSessionRepositoryImpl.save(netzklasseImportSession);

				// Assert
				assertThat(importSessionRepositoryImpl.exists(benutzer)).isTrue();
				assertThat(importSessionRepositoryImpl.find(benutzer, NetzklasseImportSession.class)).isNotEmpty();
				assertThat(importSessionRepositoryImpl.find(benutzer, NetzklasseImportSession.class).get())
					.isEqualTo(netzklasseImportSession);
			}

			@Test
			public void delete() {
				// Arrange
				importSessionRepositoryImpl.save(netzklasseImportSession);

				// Act
				importSessionRepositoryImpl.delete(benutzer);

				// Assert
				assertThat(importSessionRepositoryImpl.exists(benutzer)).isFalse();
				assertThat(importSessionRepositoryImpl.find(benutzer, NetzklasseImportSession.class)).isEmpty();
			}

			@Test
			public void clear() {
				// Arrange
				importSessionRepositoryImpl.save(netzklasseImportSession);

				// Act
				importSessionRepositoryImpl.clear();

				// Assert
				assertThat(importSessionRepositoryImpl.exists(benutzer)).isFalse();
				assertThat(importSessionRepositoryImpl.find(benutzer, NetzklasseImportSession.class)).isEmpty();
			}

			@Test
			public void findWithAbstractSuperclass() {
				// Arrange
				importSessionRepositoryImpl.save(netzklasseImportSession);

				// Act
				Optional<AbstractImportSession> result = importSessionRepositoryImpl.find(benutzer,
					AbstractImportSession.class);

				// Assert
				assertThat(result).isNotEmpty();
				assertThat(result.get()).isEqualTo(netzklasseImportSession);
			}
		}

	}
}
