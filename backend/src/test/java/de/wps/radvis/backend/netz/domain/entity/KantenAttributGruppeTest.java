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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitChangedEvent;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;

public class KantenAttributGruppeTest {

	private KantenAttributGruppe kantenAttributGruppe;

	@BeforeEach
	public void setup() {
		kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue().id(123L).build();
	}

	@Nested
	class WithEventPublisherMock {
		MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

		@BeforeEach
		public void setup() {
			domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
		}

		@AfterEach
		public void teardown() {
			domainPublisherMock.close();
		}

		@Test
		public void TestUpdate_AddRadNetzZugehoerigkeit() {
			// Act
			kantenAttributGruppe.update(Set.of(Netzklasse.RADNETZ_ALLTAG), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute());

			// Assert
			ArgumentCaptor<RadNetzZugehoerigkeitChangedEvent> captor = ArgumentCaptor.forClass(
				RadNetzZugehoerigkeitChangedEvent.class);
			domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
			assertThat(captor.getValue().getKantenAttributGruppeId()).isEqualTo(kantenAttributGruppe.getId());
			assertThat(captor.getValue().isRadnetzZugehoerig()).isEqualTo(true);
		}

		@Test
		public void TestUpdate_RemoveRadNetzZugehoerigkeit() {
			// Arrange
			kantenAttributGruppe.getNetzklassen().add(Netzklasse.RADNETZ_FREIZEIT);

			// Act
			kantenAttributGruppe.update(Set.of(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute());

			// Assert
			ArgumentCaptor<RadNetzZugehoerigkeitChangedEvent> captor = ArgumentCaptor.forClass(
				RadNetzZugehoerigkeitChangedEvent.class);
			domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
			assertThat(captor.getValue().getKantenAttributGruppeId()).isEqualTo(kantenAttributGruppe.getId());
			assertThat(captor.getValue().isRadnetzZugehoerig()).isEqualTo(false);
		}

		@Test
		public void TestUpdate_FuegeNichtRadNETZNetzklasseHinzu() {
			// Act
			kantenAttributGruppe.update(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute());

			// Assert
			domainPublisherMock.verifyNoInteractions();
		}

		@Test
		public void TestUpdate_AendereNetzklassenOhneRadNETZZuVerändern() {
			// arrange
			kantenAttributGruppe.getNetzklassen().add(Netzklasse.RADNETZ_ZIELNETZ);

			// Act
			kantenAttributGruppe.update(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ),
				kantenAttributGruppe.getIstStandards(), kantenAttributGruppe.getKantenAttribute());

			// Assert
			domainPublisherMock.verifyNoInteractions();
		}

		@Test
		public void TestUpdate_AendereNetzklassenOhneRadNETZZuVerändern_wechseltUnterklasse() {
			// arrange
			kantenAttributGruppe.getNetzklassen().add(Netzklasse.RADNETZ_FREIZEIT);

			// Act
			kantenAttributGruppe.update(Set.of(Netzklasse.RADNETZ_ALLTAG),
				kantenAttributGruppe.getIstStandards(), kantenAttributGruppe.getKantenAttribute());

			// Assert
			domainPublisherMock.verifyNoInteractions();
		}

		@Test
		public void testeUpdate_RadnetzStandardsMitNetzklasseRadNETZZulässig() {
			for (Netzklasse radnetzNetzklasse : Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT,
				Netzklasse.RADNETZ_ZIELNETZ)) {
				// arrange
				kantenAttributGruppe.update(new HashSet<>(), new HashSet<>(), kantenAttributGruppe.kantenAttribute);
				domainPublisherMock.reset();

				kantenAttributGruppe.getNetzklassen().add(radnetzNetzklasse);

				// act + assert
				assertThatNoException().isThrownBy(
					() -> kantenAttributGruppe.update(kantenAttributGruppe.getNetzklassen(),
						Set.of(IstStandard.STARTSTANDARD_RADNETZ), kantenAttributGruppe.kantenAttribute));
				assertThatNoException().isThrownBy(
					() -> kantenAttributGruppe.update(kantenAttributGruppe.getNetzklassen(),
						Set.of(IstStandard.ZIELSTANDARD_RADNETZ), kantenAttributGruppe.kantenAttribute));

				assertThat(kantenAttributGruppe.getIstStandards()).containsExactly(IstStandard.ZIELSTANDARD_RADNETZ);
				assertThat(kantenAttributGruppe.getNetzklassen()).containsExactly(radnetzNetzklasse);
				domainPublisherMock.verifyNoInteractions();
			}
		}

		@Test
		public void testeUpdate_RadnetzStandardsOhneNetzklasseRadNETZUnzulässig() {
			// arrange
			kantenAttributGruppe.update(new HashSet<>(), new HashSet<>(), kantenAttributGruppe.kantenAttribute);

			kantenAttributGruppe.getNetzklassen().add(Netzklasse.RADVORRANGROUTEN);

			// act + assert
			assertThatThrownBy(
				() -> kantenAttributGruppe.update(kantenAttributGruppe.getNetzklassen(),
					Set.of(IstStandard.STARTSTANDARD_RADNETZ), kantenAttributGruppe.kantenAttribute))
				.isInstanceOf(RequireViolation.class);
			assertThatThrownBy(
				() -> kantenAttributGruppe.update(kantenAttributGruppe.getNetzklassen(),
					Set.of(IstStandard.ZIELSTANDARD_RADNETZ), kantenAttributGruppe.kantenAttribute))
				.isInstanceOf(RequireViolation.class);

			assertThat(kantenAttributGruppe.getIstStandards()).isEmpty();
			assertThat(kantenAttributGruppe.getNetzklassen()).containsExactlyInAnyOrder(Netzklasse.RADVORRANGROUTEN);
			domainPublisherMock.verifyNoInteractions();
		}

		@Test
		public void testeUpdateNetzklassen_EntferneRadNETZNetzklasseMitRadNETZStandardGesetzt_WirftException() {
			for (Netzklasse radnetzNetzklasse : Netzklasse.RADNETZ_NETZKLASSEN) {
				// arrange
				kantenAttributGruppe.update(new HashSet<>(Set.of(radnetzNetzklasse)),
					new HashSet<>(Set.of(IstStandard.STARTSTANDARD_RADNETZ)),
					kantenAttributGruppe.kantenAttribute);
				domainPublisherMock.reset();

				// act + assert
				assertThatThrownBy(
					() -> kantenAttributGruppe.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))))
					.isInstanceOf(RequireViolation.class);

				assertThat(kantenAttributGruppe.getIstStandards()).containsExactlyInAnyOrder(
					IstStandard.STARTSTANDARD_RADNETZ);
				assertThat(kantenAttributGruppe.getNetzklassen()).containsExactlyInAnyOrder(radnetzNetzklasse);
				domainPublisherMock.verifyNoInteractions();
			}
		}

		@Test
		public void testeUpdateNetzklassen_EntferneNichtRadNETZNetzklasseMitRadNETZStandardGesetzt_WirftKeineException() {
			for (Netzklasse radnetzNetzklasse : Netzklasse.RADNETZ_NETZKLASSEN) {
				// arrange
				kantenAttributGruppe.update(new HashSet<>(Set.of(radnetzNetzklasse, Netzklasse.KOMMUNALNETZ_ALLTAG)),
					new HashSet<>(Set.of(IstStandard.STARTSTANDARD_RADNETZ)),
					kantenAttributGruppe.kantenAttribute);
				domainPublisherMock.reset();

				// act + assert
				assertThatNoException().isThrownBy(
					() -> kantenAttributGruppe.updateNetzklassen(new HashSet<>(Set.of(radnetzNetzklasse))));

				assertThat(kantenAttributGruppe.getIstStandards()).containsExactlyInAnyOrder(
					IstStandard.STARTSTANDARD_RADNETZ);
				assertThat(kantenAttributGruppe.getNetzklassen()).containsExactlyInAnyOrder(radnetzNetzklasse);
				domainPublisherMock.verifyNoInteractions();
			}
		}

		@Test
		public void testeUpdateNetzklassen_FuegeNetzklasseHinzu() {
			for (Netzklasse radnetzNetzklasse : Netzklasse.RADNETZ_NETZKLASSEN) {
				// arrange
				kantenAttributGruppe.update(new HashSet<>(Set.of()),
					new HashSet<>(Set.of()),
					kantenAttributGruppe.kantenAttribute);
				domainPublisherMock.reset();

				// act + assert
				assertThatNoException().isThrownBy(
					() -> kantenAttributGruppe.updateNetzklassen(
						new HashSet<>(Set.of(radnetzNetzklasse, Netzklasse.KOMMUNALNETZ_ALLTAG))));

				assertThat(kantenAttributGruppe.getIstStandards()).isEmpty();
				assertThat(kantenAttributGruppe.getNetzklassen()).containsExactlyInAnyOrder(radnetzNetzklasse,
					Netzklasse.KOMMUNALNETZ_ALLTAG);
				ArgumentCaptor<RadNetzZugehoerigkeitChangedEvent> captor = ArgumentCaptor.forClass(
					RadNetzZugehoerigkeitChangedEvent.class);
				domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
				assertThat(captor.getValue().isRadnetzZugehoerig()).isTrue();
			}
		}

		@Test
		public void testeUpdateNetzklassen_enferneRadNETZNetzklasse() {
			for (Netzklasse radnetzNetzklasse : Netzklasse.RADNETZ_NETZKLASSEN) {
				// arrange
				kantenAttributGruppe.update(new HashSet<>(Set.of(radnetzNetzklasse)),
					new HashSet<>(Set.of(IstStandard.RADSCHNELLVERBINDUNG)),
					kantenAttributGruppe.kantenAttribute);
				domainPublisherMock.reset();

				// act + assert
				assertThatNoException().isThrownBy(
					() -> kantenAttributGruppe.updateNetzklassen(
						new HashSet<>(Set.of())));

				assertThat(kantenAttributGruppe.getIstStandards()).containsExactlyInAnyOrder(
					IstStandard.RADSCHNELLVERBINDUNG);
				assertThat(kantenAttributGruppe.getNetzklassen()).isEmpty();
				ArgumentCaptor<RadNetzZugehoerigkeitChangedEvent> captor = ArgumentCaptor.forClass(
					RadNetzZugehoerigkeitChangedEvent.class);
				domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
				assertThat(captor.getValue().isRadnetzZugehoerig()).isFalse();
			}
		}
	}
}
