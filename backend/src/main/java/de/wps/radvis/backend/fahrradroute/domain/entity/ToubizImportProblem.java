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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public class ToubizImportProblem extends AbstractEntity implements FehlerprotokollEintrag {

	@Getter
	private Long id;

	private String routenName;

	@NonNull
	private Geometry iconPosition;

	@Getter
	@NonNull
	private Geometry originalGeometry;

	@Getter
	private LocalDateTime datum;

	private boolean netzbezugVorhanden;

	@Override
	public String getTitel() {
		return "Toubiz-Import Fehler";
	}

	@Override
	public String getBeschreibung() {
		if (!netzbezugVorhanden) {
			return String.format(
				"Beim Import der Fahrradroute '%s' aus Toubiz konnte kein Netzbezug hergestellt werden. "
					+ "Deshalb stehen nicht alle Funktionen in RadVIS für diese Route zur Verfügung. \n"
					+ "Grund für den fehlenden Verlauf können Lücken in der Projektion der Routen aus Toubiz in "
					+ "das RadVIS-Netz sein. Sie können diesen Fehler beheben, indem Sie in dem Quellsystem die Routen "
					+ "anpassen lassen oder in RadVIS das Netz erweitern.",
				routenName);
		} else {
			// In diesem Fall gibt es Segmente, an denen der ermittelte Netzbezug deutlich von der Originalgeometrie
			// abweicht
			return String.format(
				"Beim Import der Fahrradroute '%s' aus Toubiz konnte auf mindestens einem Abschnitt kein genauer Bezug "
					+ "zum RadVIS-Netz hergestellt werden.\n"
					+ "Sie können diesen Fehler beheben, indem Sie in dem Quellsystem die Routen anpassen lassen "
					+ "oder in RadVIS das Netz erweitern.",
				routenName);
		}
	}

	@Override
	public MultiPoint getIconPosition() {
		return new MultiPoint(new Point[] { (Point) this.iconPosition }, this.iconPosition.getFactory());
	}

	@Override
	public String getEntityLink() {
		return FrontendLinks.fahrradrouteDetailView(id);
	}

}
