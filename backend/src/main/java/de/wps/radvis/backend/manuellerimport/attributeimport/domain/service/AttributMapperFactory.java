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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

public class AttributMapperFactory {

	private final VerwaltungseinheitService verwaltungseinheitService;
	private final NetzService netzService;

	public AttributMapperFactory(VerwaltungseinheitService verwaltungseinheitService,
		NetzService netzService) {
		require(verwaltungseinheitService, notNullValue());
		require(netzService, notNullValue());

		this.verwaltungseinheitService = verwaltungseinheitService;
		this.netzService = netzService;
	}

	public AttributeMapper createMapper(AttributeImportFormat attributeImportFormat) {
		switch (attributeImportFormat) {
		case LUBW:
			return new LUBWMapper(netzService);
		case RADVIS:
			return new RadVISMapper(verwaltungseinheitService);
		default:
			throw new RuntimeException(
				"Es ist kein Mapper für das AttributeImportFormat: " + attributeImportFormat + " implementiert.");
		}
	}
}
