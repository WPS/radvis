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

package de.wps.radvis.backend.konsistenz.regeln.domain;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.query.NativeQuery;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderung;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeschilderungRadNETZKonsistenzregel implements Konsistenzregel {

	private final EntityManager entityManager;

	private final Integer beschilderungMaxEntfernungVonRoute;

	public BeschilderungRadNETZKonsistenzregel(EntityManager entityManager,
		Integer beschilderungMaxEntfernungVonRoute) {
		this.entityManager = entityManager;
		this.beschilderungMaxEntfernungVonRoute = beschilderungMaxEntfernungVonRoute;
	}

	@Override
	public List<KonsistenzregelVerletzungsDetails> pruefen() {
		String sqlString =
			"SELECT st_collect(k.geometry) AS geom FROM kante k"
				+ "    WHERE kanten_attributgruppe_id IN ("
				+ "        SELECT kagn.kanten_attribut_gruppe_id FROM kanten_attribut_gruppe_netzklassen kagn"
				+ "        WHERE kagn.netzklasse = 'RADNETZ_ALLTAG' OR kagn.netzklasse = 'RADNETZ_FREIZEIT'"
				+ "    )"
				+ "    AND (quelle='DLM' OR quelle='RadVis');";

		Geometry collectedRadNetzKantenGeom = (Geometry) entityManager.createNativeQuery(sqlString)
			.unwrap(NativeQuery.class)
			.addScalar("geom", Geometry.class)
			.getSingleResult();

		log.info("Maximal erlaubte entfernung zwischen Beschilderung & Route: {}m", beschilderungMaxEntfernungVonRoute);
		PreparedGeometry radnetzMitBuffer = PreparedGeometryFactory.prepare(
			collectedRadNetzKantenGeom.buffer(beschilderungMaxEntfernungVonRoute)
		);

		return entityManager.createQuery("SELECT wb FROM WegweisendeBeschilderung wb", WegweisendeBeschilderung.class)
			.getResultStream()
			.filter(wegweisendeBeschilderung -> !radnetzMitBuffer.intersects(wegweisendeBeschilderung.getGeometrie()))
			.map(wegweisendeBeschilderung ->
				new KonsistenzregelVerletzungsDetails(
					(Point) wegweisendeBeschilderung.getGeometrie(),
					"Die Beschilderung liegt weiter als " + beschilderungMaxEntfernungVonRoute
						+ "m vom RadNETZ entfernt",
					wegweisendeBeschilderung.getId().toString()))
			.collect(Collectors.toList());
	}

	@Override
	public String getVerletzungsTyp() {
		return "BESCHILDERUNG_RADNETZ";
	}

	@Override
	public String getTitel() {
		return "Beschilderung abseits von RadNETZ";
	}

	@Override
	public RegelGruppe getGruppe() {
		return RegelGruppe.WEGWEISENDE_BESCHILDERUNG;
	}
}
