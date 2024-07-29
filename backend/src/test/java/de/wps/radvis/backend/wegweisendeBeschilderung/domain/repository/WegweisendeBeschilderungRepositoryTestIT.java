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

package de.wps.radvis.backend.wegweisendeBeschilderung.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.wegweisendeBeschilderung.WegweisendeBeschilderungConfiguration;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderung;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Defizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Gemeinde;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Kreis;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Land;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenNr;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostendefizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostenzustand;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.WegweiserTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Zustandsbewertung;

@Tag("group5")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	CommonConfiguration.class,
	MailConfiguration.class,
	WegweisendeBeschilderungConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	MailConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class WegweisendeBeschilderungRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	private WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	private Verwaltungseinheit badenWuerttemberg;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		badenWuerttemberg = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND).build());
	}

	@Test
	void saveNewAndFindByPfostenNr() {
		// arrange
		WegweisendeBeschilderung beschilderung1 = new WegweisendeBeschilderung(
			PfostenNr.of("Gleicher PfostenNr"),
			GeometryTestdataProvider.createPoint(new Coordinate(11, 11)),
			WegweiserTyp.of("Beschilderung 1 WegweiserTyp"),
			PfostenTyp.of("Beschilderung 1 PfostenTyp"),
			Zustandsbewertung.of("Beschilderung 1 Zustandsbewertung"),
			Defizit.of("Beschilderung 1 Defizit"),
			Pfostenzustand.of("Beschilderung 1 Pfostenzustand"),
			Pfostendefizit.of("Beschilderung 1 Pfostendefizit"),
			Gemeinde.of("Beschilderung 1 Gemeinde"),
			Kreis.of("Beschilderung 1 Kreis"),
			Land.of("Beschilderung 1 Land"),
			badenWuerttemberg);

		WegweisendeBeschilderung beschilderung2 = new WegweisendeBeschilderung(
			PfostenNr.of("Gleicher PfostenNr"),
			GeometryTestdataProvider.createPoint(new Coordinate(12, 12)),
			WegweiserTyp.of("Beschilderung 2 WegweiserTyp"),
			PfostenTyp.of("Beschilderung 2 PfostenTyp"),
			Zustandsbewertung.of("Beschilderung 2 Zustandsbewertung"),
			Defizit.of("Beschilderung 2 Defizit"),
			Pfostenzustand.of("Beschilderung 2 Pfostenzustand"),
			Pfostendefizit.of("Beschilderung 2 Pfostendefizit"),
			Gemeinde.of("Beschilderung 2 Gemeinde"),
			Kreis.of("Beschilderung 2 Kreis"),
			Land.of("Beschilderung 2 Land"),
			badenWuerttemberg);

		WegweisendeBeschilderung beschilderung3 = new WegweisendeBeschilderung(
			PfostenNr.of("Andere PfostenNr"),
			GeometryTestdataProvider.createPoint(new Coordinate(13, 13)),
			WegweiserTyp.of("Beschilderung 3 WegweiserTyp"),
			PfostenTyp.of("Beschilderung 3 PfostenTyp"),
			Zustandsbewertung.of("Beschilderung 3 Zustandsbewertung"),
			Defizit.of("Beschilderung 3 Defizit"),
			Pfostenzustand.of("Beschilderung 3 Pfostenzustand"),
			Pfostendefizit.of("Beschilderung 3 Pfostendefizit"),
			Gemeinde.of("Beschilderung 3 Gemeinde"),
			Kreis.of("Beschilderung 3 Kreis"),
			Land.of("Beschilderung 3 Land"),
			badenWuerttemberg);

		// act
		wegweisendeBeschilderungRepository.saveAll(List.of(beschilderung1, beschilderung2, beschilderung3));

		// assert
		List<WegweisendeBeschilderung> result = wegweisendeBeschilderungRepository.findAll();
		assertThat(result.size()).isEqualTo(3);

		assertThat(result)
			.extracting(WegweisendeBeschilderung::getPfostenNr)
			.containsExactlyInAnyOrder(
				PfostenNr.of("Gleicher PfostenNr"),
				PfostenNr.of("Gleicher PfostenNr"),
				PfostenNr.of("Andere PfostenNr")
			);
	}
}
