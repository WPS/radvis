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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.view;

import java.util.Set;

import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehler;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import lombok.Getter;

@Getter
public class MassnahmenImportZuordnungAttributfehlerView {

	private final int id;
	private final MassnahmenImportZuordnungStatus status;
	private final Set<MappingFehler> fehler;
	private final MassnahmeKonzeptID massnahmeKonzeptId;

	public MassnahmenImportZuordnungAttributfehlerView(MassnahmenImportZuordnung zuordnung) {
		this.id = zuordnung.getId();
		this.status = zuordnung.getZuordnungStatus();
		this.massnahmeKonzeptId = zuordnung.getMassnahmeKonzeptId().orElse(null);
		this.fehler = zuordnung.getMappingFehler();
	}

}
