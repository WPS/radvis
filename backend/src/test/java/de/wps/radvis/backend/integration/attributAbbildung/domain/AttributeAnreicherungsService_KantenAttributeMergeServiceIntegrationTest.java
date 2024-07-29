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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.linearref.LinearLocation;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisherSensitiveTest;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributProjektionsJobStatistik;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributeMergeFehlersammlung;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.Attributprojektionsbeschreibung;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenSegment;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.LineareReferenzProjektionsergebnis;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import lombok.Getter;
import lombok.Setter;

public class AttributeAnreicherungsService_KantenAttributeMergeServiceIntegrationTest
	implements RadVisDomainEventPublisherSensitiveTest {

	GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	AttributeAnreicherungsService attributeAnreicherungsService;

	KantenAttributeMergeService kantenAttributeMergeService;

	@Getter
	@Setter
	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@Mock
	AttributeProjektionsProtokollService attributeProjektionsProtokollService;

	private static final Comparator<LinearReferenzierterAbschnitt> lineareReferenzComparator = (LR1, LR2) -> {
		if (Math.abs(LR1.getVonValue() - LR2.getVonValue()) + Math.abs(LR1.getBisValue() - LR2.getBisValue()) < 0.001) {
			return 0;
		} else {
			return LinearReferenzierterAbschnitt.vonZuerst.compare(LR1, LR2);
		}
	};

	@BeforeEach
	public void setup() {
		openMocks(this);
		kantenAttributeMergeService = new KantenAttributeMergeService();
		attributeAnreicherungsService = new AttributeAnreicherungsService(kantenAttributeMergeService,
			attributeProjektionsProtokollService);
	}

	@Test
	public void testeMergeZustaendigkeitAttribute_GrundnetzAttributeLeer_ProjizierteSindGleich() {
		// arrange
		List<ZustaendigkeitAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(null)
				.unterhaltsZustaendiger(null)
				.vereinbarungsKennung(null)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1.))
				.build());

		List<ZustaendigkeitAttribute> projizierteAttribute = new ArrayList<>();
		final Verwaltungseinheit organisation2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(2L).name("test2").build();
		final Verwaltungseinheit organisation3 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(3L).name("test3").build();
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(organisation2)
				.unterhaltsZustaendiger(organisation3)
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.02, .4))
				.build());
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(organisation2)
				.unterhaltsZustaendiger(organisation3)
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.38, 1.))
				.build());

		// act
		List<ZustaendigkeitAttribute> result = attributeAnreicherungsService
			.mergeZustaendigkeitAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				new AttributeMergeFehlersammlung(1L, null, Collections.emptyList()));

		// assert
		assertThat(result).size().isEqualTo(1);
		assertThat(result.get(0).getUnterhaltsZustaendiger()).get()
			.isEqualTo(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).name("test3").build());
		assertThat(result.get(0).getBaulastTraeger()).get()
			.isEqualTo(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).name("test2").build());
		assertThat(result.get(0).getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("123"));
		assertThat(result.get(0).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 1.));
	}

	@Test
	public void testeMergeZustaendigkeitAttribute_GrundnetzAttributeLeer_AufloesbareUeberschneidung() {
		// arrange
		List<ZustaendigkeitAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(null)
				.unterhaltsZustaendiger(null)
				.vereinbarungsKennung(null)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1.))
				.build());

		List<ZustaendigkeitAttribute> projizierteAttribute = new ArrayList<>();
		final Verwaltungseinheit organisation2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L)
			.build();
		final Verwaltungseinheit organisation3 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L)
			.build();
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(organisation2)
				.unterhaltsZustaendiger(organisation3)
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.02, .4))
				.build());
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(organisation2)
				.unterhaltsZustaendiger(organisation3)
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.38, 1.))
				.build());
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(organisation2)
				.unterhaltsZustaendiger(organisation3)
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.2, .9))
				.build());
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(organisation2)
				.unterhaltsZustaendiger(organisation3)
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.6, 1.))
				.build());

		// act
		List<ZustaendigkeitAttribute> result = attributeAnreicherungsService
			.mergeZustaendigkeitAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				new AttributeMergeFehlersammlung(1L, null, Collections.emptyList()));

		// assert
		assertThat(result).size().isEqualTo(1);
		assertThat(result.get(0).getBaulastTraeger()).get()
			.isEqualTo(organisation2);
		assertThat(result.get(0).getUnterhaltsZustaendiger()).get()
			.isEqualTo(organisation3);
		assertThat(result.get(0).getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("123"));
		assertThat(result.get(0).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 1.));
	}

	@Test
	public void testeMergeZustaendigkeitAttribute_GrundnetzAttributeLeer_UnterschiedlicheAttribute() {
		// arrange
		List<ZustaendigkeitAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(null)
				.unterhaltsZustaendiger(null)
				.vereinbarungsKennung(null)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1.))
				.build());

		Verwaltungseinheit organisation2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L)
			.build();
		Verwaltungseinheit organisation3 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L)
			.build();
		Verwaltungseinheit organisation4 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(4L)
			.build();
		Verwaltungseinheit organisation6 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(6L)
			.build();
		List<ZustaendigkeitAttribute> projizierteAttribute = new ArrayList<>();
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(
					organisation2)
				.unterhaltsZustaendiger(
					organisation3)
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.02, .4))
				.build());
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(
					organisation4)
				.unterhaltsZustaendiger(
					organisation6)
				.vereinbarungsKennung(VereinbarungsKennung.of("456"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.38, 1.))
				.build());
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(
					organisation2)
				.unterhaltsZustaendiger(
					organisation3)
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.1, .3))
				.build());
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(
					organisation4)
				.unterhaltsZustaendiger(
					organisation6)
				.vereinbarungsKennung(VereinbarungsKennung.of("456"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.6, 1.))
				.build());

		// act
		List<ZustaendigkeitAttribute> result = attributeAnreicherungsService
			.mergeZustaendigkeitAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				new AttributeMergeFehlersammlung(1L, null, Collections.emptyList()));

		// assert
		assertThat(result).size().isEqualTo(2);
		assertThat(result.get(0).getBaulastTraeger()).get()
			.isEqualTo(organisation2);
		assertThat(result.get(0).getUnterhaltsZustaendiger()).get()
			.isEqualTo(organisation3);
		assertThat(result.get(0).getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("123"));
		assertThat(result.get(0).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.39));
		assertThat(result.get(1).getBaulastTraeger()).get()
			.isEqualTo(organisation4);
		assertThat(result.get(1).getUnterhaltsZustaendiger()).get()
			.isEqualTo(organisation6);
		assertThat(result.get(1).getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("456"));
		assertThat(result.get(1).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.39, 1.));
	}

	@Test
	void testeMergeZustaendigkeitAttribute_keineProjiziertenAttributeFuerSegment() {
		// arrange
		List<ZustaendigkeitAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(null)
				.unterhaltsZustaendiger(null)
				.vereinbarungsKennung(null)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1.))
				.build());

		List<ZustaendigkeitAttribute> projizierteAttribute = new ArrayList<>();
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(
					VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).name("test2").build())
				.unterhaltsZustaendiger(
					VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).name("test3").build())
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.02, .4))
				.build());

		// act
		List<ZustaendigkeitAttribute> result = attributeAnreicherungsService
			.mergeZustaendigkeitAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				new AttributeMergeFehlersammlung(1L, null, Collections.emptyList()));

		// assert
		assertThat(result).size().isEqualTo(2);
		assertThat(result.get(0).getBaulastTraeger()).get()
			.isEqualTo(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).name("test2")
				.build());
		assertThat(result.get(0).getUnterhaltsZustaendiger()).get()
			.isEqualTo(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).name("test3")
				.build());
		assertThat(result.get(0).getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("123"));
		assertThat(result.get(0).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.4));
		assertThat(result.get(1).getBaulastTraeger()).isEmpty();
		assertThat(result.get(1).getUnterhaltsZustaendiger()).isEmpty();
		assertThat(result.get(1).getVereinbarungsKennung()).isEmpty();
		assertThat(result.get(1).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.4, 1.));
	}

	@Test
	void testeMergeZustaendigkeitAttribute_wiederspruechlicheAttribute_fehlerGesammelt() {
		// arrange
		List<ZustaendigkeitAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(null)
				.unterhaltsZustaendiger(null)
				.vereinbarungsKennung(null)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1.))
				.build());

		List<ZustaendigkeitAttribute> projizierteAttribute = new ArrayList<>();
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
				.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
				.vereinbarungsKennung(VereinbarungsKennung.of("123"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.02, .6))
				.build());
		projizierteAttribute.add(
			ZustaendigkeitAttribute.builder()
				.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(4L).build())
				.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(6L).build())
				.vereinbarungsKennung(VereinbarungsKennung.of("456"))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.3, 1.))
				.build());

		// act
		AttributeMergeFehlersammlung fehlersammlung = new AttributeMergeFehlersammlung(1L, null,
			Collections.emptyList());
		List<ZustaendigkeitAttribute> result = attributeAnreicherungsService
			.mergeZustaendigkeitAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				fehlersammlung);

		// assert
		assertThat(result).size().isEqualTo(3);
		assertThat(result.get(0).sindAttributeGleich(projizierteAttribute.get(0))).isTrue();
		assertThat(result.get(1).sindAttributeGleich(grundnetzAttribute.get(0))).isTrue();
		assertThat(result.get(2).sindAttributeGleich(projizierteAttribute.get(1))).isTrue();
		assertThat(result).map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.3), LinearReferenzierterAbschnitt.of(0.3, 0.6),
				LinearReferenzierterAbschnitt.of(0.6, 1));

		assertThat(fehlersammlung.getExceptions()).size().isEqualTo(1);
	}

	@Test
	public void testeMergeFuehrungsformAttribute_GrundnetzAttributeLeer_UnterschiedlicheAttribute() {
		// arrange
		List<FuehrungsformAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			FuehrungsformAttribute.builder()
				.belagArt(BelagArt.ASPHALT)
				.build());

		List<FuehrungsformAttribute> projizierteAttribute = new ArrayList<>();
		projizierteAttribute.add( // Attribute A
			FuehrungsformAttribute.builder()
				.belagArt(BelagArt.ASPHALT)
				.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.breite(null)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.parkenTyp(KfzParkenTyp.SCHRAEG_PARKEN)
				.parkenForm(KfzParkenForm.PARKBUCHTEN)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.02, .4))
				.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
				.build());
		projizierteAttribute.add( // Attribute B
			FuehrungsformAttribute.builder()
				.belagArt(BelagArt.ASPHALT)
				.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.breite(Laenge.of(3.4)) // Unterschied zwischen A und B
				.parkenTyp(KfzParkenTyp.SCHRAEG_PARKEN)
				.parkenForm(KfzParkenForm.PARKBUCHTEN)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.38, 1.))
				.build());
		projizierteAttribute.add( // Attribute A
			FuehrungsformAttribute.builder()
				.belagArt(BelagArt.ASPHALT)
				.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.breite(null)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.parkenTyp(KfzParkenTyp.SCHRAEG_PARKEN)
				.parkenForm(KfzParkenForm.PARKBUCHTEN)
				.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.1, .3))
				.build());
		projizierteAttribute.add( // Attribute B
			FuehrungsformAttribute.builder()
				.belagArt(BelagArt.ASPHALT)
				.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.breite(Laenge.of(3.4))
				.parkenTyp(KfzParkenTyp.SCHRAEG_PARKEN)
				.parkenForm(KfzParkenForm.PARKBUCHTEN)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.6, 1.))
				.build());

		// act
		List<FuehrungsformAttribute> result = attributeAnreicherungsService
			.mergeFuehrungsformAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				new AttributeMergeFehlersammlung(1L, null, Collections.emptyList()), "");

		// assert
		assertThat(result).size().isEqualTo(2);
		assertThat(result.get(0).getBelagArt()).isEqualTo(BelagArt.ASPHALT);
		assertThat(result.get(0).getBordstein()).isEqualTo(Bordstein.KEINE_ABSENKUNG);
		assertThat(result.get(0).getBreite()).isEmpty();
		assertThat(result.get(0).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BEGEGNUNBSZONE);
		assertThat(result.get(0).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.39));
		assertThat(result.get(0).getParkenForm()).isEqualTo(KfzParkenForm.PARKBUCHTEN);
		assertThat(result.get(0).getParkenTyp()).isEqualTo(KfzParkenTyp.SCHRAEG_PARKEN);
		assertThat(result.get(1).getBelagArt()).isEqualTo(BelagArt.ASPHALT);
		assertThat(result.get(1).getBordstein()).isEqualTo(Bordstein.KEINE_ABSENKUNG);
		assertThat(result.get(1).getBreite()).isPresent();
		assertThat(result.get(1).getBreite()).get().isEqualTo(Laenge.of(3.4));
		assertThat(result.get(1).getParkenForm()).isEqualTo(KfzParkenForm.PARKBUCHTEN);
		assertThat(result.get(1).getParkenTyp()).isEqualTo(KfzParkenTyp.SCHRAEG_PARKEN);
		assertThat(result.get(1).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BEGEGNUNBSZONE);
		assertThat(result.get(1).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.39, 1.));
	}

	@Test
	void testeMergeFuehrungsformAttribute_keineProjiziertenAttributeFuerSegment() {
		// arrange
		List<FuehrungsformAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			FuehrungsformAttribute.builder()
				.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
				.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
				.build());

		List<FuehrungsformAttribute> projizierteAttribute = new ArrayList<>();

		projizierteAttribute.add(
			FuehrungsformAttribute.builder()
				.parkenTyp(KfzParkenTyp.PARKEN_VERBOTEN)
				.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_UNMARKIERT)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.02, .4))
				.build());

		// act
		List<FuehrungsformAttribute> result = attributeAnreicherungsService
			.mergeFuehrungsformAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				new AttributeMergeFehlersammlung(1L, null, Collections.emptyList()), "");

		// assert
		assertThat(result.get(0).getBreite()).isEmpty();
		assertThat(result.get(0).getBelagArt()).isEqualTo(BelagArt.UNBEKANNT);
		assertThat(result.get(0).getBordstein()).isEqualTo(Bordstein.UNBEKANNT);
		assertThat(result.get(0).getParkenForm()).isEqualTo(KfzParkenForm.FAHRBAHNPARKEN_UNMARKIERT);
		assertThat(result.get(0).getParkenTyp())
			.isEqualTo(KfzParkenTyp.PARKEN_VERBOTEN);
		assertThat(result.get(0).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BEGEGNUNBSZONE);
		assertThat(result.get(0).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.4));
		assertThat(result.get(1).getBelagArt()).isEqualTo(BelagArt.UNBEKANNT);
		assertThat(result.get(1).getBordstein()).isEqualTo(Bordstein.UNBEKANNT);
		assertThat(result.get(1).getBreite()).isEmpty();
		assertThat(result.get(1).getParkenTyp()).isEqualTo(KfzParkenTyp.LAENGS_PARKEN);
		assertThat(result.get(1).getParkenForm()).isEqualTo(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT);
		assertThat(result.get(1).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.UNBEKANNT);
		assertThat(result.get(1).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.4, 1.));
	}

	@Test
	public void testeMergeFuehrungsformAttribute_GrundnetzAttributeLeer_AufloesbareUeberschneidung() {
		// arrange
		List<FuehrungsformAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			FuehrungsformAttribute.builder()
				.build());

		List<FuehrungsformAttribute> projizierteAttribute = new ArrayList<>();
		projizierteAttribute.add(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.build());
		projizierteAttribute.add(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.build());
		projizierteAttribute.add(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.build());
		projizierteAttribute.add(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.build());

		// act
		List<FuehrungsformAttribute> result = attributeAnreicherungsService
			.mergeFuehrungsformAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				new AttributeMergeFehlersammlung(1L, null, Collections.emptyList()), "");

		// assert
		assertThat(result).size().isEqualTo(1);
		assertThat(result.get(0).getBelagArt()).isEqualTo(BelagArt.UNBEKANNT);
		assertThat(result.get(0).getBordstein()).isEqualTo(Bordstein.UNBEKANNT);
		assertThat(result.get(0).getBreite()).isEmpty();
		assertThat(result.get(0).getParkenForm()).isEqualTo(KfzParkenForm.UNBEKANNT);
		assertThat(result.get(0).getParkenTyp())
			.isEqualTo(KfzParkenTyp.UNBEKANNT);
		assertThat(result.get(0).getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BEGEGNUNBSZONE);
		assertThat(result.get(0).getLinearReferenzierterAbschnitt()).usingComparator(lineareReferenzComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 1.));
	}

	@Test
	public void mergeFuehrungsformAttribute_wiederspruechlicheAttribute_fehlerGesammelt() {
		// arrange
		List<FuehrungsformAttribute> grundnetzAttribute = new ArrayList<>();
		grundnetzAttribute.add(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1.))
				.build());

		List<FuehrungsformAttribute> projizierteAttribute = new ArrayList<>();

		projizierteAttribute.add(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.02, .6))
				.build());
		projizierteAttribute.add(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_FORST)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.3, 1.))
				.build());

		// act
		AttributeMergeFehlersammlung fehlersammlung = new AttributeMergeFehlersammlung(1L, null,
			Collections.emptyList());
		List<FuehrungsformAttribute> result = attributeAnreicherungsService
			.mergeFuehrungsformAttribute(
				grundnetzAttribute,
				projizierteAttribute,
				QuellSystem.RadNETZ,
				30.,
				fehlersammlung,
				"");

		// assert
		assertThat(result).size().isEqualTo(3);
		assertThat(result.get(0).sindAttributeGleich(projizierteAttribute.get(0))).isTrue();
		assertThat(result.get(1).sindAttributeGleich(grundnetzAttribute.get(0))).isTrue();
		assertThat(result.get(2).sindAttributeGleich(projizierteAttribute.get(1))).isTrue();
		assertThat(result).map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.3), LinearReferenzierterAbschnitt.of(0.3, 0.6),
				LinearReferenzierterAbschnitt.of(0.6, 1));

		assertThat(fehlersammlung.getExceptions()).size().isEqualTo(1);
	}

	@Test
	void testeInterpolation_zustaendigkeit() {

		List<ZustaendigkeitAttribute> LRAttribute = new ArrayList<>();
		LRAttribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.05, 0.2)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		LRAttribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.15, 0.4)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		LRAttribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.48, 0.6)
			.vereinbarungsKennung(VereinbarungsKennung.of("123")).build());

		LRAttribute.add(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.68, 0.95)
			.vereinbarungsKennung(VereinbarungsKennung.of("456")).build());

		attributeAnreicherungsService
			.interpoliereKleineLueckenUndKleineUeberschneidungen(LRAttribute, 0.1);

		assertThat(LRAttribute).hasSize(4);
		assertThat(LRAttribute.get(0).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.175));
		assertThat(LRAttribute.get(1).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.175, 0.44));
		assertThat(LRAttribute.get(2).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.44, 0.64));
		assertThat(LRAttribute.get(3).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.64, 1.));
	}

	@Test
	void testeInterpolation_fuehrungsform() {

		List<FuehrungsformAttribute> lRAttribute = new ArrayList<>();
		lRAttribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.05, 0.2)
			.belagArt(BelagArt.BETON).build());

		lRAttribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.15, 0.4)
			.belagArt(BelagArt.ASPHALT).build());

		lRAttribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.48, 0.6)
			.belagArt(BelagArt.BETON).build());

		lRAttribute.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0.68, 0.95)
			.belagArt(BelagArt.ASPHALT).build());

		attributeAnreicherungsService
			.interpoliereKleineLueckenUndKleineUeberschneidungen(lRAttribute, 0.1);

		assertThat(lRAttribute).hasSize(4);
		assertThat(lRAttribute.get(0).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.175));
		assertThat(lRAttribute.get(1).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.175, 0.44));
		assertThat(lRAttribute.get(2).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.44, 0.64));
		assertThat(lRAttribute.get(3).getLinearReferenzierterAbschnitt())
			.usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.64, 1.));
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_schreibtNetzklassenUndIstStandards() {
		// Arrange
		List<FuehrungsformAttribute> fuehrungsformAttributeList = new ArrayList<>();
		fuehrungsformAttributeList.add(FuehrungsformAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
			.belagArt(BelagArt.BETON).build());

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(fuehrungsformAttributeList, false))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().id(124L).build())
			.build();

		Kante projizierteKante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(fuehrungsformAttributeList, false))
			.kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
					.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
					.build())
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(kante);

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(0, 0), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(0, 0), false),
				projizierteKante, kante));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		// Act
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadNETZ,
			statistik, "");

		// Assert
		assertThat(kante.getKantenAttributGruppe().getIstStandards()).containsExactlyInAnyOrder(
			IstStandard.STARTSTANDARD_RADNETZ);
		assertThat(kante.getKantenAttributGruppe().getNetzklassen()).containsExactlyInAnyOrder(
			Netzklasse.RADNETZ_ALLTAG);
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_schreibtGeschwindigkeit() {
		// Arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitAttribute.builder().hoechstgeschwindigkeit(
				Hoechstgeschwindigkeit.MAX_9_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_20_KMH)
				.build());

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.build();

		Kante projizierteKante = KanteTestDataProvider.withDefaultValues()
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(geschwindigkeitAttribute)
					.build())
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(kante);

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(0, 0), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(0, 0), false),
				projizierteKante, kante));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		// Act
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadNETZ,
			statistik, "");

		// Assert
		assertThat(kante.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrderElementsOf(geschwindigkeitAttribute);
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_schreibtRichtung() {
		// Arrange
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppeQuellnetz = new FahrtrichtungAttributGruppe(
			Richtung.IN_RICHTUNG, false);

		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppeGrundnetz = FahrtrichtungAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		fahrtrichtungAttributGruppeGrundnetz.changeSeitenbezug(true);
		fahrtrichtungAttributGruppeGrundnetz.setRichtung(Richtung.BEIDE_RICHTUNGEN, Richtung.GEGEN_RICHTUNG);
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeGrundnetz)
			.build();

		Kante projizierteKante = KanteTestDataProvider.withDefaultValues()
			.isZweiseitig(false)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(false).build())
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeQuellnetz)
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(kante);

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(0, 0), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(0, 0), false),
				projizierteKante, kante));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		// Act
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadNETZ,
			statistik, "");

		// Assert
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.IN_RICHTUNG);
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_schreibtRichtung_Seitenbezogen() {
		// Arrange
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppeQuellnetzLinks = new FahrtrichtungAttributGruppe(
			Richtung.IN_RICHTUNG, false);
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppeQuellnetzRechts = new FahrtrichtungAttributGruppe(
			Richtung.GEGEN_RICHTUNG, false);

		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppeGrundnetz = FahrtrichtungAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		fahrtrichtungAttributGruppeGrundnetz.changeSeitenbezug(true);
		fahrtrichtungAttributGruppeGrundnetz.setRichtung(Richtung.BEIDE_RICHTUNGEN, Richtung.GEGEN_RICHTUNG);
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeGrundnetz)
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(14, 11),
				new Coordinate(16, 9),
				new Coordinate(20, 10)
			}))
			.build();

		Kante projizierteKanteWeitLinks = KanteTestDataProvider.withDefaultValues()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeQuellnetzLinks)
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(7, 15),
				new Coordinate(15, 15),
				new Coordinate(16, 15),
				new Coordinate(20, 15)
			}))
			.build();

		Kante projizierteKanteLinks = KanteTestDataProvider.withDefaultValues()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeQuellnetzLinks)
			.geometry(GeometryTestdataProvider
				.getLinestringVerschobenUmCoordinate(projizierteKanteWeitLinks.getGeometry(), 3, -2))
			.build();

		Kante projizierteKanteRechts = KanteTestDataProvider.withDefaultValues()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeQuellnetzRechts)
			.geometry(GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(kante.getGeometry(), 0, -1))
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(kante);

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, .5),
					new LinearLocation(0, 0),
					new LinearLocation(1, .5), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0.2, 1),
					new LinearLocation(0, 0),
					new LinearLocation(1, .5), false),
				projizierteKanteWeitLinks, kante));

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(
					LinearReferenzierterAbschnitt.of(0.5, 1), new LinearLocation(1, .5),
					new LinearLocation(2, 1), false),
				new LineareReferenzProjektionsergebnis(
					LinearReferenzierterAbschnitt.of(0., 0.8), new LinearLocation(1, .5),
					new LinearLocation(2, 1), false),
				projizierteKanteLinks, kante));

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0., 1),
					new LinearLocation(0, 0),
					new LinearLocation(2, 1), false),
				new LineareReferenzProjektionsergebnis(
					LinearReferenzierterAbschnitt.of(0., 1.), new LinearLocation(0, 1.),
					new LinearLocation(2, 1), false),
				projizierteKanteRechts, kante));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		// Act
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadNETZ,
			statistik, "");

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.GEGEN_RICHTUNG);
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_richtungInQuelleOhneSeitenbezug_respektiertSeitenbezogenheitInGrundnetzkante() {
		// Arrange
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppeQuellnetz = new FahrtrichtungAttributGruppe(
			Richtung.BEIDE_RICHTUNGEN, false);

		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppeGrundnetz = FahrtrichtungAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		fahrtrichtungAttributGruppeGrundnetz.changeSeitenbezug(true);
		fahrtrichtungAttributGruppeGrundnetz.setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.BEIDE_RICHTUNGEN);
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeGrundnetz)
			.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(14, 11),
				new Coordinate(16, 9),
				new Coordinate(20, 10)
			}))
			.build();

		Kante projizierteKanteRechts = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadwegeDB)
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeQuellnetz)
			.geometry(GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(kante.getGeometry(), 0, -1))
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(kante);

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0., 1),
					new LinearLocation(0, 0),
					new LinearLocation(2, 1), false),
				new LineareReferenzProjektionsergebnis(
					LinearReferenzierterAbschnitt.of(0., 1.), new LinearLocation(0, 1.),
					new LinearLocation(2, 1), false),
				projizierteKanteRechts, kante));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		// Act
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadwegeDB,
			statistik, "");

		// Assert
		assertThat(kante.isZweiseitig()).isTrue();
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.GEGEN_RICHTUNG);
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
			.isEqualTo(Richtung.BEIDE_RICHTUNGEN);
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_schreibtFuehrungsform() {
		// Arrange

		Kante grundnetzKante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().build())
			.geometry(geometryFactory.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(15, 10),
				new Coordinate(20, 10)
			}))
			.build();

		FuehrungsformAttributGruppe vonKanteLinks = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte()
			.id(2L).build();
		vonKanteLinks.replaceFuehrungsformAttribute(
			List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
				.bordstein(Bordstein.KEINE_ABSENKUNG).build()));
		FuehrungsformAttributGruppe mittig = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte()
			.id(3L).build();
		mittig.replaceFuehrungsformAttribute(
			List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
				.bordstein(Bordstein.KEINE_ABSENKUNG).build()));

		Kante projizierteKanteLinks = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(vonKanteLinks)
			.geometry(
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(grundnetzKante.getGeometry(), 0, 2.5))
			.build();

		Kante projizierteKanteMittig = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(mittig)
			.geometry(GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(grundnetzKante.getGeometry(), 0, 0))
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(
			grundnetzKante);

		// links
		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(1, 0), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(1, 0), false),
				projizierteKanteLinks, grundnetzKante));

		// mittig
		attributprojektionsbeschreibung.addSegment(new KantenSegment(
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
				new LinearLocation(0, 1), false),
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
				new LinearLocation(0, 1), false),
			projizierteKanteMittig, grundnetzKante));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();

		// Act
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadNETZ,
			statistik, "");

		// Assert
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();

		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.hasSize(1);
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)
			.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.SCHUTZSTREIFEN);
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)
			.getBordstein()).isEqualTo(Bordstein.KEINE_ABSENKUNG);

		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.isEqualTo(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_fuehrungsformInQuelleOhneSeitenbezug_respetiertSeitenbezogenheitInGrundnetzkante() {
		// Arrange
		FuehrungsformAttributGruppe grundnetzKanteFAG = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte()
			.build();
		grundnetzKanteFAG.changeSeitenbezug(true);
		grundnetzKanteFAG.replaceFuehrungsformAttribute(
			List.of(
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.6))
					.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
					.bordstein(Bordstein.KEINE_ABSENKUNG).build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 1))
					.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE)
					.bordstein(Bordstein.KOMPLETT_ABGESENKT).build()),
			List.of(
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.3))
					.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE)
					.bordstein(Bordstein.KEINE_ABSENKUNG).build(),
				FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.3, 1))
					.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
					.bordstein(Bordstein.KOMPLETT_ABGESENKT).build()));

		Kante grundnetzKante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fuehrungsformAttributGruppe(grundnetzKanteFAG)
			.geometry(geometryFactory.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(15, 10),
				new Coordinate(20, 10)
			}))
			.build();

		FuehrungsformAttributGruppe quelleFAG = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte()
			.id(2L).build();
		quelleFAG.replaceFuehrungsformAttribute(
			List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				.bordstein(Bordstein.KEINE_ABSENKUNG).build()));

		Kante projizierteKante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(quelleFAG)
			.geometry(
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(grundnetzKante.getGeometry(), 0, 2.5))
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(
			grundnetzKante);

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(1, 0), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(1, 0), false),
				projizierteKante, grundnetzKante));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();

		// Act
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadwegeDB,
			statistik, "");

		// Assert
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		// Links
		List<FuehrungsformAttribute> immutableFuehrungsformAttributeLinksSorted = grundnetzKante
			.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().stream()
			.sorted(Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toList());
		assertThat(immutableFuehrungsformAttributeLinksSorted)
			.hasSize(2);
		assertThat(immutableFuehrungsformAttributeLinksSorted)
			.extracting(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.6), LinearReferenzierterAbschnitt.of(0.6, 1));
		assertThat(immutableFuehrungsformAttributeLinksSorted)
			.extracting(FuehrungsformAttribute::getRadverkehrsfuehrung)
			.containsExactly(Radverkehrsfuehrung.SCHUTZSTREIFEN, Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE);
		assertThat(immutableFuehrungsformAttributeLinksSorted)
			.extracting(FuehrungsformAttribute::getBordstein)
			.containsExactly(Bordstein.KEINE_ABSENKUNG, Bordstein.KOMPLETT_ABGESENKT);

		// Rechts
		List<FuehrungsformAttribute> immutableFuehrungsformAttributeRechtsSorted = grundnetzKante
			.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts().stream()
			.sorted(Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toList());
		assertThat(immutableFuehrungsformAttributeRechtsSorted)
			.hasSize(2);
		assertThat(immutableFuehrungsformAttributeRechtsSorted)
			.extracting(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.3), LinearReferenzierterAbschnitt.of(0.3, 1));
		assertThat(immutableFuehrungsformAttributeRechtsSorted)
			.extracting(FuehrungsformAttribute::getRadverkehrsfuehrung)
			.containsExactly(Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE, Radverkehrsfuehrung.SCHUTZSTREIFEN);
		assertThat(immutableFuehrungsformAttributeRechtsSorted)
			.extracting(FuehrungsformAttribute::getBordstein)
			.containsExactly(Bordstein.KEINE_ABSENKUNG, Bordstein.KOMPLETT_ABGESENKT);
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_schreibtFuehrungsform_OhneSeitenbezug() {
		// Arrange

		Kante grundnetzKante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().build())
			.geometry(geometryFactory.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(15, 10),
				new Coordinate(20, 10)
			}))
			.build();

		FuehrungsformAttributGruppe vonKanteLinks = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte()
			.id(2L).build();
		vonKanteLinks.replaceFuehrungsformAttribute(
			List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
				.bordstein(Bordstein.KEINE_ABSENKUNG).build()));
		FuehrungsformAttributGruppe vonKanteRechts = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte()
			.id(3L).build();
		vonKanteRechts.replaceFuehrungsformAttribute(
			List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				.bordstein(Bordstein.KEINE_ABSENKUNG).build()));
		FuehrungsformAttributGruppe mittig = FuehrungsformAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte()
			.id(3L).build();
		mittig.replaceFuehrungsformAttribute(
			List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.bordstein(Bordstein.KEINE_ABSENKUNG).build()));

		Kante projizierteKanteLinks = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(vonKanteLinks)
			.geometry(GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(grundnetzKante.getGeometry(), 0, 5))
			.build();

		Kante projizierteKanteRechts = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(vonKanteRechts)
			.geometry(GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(grundnetzKante.getGeometry(), 0, -5))
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(
			grundnetzKante);

		// links
		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(1, 0), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(1, 0), false),
				projizierteKanteLinks, grundnetzKante));

		// rechts
		attributprojektionsbeschreibung.addSegment(new KantenSegment(
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
				new LinearLocation(0, 1), false),
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
				new LinearLocation(0, 1), false),
			projizierteKanteRechts, grundnetzKante));

		// TODO: Teil von RAD-1708
		// mittig
		//		attributprojektionsbeschreibung.addSegment(new KantenSegment(
		//			new LineareReferenzProjektionsergebnis(LineareReferenz.of(0, 1), new LinearLocation(0, 0),
		//				new LinearLocation(0, 1), false),
		//			new LineareReferenzProjektionsergebnis(LineareReferenz.of(0, 1), new LinearLocation(0, 0),
		//				new LinearLocation(0, 1), false),
		//			projizierteKanteMittig, grundnetzKante
		//		));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();

		// Act
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadNETZ,
			statistik, "");

		// Assert
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.hasSize(1);
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)
			.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.SCHUTZSTREIFEN);
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)
			.getBordstein()).isEqualTo(Bordstein.KEINE_ABSENKUNG);

		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.hasSize(1);
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0)
			.getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG);
		assertThat(grundnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0)
			.getBordstein()).isEqualTo(Bordstein.KEINE_ABSENKUNG);
	}

	@Test
	public void testReichereGrundnetzKantenMitAttributenAn_schreibtKantenattribute() {
		// Arrange
		KantenAttribute kantenAttribute = KantenAttribute.builder()
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.GEWERBEGEBIET)
			.wegeNiveau(WegeNiveau.GEHWEG)
			.build();

		Kante kante = KanteTestDataProvider.withDefaultValues().build();

		Kante projizierteKante = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(kantenAttribute).build())
			.build();

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(kante);

		attributprojektionsbeschreibung.addSegment(
			new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(0, 1), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 1), new LinearLocation(0, 0),
					new LinearLocation(0, 1), false),
				projizierteKante, kante));

		List<Attributprojektionsbeschreibung> attributprojektionsbeschreibungList = new ArrayList<>();
		attributprojektionsbeschreibungList.add(attributprojektionsbeschreibung);

		// Act
		AttributProjektionsJobStatistik statistik = new AttributProjektionsJobStatistik();
		statistik.reset();
		attributeAnreicherungsService.reichereGrundnetzKantenMitAttributenAn(
			attributprojektionsbeschreibungList,
			QuellSystem.RadNETZ,
			statistik, "");

		// Assert
		assertThat(kante.getKantenAttributGruppe().getKantenAttribute()).isEqualTo(kantenAttribute);
	}
}
