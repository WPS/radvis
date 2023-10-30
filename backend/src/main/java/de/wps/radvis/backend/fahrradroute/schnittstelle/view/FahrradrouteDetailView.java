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

package de.wps.radvis.backend.fahrradroute.schnittstelle.view;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class FahrradrouteDetailView {
	private final Long id;
	private final Long version;
	private final ToubizId toubizId;
	private final FahrradrouteTyp fahrradrouteTyp;
	private final FahrradrouteName name;
	private final String kurzbeschreibung;
	private final String beschreibung;
	private final Kategorie kategorie;
	private final Tourenkategorie tourenkategorie;
	private final Double laengeHauptstrecke;
	private final Double offizielleLaenge;
	private final String homepage;
	private VerwaltungseinheitView verantwortlich;
	private final String emailAnsprechpartner;
	private final String lizenz;
	private final String lizenzNamensnennung;
	private final Double anstieg;
	private final Double abstieg;
	private final String info;
	private final LocalDateTime zuletztBearbeitet;
	private final boolean veroeffentlicht;
	private final Long customProfileId;

	private final List<AbschnittsweiserKantenBezugView> kantenBezug;
	private final Geometry originalGeometrie; // nullable
	private final Geometry stuetzpunkte; // nullable
	private final Geometry geometrie; // nullable

	private final Geometry routedOrMatchedGeometry; // nullable
	private final List<Point> kehrtwenden;
	private final Geometry abweichendeSegmente; // nullable
	private final boolean abbildungDurchRouting; // default false

	private final boolean canEditAttribute;
	private final boolean canChangeVeroeffentlicht;

	private final List<FahrradrouteVarianteView> varianten;

	private final List<LinearReferenzierteProfilEigenschaftenView> profilEigenschaften;

	public FahrradrouteDetailView(Fahrradroute fahrradroute,
		boolean canEditFahrradrouteAttribute,
		boolean canChangeVeroeffentlicht) {
		this.id = fahrradroute.getId();
		this.version = fahrradroute.getVersion();
		this.toubizId = fahrradroute.getToubizId();
		this.fahrradrouteTyp = fahrradroute.getFahrradrouteTyp();
		this.name = fahrradroute.getName();
		this.kurzbeschreibung = fahrradroute.getKurzbeschreibung();
		this.beschreibung = fahrradroute.getBeschreibung();
		this.kategorie = fahrradroute.getKategorie();
		this.tourenkategorie = fahrradroute.getTourenkategorie();
		this.laengeHauptstrecke = fahrradroute.getLaengeDerHauptstrecke();
		this.offizielleLaenge = fahrradroute.getOffizielleLaenge().map(Laenge::getValue).orElse(null);
		this.homepage = fahrradroute.getHomepage();
		fahrradroute.getVerantwortlich()
			.ifPresent(organisation -> this.verantwortlich = new VerwaltungseinheitView(organisation));
		this.emailAnsprechpartner = fahrradroute.getEmailAnsprechpartner();
		this.lizenz = fahrradroute.getLizenz();
		this.lizenzNamensnennung = fahrradroute.getLizenzNamensnennung();
		this.abstieg = fahrradroute.getAbstieg().map(Hoehenunterschied::getValue).orElse(null);
		this.anstieg = fahrradroute.getAnstieg().map(Hoehenunterschied::getValue).orElse(null);
		this.info = fahrradroute.getInfo();
		this.zuletztBearbeitet = fahrradroute.getZuletztBearbeitet();
		this.veroeffentlicht = fahrradroute.isVeroeffentlicht();

		this.kantenBezug = fahrradroute.getAbschnittsweiserKantenBezug().stream()
			.map(AKB -> new AbschnittsweiserKantenBezugView(AKB.getKante()
				.getId(), AKB.getKante().getGeometry(), AKB.getLinearReferenzierterAbschnitt()))
			.collect(Collectors.toList());
		this.originalGeometrie = fahrradroute.getOriginalGeometrie().orElse(null);
		this.stuetzpunkte = fahrradroute.getStuetzpunkte().orElse(null);
		this.geometrie = fahrradroute.getNetzbezugLineString().orElse(null);
		this.customProfileId = fahrradroute.getCustomProfileId().orElse(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);

		if (fahrradroute.getFahrradroutenMatchingAndRoutingInformation().isPresent()) {
			FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation =
				fahrradroute.getFahrradroutenMatchingAndRoutingInformation().get();
			this.routedOrMatchedGeometry = fahrradroutenMatchingAndRoutingInformation
				.getRoutedOrMatchedGeometry().orElse(null);

			this.abweichendeSegmente = fahrradroutenMatchingAndRoutingInformation
				.getAbweichendeSegmente().orElse(null);
			if (fahrradroutenMatchingAndRoutingInformation.getKehrtwenden().isPresent()) {
				this.kehrtwenden = Arrays.stream(
						fahrradroutenMatchingAndRoutingInformation.getKehrtwenden().get()
							.getCoordinates())
					.map(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()::createPoint)
					.collect(Collectors.toList());
			} else {
				this.kehrtwenden = List.of();
			}

			this.abbildungDurchRouting = fahrradroutenMatchingAndRoutingInformation
				.getAbbildungDurchRouting().orElse(false);
		} else {
			this.abbildungDurchRouting = false;
			this.routedOrMatchedGeometry = null;
			this.kehrtwenden = List.of();
			this.abweichendeSegmente = null;
		}

		this.canEditAttribute = canEditFahrradrouteAttribute;
		this.canChangeVeroeffentlicht = canChangeVeroeffentlicht;

		this.varianten = fahrradroute.getVarianten().stream()
			.map(FahrradrouteVarianteView::new)
			.collect(Collectors.toList());

		this.profilEigenschaften = fahrradroute.getLinearReferenzierteProfilEigenschaften().stream()
			.map(LinearReferenzierteProfilEigenschaftenView::new)
			.collect(Collectors.toList());
	}
}
