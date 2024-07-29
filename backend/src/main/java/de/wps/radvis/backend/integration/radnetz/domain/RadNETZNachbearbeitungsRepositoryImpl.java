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

package de.wps.radvis.backend.integration.radnetz.domain;

import java.util.Set;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class RadNETZNachbearbeitungsRepositoryImpl implements RadNETZNachbearbeitungsRepository {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Stream<Geometry> getKnotenMitHoechstensEinerAdjazentenRadNETZKante() {
		return entityManager.createQuery(
			"select knoten.point from Knoten knoten, Kante kante"
				+ "    inner join kante.kantenAttributGruppe as kag"
				+ "    inner join kag.netzklassen as netzklassen"
				+ "    where netzklassen IN :netzklassen"
				+ "    and kante.quelle = :dlmQuelle"
				+ "    and (kante.vonKnoten = knoten or kante.nachKnoten = knoten)"
				+ "    group by knoten.id having count(distinct kante.id) <= 1", Geometry.class
		).setParameter("netzklassen",
			Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ))
			.setParameter("dlmQuelle", QuellSystem.DLM)
			.getResultStream();
	}

}
