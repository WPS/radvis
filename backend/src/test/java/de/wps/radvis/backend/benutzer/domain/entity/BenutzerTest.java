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

package de.wps.radvis.backend.benutzer.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class BenutzerTest {

	private Verwaltungseinheit testOrganisation;

	@BeforeEach
	void setUp() {
		testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Coole Organisation")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();

	}

	@Test
	void benutzer_alleFelderNichtNull() {
		// arrange + act + assert
		assertDoesNotThrow(() -> new Benutzer(Name.of("AlterTestus"), Name.of("Testperson"), BenutzerStatus.INAKTIV,
			testOrganisation, Mailadresse.of("gueltigeEmailAdresse@testRadvis.de"), ServiceBwId.of("Some ID"),
			Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN)));

	}

	@Test
	void benutzerErstellen_wirdNichtErstellt_vornameIstNull() {
		// arrange + act + assert
		assertThrows(RequireViolation.class,
			() -> new Benutzer(null, Name.of("Testperson"), BenutzerStatus.INAKTIV,
				testOrganisation, Mailadresse.of("gueltigeEmailAdresse@testRadvis.de"), ServiceBwId.of("Some ID"),
				Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN)));
	}

	@Test
	void benutzerErstellen_wirdNichtErstellt_anzahlRollenIstLeer() {
		// arrange + act + assert
		assertThrows(RequireViolation.class,
			() -> new Benutzer(null, Name.of("Testperson"), BenutzerStatus.INAKTIV,
				testOrganisation, Mailadresse.of("gueltigeEmailAdresse@testRadvis.de"), ServiceBwId.of("Some ID"),
				Set.of()));
	}

	@Test
	void getRechte() {
		// arrange
		Benutzer benutzer = new Benutzer(
			Name.of("Testus"),
			Name.of("testperson"),
			BenutzerStatus.AKTIV,
			testOrganisation,
			Mailadresse.of("user@testRadvis.de"),
			ServiceBwId.of("ServiceBwId"),
			Set.of(Rolle.KREISKOORDINATOREN, Rolle.RADVERKEHRSBEAUFTRAGTER));

		// act+assert
		assertThat(benutzer.getRechte()).containsExactlyInAnyOrder(
			Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN,
			Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN,
			Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT,
			Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER,
			Recht.UMSETZUNGSSTANDSABFRAGEN_AUSWERTEN,
			Recht.RADNETZ_ROUTENVERLEGUNGEN,
			Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
			Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
			Recht.RADVERKEHRSBEAUFTRAGTER,
			Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN,
			Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
			Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
			Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN,
			Recht.BETRACHTER_EXTERNER_DIENSTLEISTER,
			Recht.ANPASSUNGSWUENSCHE_BEARBEITEN,
			Recht.ANPASSUNGSWUENSCHE_ERFASSEN,
			Recht.KREISNETZ_ROUTENVERLEGUNGEN
		);
	}

}
