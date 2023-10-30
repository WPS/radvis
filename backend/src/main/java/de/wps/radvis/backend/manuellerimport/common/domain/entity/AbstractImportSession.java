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

package de.wps.radvis.backend.manuellerimport.common.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AutomatischerImportSchritt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag.Severity;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionStatus;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class AbstractImportSession {

	protected Benutzer benutzer;

	protected Verwaltungseinheit organisation;

	@Setter
	protected AutomatischerImportSchritt aktuellerImportSchritt;

	@Setter
	protected ImportSessionStatus status;

	protected List<ImportLogEintrag> log;

	public AbstractImportSession(Benutzer benutzer, Verwaltungseinheit organisation) {
		super();
		this.benutzer = benutzer;
		this.organisation = organisation;
		this.aktuellerImportSchritt = AutomatischerImportSchritt.IMPORT_DER_DATEN;
		this.status = ImportSessionStatus.SESSION_CREATED;
		log = new ArrayList<>();
	}

	public boolean hatFehler() {
		return log.stream().anyMatch(l -> l.getSeverity() == Severity.ERROR);
	}

	public void addLogEintrag(ImportLogEintrag fehler) {
		require(fehler, notNullValue());

		log.add(fehler);
	}

	public abstract long getAnzahlFeaturesOhneMatch();
}
