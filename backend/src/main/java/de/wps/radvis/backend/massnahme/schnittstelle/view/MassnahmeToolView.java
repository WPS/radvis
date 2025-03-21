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

package de.wps.radvis.backend.massnahme.schnittstelle.view;

import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;

import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.netz.schnittstelle.view.NetzbezugView;
import lombok.Getter;

@Getter
public class MassnahmeToolView {

	private final Long id;
	private final Long version;
	private final boolean canDelete;
	private final NetzbezugView netzbezug;
	private final boolean hasUmsetzungsstand;
	private final Geometry originalGeometrie;
	private final boolean archiviert;
	private final Optional<GeometryCollection> netzbezugSnapshot;

	public MassnahmeToolView(Massnahme massnahme, boolean canMassnahmeLoeschen) {
		this.canDelete = canMassnahmeLoeschen;
		this.id = massnahme.getId();
		this.version = massnahme.getVersion();
		this.netzbezug = new NetzbezugView(massnahme.getNetzbezug());
		this.hasUmsetzungsstand = massnahme.getUmsetzungsstand().isPresent();
		this.originalGeometrie = massnahme.getOriginalRadNETZGeometrie();
		this.archiviert = massnahme.isArchiviert();
		this.netzbezugSnapshot = massnahme.getNetzbezugSnapshot();
	}
}
