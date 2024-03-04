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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;

import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;

@Tag("group7")
@EnableConfigurationProperties(value = {
	OrganisationConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
class FahrtrichtungKonsistenzregelTestIT extends AbstractKonsistenzregelTestIT {

	@Autowired
	private KantenRepository kantenRepository;

	private FahrtrichtungKonsistenzregel fahrtrichtungKonsistenzregel;

	@BeforeEach
	void setUp() {
		fahrtrichtungKonsistenzregel = new FahrtrichtungKonsistenzregel(kantenRepository);
	}

	@Test
	void testPruefe_laufenZusammen() {
		Knoten kn1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten kn2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();
		Knoten kn3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).build();

		Kante kante1 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn1, kn2, Richtung.IN_RICHTUNG).build();
		Kante kante2 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn3, kn2, Richtung.IN_RICHTUNG).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		List<KonsistenzregelVerletzungsDetails> result = fahrtrichtungKonsistenzregel.pruefen();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOriginalGeometry()).isEmpty();
		assertThat(result.get(0).getPosition().getCoordinates()).isEqualTo(kn2.getPoint().getCoordinates());
	}

	@Test
	void testPruefe_laufenZusammen_richtungenUnterschiedlich() {
		Knoten kn1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten kn2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();
		Knoten kn3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).build();

		Kante kante1 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn1, kn2, Richtung.IN_RICHTUNG).build();
		Kante kante2 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn2, kn3, Richtung.GEGEN_RICHTUNG).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		List<KonsistenzregelVerletzungsDetails> result = fahrtrichtungKonsistenzregel.pruefen();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOriginalGeometry()).isEmpty();
		assertThat(result.get(0).getPosition().getCoordinates()).isEqualTo(kn2.getPoint().getCoordinates());
	}

	@Test
	void testPruefe_laufenAuseinander() {
		Knoten kn1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten kn2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();
		Knoten kn3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).build();

		Kante kante1 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn2, kn1, Richtung.IN_RICHTUNG).build();
		Kante kante2 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn2, kn3, Richtung.IN_RICHTUNG).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		List<KonsistenzregelVerletzungsDetails> result = fahrtrichtungKonsistenzregel.pruefen();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOriginalGeometry()).isEmpty();
		assertThat(result.get(0).getPosition().getCoordinates()).isEqualTo(kn2.getPoint().getCoordinates());
	}

	@Test
	void testPruefe_laufenAuseinander_RichtungenUnterschiedlich() {
		Knoten kn1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten kn2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();
		Knoten kn3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).build();

		Kante kante1 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn2, kn1, Richtung.IN_RICHTUNG).build();
		Kante kante2 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn3, kn2, Richtung.GEGEN_RICHTUNG).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		List<KonsistenzregelVerletzungsDetails> result = fahrtrichtungKonsistenzregel.pruefen();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOriginalGeometry()).isEmpty();
		assertThat(result.get(0).getPosition().getCoordinates()).isEqualTo(kn2.getPoint().getCoordinates());
	}

	@Test
	void testPruefe_dreiKanten_laufenAuseinander_RichtungenUnterschiedlich() {
		Knoten kn1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten kn2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();
		Knoten kn3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).build();

		Kante kante1 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn2, kn1, Richtung.IN_RICHTUNG).build();
		Kante kante2 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn3, kn2, Richtung.GEGEN_RICHTUNG).build();
		Kante kante3 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn1, kn2, Richtung.BEIDE_RICHTUNGEN).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);
		kantenRepository.save(kante3);

		List<KonsistenzregelVerletzungsDetails> result = fahrtrichtungKonsistenzregel.pruefen();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOriginalGeometry()).isEmpty();
		assertThat(result.get(0).getPosition().getCoordinates()).isEqualTo(kn2.getPoint().getCoordinates());
	}

	@Test
	void testPruefe_passenZusammen_FindetNichts() {
		Knoten kn1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten kn2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();
		Knoten kn3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).build();

		Kante kante1 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn1, kn2, Richtung.IN_RICHTUNG).build();
		Kante kante2 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn2, kn3, Richtung.IN_RICHTUNG).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		List<KonsistenzregelVerletzungsDetails> result = fahrtrichtungKonsistenzregel.pruefen();

		assertThat(result).hasSize(0);
	}

	@Test
	public void pruefen_beidseitig_findetNichts() {
		Knoten kn1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten kn2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();
		Knoten kn3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).build();

		Kante kante1 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn1, kn2, Richtung.IN_RICHTUNG).build();
		Kante kante2 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn2, kn3, Richtung.BEIDE_RICHTUNGEN).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		List<KonsistenzregelVerletzungsDetails> result = fahrtrichtungKonsistenzregel.pruefen();

		assertThat(result).hasSize(0);
	}

	@Test
	public void pruefen_unbekannt_findetNichts() {
		Knoten kn1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.build();
		Knoten kn2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();
		Knoten kn3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 10), QuellSystem.DLM).build();

		Kante kante1 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn1, kn2, Richtung.IN_RICHTUNG).build();
		Kante kante2 = KanteTestDataProvider.withFahrtrichtungFromKnoten(kn2, kn3, Richtung.UNBEKANNT).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		List<KonsistenzregelVerletzungsDetails> result = fahrtrichtungKonsistenzregel.pruefen();

		assertThat(result).hasSize(0);
	}
}