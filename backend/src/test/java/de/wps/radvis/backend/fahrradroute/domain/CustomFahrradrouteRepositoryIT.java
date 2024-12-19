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

package de.wps.radvis.backend.fahrradroute.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescriptionTestDataProvider;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;

@Tag("group7")
@EnableEnversRepositories(basePackageClasses = { FahrradrouteConfiguration.class,
	NetzConfiguration.class })
@EntityScan(basePackageClasses = { FahrradrouteConfiguration.class,
	NetzConfiguration.class, OrganisationConfiguration.class, BenutzerConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class, CommonConfigurationProperties.class, NetzConfigurationProperties.class })
@ContextConfiguration(classes = { CommonConfiguration.class, GeoConverterConfiguration.class })
public class CustomFahrradrouteRepositoryIT extends AuditingTestIT {
	@Autowired
	CommonConfigurationProperties commonConfigurationProperties;
	@Autowired
	FahrradrouteRepository fahrradrouteRepository;
	@Autowired
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	KantenRepository kantenRepository;

	@Test
	public void diffViewCanBeCreatedAndIsEmptyAfterFirstReset() {
		new TransactionTemplate(transactionManager).executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		assertThat(
			jdbcTemplate.queryForList("SELECT * FROM geoserver_fahrradroute_import_diff_materialized_view"))
				.hasSize(0);
	}

	@Test
	public void doNotSelectInsertedOrDeleted() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		JobExecutionDescription jobExecutionDescription = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).name(FahrradrouteName.of("Fahrradroute Vorher"))
				.verantwortlich(null).netzbezugLineString(null)
				.originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		Fahrradroute fahrradrouteNachher = fahrradrouteVorher.toBuilder()
			.name(FahrradrouteName.of("Fahrradroute Nachher"))
			.netzbezugLineString(GeometryTestdataProvider.createLineString()).build();

		transactionTemplate.execute((status) -> fahrradrouteRepository.save(fahrradrouteNachher));

		transactionTemplate
			.executeWithoutResult((status) -> fahrradrouteRepository.deleteById(fahrradrouteVorher.getId()));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_fahrradroute_import_diff_materialized_view");
		List<Map<String, Object>> allAudEntries = jdbcTemplate
			.queryForList("SELECT * FROM fahrradroute_aud");

		List<String> findAllNamesOfGeloeschtByJobId = fahrradrouteRepository
			.findAllNamesOfDeletedByJobId(jobExecutionDescription.getId());
		List<String> findAllNamesOfInsertedByJobId = fahrradrouteRepository
			.findAllNamesOfInsertedByJobId(jobExecutionDescription.getId());

		assertThat(allAudEntries).hasSize(3);
		assertThat(allViewEntries).hasSize(1);

		assertThat(findAllNamesOfInsertedByJobId).hasSize(1);
		assertThat(findAllNamesOfInsertedByJobId.get(0)).isEqualTo(fahrradrouteVorher.getName().getName());

		assertThat(findAllNamesOfGeloeschtByJobId).hasSize(1);
		assertThat(findAllNamesOfGeloeschtByJobId.get(0)).isEqualTo(fahrradrouteNachher.getName().getName());
	}

	@Test
	public void noJobId_doNotSelect() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).verantwortlich(null).netzbezugLineString(null)
				.originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteNachher = fahrradrouteVorher.toBuilder()
			.netzbezugLineString(GeometryTestdataProvider.createLineString()).build();

		transactionTemplate.execute((status) -> fahrradrouteRepository.save(fahrradrouteNachher));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_fahrradroute_import_diff_materialized_view");

		assertThat(allViewEntries).isEmpty();
	}

	@Test
	public void updatedTwice_selectTwo() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).verantwortlich(null)
				.netzbezugLineString(GeometryTestdataProvider.createLineString())
				.beschreibung("Alte Version")
				.originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		JobExecutionDescription jobExecutionDescription = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteNachher = transactionTemplate
			.execute((status) -> fahrradrouteRepository.save(fahrradrouteVorher.toBuilder().beschreibung("Neue Version")
				.netzbezugLineString(GeometryTestdataProvider.createLineString()).build()));

		transactionTemplate
			.execute((status) -> fahrradrouteRepository
				.save(fahrradrouteNachher.toBuilder().netzbezugLineString(GeometryTestdataProvider.createLineString())
					.beschreibung("Neueste Version").build()));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList("SELECT * FROM geoserver_fahrradroute_import_diff_materialized_view");

		assertThat(allViewEntries).hasSize(2);
	}

	@Test
	public void hasCorrectProperties() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).verantwortlich(null).beschreibung("Alte Version")
				.netzbezugLineString(
					GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(0, 100)))
				.originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		JobExecutionDescription jobExecutionDescription = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteNachher = fahrradrouteVorher.toBuilder().beschreibung("Neue Version")
			.netzbezugLineString(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 0), new Coordinate(10, 100)))
			.build();

		transactionTemplate.execute((status) -> fahrradrouteRepository.save(fahrradrouteNachher));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList(
				"SELECT job_id, fahrradroute_id, ST_AsText(geometrie_vorher) as geometrie_vorher, ST_AsText(geometrie_diff) as geometrie_diff FROM geoserver_fahrradroute_import_diff_materialized_view");

		assertThat(allViewEntries).hasSize(1);
		assertThat((Long) allViewEntries.get(0).get("job_id"))
			.isEqualTo(jobExecutionDescription.getId());
		assertThat((Long) allViewEntries.get(0).get("fahrradroute_id"))
			.isEqualTo(fahrradrouteNachher.getId());
		assertThat((String) allViewEntries.get(0).get("geometrie_vorher"))
			.isEqualTo(asWkt((LineString) fahrradrouteVorher.getNetzbezugLineString().get()));
		assertThat((String) allViewEntries.get(0).get("geometrie_diff"))
			.isEqualTo(asWkt((LineString) fahrradrouteNachher.getNetzbezugLineString().get()));
	}

	@Test
	public void calculatesLineStringDiff() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).verantwortlich(null).beschreibung("Alte Version")
				.netzbezugLineString(
					GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(0, 100)))
				.originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		JobExecutionDescription jobExecutionDescription = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteNachher = fahrradrouteVorher.toBuilder().beschreibung("Neue Version")
			.netzbezugLineString(
				GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(0, 20),
					new Coordinate(20, 20), new Coordinate(20, 40), new Coordinate(0, 40), new Coordinate(0, 100)))
			.build();

		transactionTemplate.execute((status) -> fahrradrouteRepository.save(fahrradrouteNachher));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList(
				"SELECT job_id, fahrradroute_id, ST_AsText(geometrie_vorher) as geometrie_vorher, ST_AsText(geometrie_diff) as geometrie_diff FROM geoserver_fahrradroute_import_diff_materialized_view");

		assertThat(allViewEntries).hasSize(1);
		assertThat((Long) allViewEntries.get(0).get("job_id"))
			.isEqualTo(jobExecutionDescription.getId());
		assertThat((Long) allViewEntries.get(0).get("fahrradroute_id"))
			.isEqualTo(fahrradrouteNachher.getId());
		assertThat((String) allViewEntries.get(0).get("geometrie_vorher"))
			.isEqualTo(asWkt((LineString) fahrradrouteVorher.getNetzbezugLineString().get()));
		assertThat((String) allViewEntries.get(0).get("geometrie_diff"))
			.isEqualTo(asWkt(GeometryTestdataProvider.createLineString(new Coordinate(0, 20),
				new Coordinate(20, 20), new Coordinate(20, 40), new Coordinate(0, 40))));
	}

	@Test
	public void twoUpdates_selectsCorrectVorversion() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).verantwortlich(null).beschreibung("Alte Version")
				.netzbezugLineString(
					GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(0, 100)))
				.originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		JobExecutionDescription jobExecutionDescription = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteNachher = transactionTemplate.execute(
			(status) -> fahrradrouteRepository.save(fahrradrouteVorher.toBuilder().beschreibung("Neue Version")
				.netzbezugLineString(
					GeometryTestdataProvider.createLineString(new Coordinate(10, 0), new Coordinate(10, 100)))
				.build()));

		JobExecutionDescription jobExecutionDescription2 = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription2);

		Fahrradroute fahrradrouteNachher2 = transactionTemplate.execute(
			(status) -> fahrradrouteRepository.save(fahrradrouteNachher.toBuilder().beschreibung("Neueste Version")
				.netzbezugLineString(
					GeometryTestdataProvider.createLineString(new Coordinate(100, 0), new Coordinate(100, 100)))
				.build()));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList(
				"SELECT job_id, fahrradroute_id, ST_AsText(geometrie_vorher) as geometrie_vorher, ST_AsText(geometrie_diff) as geometrie_diff FROM geoserver_fahrradroute_import_diff_materialized_view");

		assertThat(allViewEntries).hasSize(2);

		assertThat((Long) allViewEntries.get(0).get("job_id"))
			.isEqualTo(jobExecutionDescription.getId());
		assertThat((Long) allViewEntries.get(0).get("fahrradroute_id"))
			.isEqualTo(fahrradrouteNachher.getId());
		assertThat((String) allViewEntries.get(0).get("geometrie_vorher"))
			.isEqualTo(asWkt((LineString) fahrradrouteVorher.getNetzbezugLineString().get()));
		assertThat((String) allViewEntries.get(0).get("geometrie_diff"))
			.isEqualTo(asWkt((LineString) fahrradrouteNachher.getNetzbezugLineString().get()));

		assertThat((Long) allViewEntries.get(1).get("job_id"))
			.isEqualTo(jobExecutionDescription2.getId());
		assertThat((Long) allViewEntries.get(1).get("fahrradroute_id"))
			.isEqualTo(fahrradrouteNachher.getId());
		assertThat((String) allViewEntries.get(1).get("geometrie_vorher"))
			.isEqualTo(asWkt((LineString) fahrradrouteNachher.getNetzbezugLineString().get()));
		assertThat((String) allViewEntries.get(1).get("geometrie_diff"))
			.isEqualTo(asWkt((LineString) fahrradrouteNachher2.getNetzbezugLineString().get()));
	}

	@Test
	public void noNetzbezugLineStringVorherAberNachher_hasEmptyGeometrieVorher_fullDiff() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).verantwortlich(null).beschreibung("Alte Version")
				.netzbezugLineString(null).originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		JobExecutionDescription jobExecutionDescription = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteNachher = fahrradrouteVorher.toBuilder().beschreibung("Neue Version")
			.netzbezugLineString(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 0), new Coordinate(10, 100)))
			.build();

		transactionTemplate.execute((status) -> fahrradrouteRepository.save(fahrradrouteNachher));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList(
				"SELECT job_id, fahrradroute_id, geometrie_vorher, ST_AsText(geometrie_diff) as geometrie_diff FROM geoserver_fahrradroute_import_diff_materialized_view");

		assertThat(allViewEntries).hasSize(1);
		assertThat((Long) allViewEntries.get(0).get("job_id"))
			.isEqualTo(jobExecutionDescription.getId());
		assertThat((Long) allViewEntries.get(0).get("fahrradroute_id"))
			.isEqualTo(fahrradrouteNachher.getId());
		assertThat(allViewEntries.get(0).get("geometrie_vorher")).isNull();
		assertThat((String) allViewEntries.get(0).get("geometrie_diff"))
			.isEqualTo(asWkt((LineString) fahrradrouteNachher.getNetzbezugLineString().get()));
	}

	@Test
	public void noNetzbezugLineStringVorherUndNachher_doesNotSelect() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).verantwortlich(null).beschreibung("Alte Version")
				.netzbezugLineString(null).originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		JobExecutionDescription jobExecutionDescription = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteNachher = fahrradrouteVorher.toBuilder().beschreibung("Neue Version")
			.netzbezugLineString(null).build();

		transactionTemplate.execute((status) -> fahrradrouteRepository.save(fahrradrouteNachher));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList(
				"SELECT job_id, fahrradroute_id, geometrie_vorher, ST_AsText(geometrie_diff) as geometrie_diff FROM geoserver_fahrradroute_import_diff_materialized_view");

		assertThat(allViewEntries).isEmpty();
	}

	@Test
	public void noNetzbezugLineStringNachherAberVorher_doesNotSelect() {
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_FAHRRADROUTE_COMMAND);
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Fahrradroute fahrradrouteVorher = transactionTemplate.execute((status) -> fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante).verantwortlich(null).beschreibung("Alte Version")
				.netzbezugLineString(null).originalGeometrie(GeometryTestdataProvider.createLineString()).build()));

		JobExecutionDescription jobExecutionDescription = transactionTemplate
			.execute((status) -> jobExecutionDescriptionRepository
				.save(JobExecutionDescriptionTestDataProvider.withDefaultValues().build()));
		AdditionalRevInfoHolder.setJobExecutionDescription(jobExecutionDescription);
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.FAHRRADROUTE_TOUBIZ_IMPORT_JOB);

		Fahrradroute fahrradrouteNachher = fahrradrouteVorher.toBuilder().beschreibung("Neue Version")
			.netzbezugLineString(null)
			.build();

		transactionTemplate.execute((status) -> fahrradrouteRepository.save(fahrradrouteNachher));

		transactionTemplate.executeWithoutResult(
			(status) -> fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(
				commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten()));

		List<Map<String, Object>> allViewEntries = jdbcTemplate
			.queryForList(
				"SELECT job_id, fahrradroute_id, geometrie_vorher, ST_AsText(geometrie_diff) as geometrie_diff FROM geoserver_fahrradroute_import_diff_materialized_view");

		assertThat(allViewEntries).isEmpty();
	}

	private String asWkt(LineString lineString) {
		return lineString.toString().replace(" (", "(").replace(", ", ",");
	}
}
