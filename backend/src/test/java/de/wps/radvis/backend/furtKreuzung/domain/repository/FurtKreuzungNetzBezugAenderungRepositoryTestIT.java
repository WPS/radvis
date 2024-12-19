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

package de.wps.radvis.backend.furtKreuzung.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

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
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.furtKreuzung.FurtKreuzungConfiguration;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungNetzBezugAenderung;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungTestDataProvider;
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
	BenutzerConfiguration.class, OrganisationConfiguration.class, FurtKreuzungConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class, OrganisationConfigurationProperties.class, NetzConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class })
@MockBeans({
	@MockBean(MailService.class),
})
class FurtKreuzungNetzBezugAenderungRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	FurtKreuzungRepository furtKreuzungRepository;
	@Autowired
	NetzService netzService;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	FurtKreuzungNetzBezugAenderungRepository furtKreuzungNetzBezugAenderungRepository;
	@Autowired
	BenutzerRepository benutzerRepository;

	@Test
	public void findFurtKreuzungNetzBezugAenderungByDatumAfter() {
		// arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Kante kante = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());

		FurtKreuzung furtKreuzung = furtKreuzungRepository.save(
			FurtKreuzungTestDataProvider.onKante(kante)
				.verantwortlicheOrganisation(gebietskoerperschaft)
				.build()
		);

		Benutzer technischerBenutzer = benutzerRepository.save(
			BenutzerTestDataProvider.admin(gebietskoerperschaft).build());

		// alte Aenderung (soll nicht gefunden werden)
		FurtKreuzungNetzBezugAenderung outdatedAenderung = furtKreuzungNetzBezugAenderungRepository.save(
			new FurtKreuzungNetzBezugAenderung(
				NetzBezugAenderungsArt.KANTE_GELOESCHT,
				kante.getId(),
				furtKreuzung,
				technischerBenutzer,
				LocalDateTime.of(2022, 10, 12, 9, 59),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				GeometryTestdataProvider.createLineString()
			)
		);

		// aktuelle Aenderung
		FurtKreuzungNetzBezugAenderung expectedAenderung = furtKreuzungNetzBezugAenderungRepository.save(
			new FurtKreuzungNetzBezugAenderung(
				NetzBezugAenderungsArt.KANTE_GELOESCHT,
				kante.getId(),
				furtKreuzung,
				technischerBenutzer,
				LocalDateTime.of(2022, 10, 13, 10, 0),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				GeometryTestdataProvider.createLineString()
			)
		);

		// act
		List<FurtKreuzungNetzBezugAenderung> result = furtKreuzungNetzBezugAenderungRepository
			.findFurtKreuzungNetzBezugAenderungByDatumAfter(LocalDateTime.of(2022, 10, 12, 10, 0));

		// assert
		assertThat(furtKreuzungNetzBezugAenderungRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(1);
		assertThat(result).contains(expectedAenderung);
		assertThat(result).doesNotContain(outdatedAenderung);
	}

	@Test
	public void findFurtKreuzungNetzBezugAenderungByDatumAfterInBereich() {
		// arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Kante kante = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());

		FurtKreuzung furtKreuzung = furtKreuzungRepository.save(
			FurtKreuzungTestDataProvider.onKante(kante)
				.verantwortlicheOrganisation(gebietskoerperschaft)
				.build()
		);

		Benutzer technischerBenutzer = benutzerRepository.save(
			BenutzerTestDataProvider.admin(gebietskoerperschaft).build());

		// alte Aenderung (soll nicht gefunden werden)
		FurtKreuzungNetzBezugAenderung outdatedAenderung = furtKreuzungNetzBezugAenderungRepository.save(
			new FurtKreuzungNetzBezugAenderung(
				NetzBezugAenderungsArt.KANTE_GELOESCHT,
				kante.getId(),
				furtKreuzung,
				technischerBenutzer,
				LocalDateTime.of(2022, 10, 12, 9, 59),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				GeometryTestdataProvider.createLineString()
			)
		);

		// aktuelle Aenderung
		FurtKreuzungNetzBezugAenderung expectedAenderung = furtKreuzungNetzBezugAenderungRepository.save(
			new FurtKreuzungNetzBezugAenderung(
				NetzBezugAenderungsArt.KANTE_GELOESCHT,
				kante.getId(),
				furtKreuzung,
				technischerBenutzer,
				LocalDateTime.of(2022, 10, 13, 10, 0),
				NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				GeometryTestdataProvider.createLineString()
			)
		);

		// act & assert (Netzbezugänderung außerhalb des Bereiches)
		List<FurtKreuzungNetzBezugAenderung> result = furtKreuzungNetzBezugAenderungRepository
			.findFurtKreuzungNetzBezugAenderungByDatumAfterInBereich(
				LocalDateTime.of(2022, 10, 12, 10, 0),
				GeometryTestdataProvider.createQuadratischerBereichAsPolygon(10, 10, 20, 20)
			);

		assertThat(furtKreuzungNetzBezugAenderungRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(0);

		// act & assert (Netzbezugänderung innerhalb des Bereiches)
		result = furtKreuzungNetzBezugAenderungRepository
			.findFurtKreuzungNetzBezugAenderungByDatumAfterInBereich(
				LocalDateTime.of(2022, 10, 12, 10, 0),
				GeometryTestdataProvider.createQuadratischerBereichAsPolygon(0, 0, 20, 20)
			);

		assertThat(furtKreuzungNetzBezugAenderungRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(1);
		assertThat(result).contains(expectedAenderung);
		assertThat(result).doesNotContain(outdatedAenderung);
	}
}