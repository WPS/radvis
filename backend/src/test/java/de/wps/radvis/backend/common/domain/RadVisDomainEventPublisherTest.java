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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

class RadVisDomainEventPublisherTest {

	@Mock
	ApplicationEventPublisher applicationEventPublisher;

	@BeforeEach
	void beforeEach() {
		MockitoAnnotations.openMocks(this);
		// Im Publisher ist alles statisch und Initialisierung findet im Konstruktor statt, daher dieser Aufruf.
		new RadVisDomainEventPublisher(applicationEventPublisher);
	}

	@AfterEach
	void cleanup() {
		// Wenn wir das so machen, wie im BeforeEach, dann müssen wir abschließend auch aufräumen,
		// sonst bleibt die initialisierte static für die nächsten Tests gesetzt!
		new RadVisDomainEventPublisher(null);
	}

	@Test
	void publish() {
		// Arrange
		final var domainEvent = mock(RadVisDomainEvent.class);

		// Act
		RadVisDomainEventPublisher.publish(domainEvent);

		// Assert
		verify(applicationEventPublisher).publishEvent(domainEvent);
	}

	@Test
	void publish_suppressed() {
		// Arrange
		final var domainEvent = mock(RadVisChangedDomainEvent.class);
		RadVisDomainEventPublisher.suppressEvents();

		// Act
		RadVisDomainEventPublisher.publish(domainEvent);

		// Assert
		verifyNoInteractions(applicationEventPublisher);
	}

	@Test
	void publish_not_suppressed() {
		// Arrange
		final var domainEvent = mock(RadVisDomainEvent.class);
		RadVisDomainEventPublisher.suppressEvents();

		// Act
		RadVisDomainEventPublisher.publish(domainEvent);

		// Assert
		verify(applicationEventPublisher).publishEvent(domainEvent);
	}

	@Test
	void reset() {
		// Arrange
		final var domainEvent = mock(RadVisDomainEvent.class);
		RadVisDomainEventPublisher.suppressEvents();

		// Act
		RadVisDomainEventPublisher.reset();
		RadVisDomainEventPublisher.publish(domainEvent);

		// Assert
		verify(applicationEventPublisher).publishEvent(domainEvent);
	}

}