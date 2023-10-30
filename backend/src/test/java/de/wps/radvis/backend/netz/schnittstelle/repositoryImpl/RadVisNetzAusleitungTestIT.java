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

package de.wps.radvis.backend.netz.schnittstelle.repositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group5")
@EnableJpaRepositories(basePackageClasses = { FahrradrouteConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class })
@EntityScan(basePackageClasses = { FahrradrouteConfiguration.class,
	NetzConfiguration.class, OrganisationConfiguration.class, BenutzerConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class })
@ContextConfiguration(classes = { CommonConfiguration.class, GeoConverterConfiguration.class })
public class RadVisNetzAusleitungTestIT extends DBIntegrationTestIT {
	@Autowired
	FahrradrouteRepository fahrradrouteRepository;
	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	KantenRepository kantenRepository;

	@PersistenceContext
	EntityManager entityManager;

	@Test
	public void RadvisNetzAusleitungGeoserverView_Fields_Exist_Test() {
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		entityManager.flush();
		entityManager.clear();

		List<String> kantenAttribute = getFieldsOfColumnExcept("kanten_attribut_gruppe",
			Set.of("id", "version", "vereinbarungs_kennung", "gemeinde_id"));
		kantenAttribute.add("netzklassen");
		kantenAttribute.add("standards");
		kantenAttribute.add("gemeinde_name");
		kantenAttribute.add("landkreis_name");
		List<String> geschwindigkeitsAttribute = getFieldsOfColumnExcept(
			"geschwindigkeit_attribut_gruppe_geschwindigkeit_attribute",
			Set.of("id", "geschwindigkeit_attribut_gruppe_id", "von", "bis"));
		List<String> fuehrungsformAttribute = getFieldsOfColumnExcept("fuehrungsform_attribut_gruppe_attribute_links",
			Set.of("id", "fuehrungsform_attribut_gruppe_id", "von", "bis", "trennstreifen_breite_rechts",
				"trennstreifen_breite_links", "trennstreifen_trennung_zu_rechts", "trennstreifen_trennung_zu_links",
				"trennstreifen_form_rechts", "trennstreifen_form_links"));
		List<String> zustaendigkeitsAttribute = getFieldsOfColumnExcept(
			"zustaendigkeit_attribut_gruppe_zustaendigkeit_attribute",
			Set.of("id", "zustaendigkeit_attribut_gruppe_id", "von", "bis"))
			.stream().map(name -> name.replace("_id", "")).collect(Collectors.toList());
		List<String> fahrtrichtungsAttribute = getFieldsOfColumnExcept("fahrtrichtung_attribut_gruppe",
			Set.of("id", "version"));

		kantenRepository.refreshRadVisNetzMaterializedView();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntries).hasSize(2);
		assertThat(allViewEntries)
			.allSatisfy(entry -> assertThat(entry.keySet()).containsAll(kantenAttribute));
		assertThat(allViewEntries)
			.allSatisfy(entry -> assertThat(entry.keySet()).containsAll(geschwindigkeitsAttribute));
		assertThat(allViewEntries)
			.allSatisfy(entry -> assertThat(entry.keySet()).containsAll(fuehrungsformAttribute));
		assertThat(allViewEntries)
			.allSatisfy(entry -> assertThat(entry.keySet()).containsAll(zustaendigkeitsAttribute));
		assertThat(allViewEntries)
			.allSatisfy(entry -> assertThat(entry.keySet()).containsAll(fahrtrichtungsAttribute));
		assertThat(allViewEntries)
			.allSatisfy(entry -> assertThat(entry.keySet()).containsAll(List.of("id", "geometry")));
		assertThat(allViewEntries)
			.allSatisfy(entry -> assertThat(entry.keySet()).hasSize(
				2 + kantenAttribute.size() + geschwindigkeitsAttribute.size() + fuehrungsformAttribute.size()
					+ zustaendigkeitsAttribute.size() + fahrtrichtungsAttribute.size()));
	}

	@Test
	public void RadvisNetzAusleitungGeoserverView_Organisations_Have_Name() {
		Verwaltungseinheit gebietskoerperschaft1 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("The best organisation").build());
		Verwaltungseinheit gebietskoerperschaft2 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("The bestest organisation").build());
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
				.zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
						.baulastTraeger(gebietskoerperschaft1)
						.unterhaltsZustaendiger(gebietskoerperschaft2)
						.erhaltsZustaendiger(gebietskoerperschaft1)
						.vereinbarungsKennung(VereinbarungsKennung.of("狂言 Ado")).build()))
				.build())
			.build());

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshRadVisNetzMaterializedView();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntries).hasSize(1);
		Map<String, Object> entry = allViewEntries.get(0);
		assertThat(entry.get("baulast_traeger")).isEqualTo(gebietskoerperschaft1.getName());
		assertThat(entry.get("unterhalts_zustaendiger")).isEqualTo(gebietskoerperschaft2.getName());
		assertThat(entry.get("erhalts_zustaendiger")).isEqualTo(gebietskoerperschaft1.getName());
		assertThat(entry.get("vereinbarungs_kennung")).isEqualTo("狂言 Ado");
	}

	@Test
	public void RadvisNetzAusleitungGeoserverView_netzklassen_und_standards() {
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT))
				.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.BASISSTANDARD))
				.build())
			.build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of())
				.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
				.build())
			.build());

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshRadVisNetzMaterializedView();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntries).hasSize(2);
		Map<String, Object> entry1 = allViewEntries.get(0);
		Map<String, Object> entry2 = allViewEntries.get(1);
		assertThat(((String) entry1.get("netzklassen")).split(";", -1))
			.containsExactlyInAnyOrder(
				Netzklasse.RADNETZ_FREIZEIT.name(), Netzklasse.RADNETZ_ALLTAG.name(),
				Netzklasse.KOMMUNALNETZ_FREIZEIT.name());
		assertThat(((String) entry1.get("standards")).split(";", -1))
			.containsExactlyInAnyOrder(
				IstStandard.STARTSTANDARD_RADNETZ.name(), IstStandard.BASISSTANDARD.name());
		assertThat(((String) entry2.get("netzklassen"))).isEqualTo(null);
		assertThat(((String) entry2.get("standards"))).isEqualTo(IstStandard.STARTSTANDARD_RADNETZ.name());
	}

	@Test
	public void RadvisNetzAusleitungGeoserverView_DlmAndRadVisKantenEnthalten() {
		// Arrange
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.dlmId(null)
			.quelle(QuellSystem.RadVis)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT))
				.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.BASISSTANDARD))
				.build())
			.build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of())
				.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
				.build())
			.build());

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshRadVisNetzMaterializedView();

		List<Map<String, Object>> allViewEntriesBeforeRefresh = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntriesBeforeRefresh).hasSize(2);
		Map<String, Object> entry1 = allViewEntriesBeforeRefresh.get(0);
		Map<String, Object> entry2 = allViewEntriesBeforeRefresh.get(1);
		assertThat(entry1.get("id")).isEqualTo(kante1.getId());
		assertThat(entry2.get("id")).isEqualTo(kante2.getId());
		assertThat(((String) entry1.get("netzklassen")).split(";", -1))
			.containsExactlyInAnyOrder(
				Netzklasse.RADNETZ_FREIZEIT.name(), Netzklasse.RADNETZ_ALLTAG.name(),
				Netzklasse.KOMMUNALNETZ_FREIZEIT.name());
		assertThat(((String) entry1.get("standards")).split(";", -1))
			.containsExactlyInAnyOrder(
				IstStandard.STARTSTANDARD_RADNETZ.name(), IstStandard.BASISSTANDARD.name());
		assertThat(((String) entry2.get("netzklassen"))).isEqualTo(null);
		assertThat(((String) entry2.get("standards"))).isEqualTo(IstStandard.STARTSTANDARD_RADNETZ.name());
	}

	@Test
	public void RadvisNetzAusleitungGeoserverView_refreshTwice() {
		// Arrange
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT))
				.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.BASISSTANDARD))
				.build())
			.build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of())
				.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
				.build())
			.build());

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshRadVisNetzMaterializedView();

		List<Map<String, Object>> allViewEntriesBeforeRefresh = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntriesBeforeRefresh).hasSize(2);
		Map<String, Object> entry1 = allViewEntriesBeforeRefresh.get(0);
		Map<String, Object> entry2 = allViewEntriesBeforeRefresh.get(1);
		assertThat(entry1.get("id")).isEqualTo(kante1.getId());
		assertThat(entry2.get("id")).isEqualTo(kante2.getId());
		assertThat(((String) entry1.get("netzklassen")).split(";", -1))
			.containsExactlyInAnyOrder(
				Netzklasse.RADNETZ_FREIZEIT.name(), Netzklasse.RADNETZ_ALLTAG.name(),
				Netzklasse.KOMMUNALNETZ_FREIZEIT.name());
		assertThat(((String) entry1.get("standards")).split(";", -1))
			.containsExactlyInAnyOrder(
				IstStandard.STARTSTANDARD_RADNETZ.name(), IstStandard.BASISSTANDARD.name());
		assertThat(((String) entry2.get("netzklassen"))).isEqualTo(null);
		assertThat(((String) entry2.get("standards"))).isEqualTo(IstStandard.STARTSTANDARD_RADNETZ.name());

		// Act
		kantenRepository.delete(kante2);

		Kante kante3 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.RADSCHNELLVERBINDUNG))
				.istStandards(Set.of())
				.build())
			.build());

		kantenRepository.findById(kante1.getId())
			.ifPresent(kante -> kante.getKantenAttributGruppe()
				.updateNetzklassen(Set.of(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ALLTAG)));

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshRadVisNetzMaterializedView();

		// Assert
		List<Map<String, Object>> allViewEntriesAfterRefresh = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntriesAfterRefresh).hasSize(2);
		Map<String, Object> entryOne = allViewEntriesAfterRefresh.get(0);
		Map<String, Object> entryTwo = allViewEntriesAfterRefresh.get(1);
		assertThat(entryOne.get("id")).isEqualTo(kante1.getId());
		assertThat(entryTwo.get("id")).isEqualTo(kante3.getId());
		assertThat(((String) entryOne.get("netzklassen")).split(";", -1))
			.containsExactlyInAnyOrder(
				Netzklasse.RADNETZ_FREIZEIT.name(), Netzklasse.RADNETZ_ALLTAG.name());
		assertThat(((String) entryOne.get("standards")).split(";", -1))
			.containsExactlyInAnyOrder(
				IstStandard.STARTSTANDARD_RADNETZ.name(), IstStandard.BASISSTANDARD.name());
		assertThat(((String) entryTwo.get("netzklassen")).split(";", -1)).containsExactlyInAnyOrder(
			Netzklasse.KREISNETZ_ALLTAG.name(),
			Netzklasse.RADSCHNELLVERBINDUNG.name());
		assertThat(((String) entryTwo.get("standards"))).isEqualTo(null);

	}

	@Test
	public void RadvisNetzAusleitungGeoserverView_maxAnteilBeiLR() {
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().isZweiseitig(true).build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.2)
							.vereinbarungsKennung(VereinbarungsKennung.of("Too small!")).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.7)
							.vereinbarungsKennung(VereinbarungsKennung.of("Largest segment!")).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.7, 1)
							.vereinbarungsKennung(VereinbarungsKennung.of("Also too small!")).build()))
					.build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().build()))
				.fuehrungsformAttributeRechts(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.3)
						.breite(Laenge.of(3)).build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 1)
						.breite(Laenge.of(10)).build()))
				.build())
			.build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues()
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().isZweiseitig(true).build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.1)
							.vereinbarungsKennung(VereinbarungsKennung.of("Too small!")).build()
						// Ambiguous maximum!
						// Note that the subtraction in the query will not necessarily find all of them
						// For this example, bis - von creates fractions:
						// 0.30000000000000004
						// 0.29999999999999993
						// 0.30000000000000004
						// Thus, it only finds two identical maximums!
						, ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.1, 0.4)
							.vereinbarungsKennung(VereinbarungsKennung.of("First largest segment!")).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.7)
							.vereinbarungsKennung(VereinbarungsKennung.of("Second largest segment!")).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.7, 1)
							.vereinbarungsKennung(VereinbarungsKennung.of("Third largest segment!")).build()))
					.build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fuehrungsformAttributeLinks(
					List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().build()))
				.fuehrungsformAttributeRechts(List.of(
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.7)
						.breite(Laenge.of(3)).build(),
					FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.7, 1)
						.breite(Laenge.of(10)).build()))
				.build())
			.build());

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshRadVisNetzMaterializedView();

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_radvisnetz_kante_materialized_view");

		assertThat(allViewEntries).hasSize(2); // Important to check disambiguation of multiple maximums!
		Map<String, Object> entry1 = allViewEntries.get(0);
		Map<String, Object> entry2 = allViewEntries.get(1);
		assertThat(((String) entry1.get("vereinbarungs_kennung"))).isEqualTo("Largest segment!");
		assertThat(((BigDecimal) entry1.get("breite")).doubleValue()).isEqualTo(10.0);
		System.out.println(entry2.get("vereinbarungs_kennung"));
		assertThat(((String) entry2.get("vereinbarungs_kennung")))
			.isEqualTo("First largest segment!", "Second largest segment!", "Third largest segment!");
		assertThat(((BigDecimal) entry2.get("breite")).doubleValue()).isEqualTo(3.0);
	}

	private List<String> getFieldsOfColumnExcept(String column_name, Set<String> except) {
		return jdbcTemplate.queryForList(
				"select column_name "
					+ "from INFORMATION_SCHEMA.COLUMNS "
					+ "where TABLE_NAME='" + column_name + "'")
			.stream()
			.map(map -> (String) map.get("column_name"))
			.filter(columnName -> !except.contains(columnName))
			.collect(Collectors.toList());
	}
}
