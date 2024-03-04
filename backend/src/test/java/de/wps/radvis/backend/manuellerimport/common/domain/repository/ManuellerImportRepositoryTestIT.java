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

package de.wps.radvis.backend.manuellerimport.common.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.manuellerimport.common.ManuellerImportCommonConfiguration;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group2")
@ContextConfiguration(classes = { ManuellerImportRepositoryTestIT.TestConfiguration.class })
public class ManuellerImportRepositoryTestIT extends DBIntegrationTestIT {
	@Configuration
	@EntityScan(basePackageClasses = { NetzConfiguration.class, OrganisationConfiguration.class,
		BenutzerConfiguration.class, ManuellerImportCommonConfiguration.class })
	@EnableJpaRepositories(basePackageClasses = { ManuellerImportFehlerRepository.class,
		VerwaltungseinheitRepository.class,
		BenutzerRepository.class })
	public static class TestConfiguration {
	}

	@Autowired
	ManuellerImportFehlerRepository manuellerImportFehlerRepository;
	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	BenutzerRepository benutzerRepository;

	private Verwaltungseinheit organisation;
	private Benutzer benutzer;
	private final static Polygon BEREICH = EnvelopeAdapter.toPolygon(new Envelope(0, 1000, 0, 1000),
		KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

	@BeforeEach
	public void setup() {
		organisation = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		benutzer = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(organisation).build());
	}

	@Nested
	public class FilterByOrgAndType {
		private ManuellerImportFehler netzklasseFehlerOrg1;
		private ManuellerImportFehler attributeFehlerOrg1;

		@BeforeEach
		public void setup() {
			LineString geometrie = GeometryTestdataProvider.createLineString();
			LocalDateTime importZeitpunkt = LocalDateTime.now().minusDays(1);
			Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
				.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

			manuellerImportFehlerRepository
				.save(new ManuellerImportFehler(geometrie, ImportTyp.NETZKLASSE_ZUWEISEN,
					importZeitpunkt, benutzer, gebietskoerperschaft));
			netzklasseFehlerOrg1 = manuellerImportFehlerRepository
				.save(new ManuellerImportFehler(geometrie, ImportTyp.NETZKLASSE_ZUWEISEN,
					importZeitpunkt, benutzer, organisation));

			manuellerImportFehlerRepository
				.save(new ManuellerImportFehler(geometrie, ImportTyp.ATTRIBUTE_UEBERNEHMEN,
					importZeitpunkt, benutzer, gebietskoerperschaft));
			attributeFehlerOrg1 = manuellerImportFehlerRepository
				.save(new ManuellerImportFehler(geometrie, ImportTyp.ATTRIBUTE_UEBERNEHMEN,
					importZeitpunkt, benutzer, organisation));
		}

		@Test
		public void getAllLatestByOrganisationAndType_Netzklasse() {
			// act
			List<ManuellerImportFehler> result = manuellerImportFehlerRepository
				.getAllLatestByOrganisationAndTypeInBereich(organisation, ImportTyp.NETZKLASSE_ZUWEISEN, BEREICH);

			// assert
			assertThat(result).containsExactly(netzklasseFehlerOrg1);
		}

		@Test
		public void getAllLatestByOrganisationAndType_Attribute() {
			// act
			List<ManuellerImportFehler> result = manuellerImportFehlerRepository
				.getAllLatestByOrganisationAndTypeInBereich(organisation, ImportTyp.ATTRIBUTE_UEBERNEHMEN, BEREICH);

			// assert
			assertThat(result).containsExactly(attributeFehlerOrg1);
		}
	}

	@Test
	public void latestByOrganisation() {
		// arrange
		LocalDateTime importZeitpunkt1 = LocalDateTime.now();
		LocalDateTime importZeitpunkt2 = LocalDateTime.now().minusDays(1);
		LocalDateTime importZeitpunkt3 = LocalDateTime.now().minusDays(2);
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		ManuellerImportFehler fehlerNewOrg1 = manuellerImportFehlerRepository
			.save(new ManuellerImportFehler(GeometryTestdataProvider.createLineString(),
				ImportTyp.NETZKLASSE_ZUWEISEN,
				importZeitpunkt1, benutzer, organisation));
		manuellerImportFehlerRepository.save(new ManuellerImportFehler(GeometryTestdataProvider.createLineString(),
			ImportTyp.NETZKLASSE_ZUWEISEN,
			importZeitpunkt2, benutzer, organisation));
		ManuellerImportFehler fehlerOlderOrg2 = manuellerImportFehlerRepository
			.save(new ManuellerImportFehler(GeometryTestdataProvider.createLineString(),
				ImportTyp.NETZKLASSE_ZUWEISEN,
				importZeitpunkt2, benutzer, gebietskoerperschaft));
		manuellerImportFehlerRepository.save(new ManuellerImportFehler(GeometryTestdataProvider.createLineString(),
			ImportTyp.NETZKLASSE_ZUWEISEN,
			importZeitpunkt3, benutzer, gebietskoerperschaft));

		// act
		List<ManuellerImportFehler> fehlerOrg2 = manuellerImportFehlerRepository
			.getAllLatestByOrganisationAndTypeInBereich(gebietskoerperschaft, ImportTyp.NETZKLASSE_ZUWEISEN, BEREICH);
		List<ManuellerImportFehler> fehlerOrg1 = manuellerImportFehlerRepository
			.getAllLatestByOrganisationAndTypeInBereich(organisation, ImportTyp.NETZKLASSE_ZUWEISEN, BEREICH);

		// assert
		assertThat(fehlerOrg1).containsExactly(fehlerNewOrg1);
		assertThat(fehlerOrg2).containsExactly(fehlerOlderOrg2);
	}

	@Test
	public void latestByNetzklasse() {
		// arrange
		LocalDateTime importZeitpunkt1 = LocalDateTime.now();
		LocalDateTime importZeitpunkt2 = LocalDateTime.now().minusDays(1);
		LocalDateTime importZeitpunkt3 = LocalDateTime.now().minusDays(2);
		ManuellerImportFehler fehlerNewNetzklasse = manuellerImportFehlerRepository
			.save(new ManuellerImportFehler(GeometryTestdataProvider.createLineString(),
				ImportTyp.NETZKLASSE_ZUWEISEN,
				importZeitpunkt1, benutzer, organisation));
		manuellerImportFehlerRepository.save(new ManuellerImportFehler(GeometryTestdataProvider.createLineString(),
			ImportTyp.NETZKLASSE_ZUWEISEN,
			importZeitpunkt2, benutzer, organisation));
		ManuellerImportFehler fehlerOlderAttribute = manuellerImportFehlerRepository
			.save(new ManuellerImportFehler(GeometryTestdataProvider.createLineString(),
				ImportTyp.ATTRIBUTE_UEBERNEHMEN,
				importZeitpunkt2, benutzer, organisation));
		manuellerImportFehlerRepository.save(new ManuellerImportFehler(GeometryTestdataProvider.createLineString(),
			ImportTyp.ATTRIBUTE_UEBERNEHMEN,
			importZeitpunkt3, benutzer, organisation));

		// act
		List<ManuellerImportFehler> fehlerAttribute = manuellerImportFehlerRepository
			.getAllLatestByOrganisationAndTypeInBereich(organisation, ImportTyp.ATTRIBUTE_UEBERNEHMEN, BEREICH);
		List<ManuellerImportFehler> fehlerNetzklasse = manuellerImportFehlerRepository
			.getAllLatestByOrganisationAndTypeInBereich(organisation, ImportTyp.NETZKLASSE_ZUWEISEN, BEREICH);

		// assert
		assertThat(fehlerNetzklasse).containsExactly(fehlerNewNetzklasse);
		assertThat(fehlerAttribute).containsExactly(fehlerOlderAttribute);
	}

	@Test
	public void latestByBereich() {
		// arrange
		LocalDateTime importZeitpunkt1 = LocalDateTime.now();
		LocalDateTime importZeitpunkt2 = LocalDateTime.now().minusDays(1);
		ManuellerImportFehler fehlerNewNetzklasseInBereich = manuellerImportFehlerRepository
			.save(new ManuellerImportFehler(
				GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)),
				ImportTyp.NETZKLASSE_ZUWEISEN,
				importZeitpunkt1, benutzer, organisation));
		ManuellerImportFehler fehlerNewNetzklasseNichtInBereich = manuellerImportFehlerRepository.save(
			new ManuellerImportFehler(
				GeometryTestdataProvider.createLineString(new Coordinate(1001, 1001), new Coordinate(2000, 2000)),
				ImportTyp.NETZKLASSE_ZUWEISEN,
				importZeitpunkt1, benutzer, organisation));
		ManuellerImportFehler fehlerOlderAttributeSchneidetBereich = manuellerImportFehlerRepository
			.save(new ManuellerImportFehler(
				GeometryTestdataProvider.createLineString(new Coordinate(800, 800), new Coordinate(1100, 1100)),
				ImportTyp.ATTRIBUTE_UEBERNEHMEN,
				importZeitpunkt2, benutzer, organisation));
		ManuellerImportFehler fehlerOlderAttributeNichtInBereich = manuellerImportFehlerRepository
			.save(new ManuellerImportFehler(
				GeometryTestdataProvider.createLineString(new Coordinate(3000, 3000), new Coordinate(1100, 1100)),
				ImportTyp.ATTRIBUTE_UEBERNEHMEN,
				importZeitpunkt2, benutzer, organisation));

		// act
		List<ManuellerImportFehler> fehlerAttribute = manuellerImportFehlerRepository
			.getAllLatestByOrganisationAndTypeInBereich(organisation, ImportTyp.ATTRIBUTE_UEBERNEHMEN, BEREICH);
		List<ManuellerImportFehler> fehlerNetzklasse = manuellerImportFehlerRepository
			.getAllLatestByOrganisationAndTypeInBereich(organisation, ImportTyp.NETZKLASSE_ZUWEISEN, BEREICH);

		// assert
		assertThat(fehlerNetzklasse).containsExactly(fehlerNewNetzklasseInBereich);
		assertThat(fehlerAttribute).containsExactly(fehlerOlderAttributeSchneidetBereich);
	}
}
