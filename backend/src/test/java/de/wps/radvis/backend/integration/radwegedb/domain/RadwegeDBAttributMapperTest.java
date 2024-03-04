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

package de.wps.radvis.backend.integration.radwegedb.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.AttributNichtImportiertException;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;

class RadwegeDBAttributMapperTest {

	private RadwegeDBAttributMapper radwegeDBNetzbildungAttributMapper;

	private RadwegeDBNetzbildungProtokollService protokollServiceMock;

	@BeforeEach
	void setup() {
		protokollServiceMock = Mockito.mock(RadwegeDBNetzbildungProtokollService.class);
		radwegeDBNetzbildungAttributMapper = new RadwegeDBAttributMapper(protokollServiceMock);
	}

	@Test
	void mapKantenAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("beleuchtun", "1")
			.build();

		// act
		KantenAttribute kantenAttribute = radwegeDBNetzbildungAttributMapper.mapKantenAttribute(feature);

		// assert
		assertThat(kantenAttribute.getBeleuchtung()).isEqualTo(Beleuchtung.VORHANDEN);
	}

	@Test
	void mapKantenAttribute_Exception() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("beleuchtun", "12938")
			.build();

		// act
		KantenAttribute kantenAttribute = radwegeDBNetzbildungAttributMapper.mapKantenAttribute(feature);

		// assert
		assertThat(kantenAttribute.getBeleuchtung()).isEqualTo(Beleuchtung.UNBEKANNT);
		Mockito.verify(protokollServiceMock, times(1))
			.handle(any(AttributNichtImportiertException.class), eq(RadwegeDBNetzbildungJob.class.getSimpleName()));
	}

	@Test
	void mapGeschwindigkeitAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("beleuchtun", "1")
			.build();

		// act
		GeschwindigkeitAttribute geschwindigkeitAttribute = radwegeDBNetzbildungAttributMapper
			.mapGeschwindigkeitAttribute(feature);

		// assert
		assertThat(geschwindigkeitAttribute)
			.isEqualTo(GeschwindigkeitAttribute.builder().build());
	}

	@Test
	void mapRichtungAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("richtung", "2")
			.build();

		// act
		Richtung richtung = radwegeDBNetzbildungAttributMapper.mapRichtungAttribute(feature);

		// assert
		assertThat(richtung).isEqualTo(Richtung.IN_RICHTUNG);
	}

	@Test
	void mapRichtungAttribute_Exception() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("richtung", "2123")
			.build();

		// act
		Richtung richtung = radwegeDBNetzbildungAttributMapper.mapRichtungAttribute(feature);

		// assert
		assertThat(richtung).isEqualTo(Richtung.UNBEKANNT);
		Mockito.verify(protokollServiceMock, times(1))
			.handle(any(AttributNichtImportiertException.class), eq(RadwegeDBNetzbildungJob.class.getSimpleName()));
	}

	@Test
	void mapZustaendigkeitAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("vereinbaru", "Mit Gemeinde Ammerbuch")
			.build();

		// act
		ZustaendigkeitAttribute zustaendigkeitAttribute = radwegeDBNetzbildungAttributMapper
			.mapZustaendigkeitAttribute(feature);

		// assert
		assertThat(zustaendigkeitAttribute.getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("Mit Gemeinde Ammerbuch"));
		assertThat(zustaendigkeitAttribute.getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator).isEqualTo(
				LinearReferenzierterAbschnitt.of(0, 1));
	}

	@Test
	void mapFuehrungsformAttribute() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("belag", "20")
			.addAttribut("breite", "1")
			.addAttribut("wegart", "310")
			.build();

		// act
		FuehrungsformAttribute fuehrungsformAttribute = radwegeDBNetzbildungAttributMapper
			.mapFuehrungsformAttribute(feature);

		// assert
		assertThat(fuehrungsformAttribute.getBelagArt()).isEqualTo(BelagArt.BETON);
		assertThat(fuehrungsformAttribute.getBreite()).contains(Laenge.of(1.49));
		assertThat(fuehrungsformAttribute.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.RADFAHRSTREIFEN);
		assertThat(fuehrungsformAttribute.getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator).isEqualTo(
				LinearReferenzierterAbschnitt.of(0, 1));
	}

	@Test
	void mapFuehrungsformAttribute_Exception() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("belag", "20")
			.addAttribut("breite", "1")
			.addAttribut("wegart", "3123123123")
			.build();

		// act
		FuehrungsformAttribute fuehrungsformAttribute = radwegeDBNetzbildungAttributMapper
			.mapFuehrungsformAttribute(feature);

		// assert
		assertThat(fuehrungsformAttribute.getBelagArt()).isEqualTo(BelagArt.BETON);
		assertThat(fuehrungsformAttribute.getBreite()).contains(Laenge.of(1.49));
		assertThat(fuehrungsformAttribute.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.UNBEKANNT);
		assertThat(fuehrungsformAttribute.getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator).isEqualTo(
				LinearReferenzierterAbschnitt.of(0, 1));
		Mockito.verify(protokollServiceMock, times(1))
			.handle(any(AttributNichtImportiertException.class), eq(RadwegeDBNetzbildungJob.class.getSimpleName()));
	}

	@Test
	void mapIstStandard() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("beleuchtun", "1")
			.build();

		// act
		Set<IstStandard> istStandards = radwegeDBNetzbildungAttributMapper.mapIstStandard(feature);

		// assert
		assertThat(istStandards).isEmpty();
	}

	@Test
	void mapNetzKlassen() {
		// arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzStrecke()
			.addAttribut("beleuchtun", "1")
			.build();

		// act
		Set<Netzklasse> netzklassen = radwegeDBNetzbildungAttributMapper.mapNetzKlassen(feature);

		// assert
		assertThat(netzklassen).isEmpty();
	}
}
