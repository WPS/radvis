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

package de.wps.radvis.backend.konsistenz.regeln.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;

@Tag("group7")
@EnableConfigurationProperties(value = {
	OrganisationConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
@AutoConfigureTestEntityManager
public class FehlendeVernetzungKonsistenzregelTestIT extends AbstractKonsistenzregelTestIT {
	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	KnotenRepository knotenRepository;

	FehlendeVernetzungKonsistenzregel fehlendeVernetzungKonsistenzregel;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		fehlendeVernetzungKonsistenzregel = new FehlendeVernetzungKonsistenzregel(jdbcTemplate);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void pruefen() {
		// arrange
		TestTransaction.end();
		TestTransaction.start();
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.DLM_REIMPORT_JOB);
		// wir bauen eine zusammenhängende U-Form aus 3 Kanten, um sicherzustellen, dass bereits adjazente Kanten nicht
		// gefunden werden
		Knoten vonKnotenBasis = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
				.build());
		Knoten bisKnotenBasis = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM)
				.build());
		Kante basis = kantenRepository
			.save(KanteTestDataProvider.fromKnoten(vonKnotenBasis, bisKnotenBasis).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("DlmId1")).build());

		Knoten vonKnotenAdjazent1 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM)
				.build());
		kantenRepository
			.save(KanteTestDataProvider.fromKnoten(vonKnotenAdjazent1, vonKnotenBasis).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("DlmId2")).build());

		Knoten bisKnotenAdjazent2 = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
				.build());
		kantenRepository
			.save(KanteTestDataProvider.fromKnoten(bisKnotenBasis, bisKnotenAdjazent2).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("DlmId3")).build());

		// Wir bauen eine Kante in die Mitte, die auf die basis-Kante stößt (Radius 1m), die soll als Fehler gefunden
		// werden
		Knoten vonKnotenFehler = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0.9, 50), QuellSystem.DLM)
				.build());
		Knoten bisKnotenFehler = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 50), QuellSystem.DLM)
				.build());
		kantenRepository
			.save(KanteTestDataProvider.fromKnoten(vonKnotenFehler, bisKnotenFehler).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("DlmId4")).build());

		// wir bauen eine losgelöste Kante mit mehr Abstand (als Deckel auf das U), die soll nicht gefunden werden

		Knoten vonKnotenUnabhaengig = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(102, 0), QuellSystem.DLM)
				.build());
		Knoten bisKnotenUnabhaengig = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(102, 100), QuellSystem.DLM)
				.build());
		kantenRepository
			.save(KanteTestDataProvider.fromKnoten(vonKnotenUnabhaengig, bisKnotenUnabhaengig).quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("DlmId5")).build());

		TestTransaction.flagForCommit();
		TestTransaction.end();

		// wir sichern uns gegen false positive tests ab

		List<Map<String, Object>> allKanten = jdbcTemplate
			.queryForList("SELECT id, von_knoten_id, nach_knoten_id FROM kante");
		assertThat(allKanten.size()).isEqualTo(5);

		// act
		List<KonsistenzregelVerletzungsDetails> result = fehlendeVernetzungKonsistenzregel.pruefen();

		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0).getIdentity()).isEqualTo(vonKnotenFehler.getId() + "/" + basis.getId());
		assertThat(result.get(0).getPosition()).isEqualTo(vonKnotenFehler.getPoint());
		assertThat(result.get(0).getBeschreibung()).isEqualTo(FehlendeVernetzungKonsistenzregel
			.createBeschreibung(basis.getDlmId().getValue(), basis.getId(), vonKnotenFehler.getId()));
		assertThat(result.get(0).getOriginalGeometry()).contains(basis.getGeometry());
	}

	@Test
	public void beschreibung() {
		String beschreibung = FehlendeVernetzungKonsistenzregel.createBeschreibung("DlmId", 237646586l, 87457654l);
		assertThat(beschreibung).isEqualTo(
			"Der Knoten mit ID 87457654 ist wahrscheinlich ein Kreuzungspunkt auf die Kante mit ID 237646586 (DLM-ID: DlmId).");
	}
}
