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

package de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzklassenStreckenSignaturView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.GeometrienVerlaufMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteFuehrungsformAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteGeschwindigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteZustaendigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzNetzklasseMapView;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class NetzToGeoJsonConverter {
	// Der Key doppelt sich mit FeatureProperties.KANTE_ID_KEY im Frontend.
	public static final String KANTE_ID_KEY = "kanteId";
	// Der Key doppelt sich mit FeatureProperties.SEITE_PROPERTY_NAME im Frontend.
	public static final String SEITE_PROPERTY_NAME = "seitenbezug";

	public FeatureCollection convertNetzAusschnitt(NetzMapView netzAusschnitt, boolean includeKanten,
		boolean mitVerlauf) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		if (includeKanten) {
			netzAusschnitt.getKanten().forEach(
				(KanteMapView kante) -> featureCollection.add(createFeature(kante, mitVerlauf)));
		}

		netzAusschnitt.getKnoten().forEach(knoten -> {
			Point point = knoten.getPoint();
			Feature feature = GeoJsonConverter.createFeature(point);
			feature.setId(knoten.getId().toString());

			featureCollection.add(feature);
		});

		return featureCollection;
	}

	public FeatureCollection convertNetzAusschnitt(NetzNetzklasseMapView netzAusschnitt,
		Map<Long, List<Long>> zuordnungen) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		netzAusschnitt.getKanten().forEach(
			(KanteNetzklasseMapView kante) -> {
				Feature createFeature = createFeature(kante, false);
				createFeature.setProperty("netzKlassen", kante.getNetzklassen());
				createFeature.setProperty("zugeordneteRadNETZKanten", zuordnungen.get(kante.getId()));
				featureCollection.add(createFeature);
			});

		netzAusschnitt.getKnoten().forEach(knoten -> {
			Point point = knoten.getPoint();
			Feature feature = GeoJsonConverter.createFeature(point);
			feature.setId(knoten.getId().toString());

			featureCollection.add(feature);
		});

		return featureCollection;
	}

	public FeatureCollection convertNetzAusschnitt(NetzNetzklasseMapView netzAusschnitt) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		netzAusschnitt.getKanten().forEach(
			(KanteNetzklasseMapView kante) -> {
				Feature createFeature = createFeature(kante, false);
				createFeature.setProperty("netzKlassen", kante.getNetzklassen());
				featureCollection.add(createFeature);
			});

		netzAusschnitt.getKnoten().forEach(knoten -> {
			Point point = knoten.getPoint();
			Feature feature = GeoJsonConverter.createFeature(point);
			feature.setId(knoten.getId().toString());

			featureCollection.add(feature);
		});

		return featureCollection;
	}

	public FeatureCollection convertNetzAusschnitt(NetzMapView netzAusschnitt, boolean mitVerlauf) {
		return convertNetzAusschnitt(netzAusschnitt, true, mitVerlauf);
	}

	public FeatureCollection convertNetzAusschnitt(NetzMapView netzAusschnitt) {
		return convertNetzAusschnitt(netzAusschnitt, true, false);
	}

	public FeatureCollection convertNetzAusschnittGeometrienVerlauf(
		Set<GeometrienVerlaufMapView> netzAusschnitt) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		netzAusschnitt.forEach(kantenVerlauf -> {
			kantenVerlauf.getGeometrieLinks().ifPresent(
				links -> featureCollection.add(createFeature(String.format("%sl", kantenVerlauf.getId()), links)));
			kantenVerlauf.getGeometrieRechts().ifPresent(
				rechts -> featureCollection.add(createFeature(String.format("%sr", kantenVerlauf.getId()), rechts)));
		});
		return featureCollection;
	}

	private Feature createFeature(KanteMapView kante, boolean mitVerlauf) {
		Feature feature;
		if (mitVerlauf && (kante.getVerlaufLinks().isPresent() || kante.getVerlaufRechts().isPresent())) {
			if (kante.getVerlaufLinks().isPresent() && kante.getVerlaufRechts().isPresent()) {
				LineString[] multiLine = new LineString[2];
				multiLine[0] = kante.getVerlaufLinks().get();
				multiLine[1] = kante.getVerlaufRechts().get();
				MultiLineString multiLineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createMultiLineString(multiLine);
				feature = createFeature(kante.getId(), multiLineString);
			} else if (kante.getVerlaufLinks().isPresent()) {
				feature = createFeature(kante.getId(), kante.getVerlaufLinks().get());
			} else {
				feature = createFeature(kante.getId(), kante.getVerlaufRechts().get());
			}
		} else {
			feature = createFeature(kante.getId(), kante.getGeometrie());
		}

		feature.setProperty("kanteZweiseitig", kante.isZweiseitig());
		feature.setProperty("istStrecke", kante.isIstStrecke());

		return feature;
	}

	private Feature createFeature(String id, Geometry geometry) {
		Feature feature = GeoJsonConverter.createFeature(geometry);
		feature.setId(id);
		return feature;
	}

	private Feature createFeature(Long id, Geometry geometry) {
		return this.createFeature(id.toString(), geometry);
	}

	public FeatureCollection convertNetzfehler(Iterable<Netzfehler> netzfehlerIterable) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		for (Netzfehler netzfehler : netzfehlerIterable) {
			Feature feature = GeoJsonConverter.createFeature(netzfehler.getGeometry());
			feature.setId(netzfehler.getId().toString());
			feature.setProperty("job", netzfehler.getJobZuordnung());
			if (netzfehler.getJobZuordnung().equals("MatchNetzAufDLMJob " + QuellSystem.RadwegeDB)
				|| netzfehler.getJobZuordnung()
					.equals("AttributProjektionsJob " + QuellSystem.RadwegeDB)
				|| netzfehler.getJobZuordnung()
					.equals("MatchNetzAufOSMJob DLM")) {
				continue;
			}
			featureCollection.add(feature);
		}

		return featureCollection;
	}

	public FeatureCollection convertKantenAttribute(Set<Kante> kanten, List<String> attribute) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		kanten.forEach(kante -> {
			Feature feature = GeoJsonConverter.createFeature(kante.getGeometry());
			KantenAttribute kantenAttribut = kante.getKantenAttributGruppe().getKantenAttribute();

			setPropertyIfAttributeExists(feature, "is_zweiseitig",
				kante.getFahrtrichtungAttributGruppe().isZweiseitig(), attribute);
			setPropertyIfAttributeExists(feature, "fahrtrichtung_links",
				kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks(), attribute);
			setPropertyIfAttributeExists(feature, "fahrtrichtung_rechts",
				kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts(), attribute);
			setPropertyIfAttributeExists(feature, "wege_niveau", kantenAttribut.getWegeNiveau().orElse(null),
				attribute);
			setPropertyIfAttributeExists(feature, "beleuchtung", kantenAttribut.getBeleuchtung(), attribute);
			setPropertyIfAttributeExists(feature, "umfeld", kantenAttribut.getUmfeld(), attribute);
			setPropertyIfAttributeExists(feature, "strassenkategorie_rin",
				kantenAttribut.getStrassenkategorieRIN().orElse(null),
				attribute);
			setPropertyIfAttributeExists(feature, "strassenquerschnittrast06",
				kantenAttribut.getStrassenquerschnittRASt06(), attribute);
			setPropertyIfAttributeExists(feature, "laenge_manuell_erfasst",
				kantenAttribut.getLaengeManuellErfasst().orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "dtv_fussverkehr",
				kantenAttribut.getDtvFussverkehr().map(VerkehrStaerke::getValue).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "dtv_radverkehr",
				kantenAttribut.getDtvRadverkehr().map(VerkehrStaerke::getValue).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "dtv_pkw",
				kantenAttribut.getDtvPkw().map(VerkehrStaerke::getValue).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "sv",
				kantenAttribut.getSv().map(VerkehrStaerke::getValue).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "kommentar",
				kantenAttribut.getKommentar().map(Kommentar::toString).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "strassen_name",
				kantenAttribut.getStrassenName().map(StrassenName::toString).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "strassen_nummer",
				kantenAttribut.getStrassenNummer().map(StrassenNummer::toString).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "gemeinde_name",
				kantenAttribut.getGemeinde().map(Verwaltungseinheit::getName).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "landkreis_name",
				kantenAttribut.getGemeinde().flatMap(Verwaltungseinheit::getUebergeordneteVerwaltungseinheit)
					.map(Verwaltungseinheit::getName).orElse(null),
				attribute);
			setPropertyIfAttributeExists(feature, "status", kantenAttribut.getStatus(), attribute);

			Optional<Set<Netzklasse>> hoechsteNetzklasseOptional = kante.getHoechsteNetzklassen();
			setPropertyIfAttributeExists(feature, "hoechsteNetzklasse",
				hoechsteNetzklasseOptional.map(Netzklasse::getHoechsteNetzklasseBezeichnung).orElse(null), attribute);
			setPropertyIfAttributeExists(feature, "netzklassen",
				kante.getKantenAttributGruppe().getNetzklassen().stream().map(Enum::name).collect(
					Collectors.joining(";")), attribute);
			setPropertyIfAttributeExists(feature, "standards",
				kante.getKantenAttributGruppe().getIstStandards().stream().map(Enum::name).collect(
					Collectors.joining(";")), attribute);

			feature.setProperty(KANTE_ID_KEY, kante.getId().toString());

			featureCollection.add(feature);
		});
		return featureCollection;
	}

	public FeatureCollection convertZustaendigkeitAttributeView(Set<KanteZustaendigkeitAttributeView> kanten,
		List<String> attribute) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		kanten.forEach(kante -> kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttributeSet()
			.forEach(zustaendigkeitAttribut -> {
				LineString segment = kante.getSegment(zustaendigkeitAttribut.getLinearReferenzierterAbschnitt());
				Feature feature = GeoJsonConverter
					.createFeature(segment);
				setPropertyIfAttributeExists(feature, "vereinbarungs_kennung",
					zustaendigkeitAttribut.getVereinbarungsKennung().map(VereinbarungsKennung::toString).orElse(null),
					attribute);
				setPropertyIfAttributeExists(feature, "baulast_traeger",
					zustaendigkeitAttribut.getBaulastTraeger().map(Verwaltungseinheit::getName).orElse(null),
					attribute);
				setPropertyIfAttributeExists(feature, "unterhalts_zustaendiger",
					zustaendigkeitAttribut.getUnterhaltsZustaendiger().map(Verwaltungseinheit::getName).orElse(null),
					attribute);
				setPropertyIfAttributeExists(feature, "erhalts_zustaendiger",
					zustaendigkeitAttribut.getErhaltsZustaendiger().map(Verwaltungseinheit::getName).orElse(null),
					attribute);

				feature.setProperty(KANTE_ID_KEY, kante.getId().toString());

				featureCollection.add(feature);
			}));
		return featureCollection;
	}

	public FeatureCollection convertFuehrungsformAttribute(Set<KanteFuehrungsformAttributeView> kanten,
		List<String> attribute) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		kanten.forEach(kante -> {
			if (kante.getFuehrungsformAttributGruppe().isZweiseitig()) {
				addFeaturesWithFuehrungsformAttributes(
					kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinksSet(),
					kante,
					KantenSeite.LINKS,
					attribute,
					featureCollection);

				addFeaturesWithFuehrungsformAttributes(
					kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechtsSet(),
					kante,
					KantenSeite.RECHTS,
					attribute,
					featureCollection);
			} else {
				addFeaturesWithFuehrungsformAttributes(
					kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinksSet(),
					kante,
					null, // Einseitige Kanten haben keine linke oder rechte Seite
					attribute,
					featureCollection);
			}
		});

		return featureCollection;
	}

	private void addFeaturesWithFuehrungsformAttributes(Set<FuehrungsformAttribute> fuehrungsformAttribute,
		KanteFuehrungsformAttributeView kante, KantenSeite seite, List<String> attribute,
		FeatureCollection featureCollection) {
		fuehrungsformAttribute.forEach(fuehrungsformAttribut -> {
			Feature feature = GeoJsonConverter.createFeature(
				kante.getSegment(fuehrungsformAttribut.getLinearReferenzierterAbschnitt()));

			feature.setProperty(KANTE_ID_KEY, kante.getId().toString());
			feature.setProperty(SEITE_PROPERTY_NAME, seite);

			setPropertyIfAttributeExists(feature, "belag_art", fuehrungsformAttribut.getBelagArt(), attribute);
			setPropertyIfAttributeExists(feature, "oberflaechenbeschaffenheit",
				fuehrungsformAttribut.getOberflaechenbeschaffenheit(), attribute);
			setPropertyIfAttributeExists(feature, "benutzungspflicht",
				fuehrungsformAttribut.getBenutzungspflicht(), attribute);
			setPropertyIfAttributeExists(feature, "bordstein", fuehrungsformAttribut.getBordstein(),
				attribute);
			setPropertyIfAttributeExists(feature, "radverkehrsfuehrung",
				fuehrungsformAttribut.getRadverkehrsfuehrung(), attribute);
			setPropertyIfAttributeExists(feature, "parken_typ", fuehrungsformAttribut.getParkenTyp(),
				attribute);
			setPropertyIfAttributeExists(feature, "parken_form", fuehrungsformAttribut.getParkenForm(),
				attribute);
			setPropertyIfAttributeExists(feature, "breite", fuehrungsformAttribut.getBreite(), attribute);

			featureCollection.add(feature);
		});
	}

	public FeatureCollection convertGeschwindigkeitattribute(Set<KanteGeschwindigkeitAttributeView> kanten,
		List<String> attribute) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		kanten.forEach(kante -> kante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute()
			.forEach(geschwindigkeitAttribut -> {
				Feature feature = GeoJsonConverter
					.createFeature(kante.getSegment(geschwindigkeitAttribut.getLinearReferenzierterAbschnitt()));
				setPropertyIfAttributeExists(feature, "ortslage", geschwindigkeitAttribut.getOrtslage().orElse(null),
					attribute);
				setPropertyIfAttributeExists(feature, "hoechstgeschwindigkeit",
					geschwindigkeitAttribut.getHoechstgeschwindigkeit(), attribute);
				setPropertyIfAttributeExists(feature, "abweichende_hoechstgeschwindigkeit_gegen_stationierungsrichtung",
					geschwindigkeitAttribut.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung()
						.orElse(null),
					attribute);

				feature.setProperty(KANTE_ID_KEY, kante.getId().toString());

				featureCollection.add(feature);
			}));

		return featureCollection;
	}

	private void setPropertyIfAttributeExists(Feature feature, String key, Object value, List<String> attribute) {
		if (attribute.contains(key)) {
			feature.setProperty(key, value);
		}
	}

	public FeatureCollection convertNetzklassenSignaturViews(
		List<NetzklassenStreckenSignaturView> netzklassenSignaturViews) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		netzklassenSignaturViews.forEach(netzklassenSignaturView -> {
			Feature feature = GeoJsonConverter.createFeature(netzklassenSignaturView.getStreckenGeometrie());

			Set<Netzklasse> hoechsteNetzklasse = netzklassenSignaturView.getNetzklassen();
			feature.setProperty("hoechsteNetzklasse",
				Netzklasse.getHoechsteNetzklasseBezeichnung(hoechsteNetzklasse));
			feature.setProperty("istStrecke", true);

			featureCollection.add(feature);
		});
		return featureCollection;
	}

	public FeatureCollection convertKanten(Set<Kante> kanten) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		kanten.forEach(kante -> {
			Feature feature = GeoJsonConverter.createFeature(kante.getGeometry());
			feature.setId(kante.getId().toString());
			featureCollection.add(feature);
		});
		return featureCollection;
	}

	/**
	 * Spendiert Features eine Property "hasNetzklasse", womit erkennbar ist, ob die Kante zur übergebenen Netzklasse
	 * gehört.
	 */
	public FeatureCollection convertKanten(Set<Kante> kanten, Netzklasse labelBy) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();

		kanten.forEach(kante -> {
			Feature feature = GeoJsonConverter.createFeature(kante.getGeometry());
			feature.setId(kante.getId().toString());
			feature.setProperty("hasNetzklasse", kante.hasNetzklasse(labelBy));
			featureCollection.add(feature);
		});
		return featureCollection;
	}

}
