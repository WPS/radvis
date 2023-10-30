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

package de.wps.radvis.backend.common.schnittstelle;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class RadVisTestContainer extends PostgreSQLContainer<RadVisTestContainer> {
	private static final DockerImageName image = DockerImageName.parse("postgis/postgis:13-3.1-alpine")
		.asCompatibleSubstituteFor("postgres");
	private static RadVisTestContainer container;

	private RadVisTestContainer() {
		super(image);
		withUsername("testuser");
		withPassword("testpass");
		withDatabaseName("test");
	}

	public static RadVisTestContainer getInstance() {
		if (container == null) {
			container = new RadVisTestContainer();
		}
		return container;
	}
}
