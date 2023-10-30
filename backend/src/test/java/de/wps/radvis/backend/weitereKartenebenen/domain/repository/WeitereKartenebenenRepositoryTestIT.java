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

package de.wps.radvis.backend.weitereKartenebenen.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.weitereKartenebenen.WeitereKartenebenenConfiguration;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenConfigurationProperties;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebene;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebenenTestDataProvider;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Deckkraft;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.HexColor;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.WeitereKartenebeneTyp;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zindex;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Zoomstufe;

@Tag("group1")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	WeitereKartenebenenConfiguration.class,
	KommentarConfiguration.class,
	CommonConfiguration.class,
	MailConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	MailConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	WeitereKartenebenenConfigurationProperties.class
})
class WeitereKartenebenenRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	private WeitereKartenebenenRepository weitereKartenebenenRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Test
	void erstelleWeitereKartenebene() {
		// arrange
		WeitereKartenebene weitereKartenebene = WeitereKartenebenenTestDataProvider.defaultValue().build();

		// act
		WeitereKartenebene saved = weitereKartenebenenRepository.save(weitereKartenebene);

		// assert
		assertThat(saved).usingRecursiveComparison().ignoringFields("id").isEqualTo(weitereKartenebene);
	}

	@Test
	void updateWeitereKartenebene() {
		// arrange
		WeitereKartenebene weitereKartenebene = WeitereKartenebenenTestDataProvider.defaultValue().build();

		// act
		WeitereKartenebene saved = weitereKartenebenenRepository.save(weitereKartenebene);

		// Farbe & Deckkraft updaten
		saved.update(Name.of("Dienst A"), "localhost", WeitereKartenebeneTyp.WFS,
			Deckkraft.of(0.5), Zoomstufe.of(6), Zindex.of(2000),
			HexColor.of("#000000"), Quellangabe.of("Quellenangabe"));

		// assert
		assertThat(weitereKartenebenenRepository.findById(saved.getId())).contains(saved);
		assertThat(weitereKartenebenenRepository.findById(saved.getId()).get().getFarbe()).isEqualTo(
			HexColor.of("#000000"));
		assertThat(weitereKartenebenenRepository.findById(saved.getId()).get().getDeckkraft()).isEqualTo(
			Deckkraft.of(0.5));
		assertThat(weitereKartenebenenRepository.findById(saved.getId()).get().getZindex()).isEqualTo(
			Zindex.of(2000));
	}

	@Test
	void getAllByBenutzer() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		gebietskoerperschaftRepository.save(gebietskoerperschaft);

		Benutzer ich = BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build();
		Benutzer personB = BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build();
		benutzerRepository.save(ich);
		benutzerRepository.save(personB);

		WeitereKartenebene meinLayer1 = WeitereKartenebenenTestDataProvider.defaultValue().benutzer(ich).build();
		WeitereKartenebene meinLayer2 = WeitereKartenebenenTestDataProvider.defaultValue().benutzer(ich).build();
		WeitereKartenebene layerVonPersonB = WeitereKartenebenenTestDataProvider.defaultValue().benutzer(personB)
			.build();

		weitereKartenebenenRepository.save(meinLayer1);
		weitereKartenebenenRepository.save(meinLayer2);
		weitereKartenebenenRepository.save(layerVonPersonB);

		// act und assert
		assertThat(weitereKartenebenenRepository.findAllByBenutzerOrderById(ich)).containsExactlyInAnyOrder(meinLayer1,
			meinLayer2);
		assertThat(weitereKartenebenenRepository.findAllByBenutzerOrderById(personB)).containsExactlyInAnyOrder(
			layerVonPersonB);
	}

	@Test
	void getAllSortiert() {
		// arrange
		Benutzer user = BenutzerTestDataProvider.defaultBenutzer().organisation(
				gebietskoerperschaftRepository.save(
					VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build()))
			.build();
		benutzerRepository.save(user);

		WeitereKartenebene l1 = WeitereKartenebenenTestDataProvider.defaultValue().benutzer(user).build();
		WeitereKartenebene l2 = WeitereKartenebenenTestDataProvider.defaultValue().benutzer(user).build();
		WeitereKartenebene l3 = WeitereKartenebenenTestDataProvider.defaultValue().benutzer(user).build();

		weitereKartenebenenRepository.save(l1);
		weitereKartenebenenRepository.save(l2);
		weitereKartenebenenRepository.save(l3);

		// act
		l2.update(Name.of("Mein Layer 2 updated"), "localhost", WeitereKartenebeneTyp.WMS, Deckkraft.of(1.0),
			Zoomstufe.of(8.7), Zindex.of(1011), null,
			Quellangabe.of("Quellenangabe"));
		weitereKartenebenenRepository.save(l2);
		l1.update(Name.of("Mein Layer 1 updated"), "localhost", WeitereKartenebeneTyp.WMS, Deckkraft.of(1.0),
			Zoomstufe.of(8.7), Zindex.of(1011), null,
			Quellangabe.of("Quellenangabe"));
		weitereKartenebenenRepository.save(l1);

		// assert
		assertThat(weitereKartenebenenRepository.findAllByBenutzerOrderById(user)).containsExactly(l1, l2, l3);
	}
}
