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

import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;

public record MassnahmenImportProtokollStatsView(
	int anzahlImportNichtMoeglich,
	int anzahlNeuAngelegt,
	int anzahlBearbeitet,
	int anzahlGeloescht,
	int anzahlNichtFuerImportSelektiert
) {

	public static MassnahmenImportProtokollStatsView of(MassnahmenImportSession session) {
		int anzahlImportNichtMoeglich = 0;
		int anzahlNeuAngelegt = 0;
		int anzahlBearbeitet = 0;
		int anzahlGeloescht = 0;
		int anzahlNichtFuerImportSelektiert = 0;

		for (MassnahmenImportZuordnung zuordnung : session.getZuordnungen()) {
			if (!zuordnung.canBeSaved()) {
				anzahlImportNichtMoeglich++;
			} else if (!zuordnung.isSelected()) {
				anzahlNichtFuerImportSelektiert++;
			} else {
				MassnahmenImportZuordnungStatus status = zuordnung.getZuordnungStatus();
				switch (status) {
				case FEHLERHAFT -> anzahlImportNichtMoeglich++;
				case NEU -> anzahlNeuAngelegt++;
				case ZUGEORDNET -> anzahlBearbeitet++;
				case GELOESCHT -> anzahlGeloescht++;
				}
			}
		}
		return new MassnahmenImportProtokollStatsView(
			anzahlImportNichtMoeglich,
			anzahlNeuAngelegt,
			anzahlBearbeitet,
			anzahlGeloescht,
			anzahlNichtFuerImportSelektiert
		);
	}
}
