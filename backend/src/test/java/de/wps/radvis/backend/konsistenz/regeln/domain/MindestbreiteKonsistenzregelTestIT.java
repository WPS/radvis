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

import static de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider.withRichtungRadverkehrsfuehrungIstStandardBreiteQuellsystem;
import static de.wps.radvis.backend.netz.domain.valueObject.IstStandard.BASISSTANDARD;
import static de.wps.radvis.backend.netz.domain.valueObject.IstStandard.RADSCHNELLVERBINDUNG;
import static de.wps.radvis.backend.netz.domain.valueObject.IstStandard.RADVORRANGROUTEN;
import static de.wps.radvis.backend.netz.domain.valueObject.IstStandard.STARTSTANDARD_RADNETZ;
import static de.wps.radvis.backend.netz.domain.valueObject.IstStandard.ZIELSTANDARD_RADNETZ;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.BETRIEBSWEG_FORST;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG;
import static de.wps.radvis.backend.netz.domain.valueObject.Richtung.BEIDE_RICHTUNGEN;
import static de.wps.radvis.backend.netz.domain.valueObject.Richtung.GEGEN_RICHTUNG;
import static de.wps.radvis.backend.netz.domain.valueObject.Richtung.IN_RICHTUNG;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import jakarta.validation.constraints.NotNull;

@Tag("group7")
@EnableConfigurationProperties(value = {
	OrganisationConfigurationProperties.class
})
@AutoConfigureTestEntityManager
class MindestbreiteKonsistenzregelTestIT extends AbstractKonsistenzregelTestIT {

	@Autowired
	KantenRepository kantenRepository;

	MindestbreiteKonsistenzregel mindestbreiteKonsistenzregel;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		mindestbreiteKonsistenzregel = new MindestbreiteKonsistenzregel(jdbcTemplate);
	}

	@Test
	void kantenSeitenhaltenMindestbreiteNichtEin() {
		// arrange
		saveNewKante(IN_RICHTUNG, SONDERWEG_RADWEG_STRASSENBEGLEITEND, STARTSTANDARD_RADNETZ, 1.39, QuellSystem.DLM);
		saveNewKante(GEGEN_RICHTUNG, GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND, ZIELSTANDARD_RADNETZ, 1.99,
			QuellSystem.DLM);
		saveNewKante(IN_RICHTUNG, GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND, BASISSTANDARD, 2.49, QuellSystem.DLM);
		saveNewKante(GEGEN_RICHTUNG, GEHWEG_RAD_FREI_STRASSENBEGLEITEND, RADVORRANGROUTEN, 2.99, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, FUEHRUNG_IN_FAHRRADSTRASSE, RADSCHNELLVERBINDUNG, 4.59, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, FUEHRUNG_IN_FAHRRADZONE, RADVORRANGROUTEN, 4.74, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG, STARTSTANDARD_RADNETZ, 1.79,
			QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND, ZIELSTANDARD_RADNETZ, 2.49,
			QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, BETRIEBSWEG_FORST, BASISSTANDARD, 2.49, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, BETRIEBSWEG_WASSERWIRTSCHAFT, RADVORRANGROUTEN, 4.49, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, SONSTIGER_BETRIEBSWEG, RADSCHNELLVERBINDUNG, 4.99, QuellSystem.DLM);

		testEntityManager.getEntityManager().flush();
		testEntityManager.getEntityManager().clear();

		assertThat(jdbcTemplate.queryForList("SELECT id FROM kante").size()).isEqualTo(11);

		// Es wird jede Kantenseite geprueft, darum ist diese Zahl doppelt so hoch wie die Anzahl Kanten.
		assertThat(mindestbreiteKonsistenzregel.pruefen().size()).isEqualTo(22);
	}

	@Test
	void kantenSeitenhaltenMindestbreiteSchonEinOderEsIstKeineRegelHinterlegt() {
		// arrange
		saveNewKante(GEGEN_RICHTUNG, GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND, STARTSTANDARD_RADNETZ, 1.4,
			QuellSystem.DLM);
		saveNewKante(IN_RICHTUNG, GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND, BASISSTANDARD, 2.5, QuellSystem.DLM);
		saveNewKante(IN_RICHTUNG, GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND,
			RADSCHNELLVERBINDUNG,
			0.3,
			QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, FUEHRUNG_IN_FAHRRADSTRASSE, RADVORRANGROUTEN, 4.75, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, FUEHRUNG_IN_FAHRRADZONE, RADSCHNELLVERBINDUNG, 4.6, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, FUEHRUNG_IN_FAHRRADZONE, ZIELSTANDARD_RADNETZ, 1, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG, STARTSTANDARD_RADNETZ, 1.8,
			QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND, ZIELSTANDARD_RADNETZ, 2.5,
			QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, BETRIEBSWEG_FORST, BASISSTANDARD, 2.5, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, BETRIEBSWEG_WASSERWIRTSCHAFT, RADVORRANGROUTEN, 4.5, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, SONSTIGER_BETRIEBSWEG, RADSCHNELLVERBINDUNG, 5, QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER, RADSCHNELLVERBINDUNG, 1,
			QuellSystem.DLM);
		saveNewKante(BEIDE_RICHTUNGEN, SONSTIGE_STRASSE_WEG, RADSCHNELLVERBINDUNG, 1, QuellSystem.DLM);

		testEntityManager.getEntityManager().flush();
		testEntityManager.getEntityManager().clear();

		assertThat(jdbcTemplate.queryForList("SELECT id FROM kante").size()).isEqualTo(13);

		assertThat(mindestbreiteKonsistenzregel.pruefen()).isEmpty();
	}

	@Test
	void identityWirdErzeugt() {
		// arrange
		Kante kante = saveNewKante(BEIDE_RICHTUNGEN, SONSTIGER_BETRIEBSWEG, STARTSTANDARD_RADNETZ, 1.79,
			QuellSystem.DLM);

		testEntityManager.getEntityManager().flush();
		testEntityManager.getEntityManager().clear();

		assertThat(jdbcTemplate.queryForList("SELECT id FROM kante").size()).isEqualTo(1);

		// act
		List<KonsistenzregelVerletzungsDetails> gepruefteKantenSeiten = mindestbreiteKonsistenzregel.pruefen();

		// assert
		KonsistenzregelVerletzungsDetails verletzungLinks = gepruefteKantenSeiten.get(0);
		assertThat(verletzungLinks.getIdentity()).isEqualTo(
			"{id:\"" + kante.getId() + "\","
				+ "standard:\"STARTSTANDARD_RADNETZ\","
				+ "fahrtrichtung_links:\"BEIDE_RICHTUNGEN\","
				+ "fahrtrichtung_rechts:\"\","
				+ "radverkehrsfuehrung:\"SONSTIGER_BETRIEBSWEG\","
				+ "von:\"0,000000\","
				+ "bis:\"1,000000\"}");

		KonsistenzregelVerletzungsDetails verletzungRechts = gepruefteKantenSeiten.get(1);
		assertThat(verletzungRechts.getIdentity()).isEqualTo(
			"{id:\"" + kante.getId() + "\","
				+ "standard:\"STARTSTANDARD_RADNETZ\","
				+ "fahrtrichtung_links:\"\","
				+ "fahrtrichtung_rechts:\"BEIDE_RICHTUNGEN\","
				+ "radverkehrsfuehrung:\"SONSTIGER_BETRIEBSWEG\","
				+ "von:\"0,000000\",bis:\"1,000000\"}");
	}

	@Test
	void beschreibungWirdErzeugt() {
		// arrange
		saveNewKante(BEIDE_RICHTUNGEN, SONSTIGER_BETRIEBSWEG, RADSCHNELLVERBINDUNG, 4.99, QuellSystem.DLM);

		testEntityManager.getEntityManager().flush();
		testEntityManager.getEntityManager().clear();

		assertThat(jdbcTemplate.queryForList("SELECT id FROM kante").size()).isEqualTo(1);

		// act
		List<KonsistenzregelVerletzungsDetails> gepruefteKantenSeiten = mindestbreiteKonsistenzregel.pruefen();

		// assert
		KonsistenzregelVerletzungsDetails verletzungLinks = gepruefteKantenSeiten.get(0);
		assertThat(verletzungLinks.getBeschreibung()).isEqualTo(
			"Die Breite ist 4,99 m und somit wird die Mindestbreite von 5,00 m nicht eingehalten.");

		KonsistenzregelVerletzungsDetails verletzungRechts = gepruefteKantenSeiten.get(1);
		assertThat(verletzungRechts.getBeschreibung()).isEqualTo(
			"Die Breite ist 4,99 m und somit wird die Mindestbreite von 5,00 m nicht eingehalten.");
	}

	@Test
	void kantenMitQuelleUngleich_DLM_RadVIS_ignoriert() {
		// arrange
		Kante kanteRadwegeDb = saveNewKante(BEIDE_RICHTUNGEN, SONSTIGER_BETRIEBSWEG, RADSCHNELLVERBINDUNG, 4.99,
			QuellSystem.RadwegeDB);
		Kante kanteRadNETZ = saveNewKante(BEIDE_RICHTUNGEN, SONSTIGER_BETRIEBSWEG, RADSCHNELLVERBINDUNG, 4.99,
			QuellSystem.RadNETZ);
		Kante kanteDlm = saveNewKante(BEIDE_RICHTUNGEN, SONSTIGER_BETRIEBSWEG, RADSCHNELLVERBINDUNG, 4.99,
			QuellSystem.DLM);
		Kante kanteRadVIS = saveNewKante(BEIDE_RICHTUNGEN, SONSTIGER_BETRIEBSWEG, RADSCHNELLVERBINDUNG, 4.99,
			QuellSystem.RadVis);

		testEntityManager.getEntityManager().flush();
		testEntityManager.getEntityManager().clear();

		assertThat(jdbcTemplate.queryForList("SELECT id FROM kante").size()).isEqualTo(4);

		// act
		List<KonsistenzregelVerletzungsDetails> gepruefteKantenSeiten = mindestbreiteKonsistenzregel.pruefen();

		// assert
		assertThat(gepruefteKantenSeiten).hasSize(4);

		assertThat(gepruefteKantenSeiten.stream()
			.map(KonsistenzregelVerletzungsDetails::getIdentity)
			.filter(s -> s.contains("id:\"" + kanteDlm.getId() + "\""))
			.collect(Collectors.toList())).hasSize(2); // Fuer jede Seite der Dlm-Kante einen Eintrag

		assertThat(gepruefteKantenSeiten.stream()
			.map(KonsistenzregelVerletzungsDetails::getIdentity)
			.filter(s -> s.contains("id:\"" + kanteRadVIS.getId() + "\""))
			.collect(Collectors.toList())).hasSize(2); // Fuer jede Seite der RadVIS-Kante einen Eintrag

		assertThat(gepruefteKantenSeiten.stream()
			.map(KonsistenzregelVerletzungsDetails::getIdentity)
			.filter(s -> s.contains("id:\"" + kanteRadwegeDb.getId() + "\"") || s.contains(
				"id:\"" + kanteRadNETZ.getId() + "\""))
			.collect(Collectors.toList())).isEmpty(); // Keinen Eintrag fuer RadNETZ oder RadwegeDB
	}

	@NotNull
	private Kante saveNewKante(Richtung richtung, Radverkehrsfuehrung radverkehrsfuehrung, IstStandard istStandard,
		double breite, QuellSystem quellSystem) {
		return kantenRepository.save(
			withRichtungRadverkehrsfuehrungIstStandardBreiteQuellsystem(richtung, radverkehrsfuehrung, istStandard,
				breite, quellSystem, true).build());
	}

}
