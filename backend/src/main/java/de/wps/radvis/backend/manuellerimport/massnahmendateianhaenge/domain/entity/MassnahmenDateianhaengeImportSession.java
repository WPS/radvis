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

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionSchritt;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class MassnahmenDateianhaengeImportSession extends AbstractImportSession {
	public final static ImportSessionSchritt DATEI_HOCHLADEN = ImportSessionSchritt.of(1);
	public final static ImportSessionSchritt FEHLER_UEBERPRUEFEN = ImportSessionSchritt.of(2);
	public final static ImportSessionSchritt DUPLIKATE_UEBERPRUEFEN = ImportSessionSchritt.of(3);

	@Getter
	private final MultiPolygon bereich;

	@Getter
	private final String bereichName;

	@Getter
	private final List<Long> gebietskoerperschaftenIds;

	@Getter
	private final SollStandard sollStandard;

	@Getter
	private final Konzeptionsquelle konzeptionsquelle;

	@Getter
	@Setter
	private List<MassnahmenDateianhaengeImportZuordnung> zuordnungen;

	@Builder
	public MassnahmenDateianhaengeImportSession(
		@NonNull Benutzer benutzer,
		MultiPolygon bereich,
		String bereichName,
		List<Long> gebietskoerperschaftenIds,
		Konzeptionsquelle konzeptionsquelle,
		SollStandard sollStandard) {
		super(benutzer);
		this.konzeptionsquelle = konzeptionsquelle;
		this.zuordnungen = new ArrayList<>();
		this.bereich = bereich;
		this.bereichName = bereichName;
		this.gebietskoerperschaftenIds = gebietskoerperschaftenIds;
		this.schritt = DATEI_HOCHLADEN;
		this.sollStandard = sollStandard;
	}

	@Override
	public long getAnzahlFeaturesOhneMatch() {
		// Implement the method here
		return 0; // replace with actual implementation
	}

}