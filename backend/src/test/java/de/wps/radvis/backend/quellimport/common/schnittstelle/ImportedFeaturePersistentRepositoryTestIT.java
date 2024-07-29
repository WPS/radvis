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

package de.wps.radvis.backend.quellimport.common.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureBuilder;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureMapView;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group2")
@ContextConfiguration(classes = ImportedFeaturePersistentRepositoryTestIT.TestConfiguration.class)
public class ImportedFeaturePersistentRepositoryTestIT extends DBIntegrationTestIT {

	@Configuration
	@EnableJpaRepositories(basePackages = "de.wps.radvis.backend.quellimport.common")
	@EntityScan({ "de.wps.radvis.backend.quellimport.common.domain.entity",
		"de.wps.radvis.backend.quellimport.common.domain.valueObject",
		"de.wps.radvis.backend.common.domain.valueObject" })
	public static class TestConfiguration {
	}

	@Autowired
	private ImportedFeaturePersistentRepository repository;

	@PersistenceContext
	EntityManager entityManager;

	@SuppressWarnings("unchecked")
	@Test
	public void saveAndGet() {
		// Arrange
		assertThat(repository).isNotNull();

		ImportedFeature feature = ImportedFeatureBuilder.empty()
			.fachId("Some id")
			.addAttribut("Name", "Karsten")
			.addAttribut("Breite", 50)
			.lineString(new Coordinate(10, 20), new Coordinate(10, 30), new Coordinate(20, 30))
			.art(Art.Strecke)
			.quelle(QuellSystem.RadNETZ)
			.importDatum(LocalDateTime.of(2021, 3, 22, 15, 12))
			.build();

		Geometry originalGeometrie = feature.getGeometrie();

		// Act
		Long savedFeatureId = repository.save(feature).getId();

		entityManager.flush();
		entityManager.clear();

		Optional<ImportedFeature> result = repository.findById(savedFeatureId);

		// Assert
		assertThat(result).isPresent();
		ImportedFeature resultingFeature = result.get();
		assertThat(resultingFeature.getTechnischeId()).isEqualTo("Some id");
		assertThat(resultingFeature.getArt()).isEqualTo(Art.Strecke);
		assertThat(resultingFeature.getQuelle()).isEqualTo(QuellSystem.RadNETZ);
		assertThat(resultingFeature.getImportDatum()).isEqualTo(LocalDateTime.of(2021, 3, 22, 15, 12));
		assertThat(resultingFeature.getGeometrie()).isEqualTo(originalGeometrie);
		assertThat(resultingFeature.getAttribute()).hasSize(2);
		assertThat(resultingFeature.getAttribute()).contains(entry("Name", "Karsten"));
		assertThat(resultingFeature.getAttribute()).contains(entry("Breite", 50));
	}

	@Test
	public void getAllByQuelleAndArt() {
		// Arrange
		assertThat(repository).isNotNull();

		Coordinate[] coordinates = { new Coordinate(10, 20), new Coordinate(10, 30), new Coordinate(20, 30) };
		LocalDateTime date = LocalDateTime.of(2021, 3, 22, 15, 12);
		ImportedFeature featureRadNetzMassnahme = ImportedFeatureBuilder.empty()
			.art(Art.Massnahme)
			.quelle(QuellSystem.RadNETZ)
			.fachId("fach id")
			.importDatum(date)
			.lineString(coordinates)
			.build();

		ImportedFeature featureRadNetzStrecke = ImportedFeatureBuilder.empty()
			.art(Art.Strecke)
			.quelle(QuellSystem.RadNETZ)
			.fachId("fach id")
			.importDatum(date)
			.lineString(coordinates)
			.build();

		ImportedFeature featureGrundnetzStrecke = ImportedFeatureBuilder.empty()
			.art(Art.Strecke)
			.quelle(QuellSystem.DLM)
			.fachId("fach id")
			.importDatum(date)
			.lineString(coordinates)
			.build();

		// Act
		repository.save(featureRadNetzMassnahme);
		repository.save(featureRadNetzStrecke);
		repository.save(featureGrundnetzStrecke);

		entityManager.flush();
		entityManager.clear();

		List<ImportedFeature> result = repository.getAllByQuelleAndArt(QuellSystem.RadNETZ, Art.Strecke).collect(
			Collectors.toList());

		// Assert
		assertThat(result).isNotEmpty();
		assertThat(result).containsExactly(featureRadNetzStrecke);
	}

	@Test
	public void testGetAllByQuelleAndArtAndGeometryType() {
		// Arrange
		assertThat(repository).isNotNull();

		Coordinate[] coordinates = { new Coordinate(10, 20), new Coordinate(10, 30), new Coordinate(20, 30) };
		LocalDateTime date = LocalDateTime.of(2021, 3, 22, 15, 12);
		ImportedFeature featureRadNetzMassnahme = ImportedFeatureBuilder.empty()
			.art(Art.Massnahme)
			.quelle(QuellSystem.RadNETZ)
			.fachId("fach id")
			.importDatum(date)
			.lineString(coordinates)
			.build();

		ImportedFeature featureRadNetzStrecke = ImportedFeatureBuilder.empty()
			.art(Art.Strecke)
			.quelle(QuellSystem.RadNETZ)
			.fachId("fach id")
			.importDatum(date)
			.lineString(coordinates)
			.build();

		ImportedFeature featureGrundnetzStrecke = ImportedFeatureBuilder.empty()
			.art(Art.Strecke)
			.quelle(QuellSystem.DLM)
			.fachId("fach id")
			.importDatum(date)
			.lineString(coordinates)
			.build();

		ImportedFeature featureRadNetzPunkt = ImportedFeatureBuilder.empty()
			.art(Art.Strecke)
			.quelle(QuellSystem.DLM)
			.fachId("fach id")
			.importDatum(date)
			.point(coordinates[0])
			.build();

		// Act
		repository.save(featureRadNetzMassnahme);
		repository.save(featureRadNetzStrecke);
		repository.save(featureGrundnetzStrecke);
		repository.save(featureRadNetzPunkt);

		entityManager.flush();
		entityManager.clear();

		List<ImportedFeature> result = repository.getAllByQuelleAndArtAndGeometryType(QuellSystem.RadNETZ, Art.Strecke,
			ImportedFeaturePersistentRepository.LINESTRING).collect(
				Collectors.toList());

		// Assert
		assertThat(result).isNotEmpty();
		assertThat(result).containsExactly(featureRadNetzStrecke);
	}

	@Test
	public void getFeaturesInBereich() {
		// Arrange
		assertThat(repository).isNotNull();

		LocalDateTime date = LocalDateTime.of(2021, 3, 22, 15, 12);
		ImportedFeature featureInnerhalb = ImportedFeatureBuilder.empty()
			.art(Art.Massnahme)
			.quelle(QuellSystem.RadNETZ)
			.fachId("fach id")
			.importDatum(date)
			.lineString(new Coordinate(10, 10), new Coordinate(10, 30), new Coordinate(20, 30))
			.build();

		ImportedFeature featureTeilweiseAusserhalb = ImportedFeatureBuilder.empty()
			.art(Art.Massnahme)
			.quelle(QuellSystem.RadNETZ)
			.fachId("fach id")
			.importDatum(date)
			.lineString(new Coordinate(20, 20), new Coordinate(40, 40), new Coordinate(40, 50))
			.build();

		ImportedFeature featureKomplettAusserhalb = ImportedFeatureBuilder.empty()
			.art(Art.Massnahme)
			.quelle(QuellSystem.RadNETZ)
			.fachId("fach id")
			.importDatum(date)
			.lineString(new Coordinate(31, 31), new Coordinate(40, 40), new Coordinate(40, 50))
			.build();
		// Act
		repository.save(featureInnerhalb);
		repository.save(featureTeilweiseAusserhalb);
		repository.save(featureKomplettAusserhalb);

		entityManager.flush();
		entityManager.clear();

		Envelope envelope = new Envelope(9, 21, 9, 31);
		List<ImportedFeatureMapView> result = repository.getFeaturesInBereich(QuellSystem.RadNETZ, Art.Massnahme,
			envelope);

		// Assert
		assertThat(result).isNotEmpty();
		assertThat(result).extracting(ImportedFeatureMapView::getGeometrie).containsExactlyInAnyOrder(
			featureInnerhalb.getGeometrie(),
			featureTeilweiseAusserhalb.getGeometrie());
	}

	@Test
	void geometry3Dvs2D() {
		assertThat(repository).isNotNull();

		ImportedFeature feature = ImportedFeatureBuilder.empty()
			.art(Art.Massnahme)
			.quelle(QuellSystem.RadNETZ)
			.fachId("fach id")
			.importDatum(LocalDateTime.of(2021, 3, 22, 15, 12))
			.lineString(new Coordinate(10, 10), new Coordinate(10, 30), new Coordinate(20, 30))
			.build();

		assertThat(feature.getGeometrie().getDimension()).isEqualTo(1);

		// Act
		repository.save(feature);

		entityManager.flush();
		entityManager.clear();

		ImportedFeatureMapView loadedFeature = repository
			.getFeaturesInBereich(QuellSystem.RadNETZ, Art.Massnahme, new Envelope(0, 100, 0, 100)).get(0);
		assertThat(loadedFeature.getGeometrie().getDimension()).isEqualTo(1);
	}
}
