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

import java.io.File;

import lombok.Getter;
import lombok.Setter;

@Getter
public class MassnahmenDateianhaengeImportDatei {

	private final File datei;

	private boolean isDuplicate;

	@Setter
	private boolean isSelected;

	@Setter
	private boolean isApplied;

	public MassnahmenDateianhaengeImportDatei(File datei) {
		this.datei = datei;
		this.isDuplicate = false;
		this.isSelected = false;
		this.isApplied = false;
	}

	/**
	 * Markiert die Datei als Duplikat eines bereits hochgeladenen Dokuments und unselected sie.
	 * Im Anschließenden Schritt kann die Datei ausgewählt werden, um das bestehende Dokument
	 * an der Maßnahme zu überschreiben.
	 */
	public void markAsDuplicate() {
		this.isDuplicate = true;
		this.isSelected = false;
	}

	public String getDateiname() {
		return this.datei.getName();
	}

}
