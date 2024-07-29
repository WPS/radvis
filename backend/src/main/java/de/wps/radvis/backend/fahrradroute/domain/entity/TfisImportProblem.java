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
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;

import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class TfisImportProblem extends AbstractEntity implements FehlerprotokollEintrag {
	static final String LUECKEN_HINWEISTEXT = "Alle Lücken im Verlauf werden als Fehler angezeigt."
		+ " Diese können allerdings auch korrekt sein, wenn die Route Kehrtwenden beinhaltet sowie an Start- und End-Punkt.";

	private Long id;

	private String routenName;

	@NonNull
	private Geometry originalGeometry;

	private LocalDateTime datum;

	private String titel;

	private boolean hasNetzbezug;

	private List<Point> sackgassenPunkte;

	public TfisImportProblem(Long id, String routenName, Geometry originalGeometry, LocalDateTime datum,
		String titel, boolean hasNetzbezug) {
		this.id = id;
		this.routenName = routenName;
		this.originalGeometry = originalGeometry;
		this.datum = datum;
		this.titel = titel;
		this.hasNetzbezug = hasNetzbezug;
		this.sackgassenPunkte = new ArrayList<>();
	}

	@Override
	public String getBeschreibung() {
		StringBuilder stringBuilder = new StringBuilder();
		String begruendung = "Deshalb stehen nicht alle Funktionen in RadVIS für diese Route zur Verfügung.\n"
			+ "Grund für den fehlenden Verlauf können Lücken in der Projektion der Routen aus TFIS in "
			+ "das RadVIS-Netz sein. Sie können diesen Fehler beheben, indem Sie in dem Quellsystem die Routen "
			+ "anpassen lassen oder in RadVIS das Netz erweitern.";
		if (hasNetzbezug) {
			stringBuilder.append(String.format(
				"Beim Import von '%s' aus TFIS konnte zwar ein Netzbezug ermittelt, aber kein zusammenhängender Verlauf konstruiert werden. ",
				routenName));
			stringBuilder.append(begruendung);
			stringBuilder.append("\n\n");
			stringBuilder.append(
				"Weiterhin können Kreuzungen oder Schlaufen in der Route zu nicht entscheidbaren Abbildungen führen. "
					+ "In diesem Fall können die Routen nur durch Umverlegung auf andere Kanten in TFIS für RadVIS "
					+ "vollständig importierbar gemacht werden. Weitere Informationen entnehmen Sie bitte dem Handbuch!");

		} else {
			stringBuilder.append(
				String.format("Beim Import von '%s' aus TFIS konnte kein Netzbezug ermittelt werden. ", routenName));
			stringBuilder.append(begruendung);
		}

		if (!sackgassenPunkte.isEmpty()) {
			stringBuilder.append("\n\n");
			stringBuilder.append(LUECKEN_HINWEISTEXT);
		}

		return stringBuilder.toString();
	}

	@Override
	public String getEntityLink() {
		return FrontendLinks.fahrradrouteDetailView(id);
	}

	@Override
	public MultiPoint getIconPosition() {
		GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
		if (this.sackgassenPunkte.isEmpty()) {
			// Die Originalgeometrie ist zwar ein MultiLineString und kein LineString -> Punkt auf der Mitte nicht
			// trivial zu finden. Aber so sitzt der Punkt einfach mittig auf irgendeinem Segment der Route
			Coordinate mitteAufLineString = new LengthIndexedLine(originalGeometry)
				.extractPoint(originalGeometry.getLength() / 2.0);
			return new MultiPoint(new Point[] { geometryFactory.createPoint(mitteAufLineString) }, geometryFactory);
		}

		return new MultiPoint(sackgassenPunkte.toArray(new Point[0]),
			geometryFactory);
	}

	public void addSackgassen(List<Point> sackgassenPunkte) {
		this.sackgassenPunkte.addAll(sackgassenPunkte);
	}
}
