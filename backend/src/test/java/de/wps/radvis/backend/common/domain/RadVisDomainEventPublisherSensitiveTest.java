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

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

public interface RadVisDomainEventPublisherSensitiveTest {

	MockedStatic<RadVisDomainEventPublisher> getDomainPublisherMock();

	void setDomainPublisherMock(MockedStatic<RadVisDomainEventPublisher> domainPublisherMock);

	@BeforeEach
	default void setupDomainPublisherMock() {
		MockitoAnnotations.openMocks(this);

		setDomainPublisherMock(mockStatic(RadVisDomainEventPublisher.class));
	}

	@AfterEach
	default void cleanUpDomainPublisherMock() {
		getDomainPublisherMock().close();
	}

}
