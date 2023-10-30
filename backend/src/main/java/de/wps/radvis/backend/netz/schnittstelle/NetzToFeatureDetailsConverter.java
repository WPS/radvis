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

package de.wps.radvis.backend.netz.schnittstelle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.schnittstelle.view.AttributeView;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.netz.schnittstelle.view.KanteDetailView;
import de.wps.radvis.backend.netz.schnittstelle.view.KnotenDetailView;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class NetzToFeatureDetailsConverter {

	public KanteDetailView convertKantetoKanteDetailView(Kante kante, Coordinate atPosition, String seite) {
		KantenSeite kantenSeite = "links".equals(seite) ? KantenSeite.LINKS : KantenSeite.RECHTS;

		if (!kante.isZweiseitig()) {
			kantenSeite = KantenSeite.LINKS;
		}

		Map<String, String> attributeAufGanzerLaenge = new HashMap<>();
		attributeAufGanzerLaenge.put("ID", kante.getId().toString());
		attributeAufGanzerLaenge.put("Richtung",
			KantenSeite.LINKS.equals(kantenSeite)
				? kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks().toString()
				: kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts().toString());

		KantenAttribute kantenAttribute = kante.getKantenAttributGruppe().getKantenAttribute();
		attributeAufGanzerLaenge.put("Wegeniveau",
			kantenAttribute.getWegeNiveau().map(WegeNiveau::toString).orElse(null));
		attributeAufGanzerLaenge.put("Beleuchtung", kantenAttribute.getBeleuchtung().toString());
		attributeAufGanzerLaenge.put("Umfeld", kantenAttribute.getUmfeld().toString());
		attributeAufGanzerLaenge.put("Strassenquerschnitt nach RASt 06",
			kantenAttribute.getStrassenquerschnittRASt06().toString());
		attributeAufGanzerLaenge.put("Länge (berechnet)", kante.getLaengeBerechnet().toString());
		attributeAufGanzerLaenge.put("Länge (manuell)",
			kantenAttribute.getLaengeManuellErfasst().map(Laenge::toString).orElse(null));
		attributeAufGanzerLaenge.put("DTV Fußverkehr",
			kantenAttribute.getDtvFussverkehr().map(VerkehrStaerke::toString).orElse(null));
		attributeAufGanzerLaenge.put("DTV Radverkehr",
			kantenAttribute.getDtvRadverkehr().map(VerkehrStaerke::toString).orElse(null));
		attributeAufGanzerLaenge.put("DTV PKW", kantenAttribute.getDtvPkw().map(VerkehrStaerke::toString).orElse(null));
		attributeAufGanzerLaenge.put("SV", kantenAttribute.getSv().map(VerkehrStaerke::toString).orElse(null));
		attributeAufGanzerLaenge.put("Kommentar", kantenAttribute.getKommentar().map(Kommentar::toString).orElse(null));
		attributeAufGanzerLaenge.put("Straßenname",
			kantenAttribute.getStrassenName().map(StrassenName::toString).orElse(null));
		attributeAufGanzerLaenge.put("Straßennummer",
			kantenAttribute.getStrassenNummer().map(StrassenNummer::toString).orElse(null));
		attributeAufGanzerLaenge.put("Status", kantenAttribute.getStatus().toString());
		attributeAufGanzerLaenge.put("Gemeinde",
			kantenAttribute.getGemeinde().map(Verwaltungseinheit::getName).orElse(null));
		attributeAufGanzerLaenge.put("Landkreis",
			kantenAttribute.getGemeinde()
				.flatMap(Verwaltungseinheit::getUebergeordneteVerwaltungseinheit)
				.map(Verwaltungseinheit::getName).orElse(null));
		attributeAufGanzerLaenge.put("Quelle", kante.getQuelle().toString());

		List<String> netzklassen = kante.getKantenAttributGruppe().getNetzklassen().stream()
			.map(Netzklasse::toString).collect(Collectors.toList());
		List<String> istStandards = kante.getKantenAttributGruppe().getIstStandards().stream()
			.map(IstStandard::toString).collect(Collectors.toList());

		attributeAufGanzerLaenge.put("Netzklassen", String.join(", ", netzklassen));
		attributeAufGanzerLaenge.put("Ist-Standards", String.join(", ", istStandards));

		Map<String, String> attributeAnPosition = new HashMap<>();
		GeschwindigkeitAttribute geschwindigkeitAttributeAnPunkt = kante.getGeschwindigkeitAttributeAnPunkt(atPosition);
		attributeAnPosition.put("Höchstgeschwindigkeit",
			geschwindigkeitAttributeAnPunkt.getHoechstgeschwindigkeit().toString());
		attributeAnPosition.put("Abweichende Höchstgeschwindigkeit in Gegenrichtung",
			geschwindigkeitAttributeAnPunkt
				.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung()
				.map(Hoechstgeschwindigkeit::toString)
				.orElse(null));
		attributeAnPosition.put("Ortslage",
			geschwindigkeitAttributeAnPunkt.getOrtslage()
				.map(KantenOrtslage::toString)
				.orElse(null));

		ZustaendigkeitAttribute zustaendigkeitAttributeAnPunkt = kante.getZustaendigkeitAttributeAnPunkt(atPosition);
		attributeAnPosition.put("Vereinbarungskennung",
			zustaendigkeitAttributeAnPunkt.getVereinbarungsKennung().map(VereinbarungsKennung::toString).orElse(null));
		attributeAnPosition.put("Baulastträger",
			zustaendigkeitAttributeAnPunkt.getBaulastTraeger().map(Verwaltungseinheit::getName).orElse(null));
		attributeAnPosition.put("Unterhaltszuständiger",
			zustaendigkeitAttributeAnPunkt.getUnterhaltsZustaendiger().map(Verwaltungseinheit::getName).orElse(null));
		attributeAnPosition.put("Erhaltszuständiger",
			zustaendigkeitAttributeAnPunkt.getErhaltsZustaendiger().map(Verwaltungseinheit::getName).orElse(null));

		FuehrungsformAttribute fuehrungsformAttributeAnPunkt = kante.getFuehrungsformAttributeAnPunkt(atPosition,
			kantenSeite);
		attributeAnPosition.put("Belagart", fuehrungsformAttributeAnPunkt.getBelagArt().toString());
		attributeAnPosition.put("Oberflächenbeschaffenheit",
			fuehrungsformAttributeAnPunkt.getOberflaechenbeschaffenheit().toString());
		attributeAnPosition.put("Bordstein", fuehrungsformAttributeAnPunkt.getBordstein().toString());
		attributeAnPosition.put("Radverkehrsführung",
			fuehrungsformAttributeAnPunkt.getRadverkehrsfuehrung().toString());
		attributeAnPosition.put("Kfz-Parken-Form", fuehrungsformAttributeAnPunkt.getParkenForm().toString());
		attributeAnPosition.put("Kfz-Parken-Typ", fuehrungsformAttributeAnPunkt.getParkenTyp().toString());
		attributeAnPosition.put("Benutzungspflicht", fuehrungsformAttributeAnPunkt.getBenutzungspflicht().toString());
		attributeAnPosition.put("Breite", fuehrungsformAttributeAnPunkt.getBreite().map(Laenge::toString).orElse(null));

		// Trennstreifen Attribute fuer Tabelle
		Map<String, String> trennstreifenAttribute = new LinkedHashMap<>();

		FuehrungsformAttribute kagAnPunktLinks = kante.getFuehrungsformAttributeAnPunkt(atPosition, KantenSeite.LINKS);
		trennstreifenAttribute.put("Form (A)", kagAnPunktLinks.getTrennstreifenFormLinks()
			.map(Enum::toString).orElse(null));
		trennstreifenAttribute.put("Trennung zu (A)", kagAnPunktLinks.getTrennstreifenTrennungZuLinks()
			.map(Enum::toString).orElse(null));
		trennstreifenAttribute.put("Breite des Trennstreifens (A)", kagAnPunktLinks.getTrennstreifenBreiteLinks()
			.map(Laenge::toString).orElse(null));
		trennstreifenAttribute.put("Form (B)", kagAnPunktLinks.getTrennstreifenFormRechts()
			.map(Enum::toString).orElse(null));
		trennstreifenAttribute.put("Trennung zu (B)", kagAnPunktLinks.getTrennstreifenTrennungZuRechts()
			.map(Enum::toString).orElse(null));
		trennstreifenAttribute.put("Breite des Trennstreifens (B)", kagAnPunktLinks.getTrennstreifenBreiteRechts()
			.map(Laenge::toString).orElse(null));

		if (kante.getFuehrungsformAttributGruppe().isZweiseitig()) {
			FuehrungsformAttribute kagAnPunktRechts = kante.getFuehrungsformAttributeAnPunkt(atPosition,
				KantenSeite.RECHTS);
			trennstreifenAttribute.put("Form (C)", kagAnPunktRechts.getTrennstreifenFormLinks()
				.map(Enum::toString).orElse(null));
			trennstreifenAttribute.put("Trennung zu (C)", kagAnPunktRechts.getTrennstreifenTrennungZuLinks()
				.map(Enum::toString).orElse(null));
			trennstreifenAttribute.put("Breite des Trennstreifens (C)", kagAnPunktRechts.getTrennstreifenBreiteLinks()
				.map(Laenge::toString).orElse(null));
			trennstreifenAttribute.put("Form (D)", kagAnPunktRechts.getTrennstreifenFormRechts()
				.map(Enum::toString).orElse(null));
			trennstreifenAttribute.put("Trennung zu (D)", kagAnPunktRechts.getTrennstreifenTrennungZuRechts()
				.map(Enum::toString).orElse(null));
			trennstreifenAttribute.put("Breite des Trennstreifens (D)", kagAnPunktRechts.getTrennstreifenBreiteRechts()
				.map(Laenge::toString).orElse(null));
		}

		// Trennstreifen Infos zur Visualisierung
		Boolean trennstreifenEinseitig = !kante.getFuehrungsformAttributGruppe().isZweiseitig();
		Richtung trennstreifenRichtungRechts = kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts();
		Richtung trennstreifenRichtungLinks = kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks();

		return new KanteDetailView(
			kante.getId(),
			kante.getGeometry(),
			attributeAufGanzerLaenge,
			attributeAnPosition,
			trennstreifenAttribute.values().stream().filter(e -> e != null).count() > 1 ? trennstreifenAttribute : null,
			kante.isZweiseitig() ? seite : null, kante.getVerlaufLinks().orElse(null),
			kante.getVerlaufRechts().orElse(null),
			trennstreifenEinseitig,
			trennstreifenRichtungRechts,
			trennstreifenRichtungLinks);
	}

	public KnotenDetailView convertKnotenToKnotenDetailView(Knoten knoten, KnotenOrtslage knotenOrtslage) {
		Map<String, String> attribute = new HashMap<>();
		KnotenAttribute knotenAttribute = knoten.getKnotenAttribute();
		attribute.put("Ortslage", knotenOrtslage != null ? knotenOrtslage.toString() : null);
		attribute.put("Gemeinde", knotenAttribute.getGemeinde().map(Verwaltungseinheit::getName).orElse(null));
		attribute.put("Kommentar", knotenAttribute.getKommentar().map(Kommentar::toString).orElse(null));
		attribute.put("Zustandsbeschreibung",
			knotenAttribute.getZustandsbeschreibung().map(Zustandsbeschreibung::toString).orElse(null));
		attribute.put("Knoten-Form", knotenAttribute.getKnotenForm().map(KnotenForm::toString).orElse(null));
		return new KnotenDetailView(knoten.getId(), knoten.getPoint(), attribute);
	}

	public List<AttributeView> convertKanteToFeatureDetails(Kante kante, Coordinate atPosition, KantenSeite seite) {
		KantenAttribute kantenAttribute = kante.getKantenAttributGruppe().getKantenAttribute();

		List<AttributeView> attributes = new ArrayList<>();
		attributes.add(new AttributeView("ID", kante.getId().toString(), false));

		if (KantenSeite.LINKS.equals(seite)) {
			attributes.add(
				new AttributeView("Richtung",
					kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks().toString(),
					false));
		} else {
			attributes.add(
				new AttributeView("Richtung",
					kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts().toString(),
					false));
		}

		attributes.add(new AttributeView("Wegeniveau",
			kantenAttribute.getWegeNiveau().map(WegeNiveau::toString).orElse(null), false));
		attributes.add(new AttributeView("Beleuchtung",
			kantenAttribute.getBeleuchtung().toString(), false));
		attributes.add(new AttributeView("Umfeld",
			kantenAttribute.getUmfeld().toString(), true));
		attributes.add(new AttributeView("Strassenquerschnitt nach RASt 06",
			kantenAttribute.getStrassenquerschnittRASt06().toString(), true));
		attributes.add(new AttributeView("Länge (berechnet)",
			kante.getLaengeBerechnet().toString(), false));
		attributes.add(new AttributeView("Länge (manuell)",
			kantenAttribute.getLaengeManuellErfasst().map(Laenge::toString).orElse(null), false));
		attributes.add(new AttributeView("DTV Fußverkehr",
			kantenAttribute.getDtvFussverkehr().map(VerkehrStaerke::toString).orElse(null), false));
		attributes.add(new AttributeView("DTV Radverkehr",
			kantenAttribute.getDtvRadverkehr().map(VerkehrStaerke::toString).orElse(null), false));
		attributes.add(new AttributeView("DTV PKW",
			kantenAttribute.getDtvPkw().map(VerkehrStaerke::toString).orElse(null), false));
		attributes.add(new AttributeView("SV",
			kantenAttribute.getSv().map(VerkehrStaerke::toString).orElse(null), false));
		attributes.add(new AttributeView("Kommentar",
			kantenAttribute.getKommentar().map(Kommentar::toString).orElse(null), false));
		attributes.add(new AttributeView("Straßenname",
			kantenAttribute.getStrassenName().map(StrassenName::toString).orElse(null), false));
		attributes.add(new AttributeView("Straßennummer",
			kantenAttribute.getStrassenNummer().map(StrassenNummer::toString).orElse(null), false));
		attributes.add(new AttributeView("Status",
			kantenAttribute.getStatus().toString(), false));
		attributes.add(new AttributeView("Gemeinde",
			kantenAttribute.getGemeinde().map(Verwaltungseinheit::getName).orElse(null), false));
		attributes.add(new AttributeView("Landkreis",
			kantenAttribute.getGemeinde()
				.flatMap(Verwaltungseinheit::getUebergeordneteVerwaltungseinheit)
				.map(Verwaltungseinheit::getName).orElse(null),
			false));
		attributes.add(new AttributeView("Quelle", kante.getQuelle().toString(), false));

		GeschwindigkeitAttribute geschwindigkeitAttributeAnPunkt = kante.getGeschwindigkeitAttributeAnPunkt(
			atPosition);
		attributes.add(new AttributeView("Höchstgeschwindigkeit",
			geschwindigkeitAttributeAnPunkt.getHoechstgeschwindigkeit().toString(),
			true));
		attributes.add(new AttributeView("Abweichende Höchstgeschwindigkeit in Gegenrichtung",
			geschwindigkeitAttributeAnPunkt
				.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung()
				.map(Hoechstgeschwindigkeit::toString)
				.orElse(null),
			true));
		attributes.add(new AttributeView("Ortslage",
			geschwindigkeitAttributeAnPunkt.getOrtslage()
				.map(KantenOrtslage::toString)
				.orElse(null),
			true));

		ZustaendigkeitAttribute zustaendigkeitAttributeAnPunkt = kante.getZustaendigkeitAttributeAnPunkt(atPosition);
		attributes.add(new AttributeView("Vereinbarungskennung",
			zustaendigkeitAttributeAnPunkt.getVereinbarungsKennung().map(VereinbarungsKennung::toString)
				.orElse(null),
			true));
		attributes.add(new AttributeView("Baulastträger",
			zustaendigkeitAttributeAnPunkt.getBaulastTraeger().map(Verwaltungseinheit::getName).orElse(null),
			true));
		attributes.add(new AttributeView("Unterhaltszuständiger",
			zustaendigkeitAttributeAnPunkt.getUnterhaltsZustaendiger().map(Verwaltungseinheit::getName)
				.orElse(null),
			true));
		attributes.add(new AttributeView("Erhaltszuständiger",
			zustaendigkeitAttributeAnPunkt.getErhaltsZustaendiger().map(Verwaltungseinheit::getName)
				.orElse(null),
			true));

		FuehrungsformAttribute fuehrungsformAttributeAnPunkt = kante.getFuehrungsformAttributeAnPunkt(atPosition,
			seite);
		attributes.add(new AttributeView("Belagart",
			fuehrungsformAttributeAnPunkt.getBelagArt().toString(), true));
		attributes.add(new AttributeView("Oberflächenbeschaffenheit",
			fuehrungsformAttributeAnPunkt.getOberflaechenbeschaffenheit().toString(), true));
		attributes.add(new AttributeView("Bordstein",
			fuehrungsformAttributeAnPunkt.getBordstein().toString(), true));
		attributes.add(new AttributeView("Radverkehrsführung",
			fuehrungsformAttributeAnPunkt.getRadverkehrsfuehrung().toString(), true));
		attributes.add(new AttributeView("Kfz-Parken-Form",
			fuehrungsformAttributeAnPunkt.getParkenForm().toString(), true));
		attributes.add(new AttributeView("Kfz-Parken-Typ",
			fuehrungsformAttributeAnPunkt.getParkenTyp().toString(), true));
		attributes.add(new AttributeView("Benutzungspflicht",
			fuehrungsformAttributeAnPunkt.getBenutzungspflicht().toString(), true));
		attributes.add(new AttributeView("Breite",
			fuehrungsformAttributeAnPunkt.getBreite()
				.map(Laenge::toString)
				.orElse(null),
			true));

		List<String> netzklassen = kante.getKantenAttributGruppe().getNetzklassen().stream().map(Netzklasse::toString)
			.collect(Collectors.toList());
		List<String> istStandards = kante.getKantenAttributGruppe().getIstStandards().stream()
			.map(IstStandard::toString).collect(Collectors.toList());

		attributes.add(new AttributeView("Netzklassen", String.join(", ", netzklassen), false));
		attributes.add(new AttributeView("Ist-Standards", String.join(", ", istStandards), false));

		return attributes;
	}

	@Deprecated
	public List<AttributeView> convertKanteToFeatureDetails(Kante kante, Coordinate atPosition) {
		return this.convertKanteToFeatureDetails(kante, atPosition, KantenSeite.LINKS);
	}

}
