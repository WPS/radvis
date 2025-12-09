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

package de.wps.radvis.backend.abstellanlage.domain.entity;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.abstellanlage.domain.entity.provider.AbstellanlageTestDataProvider;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBeschreibung;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBetreiber;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenOrt;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenStatus;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenWeitereInformation;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlLademoeglichkeiten;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlSchliessfaecher;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlStellplaetze;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.ExterneAbstellanlagenId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProJahr;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProMonat;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProTag;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Groessenklasse;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.MobiDataQuellId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.common.GeometryTestdataProvider;

class AbstellanlageTest {

	@Test
	void konstruktor_groessenklasseNurBeiBikeAndRide() {
		assertThatExceptionOfType(RequireViolation.class).isThrownBy(
			() -> AbstellanlageTestDataProvider.withDefaultValues()
				.abstellanlagenOrt(AbstellanlagenOrt.SONSTIGES)
				.groessenklasse(Groessenklasse.HOTSPOT_XL)
				.build());
	}

	@Test
	void konstruktor_QuellSystemMobiData_MobidataQuellIdRequired() {
		Abstellanlage.AbstellanlageBuilder abstellanlageBuilderMobiData = AbstellanlageTestDataProvider
			.withDefaultValues()
			.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW);

		assertThatNoException().isThrownBy(() -> abstellanlageBuilderMobiData.mobiDataQuellId(MobiDataQuellId.of(10)));

		assertThatExceptionOfType(RequireViolation.class).isThrownBy(() -> abstellanlageBuilderMobiData.mobiDataQuellId(
			null).build());
	}

	@Test
	void konstruktor_QuellSystemRadVIS_MobidataQuellIdDarfNichtGesetztSein() {
		Abstellanlage.AbstellanlageBuilder abstellanlageBuilderRadVIS = AbstellanlageTestDataProvider
			.withDefaultValues()
			.quellSystem(AbstellanlagenQuellSystem.RADVIS);

		assertThatNoException().isThrownBy(() -> abstellanlageBuilderRadVIS.mobiDataQuellId(null));

		assertThatExceptionOfType(RequireViolation.class).isThrownBy(() -> abstellanlageBuilderRadVIS.mobiDataQuellId(
			MobiDataQuellId.of(10)).build());
	}

	@Test
	void update_groessenklasseNurBeiBikeAndRide() {
		assertThatExceptionOfType(RequireViolation.class).isThrownBy(
			() -> AbstellanlageTestDataProvider.withDefaultValues().build()
				.update(
					GeometryTestdataProvider.createPoint(new Coordinate(0, 0)),
					AbstellanlagenBetreiber.of("betreiberString"),
					ExterneAbstellanlagenId.of("externe-id_12###"),
					null,
					AnzahlStellplaetze.of(20),
					AnzahlSchliessfaecher.of(10),
					AnzahlLademoeglichkeiten.of(2),
					Ueberwacht.UNBEKANNT,
					AbstellanlagenOrt.SONSTIGES,
					Groessenklasse.BASISANGEBOT_XXS,
					Stellplatzart.FAHRRADBOX,
					Ueberdacht.of(false),
					GebuehrenProTag.of(2),
					GebuehrenProMonat.of(10),
					GebuehrenProJahr.of(100),
					AbstellanlagenBeschreibung.of("beschreibung"),
					AbstellanlagenWeitereInformation.of("weitereInformation"),
					AbstellanlagenStatus.AKTIV
				));
	}

	@Test
	void update_nurFuerQuellsystemRadVIS() {
		assertThatNoException().isThrownBy(() -> defaultUpdate.accept(
			AbstellanlageTestDataProvider.withDefaultValues().quellSystem(AbstellanlagenQuellSystem.RADVIS).build()
		));

		assertThatExceptionOfType(RequireViolation.class).isThrownBy(() -> defaultUpdate.accept(
			AbstellanlageTestDataProvider.withDefaultValues().quellSystem(AbstellanlagenQuellSystem.MOBIDATABW).build()
		));
	}

	Consumer<Abstellanlage> defaultUpdate = abstellanlage -> abstellanlage.update(
		GeometryTestdataProvider.createPoint(new Coordinate(0, 0)),
		AbstellanlagenBetreiber.of("betreiberString"),
		ExterneAbstellanlagenId.of("externe-id_12###"),
		null,
		AnzahlStellplaetze.of(20),
		AnzahlSchliessfaecher.of(10),
		AnzahlLademoeglichkeiten.of(2),
		Ueberwacht.UNBEKANNT,
		AbstellanlagenOrt.SONSTIGES,
		null,
		Stellplatzart.FAHRRADBOX,
		Ueberdacht.of(false),
		GebuehrenProTag.of(2),
		GebuehrenProMonat.of(10),
		GebuehrenProJahr.of(100),
		AbstellanlagenBeschreibung.of("beschreibung"),
		AbstellanlagenWeitereInformation.of("weitereInformation"),
		AbstellanlagenStatus.AKTIV
	);

}
