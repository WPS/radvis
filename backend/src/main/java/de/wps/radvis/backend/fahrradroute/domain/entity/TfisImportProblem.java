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

package de.wps.radvis.backend.fahrradroute.domain.entity;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.linearref.LengthIndexedLine;

import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class TfisImportProblem extends AbstractEntity implements FehlerprotokollEintrag {
	private Long id;

	private String routenName;

	@NonNull
	private Geometry originalGeometry;

	private LocalDateTime datum;

	private String titel;

	private boolean hasNetzbezug;

	@Override
	public String getBeschreibung() {
		String begruendung = "Deshalb stehen nicht alle Funktionen in RadVIS für diese Route zur Verfügung.\n"
			+ "Grund für den fehlenden Verlauf können Lücken in der Projektion der Routen aus TFIS in "
			+ "das RadVIS-Netz sein. Sie können diesen Fehler beheben, indem Sie in dem Quellsystem die Routen "
			+ "anpassen lassen oder in RadVIS das Netz erweitern.";
		if (hasNetzbezug) {
			return String.format(
				"Beim Import von '%s' aus TFIS konnte zwar ein Netzbezug ermittelt, aber kein zusammenhängender Verlauf konstruiert werden. "
					+ begruendung + "\n\n"
					+ "Weiterhin können Kreuzungen oder Schlaufen in der Route zu nicht entscheidbaren Abbildungen führen. "
					+ "In diesem Fall können die Routen nur durch Umverlegung auf andere Kanten in TFIS für RadVIS "
					+ "vollständig importierbar gemacht werden. Weitere Informationen entnehmen Sie bitte dem Handbuch!",
				routenName);
		} else {
			return String.format("Beim Import von '%s' aus TFIS konnte kein Netzbezug ermittelt werden. "
					+ begruendung,
				routenName);
		}
	}

	@Override
	public String getEntityLink() {
		return FrontendLinks.fahrradrouteDetailView(id);
	}

	@Override
	public Geometry getIconPosition() {
		// Die Originalgeometrie ist zwar ein MultiLineString und kein LineString -> Punkt auf der Mitte nicht trivial zu finden
		// Aber so sitzt der Punkt einfach mittig auf irgendeinem Segment der Route
		Coordinate mitteAufLineString = new LengthIndexedLine(originalGeometry)
			.extractPoint(originalGeometry.getLength() / 2.0);
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(mitteAufLineString);
	}
}
