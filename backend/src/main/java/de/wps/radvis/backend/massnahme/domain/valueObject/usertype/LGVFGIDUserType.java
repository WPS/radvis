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

package de.wps.radvis.backend.massnahme.domain.valueObject.usertype;

import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply=true)
public class LGVFGIDUserType implements AttributeConverter<LGVFGID, String> {

	@Override
	public String convertToDatabaseColumn(LGVFGID lgvfgid) {
		return lgvfgid == null ? null : lgvfgid.toString();
	}

	@Override
	public LGVFGID convertToEntityAttribute(String dbData) {
		return dbData == null ? null : LGVFGID.of(dbData);
	}
}