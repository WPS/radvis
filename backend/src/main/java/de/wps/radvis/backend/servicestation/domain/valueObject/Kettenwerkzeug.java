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

package de.wps.radvis.backend.servicestation.domain.valueObject;

import com.fasterxml.jackson.annotation.JsonCreator;

import de.wps.radvis.backend.common.domain.valueObject.AbstractBooleanVO;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class Kettenwerkzeug extends AbstractBooleanVO {

	private Kettenwerkzeug(Boolean value) {
		super(value);
	}

	private Kettenwerkzeug(String value) {
		super(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Kettenwerkzeug of(Boolean value) {
		return new Kettenwerkzeug(value);
	}

	public static Kettenwerkzeug of(String value) {
		return new Kettenwerkzeug(value);
	}

	@Override
	public String toString() {
		return String.format("Kettenwerkzeug{value='%s'}", value);
	}
}