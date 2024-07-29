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

import java.util.List;

import de.wps.radvis.backend.manuellerimport.common.schnittstelle.view.AbstractImportSessionView;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportSession;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import lombok.Getter;

@Getter
public class MassnahmenDateianhaengeImportSessionView extends AbstractImportSessionView {
	private final List<Long> gebietskoerperschaften;
	private final Konzeptionsquelle konzeptionsquelle;
	private final SollStandard sollStandard;
	private final List<MassnahmenDateianhaengeImportZuordnungView> zuordnungen;

	public MassnahmenDateianhaengeImportSessionView(MassnahmenDateianhaengeImportSession session) {
		super(session);
		this.gebietskoerperschaften = session.getGebietskoerperschaftenIds();
		this.sollStandard = session.getSollStandard();
		this.konzeptionsquelle = session.getKonzeptionsquelle();
		this.zuordnungen = session.getZuordnungen().values().stream()
			.map(MassnahmenDateianhaengeImportZuordnungView::of)
			.toList();
	}
}
