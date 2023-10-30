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

package de.wps.radvis.backend.abfrage.export.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.abfrage.export.ExportConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.KanteElevationUpdate;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group4")
@ContextConfiguration(classes = {
	ExportFromViewServiceTestIT.TestConfiguration.class,
})
@MockBeans({
	@MockBean(CommonConfigurationProperties.class),
	@MockBean(FeatureToggleProperties.class),
	@MockBean(PostgisConfigurationProperties.class)
})
class ExportFromViewServiceTestIT extends DBIntegrationTestIT {

	@Configuration
	@EnableJpaRepositories(basePackages = {
		"de.wps.radvis.backend.common",
		"de.wps.radvis.backend.netz"
	})
	@EntityScan(basePackageClasses = { ExportConfiguration.class, NetzConfiguration.class })
	public static class TestConfiguration {
	}

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	KnotenRepository knotenRepository;

	@Autowired
	KantenRepository kantenRepository;

	ExportFromViewService exportFromViewService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		exportFromViewService = new ExportFromViewService(entityManager);
	}

	@Test
	void exportFromView_knoten() {
		// arrange
		Kante kanteRadnetzAlltag = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 20, 22, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)),
						new HashSet<>()
					)
				)
				.build());

		// Achtung: hier interessieren uns nur die Knoten, aber das Feld kante.geometry3d muss gesetzt sein.
		entityManager.createNativeQuery("UPDATE kante SET geometry3d = geometry").executeUpdate();

		kantenRepository.refreshRadVisNetzMaterializedView(); // knoten-view setzt auf mat. View der Kanten auf
		kantenRepository.refreshRadVisNetzAbschnitteMaterializedView();

		entityManager.flush();
		entityManager.clear();

		// act
		Map<String, List<ExportData>> exportDataMulti = exportFromViewService.exportBalm();

		// assert
		List<ExportData> exportDataKnoten = exportDataMulti.get("Knoten");
		assertThat(exportDataKnoten).hasSize(2);

		Optional<ExportData> vonKnoten = exportDataKnoten.stream().filter(ed ->
			ed.getGeometry().getCoordinate().equals(kanteRadnetzAlltag.getVonKnoten().getKoordinate())
		).findFirst();
		assertThat(vonKnoten).isPresent();
		assertThat(vonKnoten.get().getProperties().get("Quell-ID"))
			.isEqualTo(kanteRadnetzAlltag.getVonKnoten().getId().toString());

		Optional<ExportData> nachKnoten = exportDataKnoten.stream().filter(ed ->
			ed.getGeometry().getCoordinate().equals(kanteRadnetzAlltag.getNachKnoten().getKoordinate())
		).findFirst();
		assertThat(nachKnoten).isPresent();
		assertThat(nachKnoten.get().getProperties().get("Quell-ID"))
			.isEqualTo(kanteRadnetzAlltag.getNachKnoten().getId().toString());
	}

	@Test
	void exportFromView_kanten() {
		// arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000010),
				QuellSystem.DLM)
			.build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400000, 5000020),
				QuellSystem.DLM)
			.build();

		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });
		LineString lineString_3d = GeometryTestdataProvider.createLineString(
			new Coordinate(lineString.getStartPoint().getX(), lineString.getStartPoint().getY(), 666.),
			new Coordinate(lineString.getEndPoint().getX(), lineString.getEndPoint().getY(), 777.)
		);
		Kante kanteRadnetzAlltag = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues()
				.geometry(lineString)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)),
						new HashSet<>()
					)
				)
				.build());

		entityManager.flush();
		entityManager.clear();

		// Es braucht die 3D-Geometrie an den Kanten für den Balm Export
		kantenRepository.updateKanteElevation(
			new SliceImpl<>(List.of(
				new KanteElevationUpdate(kanteRadnetzAlltag.getId(), lineString_3d)
			)));

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshRadVisNetzMaterializedView(); // Notwendig da die AbschnitteView die Netzklassen aus einer der materialized "Subviews" dieser View zieht
		kantenRepository.refreshRadVisNetzAbschnitteMaterializedView();

		// act
		Map<String, List<ExportData>> exportDataMulti = exportFromViewService.exportBalm();
		List<ExportData> exportDataKanten = exportDataMulti.get("Kanten");

		// assert
		assertThat(exportDataKanten).hasSize(1);
		assertThat(exportDataKanten.get(0).getGeometry().getCoordinates())
			.containsExactly(lineString_3d.getCoordinates());
		assertThat(exportDataKanten.get(0).getProperties().get("Quell-ID")).isEqualTo("dlm-id");
	}
}