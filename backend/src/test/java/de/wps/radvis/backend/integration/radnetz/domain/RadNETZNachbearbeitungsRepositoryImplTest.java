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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;

@Tag("group5")
@ContextConfiguration(classes = { NetzConfiguration.class, IntegrationRadNetzConfiguration.class,
	OrganisationConfiguration.class, BenutzerConfiguration.class, CommonConfiguration.class,
	GeoConverterConfiguration.class, NetzfehlerConfiguration.class, KommentarConfiguration.class,
	KommentarConfiguration.class, KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class })
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
@EntityScan(basePackageClasses = { KonsistenzregelPruefungsConfiguration.class })
class RadNETZNachbearbeitungsRepositoryImplTestIT extends DBIntegrationTestIT {
	@Autowired
	private KnotenRepository knotenRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private RadNETZNachbearbeitungsRepository radNETZNachbearbeitungsRepository;

	@Test
	void testGetKnotenMitHoechstensEinerAdjazentenRadNETZKante() {
		// assert
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 2), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();

		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(4, 4), QuellSystem.DLM)
			.build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.DLM)
			.build();
		Knoten knoten6 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(6, 6), QuellSystem.DLM)
			.build();
		Knoten knoten7 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(7, 7), QuellSystem.DLM)
			.build();

		knotenRepository.saveAll(List.of(knoten1, knoten2, knoten3, knoten4, knoten5, knoten6, knoten7));

		kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten1, knoten2)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT))
				.build())
			.quelle(QuellSystem.DLM).build());
		kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten2, knoten3)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT))
				.build())
			.quelle(QuellSystem.DLM).build());

		kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten4, knoten5)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT))
				.build())
			.quelle(QuellSystem.DLM).build());

		kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten4, knoten6)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT))
				.build())
			.quelle(QuellSystem.DLM).build());

		kantenRepository.save(KanteTestDataProvider.fromKnoten(knoten4, knoten7)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_FREIZEIT))
				.build())
			.quelle(QuellSystem.DLM).build());

		// act
		Stream<Geometry> sackgassen = radNETZNachbearbeitungsRepository
			.getKnotenMitHoechstensEinerAdjazentenRadNETZKante();

		// assert
		assertThat(sackgassen).containsExactlyInAnyOrder(knoten1.getPoint(), knoten3.getPoint(), knoten5.getPoint(),
			knoten6.getPoint(),
			knoten7.getPoint());
	}
}
