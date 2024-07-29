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

import java.util.List;

import org.geojson.GeoJsonObject;

import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.netz.schnittstelle.view.NetzbezugView;
import lombok.Getter;

@Getter
public class MassnahmenImportZuordnungUeberpruefungView {

	private final int id;
	private final MassnahmenImportZuordnungStatus status;
	private final List<NetzbezugHinweis> netzbezugHinweise;
	private final MassnahmeKonzeptID massnahmeKonzeptId;
	private final NetzbezugView netzbezug;
	private final GeoJsonObject originalGeometrie;
	private final GeoJsonObject netzbezugGeometrie;
	private final boolean selected;

	public MassnahmenImportZuordnungUeberpruefungView(MassnahmenImportZuordnung zuordnung,
		GeoJsonObject originalGeometrie,
		GeoJsonObject netzbezugGeometrie) {
		this.id = zuordnung.getId();
		this.status = zuordnung.getZuordnungStatus();
		this.massnahmeKonzeptId = zuordnung.getMassnahmeKonzeptId().orElse(null);
		this.netzbezugHinweise = zuordnung.getNetzbezugHinweise();
		this.netzbezug = zuordnung.getNetzbezug().map(NetzbezugView::new).orElse(null);
		this.originalGeometrie = originalGeometrie;
		this.netzbezugGeometrie = netzbezugGeometrie;
		this.selected = zuordnung.isSelected();
	}
}
