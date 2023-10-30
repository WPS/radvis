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

package de.wps.radvis.backend.integration.radnetz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.AttributNichtImportiertException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;

class RadNetzAttributMapperTest {
	@Mock
	private RadNetzNetzbildungProtokollService protokollService;

	private RadNetzAttributMapper radNetzAttributMapper;

	@BeforeEach
	void clearLog() {
		MockitoAnnotations.openMocks(this);
		radNetzAttributMapper = new RadNetzAttributMapper(protokollService);
	}

	@Test
	public void mapKantenAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("LICHT", "vorhanden")
			.addAttribut("ANM", "Fritz")
			.addAttribut("Anm_Nach", "Cola")
			.build();

		// act
		KantenAttribute kantenAttribute = radNetzAttributMapper.mapKantenAttribute(feature);

		// assert
		assertThat(kantenAttribute.getBeleuchtung()).isEqualTo(Beleuchtung.VORHANDEN);
		assertThat(kantenAttribute.getKommentar()).contains(Kommentar.of("Fritz\nCola"));
	}

	@Test
	void mapFahrtrichtungAttributGruppe() {
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("RICHTUNG", "Einrichtungsverkehr")
			.build();

		// act
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = radNetzAttributMapper.mapFahrtrichtungAttributGruppe(
			feature);

		// assert
		assertThat(fahrtrichtungAttributGruppe.getFahrtrichtungLinks()).isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(fahrtrichtungAttributGruppe.getFahrtrichtungRechts()).isEqualTo(Richtung.IN_RICHTUNG);
	}

	@Test
	void mapFahrtrichtungAttributGruppeUnbekannt() {
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke().build();

		// act
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = radNetzAttributMapper.mapFahrtrichtungAttributGruppe(
			feature);

		// assert
		assertThat(fahrtrichtungAttributGruppe.getFahrtrichtungLinks()).isEqualTo(Richtung.UNBEKANNT);
		assertThat(fahrtrichtungAttributGruppe.getFahrtrichtungRechts()).isEqualTo(Richtung.UNBEKANNT);
	}

	@Test
	void mapZustaendigkeitAttributeDefaultEmpty() {
		// act
		ZustaendigkeitAttribute zustaendigkeitAttribute = radNetzAttributMapper.mapZustaendigkeitAttribute();

		// assert
		assertThat(zustaendigkeitAttribute.getErhaltsZustaendiger()).isEmpty();
		assertThat(zustaendigkeitAttribute.getUnterhaltsZustaendiger()).isEmpty();
		assertThat(zustaendigkeitAttribute.getVereinbarungsKennung()).isEmpty();
		assertThat(zustaendigkeitAttribute.getBaulastTraeger()).isEmpty();
	}

	@Test
	public void mapKantenAttribute_protokolliertFehler() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("LICHT", "leuchtet")
			.addAttribut("LRVN_KAT", "1")
			.build();

		// act
		radNetzAttributMapper.mapKantenAttribute(feature);

		// assert
		verify(protokollService).handle(any(AttributNichtImportiertException.class), any(String.class));
	}

	@Test
	public void mapGeschwindigkeitAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("ORTSLAGE", "innerorts")
			.addAttribut("STRASSE", "Vzul < 30 km/h")
			.addAttribut("WEGEART", "Verkehrsberuhigter Bereich")
			.build();

		// act
		GeschwindigkeitAttribute attribute = radNetzAttributMapper.mapGeschwindigkeitAttribute(feature);

		// assert
		assertThat(attribute.getOrtslage()).contains(KantenOrtslage.INNERORTS);
		assertThat(attribute.getHoechstgeschwindigkeit()).isEqualTo(Hoechstgeschwindigkeit.MAX_9_KMH);
		assertThat(attribute.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung()).isEmpty();
	}

	@Test
	public void mapGeschwindigkeitAttribute_protokolliertFehler() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("ORTSLAGE", "Innerorts")
			.addAttribut("STRASSE", "Vzul < 30 km/h")
			.addAttribut("WEGEART", "Bier")
			.build();

		// act
		GeschwindigkeitAttribute attribute = radNetzAttributMapper.mapGeschwindigkeitAttribute(feature);

		// assert
		assertThat(attribute.getOrtslage()).contains(KantenOrtslage.INNERORTS);
		assertThat(attribute.getHoechstgeschwindigkeit()).isEqualTo(Hoechstgeschwindigkeit.UNBEKANNT);
		assertThat(attribute.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung()).isEmpty();
		verify(protokollService).handle(any(AttributNichtImportiertException.class), any(String.class));
	}

	@Test
	public void mapFuehrungsformAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("BELAGART", "Ungebundene Decke (Kies/Split/Sand/Erde/Gras)")
			.addAttribut("STRASSE", "Vzul 30")
			.addAttribut("BORD", "Bordsteine auf Abschnitt überwiegend abgesenkt")
			.addAttribut("WEGEART", "Einbahnstraße (für Rad nicht freigegeben)")
			.addAttribut("WEGETYP", "Führung auf der Fahrbahn (unmarkiert)")
			.addAttribut("BREITEVA", "3,00 m bis < 3,50 m")
			.build();

		// act
		FuehrungsformAttribute attribute = radNetzAttributMapper.mapFuehrungsformAttribute(feature);

		// assert
		assertThat(attribute.getBelagArt()).isEqualTo(BelagArt.UNGEBUNDENE_DECKE);
		assertThat(attribute.getBordstein()).isEqualTo(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER);
		assertThat(attribute.getLinearReferenzierterAbschnitt()).isEqualTo(LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(attribute.getRadverkehrsfuehrung())
			.isEqualTo(Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30);
		assertThat(attribute.getParkenTyp()).isEqualTo(KfzParkenTyp.UNBEKANNT);
		assertThat(attribute.getParkenForm()).isEqualTo(KfzParkenForm.UNBEKANNT);
		assertThat(attribute.getBreite()).contains(Laenge.of(3.));
	}

	@Test
	void radverkehrsFuehrungUnbekannt() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("WEGEART", "")
			.addAttribut("WEGETYP", "")
			.build();

		// act
		FuehrungsformAttribute attribute = radNetzAttributMapper.mapFuehrungsformAttribute(feature);

		// assert
		assertThat(attribute.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.UNBEKANNT);
	}

	/**
	 * Tests für Knotenattribute
	 */

	// Tests für alle Attribute
	@Test
	void testUebersetzePunktAttributeNachRadVis_keineRadNetzAttribute_leereKantenAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzPunkt().build();

		// act
		KnotenAttribute knotenAttribute = radNetzAttributMapper.uebersetzePunktAttributeNachRadVis(feature);

		// assert
		assertThat(knotenAttribute.getKommentar()).isEmpty();
		assertThat(knotenAttribute.getKnotenForm()).isEmpty();
	}

	@Test
	void testUebersetzePunktAttributeNachRadVis_AnmNach_mapptAufKommentar() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("Anm_Nach", "Mein Kommentar")
			.addAttribut("Anm_NachB", "dazu")
			.build();

		// act
		KnotenAttribute attribute = radNetzAttributMapper.uebersetzePunktAttributeNachRadVis(feature);

		// assert
		assertThat(attribute.getKommentar()).contains(Kommentar.of("Mein Kommentar\ndazu"));
	}

	@Test
	void testUebersetzePunktAttributeNachRadVis_AnmNach_Leer_mapptAufKommentarNull() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("Anm_Nach", "")
			.addAttribut("Anm_NachB", "")
			.build();

		// act
		KnotenAttribute attribute = radNetzAttributMapper.uebersetzePunktAttributeNachRadVis(feature);

		// assert
		assertThat(attribute.getKommentar()).isEmpty();
	}

	@Test
	void testUebersetzePunktAttributeNachRadVis_kutyp_mapptAufKnotenForm() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("KUTYP", "erweiterte Fuß-/Radfahrer LSA")
			.build();

		// act
		KnotenAttribute attribute = radNetzAttributMapper.uebersetzePunktAttributeNachRadVis(feature);

		// assert
		assertThat(attribute.getKnotenForm()).contains(KnotenForm.ERWEITERTE_FUSS_RADFAHRER_LSA);
	}

	@Test
	void testUebersetzePunktAttributeNachRadVis_kuuty_filtertKreisfahrbahn() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("KUUTY", "Führung in Kreisfahrbahn")
			.build();

		// act
		KnotenAttribute attribute = radNetzAttributMapper.uebersetzePunktAttributeNachRadVis(feature);

		// assert
		assertThat(attribute.getZustandsbeschreibung()).isEmpty();
	}

	@Test
	void testUebersetzePunktAttributeNachRadVis_kuuty_filtertNicht() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("KUUTY", "Führung an Fußg.-/Radf.-LSA in Ordnung")
			.build();

		// act
		KnotenAttribute attribute = radNetzAttributMapper.uebersetzePunktAttributeNachRadVis(feature);

		// assert
		assertThat(attribute.getZustandsbeschreibung().get())
			.isEqualTo(Zustandsbeschreibung.of("Führung an Fußg.-/Radf.-LSA in Ordnung"));
	}

	@Test
	public void mapNetzKlasse() {
		// arrange
		ImportedFeature feature1 = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("LRVN_KAT", "1.0")
			.build();
		ImportedFeature feature2 = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("LRVN_KAT", "3.0")
			.build();
		ImportedFeature feature3 = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("LRVN_KAT", "4.0")
			.build();
		ImportedFeature feature4 = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("LRVN_KAT", "6.0")
			.build();

		// act
		Set<Netzklasse> netzKlassen1 = radNetzAttributMapper.mapNetzKlassen(feature1);
		Set<Netzklasse> netzKlassen2 = radNetzAttributMapper.mapNetzKlassen(feature2);
		Set<Netzklasse> netzKlassen3 = radNetzAttributMapper.mapNetzKlassen(feature3);
		Set<Netzklasse> netzKlassen4 = radNetzAttributMapper.mapNetzKlassen(feature4);

		// assert
		assertThat(netzKlassen1.size()).isEqualTo(1);
		assertThat(netzKlassen1).containsExactlyInAnyOrder(Netzklasse.RADNETZ_FREIZEIT);

		assertThat(netzKlassen2.size()).isEqualTo(1);
		assertThat(netzKlassen2).containsExactlyInAnyOrder(Netzklasse.RADNETZ_ALLTAG);

		assertThat(netzKlassen3.size()).isEqualTo(1);
		assertThat(netzKlassen3).containsExactlyInAnyOrder(Netzklasse.RADNETZ_ZIELNETZ);

		assertThat(netzKlassen4.size()).isEqualTo(2);
		assertThat(netzKlassen4).containsExactlyInAnyOrder(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ALLTAG);
	}

	@Test
	public void mapNetzKlasse_wirfException() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("LRVN_KAT", "5")
			.build();

		// act
		Set<Netzklasse> netzKlassen = radNetzAttributMapper.mapNetzKlassen(feature);

		// assert
		assertThat(netzKlassen).isEmpty();
		verify(protokollService).handle(any(AttributNichtImportiertException.class), any(String.class));
	}

	@Test
	public void mapIstStandards_wirftException() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("IstZustand", "bla")
			.build();

		// act
		Set<IstStandard> istStandards = radNetzAttributMapper.mapIstStandards(feature);

		// assert
		assertThat(istStandards).isEmpty();
		verify(protokollService).handle(any(AttributNichtImportiertException.class), any(String.class));
	}

	@Test
	public void mapIstStandards_ZielStandardAlsQuellwert_RadNetzStartUndZielStandardWerdenGesetzt() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("IstZustand", "Start- und Zielstandard erfüllt")
			.build();

		// act
		Set<IstStandard> istStandards = radNetzAttributMapper.mapIstStandards(feature);

		// assert
		assertThat(istStandards).containsExactlyInAnyOrder(IstStandard.STARTSTANDARD_RADNETZ,
			IstStandard.ZIELSTANDARD_RADNETZ);
	}
}
