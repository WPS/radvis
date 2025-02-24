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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.KantenMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.Haendigkeit;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationsartUndNameNichtEindeutigException;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;

@Tag("group1")
@EnableEnversRepositories(basePackageClasses = { NetzConfiguration.class, OrganisationConfiguration.class,
	BenutzerConfiguration.class })
@EntityScan(basePackageClasses = { NetzConfiguration.class, OrganisationConfiguration.class,
	BenutzerConfiguration.class })
class RadVISMapperTestIT extends DBIntegrationTestIT {
	private static final Set<String> NICHT_IMPORTIERTE_ATTRIBUTE = Set
		.of("geometry", "id", "baulast_traeger_art", "seite", "letzte_aenderung", "landkreis_name", "netzklassen",
			"kante_id", "balm_id");
	@Autowired
	KantenRepository kantenRepository;
	@Autowired
	EntityManager entityManager;
	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Mock
	VerwaltungseinheitService verwaltungseinheitService;
	@MockitoBean
	FeatureToggleProperties featureToggleProperties;

	private RadVISMapper radVISMapper;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		radVISMapper = new RadVISMapper(verwaltungseinheitService);
	}

	@Test
	void alleRadvisAttributeUnterstuetzt() {
		// arrange
		kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		entityManager.flush();
		kantenRepository.refreshNetzMaterializedViews();
		List<Map<String, Object>> radvisGeoView = jdbcTemplate
			.queryForList("Select * from geoserver_radvisnetz_kante_abschnitte_materialized_view");

		// act + assert
		assertThat(radvisGeoView).isNotEmpty();
		Set<String> zuUnterstuetzendeAttribute = toShpProperties(radvisGeoView.get(0)).keySet();

		assertThat(zuUnterstuetzendeAttribute).allSatisfy(attr -> {
			assertThat(radVISMapper.isAttributNameValid(attr))
				.withFailMessage("Attribut wird beim Import nicht unterstützt: %s", attr).isTrue();
		});
	}

	@Test
	void nurRadvisAttributeUnterstuetzt() {
		// arrange
		kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		entityManager.flush();
		kantenRepository.refreshNetzMaterializedViews();
		List<Map<String, Object>> radvisGeoView = jdbcTemplate
			.queryForList("Select * from geoserver_radvisnetz_kante_abschnitte_materialized_view");

		// assert
		assertThat(radvisGeoView).isNotEmpty();
		assertThat(radvisGeoView).isNotEmpty();
		Set<String> allowedAttributes = toShpProperties(radvisGeoView.get(0)).keySet();

		assertThat(allowedAttributes).containsAll(RadVISMapper.UNTERSTUETZTE_ATTRIBUTE);
	}

	@Test
	void map_alleWerteKorrektAbgebildet() throws OrganisationsartUndNameNichtEindeutigException {
		// arrange
		Gebietskoerperschaft gemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		when(verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(any(), any()))
			.thenReturn(Optional.of(gemeinde));
		when(verwaltungseinheitService.hasVerwaltungseinheitNachNameUndArt(any(), any())).thenReturn(true);
		Kante kante = kantenRepository.save(KanteTestDataProvider.createWithValues(gemeinde).build());
		entityManager.flush();
		kantenRepository.refreshNetzMaterializedViews();
		List<Map<String, Object>> radvisGeoView = jdbcTemplate
			.queryForList("Select * from geoserver_radvisnetz_kante_abschnitte_materialized_view");
		MappingService mappingService = new MappingService();

		// act
		assertThat(radvisGeoView).isNotEmpty();
		Map<String, Object> shpProperties = toShpProperties(radvisGeoView.get(0));
		Kante kante2 = KanteTestDataProvider.withDefaultValues().id(3456l).build();
		KantenMapping kantenMapping = new KantenMapping(kante2);
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.valueOf(radvisGeoView.get(0).get("seite").toString())));

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
			kante.getGeometry());
		RadVISMapper.UNTERSTUETZTE_ATTRIBUTE.stream().sorted(radVISMapper::sortAttribute).forEach(attr -> {
			mappingService.map(radVISMapper, attr, kantenMapping, kantenKonfliktProtokoll);
		});

		// assert
		assertThat(kante2.getKantenAttributGruppe()).usingRecursiveComparison()
			.ignoringFields("id", "version", "netzklassen").isEqualTo(kante.getKantenAttributGruppe());
		assertThat(kante2.getFahrtrichtungAttributGruppe()).usingRecursiveComparison()
			.ignoringFields("id", "version").isEqualTo(kante.getFahrtrichtungAttributGruppe());
		assertThat(kante2.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute().get(0))
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringFields("id", "version")
			.isEqualTo(kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute().get(0));
		assertThat(kante2.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.isEqualTo(kante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0));
		assertThat(kante2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.isEqualTo(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0));
		assertThat(kante2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.isEqualTo(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0));
	}

	@Test
	void map_seitenbezogen_alleWerteKorrektAbgebildet() throws OrganisationsartUndNameNichtEindeutigException {
		// arrange
		Gebietskoerperschaft gemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		when(verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(any(), any()))
			.thenReturn(Optional.of(gemeinde));
		when(verwaltungseinheitService.hasVerwaltungseinheitNachNameUndArt(any(), any())).thenReturn(true);
		Kante kante = kantenRepository.save(KanteTestDataProvider.createZweiseitigWithValues(gemeinde).build());
		entityManager.flush();
		kantenRepository.refreshNetzMaterializedViews();
		List<Map<String, Object>> radvisGeoView = jdbcTemplate
			.queryForList("Select * from geoserver_radvisnetz_kante_abschnitte_materialized_view");
		MappingService mappingService = new MappingService();

		// act
		assertThat(radvisGeoView).isNotEmpty();
		Kante kante2 = KanteTestDataProvider.withDefaultValues().id(3456l).build();
		// zweiseitige Kanten haben jeweils einen Eintrag für links und rechts
		radvisGeoView.forEach(seite -> {
			Map<String, Object> shpProperties = toShpProperties(seite);
			KantenMapping kantenMapping = new KantenMapping(kante2);
			kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
				LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
				Seitenbezug.valueOf(seite.get("seite").toString())));

			KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(),
				kante.getGeometry());
			RadVISMapper.UNTERSTUETZTE_ATTRIBUTE.stream().sorted(radVISMapper::sortAttribute).forEach(attr -> {
				mappingService.map(radVISMapper, attr, kantenMapping, kantenKonfliktProtokoll);
			});
		});

		// assert
		assertThat(kante2.getKantenAttributGruppe()).usingRecursiveComparison()
			.ignoringFields("id", "version", "netzklassen").isEqualTo(kante.getKantenAttributGruppe());
		assertThat(kante2.getFahrtrichtungAttributGruppe()).usingRecursiveComparison()
			.ignoringFields("id", "version").isEqualTo(kante.getFahrtrichtungAttributGruppe());
		assertThat(kante2.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute().get(0))
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringFields("id", "version")
			.isEqualTo(kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute().get(0));
		assertThat(kante2.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.isEqualTo(kante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0));
		assertThat(kante2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.isEqualTo(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0));
		assertThat(kante2.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.isEqualTo(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0));
	}

	@Test
	void map_mappingFuerAlleAttributeImplementiert() throws OrganisationsartUndNameNichtEindeutigException {
		// arrange
		Gebietskoerperschaft gemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		when(verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(any(), any()))
			.thenReturn(Optional.of(gemeinde));
		when(verwaltungseinheitService.hasVerwaltungseinheitNachNameUndArt(any(), any())).thenReturn(true);
		Kante kante = kantenRepository.save(KanteTestDataProvider.createWithValues(gemeinde).build());
		entityManager.flush();
		kantenRepository.refreshNetzMaterializedViews();
		List<Map<String, Object>> radvisGeoView = jdbcTemplate
			.queryForList("Select * from geoserver_radvisnetz_kante_abschnitte_materialized_view");
		MappingService mappingService = new MappingService();

		// act + assert
		assertThat(radvisGeoView).isNotEmpty();
		Map<String, Object> shpProperties = toShpProperties(radvisGeoView.get(0));
		assertThat(shpProperties.entrySet()).allSatisfy((entry) -> {
			assertThat(Objects.toString(entry.getValue()))
				.withFailMessage(
					"Alle Attribute sollen abgebildet werden, aber dieses ist null und wird ignoriert: %s \n Bitte Testsetup nachziehen.",
					entry.getKey())
				.isNotBlank();
		});

		KantenMapping kantenMapping = new KantenMapping(kante);
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.valueOf(radvisGeoView.get(0).get("seite").toString())));

		assertThat(RadVISMapper.UNTERSTUETZTE_ATTRIBUTE).allSatisfy(attr -> {
			assertThatNoException().isThrownBy(() -> mappingService.map(radVISMapper, attr, kantenMapping,
				new KantenKonfliktProtokoll(kante.getId(), kante.getGeometry())));
		});
	}

	@Test
	void map_seitenbezogen_mappingFuerAlleAttributeImplementiert()
		throws OrganisationsartUndNameNichtEindeutigException {
		// arrange
		Gebietskoerperschaft gemeinde = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		when(verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(any(), any()))
			.thenReturn(Optional.of(gemeinde));
		when(verwaltungseinheitService.hasVerwaltungseinheitNachNameUndArt(any(), any())).thenReturn(true);
		Kante kante = kantenRepository.save(KanteTestDataProvider.createZweiseitigWithValues(gemeinde).build());
		entityManager.flush();
		kantenRepository.refreshNetzMaterializedViews();
		List<Map<String, Object>> radvisGeoView = jdbcTemplate
			.queryForList("Select * from geoserver_radvisnetz_kante_abschnitte_materialized_view");
		MappingService mappingService = new MappingService();

		// act + assert
		assertThat(radvisGeoView).isNotEmpty();
		Map<String, Object> shpProperties = toShpProperties(radvisGeoView.get(0));
		assertThat(shpProperties.entrySet()).allSatisfy((entry) -> {
			assertThat(Objects.toString(entry.getValue()))
				.withFailMessage(
					"Alle Attribute sollen abgebildet werden, aber dieses ist null und wird ignoriert: %s \n Bitte Testsetup nachziehen.",
					entry.getKey())
				.isNotBlank();
		});

		KantenMapping kantenMapping = new KantenMapping(kante);
		kantenMapping.add(MappedFeature.of(kante.getGeometry(), shpProperties,
			LinearReferenzierterAbschnitt.of(0, 1), Haendigkeit.of(kante.getGeometry(), kante.getGeometry()),
			Seitenbezug.valueOf(radvisGeoView.get(0).get("seite").toString())));

		assertThat(RadVISMapper.UNTERSTUETZTE_ATTRIBUTE).allSatisfy(attr -> {
			assertThatNoException().isThrownBy(() -> mappingService.map(radVISMapper, attr, kantenMapping,
				new KantenKonfliktProtokoll(kante.getId(), kante.getGeometry())));
		});
	}

	private Map<String, Object> toShpProperties(Map<String, Object> dbAttributes) {
		Map<String, Object> result = new HashMap<>();
		dbAttributes.keySet().stream().filter(k -> {
			return !NICHT_IMPORTIERTE_ATTRIBUTE.contains(k);
		}).forEach(attr -> {
			String croppedPropertyName = attr.substring(0, Math.min(attr.length(), 10));
			if (result.keySet().contains(croppedPropertyName)) {
				// falls es je mehr als eine Kollision pro attributname geben sollte, muss hier was angepasst werden
				String resolvedAmbiguousName = croppedPropertyName.substring(0, croppedPropertyName.length() - 1) + "0";
				result.put(resolvedAmbiguousName, dbAttributes.get(attr));
			} else {
				result.put(croppedPropertyName, dbAttributes.get(attr));
			}
		});
		return result;
	}
}
