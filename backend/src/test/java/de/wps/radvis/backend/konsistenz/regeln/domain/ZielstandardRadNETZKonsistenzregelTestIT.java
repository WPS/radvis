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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import jakarta.persistence.EntityManager;

@Tag("group7")
@EnableConfigurationProperties(value = {
	OrganisationConfigurationProperties.class
})
class ZielstandardRadNETZKonsistenzregelTestIT extends AbstractKonsistenzregelTestIT {

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	KnotenRepository knotenRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	EntityManager entityManager;

	ZielstandardRadNETZKonsistenzregel zielstandardRadNETZKonsistenzregel;

	@BeforeEach
	void setUp() {
		zielstandardRadNETZKonsistenzregel = new ZielstandardRadNETZKonsistenzregel(jdbcTemplate);
	}

	@Test
	void pruefen() {
		Knoten knoten1 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 50), QuellSystem.DLM).build());
		Knoten knoten2 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 10), QuellSystem.DLM).build());
		Knoten knoten3 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build());
		Knoten knoten4 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 100), QuellSystem.RadVis).build());

		Kante tempoGroesser50 = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
						.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.ortslage(KantenOrtslage.INNERORTS)
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_60_KMH)
								.build()))
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.fuehrungsformAttributeLinks(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.fuehrungsformAttributeRechts(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.build())
				.build());

		Kante tempo20_30_aberKeinRadNETZ = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten4, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
						.kantenAttribute(
							KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
								.sv(VerkehrStaerke.of(900))
								.build())
						.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.ortslage(KantenOrtslage.INNERORTS)
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
								.build()))
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.fuehrungsformAttributeLinks(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.fuehrungsformAttributeRechts(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.build())
				.build());

		Kante tempo30_50 = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
						.kantenAttribute(
							KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
								.dtvPkw(VerkehrStaerke.of(5500))
								.build())
						.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.ortslage(KantenOrtslage.INNERORTS)
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH)
								.build()))
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.fuehrungsformAttributeLinks(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.fuehrungsformAttributeRechts(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.build())
				.build());

		Kante tempo20_30 = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten4, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
						.kantenAttribute(
							KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
								.sv(VerkehrStaerke.of(900))
								.build())
						.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.ortslage(KantenOrtslage.INNERORTS)
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
								.build()))
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.fuehrungsformAttributeLinks(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.fuehrungsformAttributeRechts(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.build())
				.build());

		Kante tempoKleiner20 = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten4, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
						.kantenAttribute(
							KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
								.sv(VerkehrStaerke.of(1100))
								.build())
						.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.ortslage(KantenOrtslage.INNERORTS)
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_9_KMH)
								.build()))
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.fuehrungsformAttributeLinks(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.fuehrungsformAttributeRechts(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.build())
				.build());

		Kante tempoKleiner20_aberSVOK = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten3, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
						.kantenAttribute(
							KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
								.sv(VerkehrStaerke.of(900))
								.build())
						.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.ortslage(KantenOrtslage.INNERORTS)
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_9_KMH)
								.build()))
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.fuehrungsformAttributeLinks(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.fuehrungsformAttributeRechts(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.build())
				.build());

		kantenRepository.refreshNetzMaterializedViews();

		List<KonsistenzregelVerletzungsDetails> verletzungsDetails = zielstandardRadNETZKonsistenzregel.pruefen();

		assertThat(verletzungsDetails).containsExactlyInAnyOrder(
			new KonsistenzregelVerletzungsDetails(tempoGroesser50.getGeometry().getCentroid(),
				tempoGroesser50.getGeometry(),
				"Verletzung der Konsistenzregel: “Zielstandard: Wenn (Strecke Landesradfernweg oder RadNETZ) und Mischverkehr innerorts: Tempo <= 50“",
				tempoGroesser50.getId() + "_0-1"),
			new KonsistenzregelVerletzungsDetails(tempo30_50.getGeometry().getCentroid(), tempo30_50.getGeometry(),
				"Verletzung der Konsistenzregel: “Zielstandard: Wenn (Strecke Landesradfernweg oder RadNETZ) und Mischverkehr innerorts und Tempo > 30 und Tempo <= 50: DTV <= 5000 Fz/Tag und SV <= 500 Fz/Tag“",
				tempo30_50.getId() + "_0-1"),
			new KonsistenzregelVerletzungsDetails(tempo20_30.getGeometry().getCentroid(), tempo20_30.getGeometry(),
				"Verletzung der Konsistenzregel: “Zielstandard: Wenn (Strecke Landesradfernweg oder RadNETZ) und Mischverkehr innerorts und Tempo > 20 und Tempo <= 30: DTV <= 10.000 Fz/Tag und SV <= 800 Fz/Tag“",
				tempo20_30.getId() + "_0-1"),
			new KonsistenzregelVerletzungsDetails(tempoKleiner20.getGeometry().getCentroid(),
				tempoKleiner20.getGeometry(),
				"Verletzung der Konsistenzregel: “Zielstandard: Wenn (Strecke Landesradfernweg oder RadNETZ) und Mischverkehr innerorts und Tempo <= 20: DTV <= 12.000 Fz/Tag und SV <= 1.000 Fz/Tag“",
				tempoKleiner20.getId() + "_0-1"));
	}

	@Test
	void pruefen_lr() {
		Knoten knoten1 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 50), QuellSystem.DLM).build());
		Knoten knoten2 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 10), QuellSystem.DLM).build());
		Knoten knoten3 = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build());

		Kante halbAusserorts = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten1, knoten2, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.netzklassen(Set.of(Netzklasse.RADNETZ_FREIZEIT))
						.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
						.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							List.of(
								GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
									.ortslage(KantenOrtslage.INNERORTS)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_60_KMH)
									.build(),
								GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
									.ortslage(KantenOrtslage.AUSSERORTS)
									.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_80_KMH)
									.build()))
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.fuehrungsformAttributeLinks(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.fuehrungsformAttributeRechts(
							List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.radverkehrsfuehrung(Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
								.build()))
						.build())
				.build());

		Kante drittelMischverkehr = kantenRepository.save(
			KanteTestDataProvider.fromKnotenUndQuelle(knoten2, knoten3, QuellSystem.DLM)
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.ZIELSTANDARD_RADNETZ))
						.kantenAttribute(
							KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
								.dtvPkw(VerkehrStaerke.of(5500))
								.build())
						.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.geschwindigkeitAttribute(
							List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.ortslage(KantenOrtslage.INNERORTS)
								.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH)
								.build()))
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
						.fuehrungsformAttributeLinks(
							List.of(
								FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.33))
									.radverkehrsfuehrung(
										Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
									.build(),
								FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.33, 1))
									.radverkehrsfuehrung(Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR)
									.build()))
						.fuehrungsformAttributeRechts(
							List.of(
								FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.33))
									.radverkehrsfuehrung(
										Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN)
									.build(),
								FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.33, 1))
									.radverkehrsfuehrung(Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR)
									.build()))
						.build())
				.build());

		kantenRepository.refreshNetzMaterializedViews();

		List<KonsistenzregelVerletzungsDetails> verletzungsDetails = zielstandardRadNETZKonsistenzregel.pruefen();

		LineString halbAusserortsAbschnitt = GeometryTestdataProvider.getAbschnitt(halbAusserorts.getGeometry(),
			LinearReferenzierterAbschnitt.of(0, 0.5));

		assertThat(verletzungsDetails).hasSize(2);
		LineString drittelMischverkehrAbschnitt = GeometryTestdataProvider.getAbschnitt(
			drittelMischverkehr.getGeometry(), LinearReferenzierterAbschnitt.of(0, 0.33));
		assertThat(verletzungsDetails).containsExactlyInAnyOrder(
			new KonsistenzregelVerletzungsDetails(halbAusserortsAbschnitt.getCentroid(), halbAusserortsAbschnitt,
				"Verletzung der Konsistenzregel: “Zielstandard: Wenn (Strecke Landesradfernweg oder RadNETZ) und Mischverkehr innerorts: Tempo <= 50“",
				halbAusserorts.getId() + "_0-0.5"),
			new KonsistenzregelVerletzungsDetails(drittelMischverkehrAbschnitt.getCentroid(),
				drittelMischverkehrAbschnitt,
				"Verletzung der Konsistenzregel: “Zielstandard: Wenn (Strecke Landesradfernweg oder RadNETZ) und Mischverkehr innerorts und Tempo > 30 und Tempo <= 50: DTV <= 5000 Fz/Tag und SV <= 500 Fz/Tag“",
				drittelMischverkehr.getId() + "_0-0.33"));
	}

}