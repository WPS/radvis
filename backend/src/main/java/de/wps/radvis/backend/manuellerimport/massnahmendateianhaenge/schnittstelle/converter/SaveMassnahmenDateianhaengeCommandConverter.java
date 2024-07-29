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

package de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.converter;

import java.util.List;
import java.util.Map;

import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportDatei;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.command.SaveMassnahmenDateianhaengeCommand;

public class SaveMassnahmenDateianhaengeCommandConverter {

	public void applyCommandsToSession(
		MassnahmenDateianhaengeImportSession session,
		List<SaveMassnahmenDateianhaengeCommand> commands) {
		Map<String, MassnahmenDateianhaengeImportZuordnung> zuordnungen = session.getZuordnungen();

		commands.forEach(command -> {
			MassnahmenDateianhaengeImportZuordnung zuordnung = zuordnungen.get(command.massnahmeKonzeptId());

			if (zuordnung == null) {
				session.addLogEintrag(ImportLogEintrag.ofError(
					"Eine Zuordnung für die ausgewählte Massnahme '"
						+ command.massnahmeKonzeptId() + "', konnte nicht erfolgen"));
			} else {
				Map<String, MassnahmenDateianhaengeImportDatei> dateien = zuordnung.getDateien();

				command.dateien().forEach(commandDatei -> {
					MassnahmenDateianhaengeImportDatei datei = dateien.get(commandDatei.datei());
					if (datei == null) {
						session.addLogEintrag(ImportLogEintrag.ofError(
							"Eine Zuordnung für die ausgewählte Datei '"
								+ commandDatei.datei() + "', konnte nicht erfolgen"));
					} else {
						datei.setSelected(commandDatei.selected());
					}
				});
			}
		});
	}
}
