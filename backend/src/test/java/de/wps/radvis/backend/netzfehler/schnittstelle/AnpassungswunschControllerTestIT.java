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

package de.wps.radvis.backend.netzfehler.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.KonsistenzregelVerletzungTestdataProvider;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.domain.KonsistenzregelVerletzungsRepository;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschRepository;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschService;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.netzfehler.schnittstelle.view.AnpassungswunschListenView;
import de.wps.radvis.backend.netzfehler.schnittstelle.view.AnpassungswunschView;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import jakarta.persistence.EntityNotFoundException;

@Tag("group6")
@ContextConfiguration(classes = {
	NetzfehlerConfiguration.class,
	KommentarConfiguration.class,
	GeoConverterConfiguration.class,
	OrganisationConfiguration.class,
	AnpassungswunschControllerTestIT.TestConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class AnpassungswunschControllerTestIT extends DBIntegrationTestIT {
	@EnableJpaRepositories(basePackageClasses = { BenutzerConfiguration.class,
		KonsistenzregelPruefungsConfiguration.class })
	@EntityScan(basePackageClasses = { KonsistenzregelPruefungsConfiguration.class })
	public static class TestConfiguration {
		@Autowired
		private AnpassungswunschService anpassungswunschService;

		@Autowired
		private AnpassungswunschRepository anpassungswunschRepository;

		@Autowired
		private AnpassungswunschGuard anpassungswunschGuard;

		@Autowired
		private VerwaltungseinheitService verwaltungseinheitService;

		@MockBean
		BenutzerResolver benutzerResolver;

		@Bean
		public AnpassungswunschController anpassungswunschController() {
			Mockito.when(benutzerResolver.fromAuthentication(Mockito.any()))
				.thenReturn(
					BenutzerTestDataProvider.admin(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
						.build());
			return new AnpassungswunschController(anpassungswunschService,
				anpassungswunschGuard, benutzerResolver, verwaltungseinheitService,
				anpassungswunschRepository, new SaveAnpassungswunschCommandConverter(verwaltungseinheitService));
		}
	}

	static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),
		KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

	private final CountDownLatch waiter = new CountDownLatch(1);

	@Autowired
	AnpassungswunschController anpassungswunschController;

	@Autowired
	private BenutzerResolver benutzerResolver;

	@Mock
	private Authentication authentication;

	@Autowired
	BenutzerRepository benutzerRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository;

	@Autowired
	PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() {
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.admin(gebietskoerperschaft).build();

		gebietskoerperschaftRepository.save(gebietskoerperschaft);
		benutzerRepository.save(benutzer);

		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(
			benutzer);
	}

	@Test
	void createAnpassungswunsch() throws AccessDeniedException {
		// Arrange
		SaveAnpassungswunschCommand command = new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(1.5, 10.5)),
			"Dies ist eine Beschreibung",
			AnpassungswunschStatus.NACHBEARBEITUNG, null, AnpassungswunschKategorie.DLM, null);

		// Act
		AnpassungswunschView anpassungswunsch = anpassungswunschController.createAnpassungswunsch(authentication,
			command);

		// Assert
		assertThat(anpassungswunsch.getErstellung()).isNotNull();
		assertThat(anpassungswunsch.getAenderung()).isNotNull();
		assertThat(anpassungswunsch.getBeschreibung()).isEqualTo("Dies ist eine Beschreibung");
		assertThat(anpassungswunsch.getGeometrie().getCoordinate()).isEqualTo(new Coordinate(1.5, 10.5));
		assertThat(anpassungswunsch.getStatus()).isEqualTo(AnpassungswunschStatus.NACHBEARBEITUNG);
		assertThat(anpassungswunsch.getUrsaechlicheKonsistenzregelVerletzung()).isNull();
	}

	@Test
	void createAnpassungswunsch_FuerKonsistenzregelVerletzung() throws AccessDeniedException {
		// Arrange
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.KONSISTENZREGEL_PRUEFJOB);
		KonsistenzregelVerletzung konsistenzregelVerletzung = new TransactionTemplate(transactionManager).execute(
			status -> konsistenzregelVerletzungsRepository.save(
				KonsistenzregelVerletzungTestdataProvider.defaultVerletzung().build()));
		AdditionalRevInfoHolder.clear();

		// Act
		AnpassungswunschView anpassungswunsch = anpassungswunschController.createAnpassungswunsch(authentication,
			new SaveAnpassungswunschCommand(geometryFactory.createPoint(new Coordinate(1.5, 10.5)), "abc",
				AnpassungswunschStatus.OFFEN, null, AnpassungswunschKategorie.DLM,
				String.format("KonsistenzregelVerletzung/%d", konsistenzregelVerletzung.getId())));

		// Assert
		assertThat(anpassungswunsch.getUrsaechlicheKonsistenzregelVerletzung()).isEqualTo(konsistenzregelVerletzung);
	}

	@Test
	void deleteAnpassungswunsch() throws AccessDeniedException {
		// Arrange
		SaveAnpassungswunschCommand command = new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(1.5, 10.5)),
			"Dies ist eine Beschreibung",
			AnpassungswunschStatus.NACHBEARBEITUNG, null, AnpassungswunschKategorie.DLM, null);
		AnpassungswunschView anpassungswunsch = anpassungswunschController.createAnpassungswunsch(authentication,
			command);

		// Act
		boolean didDeleteExisting = anpassungswunschController.deleteAnpassungswunsch(authentication,
			anpassungswunsch.getId());
		boolean didDeleteNotExisting = anpassungswunschController.deleteAnpassungswunsch(authentication, 1337L);

		// Assert
		assertThat(didDeleteExisting).isTrue();
		assertThat(didDeleteNotExisting).isFalse();
	}

	@Test
	void updateAnpassungswunsch() throws InterruptedException, AccessDeniedException {
		// Arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		SaveAnpassungswunschCommand createCommand = new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(1.5, 10.5)),
			"Dies ist eine Beschreibung", AnpassungswunschStatus.OFFEN, null,
			AnpassungswunschKategorie.DLM, null);
		AnpassungswunschView anpassungswunsch = anpassungswunschController.createAnpassungswunsch(authentication,
			createCommand);
		LocalDateTime erstesAenderungsDatum = anpassungswunsch.getAenderung();
		Point geometrie = anpassungswunsch.getGeometrie();
		LocalDateTime erstellung = anpassungswunsch.getErstellung();

		// Act
		waiter.await(100, TimeUnit.MILLISECONDS);

		SaveAnpassungswunschCommand updateCommand = new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(1.5, 10.5)), "Neue Beschreibung",
			AnpassungswunschStatus.KLAERUNGSBEDARF, gebietskoerperschaft.getId(), AnpassungswunschKategorie.DLM, null);

		AnpassungswunschView updatedAnpassungswunsch = anpassungswunschController.updateAnpassungswunsch(authentication,
			anpassungswunsch.getId(), updateCommand);

		// Assert
		assertThat(updatedAnpassungswunsch.getAenderung().equals(erstesAenderungsDatum)).isFalse();
		assertThat(updatedAnpassungswunsch.getErstellung().equals(erstellung)).isTrue();
		assertThat(updatedAnpassungswunsch.getGeometrie().equals(geometrie)).isTrue();
		assertThat(updatedAnpassungswunsch.getBeschreibung()).isEqualTo("Neue Beschreibung");
		assertThat(updatedAnpassungswunsch.getStatus()).isEqualTo(AnpassungswunschStatus.KLAERUNGSBEDARF);
		assertThat(updatedAnpassungswunsch.getVerantwortlicheOrganisation())
			.contains(new VerwaltungseinheitView(gebietskoerperschaft));
	}

	@Test
	void updateNonExistingAnpassungswunsch() {
		// Arrange
		// Act
		SaveAnpassungswunschCommand updateCommand = new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(1.5, 10.5)), "Neue Beschreibung",
			AnpassungswunschStatus.KLAERUNGSBEDARF, null, AnpassungswunschKategorie.DLM, null);

		assertThrows(EntityNotFoundException.class, () -> {
			anpassungswunschController.updateAnpassungswunsch(authentication, 1337L, updateCommand);
		});
	}

	@Test
	void alleAnpassungswuensche() throws AccessDeniedException {
		// Arrange
		Gebietskoerperschaft wunsch1Gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Wunsch 1 Organisation").build());

		anpassungswunschController.createAnpassungswunsch(authentication, new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(1.5, 10.5)),
			"Wunsch 1",
			AnpassungswunschStatus.OFFEN,
			wunsch1Gebietskoerperschaft.getId(),
			AnpassungswunschKategorie.DLM, null));

		anpassungswunschController.createAnpassungswunsch(authentication, new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(2.5, 11.5)), "Wunsch 2",
			AnpassungswunschStatus.ERLEDIGT, null, AnpassungswunschKategorie.DLM, null));

		anpassungswunschController.createAnpassungswunsch(authentication, new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(2.5, 20.5)), "Wunsch 3",
			AnpassungswunschStatus.KLAERUNGSBEDARF, null, AnpassungswunschKategorie.DLM, null));

		// Act
		List<AnpassungswunschListenView> alleAnpassungswuensche = anpassungswunschController.getAlleAnpassungswuensche(
			Optional.of(false));

		// Assert
		assertThat(alleAnpassungswuensche.size()).isEqualTo(3);
		AnpassungswunschListenView anpassungswunschListenView1 = alleAnpassungswuensche.get(0);
		assertThat(anpassungswunschListenView1.getBeschreibung()).isEqualTo("Wunsch 1");
		assertThat(anpassungswunschListenView1.getVerantwortlicheOrganisation().get().getName()).isEqualTo(
			"Wunsch 1 Organisation");
		assertThat(anpassungswunschListenView1.getStatus()).isEqualTo(AnpassungswunschStatus.OFFEN);
		assertThat(anpassungswunschListenView1.getGeometrie().getCoordinate()).isEqualTo(new Coordinate(1.5, 10.5));

		AnpassungswunschListenView anpassungswunschListenView2 = alleAnpassungswuensche.get(1);
		assertThat(anpassungswunschListenView2.getBeschreibung()).isEqualTo("Wunsch 2");
		assertThat(anpassungswunschListenView2.getVerantwortlicheOrganisation()).isEmpty();
		assertThat(anpassungswunschListenView2.getStatus()).isEqualTo(AnpassungswunschStatus.ERLEDIGT);
		assertThat(anpassungswunschListenView2.getGeometrie().getCoordinate()).isEqualTo(new Coordinate(2.5, 11.5));

		AnpassungswunschListenView anpassungswunschListenView3 = alleAnpassungswuensche.get(2);
		assertThat(anpassungswunschListenView3.getBeschreibung()).isEqualTo("Wunsch 3");
		assertThat(anpassungswunschListenView3.getVerantwortlicheOrganisation()).isEmpty();
		assertThat(anpassungswunschListenView3.getStatus()).isEqualTo(AnpassungswunschStatus.KLAERUNGSBEDARF);
		assertThat(anpassungswunschListenView3.getGeometrie().getCoordinate()).isEqualTo(new Coordinate(2.5, 20.5));
	}

	@Test
	void alleAnpassungswuensche_abgeschlosseneAusblenden() throws AccessDeniedException {
		// Arrange
		Gebietskoerperschaft wunsch1Organisation = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Wunsch 1 Organisation").build());

		anpassungswunschController.createAnpassungswunsch(authentication, new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(1.5, 10.5)),
			"Wunsch 1",
			AnpassungswunschStatus.OFFEN,
			wunsch1Organisation.getId(),
			AnpassungswunschKategorie.DLM, null));

		anpassungswunschController.createAnpassungswunsch(authentication, new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(2.5, 11.5)), "Wunsch 2",
			AnpassungswunschStatus.ERLEDIGT, null, AnpassungswunschKategorie.DLM, null));

		anpassungswunschController.createAnpassungswunsch(authentication, new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(2.5, 20.5)), "Wunsch 3",
			AnpassungswunschStatus.KLAERUNGSBEDARF, null, AnpassungswunschKategorie.DLM, null));

		anpassungswunschController.createAnpassungswunsch(authentication, new SaveAnpassungswunschCommand(
			geometryFactory.createPoint(new Coordinate(2.5, 42)), "Wunsch 4",
			AnpassungswunschStatus.ZURUECKGEZOGEN, null, AnpassungswunschKategorie.DLM, null));

		// Act
		List<AnpassungswunschListenView> alleAnpassungswuensche = anpassungswunschController.getAlleAnpassungswuensche(
			Optional.of(true));

		// Assert
		assertThat(alleAnpassungswuensche.size()).isEqualTo(2);
		AnpassungswunschListenView anpassungswunschListenView1 = alleAnpassungswuensche.get(0);
		assertThat(anpassungswunschListenView1.getBeschreibung()).isEqualTo("Wunsch 1");
		assertThat(anpassungswunschListenView1.getVerantwortlicheOrganisation().get().getName()).isEqualTo(
			"Wunsch 1 Organisation");
		assertThat(anpassungswunschListenView1.getStatus()).isEqualTo(AnpassungswunschStatus.OFFEN);
		assertThat(anpassungswunschListenView1.getGeometrie().getCoordinate()).isEqualTo(new Coordinate(1.5, 10.5));

		AnpassungswunschListenView anpassungswunschListenView3 = alleAnpassungswuensche.get(1);
		assertThat(anpassungswunschListenView3.getBeschreibung()).isEqualTo("Wunsch 3");
		assertThat(anpassungswunschListenView3.getVerantwortlicheOrganisation()).isEmpty();
		assertThat(anpassungswunschListenView3.getStatus()).isEqualTo(AnpassungswunschStatus.KLAERUNGSBEDARF);
		assertThat(anpassungswunschListenView3.getGeometrie().getCoordinate()).isEqualTo(new Coordinate(2.5, 20.5));
	}

}
