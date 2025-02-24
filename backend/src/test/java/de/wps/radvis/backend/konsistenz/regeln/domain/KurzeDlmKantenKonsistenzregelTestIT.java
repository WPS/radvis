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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;

@Tag("group7")
@EnableConfigurationProperties(value = {
	OrganisationConfigurationProperties.class
})
class KurzeDlmKantenKonsistenzregelTestIT extends AbstractKonsistenzregelTestIT {

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	KnotenRepository knotenRepository;

	KurzeDlmKantenKonsistenzregel kurzeDlmKantenKonsistenzregel;

	@BeforeEach
	void setUp() {
		kurzeDlmKantenKonsistenzregel = new KurzeDlmKantenKonsistenzregel(kantenRepository);
	}

	@Test
	void testPruefe_happypath_alliswell() {
		Knoten knoten1 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(500, 500), QuellSystem.DLM).build());
		Knoten knoten2 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100.80, 100), QuellSystem.DLM).build());
		Knoten knoten3 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).build());
		Knoten knoten4 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100.80), QuellSystem.DLM).build());

		Kante dlmKanteOK = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM).build());
		Kante dlmKanteZuKurz = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.DLM).build());
		Kante kanteRadvisZuKurz = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten3, knoten4, QuellSystem.RadVis).build());

		List<KonsistenzregelVerletzungsDetails> verletzungsDetails = kurzeDlmKantenKonsistenzregel.pruefen();

		assertThat(verletzungsDetails)
			.extracting(KonsistenzregelVerletzungsDetails::getOriginalGeometry)
			.extracting(Optional::get)
			.containsExactly(dlmKanteZuKurz.getGeometry());
	}
}