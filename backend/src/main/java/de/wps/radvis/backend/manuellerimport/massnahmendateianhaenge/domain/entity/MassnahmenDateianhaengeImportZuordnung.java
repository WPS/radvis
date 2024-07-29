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

package de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.valueObject.MassnahmenDateianhaengeImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.valueObject.MassnahmenDateianhaengeMappingHinweis;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MassnahmenDateianhaengeImportZuordnung {

	private final String ordnerName;
	@Setter
	private Long massnahmeId;

	@Setter
	private MassnahmenDateianhaengeImportZuordnungStatus status;

	@Setter
	private Map<String, MassnahmenDateianhaengeImportDatei> dateien;

	@Setter
	private MassnahmenDateianhaengeMappingHinweis hinweis;

	public MassnahmenDateianhaengeImportZuordnung(
		String ordnerName) {
		this.ordnerName = ordnerName;
		this.dateien = new HashMap<>();
	}

	public boolean hasAnySelectedDateien() {
		return this.dateien.values().stream().anyMatch(MassnahmenDateianhaengeImportDatei::isSelected);
	}

	public Optional<MassnahmenDateianhaengeMappingHinweis> getHinweis() {
		return Optional.ofNullable(this.hinweis);
	}

}
