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

package de.wps.radvis.backend.massnahme.domain.dbView;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeListenDbViewTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class MassnahmeListenDbViewTest {

	@SuppressWarnings("unchecked")
	@Test
	void getGeometry_nichtArchiviert_returnsNetzbezug() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(234l).build();
		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante)
			.id(1L)
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Baulast-Zuständiger")
					.organisationsArt(OrganisationsArt.KREIS)
					.build())
			.unterhaltsZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Unterhalts-Zuständiger")
					.organisationsArt(OrganisationsArt.GEMEINDE)
					.build())
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		MassnahmeListenDbView massnahmeListenDbView = MassnahmeListenDbViewTestDataProvider.withMassnahme(massnahme)
			.build();

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry().getNumGeometries()).isEqualTo(1);
		assertThat(massnahmeListenDbView.getGeometry().getGeometryN(0)).isEqualTo(kante.getGeometry());
	}

	@SuppressWarnings("unchecked")
	@Test
	void getGeometry_archiviert_linesAndPoints() {
		// arrange
		Kante kante = KanteTestDataProvider
			.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(645l).build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).id(5346l)
					.build())
			.id(234l).build();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.netzbezug(new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1.0),
					Seitenbezug.BEIDSEITIG)),
				Collections.emptySet(), Set.of(kante.getVonKnoten())))
			.id(1L)
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Baulast-Zuständiger")
					.organisationsArt(OrganisationsArt.KREIS)
					.build())
			.unterhaltsZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Unterhalts-Zuständiger")
					.organisationsArt(OrganisationsArt.GEMEINDE)
					.build())
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme.archivieren();
		massnahme.removeKnotenFromNetzbezug(List.of(kante.getVonKnoten().getId()));
		MassnahmeListenDbView massnahmeListenDbView = MassnahmeListenDbViewTestDataProvider.withMassnahme(massnahme)
			.build();

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry().getNumGeometries()).isEqualTo(2);
		assertThat(massnahmeListenDbView.getGeometry().getGeometryN(0).getGeometryN(0)).isEqualTo(kante.getGeometry());
		assertThat(massnahmeListenDbView.getGeometry().getGeometryN(1).getGeometryN(0))
			.isEqualTo(kante.getVonKnoten().getPoint());
	}

	@SuppressWarnings("unchecked")
	@Test
	void getGeometry_archiviert_onlyPoints() {
		// arrange
		Kante kante = KanteTestDataProvider
			.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(645l).build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).id(5346l)
					.build())
			.id(234l).build();
		Massnahme massnahme = MassnahmeTestDataProvider.withKnoten(kante.getVonKnoten())
			.id(1L)
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Baulast-Zuständiger")
					.organisationsArt(OrganisationsArt.KREIS)
					.build())
			.unterhaltsZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Unterhalts-Zuständiger")
					.organisationsArt(OrganisationsArt.GEMEINDE)
					.build())
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme.archivieren();
		massnahme.removeKnotenFromNetzbezug(List.of(kante.getVonKnoten().getId()));
		MassnahmeListenDbView massnahmeListenDbView = MassnahmeListenDbViewTestDataProvider.withMassnahme(massnahme)
			.build();

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry().getNumGeometries()).isEqualTo(1);
		assertThat(massnahmeListenDbView.getGeometry().getGeometryN(0).getGeometryN(0))
			.isEqualTo(kante.getVonKnoten().getPoint());
	}

	@SuppressWarnings("unchecked")
	@Test
	void getGeometry_archiviert_onlyLines() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(234l).build();
		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante)
			.id(1L)
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Baulast-Zuständiger")
					.organisationsArt(OrganisationsArt.KREIS)
					.build())
			.unterhaltsZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Unterhalts-Zuständiger")
					.organisationsArt(OrganisationsArt.GEMEINDE)
					.build())
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme.archivieren();
		massnahme.removeKanteFromNetzbezug(List.of(kante.getId()));
		MassnahmeListenDbView massnahmeListenDbView = MassnahmeListenDbViewTestDataProvider.withMassnahme(massnahme)
			.build();

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry().getNumGeometries()).isEqualTo(1);
		assertThat(massnahmeListenDbView.getGeometry().getGeometryN(0).getGeometryN(0)).isEqualTo(kante.getGeometry());
	}

	@Test
	void getGeometry_archiviert_noNetzbezugSnapshot_returnsEmpty() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(234l).build();
		Massnahme massnahme = MassnahmeTestDataProvider.withKanten(kante)
			.id(1L)
			.letzteAenderung(LocalDateTime.of(2020, 10, 1, 10, 12)).planungErforderlich(true)
			.sollStandard(SollStandard.KEIN_STANDARD_ERFUELLT)
			.baulastZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Baulast-Zuständiger")
					.organisationsArt(OrganisationsArt.KREIS)
					.build())
			.unterhaltsZustaendiger(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.name("Beispiel-Unterhalts-Zuständiger")
					.organisationsArt(OrganisationsArt.GEMEINDE)
					.build())
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();
		massnahme.removeKanteFromNetzbezug(List.of(kante.getId()));
		massnahme.archivieren();
		MassnahmeListenDbView massnahmeListenDbView = MassnahmeListenDbViewTestDataProvider.withMassnahme(massnahme)
			.build();

		// act + assert
		assertThat(massnahmeListenDbView.getGeometry().getNumGeometries()).isEqualTo(0);
	}

}
