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
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@ExtendWith(MockitoExtension.class)
class SaveAnpassungswunschCommandConverterTest {
	@Mock
	private VerwaltungseinheitResolver verwaltungseinheitResolver;
	@InjectMocks
	SaveAnpassungswunschCommandConverter saveAnpassungswunschCommandConverter;

	@Test
	void apply_aktualisiertAttribute() {
		// arrange
		Point point = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(0, 1));

		Anpassungswunsch anpassungswunsch = new Anpassungswunsch(
			point,
			"alte Beschreibung",
			AnpassungswunschStatus.OFFEN,
			AnpassungswunschKategorie.RADVIS,
			BenutzerTestDataProvider.defaultBenutzer().id(1L).build(),
			Optional.of(VerwaltungseinheitTestDataProvider.defaultOrganisation().id(1L).build()),
			Optional.empty());

		Point neuerPoint = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPoint(new Coordinate(2, 3));
		String neueBeschreibung = "Neue Beschreibung";
		AnpassungswunschStatus neuerStatus = AnpassungswunschStatus.KLAERUNGSBEDARF;
		Organisation neueOrganisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().id(2L).build();
		AnpassungswunschKategorie neueKategorie = AnpassungswunschKategorie.OSM;

		when(verwaltungseinheitResolver.resolve(2L)).thenReturn(neueOrganisation);

		// act
		SaveAnpassungswunschCommand command = new SaveAnpassungswunschCommand(
			neuerPoint,
			neueBeschreibung,
			neuerStatus,
			neueOrganisation.getId(),
			neueKategorie,
			null);

		Benutzer neuerBenutzer = BenutzerTestDataProvider.defaultBenutzer().id(2L).build();
		saveAnpassungswunschCommandConverter.apply(anpassungswunsch, command, neuerBenutzer);

		assertThat(anpassungswunsch.getGeometrie().getCoordinate()).isEqualTo(neuerPoint.getCoordinate());
		assertThat(anpassungswunsch.getBeschreibung()).isEqualTo(neueBeschreibung);
		assertThat(anpassungswunsch.getStatus()).isEqualTo(neuerStatus);
		assertThat(anpassungswunsch.getKategorie()).isEqualTo(neueKategorie);
		assertThat(anpassungswunsch.getBenutzerLetzteAenderung()).isEqualTo(neuerBenutzer);
		assertThat(anpassungswunsch.getVerantwortlicheOrganisation()).contains(neueOrganisation);
	}
}
