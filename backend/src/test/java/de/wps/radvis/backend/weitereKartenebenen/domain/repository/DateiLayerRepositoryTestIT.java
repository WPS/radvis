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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.DateiLayer;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.DateiLayerFormat;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverDatastoreName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverLayerName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group6")
@ContextConfiguration(classes = { DateiLayerRepositoryTestIT.TestConfiguration.class,
	CommonConfiguration.class,
	GeoConverterConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class })
class DateiLayerRepositoryTestIT extends DBIntegrationTestIT {

	@EnableJpaRepositories(basePackages = { "de.wps.radvis.backend.weitereKartenebenen",
		"de.wps.radvis.backend.benutzer",
		"de.wps.radvis.backend.organisation"
	})
	@EntityScan(basePackages = { "de.wps.radvis.backend.weitereKartenebenen",
		"de.wps.radvis.backend.benutzer",
		"de.wps.radvis.backend.organisation",
		"de.wps.radvis.backend.common" })
	public static class TestConfiguration {
	}

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private DateiLayerRepository dateiLayerRepository;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private OrganisationRepository organisationRepository;

	GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Test
	void save_and_findAllAsView() {

		Organisation organisation = organisationRepository.save(
			VerwaltungseinheitTestDataProvider.defaultOrganisation().build()
		);
		Benutzer benutzer = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer()
				.organisation(organisation)
				.build()
		);
		DateiLayer dateiLayer1 = dateiLayerRepository.save(new DateiLayer(Name.of("testName"),
			Quellangabe.of("TestQuellangabe"),
			GeoserverLayerName.of("testGeoserverLayerName"),
			GeoserverDatastoreName.withTimestamp("testGeoserverDatastoreName"),
			benutzer,
			LocalDateTime.now(),
			DateiLayerFormat.SHAPE
		));
		DateiLayer dateiLayer2 = dateiLayerRepository.save(new DateiLayer(Name.of("testName2"),
			Quellangabe.of("TestQuellangabe2"),
			GeoserverLayerName.of("testGeoserverLayerName"),
			GeoserverDatastoreName.withTimestamp("testGeoserverDatastoreName"),
			benutzer,
			LocalDateTime.now(),
			DateiLayerFormat.GEOJSON
		));

		entityManager.flush();
		entityManager.clear();

		List<DateiLayer> allAsView = StreamSupport.stream(dateiLayerRepository.findAll().spliterator(), false).collect(
			Collectors.toList());
		assertThat(allAsView).hasSize(2);

		Optional<DateiLayer> dateiLayerView1 = allAsView.stream()
			.filter(dateiLayerView -> dateiLayerView.getId().equals(dateiLayer1.getId()))
			.findFirst();
		assertThat(dateiLayerView1).isPresent();
		assertThat(dateiLayerView1.get().getBenutzer().getVorname()).isEqualTo(benutzer.getVorname());
		assertThat(dateiLayerView1.get().getBenutzer().getNachname()).isEqualTo(benutzer.getNachname());
		assertThat(dateiLayerView1.get().getDateiLayerFormat()).isEqualTo(dateiLayer1.getDateiLayerFormat());
		assertThat(dateiLayerView1.get().getName()).isEqualTo(dateiLayer1.getName());
		assertThat(dateiLayerView1.get().getGeoserverLayerName()).isEqualTo(dateiLayer1.getGeoserverLayerName());
		assertThat(dateiLayerView1.get().getGeoserverDatastoreName()).isEqualTo(
			dateiLayer1.getGeoserverDatastoreName());
		assertThat(dateiLayerView1.get().getQuellangabe()).isEqualTo(dateiLayer1.getQuellangabe());
		assertThat(dateiLayerView1.get().getErstelltAm().truncatedTo(ChronoUnit.SECONDS))
			.isEqualTo(dateiLayer1.getErstelltAm().truncatedTo(ChronoUnit.SECONDS));

		Optional<DateiLayer> dateiLayerView2 = allAsView.stream()
			.filter(dateiLayerView -> dateiLayerView.getId().equals(dateiLayer2.getId()))
			.findFirst();
		assertThat(dateiLayerView2).isPresent();
		assertThat(dateiLayerView2.get().getBenutzer().getVorname()).isEqualTo(benutzer.getVorname());
		assertThat(dateiLayerView2.get().getBenutzer().getNachname()).isEqualTo(benutzer.getNachname());
		assertThat(dateiLayerView2.get().getDateiLayerFormat()).isEqualTo(dateiLayer2.getDateiLayerFormat());
		assertThat(dateiLayerView2.get().getName()).isEqualTo(dateiLayer2.getName());
		assertThat(dateiLayerView2.get().getGeoserverLayerName()).isEqualTo(dateiLayer2.getGeoserverLayerName());
		assertThat(dateiLayerView2.get().getGeoserverDatastoreName()).isEqualTo(
			dateiLayer2.getGeoserverDatastoreName());
		assertThat(dateiLayerView2.get().getQuellangabe()).isEqualTo(dateiLayer2.getQuellangabe());
		assertThat(dateiLayerView2.get().getErstelltAm().truncatedTo(ChronoUnit.SECONDS))
			.isEqualTo(dateiLayer2.getErstelltAm().truncatedTo(ChronoUnit.SECONDS));
	}
}