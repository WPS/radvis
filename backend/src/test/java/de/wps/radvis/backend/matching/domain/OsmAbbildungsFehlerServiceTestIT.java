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

package de.wps.radvis.backend.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.matching.domain.entity.OsmAbbildungsFehler;

@Tag("group4")
@ContextConfiguration(classes = {
	OsmAbbildungsFehlerServiceTestIT.TestConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class
})
class OsmAbbildungsFehlerServiceTestIT extends DBIntegrationTestIT {
	@EnableJpaRepositories(basePackages = { "de.wps.radvis.backend.matching" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.matching" })
	public static class TestConfiguration {
	}

	@Autowired
	private OsmAbbildungsFehlerRepository osmAbbildungsFehlerRepository;

	private OsmAbbildungsFehlerService osmAbbildungsFehlerService;

	private List<OsmAbbildungsFehler> osmAbbildungsFehler = new ArrayList<>();

	@BeforeEach
	void setUp() {
		osmAbbildungsFehlerService = new OsmAbbildungsFehlerService(osmAbbildungsFehlerRepository);
		LocalDateTime now = LocalDateTime.now();

		osmAbbildungsFehler.clear();

		// 0: unklassifiziert
		osmAbbildungsFehler.add(osmAbbildungsFehlerRepository.save(
			new OsmAbbildungsFehler(1L,
				GeometryTestdataProvider.createLineString(
					new Coordinate(1, 1),
					new Coordinate(5, 5)
				), now, false, false, false)
		));
		// 1: RadNETZ
		osmAbbildungsFehler.add(osmAbbildungsFehlerRepository.save(
			new OsmAbbildungsFehler(2L,
				GeometryTestdataProvider.createLineString(
					new Coordinate(100, 100),
					new Coordinate(105, 105)
				), now, true, false, false)
		));
		// 2: Kreisnetz
		osmAbbildungsFehler.add(osmAbbildungsFehlerRepository.save(
			new OsmAbbildungsFehler(3L,
				GeometryTestdataProvider.createLineString(
					new Coordinate(102, 102),
					new Coordinate(107, 107)
				), now, false, true, false)
		));
		// 3: Kommunalnetz
		osmAbbildungsFehler.add(osmAbbildungsFehlerRepository.save(
			new OsmAbbildungsFehler(4L,
				GeometryTestdataProvider.createLineString(
					new Coordinate(104, 104),
					new Coordinate(109, 109)
				), now, false, false, true)
		));
		// 4: Mehrere Netzklassen gleichzeitig
		osmAbbildungsFehler.add(osmAbbildungsFehlerRepository.save(
			new OsmAbbildungsFehler(5L,
				GeometryTestdataProvider.createLineString(
					new Coordinate(106, 106),
					new Coordinate(111, 111)
				), now, true, true, true)
		));
		// 5: RadNETZ in kleinerem Bereich
		osmAbbildungsFehler.add(osmAbbildungsFehlerRepository.save(
			new OsmAbbildungsFehler(6L,
				GeometryTestdataProvider.createLineString(
					new Coordinate(2, 2),
					new Coordinate(6, 6)
				), now, true, false, false)
		));
	}

	@Test
	void getAktuelleFehlerprotokolle_RadNETZ() {
		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolle(FehlerprotokollTyp.OSM_ABBILDUNG_RADNETZ));

		// assert
		assertThat(fehlerprotokolle).hasSize(3);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(1).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(1).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(1).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(1).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ");

		Optional<FehlerprotokollEintrag> actualEintrag2 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(4).getKanteId())).findFirst();
		assertThat(actualEintrag2).isPresent();
		assertThat(actualEintrag2.get().getId()).isEqualTo(osmAbbildungsFehler.get(4).getKanteId());
		assertThat(actualEintrag2.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(4).getDatum());
		assertThat(actualEintrag2.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(4).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag2.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ, Kreisnetz, Kommunalnetz");

		// Der letzte Eintrag fuer RadNETZ nicht extra nochmal genauer asserten...
		assertThat(fehlerprotokolle.stream().filter(f -> f.getId().equals(osmAbbildungsFehler.get(5).getKanteId()))
			.findFirst())
				.isPresent();
	}

	@Test
	void getAktuelleFehlerprotokolle_Kreisnetz() {
		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolle(FehlerprotokollTyp.OSM_ABBILDUNG_KREISNETZ));

		// assert
		assertThat(fehlerprotokolle).hasSize(2);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(2).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(2).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(2).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(2).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: Kreisnetz");

		Optional<FehlerprotokollEintrag> actualEintrag2 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(4).getKanteId())).findFirst();
		assertThat(actualEintrag2).isPresent();
		assertThat(actualEintrag2.get().getId()).isEqualTo(osmAbbildungsFehler.get(4).getKanteId());
		assertThat(actualEintrag2.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(4).getDatum());
		assertThat(actualEintrag2.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(4).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag2.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ, Kreisnetz, Kommunalnetz");
	}

	@Test
	void getAktuelleFehlerprotokolle_Kommunalnetz() {
		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolle(FehlerprotokollTyp.OSM_ABBILDUNG_KOMMUNALNETZ));

		// assert
		assertThat(fehlerprotokolle).hasSize(2);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(3).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(3).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(3).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(3).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: Kommunalnetz");

		Optional<FehlerprotokollEintrag> actualEintrag2 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(4).getKanteId())).findFirst();
		assertThat(actualEintrag2).isPresent();
		assertThat(actualEintrag2.get().getId()).isEqualTo(osmAbbildungsFehler.get(4).getKanteId());
		assertThat(actualEintrag2.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(4).getDatum());
		assertThat(actualEintrag2.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(4).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag2.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ, Kreisnetz, Kommunalnetz");
	}

	@Test
	void getAktuelleFehlerprotokolle_Unklassifiziert() {
		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolle(FehlerprotokollTyp.OSM_ABBILDUNG_SONSTIGE));

		// assert
		assertThat(fehlerprotokolle).hasSize(1);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(0).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(0).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(0).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(0).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: Sonstige");
	}

	@Test
	void getAktuelleFehlerprotokolleInBereich() {
		Envelope bereich = new Envelope(0, 10, 0, 10);

		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolleInBereich(
				FehlerprotokollTyp.OSM_ABBILDUNG_RADNETZ, bereich));

		// assert
		assertThat(fehlerprotokolle).hasSize(1);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(5).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(5).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(5).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(5).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ");
	}

	@Test
	void getAktuelleFehlerprotokolle_InBereich_RadNETZ() {
		Envelope bereich = new Envelope(0, 200, 0, 200);

		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolleInBereich(FehlerprotokollTyp.OSM_ABBILDUNG_RADNETZ,
				bereich));

		// assert
		assertThat(fehlerprotokolle).hasSize(3);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(1).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(1).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(1).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(1).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ");

		Optional<FehlerprotokollEintrag> actualEintrag2 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(4).getKanteId())).findFirst();
		assertThat(actualEintrag2).isPresent();
		assertThat(actualEintrag2.get().getId()).isEqualTo(osmAbbildungsFehler.get(4).getKanteId());
		assertThat(actualEintrag2.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(4).getDatum());
		assertThat(actualEintrag2.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(4).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag2.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ, Kreisnetz, Kommunalnetz");

		// Der letzte Eintrag fuer RadNETZ nicht extra nochmal genauer asserten...
		assertThat(fehlerprotokolle.stream().filter(f -> f.getId().equals(osmAbbildungsFehler.get(5).getKanteId()))
			.findFirst())
				.isPresent();
	}

	@Test
	void getAktuelleFehlerprotokolle_InBereich_Kreisnetz() {
		Envelope bereich = new Envelope(0, 200, 0, 200);

		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolleInBereich(FehlerprotokollTyp.OSM_ABBILDUNG_KREISNETZ,
				bereich));

		// assert
		assertThat(fehlerprotokolle).hasSize(2);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(2).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(2).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(2).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(2).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: Kreisnetz");

		Optional<FehlerprotokollEintrag> actualEintrag2 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(4).getKanteId())).findFirst();
		assertThat(actualEintrag2).isPresent();
		assertThat(actualEintrag2.get().getId()).isEqualTo(osmAbbildungsFehler.get(4).getKanteId());
		assertThat(actualEintrag2.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(4).getDatum());
		assertThat(actualEintrag2.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(4).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag2.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ, Kreisnetz, Kommunalnetz");
	}

	@Test
	void getAktuelleFehlerprotokolle_InBereich_Kommunalnetz() {
		Envelope bereich = new Envelope(0, 200, 0, 200);

		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolleInBereich(
				FehlerprotokollTyp.OSM_ABBILDUNG_KOMMUNALNETZ,
				bereich));

		// assert
		assertThat(fehlerprotokolle).hasSize(2);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(3).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(3).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(3).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(3).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: Kommunalnetz");

		Optional<FehlerprotokollEintrag> actualEintrag2 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(4).getKanteId())).findFirst();
		assertThat(actualEintrag2).isPresent();
		assertThat(actualEintrag2.get().getId()).isEqualTo(osmAbbildungsFehler.get(4).getKanteId());
		assertThat(actualEintrag2.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(4).getDatum());
		assertThat(actualEintrag2.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(4).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag2.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: RadNETZ, Kreisnetz, Kommunalnetz");
	}

	@Test
	void getAktuelleFehlerprotokolle_InBereich_Unklassifiziert() {
		Envelope bereich = new Envelope(0, 200, 0, 200);

		// act
		List<FehlerprotokollEintrag> fehlerprotokolle = new ArrayList<>(
			osmAbbildungsFehlerService.getAktuelleFehlerprotokolleInBereich(
				FehlerprotokollTyp.OSM_ABBILDUNG_SONSTIGE,
				bereich));

		// assert
		assertThat(fehlerprotokolle).hasSize(1);

		Optional<FehlerprotokollEintrag> actualEintrag1 = fehlerprotokolle.stream()
			.filter(f -> f.getId().equals(osmAbbildungsFehler.get(0).getKanteId())).findFirst();
		assertThat(actualEintrag1).isPresent();
		assertThat(actualEintrag1.get().getId()).isEqualTo(osmAbbildungsFehler.get(0).getKanteId());
		assertThat(actualEintrag1.get().getDatum()).isEqualTo(osmAbbildungsFehler.get(0).getDatum());
		assertThat(actualEintrag1.get().getOriginalGeometry().getCoordinates())
			.containsExactly(osmAbbildungsFehler.get(0).getOriginalGeometry().getCoordinates());
		assertThat(actualEintrag1.get().getTitel()).isEqualTo("OSM-Abbildungsfehler: Sonstige");
	}
}