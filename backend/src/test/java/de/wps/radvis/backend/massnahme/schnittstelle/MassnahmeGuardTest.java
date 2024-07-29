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

package de.wps.radvis.backend.massnahme.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.dokument.schnittstelle.AddDokumentCommand;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class MassnahmeGuardTest {

	ZustaendigkeitsService zustaendigkeitsService;
	@Mock
	NetzService netzService;
	@Mock
	BenutzerResolver benutzerResolver;
	@Mock
	MassnahmeService massnahmeService;
	@Mock
	MultipartFile mockedMultipartFile;
	@Mock
	OrganisationConfigurationProperties organisationConfigurationProperties;

	MassnahmeGuard massnahmeGuard;

	@BeforeEach
	void beforeEach() throws IOException {
		MockitoAnnotations.openMocks(this);
		when(organisationConfigurationProperties.getZustaendigkeitBufferInMeter()).thenReturn(500);
		zustaendigkeitsService = new ZustaendigkeitsService(organisationConfigurationProperties);
		massnahmeGuard = new MassnahmeGuard(zustaendigkeitsService, netzService, benutzerResolver, massnahmeService);
	}

	@Nested
	class NoMassnahmeRecht {
		@Mock
		Authentication authentication;
		LineString geometryWithinZustaendigkeitsbereich;
		Benutzer benutzer;

		@BeforeEach
		public void setup() {
			MockitoAnnotations.openMocks(this);

			Polygon zustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 100),
					new Coordinate(100, 100), new Coordinate(100, 0), new Coordinate(0, 0) });
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createMultiPolygon(new Polygon[] { zustaendigkeitsbereich }))
				.build();
			benutzer = BenutzerTestDataProvider.defaultBenutzer()
				.rollen(Set.of(Rolle.RADVIS_BETRACHTER))
				.organisation(organisation).build();
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(benutzer);
			geometryWithinZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(10, 10), new Coordinate(50, 50) });
		}

		@Test
		public void saveMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertThrows(AccessDeniedException.class, () -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void createMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertThrows(AccessDeniedException.class, () -> massnahmeGuard.createMassnahme(authentication,
				CreateMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void saveUmsetzungsstand() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long umsetzungsstandId = 356L;
			when(massnahmeService.getNetzbezugByUmsetzungsstandId(umsetzungsstandId)).thenReturn(
				Optional.of(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)));

			assertThrows(AccessDeniedException.class, () -> massnahmeGuard.saveUmsetzungsstand(authentication,
				SaveUmsetzungsstandCommandTestDataProvider.defaultValue().id(umsetzungsstandId).build()));
		}

		@Test
		public void deleteDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());

			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteDatei(authentication, massnahmeId));
		}

		@Test
		public void uploadDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());
			AddDokumentCommand command = new AddDokumentCommand("foobar.jpg");

			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.uploadDatei(massnahmeId, command, mockedMultipartFile, authentication));
		}

		@Test
		public void canMassnahmeBearbeiten() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeBearbeiten(benutzer, massnahme)).isFalse();
		}

		@Test
		public void canMassnahmeLoeschen() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahme)).isFalse();
			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahmeRadNETZ)).isFalse();
		}

		@Test
		public void deleteMassnahme() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteMassnahme(authentication, massnahme));
			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteMassnahme(authentication, massnahmeRadNETZ));
		}

	}

	@Nested
	class UmsetzungsStandAbfrage {
		@Mock
		Authentication authentication;

		@BeforeEach
		public void setup() {
			MockitoAnnotations.openMocks(this);
		}

		@Test
		public void withRecht() {
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(BenutzerTestDataProvider
					.bearbeiterinVmRadnetzAdminInaktiv(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
					.status(BenutzerStatus.AKTIV).build());

			assertDoesNotThrow(() -> massnahmeGuard.starteUmsetzungsstandsabfrage(authentication));
		}

		@Test
		public void ohneRecht() {
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(BenutzerTestDataProvider
					.radwegeErfasserinKommuneKreis(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
					.build());

			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.starteUmsetzungsstandsabfrage(authentication));
		}
	}

	@Nested
	class AlleMassnahmenMitZustaendigkeitsbereich {
		@Mock
		Authentication authentication;
		LineString geometryWithinZustaendigkeitsbereich;
		Benutzer benutzer;

		@BeforeEach
		public void setup() {
			MockitoAnnotations.openMocks(this);

			Polygon zustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 100),
					new Coordinate(100, 100), new Coordinate(100, 0), new Coordinate(0, 0) });
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createMultiPolygon(new Polygon[] { zustaendigkeitsbereich }))
				.build();
			benutzer = BenutzerTestDataProvider.admin(organisation).build();
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(benutzer);
			geometryWithinZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(10, 10), new Coordinate(50, 50) });
		}

		@Test
		public void saveMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertDoesNotThrow(() -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void createMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertDoesNotThrow(() -> massnahmeGuard.createMassnahme(authentication,
				CreateMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void saveUmsetzungsstand() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long umsetzungsstandId = 356L;
			when(massnahmeService.getNetzbezugByUmsetzungsstandId(umsetzungsstandId)).thenReturn(
				Optional.of(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)));

			assertDoesNotThrow(() -> massnahmeGuard.saveUmsetzungsstand(authentication,
				SaveUmsetzungsstandCommandTestDataProvider.defaultValue().id(umsetzungsstandId).build()));
		}

		@Test
		public void deleteDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());

			assertDoesNotThrow(() -> massnahmeGuard.deleteDatei(authentication, massnahmeId));
		}

		@Test
		public void uploadDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());
			AddDokumentCommand command = new AddDokumentCommand("foobar.jpg");

			assertDoesNotThrow(
				() -> massnahmeGuard.uploadDatei(massnahmeId, command, mockedMultipartFile, authentication));
		}

		@Test
		public void canMassnahmeBearbeiten() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeBearbeiten(benutzer, massnahme)).isTrue();
		}

		@Test
		public void canMassnahmeLoeschen_nichtRadNetz() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahme)).isTrue();
			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahmeRadNETZ)).isFalse();
		}

		@Test
		public void deleteMassnahme() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertDoesNotThrow(() -> massnahmeGuard.deleteMassnahme(authentication, massnahme));
			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteMassnahme(authentication, massnahmeRadNETZ));
		}
	}

	@Nested
	class AlleMassnahmenOhneZustaendigkeitsbereich {
		@Mock
		Authentication authentication;
		LineString geometryWithinZustaendigkeitsbereich;
		Benutzer benutzer;

		@BeforeEach
		public void setup() {
			MockitoAnnotations.openMocks(this);

			Polygon zustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 100),
					new Coordinate(100, 100), new Coordinate(100, 0), new Coordinate(0, 0) });
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createMultiPolygon(new Polygon[] { zustaendigkeitsbereich }))
				.build();
			benutzer = BenutzerTestDataProvider.admin(organisation).build();
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(benutzer);
			geometryWithinZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(200, 200), new Coordinate(500, 500) });
		}

		@Test
		public void saveMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertDoesNotThrow(() -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void createMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertDoesNotThrow(() -> massnahmeGuard.createMassnahme(authentication,
				CreateMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void saveUmsetzungsstand() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long umsetzungsstandId = 356L;
			when(massnahmeService.getNetzbezugByUmsetzungsstandId(umsetzungsstandId)).thenReturn(
				Optional.of(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)));

			assertDoesNotThrow(() -> massnahmeGuard.saveUmsetzungsstand(authentication,
				SaveUmsetzungsstandCommandTestDataProvider.defaultValue().id(umsetzungsstandId).build()));
		}

		@Test
		public void deleteDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());

			assertDoesNotThrow(() -> massnahmeGuard.deleteDatei(authentication, massnahmeId));
		}

		@Test
		public void uploadDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());
			AddDokumentCommand command = new AddDokumentCommand("foobar.jpg");

			assertDoesNotThrow(
				() -> massnahmeGuard.uploadDatei(massnahmeId, command, mockedMultipartFile, authentication));
		}

		@Test
		public void canMassnahmeBearbeiten() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeBearbeiten(benutzer, massnahme)).isTrue();
		}

		@Test
		public void canMassnahmeLoeschen() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahme)).isTrue();
			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahmeRadNETZ)).isFalse();
		}

		@Test
		public void deleteMassnahme() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertDoesNotThrow(() -> massnahmeGuard.deleteMassnahme(authentication,
				massnahme));

			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteMassnahme(authentication, massnahmeRadNETZ));
		}
	}

	@Nested
	class Zustaendigkeit_VerschiedeneGeometrien {
		@Mock
		Authentication authentication;
		LineString linesStringWithinZustaendigkeitsbereich;
		LineString lineStringOutsideZustaendigkeitsbereich;
		Point pointWithinZustaendigkeitsbereich;
		Point pointOutsideZustaendigkeitsbereich;
		Benutzer benutzer;
		long kanteId = 1L;
		long knotenId = 2L;
		long punktuellerNetzbezugId = 3L;

		@BeforeEach
		public void setup() {
			MockitoAnnotations.openMocks(this);

			Polygon zustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 100),
					new Coordinate(100, 100), new Coordinate(100, 0), new Coordinate(0, 0) });
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createMultiPolygon(new Polygon[] { zustaendigkeitsbereich }))
				.build();
			benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation).build();
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(benutzer);
			linesStringWithinZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(10, 10), new Coordinate(50, 50) });
			lineStringOutsideZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(1000, 1000), new Coordinate(5000, 5000) });
			pointWithinZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createPoint(new Coordinate(10, 10));
			pointOutsideZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createPoint(new Coordinate(1000, 1000));
		}

		@Test
		public void saveMassnahme_kanteDrin_knotenDrin() {
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(linesStringWithinZustaendigkeitsbereich)
				.build());
			when(netzService.getKnoten(knotenId)).thenReturn(KnotenTestDataProvider.withDefaultValues()
				.point(pointWithinZustaendigkeitsbereich)
				.build());

			assertDoesNotThrow(() -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withNetzbezuege(List.of(kanteId), List.of(knotenId), List.of())
					.build()));
		}

		@Test
		public void saveMassnahme_kanteDrin_knotenDraussen() {
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(linesStringWithinZustaendigkeitsbereich)
				.build());
			when(netzService.getKnoten(knotenId)).thenReturn(KnotenTestDataProvider.withDefaultValues()
				.point(pointOutsideZustaendigkeitsbereich)
				.build());

			assertThrows(AccessDeniedException.class, () -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withNetzbezuege(List.of(kanteId), List.of(knotenId), List.of())
					.build()));
		}

		@Test
		public void saveMassnahme_kanteDrin_knotenDrin_punktDraussen() {
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(linesStringWithinZustaendigkeitsbereich)
				.build());
			when(netzService.getKnoten(knotenId)).thenReturn(KnotenTestDataProvider.withDefaultValues()
				.point(pointWithinZustaendigkeitsbereich)
				.build());
			when(netzService.getKante(punktuellerNetzbezugId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(lineStringOutsideZustaendigkeitsbereich)
				.build());

			assertThrows(AccessDeniedException.class, () -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withNetzbezuege(List.of(kanteId), List.of(knotenId),
					List.of(punktuellerNetzbezugId)).build()));
		}

		@Test
		public void saveMassnahme_kanteDrin_knotenDrin_punktDrin() {
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(linesStringWithinZustaendigkeitsbereich)
				.build());
			when(netzService.getKnoten(knotenId)).thenReturn(KnotenTestDataProvider.withDefaultValues()
				.point(pointWithinZustaendigkeitsbereich)
				.build());
			when(netzService.getKante(punktuellerNetzbezugId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(linesStringWithinZustaendigkeitsbereich)
				.build());

			assertDoesNotThrow(() -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withNetzbezuege(List.of(kanteId), List.of(knotenId),
					List.of(punktuellerNetzbezugId)).build()));
		}
	}

	@Nested
	class ZustaendigeMassnahmenOhneZustaendigkeitsbereich {
		@Mock
		Authentication authentication;
		LineString geometryWithinZustaendigkeitsbereich;
		Benutzer benutzer;

		@BeforeEach
		public void setup() {
			MockitoAnnotations.openMocks(this);

			Polygon zustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 100),
					new Coordinate(100, 100), new Coordinate(100, 0), new Coordinate(0, 0) });
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createMultiPolygon(new Polygon[] { zustaendigkeitsbereich }))
				.build();
			benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation).build();
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(benutzer);
			geometryWithinZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(700, 700), new Coordinate(1000, 1000) });
		}

		@Test
		public void saveMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertThrows(AccessDeniedException.class, () -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void createMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertThrows(AccessDeniedException.class, () -> massnahmeGuard.createMassnahme(authentication,
				CreateMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void saveUmsetzungsstand() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long umsetzungsstandId = 356L;
			when(massnahmeService.getNetzbezugByUmsetzungsstandId(umsetzungsstandId)).thenReturn(
				Optional.of(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)));

			assertThrows(AccessDeniedException.class, () -> massnahmeGuard.saveUmsetzungsstand(authentication,
				SaveUmsetzungsstandCommandTestDataProvider.defaultValue().id(umsetzungsstandId).build()));
		}

		@Test
		public void deleteDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());

			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteDatei(authentication, massnahmeId));
		}

		@Test
		public void uploadDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());
			AddDokumentCommand command = new AddDokumentCommand("foobar.jpg");

			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.uploadDatei(massnahmeId, command, mockedMultipartFile, authentication));
		}

		@Test
		public void canMassnahmeBearbeiten() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeBearbeiten(benutzer, massnahme)).isFalse();
		}

		@Test
		public void canMassnahmeLoeschen() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahme)).isFalse();
			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahmeRadNETZ)).isFalse();
		}

		@Test
		public void deleteMassnahme() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteMassnahme(authentication, massnahme));
			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteMassnahme(authentication, massnahmeRadNETZ));
		}

	}

	@Nested
	class ZustaendigeMassnahmenMitZustaendigkeitsbereich {
		@Mock
		Authentication authentication;
		LineString geometryWithinZustaendigkeitsbereich;
		Benutzer benutzer;

		@BeforeEach
		public void setup() {
			MockitoAnnotations.openMocks(this);

			Polygon zustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 100),
					new Coordinate(100, 100), new Coordinate(100, 0), new Coordinate(0, 0) });
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createMultiPolygon(new Polygon[] { zustaendigkeitsbereich }))
				.build();
			benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation).build();
			when(benutzerResolver.fromAuthentication(any()))
				.thenReturn(benutzer);
			geometryWithinZustaendigkeitsbereich = KoordinatenReferenzSystem.ETRS89_UTM32_N
				.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(10, 10), new Coordinate(50, 50) });
		}

		@Test
		public void saveMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertDoesNotThrow(() -> massnahmeGuard.saveMassnahme(authentication,
				SaveMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void createMassnahme() {
			long kanteId = 1L;
			when(netzService.getKante(kanteId)).thenReturn(KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich)
				.build());

			assertDoesNotThrow(() -> massnahmeGuard.createMassnahme(authentication,
				CreateMassnahmeCommandTestDataProvider.withKante(kanteId).build()));
		}

		@Test
		public void saveUmsetzungsstand() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long umsetzungsstandId = 356L;
			when(massnahmeService.getNetzbezugByUmsetzungsstandId(umsetzungsstandId)).thenReturn(
				Optional.of(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)));

			assertDoesNotThrow(() -> massnahmeGuard.saveUmsetzungsstand(authentication,
				SaveUmsetzungsstandCommandTestDataProvider.defaultValue().id(umsetzungsstandId).build()));
		}

		@Test
		public void deleteDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());

			assertDoesNotThrow(() -> massnahmeGuard.deleteDatei(authentication, massnahmeId));
		}

		@Test
		public void uploadDatei() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Long massnahmeId = 356L;
			when(massnahmeService.get(massnahmeId)).thenReturn(
				MassnahmeTestDataProvider.withDefaultValues()
					.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build());
			AddDokumentCommand command = new AddDokumentCommand("foobar.jpg");

			assertDoesNotThrow(
				() -> massnahmeGuard.uploadDatei(massnahmeId, command, mockedMultipartFile, authentication));
		}

		@Test
		public void canMassnahmeBearbeiten() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeBearbeiten(benutzer, massnahme)).isTrue();
		}

		@Test
		public void canMassnahmeLoeschen() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);
			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahme)).isTrue();
			assertThat(massnahmeGuard.canMassnahmeLoeschen(benutzer, massnahmeRadNETZ)).isFalse();
		}

		@Test
		public void deleteMassnahme() {
			long kanteId = 45L;
			Kante kante = KanteTestDataProvider.withDefaultValues()
				.geometry(geometryWithinZustaendigkeitsbereich).id(kanteId)
				.build();
			when(netzService.getKante(kanteId)).thenReturn(kante);

			Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();
			Massnahme massnahmeRadNETZ = MassnahmeTestDataProvider.withDefaultValues()
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
				.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante)).build();

			assertDoesNotThrow(() -> massnahmeGuard.deleteMassnahme(authentication, massnahme));
			assertThrows(AccessDeniedException.class,
				() -> massnahmeGuard.deleteMassnahme(authentication, massnahmeRadNETZ));
		}
	}

}
