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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.valueObject.MassnahmenDateianhaengeImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.valueObject.MassnahmenDateianhaengeMappingHinweis;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import lombok.Getter;
import lombok.Setter;

public class MassnahmenDateianhaengeImportZuordnung {

	@Getter
	private final String ordnerName;
	@Setter
	private Massnahme massnahme;

	@Getter
	@Setter
	private MassnahmenDateianhaengeImportZuordnungStatus status;

	@Getter
	private final List<File> dateien;
	@Getter
	private final List<MassnahmenDateianhaengeMappingHinweis> hinweise;

	public MassnahmenDateianhaengeImportZuordnung(
		String ordnerName) {
		this.ordnerName = ordnerName;
		this.dateien = new ArrayList<>();
		this.hinweise = new ArrayList<>();
	}

	public void addDatei(File tempFile) {
		require(tempFile, notNullValue());
		this.dateien.add(tempFile);
	}

	public void addAllDateien(List<File> tempFiles) {
		this.dateien.addAll(tempFiles);
	}

	public void addHinweis(MassnahmenDateianhaengeMappingHinweis mappingHinweis) {
		require(mappingHinweis, notNullValue());
		this.hinweise.add(mappingHinweis);
	}

	public Optional<Massnahme> getMassnahme() {
		return Optional.ofNullable(massnahme);
	}
}
