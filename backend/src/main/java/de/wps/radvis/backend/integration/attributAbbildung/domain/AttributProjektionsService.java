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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.springframework.beans.factory.annotation.Autowired;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributProjektionsJobStatistik;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.Attributprojektionsbeschreibung;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KanteDublette;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenSegment;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.LineareReferenzProjektionsergebnis;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.MappedKante;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjektionsLaengeZuKurzException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjektionsLaengenVerhaeltnisException;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class AttributProjektionsService {

	private AttributeProjektionsProtokollService attributeProjektionsProtokollService;

	@Autowired
	private KantenMappingRepository kantenMappingRepository;

	private static final double MAXIMALE_ABSOLUTE_LAENGEN_DIFFERENZ = 30;
	private static final double MAXIMALE_RELATIVE_LAENGEN_DIFFERENZ = 2.0;
	private static final double MINIMALE_ABSOLUTE_SEGMENT_LAENGE = 2.;

	/**
	 * Diese Methode nimmt die Dubletten und löst diese nach Grundnetzkanten auf. Für jede Grundnetzkante
	 * die in Dubletten (ohne Fehler) vorkommt wird eine (!) Attributprojektionsbeschreibung erstellt,
	 * ein Zwischenschritt in den Algorithmus.
	 * Hierfür müssen die Bereiche der Grundnetzkante und der kanteMitZuProjizierendenAttribute bestimmt werden,
	 * die der eigentlichen Dublette (dem OSM-Ueberschneidungs-LineString) entsprechen. Dies passiert durch
	 * Rückprojektion auf die beiden Kanten. Den Bereich müssen wir feststellen, damit wir wissen:
	 * 1. welche Attribute liegen gehören dem Uberschneidungsbereich an die wir projizieren wollen
	 * 2. wohin auf der Grundnetzkante wollen wir diese Attribute projizieren
	 * <p>
	 * Da die Geometrien von OSM und der beiden Kanten (auch der beiden kanten untereinander) eine unterschiedliche
	 * Stationierungsrichtung zueinander haben können, müssen wir auch diesen Sonderfall berücksichtigen (Variable
	 * wurdenVonUnsBisVertauscht tracked dies).
	 */
	public Collection<Attributprojektionsbeschreibung> projiziereAttributeAufGrundnetzKanten(
		List<KanteDublette> kantenDubletten,
		AttributProjektionsJobStatistik statistik, String jobZuordnung) {
		Map<Kante, Attributprojektionsbeschreibung> grundnetzKanteToAttributProjektion = new HashMap<>();
		Map<Kante, KantenMapping> grundnetzKanteToKantenmapping = new HashMap<>();

		int indexFuerFortschritt = 0;
		int size = kantenDubletten.size();
		for (KanteDublette kanteDublette : kantenDubletten) {
			KantenSegment segment;
			Kante grundnetzKante = kanteDublette.getZielnetzKante();
			Kante quellnetz = kanteDublette.getQuellnetzKante();
			LineareReferenzProjektionsergebnis lineareReferenzFuerKantenSegment;
			LineareReferenzProjektionsergebnis lineareReferenzFuerLinearReferenzierteAttribute;
			try {
				lineareReferenzFuerKantenSegment = createLineareReferenzAufGrundnetzKante(
					kanteDublette);
				lineareReferenzFuerLinearReferenzierteAttribute = createLineareReferenzAufKanteMitZuProjizierendenAttributen(
					kanteDublette);
			} catch (ProjektionsLaengeZuKurzException e) {
				attributeProjektionsProtokollService.handle(e, jobZuordnung);
				statistik.rueckProjektionsLaengeZuKurz++;
				indexFuerFortschritt++;
				continue;
			} catch (ProjektionsLaengenVerhaeltnisException e) {
				attributeProjektionsProtokollService.handle(e, jobZuordnung);
				statistik.rueckProjektionsLaengenVerhaeltnisZuUnterschiedlich++;
				indexFuerFortschritt++;
				continue;
			}

			MappedKante mappedKante = new MappedKante(lineareReferenzFuerKantenSegment,
				lineareReferenzFuerLinearReferenzierteAttribute, quellnetz.getId());
			grundnetzKanteToKantenmapping.merge(grundnetzKante,
				new KantenMapping(grundnetzKante.getId(), quellnetz.getQuelle(),
					new ArrayList<>(List.of(mappedKante))), KantenMapping::merge);

			segment = new KantenSegment(lineareReferenzFuerKantenSegment,
				lineareReferenzFuerLinearReferenzierteAttribute,
				quellnetz, grundnetzKante);

			if (grundnetzKanteToAttributProjektion.containsKey(grundnetzKante)) {
				grundnetzKanteToAttributProjektion.get(grundnetzKante).addSegment(segment);
			} else {
				Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(
					grundnetzKante);
				attributprojektionsbeschreibung.addSegment(segment);
				grundnetzKanteToAttributProjektion.put(grundnetzKante, attributprojektionsbeschreibung);
			}

			int fortschrittsrate = 4;
			if (size >= fortschrittsrate
				&& indexFuerFortschritt % (size / fortschrittsrate) == 0) {
				log.info("Fortschritt {}%",
					(indexFuerFortschritt / (double) size) * 100);
				log.info("Current free memory: {} / {}", Runtime.getRuntime().freeMemory(),
					Runtime.getRuntime().totalMemory());
			}
			indexFuerFortschritt++;
		}

		grundnetzKanteToKantenmapping.values().forEach(kantenMappingRepository::save);

		return grundnetzKanteToAttributProjektion.values();
	}

	private LineareReferenzProjektionsergebnis createLineareReferenzAufKanteMitZuProjizierendenAttributen(
		KanteDublette kanteDublette)
		throws ProjektionsLaengenVerhaeltnisException, ProjektionsLaengeZuKurzException {
		Kante kanteMitZuProjizierendenAttributen = kanteDublette.getQuellnetzKante();
		LineString ueberschneidungLineString = kanteDublette.getZielnetzUeberschneidung();

		try {
			return createLineareReferenz(kanteMitZuProjizierendenAttributen.getGeometry(),
				ueberschneidungLineString);
		} catch (ProjektionsLaengenVerhaeltnisException e) {
			e.setGrundNetzKanteId(kanteDublette.getZielnetzKante().getId());
			e.setKanteMitZuProjizierendenAttributenId(kanteMitZuProjizierendenAttributen.getId());
			e.setKanteMitFehlerhafterRueckprojektionId(kanteMitZuProjizierendenAttributen.getId());
			throw e;
		} catch (ProjektionsLaengeZuKurzException e) {
			e.setGrundNetzKanteId(kanteDublette.getZielnetzKante().getId());
			e.setKanteMitZuProjizierendenAttributenId(kanteMitZuProjizierendenAttributen.getId());
			e.setKanteMitFehlerhafterRueckprojektionId(kanteMitZuProjizierendenAttributen.getId());
			throw e;
		}
	}

	private LineareReferenzProjektionsergebnis createLineareReferenzAufGrundnetzKante(KanteDublette kanteDublette)
		throws ProjektionsLaengenVerhaeltnisException, ProjektionsLaengeZuKurzException {
		Kante grundnetzKante = kanteDublette.getZielnetzKante();

		LineString ueberschneidungLineString = kanteDublette.getZielnetzUeberschneidung();

		try {
			return createLineareReferenz(grundnetzKante.getGeometry(), ueberschneidungLineString);
		} catch (ProjektionsLaengeZuKurzException e) {
			e.setGrundNetzKanteId(grundnetzKante.getId());
			e.setKanteMitZuProjizierendenAttributenId(kanteDublette.getQuellnetzKante().getId());
			e.setKanteMitFehlerhafterRueckprojektionId(grundnetzKante.getId());
			throw e;
		} catch (ProjektionsLaengenVerhaeltnisException e) {
			e.setGrundNetzKanteId(grundnetzKante.getId());
			e.setKanteMitZuProjizierendenAttributenId(kanteDublette.getQuellnetzKante().getId());
			e.setKanteMitFehlerhafterRueckprojektionId(grundnetzKante.getId());
			throw e;
		}
	}

	private LineareReferenzProjektionsergebnis createLineareReferenz(LineString zielLineString,
		LineString subLineString)
		throws ProjektionsLaengenVerhaeltnisException, ProjektionsLaengeZuKurzException {

		LocationIndexedLine locationIndexedGrundnetzGeometrie = new LocationIndexedLine(zielLineString);

		final LinearLocation projektionDesAnfangs = locationIndexedGrundnetzGeometrie
			.project(subLineString.getStartPoint().getCoordinate());
		final LinearLocation projektionDesEndes = locationIndexedGrundnetzGeometrie
			.project(subLineString.getEndPoint().getCoordinate());

		List<LinearLocation> projektionen = Arrays.asList(projektionDesAnfangs, projektionDesEndes);
		projektionen.sort(Comparator.comparing(LinearLocation::getSegmentIndex)
			.thenComparing(LinearLocation::getSegmentFraction));

		final LinearLocation ueberschneidungsAnfang = projektionen.get(0);
		final LinearLocation ueberschneidungsEnde = projektionen.get(1);

		checkLaengenverhaeltnis(subLineString, locationIndexedGrundnetzGeometrie, ueberschneidungsAnfang,
			ueberschneidungsEnde);

		if (ueberschneidungsAnfang != projektionDesAnfangs) {
			return new LineareReferenzProjektionsergebnis(
				LinearReferenzierterAbschnitt.of(ueberschneidungsAnfang, ueberschneidungsEnde,
					locationIndexedGrundnetzGeometrie),
				ueberschneidungsAnfang, ueberschneidungsEnde,
				true);
		} else {
			return new LineareReferenzProjektionsergebnis(
				LinearReferenzierterAbschnitt.of(ueberschneidungsAnfang, ueberschneidungsEnde,
					locationIndexedGrundnetzGeometrie),
				ueberschneidungsAnfang, ueberschneidungsEnde,
				false);
		}
	}

	private void checkLaengenverhaeltnis(LineString dlmUeberschneidungsLinestring,
		LocationIndexedLine locationIndexedGrundnetzGeometrie, final LinearLocation ueberschneidungsAnfang,
		final LinearLocation ueberschneidungsEnde)
		throws ProjektionsLaengenVerhaeltnisException, ProjektionsLaengeZuKurzException {
		double laengeUberschneidungsLineString = dlmUeberschneidungsLinestring.getLength();

		double laengeProjektion = locationIndexedGrundnetzGeometrie
			.extractLine(ueberschneidungsAnfang, ueberschneidungsEnde).getLength();

		if (laengeProjektion == 0) {
			throw new ProjektionsLaengeZuKurzException(dlmUeberschneidungsLinestring, laengeProjektion);
		}

		if (laengeProjektion < MINIMALE_ABSOLUTE_SEGMENT_LAENGE) {
			throw new ProjektionsLaengeZuKurzException(dlmUeberschneidungsLinestring, laengeProjektion);
		}

		double relativeLaengenDifferenz = (Math.max(laengeProjektion, laengeUberschneidungsLineString))
			/ Math.min(laengeProjektion, laengeUberschneidungsLineString);

		if (Math.abs(laengeProjektion - laengeUberschneidungsLineString) > MAXIMALE_ABSOLUTE_LAENGEN_DIFFERENZ
			|| relativeLaengenDifferenz > MAXIMALE_RELATIVE_LAENGEN_DIFFERENZ) {
			throw new ProjektionsLaengenVerhaeltnisException(dlmUeberschneidungsLinestring, laengeProjektion);
		}
	}
}
