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

package de.wps.radvis.backend.konsistenz.pruefung;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.internal.ConfigurableRecursiveFieldByFieldComparator;
import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.konsistenz.KonsistenzregelVerletzungTestdataProvider;
import de.wps.radvis.backend.konsistenz.pruefung.domain.KonsistenzregelVerletzungsRepository;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.SackgassenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group2")
@ContextConfiguration(classes = { KonsistenzregelVerletzungsRepositoryTestIT.TestConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class, KonsistenzregelnConfiguration.class,
})
class KonsistenzregelVerletzungsRepositoryTestIT extends DBIntegrationTestIT {
	@Configuration
	public static class TestConfiguration {
		@MockBean
		public KantenRepository kantenRepository;

		@MockBean
		public SackgassenService sackgassenService;

		@MockBean
		public JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

		@MockBean
		public KonsistenzregelnConfigurationProperties konsistenzregelnConfigurationProperties;
	}

	@Autowired
	private KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository;

	@PersistenceContext
	EntityManager entityManager;

	private final static Polygon BEREICH = EnvelopeAdapter.toPolygon(new Envelope(0, 1000, 0, 1000),
		KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

	@Test
	public void saveAndLoad() {
		KonsistenzregelVerletzung konsistenzregelVerletzung = KonsistenzregelVerletzungTestdataProvider
			.defaultVerletzung()
			.build();
		assertThat(konsistenzregelVerletzung.getId()).isNull();
		KonsistenzregelVerletzung saved = konsistenzregelVerletzungsRepository.save(konsistenzregelVerletzung);

		entityManager.flush();
		entityManager.clear();

		KonsistenzregelVerletzung loaded = konsistenzregelVerletzungsRepository.findById(
			saved.getId()).get();

		assertThat(saved).isEqualTo(loaded);
		assertThat(saved.getId()).isEqualTo(loaded.getId());
		assertThat(saved.getBeschreibung()).isEqualTo(loaded.getBeschreibung());
		assertThat(saved.getDatum()).isEqualTo(loaded.getDatum());
		assertThat(saved.getEntityLink()).isEqualTo(loaded.getEntityLink());
		assertThat(saved.getIconPosition().getCoordinates()).isEqualTo(loaded.getIconPosition().getCoordinates());
		assertThat(saved.getOriginalGeometry().getCoordinates()).isEqualTo(
			loaded.getOriginalGeometry().getCoordinates());
		assertThat(saved)
			.usingComparator(new ConfigurableRecursiveFieldByFieldComparator(
				RecursiveComparisonConfiguration.builder().withIgnoredFields("id").build()))
			.isEqualTo(konsistenzregelVerletzung);
	}

	@Test
	public void findAllByTypInAndInBereich() {
		KonsistenzregelVerletzung typ1InBereich = konsistenzregelVerletzungsRepository.save(
			KonsistenzregelVerletzungTestdataProvider.defaultVerletzung()
				.typ("typ1")
				.details(
					new KonsistenzregelVerletzungsDetails(
						GeometryTestdataProvider.createPoint(new Coordinate(10, 10)),
						GeometryTestdataProvider.createPoint(new Coordinate(10, 10)),
						"beschreibung",
						"id1"))
				.build());

		KonsistenzregelVerletzung typ1NichtInBereich = konsistenzregelVerletzungsRepository.save(
			KonsistenzregelVerletzungTestdataProvider.defaultVerletzung()
				.typ("typ1")
				.details(
					new KonsistenzregelVerletzungsDetails(
						GeometryTestdataProvider.createPoint(new Coordinate(1001, 1001)),
						GeometryTestdataProvider.createPoint(new Coordinate(1001, 1001)),
						"beschreibung",
						"id2"))
				.build());

		KonsistenzregelVerletzung typ2InBereich = konsistenzregelVerletzungsRepository.save(
			KonsistenzregelVerletzungTestdataProvider.defaultVerletzung()
				.typ("typ2")
				.details(
					new KonsistenzregelVerletzungsDetails(
						GeometryTestdataProvider.createPoint(new Coordinate(10, 10)),
						GeometryTestdataProvider.createPoint(new Coordinate(10, 10)),
						"beschreibung",
						"id3"))
				.build());

		KonsistenzregelVerletzung typ2NichtInBereich = konsistenzregelVerletzungsRepository.save(
			KonsistenzregelVerletzungTestdataProvider.defaultVerletzung()
				.typ("typ2")
				.details(
					new KonsistenzregelVerletzungsDetails(
						GeometryTestdataProvider.createPoint(new Coordinate(1001, 1001)),
						GeometryTestdataProvider.createPoint(new Coordinate(1001, 1001)),
						"beschreibung",
						"id4"))
				.build());

		entityManager.flush();
		entityManager.clear();

		Stream<KonsistenzregelVerletzung> result = konsistenzregelVerletzungsRepository.findAllByTypInAndInBereich(
			Set.of("typ1", "typ2"), BEREICH);

		assertThat(result).extracting(KonsistenzregelVerletzung::getId)
			.containsExactlyInAnyOrder(typ1InBereich.getId(), typ2InBereich.getId());

	}
}
