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

package de.wps.radvis.backend.abfrage.fehlerprotokoll.schnittstelle.view;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import lombok.Getter;

@Getter
public class FehlerprotokollView {
	private final Long id;
	private final String fehlerprotokollKlasse;
	private final Geometry iconPosition;
	private final Geometry originalGeometry;
	private final LocalDateTime datum;
	private final String titel;
	private final String beschreibung;
	// z.B. Link zu einer Massnahme oder andere Entity
	private final String entityLink;

	public FehlerprotokollView(FehlerprotokollEintrag fehlerprotokollEintrag) {
		id = fehlerprotokollEintrag.getId();
		fehlerprotokollKlasse = fehlerprotokollEintrag.getClass().getSimpleName();
		iconPosition = fehlerprotokollEintrag.getIconPosition();
		originalGeometry = fehlerprotokollEintrag.getOriginalGeometry();
		datum = fehlerprotokollEintrag.getDatum();
		titel = fehlerprotokollEintrag.getTitel();
		beschreibung = fehlerprotokollEintrag.getBeschreibung();
		entityLink = fehlerprotokollEintrag.getEntityLink();
	}
}
