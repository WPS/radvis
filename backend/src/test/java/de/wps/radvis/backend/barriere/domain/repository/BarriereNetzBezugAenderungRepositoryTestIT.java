/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.barriere.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezugAenderung;
import de.wps.radvis.backend.barriere.domain.entity.provider.BarriereTestDataProvider;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group6")
@ContextConfiguration(classes = { CommonConfiguration.class, GeoConverterConfiguration.class, NetzConfiguration.class,
	BenutzerConfiguration.class, OrganisationConfiguration.class, BarriereConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class, OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class })
class BarriereNetzBezugAenderungRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	BarriereRepository barriereRepository;
	@Autowired
	NetzService netzService;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	BarriereNetzBezugAenderungRepository barriereNetzBezugAenderungRepository;
	@Autowired
	BenutzerRepository benutzerRepository;

	@Test
	public void findBarriereNetzBezugAenderungByDatumAfter() {
		// arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Kante kante = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());

		Barriere barriere = barriereRepository.save(
			BarriereTestDataProvider.onKante(kante)
				.verantwortlicheOrganisation(gebietskoerperschaft)
				.build());

		Benutzer technischerBenutzer = benutzerRepository.save(
			BenutzerTestDataProvider.admin(gebietskoerperschaft).build());

		// alte Aenderung (soll nicht gefunden werden)
		BarriereNetzBezugAenderung outdatedAenderung = barriereNetzBezugAenderungRepository.save(
			new BarriereNetzBezugAenderung(
				NetzBezugAenderungsArt.KANTE_GELOESCHT,
				kante.getId(),
				barriere,
				technischerBenutzer,
				LocalDateTime.of(2022, 10, 12, 9, 59),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				GeometryTestdataProvider.createLineString()));

		// aktuelle Aenderung
		BarriereNetzBezugAenderung expectedAenderung = barriereNetzBezugAenderungRepository.save(
			new BarriereNetzBezugAenderung(
				NetzBezugAenderungsArt.KANTE_GELOESCHT,
				kante.getId(),
				barriere,
				technischerBenutzer,
				LocalDateTime.of(2022, 10, 13, 10, 0),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				GeometryTestdataProvider.createLineString()));

		// act
		List<BarriereNetzBezugAenderung> result = barriereNetzBezugAenderungRepository
			.findBarriereNetzBezugAenderungByDatumAfter(LocalDateTime.of(2022, 10, 12, 10, 0));

		// assert
		assertThat(barriereNetzBezugAenderungRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(1);
		assertThat(result).contains(expectedAenderung);
		assertThat(result).doesNotContain(outdatedAenderung);
	}

	@Test
	public void findBarriereNetzBezugAenderungByDatumAfterInBereich() {
		// arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Kante kante = netzService.saveKante(
			KanteTestDataProvider.withDefaultValues()
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 1), new Coordinate(1, 2)))
				.build());

		Barriere barriere = barriereRepository.save(
			BarriereTestDataProvider.onKante(kante)
				.verantwortlicheOrganisation(gebietskoerperschaft)
				.build());

		Benutzer technischerBenutzer = benutzerRepository.save(
			BenutzerTestDataProvider.admin(gebietskoerperschaft).build());

		// alte Aenderung (soll nicht gefunden werden)
		BarriereNetzBezugAenderung outdatedAenderung = barriereNetzBezugAenderungRepository.save(
			new BarriereNetzBezugAenderung(
				NetzBezugAenderungsArt.KANTE_GELOESCHT,
				kante.getId(),
				barriere,
				technischerBenutzer,
				LocalDateTime.of(2022, 10, 12, 9, 59),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				kante.getGeometry()));

		// aktuelle Aenderung
		BarriereNetzBezugAenderung expectedAenderung = barriereNetzBezugAenderungRepository.save(
			new BarriereNetzBezugAenderung(
				NetzBezugAenderungsArt.KANTE_GELOESCHT,
				kante.getId(),
				barriere,
				technischerBenutzer,
				LocalDateTime.of(2022, 10, 13, 10, 0),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				kante.getGeometry()));

		// act & assert (Netzbezugänderung außerhalb des Bereiches)
		List<BarriereNetzBezugAenderung> result = barriereNetzBezugAenderungRepository
			.findBarriereNetzBezugAenderungByDatumAfterInBereich(
				LocalDateTime.of(2022, 10, 12, 10, 0),
				GeometryTestdataProvider.createQuadratischerBereichAsPolygon(10, 10, 20, 20));

		assertThat(barriereNetzBezugAenderungRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(0);

		// act & assert (Netzbezugänderung innerhalb des Bereiches)
		result = barriereNetzBezugAenderungRepository
			.findBarriereNetzBezugAenderungByDatumAfterInBereich(
				LocalDateTime.of(2022, 10, 12, 10, 0),
				GeometryTestdataProvider.createQuadratischerBereichAsPolygon(0, 0, 20, 20));

		assertThat(barriereNetzBezugAenderungRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(1);
		assertThat(result).contains(expectedAenderung);
		assertThat(result).doesNotContain(outdatedAenderung);
	}
}