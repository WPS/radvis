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

package de.wps.radvis.backend.massnahme.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.Mock;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.Mailadresse;

class MassnahmenZustaendigkeitsServiceTest {

	@Mock
	private BenutzerRepository benutzerRepository;

	@Mock
	private VerwaltungseinheitRepository verwaltungseinheitRepository;

	private MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService;

	@BeforeEach
	void setup() {
		openMocks(this);
		massnahmenZustaendigkeitsService = new MassnahmenZustaendigkeitsService(benutzerRepository,
			verwaltungseinheitRepository);
	}

	@Test
	void getZustaendigeBearbeiter_bearbeiterDirektInOrga() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();
		Benutzer radwegeErfasserIn = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation).build();
		Benutzer kreiskoordinator = BenutzerTestDataProvider.kreiskoordinator(organisation).build();
		Benutzer radverkehrsBeauftrager = BenutzerTestDataProvider.defaultBenutzer().organisation(organisation)
			.rollen(Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER)).build();

		when(benutzerRepository.findByOrganisationAndStatus(organisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radwegeErfasserIn, kreiskoordinator, radverkehrsBeauftrager));

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();

		// act
		List<Benutzer> zustaendigeBearbeiter = massnahmenZustaendigkeitsService
			.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
				betroffeneMassnahme1);

		// assert
		assertThat(zustaendigeBearbeiter).containsExactlyInAnyOrder(radwegeErfasserIn, kreiskoordinator,
			radverkehrsBeauftrager);
	}

	@Test
	void findetEmpfaengerkreisAusMassnahmeWennZustaendigeBearbeiter() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation).build();
		when(benutzerRepository.findByOrganisationAndStatus(organisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(benutzer));

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.zustaendiger(organisation)
			.build();

		// act
		List<Benutzer> zustaendigeBearbeiter = massnahmenZustaendigkeitsService
			.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
				massnahme);

		// assert
		assertThat(zustaendigeBearbeiter).containsExactly(benutzer);
	}

	@Test
	void findetMehrereBenutzerEmpfaengerkreisAusMassnahmeWennZustaendigeBearbeiter() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();

		Benutzer benutzer1 = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation)
			.id(1L)
			.vorname(Name.of("BenutzerA")).mailadresse(
				Mailadresse.of("benutzerA@testRadvis.com"))
			.build();
		Benutzer benutzer2 = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation)
			.id(2L)
			.vorname(Name.of("BenutzerB")).mailadresse(
				Mailadresse.of("benutzerB@testRadvis.com"))
			.build();

		when(benutzerRepository.findByOrganisationAndStatus(organisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(benutzer1, benutzer2));

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.zustaendiger(organisation)
			.build();

		// act
		List<Benutzer> zustaendigeBearbeiter = massnahmenZustaendigkeitsService
			.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
				massnahme);

		// assert
		assertThat(zustaendigeBearbeiter).containsExactly(benutzer1, benutzer2);
	}

	@Test
	void findetKeineEmpfaengerkreisAusMassnahmeWennKeineZustaendigenBearbeiter() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().organisation(organisation).rollen(Set.of(
			Rolle.RADROUTEN_BEARBEITERIN)).build();
		when(benutzerRepository.findByOrganisationAndStatus(organisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(benutzer));

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.baulastZustaendiger(organisation)
			.build();

		// act
		List<Benutzer> zustaendigeBearbeiter = massnahmenZustaendigkeitsService
			.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
				massnahme);

		// assert
		assertThat(zustaendigeBearbeiter).isEmpty();
	}

	@Test
	void getZustaendigeRegierungsbezirke_filtersByIntesection() {
		// arrange
		Gebietskoerperschaft rp1 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 10, 10)).id(1L).build();
		Gebietskoerperschaft rp2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(100, 100, 110, 110)).id(2L).build();
		when(verwaltungseinheitRepository.findByOrganisationsArt(any())).thenReturn(List.of(rp1, rp2));
		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(KanteTestDataProvider
			.withCoordinates(new Coordinate[] { new Coordinate(5, 5), new Coordinate(15, 15) }).build()).build();

		// act
		List<Verwaltungseinheit> result = massnahmenZustaendigkeitsService.getZustaendigeRegierungsbezirke(massnahme);

		// assert
		assertThat(result).containsExactly(rp1);
	}
}
