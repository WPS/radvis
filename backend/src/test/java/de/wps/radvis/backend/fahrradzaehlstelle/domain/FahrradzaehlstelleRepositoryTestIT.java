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

package de.wps.radvis.backend.fahrradzaehlstelle.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Channel;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.FahrradzaehlDatenEintrag;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Fahrradzaehlstelle;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.BetreiberEigeneId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstand;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;

@Tag("group3")
@ContextConfiguration(classes = { FahrradzaehlstelleRepositoryTestIT.TestConfiguration.class,
	CommonConfiguration.class, GeoConverterConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class })
class FahrradzaehlstelleRepositoryTestIT extends DBIntegrationTestIT {

	@EnableJpaRepositories(basePackages = { "de.wps.radvis.backend.fahrradzaehlstelle" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.fahrradzaehlstelle", "de.wps.radvis.backend.common" })
	public static class TestConfiguration {
	}

	@Autowired
	FahrradzaehlstelleRepository fahrradzaehlstelleRepository;

	@Test
	void findeLetztesImportDatum_nichtVorhanden() {
		// act
		Optional<Zeitstempel> zeitstempel = fahrradzaehlstelleRepository.findeLetztesImportDatum();

		// assert
		assertThat(zeitstempel).isEmpty();
	}

	@Test
	void findeLetztesImportDatum_vorhanden_findeLetzenWert() {
		// arrange
		Zeitstempel aeltererZeitstempel = Zeitstempel.of("2023-03-28T01:00:00+0200");
		Zeitstempel neuesterZeitstempel = Zeitstempel.of("2023-04-28T01:00:00+0200");
		fahrradzaehlstelleRepository.save(
			Fahrradzaehlstelle.builder()
				.betreiberEigeneId(BetreiberEigeneId.of(123L))
				.geometrie(GeometryTestdataProvider.createPoint(new Coordinate(15, 15)))
				.neusterZeitstempel(neuesterZeitstempel)
				.channels(List.of(
					Channel.builder()
						.channelId(ChannelId.of(456L))
						.fahrradzaehlDaten(Map.of(
							aeltererZeitstempel,
							FahrradzaehlDatenEintrag.builder().zaehlstand(Zaehlstand.of(12L)).build(),
							neuesterZeitstempel,
							FahrradzaehlDatenEintrag.builder().zaehlstand(Zaehlstand.of(25L)).build()
						)
						).build()
				)).build()
		);

		// act
		Optional<Zeitstempel> zeitstempel = fahrradzaehlstelleRepository.findeLetztesImportDatum();

		// assert
		assertThat(zeitstempel).isPresent();
		assertThat(zeitstempel.get()).isEqualTo(neuesterZeitstempel);
	}
}