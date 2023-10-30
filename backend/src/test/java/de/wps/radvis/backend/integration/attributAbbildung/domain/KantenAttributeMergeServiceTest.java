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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.MehrdeutigeLinearReferenzierteAttributeException;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class KantenAttributeMergeServiceTest {

	KantenAttributeMergeService kantenAttributeMergeService;

	@BeforeEach
	public void setUp() {
		kantenAttributeMergeService = new KantenAttributeMergeService();
	}

	@Nested
	class RadNetzAttributeMergeTest {
		@Test
		void testMerge_ZustaendigkeitAttribute_Radnetz_MergeIdentischeRadNetzAttribute()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			ZustaendigkeitAttribute grundnetzAttribute = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, 1),
				null, null, null, VereinbarungsKennung.of("123"));
			ZustaendigkeitAttribute radNetzAttribute1 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, .7),
				null, null, null, VereinbarungsKennung.of("456"));
			ZustaendigkeitAttribute radNetzAttribute2 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0.1, .9),
				null, null, null, VereinbarungsKennung.of("456"));
			// act
			ZustaendigkeitAttribute mergedAttribute = kantenAttributeMergeService
				.mergeZustaendigkeitAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(radNetzAttribute1, radNetzAttribute2),
					QuellSystem.RadNETZ);

			// assert
			assertThat(mergedAttribute.getUnterhaltsZustaendiger()).isEmpty();
			assertThat(mergedAttribute.getErhaltsZustaendiger()).isEmpty();
			assertThat(mergedAttribute.getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("456"));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_ZustaendigkeitAttribute_Radnetz_MergeMitUnbekanntenRadNetzAttribute()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			ZustaendigkeitAttribute grundnetzAttribute = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, 1),
				null, null, null, VereinbarungsKennung.of("123"));
			final Verwaltungseinheit organisation2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.id(2L).build();
			final Verwaltungseinheit organisation3 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.id(3L).build();
			ZustaendigkeitAttribute radNetzAttribute1 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, .7),
				organisation2, null, null, VereinbarungsKennung.of("456"));
			ZustaendigkeitAttribute radNetzAttribute2 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0.1, .9),
				null, organisation3, organisation2, null);
			// act
			ZustaendigkeitAttribute mergedAttribute = kantenAttributeMergeService
				.mergeZustaendigkeitAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(radNetzAttribute1, radNetzAttribute2),
					QuellSystem.RadNETZ);

			// assert
			assertThat(mergedAttribute.getUnterhaltsZustaendiger()).get()
				.isEqualTo(organisation3);
			assertThat(mergedAttribute.getErhaltsZustaendiger()).get()
				.isEqualTo(organisation2);
			assertThat(mergedAttribute.getBaulastTraeger()).get()
				.isEqualTo(organisation2);
			assertThat(mergedAttribute.getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("456"));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_ZustaendigkeitAttribute_Radnetz_NichtMergbarWirftException() {
			// arrange
			ZustaendigkeitAttribute grundnetzAttribute = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, 1),
				null, null, null, VereinbarungsKennung.of("123"));
			ZustaendigkeitAttribute radNetzAttribute1 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, .7),
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build(), null, null,
				VereinbarungsKennung.of("456"));
			ZustaendigkeitAttribute radNetzAttribute2 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0.1, .5),
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build(), null, null,
				VereinbarungsKennung.of("456"));

			// act + assert
			assertThrows(MehrdeutigeLinearReferenzierteAttributeException.class, () -> kantenAttributeMergeService
				.mergeZustaendigkeitAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(radNetzAttribute1, radNetzAttribute2), QuellSystem.RadNETZ));
		}

		@Test
		void testMerge_FuehrungsformAttribute_Radnetz() throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			FuehrungsformAttribute grundnetzAttribute = FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1.))
				.build();
			FuehrungsformAttribute radNetzAttribute = FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., .7))
				.belagArt(BelagArt.ASPHALT)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
				.parkenForm(KfzParkenForm.GEHWEGPARKEN_MARKIERT)
				.breite(Laenge.of(4.6))
				.build();

			// act
			FuehrungsformAttribute mergedAttribute = kantenAttributeMergeService
				.mergeFuehrungsformAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(radNetzAttribute),
					QuellSystem.RadNETZ);

			// assert
			assertThat(mergedAttribute.getBelagArt())
				.isEqualTo(BelagArt.ASPHALT);
			assertThat(mergedAttribute.getBordstein())
				.isEqualTo(Bordstein.KEINE_ABSENKUNG);
			assertThat(mergedAttribute.getRadverkehrsfuehrung())
				.isEqualTo(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG);
			assertThat(mergedAttribute.getParkenTyp()).isEqualTo(KfzParkenTyp.LAENGS_PARKEN);
			assertThat(mergedAttribute.getParkenForm()).isEqualTo(KfzParkenForm.GEHWEGPARKEN_MARKIERT);
			assertThat(mergedAttribute.getBreite()).get().isEqualTo(Laenge.of(4.6));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_FuehrungsformAttribute_Radnetz_MergeRadNetzIdentischeAttribute()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			FuehrungsformAttribute grundnetzAttribute = FuehrungsformAttribute.builder()
				.build();
			FuehrungsformAttribute radNetzAttribute1 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(0., .7),
				BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.NEUWERTIG,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.UNBEKANNT,
				Laenge.of(4.6),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			);
			FuehrungsformAttribute radNetzAttribute2 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(.1, .9),
				BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.NEUWERTIG,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.UNBEKANNT,
				Laenge.of(4.6),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			);
			// act
			FuehrungsformAttribute mergedAttribute = kantenAttributeMergeService
				.mergeFuehrungsformAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(radNetzAttribute1, radNetzAttribute2), QuellSystem.RadNETZ);

			// assert
			assertThat(mergedAttribute.getBelagArt())
				.isEqualTo(BelagArt.ASPHALT);
			assertThat(mergedAttribute.getBordstein())
				.isEqualTo(Bordstein.KEINE_ABSENKUNG);
			assertThat(mergedAttribute.getOberflaechenbeschaffenheit()).isEqualTo(Oberflaechenbeschaffenheit.NEUWERTIG);
			assertThat(mergedAttribute.getRadverkehrsfuehrung())
				.isEqualTo(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND);
			assertThat(mergedAttribute.getParkenTyp()).isEqualTo(KfzParkenTyp.LAENGS_PARKEN);
			assertThat(mergedAttribute.getParkenForm()).isEqualTo(KfzParkenForm.UNBEKANNT);
			assertThat(mergedAttribute.getBreite()).get().isEqualTo(Laenge.of(4.6));
			assertThat(mergedAttribute.getBenutzungspflicht()).isEqualTo(Benutzungspflicht.VORHANDEN);
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_FuehrungsformAttribute_Radnetz_MergeRadNetzMitUnbekanntenAttributen()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			FuehrungsformAttribute grundnetzAttribute = FuehrungsformAttribute.builder()
				.build();
			FuehrungsformAttribute radNetzAttribute1 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(0., .7),
				BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.NEUWERTIG,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.UNBEKANNT,
				Laenge.of(4.6),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			);
			FuehrungsformAttribute radNetzAttribute2 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(.1, .9),
				BelagArt.UNBEKANNT,
				Oberflaechenbeschaffenheit.UNBEKANNT,
				Bordstein.UNBEKANNT,
				Radverkehrsfuehrung.UNBEKANNT,
				KfzParkenTyp.UNBEKANNT,
				KfzParkenForm.GEHWEGPARKEN_MARKIERT,
				null,
				Benutzungspflicht.UNBEKANNT,
				null,
				null,
				null,
				null,
				null,
				null
			);

			// act
			FuehrungsformAttribute mergedAttribute = kantenAttributeMergeService
				.mergeFuehrungsformAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(radNetzAttribute1, radNetzAttribute2), QuellSystem.RadNETZ);

			// assert
			assertThat(mergedAttribute.getBelagArt())
				.isEqualTo(BelagArt.ASPHALT);
			assertThat(mergedAttribute.getBordstein())
				.isEqualTo(Bordstein.KEINE_ABSENKUNG);
			assertThat(mergedAttribute.getOberflaechenbeschaffenheit()).isEqualTo(Oberflaechenbeschaffenheit.NEUWERTIG);
			assertThat(mergedAttribute.getRadverkehrsfuehrung())
				.isEqualTo(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND);
			assertThat(mergedAttribute.getParkenTyp()).isEqualTo(KfzParkenTyp.LAENGS_PARKEN);
			assertThat(mergedAttribute.getParkenForm()).isEqualTo(KfzParkenForm.GEHWEGPARKEN_MARKIERT);
			assertThat(mergedAttribute.getBreite()).get().isEqualTo(Laenge.of(4.6));
			assertThat(mergedAttribute.getBenutzungspflicht()).isEqualTo(Benutzungspflicht.VORHANDEN);
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_FuehrungsformAttribute_Radnetz_MehrdeutigWirftException() {
			// arrange
			FuehrungsformAttribute grundnetzAttribute = FuehrungsformAttribute.builder()
				.build();
			FuehrungsformAttribute radNetzAttribute1 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(0., .7),
				BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.UNBEKANNT,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT,
				KfzParkenForm.UNBEKANNT,
				Laenge.of(4.6),
				Benutzungspflicht.UNBEKANNT,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			);
			FuehrungsformAttribute radNetzAttribute2 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(0., .7),
				BelagArt.BETON,
				Oberflaechenbeschaffenheit.UNBEKANNT,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT,
				KfzParkenForm.GEHWEGPARKEN_MARKIERT,
				Laenge.of(4.6),
				Benutzungspflicht.UNBEKANNT,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			);
			// act
			assertThrows(MehrdeutigeLinearReferenzierteAttributeException.class, () -> kantenAttributeMergeService
				.mergeFuehrungsformAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(radNetzAttribute1, radNetzAttribute2), QuellSystem.RadNETZ));

		}

		@Test
		void testMerge_GrundnetzUndRadnetz() {
			// arrange
			KantenAttribute grundnetzAttribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.umfeld(Umfeld.UNBEKANNT)
				.strassenName(StrassenName.of("Entenhausen"))
				.strassenNummer(StrassenNummer.of("420"))
				.build();
			KantenAttribute radNetzAttribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.kommentar(Kommentar.of("Watch Liz and the Blue Bird you cowards"))
				.wegeNiveau(WegeNiveau.FAHRBAHN)
				.gemeinde(
					VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).name("LustigeOrganisation")
						.build())
				.dtvFussverkehr(VerkehrStaerke.of(3))
				.dtvRadverkehr(VerkehrStaerke.of(2))
				.dtvPkw(VerkehrStaerke.of(1))
				.sv(VerkehrStaerke.of(4))
				.strassenName(null)
				.strassenNummer(null)
				.laengeManuellErfasst(Laenge.of(9000))
				.beleuchtung(Beleuchtung.VORHANDEN)
				.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
				.umfeld(Umfeld.GEWERBEGEBIET)
				.build();

			// act
			KantenAttribute mergedAttribute = kantenAttributeMergeService
				.mergeKantenAttribute(grundnetzAttribute, radNetzAttribute, QuellSystem.RadNETZ);

			// assert
			assertThat(mergedAttribute.getDtvFussverkehr()).contains(VerkehrStaerke.of(3));
			assertThat(mergedAttribute.getWegeNiveau()).get().isEqualTo(WegeNiveau.FAHRBAHN);
			assertThat(mergedAttribute.getDtvFussverkehr()).get().isEqualTo(VerkehrStaerke.of(3));
			assertThat(mergedAttribute.getDtvRadverkehr()).get().isEqualTo(VerkehrStaerke.of(2));
			assertThat(mergedAttribute.getDtvPkw()).get().isEqualTo(VerkehrStaerke.of(1));
			assertThat(mergedAttribute.getSv()).get().isEqualTo(VerkehrStaerke.of(4));
			assertThat(mergedAttribute.getLaengeManuellErfasst()).get().isEqualTo(Laenge.of(9000));
			assertThat(mergedAttribute.getKommentar()).get()
				.isEqualTo(Kommentar.of("Watch Liz and the Blue Bird you cowards"));
			assertThat(mergedAttribute.getBeleuchtung()).isEqualTo(Beleuchtung.VORHANDEN);
			assertThat(mergedAttribute.getStrassenquerschnittRASt06())
				.isEqualTo(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE);
			assertThat(mergedAttribute.getUmfeld()).isEqualTo(Umfeld.GEWERBEGEBIET);
			assertThat(mergedAttribute.getGemeinde()).get()
				.isEqualTo(
					VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).name("LustigeOrganisation")
						.build());

			assertThat(mergedAttribute.getStrassenName()).get().isEqualTo(StrassenName.of("Entenhausen"));
			assertThat(mergedAttribute.getStrassenNummer()).get().isEqualTo(StrassenNummer.of("420"));
		}

		@Test
		void testMergeFahrtrichtung() {
			// arrange + act + assert
			assertThat(kantenAttributeMergeService
				.mergeFahrtrichtung(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG, QuellSystem.RadNETZ))
				.isEqualTo(Richtung.GEGEN_RICHTUNG);
		}

		@Test
		void testMergeNetzklassen() {
			// arrange
			Set<Netzklasse> grundnetz = new HashSet<>();
			grundnetz.add(Netzklasse.KOMMUNALNETZ_FREIZEIT);
			grundnetz.add(Netzklasse.KREISNETZ_ALLTAG);

			Set<Netzklasse> quellnetz = new HashSet<>();
			quellnetz.add(Netzklasse.RADNETZ_ALLTAG);

			// act
			Set<Netzklasse> result = kantenAttributeMergeService
				.mergeNetzklassen(grundnetz, quellnetz, QuellSystem.RadNETZ);

			// assert
			assertThat(result)
				.containsExactlyInAnyOrderElementsOf(
					Stream.of(grundnetz, quellnetz).flatMap(Collection::stream).collect(
						Collectors.toSet()));
		}

		@Test
		void testMergeIstStandard() {
			// arrange
			Set<IstStandard> grundnetz = new HashSet<>();
			grundnetz.add(IstStandard.BASISSTANDARD);
			grundnetz.add(IstStandard.RADSCHNELLVERBINDUNG);

			Set<IstStandard> quellnetz = new HashSet<>();
			quellnetz.add(IstStandard.STARTSTANDARD_RADNETZ);

			// act
			Set<IstStandard> result = kantenAttributeMergeService
				.mergeIstStandards(grundnetz, quellnetz, QuellSystem.RadNETZ);

			// assert
			assertThat(result)
				.containsExactlyInAnyOrderElementsOf(
					Stream.of(grundnetz, quellnetz).flatMap(Collection::stream).collect(
						Collectors.toSet()));
		}
	}

	@Nested
	class RadwegeDBMergeTest {
		@Test
		void testeMergeKantenAttributeRadwegeDB() {
			KantenAttribute grundnetzAttribute = KantenAttribute.builder()
				.strassenName(StrassenName.of("Entenhausen"))
				.strassenNummer(StrassenNummer.of("420"))
				.beleuchtung(Beleuchtung.NICHT_VORHANDEN)
				.build();

			KantenAttribute quellnetzAttribute = KantenAttribute.builder()
				.kommentar(Kommentar.of("Watch Liz and the Blue Bird you cowards"))
				.wegeNiveau(WegeNiveau.FAHRBAHN)
				.gemeinde(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
				.dtvFussverkehr(VerkehrStaerke.of(3))
				.dtvRadverkehr(VerkehrStaerke.of(2))
				.dtvPkw(VerkehrStaerke.of(1))
				.sv(VerkehrStaerke.of(4))
				.strassenName(null)
				.strassenNummer(null)
				.laengeManuellErfasst(Laenge.of(9000))
				.beleuchtung(Beleuchtung.VORHANDEN) // stimmt nicht mit Wert auf Grundnetz überein
				.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
				.umfeld(Umfeld.GEWERBEGEBIET).build();

			KantenAttribute mergedAttribute = kantenAttributeMergeService
				.mergeKantenAttribute(grundnetzAttribute, quellnetzAttribute, QuellSystem.RadwegeDB);

			// assert
			assertThat(mergedAttribute.getDtvFussverkehr()).contains(VerkehrStaerke.of(3));
			assertThat(mergedAttribute.getWegeNiveau()).get().isEqualTo(WegeNiveau.FAHRBAHN);
			assertThat(mergedAttribute.getDtvFussverkehr()).get().isEqualTo(VerkehrStaerke.of(3));
			assertThat(mergedAttribute.getDtvRadverkehr()).get().isEqualTo(VerkehrStaerke.of(2));
			assertThat(mergedAttribute.getDtvPkw()).get().isEqualTo(VerkehrStaerke.of(1));
			assertThat(mergedAttribute.getSv()).get().isEqualTo(VerkehrStaerke.of(4));
			assertThat(mergedAttribute.getLaengeManuellErfasst()).get().isEqualTo(Laenge.of(9000));
			assertThat(mergedAttribute.getKommentar()).get()
				.isEqualTo(Kommentar.of("Watch Liz and the Blue Bird you cowards"));
			assertThat(mergedAttribute.getBeleuchtung()).isEqualTo(Beleuchtung.NICHT_VORHANDEN); // Grundnetzwert
			assertThat(mergedAttribute.getStrassenquerschnittRASt06())
				.isEqualTo(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE);
			assertThat(mergedAttribute.getUmfeld()).isEqualTo(Umfeld.GEWERBEGEBIET);
			assertThat(mergedAttribute.getGemeinde()).get()
				.isEqualTo(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build());
			assertThat(mergedAttribute.getStrassenName()).get().isEqualTo(StrassenName.of("Entenhausen"));
			assertThat(mergedAttribute.getStrassenNummer()).get().isEqualTo(StrassenNummer.of("420"));
		}

		@Test
		void testMerge_ZustaendigkeitAttribute_MergeIdentischeAttribute()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L)
				.build();
			ZustaendigkeitAttribute grundnetzAttribute = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, 1),
				null, null, null, VereinbarungsKennung.of("123"));
			ZustaendigkeitAttribute quellnetzAttribute1 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, .7),
				null, organisation, null, VereinbarungsKennung.of("456"));
			ZustaendigkeitAttribute quellnetzAttribute2 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0.1, .9),
				null, organisation, null, VereinbarungsKennung.of("456"));
			// act
			ZustaendigkeitAttribute mergedAttribute = kantenAttributeMergeService
				.mergeZustaendigkeitAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(quellnetzAttribute1, quellnetzAttribute2),
					QuellSystem.RadwegeDB);

			// assert
			assertThat(mergedAttribute.getUnterhaltsZustaendiger()).contains(organisation);
			assertThat(mergedAttribute.getBaulastTraeger()).isEmpty();
			assertThat(mergedAttribute.getErhaltsZustaendiger()).isEmpty();
			assertThat(mergedAttribute.getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("123"));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_ZustaendigkeitAttribute_MergeUnbekannteAttribute()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L)
				.build();
			ZustaendigkeitAttribute grundnetzAttribute = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, 1),
				null, null, null, VereinbarungsKennung.of("123"));
			ZustaendigkeitAttribute quellnetzAttribute1 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, .7),
				organisation, organisation, null, null);
			ZustaendigkeitAttribute quellnetzAttribute2 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0.1, .9),
				null, null, organisation, VereinbarungsKennung.of("456"));
			// act
			ZustaendigkeitAttribute mergedAttribute = kantenAttributeMergeService
				.mergeZustaendigkeitAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(quellnetzAttribute1, quellnetzAttribute2),
					QuellSystem.RadwegeDB);

			// assert
			assertThat(mergedAttribute.getUnterhaltsZustaendiger()).contains(organisation);
			assertThat(mergedAttribute.getBaulastTraeger()).contains(organisation);
			assertThat(mergedAttribute.getErhaltsZustaendiger()).contains(organisation);
			assertThat(mergedAttribute.getVereinbarungsKennung()).get().isEqualTo(VereinbarungsKennung.of("123"));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_ZustaendigkeitAttribute_Radnetz_NichtMergbarWirftException() {
			// arrange
			ZustaendigkeitAttribute grundnetzAttribute = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, 1),
				null, null, null, VereinbarungsKennung.of("123"));
			ZustaendigkeitAttribute quellnetzAttribute1 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, .7),
				null, VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), null,
				VereinbarungsKennung.of("456"));
			ZustaendigkeitAttribute quellnetzAttribute2 = new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0.1, .9),
				null, VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), null,
				VereinbarungsKennung.of("789"));
			// act

			// act + assert
			assertThrows(MehrdeutigeLinearReferenzierteAttributeException.class, () -> kantenAttributeMergeService
				.mergeZustaendigkeitAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(quellnetzAttribute1, quellnetzAttribute2), QuellSystem.RadwegeDB));
		}

		@Test
		void testMerge_FuehrungsformAttribute_MergeIdentischeAttribute()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			FuehrungsformAttribute grundnetzAttribute = FuehrungsformAttribute.builder()
				.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
				.parkenForm(KfzParkenForm.GEHWEGPARKEN_MARKIERT)
				.build();
			FuehrungsformAttribute quellnetzAttribute1 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(0., .7),
				BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.UNBEKANNT,
				Laenge.of(4.6),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			);
			FuehrungsformAttribute quellnetzAttribute2 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(.1, .9),
				BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.UNBEKANNT,
				Laenge.of(4.6),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			);
			// act
			FuehrungsformAttribute mergedAttribute = kantenAttributeMergeService
				.mergeFuehrungsformAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(quellnetzAttribute1, quellnetzAttribute2), QuellSystem.RadwegeDB);

			// assert
			assertThat(mergedAttribute.getBelagArt())
				.isEqualTo(BelagArt.ASPHALT);
			assertThat(mergedAttribute.getBordstein())
				.isEqualTo(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER);
			assertThat(mergedAttribute.getOberflaechenbeschaffenheit())
				.isEqualTo(Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE);
			assertThat(mergedAttribute.getRadverkehrsfuehrung())
				.isEqualTo(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND);
			assertThat(mergedAttribute.getParkenTyp()).isEqualTo(KfzParkenTyp.LAENGS_PARKEN);
			assertThat(mergedAttribute.getParkenForm()).isEqualTo(KfzParkenForm.GEHWEGPARKEN_MARKIERT);
			assertThat(mergedAttribute.getBenutzungspflicht()).isEqualTo(Benutzungspflicht.VORHANDEN);
			assertThat(mergedAttribute.getBreite()).get().isEqualTo(Laenge.of(4.6));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_FuehrungsformAttribute_MergeUnbekannteAttribute()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			FuehrungsformAttribute grundnetzAttribute = FuehrungsformAttribute.builder()
				.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
				.parkenForm(KfzParkenForm.GEHWEGPARKEN_MARKIERT)
				.build();
			FuehrungsformAttribute quellnetzAttribute1 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(0., .7),
				BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.UNBEKANNT,
				Laenge.of(4.6),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			);
			FuehrungsformAttribute quellnetzAttribute2 = new FuehrungsformAttribute(
				LinearReferenzierterAbschnitt.of(.1, .9),
				BelagArt.UNBEKANNT,
				Oberflaechenbeschaffenheit.UNBEKANNT,
				Bordstein.UNBEKANNT,
				Radverkehrsfuehrung.UNBEKANNT,
				KfzParkenTyp.UNBEKANNT,
				KfzParkenForm.HALBES_GEHWEGPARKEN_MARKIERT,
				Laenge.of(4.6),
				Benutzungspflicht.UNBEKANNT,
				null,
				null,
				null,
				null,
				null,
				null
			);
			// act
			FuehrungsformAttribute mergedAttribute = kantenAttributeMergeService
				.mergeFuehrungsformAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(quellnetzAttribute1, quellnetzAttribute2), QuellSystem.RadwegeDB);

			// assert
			assertThat(mergedAttribute.getBelagArt())
				.isEqualTo(BelagArt.ASPHALT);
			assertThat(mergedAttribute.getBordstein())
				.isEqualTo(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER);
			assertThat(mergedAttribute.getOberflaechenbeschaffenheit())
				.isEqualTo(Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE);
			assertThat(mergedAttribute.getRadverkehrsfuehrung())
				.isEqualTo(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND);
			assertThat(mergedAttribute.getParkenTyp()).isEqualTo(KfzParkenTyp.LAENGS_PARKEN);
			assertThat(mergedAttribute.getParkenForm()).isEqualTo(KfzParkenForm.GEHWEGPARKEN_MARKIERT);
			assertThat(mergedAttribute.getBenutzungspflicht()).isEqualTo(Benutzungspflicht.VORHANDEN);
			assertThat(mergedAttribute.getBreite()).get().isEqualTo(Laenge.of(4.6));
			assertThat(mergedAttribute.getLinearReferenzierterAbschnitt()).isEqualTo(
				LinearReferenzierterAbschnitt.of(0.1, 0.5));
		}

		@Test
		void testMerge_FuehrungsformAttribute_Radnetz_MehrdeutigWirftException() {
			// arrange
			FuehrungsformAttribute grundnetzAttribute = FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				.parkenForm(KfzParkenForm.GEHWEGPARKEN_MARKIERT)
				.build();
			FuehrungsformAttribute quellnetzAttribute1 = FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., .7))
				.belagArt(BelagArt.ASPHALT)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				.build();
			FuehrungsformAttribute quellnetzAttribute2 = FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., .7))
				.belagArt(BelagArt.BETON)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				.parkenForm(KfzParkenForm.GEHWEGPARKEN_MARKIERT)
				.breite(Laenge.of(4.6))
				.build();

			//			// act
			assertThrows(MehrdeutigeLinearReferenzierteAttributeException.class, () -> kantenAttributeMergeService
				.mergeFuehrungsformAttribute(grundnetzAttribute, LinearReferenzierterAbschnitt.of(0.1, .5),
					List.of(quellnetzAttribute1, quellnetzAttribute2), QuellSystem.RadwegeDB));
		}

		@Test
		void testMergeFahrtrichtung() {
			// arrange + act + assert
			assertThat(kantenAttributeMergeService
				.mergeFahrtrichtung(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG, QuellSystem.RadwegeDB))
				.isEqualTo(Richtung.IN_RICHTUNG);
		}

		@Test
		void testMergeNetzklassen() {
			// arrange
			Set<Netzklasse> grundnetz = new HashSet<>();
			grundnetz.add(Netzklasse.RADNETZ_ALLTAG);
			grundnetz.add(Netzklasse.RADNETZ_FREIZEIT);

			Set<Netzklasse> quellnetz = new HashSet<>();
			quellnetz.add(Netzklasse.KOMMUNALNETZ_FREIZEIT);

			// act
			Set<Netzklasse> result = kantenAttributeMergeService
				.mergeNetzklassen(grundnetz, quellnetz, QuellSystem.RadwegeDB);

			// assert
			assertThat(result).isEqualTo(grundnetz);
		}

		@Test
		void testMergeIstStandard() {
			// arrange
			Set<IstStandard> grundnetz = new HashSet<>();
			grundnetz.add(IstStandard.BASISSTANDARD);
			grundnetz.add(IstStandard.RADSCHNELLVERBINDUNG);

			Set<IstStandard> quellnetz = new HashSet<>();
			quellnetz.add(IstStandard.RADVORRANGROUTEN);

			// act
			Set<IstStandard> result = kantenAttributeMergeService
				.mergeIstStandards(grundnetz, quellnetz, QuellSystem.RadwegeDB);

			// assert
			assertThat(result).isEqualTo(grundnetz);
		}

		@Test
		void mergeGeschwindigkeitAttribute_MergeRadnetz() throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			GeschwindigkeitAttribute geschwindigkeitAttributeGrundnetzKante = GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.INNERORTS)
				.hoechstgeschwindigkeit(
					Hoechstgeschwindigkeit.UNBEKANNT)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(null)
				.build();

			GeschwindigkeitAttribute geschwindigkeitAttributeRadnetz = GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.AUSSERORTS)
				.hoechstgeschwindigkeit(
					Hoechstgeschwindigkeit.MAX_20_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_30_KMH)
				.build();

			// act
			GeschwindigkeitAttribute result = kantenAttributeMergeService
				.mergeGeschwindigkeitAttribute(geschwindigkeitAttributeGrundnetzKante,
					LinearReferenzierterAbschnitt.of(0, 1),
					List.of(geschwindigkeitAttributeRadnetz),
					QuellSystem.RadNETZ);

			// assert
			assertThat(result).isEqualTo(
				GeschwindigkeitAttribute.builder()
					.ortslage(KantenOrtslage.AUSSERORTS)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_30_KMH)
					.build());
		}

		@Test
		void mergeGeschwindigkeitAttribute_MergeRadwegeDb() throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			GeschwindigkeitAttribute geschwindigkeitAttributeGrundnetzKante = GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.INNERORTS)
				.hoechstgeschwindigkeit(
					Hoechstgeschwindigkeit.MAX_9_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_9_KMH)
				.build();

			GeschwindigkeitAttribute geschwindigkeitAttributeRadnetz = GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.AUSSERORTS)
				.hoechstgeschwindigkeit(
					Hoechstgeschwindigkeit.MAX_20_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_30_KMH)
				.build();

			// act
			GeschwindigkeitAttribute result = kantenAttributeMergeService
				.mergeGeschwindigkeitAttribute(geschwindigkeitAttributeGrundnetzKante,
					LinearReferenzierterAbschnitt.of(0, 1),
					List.of(geschwindigkeitAttributeRadnetz),
					QuellSystem.RadwegeDB);

			// assert
			assertThat(result).isEqualTo(
				GeschwindigkeitAttribute.builder()
					.ortslage(KantenOrtslage.INNERORTS)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_9_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_9_KMH)
					.build());
		}

		@Test
		void mergeGeschwindigkeitAttribute_MergeRadwegeDb_nichtUebernommen()
			throws MehrdeutigeLinearReferenzierteAttributeException {
			// arrange
			GeschwindigkeitAttribute geschwindigkeitAttributeGrundnetzKante = GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.INNERORTS)
				.hoechstgeschwindigkeit(
					Hoechstgeschwindigkeit.MAX_9_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_9_KMH)
				.build();

			GeschwindigkeitAttribute geschwindigkeitAttributeRadnetz = GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.AUSSERORTS)
				.hoechstgeschwindigkeit(
					Hoechstgeschwindigkeit.MAX_20_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_30_KMH)
				.build();

			// act
			GeschwindigkeitAttribute result = kantenAttributeMergeService
				.mergeGeschwindigkeitAttribute(geschwindigkeitAttributeGrundnetzKante,
					LinearReferenzierterAbschnitt.of(0, 1),
					List.of(geschwindigkeitAttributeRadnetz),
					QuellSystem.RadwegeDB);

			// assert
			assertThat(result).isEqualTo(
				GeschwindigkeitAttribute.builder()
					.ortslage(KantenOrtslage.INNERORTS)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_9_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_9_KMH)
					.build());
		}

		@Test
		void mergeGeschwindigkeitAttribute_unzulaessigeQuelle() {
			// arrange
			GeschwindigkeitAttribute geschwindigkeitAttributeGrundnetzKante = GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.INNERORTS)
				.hoechstgeschwindigkeit(
					Hoechstgeschwindigkeit.MAX_9_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_9_KMH)
				.build();

			GeschwindigkeitAttribute geschwindigkeitAttributeRadnetz = GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.AUSSERORTS)
				.hoechstgeschwindigkeit(
					Hoechstgeschwindigkeit.MAX_20_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_30_KMH)
				.build();

			// act & assert
			assertThrows(RuntimeException.class, () -> kantenAttributeMergeService
				.mergeGeschwindigkeitAttribute(geschwindigkeitAttributeGrundnetzKante,
					LinearReferenzierterAbschnitt.of(0, 1),
					List.of(geschwindigkeitAttributeRadnetz),
					QuellSystem.DLM));

		}
	}
}
