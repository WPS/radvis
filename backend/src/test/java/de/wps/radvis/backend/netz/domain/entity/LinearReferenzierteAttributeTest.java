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

package de.wps.radvis.backend.netz.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;

class LinearReferenzierteAttributeTest {

	@Test
	public void testeGetReversedAttribute() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = new ArrayList<>();

		zustaendigkeitAttribute.add(ZustaendigkeitAttribute.builder().vereinbarungsKennung(VereinbarungsKennung.of("1"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.2)).build());

		zustaendigkeitAttribute.add(ZustaendigkeitAttribute.builder().vereinbarungsKennung(VereinbarungsKennung.of("2"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.35)).build());

		zustaendigkeitAttribute.add(ZustaendigkeitAttribute.builder().vereinbarungsKennung(VereinbarungsKennung.of("3"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.35, 0.78)).build());

		zustaendigkeitAttribute.add(ZustaendigkeitAttribute.builder().vereinbarungsKennung(VereinbarungsKennung.of("4"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.78, 1)).build());

		// act
		List<ZustaendigkeitAttribute> reversedAttribute = LinearReferenzierteAttribute
			.getReversedAttribute(zustaendigkeitAttribute);

		// assert
		assertThat(reversedAttribute).size().isEqualTo(4);
		assertThat(LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			reversedAttribute.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(
				Collectors.toList()))).isTrue();
		assertThat(reversedAttribute.get(0).sindAttributeGleich(zustaendigkeitAttribute.get(
			zustaendigkeitAttribute.size() - 1))).isTrue();
		assertThat(reversedAttribute.get(0).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.22));

		assertThat(reversedAttribute.get(1).sindAttributeGleich(zustaendigkeitAttribute.get(
			zustaendigkeitAttribute.size() - 2))).isTrue();
		assertThat(reversedAttribute.get(1).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.22, 0.65));

		assertThat(reversedAttribute.get(2).sindAttributeGleich(zustaendigkeitAttribute.get(
			zustaendigkeitAttribute.size() - 3))).isTrue();
		assertThat(reversedAttribute.get(2).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.65, 0.8));

		assertThat(reversedAttribute.get(3).sindAttributeGleich(zustaendigkeitAttribute.get(
			zustaendigkeitAttribute.size() - 4))).isTrue();
		assertThat(reversedAttribute.get(3).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.8, 1.));
	}

	@Test
	public void testeGetReversedAttribute_segmentsDontCoverFullLine_ContractExplodiert() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = new ArrayList<>();

		zustaendigkeitAttribute.add(ZustaendigkeitAttribute.builder().vereinbarungsKennung(VereinbarungsKennung.of("1"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.2)).build());

		zustaendigkeitAttribute.add(ZustaendigkeitAttribute.builder().vereinbarungsKennung(VereinbarungsKennung.of("1"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.35)).build());

		// act + assert
		assertThrows(RequireViolation.class, () -> LinearReferenzierteAttribute
			.getReversedAttribute(zustaendigkeitAttribute));

	}

	@Test
	public void testeGetAufLineareReferenzZugeschnitten_schneidetZweiAttribute_ergibtVierAttribute() {
		// arrange
		ZustaendigkeitAttribute attribute1 = ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.5)
			.vereinbarungsKennung(VereinbarungsKennung.of("好きだよ")).build();
		ZustaendigkeitAttribute attribute2 = ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("えー？好きなわけねーだろう。")).build();

		List<ZustaendigkeitAttribute> attributes = new ArrayList<>(List.of(attribute1, attribute2));

		// act
		List<ZustaendigkeitAttribute> result = LinearReferenzierteAttribute.getAufLineareReferenzZugeschnitten(
			attributes, LinearReferenzierterAbschnitt.of(0.3, 0.8));

		// assert
		assertThat(result).hasSize(4);
		assertThat(result.get(0).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("好きだよ"));
		assertThat(result.get(1).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("好きだよ"));
		assertThat(result.get(2).getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("えー？好きなわけねーだろう。"));
		assertThat(result.get(3).getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("えー？好きなわけねーだろう。"));

		assertThat(result).extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
			.containsExactly(
				LinearReferenzierterAbschnitt.of(0, 0.3),
				LinearReferenzierterAbschnitt.of(0.3, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 0.8),
				LinearReferenzierterAbschnitt.of(0.8, 1.));

		assertAttributeSindSortiertUndCovernFullLine(result);
	}

	@Test
	void testeDefragmentieren_zustaendigkeit_kompliziertesBeispielMitEntfernenUndInterpolieren() {

		List<ZustaendigkeitAttribute> attribute = new ArrayList<>();
		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.15)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.15, 0.2)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.4)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.6)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 0.68)
			.vereinbarungsKennung(VereinbarungsKennung.of("789")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.68, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		List<ZustaendigkeitAttribute> defragmentiert = LinearReferenzierteAttribute
			.defragmentiereLinearReferenzierteAttribute(
				attribute, Laenge.of(10), Laenge.of(1.0));

		assertThat(defragmentiert)
			.extracting(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.usingComparatorForType(LineareReferenzTestProvider.lenientComparator, LinearReferenzierterAbschnitt.class)
			.containsExactly(
				LinearReferenzierterAbschnitt.of(0, 0.2),
				LinearReferenzierterAbschnitt.of(0.2, 1.));

		assertThat(defragmentiert.stream().map(ZustaendigkeitAttribute::getVereinbarungsKennung)
			.map(Optional::get).collect(Collectors.toList()))
				.containsExactly(VereinbarungsKennung.of("123"), VereinbarungsKennung.of("456"));
	}

	@Test
	void testeDefragmentieren_zustaendigkeit_ersterUndLetztesElementZuKlein() {

		List<ZustaendigkeitAttribute> attribute = new ArrayList<>();
		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.05)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.05, 0.95)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.95, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		List<ZustaendigkeitAttribute> defragmentiert = LinearReferenzierteAttribute
			.defragmentiereLinearReferenzierteAttribute(
				attribute, Laenge.of(10), Laenge.of(1.0));

		assertThat(defragmentiert)
			.extracting(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.usingComparatorForType(LineareReferenzTestProvider.lenientComparator, LinearReferenzierterAbschnitt.class)
			.containsExactly(
				LinearReferenzierterAbschnitt.of(0, 1.));

		assertThat(defragmentiert.stream().map(ZustaendigkeitAttribute::getVereinbarungsKennung)
			.map(Optional::get).collect(Collectors.toList()))
				.containsExactly(VereinbarungsKennung.of("123"));
	}

	@Test
	void testeDefragmentieren_zustaendigkeit_ElementInDerMitteZuKlein() {

		List<ZustaendigkeitAttribute> attribute = new ArrayList<>();
		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.5)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 0.55)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		attribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.55, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		List<ZustaendigkeitAttribute> defragmentiert = LinearReferenzierteAttribute
			.defragmentiereLinearReferenzierteAttribute(
				attribute, Laenge.of(10), Laenge.of(1.0));

		assertThat(defragmentiert)
			.extracting(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.usingComparatorForType(LineareReferenzTestProvider.lenientComparator, LinearReferenzierterAbschnitt.class)
			.containsExactly(
				LinearReferenzierterAbschnitt.of(0, 1.));

		assertThat(defragmentiert.stream().map(ZustaendigkeitAttribute::getVereinbarungsKennung)
			.map(Optional::get).collect(Collectors.toList()))
				.containsExactly(VereinbarungsKennung.of("456"));
	}

	@Test
	void testeDefragmentieren_fuehrungsform_kompliziertesBeispielMitEntfernenUndInterpolieren() {

		List<FuehrungsformAttribute> attribute = new ArrayList<>();
		attribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.15)
			.belagArt(BelagArt.BETON).build());

		attribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.15, 0.2)
			.belagArt(BelagArt.UNBEKANNT).build());

		attribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.4)
			.belagArt(BelagArt.ASPHALT).build());

		attribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.6)
			.belagArt(BelagArt.BETON).build());

		attribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.6, 0.68)
			.belagArt(BelagArt.UNBEKANNT).build());

		attribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.68, 1.)
			.belagArt(BelagArt.ASPHALT).build());

		List<FuehrungsformAttribute> defragmentiert = LinearReferenzierteAttribute
			.defragmentiereLinearReferenzierteAttribute(
				attribute, Laenge.of(10), Laenge.of(1.0));

		assertThat(defragmentiert)
			.extracting(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.usingComparatorForType(LineareReferenzTestProvider.lenientComparator, LinearReferenzierterAbschnitt.class)
			.containsExactly(
				LinearReferenzierterAbschnitt.of(0, 0.175),
				LinearReferenzierterAbschnitt.of(0.175, 0.4),
				LinearReferenzierterAbschnitt.of(0.4, 0.64),
				LinearReferenzierterAbschnitt.of(0.64, 1.));

		assertThat(defragmentiert.stream().map(FuehrungsformAttribute::getBelagArt))
			.containsExactly(BelagArt.BETON, BelagArt.ASPHALT, BelagArt.BETON, BelagArt.ASPHALT);
	}

	@Test
	void testeDefragmentieren_zustaendigkeit_segmentsDontCoverFullLine() {
		List<ZustaendigkeitAttribute> attributeStueckAmAnfangFehlt = new ArrayList<>();
		attributeStueckAmAnfangFehlt.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.001, 0.2)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		attributeStueckAmAnfangFehlt.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		List<ZustaendigkeitAttribute> attributeStueckAmEndeFehlt = new ArrayList<>();
		attributeStueckAmEndeFehlt.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.2)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		attributeStueckAmEndeFehlt.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.99)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		List<ZustaendigkeitAttribute> attributeMitIntersection = new ArrayList<>();
		attributeMitIntersection.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.0, 0.4)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		attributeMitIntersection.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		assertThatThrownBy(() -> LinearReferenzierteAttribute.defragmentiereLinearReferenzierteAttribute(
			attributeStueckAmAnfangFehlt, Laenge.of(10), Laenge.of(1.0))).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> LinearReferenzierteAttribute.defragmentiereLinearReferenzierteAttribute(
			attributeStueckAmEndeFehlt, Laenge.of(10), Laenge.of(1.0))).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> LinearReferenzierteAttribute.defragmentiereLinearReferenzierteAttribute(
			attributeMitIntersection, Laenge.of(10), Laenge.of(1.0))).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() -> LinearReferenzierteAttribute.defragmentiereLinearReferenzierteAttribute(
			new ArrayList<>(), Laenge.of(10), Laenge.of(1.0))).isInstanceOf(RequireViolation.class);
	}

	@Test
	public void testeGetAufLineareReferenzZugeschnitten_passtPerfektInAnfangsMetermarke() {
		// arrange
		ZustaendigkeitAttribute attribute1 = ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.5)
			.vereinbarungsKennung(VereinbarungsKennung.of("桜の子")).build();
		ZustaendigkeitAttribute attribute2 = ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("戸惑い子")).build();

		List<ZustaendigkeitAttribute> attributes = new ArrayList<>(List.of(attribute1, attribute2));

		// act
		List<ZustaendigkeitAttribute> result = LinearReferenzierteAttribute.getAufLineareReferenzZugeschnitten(
			attributes, LinearReferenzierterAbschnitt.of(0.5, 0.8));

		// assert
		assertThat(result).hasSize(3);
		assertThat(result.get(0).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("桜の子"));
		assertThat(result.get(1).getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("戸惑い子"));
		assertThat(result.get(2).getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("戸惑い子"));

		assertThat(result).extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
			.containsExactly(
				LinearReferenzierterAbschnitt.of(0, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 0.8),
				LinearReferenzierterAbschnitt.of(0.8, 1.));

		assertAttributeSindSortiertUndCovernFullLine(result);
	}

	@Test
	public void testeGetAufLineareReferenzZugeschnitten_passtPerfektInEndMetermarke() {
		// arrange
		ZustaendigkeitAttribute attribute1 = ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.5)
			.vereinbarungsKennung(VereinbarungsKennung.of("桜の子")).build();
		ZustaendigkeitAttribute attribute2 = ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("戸惑い子(guter song)")).build();

		List<ZustaendigkeitAttribute> attributes = new ArrayList<>(List.of(attribute1, attribute2));

		// act
		List<ZustaendigkeitAttribute> result = LinearReferenzierteAttribute.getAufLineareReferenzZugeschnitten(
			attributes, LinearReferenzierterAbschnitt.of(0.3, 0.5));

		// assert
		assertThat(result).hasSize(3);
		assertThat(result.get(0).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("桜の子"));
		assertThat(result.get(1).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("桜の子"));
		assertThat(result.get(2).getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("戸惑い子(guter song)"));

		assertThat(result).extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
			.containsExactly(
				LinearReferenzierterAbschnitt.of(0, 0.3),
				LinearReferenzierterAbschnitt.of(0.3, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 1.));

		assertAttributeSindSortiertUndCovernFullLine(result);
	}

	@Test
	public void testeGetAufLineareReferenzZugeschnitten_kommtExtremnahAnMetermarkeRan() {
		// arrange
		ZustaendigkeitAttribute attribute1 = ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.5)
			.vereinbarungsKennung(VereinbarungsKennung.of("桜の子")).build();
		ZustaendigkeitAttribute attribute2 = ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1.)
			.vereinbarungsKennung(VereinbarungsKennung.of("戸惑い子(guter song)")).build();

		List<ZustaendigkeitAttribute> attributes = new ArrayList<>(List.of(attribute1, attribute2));

		// act
		List<ZustaendigkeitAttribute> result = LinearReferenzierteAttribute.getAufLineareReferenzZugeschnitten(
			attributes, LinearReferenzierterAbschnitt.of(0., 0.4999));

		// assert
		assertThat(result).hasSize(3);
		assertThat(result.get(0).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("桜の子"));
		assertThat(result.get(1).getVereinbarungsKennung()).contains(VereinbarungsKennung.of("桜の子"));
		assertThat(result.get(2).getVereinbarungsKennung())
			.contains(VereinbarungsKennung.of("戸惑い子(guter song)"));

		assertThat(result).extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
			.containsExactly(
				LinearReferenzierterAbschnitt.of(0, 0.4999),
				LinearReferenzierterAbschnitt.of(0.4999, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 1.));

		assertAttributeSindSortiertUndCovernFullLine(result);
	}

	private <T extends LinearReferenzierteAttribute> void assertAttributeSindSortiertUndCovernFullLine(
		List<T> attribute) {
		List<T> sortedCopy = attribute.stream()
			.sorted(Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toList());
		assertThat(LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			sortedCopy.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()))).isTrue();
		assertThat(sortedCopy).isEqualTo(attribute);
	}
}
