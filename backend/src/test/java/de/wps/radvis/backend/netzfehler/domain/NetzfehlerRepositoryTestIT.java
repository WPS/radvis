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

package de.wps.radvis.backend.netzfehler.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerBeschreibung;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group5")
@EnableJpaRepositories(basePackageClasses = { NetzfehlerConfiguration.class })
@EntityScan(basePackageClasses = { NetzfehlerConfiguration.class, KommentarConfiguration.class })
class NetzfehlerRepositoryTestIT extends DBIntegrationTestIT {
	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Autowired
	private NetzfehlerRepository repository;

	@PersistenceContext
	EntityManager entityManager;

	@Test
	void testgetNetzfehlerInBereich() {
		// Arrange
		Netzfehler netzfehlerInnerhalb = new Netzfehler(
			NetzfehlerTyp.ATTRIBUT_ABBILDUNG,
			NetzfehlerBeschreibung.of("fehlerInnerhalb"),
			"steveJob",
			GEO_FACTORY.createLineString(new Coordinate[] { new Coordinate(1, 10), new Coordinate(2, 20) }));
		Netzfehler netzfehlerTeilweiseAusserhalb = new Netzfehler(
			NetzfehlerTyp.ATTRIBUT_ABBILDUNG,
			NetzfehlerBeschreibung.of("netzfehlerTeilweiseAusserhalb"),
			"steveJob",
			GEO_FACTORY.createLineString(new Coordinate[] { new Coordinate(1, 10), new Coordinate(2, 40) }));
		Netzfehler netzfehlerKomplettAusserhalb = new Netzfehler(
			NetzfehlerTyp.ATTRIBUT_ABBILDUNG,
			NetzfehlerBeschreibung.of("netzfehlerKomplettAusserhalb"),
			"steveJob",
			GEO_FACTORY.createLineString(new Coordinate[] { new Coordinate(31, 31), new Coordinate(40, 40) }));
		Netzfehler erledigt = new Netzfehler(
			NetzfehlerTyp.ATTRIBUT_ABBILDUNG,
			NetzfehlerBeschreibung.of("Der ist sowas von done"),
			"steveJob",
			GEO_FACTORY.createLineString(new Coordinate[] { new Coordinate(12, 10), new Coordinate(14, 20) }));
		erledigt.alsErledigtMarkieren();

		repository.save(netzfehlerInnerhalb);
		repository.save(netzfehlerTeilweiseAusserhalb);
		repository.save(netzfehlerKomplettAusserhalb);
		repository.save(erledigt);

		entityManager.flush();
		entityManager.clear();

		// Act
		Iterable<Netzfehler> result = repository
			.getNetzfehlerInBereich(new Envelope(0, 30, 0, 30));

		// Assert
		assertThat(result).isNotEmpty();
		assertThat(result).extracting(Netzfehler::getGeometry)
			.containsExactlyInAnyOrder(netzfehlerInnerhalb.getGeometry(),
				netzfehlerTeilweiseAusserhalb.getGeometry());
	}

}
