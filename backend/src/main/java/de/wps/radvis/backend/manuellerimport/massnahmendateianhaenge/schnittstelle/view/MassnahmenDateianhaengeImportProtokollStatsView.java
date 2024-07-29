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

package de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.view;

import java.util.Collection;

import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportDatei;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.valueObject.MassnahmenDateianhaengeImportZuordnungStatus;

public record MassnahmenDateianhaengeImportProtokollStatsView(
	int anzahlErfolgreichImportierterDateien,
	int anzahlNichtErfolgreichImportierterDateien,
	int anzahlFehlerhafterDateien,
	int anzahlIgnorierterDateien
) {

	public static MassnahmenDateianhaengeImportProtokollStatsView of(MassnahmenDateianhaengeImportSession session) {
		int anzahlErfolgreichImportierterDateien = 0;
		int anzahlNichtErfolgreichImportierterDateien = 0;
		int anzahlFehlerhafterDateien = 0;
		int anzahlIgnorierterDateien = 0;

		for(MassnahmenDateianhaengeImportZuordnung zuordnung: session.getZuordnungen().values()){
			MassnahmenDateianhaengeImportZuordnungStatus status = zuordnung.getStatus();
			switch (status) {
			case FEHLERHAFT ->anzahlFehlerhafterDateien += zuordnung.getDateien().size();
			case IGNORIERT -> anzahlIgnorierterDateien += zuordnung.getDateien().size();
			case ZUGEORDNET -> {
				Collection<MassnahmenDateianhaengeImportDatei> values = zuordnung.getDateien().values();
				anzahlErfolgreichImportierterDateien += (int) values.stream().filter(MassnahmenDateianhaengeImportDatei::isApplied).count();
				anzahlNichtErfolgreichImportierterDateien += (int) values.stream().filter(datei -> datei.isSelected() && !datei.isApplied()).count();
				anzahlIgnorierterDateien += (int) values.stream().filter(datei -> !datei.isSelected()).count();
			}
			}
		};
		return new MassnahmenDateianhaengeImportProtokollStatsView(
			anzahlErfolgreichImportierterDateien,
			anzahlNichtErfolgreichImportierterDateien,
			anzahlFehlerhafterDateien,
			anzahlIgnorierterDateien
		);
	}
}
