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

package de.wps.radvis.backend.manuellerimport.sessionabfrage.schnittstelle.view;

import java.util.List;

import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AutomatischerImportSchritt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionStatus;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import lombok.Getter;

@Getter
public abstract class AbstractImportSessionView {
	private final ImportTyp typ;
	private final AutomatischerImportSchritt aktuellerImportSchritt;
	private final List<ImportLogEintrag> log;
	private final ImportSessionStatus status;
	private final Long organisationsID;

	private final Long anzahlFeaturesOhneMatch;

	protected AbstractImportSessionView(AbstractImportSession abstractImportSession, ImportTyp typ) {
		this.typ = typ;
		this.aktuellerImportSchritt = abstractImportSession.getAktuellerImportSchritt();
		this.log = abstractImportSession.getLog();
		this.status = abstractImportSession.getStatus();
		this.organisationsID = abstractImportSession.getOrganisation().getId();
		this.anzahlFeaturesOhneMatch = abstractImportSession.getAnzahlFeaturesOhneMatch();
	}
}