/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.integration.dlm.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AttributlueckenSchliessenProblem extends AbstractEntity implements FehlerprotokollEintrag {

	@NotNull
	private LocalDateTime datum;

	private Point lueckeStartKnotenPoint;

	private Long lueckeStartKnotenId;

	public AttributlueckenSchliessenProblem(LocalDateTime datum,
		Knoten lueckeStartKnoten) {
		require(datum, notNullValue());
		require(lueckeStartKnoten, notNullValue());

		this.datum = datum;
		this.lueckeStartKnotenId = lueckeStartKnoten.getId();
		this.lueckeStartKnotenPoint = lueckeStartKnoten.getPoint();
	}

	@Override
	public MultiPoint getIconPosition() {
		return new MultiPoint(new Point[] { lueckeStartKnotenPoint }, lueckeStartKnotenPoint.getFactory());
	}

	@Override
	public Geometry getOriginalGeometry() {
		return null;
	}

	@Override
	public String getTitel() {
		return "Attributlücke";
	}

	@Override
	public String getBeschreibung() {
		return MessageFormat.format(
			"An diesem Knoten ({0,number,#}) sind Kanten verbunden, von denen einige Kanten spezifische Attribute angegeben haben, andere gar keine. "
				+ "Dies kann auf eine Inkonstistenz in den Daten hinweisen. "
				+ "Bitte prüfen Sie die Attribute der anliegenden Kanten auf ihre Richtigkeit, beachten Sie dabei auch linear referenzierte Attributgruppen.\n"
				+ "\n"
				+ "Um Lücken zu schließen, können Sie auch die Kopierfunktion für Allgemeine Kantenattribute im Editor nutzen.",
			lueckeStartKnotenId);
	}

	@Override
	public String getEntityLink() {
		return FrontendLinks.knotenDetailView(lueckeStartKnotenId);
	}
}
