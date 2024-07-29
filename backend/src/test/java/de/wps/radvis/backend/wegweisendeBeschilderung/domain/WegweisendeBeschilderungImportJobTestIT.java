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

package de.wps.radvis.backend.wegweisendeBeschilderung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.data.util.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.SimpleFeatureTypeFactory;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.wegweisendeBeschilderung.WegweisendeBeschilderungConfiguration;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.repository.WegweisendeBeschilderungRepository;

@Tag("group7")
@ContextConfiguration(classes = {
	CommonConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	OrganisationConfiguration.class,
	WegweisendeBeschilderungConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
public class WegweisendeBeschilderungImportJobTestIT extends AuditingTestIT {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@MockBean
	private GeoJsonImportRepository geoJsonImportRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Autowired
	private WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository;

	private final SimpleFeatureType simpleFeatureType = SimpleFeatureTypeFactory.createSimpleFeatureType(
		Set.of("PfostenNr", "WWTyp_Tx", "PfTyp_Tx", "GesZus", "GesMangel", "PfZus", "PfMangel", "GE_Gem", "GE_Kreis",
			"GE_Land"
		),
		Point.class,
		SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_GEOMETRY);

	private WegweisendeBeschilderungImportJob job;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
				.organisationsArt(OrganisationsArt.BUNDESLAND).build());

		job = new WegweisendeBeschilderungImportJob(
			"http://url-wird-ignoriert.de",
			geoJsonImportRepository,
			wegweisendeBeschilderungRepository,
			Lazy.of(() -> gebietskoerperschaftRepository.findByNameAndOrganisationsArt("Baden-Württemberg",
				OrganisationsArt.BUNDESLAND).orElseThrow()),
			jobExecutionDescriptionRepository);
	}

	@Test
	void run_threeImports_viewHatEintraegeFuerAlleJobsUndAlleRevtypesUndGeometrienStimmen()
		throws ReadGeoJSONException {
		// arrange und act 1 - der aller erste Pfosten
		JobExecutionDescription jobExecutionDescriptionOneNew = run(
			createFeature("Pfosten 1", new Coordinate(1, 1)));

		// arrange und act 2 - der erste Pfosten wurde neu verortet und es gibt einen zweiten Pfosten
		JobExecutionDescription jobExecutionDescriptionUpdateAndNew = run(
			createFeature("Pfosten 1", new Coordinate(42, 1)),
			createFeature("Pfosten 2", new Coordinate(2, 2))
		);

		// arrange und act 3 - der erste Pfosten wurde entfernt, der zweite Pfosten wurde nicht veraendert
		JobExecutionDescription jobExecutionDescriptionDelete = run(
			// Pfosten 1 ist weg
			// Pfosten 2 ist unveraendert
			createFeature("Pfosten 2", new Coordinate(2, 2))
		);

		// assert 1
		List<Map<String, Object>> import1 = jdbcTemplate.queryForList(
			"SELECT job_id, revtype, st_astext(geometrie) as geometrie FROM geoserver_wegweisende_beschilderung_diff WHERE job_id = "
				+ jobExecutionDescriptionOneNew.getId());
		assertThat(import1).hasSize(1);
		assertThat(import1.get(0).get("revtype")).isEqualTo(RevisionType.ADD.ordinal());
		assertThat(import1.get(0).get("geometrie")).isEqualTo("POINT(1 1)");

		// assert 2
		List<Map<String, Object>> import2 = jdbcTemplate.queryForList(
			"SELECT job_id, revtype, st_astext(geometrie) as geometrie FROM geoserver_wegweisende_beschilderung_diff WHERE job_id = "
				+ jobExecutionDescriptionUpdateAndNew.getId());
		assertThat(import2).hasSize(2);
		assertThat(import2.get(0).get("revtype")).isEqualTo(RevisionType.MOD.ordinal());
		assertThat(import2.get(0).get("geometrie")).isEqualTo("POINT(42 1)");
		assertThat(import2.get(1).get("revtype")).isEqualTo(RevisionType.ADD.ordinal());
		assertThat(import2.get(1).get("geometrie")).isEqualTo("POINT(2 2)");

		// assert 2
		List<Map<String, Object>> import3 = jdbcTemplate.queryForList(
			"SELECT job_id, revtype, st_astext(geometrie) as geometrie FROM geoserver_wegweisende_beschilderung_diff WHERE job_id = "
				+ jobExecutionDescriptionDelete.getId());
		assertThat(import3).hasSize(1);
		assertThat(import3.get(0).get("revtype")).isEqualTo(RevisionType.DEL.ordinal());
		assertThat(import3.get(0).get("geometrie")).isEqualTo("POINT(42 1)");
	}

	private <E> JobExecutionDescription run(SimpleFeature... features) throws ReadGeoJSONException {
		when(geoJsonImportRepository.getSimpleFeatures(any())).thenReturn(List.of(features));

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.executeWithoutResult(status -> {
			AdditionalRevInfoHolder.setAuditingContext(AuditingContext.WEGWEISENDE_BESCHILDERUNGEN_JOB);
			job.run();
		});
		JobExecutionDescription jobExecutionDescription = AdditionalRevInfoHolder.getJobExecutionDescription();
		AdditionalRevInfoHolder.clear();
		return jobExecutionDescription;
	}

	private SimpleFeature createFeature(String pfostenNr, Coordinate coordinate) {
		SimpleFeatureBuilder feature = new SimpleFeatureBuilder(simpleFeatureType);
		feature.add(GeometryTestdataProvider.createPoint(coordinate));
		feature.set("PfostenNr", pfostenNr);
		feature.set("WWTyp_Tx", "");
		feature.set("PfTyp_Tx", "");
		feature.set("GesZus", "");
		feature.set("GesMangel", "");
		feature.set("PfZus", "");
		feature.set("PfMangel", "");
		feature.set("GE_Gem", "");
		feature.set("GE_Kreis", "");
		feature.set("GE_Land", "");
		return feature.buildFeature(pfostenNr);
	}
}
