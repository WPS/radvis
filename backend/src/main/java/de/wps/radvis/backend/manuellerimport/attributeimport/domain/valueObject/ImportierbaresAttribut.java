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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode
public class ImportierbaresAttribut {
	private final String attributName;
	private final String radvisName;
	private final String attributDisplayName;
	private final boolean valid;
	private final Set<String> ungueltigeWerte;

	private ImportierbaresAttribut(
		@NonNull String attributName,
		@NonNull String radvisName,
		String attributDisplayName,
		boolean valid,
		Set<String> ungueltigeWerte) {
		this.attributName = attributName;
		this.radvisName = radvisName;
		this.attributDisplayName = attributDisplayName;
		this.valid = valid;
		this.ungueltigeWerte = ungueltigeWerte;
	}

	public static ImportierbaresAttribut of(
		String attributName,
		String radvisName,
		String importGruppe,
		boolean valid,
		Set<String> ungueltigeWerte) {
		return new ImportierbaresAttribut(attributName, radvisName, importGruppe, valid, ungueltigeWerte);
	}
}
