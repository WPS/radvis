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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.dlm.domain.entity.VernetzungKorrekturJobStatistik;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import jakarta.persistence.EntityManager;

@Tag("group4")
@ContextConfiguration(classes = { NetzConfiguration.class, OrganisationConfiguration.class, BenutzerConfiguration.class,
	GeoConverterConfiguration.class, CommonConfiguration.class })
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	FeatureToggleProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@RecordApplicationEvents
public class VernetzungKorrekturJobTestIT extends DBIntegrationTestIT {
	private VernetzungKorrekturJob job;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private KnotenRepository knotenRepository;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private NetzService netzService;
	@Autowired
	private ApplicationEvents applicationEvents;

	@BeforeEach
	void setup() {
		job = new VernetzungKorrekturJob(jobExecutionDescriptionRepository, kantenRepository, knotenRepository,
			entityManager, new Envelope(0, 2000, 0, 2000), 20,
			new VernetzungService(kantenRepository, knotenRepository, netzService), netzService);
	}

	@AfterEach
	void cleanup() {
		AdditionalRevInfoHolder.clear();
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_1kante_vernetzungKorrekt_verschobenerKnoten() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM).build());
		Knoten nachKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 100), QuellSystem.DLM).build());

		Kante kante = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(50, 50), new Coordinate(100, 100)))
			.build());

		entityManager.flush();
		entityManager.clear();

		job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante updatedKante = kantenRepository.findById(kante.getId()).get();

		assertThat(updatedKante.getVonKnoten()).isEqualTo(vonKnoten);
		assertThat(updatedKante.getNachKnoten()).isEqualTo(nachKnoten);
		assertThat(updatedKante.getGeometry()).isEqualTo(kante.getGeometry());
		assertThat(updatedKante.getVonKnoten().getPoint()).isEqualTo(kante.getGeometry().getStartPoint());
		assertThat(updatedKante.getNachKnoten().getPoint()).isEqualTo(kante.getGeometry().getEndPoint());
		assertThat(knotenRepository.count()).isEqualTo(2);
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_2kanten_korrekturVonKnotenGeometrie_vernetzungGleich_verschoben() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM).build());
		Knoten nachKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 100), QuellSystem.DLM).build());
		Knoten nachKnoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 1), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(15, 15), nachKnoten.getKoordinate()))
			.build());
		Kante dlmKante2 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten2)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(15, 15), nachKnoten2.getKoordinate()))
			.build());

		entityManager.flush();
		entityManager.clear();

		Optional<JobStatistik> result = job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante updatedDlmKante1 = kantenRepository.findById(dlmKante1.getId()).get();
		Kante updatedDlmKante2 = kantenRepository.findById(dlmKante2.getId()).get();

		assertThat(updatedDlmKante1.getVonKnoten()).isEqualTo(dlmKante1.getVonKnoten());
		assertThat(updatedDlmKante1.getNachKnoten()).isEqualTo(dlmKante1.getNachKnoten());

		assertThat(updatedDlmKante2.getVonKnoten()).isEqualTo(dlmKante2.getVonKnoten());
		assertThat(updatedDlmKante2.getNachKnoten()).isEqualTo(dlmKante2.getNachKnoten());

		assertThat(updatedDlmKante1.getVonKnoten()).isEqualTo(updatedDlmKante2.getVonKnoten());

		assertThat(knotenRepository.findAll()).containsExactlyInAnyOrder(updatedDlmKante1.getVonKnoten(),
			updatedDlmKante1.getNachKnoten(), updatedDlmKante2.getNachKnoten());
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_2kanten_knotenVerschieben_KantenGeometrieTreffenSichNichtGanz_unterToleranz_centroid() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM).build());
		Knoten nachKnoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 100), QuellSystem.DLM).build());
		Knoten nachKnoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 1), QuellSystem.DLM).build());

		Kante kante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten1)
			.build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten2)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(1.8, 1), nachKnoten2.getKoordinate()))
			.build());

		entityManager.flush();
		entityManager.clear();

		Optional<JobStatistik> jobStatistik = job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante updatedKante1 = kantenRepository.findById(kante1.getId()).get();
		Kante updatedKante2 = kantenRepository.findById(kante2.getId()).get();

		assertThat(updatedKante1.getVonKnoten()).isEqualTo(updatedKante2.getVonKnoten());

		Point centroidVonStart1Start2 = GeometryTestdataProvider.createMultiPoint(
			kante1.getGeometry().getStartPoint().getCoordinate(),
			kante2.getGeometry().getStartPoint().getCoordinate()).getCentroid();

		assertThat(updatedKante1.getGeometry()).isEqualTo(kante1.getGeometry());
		assertThat(updatedKante1.getVonKnoten().getPoint().getCoordinate()).usingComparatorForType(
			GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR, Coordinate.class)
			.isEqualTo(centroidVonStart1Start2.getCoordinate());
		assertThat(updatedKante1.getNachKnoten().getPoint()).isEqualTo(kante1.getGeometry().getEndPoint());

		assertThat(updatedKante2.getGeometry()).isEqualTo(kante2.getGeometry());
		assertThat(updatedKante2.getVonKnoten().getPoint().getCoordinate()).usingComparatorForType(
			GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR, Coordinate.class)
			.isEqualTo(centroidVonStart1Start2.getCoordinate());
		assertThat(updatedKante2.getNachKnoten().getPoint()).isEqualTo(kante2.getGeometry().getEndPoint());

		assertThat(knotenRepository.count()).isEqualTo(3);

		VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik = (VernetzungKorrekturJobStatistik) jobStatistik
			.get();
		assertThat(vernetzungKorrekturJobStatistik.anzahlKnotenNeuAngelegt).isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_2kanten_knotenVerschieben_KantenGeometrieTreffenSichNichtGanz_ueberToleranz_neuerKnoten() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM).build());
		Knoten nachKnoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 100), QuellSystem.DLM).build());
		Knoten nachKnoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 1), QuellSystem.DLM).build());

		Kante kante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten1)
			.build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten2)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(2.1, 1), nachKnoten2.getKoordinate()))
			.build());

		entityManager.flush();
		entityManager.clear();

		Optional<JobStatistik> jobStatistik = job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante updatedKante1 = kantenRepository.findById(kante1.getId()).get();
		Kante updatedKante2 = kantenRepository.findById(kante2.getId()).get();

		assertThat(updatedKante1.getVonKnoten()).isNotEqualTo(updatedKante2.getVonKnoten());

		assertThat(updatedKante1.getGeometry()).isEqualTo(kante1.getGeometry());
		assertThat(updatedKante1.getVonKnoten().getKoordinate()).isEqualTo(vonKnoten.getKoordinate());
		assertThat(updatedKante1.getNachKnoten().getPoint()).isEqualTo(kante1.getGeometry().getEndPoint());

		assertThat(updatedKante2.getGeometry()).isEqualTo(
			GeometryTestdataProvider.createLineString(new Coordinate(2.1, 1), nachKnoten2.getKoordinate()));
		assertThat(updatedKante2.getVonKnoten().getPoint().getCoordinate()).isEqualTo(new Coordinate(2.1, 1));
		assertThat(updatedKante2.getNachKnoten().getPoint()).isEqualTo(kante2.getGeometry().getEndPoint());

		assertThat(knotenRepository.count()).isEqualTo(4);
		assertThat(knotenRepository.findAll()).containsExactlyInAnyOrder(nachKnoten1,
			nachKnoten2, vonKnoten, updatedKante2.getVonKnoten());

		VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik = (VernetzungKorrekturJobStatistik) jobStatistik
			.get();
		assertThat(vernetzungKorrekturJobStatistik.anzahlKnotenNeuAngelegt).isEqualTo(1);
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_korrekturRadVisKante_vonKorrektur() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM).build());
		Knoten nachKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 100), QuellSystem.DLM).build());
		Knoten nachKnoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 1), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(15, 15), nachKnoten.getKoordinate()))
			.build());
		Kante dlmKante2 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten2)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(15, 15), nachKnoten2.getKoordinate()))
			.build());
		Kante radvisKante = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(vonKnoten, nachKnoten2, QuellSystem.RadVis)
				.build());

		entityManager.flush();
		entityManager.clear();

		Optional<JobStatistik> result = job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante updatedDlmKante1 = kantenRepository.findById(dlmKante1.getId()).get();
		Kante updatedDlmKante2 = kantenRepository.findById(dlmKante2.getId()).get();
		Kante updatedRadvisKante = kantenRepository.findById(radvisKante.getId()).get();

		assertThat(updatedDlmKante1.getVonKnoten()).isEqualTo(dlmKante1.getVonKnoten());
		assertThat(updatedDlmKante1.getNachKnoten()).isEqualTo(dlmKante1.getNachKnoten());

		assertThat(updatedDlmKante2.getVonKnoten()).isEqualTo(dlmKante2.getVonKnoten());
		assertThat(updatedDlmKante2.getNachKnoten()).isEqualTo(dlmKante2.getNachKnoten());

		assertThat(updatedDlmKante1.getVonKnoten()).isEqualTo(updatedDlmKante2.getVonKnoten());
		assertThat(updatedRadvisKante.getVonKnoten()).isEqualTo(updatedDlmKante2.getVonKnoten());
		assertThat(updatedRadvisKante.getNachKnoten()).isEqualTo(updatedDlmKante2.getNachKnoten());

		assertThat(updatedRadvisKante.getGeometry())
			.isEqualTo(GeometryTestdataProvider.createLineString(new Coordinate(15, 15), new Coordinate(1, 1),
				new Coordinate(100, 1)));

		assertThat(knotenRepository.findAll()).containsExactlyInAnyOrder(updatedDlmKante1.getVonKnoten(),
			updatedDlmKante1.getNachKnoten(), updatedDlmKante2.getNachKnoten());

		VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik = (VernetzungKorrekturJobStatistik) result
			.get();
		assertThat(vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlKnotenBehalten).isEqualTo(0);
		assertThat(vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenBetrachtet)
			.isEqualTo(1);
		assertThat(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenGeometrieKorrigiert)
				.isEqualTo(1);
		assertThat(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenVerwaistOderMitAnderenRadVisKantenVerbunden)
				.isEqualTo(0);

		assertThat(applicationEvents.stream(KnotenDeletedEvent.class).collect(Collectors.toList()))
			.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_korrekturRadVisKante_nachKorrektur() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM).build());
		Knoten nachKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 100), QuellSystem.DLM).build());
		Knoten nachKnoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 1), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(15, 15), nachKnoten.getKoordinate()))
			.build());
		Kante dlmKante2 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten2)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(15, 15), nachKnoten2.getKoordinate()))
			.build());
		Kante radvisKante = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(nachKnoten2, vonKnoten, QuellSystem.RadVis)
				.build());

		entityManager.flush();
		entityManager.clear();

		Optional<JobStatistik> result = job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante updatedDlmKante1 = kantenRepository.findById(dlmKante1.getId()).get();
		Kante updatedDlmKante2 = kantenRepository.findById(dlmKante2.getId()).get();
		Kante updatedRadvisKante = kantenRepository.findById(radvisKante.getId()).get();

		assertThat(updatedDlmKante1.getVonKnoten()).isEqualTo(dlmKante1.getVonKnoten());
		assertThat(updatedDlmKante1.getNachKnoten()).isEqualTo(dlmKante1.getNachKnoten());

		assertThat(updatedDlmKante2.getVonKnoten()).isEqualTo(dlmKante2.getVonKnoten());
		assertThat(updatedDlmKante2.getNachKnoten()).isEqualTo(dlmKante2.getNachKnoten());

		assertThat(updatedDlmKante1.getVonKnoten()).isEqualTo(updatedDlmKante2.getVonKnoten());
		assertThat(updatedRadvisKante.getVonKnoten()).isEqualTo(updatedDlmKante2.getNachKnoten());
		assertThat(updatedRadvisKante.getNachKnoten()).isEqualTo(updatedDlmKante2.getVonKnoten());

		assertThat(updatedRadvisKante.getGeometry())
			.isEqualTo(GeometryTestdataProvider.createLineString(new Coordinate(100, 1), new Coordinate(1, 1),
				new Coordinate(15, 15)));
		assertThat(knotenRepository.count()).isEqualTo(3);

		VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik = (VernetzungKorrekturJobStatistik) result
			.get();
		assertThat(vernetzungKorrekturJobStatistik.anzahlKnotenGeloescht).isEqualTo(0);
		assertThat(vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlKnotenBehalten).isEqualTo(0);
		assertThat(vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenBetrachtet)
			.isEqualTo(1);
		assertThat(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenGeometrieKorrigiert)
				.isEqualTo(1);
		assertThat(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenVerwaistOderMitAnderenRadVisKantenVerbunden)
				.isEqualTo(0);
	}

	// Dieser Fall kann im Grunde nur entstanden sein, wenn durch den DLM-Reimport der Knoten mehrfach innerhalb der
	// Toleranz
	// (1m) verschoben wurde
	// und anschließend gelöscht etc., dann endet/beginnt die RadVIS-Kante nicht mehr auf dem Knoten, wird aber auch
	// nicht neu vernetzt
	@SuppressWarnings("unchecked")
	@Test
	void run_korrekturRadVisKante_korrekturGeometrie_KnotenVerschoben() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM).build());
		Knoten nachKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1000), QuellSystem.DLM).build());
		Knoten nachKnoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1000, 1), QuellSystem.DLM).build());

		Coordinate wrongRadvisVonKoordinate = new Coordinate(5, 1);
		Coordinate richtigeDlmVonKoordinate = new Coordinate(10, 1);
		Kante dlmKante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.geometry(GeometryTestdataProvider.createLineString(
				richtigeDlmVonKoordinate,
				nachKnoten.getKoordinate()))
			.build());
		Kante dlmKante2 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten2)
			.geometry(GeometryTestdataProvider.createLineString(
				richtigeDlmVonKoordinate,
				nachKnoten2.getKoordinate()))
			.build());
		Kante radvisKante = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(vonKnoten, nachKnoten2, QuellSystem.RadVis)
				.geometry(
					GeometryTestdataProvider.createLineString(wrongRadvisVonKoordinate, nachKnoten2.getKoordinate()))
				.build());

		entityManager.flush();
		entityManager.clear();

		Optional<JobStatistik> result = job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante updatedDlmKante1 = kantenRepository.findById(dlmKante1.getId()).get();
		Kante updatedDlmKante2 = kantenRepository.findById(dlmKante2.getId()).get();
		Kante updatedRadvisKante = kantenRepository.findById(radvisKante.getId()).get();

		assertThat(updatedDlmKante1.getVonKnoten()).isEqualTo(dlmKante1.getVonKnoten());
		assertThat(updatedDlmKante1.getNachKnoten()).isEqualTo(dlmKante1.getNachKnoten());

		assertThat(updatedDlmKante2.getVonKnoten()).isEqualTo(dlmKante2.getVonKnoten());
		assertThat(updatedDlmKante2.getNachKnoten()).isEqualTo(dlmKante2.getNachKnoten());

		assertThat(updatedRadvisKante.getVonKnoten()).isEqualTo(radvisKante.getVonKnoten());
		assertThat(updatedRadvisKante.getNachKnoten()).isEqualTo(updatedDlmKante2.getNachKnoten());

		assertThat(updatedDlmKante1.getVonKnoten()).isEqualTo(updatedDlmKante2.getVonKnoten());

		Knoten updatedVonKnoten = knotenRepository.findById(vonKnoten.getId()).get();
		assertThat(updatedVonKnoten.getKoordinate()).isEqualTo(richtigeDlmVonKoordinate);

		assertThat(updatedRadvisKante.getGeometry())
			.isEqualTo(GeometryTestdataProvider.createLineString(updatedVonKnoten.getKoordinate(),
				wrongRadvisVonKoordinate,
				nachKnoten2.getKoordinate()));

		assertThat(knotenRepository.findAll()).containsExactlyInAnyOrder(updatedDlmKante1.getVonKnoten(),
			updatedDlmKante1.getNachKnoten(), updatedDlmKante2.getNachKnoten());

		VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik = (VernetzungKorrekturJobStatistik) result
			.get();
		assertThat(vernetzungKorrekturJobStatistik.anzahlKnotenGeloescht).isEqualTo(0);
		assertThat(vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenBetrachtet)
			.isEqualTo(1);
		assertThat(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenGeometrieKorrigiert)
				.isEqualTo(1);
		assertThat(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenVerwaistOderMitAnderenRadVisKantenVerbunden)
				.isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_korrekturRadVisKante_radvisKuerzerDlm() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 1), QuellSystem.DLM).build());
		Knoten nachKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 1), QuellSystem.DLM).build());
		Kante dlmKante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.geometry(GeometryTestdataProvider.createLineString(
				new Coordinate(15, 1), new Coordinate(65, 1)))
			.build());
		Kante radvisKante = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(vonKnoten, nachKnoten, QuellSystem.RadVis)
				.build());

		entityManager.flush();
		entityManager.clear();

		job.run();

		entityManager.flush();
		entityManager.clear();

		Kante updatedDlmKante1 = kantenRepository.findById(dlmKante1.getId()).get();
		Kante updatedRadvisKante = kantenRepository.findById(radvisKante.getId()).get();

		assertThat(updatedRadvisKante.getVonKnoten()).isEqualTo(updatedDlmKante1.getVonKnoten());
		assertThat(updatedRadvisKante.getNachKnoten()).isEqualTo(updatedDlmKante1.getNachKnoten());

		assertThat(updatedRadvisKante.getGeometry())
			.isEqualTo(GeometryTestdataProvider.createLineString(
				new Coordinate(15, 1), new Coordinate(30, 1), new Coordinate(50, 1), new Coordinate(65, 1)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_korrekturRadVisKante_dlmKuerzerRadvis() {
		Knoten vonKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(15, 1), QuellSystem.DLM).build());
		Knoten nachKnoten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(65, 1), QuellSystem.DLM).build());
		Kante dlmKante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.geometry(GeometryTestdataProvider.createLineString(
				new Coordinate(30, 1), new Coordinate(50, 1)))
			.build());
		Kante radvisKante = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(vonKnoten, nachKnoten, QuellSystem.RadVis)
				.build());

		entityManager.flush();
		entityManager.clear();

		job.run();

		entityManager.flush();
		entityManager.clear();

		Kante updatedDlmKante1 = kantenRepository.findById(dlmKante1.getId()).get();
		Kante updatedRadvisKante = kantenRepository.findById(radvisKante.getId()).get();

		assertThat(updatedRadvisKante.getVonKnoten()).isEqualTo(updatedDlmKante1.getVonKnoten());
		assertThat(updatedRadvisKante.getNachKnoten()).isEqualTo(updatedDlmKante1.getNachKnoten());

		assertThat(updatedRadvisKante.getGeometry())
			.isEqualTo(GeometryTestdataProvider.createLineString(
				new Coordinate(30, 1), new Coordinate(15, 1), new Coordinate(65, 1), new Coordinate(50, 1)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_korrekturRadVisKante_findBestMatch() {
		Knoten knoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 1), QuellSystem.DLM).build());
		Knoten knoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 100), QuellSystem.DLM).build());
		Knoten knoten3 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 1), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM)
				.build());
		Kante dlmKante2 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.DLM)
				.build());
		Kante radvisKante = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten3, QuellSystem.RadVis)
				.build());

		entityManager.flush();
		entityManager.clear();

		Optional<JobStatistik> result = job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante updatedDlmKante1 = kantenRepository.findById(dlmKante1.getId()).get();
		Kante updatedDlmKante2 = kantenRepository.findById(dlmKante2.getId()).get();
		Kante updatedRadvisKante = kantenRepository.findById(radvisKante.getId()).get();

		assertThat(updatedRadvisKante.getVonKnoten()).isEqualTo(updatedDlmKante1.getVonKnoten());
		assertThat(updatedRadvisKante.getNachKnoten()).isEqualTo(updatedDlmKante2.getNachKnoten());
		assertThat(updatedRadvisKante.getGeometry()).isEqualTo(radvisKante.getGeometry());

		VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik = (VernetzungKorrekturJobStatistik) result
			.get();
		assertThat(vernetzungKorrekturJobStatistik.anzahlKnotenGeloescht).isEqualTo(0);
		assertThat(vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlKnotenBehalten).isEqualTo(0);
		assertThat(vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenBetrachtet)
			.isEqualTo(1);
		assertThat(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenGeometrieKorrigiert)
				.isEqualTo(0);
		assertThat(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenVerwaistOderMitAnderenRadVisKantenVerbunden)
				.isEqualTo(0);
	}

	@Test
	void test_entferneFalscheVernetzung() {
		Knoten knotenMitFalscherVernertung = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).build());

		Knoten knoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build());

		Knoten knoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM).build());

		Knoten knoten3 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knotenMitFalscherVernertung, knoten1, QuellSystem.DLM)
				.build());
		Kante dlmKante2LeichtOff = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knotenMitFalscherVernertung, QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(knoten2.getKoordinate(), new Coordinate(99.1, 100)))
				.build());
		Kante dlmKante3FalschVernetzt = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten3, knotenMitFalscherVernertung, QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(knoten3.getKoordinate(), new Coordinate(98, 100)))
				.build());

		entityManager.flush();
		entityManager.clear();

		VernetzungKorrekturJobStatistik statistik = new VernetzungKorrekturJobStatistik();
		job.entferneFalscheVernetzung(statistik);

		entityManager.flush();
		entityManager.clear();

		assertThat(statistik.anzahlKnotenMitAuseinanderLiegendenKantenEndenVorEntfernungFalscherVernetzung).isEqualTo(
			1);
		assertThat(statistik.anzahlFalscheVernetzungEntfernt).isEqualTo(1);

		Kante kante1 = kantenRepository.findById(dlmKante1.getId()).get();
		Knoten vonKnotenKante1 = kante1.getVonKnoten();
		assertThat(vonKnotenKante1).isEqualTo(knotenMitFalscherVernertung);
		assertThat(vonKnotenKante1.getKoordinate()).isEqualTo(new Coordinate(100, 100));
		assertThat(kante1.getNachKnoten()).isEqualTo(dlmKante1.getNachKnoten());
		assertThat(kante1.getNachKnoten().getKoordinate()).isEqualTo(dlmKante1.getNachKnoten().getKoordinate());

		Knoten nachKnotenKante2LeichtOff = kantenRepository.findById(dlmKante2LeichtOff.getId()).get().getNachKnoten();
		assertThat(nachKnotenKante2LeichtOff).isEqualTo(knotenMitFalscherVernertung);
		assertThat(nachKnotenKante2LeichtOff.getKoordinate()).isEqualTo(new Coordinate(100, 100));

		Knoten nachKnotenKante3FalschVernetzt = kantenRepository.findById(dlmKante3FalschVernetzt.getId()).get()
			.getNachKnoten();
		assertThat(nachKnotenKante3FalschVernetzt).isNotEqualTo(knotenMitFalscherVernertung);
		assertThat(nachKnotenKante3FalschVernetzt.getKoordinate()).isEqualTo(new Coordinate(98, 100));
	}

	@Test
	void test_mergeDuplicatKnoten() {
		Knoten knoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).build());

		Knoten knoten1a = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(99.1, 100), QuellSystem.DLM).build());

		Knoten knoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM).build());

		Knoten knoten3 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).build());

		Knoten knoten4 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten1, QuellSystem.DLM)
				.build());
		Kante dlmKante2 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1a, knoten3, QuellSystem.DLM)
				.build());
		Kante dlmKante3 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten4, QuellSystem.DLM)
				.build());

		entityManager.flush();
		entityManager.clear();

		job.mergeDuplicateKnoten(new VernetzungKorrekturJobStatistik());

		entityManager.flush();
		entityManager.clear();

		Kante kante1 = kantenRepository.findById(dlmKante1.getId()).get();
		assertThat(kante1.getVonKnoten()).isEqualTo(knoten2);
		assertThat(kante1.getNachKnoten()).isEqualTo(knoten1);

		Kante kante2 = kantenRepository.findById(dlmKante2.getId()).get();
		assertThat(kante2.getVonKnoten()).isEqualTo(knoten1);
		assertThat(kante2.getNachKnoten()).isEqualTo(knoten3);

		Kante kante3 = kantenRepository.findById(dlmKante3.getId()).get();
		assertThat(kante3.getVonKnoten()).isEqualTo(knoten1);
		assertThat(kante3.getNachKnoten()).isEqualTo(knoten4);
	}

	@Test
	void test_dorRun_mergeDuplicatKnoten() {
		Knoten knoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).build());

		Knoten knoten1a = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(99.1, 100), QuellSystem.DLM).build());

		Knoten knoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM).build());

		Knoten knoten3 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).build());

		Knoten knoten4 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200), QuellSystem.DLM).build());

		// NachKnoten der dlmKante1 wird vor dem Merge auf knoten1a geändert, da das LineString-Ende näher an ihm liegt
		Kante dlmKante1 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten1, QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(100, 0), new Coordinate(99.4, 100)))
				.build());
		Kante dlmKante2 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1a, knoten3, QuellSystem.DLM)
				.build());
		// VonKnoten der dlmKante3 wird beim mergen der Knoten auf knoten1a geändert, da knoten1 nach der Korrektur der
		// Dlm-Vernetzung weniger adjazente Kanten hat als knoten1a
		Kante dlmKante3 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten4, QuellSystem.DLM)
				.build());

		entityManager.flush();
		entityManager.clear();

		job.doRun();

		entityManager.flush();
		entityManager.clear();

		Kante kante1 = kantenRepository.findById(dlmKante1.getId()).get();
		assertThat(kante1.getVonKnoten()).isEqualTo(knoten2);
		assertThat(kante1.getNachKnoten()).isEqualTo(knoten1a);

		Kante kante2 = kantenRepository.findById(dlmKante2.getId()).get();
		assertThat(kante2.getVonKnoten()).isEqualTo(knoten1a);
		assertThat(kante2.getNachKnoten()).isEqualTo(knoten3);

		Kante kante3 = kantenRepository.findById(dlmKante3.getId()).get();
		assertThat(kante3.getVonKnoten()).isEqualTo(knoten1a);
		assertThat(kante3.getNachKnoten()).isEqualTo(knoten4);

		assertThat(knotenRepository.findById(knoten1.getId())).isEmpty();
	}

	@Test
	void test_mergeDuplicatKnoten_keinMergeWennDadurchLoopKanteEntsteht() {
		Knoten knoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).build());

		Knoten knoten1a = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(99.1, 100), QuellSystem.DLM).build());

		Knoten knoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten1, QuellSystem.DLM)
				.build());
		Kante dlmKante2 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1a, knoten1, QuellSystem.DLM)
				.build());

		entityManager.flush();
		entityManager.clear();

		job.mergeDuplicateKnoten(new VernetzungKorrekturJobStatistik());

		entityManager.flush();
		entityManager.clear();

		Kante kante1 = kantenRepository.findById(dlmKante1.getId()).get();
		assertThat(kante1.getVonKnoten()).isEqualTo(knoten2);
		assertThat(kante1.getNachKnoten()).isEqualTo(knoten1);

		Kante kante2 = kantenRepository.findById(dlmKante2.getId()).get();
		assertThat(kante2.getVonKnoten()).isEqualTo(knoten1a);
		assertThat(kante2.getNachKnoten()).isEqualTo(knoten1);
	}

	@Test
	void test_mergeDuplicatKnoten_keinMergeWennDadurchLoopKanteEntsteht_AuchNichtFuerAndere() {
		Knoten knoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).build());

		Knoten knoten1a = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(99.1, 100), QuellSystem.DLM).build());

		Knoten knoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM).build());

		Knoten knoten3 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten1, QuellSystem.DLM)
				.build());
		Kante dlmKante2 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1a, knoten1, QuellSystem.DLM)
				.build());
		Kante dlmKante3 = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten3, knoten1a, QuellSystem.DLM)
				.build());

		entityManager.flush();
		entityManager.clear();

		job.mergeDuplicateKnoten(new VernetzungKorrekturJobStatistik());

		entityManager.flush();
		entityManager.clear();

		Kante kante1 = kantenRepository.findById(dlmKante1.getId()).get();
		assertThat(kante1.getVonKnoten()).isEqualTo(knoten2);
		assertThat(kante1.getNachKnoten()).isEqualTo(knoten1);

		Kante kante2 = kantenRepository.findById(dlmKante2.getId()).get();
		assertThat(kante2.getVonKnoten()).isEqualTo(knoten1a);
		assertThat(kante2.getNachKnoten()).isEqualTo(knoten1);

		Kante kante3 = kantenRepository.findById(dlmKante3.getId()).get();
		assertThat(kante3.getVonKnoten()).isEqualTo(knoten3);
		assertThat(kante3.getNachKnoten()).isEqualTo(knoten1a);
	}

	@Test
	void test_entferneFalscheVernetzung_BeideEndenKorrigeren() {
		Knoten knoten1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build());

		Knoten knoten2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 3), QuellSystem.DLM).build());

		Kante dlmKante1_knoten2WeitWeg = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(knoten1.getKoordinate(), new Coordinate(100, 0)))
				.build());
		Kante dlmKante2_knoten1WeitWeg = kantenRepository
			.save(KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten1, QuellSystem.DLM)
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 3), knoten2.getKoordinate()))
				.build());

		entityManager.flush();
		entityManager.clear();

		VernetzungKorrekturJobStatistik statistik = (VernetzungKorrekturJobStatistik) job.doRun().get();

		entityManager.flush();
		entityManager.clear();

		assertThat(statistik.anzahlKnotenMitAuseinanderLiegendenKantenEndenVorEntfernungFalscherVernetzung).isEqualTo(
			2);
		assertThat(statistik.anzahlFalscheVernetzungEntfernt).isEqualTo(2);

		Kante kante1 = kantenRepository.findById(dlmKante1_knoten2WeitWeg.getId()).get();
		Kante kante2 = kantenRepository.findById(dlmKante2_knoten1WeitWeg.getId()).get();

		assertThat(knotenRepository.findAll()).hasSize(4);
		// Nicht mehr als Kreis verbunden
		assertThat(kante1.getVonKnoten()).isNotEqualTo(kante2.getNachKnoten());
		assertThat(kante1.getNachKnoten()).isNotEqualTo(kante2.getVonKnoten());
		// Enden liegen richtig:
		assertThat(kante1.getVonKnoten().getPoint().getCoordinate())
			.isEqualTo(kante1.getGeometry().getStartPoint().getCoordinate());
		assertThat(kante1.getNachKnoten().getPoint().getCoordinate())
			.isEqualTo(kante1.getGeometry().getEndPoint().getCoordinate());
		assertThat(kante2.getVonKnoten().getPoint().getCoordinate())
			.isEqualTo(kante2.getGeometry().getStartPoint().getCoordinate());
		assertThat(kante2.getNachKnoten().getPoint().getCoordinate())
			.isEqualTo(kante2.getGeometry().getEndPoint().getCoordinate());
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_2RadVisKanten_durchDlmKnotenVerbunden_grosserKnotenAbstand_nichtsAendern() {
		// arrange
		Knoten k1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build());
		Knoten k2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM).build());
		Knoten k3MitNurRadvisKanten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 0), QuellSystem.DLM).build());
		Knoten k4 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(300, 0), QuellSystem.DLM).build());
		Knoten k5 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400, 0), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(k1, k2)
			.build());
		Kante radVisKante1 = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(k2, k3MitNurRadvisKanten, QuellSystem.RadVis)
				.build());
		Kante radVisKante2 = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(k3MitNurRadvisKanten, k4, QuellSystem.RadVis)
				.build());
		Kante dlmKante2 = kantenRepository.save(KanteTestDataProvider.fromKnoten(k4, k5)
			.build());

		entityManager.flush();
		entityManager.clear();

		// act
		VernetzungKorrekturJobStatistik statistik = (VernetzungKorrekturJobStatistik) job.doRun().get();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(knotenRepository.count()).isEqualTo(5);
		assertThat(knotenRepository.findAll()).containsExactlyInAnyOrder(k1, k2, k3MitNurRadvisKanten, k4, k5);

		Kante updatedRadVisKante1 = kantenRepository.findById(radVisKante1.getId()).get();
		Kante updatedRadVisKante2 = kantenRepository.findById(radVisKante2.getId()).get();

		assertThat(updatedRadVisKante1.getVonKnoten()).isEqualTo(k2);
		assertThat(updatedRadVisKante1.getNachKnoten()).isEqualTo(k3MitNurRadvisKanten);
		assertThat(updatedRadVisKante2.getVonKnoten()).isEqualTo(k3MitNurRadvisKanten);
		assertThat(updatedRadVisKante2.getNachKnoten()).isEqualTo(k4);
		assertThat(updatedRadVisKante1.getGeometry()).isEqualTo(radVisKante1.getGeometry());
		assertThat(updatedRadVisKante2.getGeometry()).isEqualTo(radVisKante2.getGeometry());

		assertThat(statistik.radvisKantenVernetzungStatistik.anzahlKnotenGeloescht).isEqualTo(0);
		assertThat(statistik.radvisKantenVernetzungStatistik.anzahlKnotenBehalten).isEqualTo(2);
		assertThat(statistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenTopologieKorrigiert).isEqualTo(0);
		assertThat(statistik.anzahlKnotenGeometrieDurchEntferneFalscheVernetzungKorrigiert).isEqualTo(0);
		assertThat(statistik.anzahlKnotenGeloescht).isEqualTo(0);

		assertThat(statistik.anzahlKnotenMitNurRadvisKantenVorher).isEqualTo(1);
		assertThat(statistik.anzahlKnotenMitNurRadvisKantenNachher).isEqualTo(1);
	}

	@SuppressWarnings("unchecked")
	@Test
	void run_2RadVisKanten_durchDlmKnotenVerbunden_kleinerKnotenAbstand_snappenAberKeinenKnotenLoeschen() {
		// arrange
		Knoten k1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build());
		Knoten k2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(18.5, 0), QuellSystem.DLM).build());
		Knoten k3MitNurRadvisKanten = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 0), QuellSystem.DLM).build());
		Knoten k4 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 0), QuellSystem.DLM).build());
		Knoten k5 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(40, 0), QuellSystem.DLM).build());

		Kante dlmKante1 = kantenRepository.save(KanteTestDataProvider.fromKnoten(k1, k2)
			.build());
		Kante radVisKante1 = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(k2, k3MitNurRadvisKanten, QuellSystem.RadVis)
				.build());
		Kante radVisKante2 = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(k3MitNurRadvisKanten, k4, QuellSystem.RadVis)
				.build());
		Kante dlmKante2 = kantenRepository.save(KanteTestDataProvider.fromKnoten(k4, k5)
			.build());

		entityManager.flush();
		entityManager.clear();

		// act
		VernetzungKorrekturJobStatistik statistik = (VernetzungKorrekturJobStatistik) job.doRun().get();
		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(knotenRepository.count()).isEqualTo(5);
		assertThat(knotenRepository.findAll()).containsExactlyInAnyOrder(k1, k2, k3MitNurRadvisKanten, k4, k5);

		Kante updatedRadVisKante1 = kantenRepository.findById(radVisKante1.getId()).get();
		Kante updatedRadVisKante2 = kantenRepository.findById(radVisKante2.getId()).get();

		assertThat(updatedRadVisKante1.getVonKnoten()).isEqualTo(k2);
		assertThat(updatedRadVisKante1.getNachKnoten()).isEqualTo(k3MitNurRadvisKanten);
		assertThat(updatedRadVisKante2.getVonKnoten()).isEqualTo(k2);
		assertThat(updatedRadVisKante2.getNachKnoten()).isEqualTo(k4);

		assertThat(updatedRadVisKante1.getGeometry()).isEqualTo(radVisKante1.getGeometry());
		assertThat(updatedRadVisKante2.getGeometry()).isNotEqualTo(radVisKante2.getGeometry());

		assertThat(statistik.radvisKantenVernetzungStatistik.anzahlKnotenGeloescht).isEqualTo(0);
		assertThat(statistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenTopologieKorrigiert).isEqualTo(1);
		assertThat(statistik.radvisKantenVernetzungStatistik.anzahlRadVisKantenNichtVeraendertDaSonstLoop).isEqualTo(1);
		assertThat(statistik.anzahlKnotenGeometrieDurchEntferneFalscheVernetzungKorrigiert).isEqualTo(0);
		assertThat(statistik.anzahlKnotenGeloescht).isEqualTo(0);

		assertThat(statistik.anzahlKnotenMitNurRadvisKantenVorher).isEqualTo(1);
		assertThat(statistik.anzahlKnotenMitNurRadvisKantenNachher).isEqualTo(1);
	}
}
