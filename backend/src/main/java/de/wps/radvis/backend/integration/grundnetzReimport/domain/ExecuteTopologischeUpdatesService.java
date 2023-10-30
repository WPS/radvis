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

package de.wps.radvis.backend.integration.grundnetzReimport.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.DLMReimportJobStatistik;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.KnotenTupel;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.SplitUpdate;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.TopologischesUpdate;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.exception.UmkehrDerStationierungsrichtungNichtErkennbarException;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteTopologischeUpdatesService {

	private static final double PROJECTION_TOLERANCE_IN_METER = 15;
	private static final double MAXIMUM_DISTANCE_TO_BE_SAME_KANTE = 30;
	private final FindKnotenFromIndexService findKnotenFromIndexService;
	private final KantenMappingRepository kantenMappingRepository;

	public ExecuteTopologischeUpdatesService(FindKnotenFromIndexService findKnotenFromIndexService,
		KantenMappingRepository kantenMappingRepository) {
		this.findKnotenFromIndexService = findKnotenFromIndexService;
		this.kantenMappingRepository = kantenMappingRepository;
	}

	public Kante executeSimplesTopologischesUpdate(TopologischesUpdate topologischesUpdate,
		DLMReimportJobStatistik statistik, KnotenIndex knotenIndex,
		Map<Kante, LineString> topologischStarkVeraenderteKanten) {
		Kante kante = topologischesUpdate.getKante();
		LineString featureGeometrie = topologischesUpdate.getNeueGeometry();

		if (kante.getGeometry().distance(topologischesUpdate.getNeueGeometry()) > MAXIMUM_DISTANCE_TO_BE_SAME_KANTE) {
			topologischStarkVeraenderteKanten.put(kante, kante.getGeometry());
			updateGeometrieUndTopologieOnKante(topologischesUpdate, knotenIndex, statistik);
			resetAttributeDerKanteAusserDLMAttribute(kante);
			statistik.durchSimpleTopologicalUpdateSehrWeitEntferntVonUrsprungskante++;
			return kante;
		}

		try {
			boolean umgekehrteStationierungsrichtung = umgekehrteStationierungsrichtung(featureGeometrie,
				kante.getGeometry());
			updateZustaendigkeitAttributgruppeOnKante(featureGeometrie, kante, umgekehrteStationierungsrichtung,
				statistik);
			updateFuehrungsformAttributgruppeOnKante(featureGeometrie, kante, umgekehrteStationierungsrichtung);
			updateGeometrieUndTopologieOnKante(topologischesUpdate, knotenIndex, statistik);
			if (umgekehrteStationierungsrichtung) {
				invertGeschwindigkeitsAttributgruppeOnKante(kante);
				invertFahrtrichtungsAttributGruppeOnKante(kante);
			}
		} catch (UmkehrDerStationierungsrichtungNichtErkennbarException e) {
			updateGeometrieUndTopologieOnKante(topologischesUpdate, knotenIndex, statistik);
			resetAttributeDerKanteAusserDLMAttribute(kante);
			statistik.umkehrDerStationierungsrichtungNichtErkennbar++;
			log.warn("Fehler beim Ermitteln, ob eine Umkehr der Stationierung stattfand: "
				+ e.getMessage()
				+ " Betrifft Topologie mit folgenden Daten: {}", topologischesUpdate);
		} catch (ProjektionsrichtungsAenderungsException e) {
			updateGeometrieUndTopologieOnKante(topologischesUpdate, knotenIndex, statistik);
			resetAttributeDerKanteAusserDLMAttribute(kante);
			statistik.isProjektionsReihenfolgeReversed++;
			log.warn("Fehler bei der Projektion: "
				+ e.getMessage()
				+ " Betrifft Topologie mit folgenden Daten: {}", topologischesUpdate);
		} catch (Exception | RequireViolation e) {
			log.error("Fehler beim Update der lin. Referenzen/Topologie mit folgenden Daten: " + topologischesUpdate,
				e);
			log.error("Zustaendigkeitattribute: " + topologischesUpdate.getKante().getZustaendigkeitAttributGruppe()
				.getImmutableZustaendigkeitAttribute());
			log.error("Fuehrungsformattribute: " + topologischesUpdate.getKante().getFuehrungsformAttributGruppe()
				.getImmutableFuehrungsformAttributeLinks());
			statistik.fatalErrorOccurred = true;
		}
		return kante;
	}

	private void resetAttributeDerKanteAusserDLMAttribute(Kante kante) {
		kantenMappingRepository.deleteByGrundnetzKantenId(kante.getId());

		Optional<StrassenName> strassenName = kante.getKantenAttributGruppe().getKantenAttribute()
			.getStrassenName();
		Optional<StrassenNummer> strassenNummer = kante.getKantenAttributGruppe().getKantenAttribute()
			.getStrassenNummer();
		kante.setGrundnetz(true);
		kante.getKantenAttributGruppe().reset();
		strassenName.ifPresent(kante.getKantenAttributGruppe().getKantenAttribute()::setStrassenName);
		strassenNummer.ifPresent(kante.getKantenAttributGruppe().getKantenAttribute()::setStrassenNummer);
		kante.getFahrtrichtungAttributGruppe().reset();
		kante.getGeschwindigkeitAttributGruppe().reset();
		kante.getFuehrungsformAttributGruppe().reset();
		kante.getZustaendigkeitAttributGruppe().reset();
	}

	private void invertFahrtrichtungsAttributGruppeOnKante(Kante kante) {
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = kante.getFahrtrichtungAttributGruppe();
		fahrtrichtungAttributGruppe.setRichtung(fahrtrichtungAttributGruppe.getFahrtrichtungRechts().umgedreht(),
			fahrtrichtungAttributGruppe.getFahrtrichtungLinks().umgedreht());
	}

	private void invertGeschwindigkeitsAttributgruppeOnKante(Kante kante) {
		Set<GeschwindigkeitAttribute> geschwindigkeitAttribute = kante.getGeschwindigkeitAttributGruppe()
			.getGeschwindigkeitAttribute();
		List<GeschwindigkeitAttribute> vertauschteGeschwindigkeitAttribute = geschwindigkeitAttribute.stream()
			.map(GeschwindigkeitAttribute::vertauscht)
			.collect(Collectors.toList());
		kante.getGeschwindigkeitAttributGruppe().replaceGeschwindigkeitAttribute(vertauschteGeschwindigkeitAttribute);
	}

	private void updateGeometrieUndTopologieOnKante(TopologischesUpdate topologischesUpdate,
		KnotenIndex knotenIndex, DLMReimportJobStatistik dlmReimportJobStatistik) {
		try {
			KnotenTupel knotenTupel = findKnotenFromIndexService
				.getNeuenKnotenTupelFuerUpdate(topologischesUpdate, knotenIndex);

			topologischesUpdate.getKante()
				.updateDLMGeometryUndTopology(topologischesUpdate.getNeueGeometry(), knotenTupel.vonKnoten,
					knotenTupel.nachKnoten);
			dlmReimportJobStatistik.topologieAenderungOhneSplit++;
		} catch (StartUndEndpunktGleichException e) {
			dlmReimportJobStatistik.startUndEndpunktGleich++;
		}
	}

	@SuppressWarnings("unchecked")
	private void updateZustaendigkeitAttributgruppeOnKante(LineString featureGeometrie, Kante kante,
		boolean umgekehrteStationierungsrichtung,
		DLMReimportJobStatistik statistik) throws ProjektionsrichtungsAenderungsException {
		List<ZustaendigkeitAttribute> updatedAttribute = (List<ZustaendigkeitAttribute>) ((List<?>) this
			.updateAttributgruppeOnKante(kante.getGeometry(),
				kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute(), featureGeometrie,
				umgekehrteStationierungsrichtung));
		if (updatedAttribute.isEmpty()) {
			ZustaendigkeitAttribute attribute = ZustaendigkeitAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 1.0)).build();
			updatedAttribute.add(attribute);
			statistik.nachSimpleTopologicalUpdateKeineUeberschneidung++;
		}
		kante.getZustaendigkeitAttributGruppe().replaceZustaendigkeitAttribute(updatedAttribute);
	}

	@SuppressWarnings("unchecked")
	private void updateFuehrungsformAttributgruppeOnKante(LineString featureGeometrie, Kante kante,
		boolean umgekehrteStationierungsrichtung) throws ProjektionsrichtungsAenderungsException {
		List<FuehrungsformAttribute> updatedAttributeLinks = (List<FuehrungsformAttribute>) ((List<?>) this
			.updateAttributgruppeOnKante(kante.getGeometry(),
				kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks(), featureGeometrie,
				umgekehrteStationierungsrichtung));
		if (updatedAttributeLinks.isEmpty()) {
			FuehrungsformAttribute attribute = FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 1.0)).build();
			updatedAttributeLinks.add(attribute);
		}
		if (!kante.isZweiseitig()) {
			kante.getFuehrungsformAttributGruppe().replaceFuehrungsformAttribute(updatedAttributeLinks);
		} else {
			List<FuehrungsformAttribute> updatedAttributeRechts = (List<FuehrungsformAttribute>) ((List<?>) this
				.updateAttributgruppeOnKante(kante.getGeometry(),
					kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts(), featureGeometrie,
					umgekehrteStationierungsrichtung));
			if (updatedAttributeRechts.isEmpty()) {
				FuehrungsformAttribute attribute = FuehrungsformAttribute.builder()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 1.0)).build();
				updatedAttributeRechts.add(attribute);
			}
			if (!umgekehrteStationierungsrichtung) {
				kante.getFuehrungsformAttributGruppe()
					.replaceFuehrungsformAttribute(updatedAttributeLinks, updatedAttributeRechts);
			} else {
				kante.getFuehrungsformAttributGruppe()
					.replaceFuehrungsformAttribute(updatedAttributeRechts, updatedAttributeLinks);
			}
		}
	}

	private List<LinearReferenzierteAttribute> updateAttributgruppeOnKante(LineString kantenGeometrie,
		List<? extends LinearReferenzierteAttribute> attributeToProject, LineString featureGeometrie,
		boolean umgekehrteStationierungsrichtung) throws ProjektionsrichtungsAenderungsException {
		List<? extends LinearReferenzierteAttribute> sortedAttribute = attributeToProject.stream().sorted(
				Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
					LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toUnmodifiableList());

		List<LinearReferenzierteAttribute> resultList = new ArrayList<>();
		Double fractionOfProjectedVon = this.getFractionOfProjectedPointOnFeature(
			sortedAttribute.get(0).getLinearReferenzierterAbschnitt().getVonValue(), kantenGeometrie, featureGeometrie);

		for (LinearReferenzierteAttribute attribute : sortedAttribute) {
			Double fractionOfProjectedBis = this.getFractionOfProjectedPointOnFeature(
				attribute.getLinearReferenzierterAbschnitt().getBisValue(), kantenGeometrie, featureGeometrie);
			// Fortfahren, solange kein sinnvoller Abschnitt auf die neue Geometrie projiziert werden konnte
			if (isProjectedLinearSegmentInvalid(fractionOfProjectedVon, fractionOfProjectedBis,
				umgekehrteStationierungsrichtung)) {
				log.info(
					"Projektion nicht valide: fractionOfProjectedVon {}, fractionOfProjectedBis {}, umgekehrteStationierungsrichtung {}",
					fractionOfProjectedVon, fractionOfProjectedBis, umgekehrteStationierungsrichtung);
				continue;
			}

			if (isProjektionsReihenfolgeReversed(fractionOfProjectedVon, fractionOfProjectedBis,
				umgekehrteStationierungsrichtung)) {
				throw new ProjektionsrichtungsAenderungsException("Das Segment"
					+ " von " + attribute.getLinearReferenzierterAbschnitt().getVonValue()
					+ " bis " + attribute.getLinearReferenzierterAbschnitt().getBisValue()
					+ " wurde entgegengesetzt zur vorher ermittelten Projektionsrichtung projiziert.");
			}

			double von = umgekehrteStationierungsrichtung ? 1.0 : 0.0;
			double bis = umgekehrteStationierungsrichtung ? 0.0 : 1.0;

			log.debug("Vor der Verarbeitung: von {}, bis {}, umgekehrteStationierungsrichtung {}", von, bis,
				umgekehrteStationierungsrichtung);

			log.debug("fractionOfProjectedVon {}, fractionOfProjectedBis {}", fractionOfProjectedVon,
				fractionOfProjectedBis);

			if (fractionOfProjectedVon != null) {
				von = fractionOfProjectedVon;
			}
			if (fractionOfProjectedBis != null) {
				bis = fractionOfProjectedBis;
			}

			// Umgedrehte Stationierungsrichtung beachten
			if (umgekehrteStationierungsrichtung) {
				double vonTmp = von;
				von = bis;
				bis = vonTmp;
			}

			log.debug("Nach der Verarbeitung: von {}, bis {}", von, bis);

			resultList.add(attribute.withLinearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(von, bis)));
			// Breche in jedem Fall ab, wenn bis ans Ende der neuen Geometrie projiziert wurde.databasechangelog
			// Dies verhindert inkonsistente lin. Referenzen, wenn es Schleifen oder andere Abnormalitäten gibt.
			if ((umgekehrteStationierungsrichtung && von == 0.0) || (!umgekehrteStationierungsrichtung && bis == 1.0)) {
				break;
			}
			fractionOfProjectedVon = fractionOfProjectedBis;
		}

		// Keine Uebernahme von Attributen wenn es keine Ueberschneidung zwischen alter und neuer Geometrie gibt
		if (resultList.isEmpty()) {
			return resultList;
		}
		// Für die Prüfung auf Erweiterung unten müssen die lin. ref. Attribute aufsteigend sortiert sein
		resultList.sort(
			Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst));

		// Erweitere ggf. erstes Segment
		if (resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue() != 0.0) {
			LinearReferenzierteAttribute attribute = resultList.get(0);
			double bis = attribute.getLinearReferenzierterAbschnitt().getBisValue();
			if (resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue()
				* featureGeometrie.getLength() < PROJECTION_TOLERANCE_IN_METER) {
				resultList.set(0,
					attribute.withLinearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, bis)));
			} else {
				resultList.add(0, attribute.withDefaultValuesAndLineareReferenz(
					LinearReferenzierterAbschnitt.of(0.0,
						resultList.get(0).getLinearReferenzierterAbschnitt().getVonValue())));
			}
		}
		// Erweitere ggf. letztes Segment
		if (resultList.get(resultList.size() - 1).getLinearReferenzierterAbschnitt().getBisValue() != 1.0) {
			LinearReferenzierteAttribute attribute = resultList.get(resultList.size() - 1);
			double von = attribute.getLinearReferenzierterAbschnitt().getVonValue();
			if ((1 - resultList.get(resultList.size() - 1).getLinearReferenzierterAbschnitt().getBisValue())
				* featureGeometrie.getLength() < PROJECTION_TOLERANCE_IN_METER) {
				resultList.set(resultList.size() - 1, attribute.withLinearReferenzierterAbschnitt(
					LinearReferenzierterAbschnitt.of(von, 1.0)));
			} else {
				resultList.add(attribute.withDefaultValuesAndLineareReferenz(
					LinearReferenzierterAbschnitt.of(
						resultList.get(resultList.size() - 1).getLinearReferenzierterAbschnitt().getBisValue(), 1.0)));
			}
		}
		return resultList;
	}

	private boolean isProjektionsReihenfolgeReversed(Double fractionOfProjectedVon, Double fractionOfProjectedBis,
		boolean umgekehrteStationierungsrichtung) {
		return fractionOfProjectedVon != null && fractionOfProjectedBis != null
			&& (
			(Double.compare(fractionOfProjectedVon, fractionOfProjectedBis) > 0 && !umgekehrteStationierungsrichtung)
				|| (Double.compare(fractionOfProjectedVon, fractionOfProjectedBis) < 0
				&& umgekehrteStationierungsrichtung));
	}

	// Input: Der relative Laengenanteil an der alten Kantengeometrie
	// Return: Der relative Laengenanteil an dem neuen Feature-LineString nach einer Projektion
	// Gibt null zurück, wenn der projizierte Punkt nicht auf dem Feature-LineString liegt
	private Double getFractionOfProjectedPointOnFeature(double fractionOnKante, LineString kantenGeometrie,
		LineString featureGeometrie) {
		// Berechne Punkt auf Kante
		double metermarkeOnKante = kantenGeometrie.getLength() * fractionOnKante;
		Coordinate pointOnKanteAtFraction = new LengthIndexedLine(kantenGeometrie).extractPoint(metermarkeOnKante);

		// Projiziere Punkt auf Feature-LineString
		LocationIndexedLine locationIndexedLineOfFeature = new LocationIndexedLine(featureGeometrie);
		LinearLocation linearLocationOfProjectedPoint = locationIndexedLineOfFeature.project(pointOnKanteAtFraction);
		Coordinate projectedPointOnFeature = locationIndexedLineOfFeature.extractPoint(linearLocationOfProjectedPoint);

		// Bestimme, ob Punkt auf Kante auch auf Feature-Linestring liegt -> Sonst null
		if (pointOnKanteAtFraction.distance(projectedPointOnFeature) > PROJECTION_TOLERANCE_IN_METER) {
			return null;
		}

		// Bestimme Fraction des projizierten Punktes auf dem Feature-LineString
		double metermarkeOnFeature = new LengthIndexedLine(featureGeometrie).indexOf(projectedPointOnFeature);
		double fractionOfProjectedPoint = metermarkeOnFeature / featureGeometrie.getLength();

		// Runden
		return Math.round(fractionOfProjectedPoint * 100.0) / 100.0;
	}

	private boolean isProjectedLinearSegmentInvalid(Double fractionOfProjectedVon, Double fractionOfProjectedBis,
		boolean umgekehrteStationierungsrichtung) {
		boolean vonAndBisTooFarAway = fractionOfProjectedVon == null && fractionOfProjectedBis == null;
		boolean vonAndBisGleich = fractionOfProjectedVon != null
			&& fractionOfProjectedVon.equals(fractionOfProjectedBis);
		boolean vonTooFarAwayAndBisIsStartpoint = !umgekehrteStationierungsrichtung &&
			fractionOfProjectedBis != null && fractionOfProjectedBis == 0.0 && fractionOfProjectedVon == null;
		boolean bisTooFarAwayAndVonIsEndpoint = !umgekehrteStationierungsrichtung &&
			fractionOfProjectedVon != null && fractionOfProjectedVon == 1.0
			&& fractionOfProjectedBis == null;
		boolean reversedOrientationAndVonTooFarAwayAndBisIsEndpoint = umgekehrteStationierungsrichtung
			&& fractionOfProjectedBis != null && fractionOfProjectedBis == 1.0
			&& fractionOfProjectedVon == null;
		boolean reversedOrientationAndBisTooFarAwayAndVonIsStartpoint = umgekehrteStationierungsrichtung
			&& fractionOfProjectedVon != null && fractionOfProjectedVon == 0.0
			&& fractionOfProjectedBis == null;
		return vonAndBisTooFarAway || vonAndBisGleich || vonTooFarAwayAndBisIsStartpoint
			|| bisTooFarAwayAndVonIsEndpoint || reversedOrientationAndVonTooFarAwayAndBisIsEndpoint
			|| reversedOrientationAndBisTooFarAwayAndVonIsStartpoint;
	}

	boolean umgekehrteStationierungsrichtung(LineString featureGeometry, LineString kantenGeometry)
		throws UmkehrDerStationierungsrichtungNichtErkennbarException {

		LocationIndexedLine locationIndexedKantenGeometrie = new LocationIndexedLine(
			kantenGeometry);
		LinearLocation projektionDesAnfangs = locationIndexedKantenGeometrie
			.project(featureGeometry.getStartPoint().getCoordinate());
		LinearLocation projektionDesEndes = locationIndexedKantenGeometrie
			.project(featureGeometry.getEndPoint().getCoordinate());

		// Falls die Projektionen von Ende und Anfang zu nahe beieinander liegen
		// versuchen wir in der umgekehrten Richtung zu projizieren
		// Wenn eine der Projektionen nicht mindestens 5% Überschneidung aufweist, brechen wir ab mit Exception
		if (projektionDesAnfangs.getCoordinate(kantenGeometry)
			.distance(projektionDesEndes.getCoordinate(kantenGeometry)) < kantenGeometry.getLength() * 0.05) {
			LocationIndexedLine locationIndexedLine = new LocationIndexedLine(
				featureGeometry);
			projektionDesAnfangs = locationIndexedLine
				.project(kantenGeometry.getStartPoint().getCoordinate());
			projektionDesEndes = locationIndexedLine
				.project(kantenGeometry.getEndPoint().getCoordinate());
			if (projektionDesAnfangs.getCoordinate(featureGeometry)
				.distance(projektionDesEndes.getCoordinate(featureGeometry)) < featureGeometry.getLength() * 0.05) {
				throw new UmkehrDerStationierungsrichtungNichtErkennbarException(
					"Projektionen nicht mit mindestens 5% Überschneidung");
			}
		}

		List<LinearLocation> projektionen = Arrays.asList(projektionDesAnfangs, projektionDesEndes);
		projektionen.sort(Comparator.comparing(LinearLocation::getSegmentIndex)
			.thenComparing(LinearLocation::getSegmentFraction));

		final LinearLocation ueberschneidungsAnfang = projektionen.get(0);

		return ueberschneidungsAnfang != projektionDesAnfangs;
	}

	public List<Kante> executeSplitUpdate(SplitUpdate splitUpdate) {
		List<Kante> alleBearbeitetenKanten = new ArrayList<>();

		Kante gesplitteteKante = splitUpdate.getKante();

		// Schreibe Attribute & Ändere Topologie
		LineString alteGeometrieDerBestehendenKante = gesplitteteKante.getGeometry();

		gesplitteteKante.updateDLMGeometryUndTopology(splitUpdate.getUpdatedGeometry(), splitUpdate.getUpdatedVon(),
			splitUpdate.getUpdatedNach());

		Set<GeschwindigkeitAttribute> geschwindigkeitAttributeDerGesplittetenKante = gesplitteteKante
			.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute();

		Richtung fahrtrichtungLinksDerGesplittetenKante = gesplitteteKante.getFahrtrichtungAttributGruppe()
			.getFahrtrichtungLinks();
		Richtung fahrtrichtungRechtsDerGesplittetenKante = gesplitteteKante.getFahrtrichtungAttributGruppe()
			.getFahrtrichtungRechts();

		List<FuehrungsformAttribute> immutableFuehrungsformAttributeLinksDerGesplittetenKante = gesplitteteKante
			.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks();
		List<FuehrungsformAttribute> immutableFuehrungsformAttributeRechtsDerGesplittetenKante = gesplitteteKante
			.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts();

		List<ZustaendigkeitAttribute> immutableZustaendigkeitAttributeDerGesplittetenKante = gesplitteteKante
			.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute();

		LocationIndexedLine locationIndexedLineAlteGeometrie = new LocationIndexedLine(
			alteGeometrieDerBestehendenKante);
		splitUpdate.getNeuerWegDerKanteErsetzt().forEach(teilKante -> {
			teilKante.changeSeitenbezug(gesplitteteKante.isZweiseitig());
			final LinearLocation projektionDesAnfangs = locationIndexedLineAlteGeometrie
				.project(teilKante.getGeometry().getStartPoint().getCoordinate());
			final LinearLocation projektionDesEndes = locationIndexedLineAlteGeometrie
				.project(teilKante.getGeometry().getEndPoint().getCoordinate());

			List<LinearLocation> projektionen = Arrays.asList(projektionDesAnfangs, projektionDesEndes);
			projektionen.sort(Comparator.comparing(LinearLocation::getSegmentIndex)
				.thenComparing(LinearLocation::getSegmentFraction));

			final LinearLocation ueberschneidungsAnfang = projektionen.get(0);
			final LinearLocation ueberschneidungsEnde = projektionen.get(1);
			if (ueberschneidungsAnfang.compareTo(ueberschneidungsEnde) == 0) {
				log.warn(
					"Der linearReferenzierteAbschnitt für die Projektion hat Länge 0! "
						+ "Projektion wird für diese TeilKante übersprungen.\n"
						+ "Details: \n"
						+ " alteGeometrie: {} \n"
						+ " teilKante: {} \n"
						+ " alleTeilKanten: {} \n"
						+ " überschneidungsAnfang/Ende: {}/{}",
					alteGeometrieDerBestehendenKante,
					teilKante, splitUpdate.getNeuerWegDerKanteErsetzt(), ueberschneidungsAnfang, ueberschneidungsEnde);
				return;
			}

			LinearReferenzierterAbschnitt linearReferenzierterAbschnitt = LinearReferenzierterAbschnitt.of(
				ueberschneidungsAnfang, ueberschneidungsEnde,
				locationIndexedLineAlteGeometrie);
			boolean vonUndBisVertauscht = ueberschneidungsAnfang != projektionDesAnfangs;

			// Kantenattribute bis auf StrassenName und StrassenNummer (wenn vorhanden)
			KantenAttribute.KantenAttributeBuilder kantenAttributeBuilder = gesplitteteKante.getKantenAttributGruppe()
				.getKantenAttribute().getBuilderMitGleichenAttributen();
			if (teilKante.getKantenAttributGruppe().getKantenAttribute().getStrassenName().isPresent()) {
				kantenAttributeBuilder.strassenName(
					teilKante.getKantenAttributGruppe().getKantenAttribute().getStrassenName().get());
			}
			if (teilKante.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer().isPresent()) {
				kantenAttributeBuilder.strassenNummer(
					teilKante.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer().get());
			}

			teilKante.getKantenAttributGruppe().update(
				new HashSet<>(gesplitteteKante.getKantenAttributGruppe().getNetzklassen()),
				new HashSet<>(gesplitteteKante.getKantenAttributGruppe().getIstStandards()),
				kantenAttributeBuilder.build());

			teilKante.setGrundnetz(gesplitteteKante.isGrundnetz());

			// GeschwindigkeitsAttribute

			Set<GeschwindigkeitAttribute> geschwindigkeitsAttributeDerTeilkante = vonUndBisVertauscht
				? geschwindigkeitAttributeDerGesplittetenKante.stream()
				.map(GeschwindigkeitAttribute::vertauscht)
				.collect(Collectors.toSet())
				: geschwindigkeitAttributeDerGesplittetenKante;
			teilKante.getGeschwindigkeitAttributGruppe()
				.replaceGeschwindigkeitAttribute(new ArrayList<>(geschwindigkeitsAttributeDerTeilkante));

			// FahrttrichtungsAttribute

			teilKante.getFahrtrichtungAttributGruppe().setRichtung(
				vonUndBisVertauscht ? fahrtrichtungRechtsDerGesplittetenKante.umgedreht()
					: fahrtrichtungLinksDerGesplittetenKante,
				vonUndBisVertauscht ? fahrtrichtungLinksDerGesplittetenKante.umgedreht()
					: fahrtrichtungRechtsDerGesplittetenKante);

			// FührungsformAttribute
			List<FuehrungsformAttribute> zugeschnitteneFuehrungsformAttributeLinks = LinearReferenzierteAttribute
				.projiziereAuschnittLinearReferenzierterAttributeAufLineareReferenz(
					immutableFuehrungsformAttributeLinksDerGesplittetenKante,
					linearReferenzierterAbschnitt,
					vonUndBisVertauscht);

			if (teilKante.isZweiseitig()) {
				List<FuehrungsformAttribute> zugeschnitteneFuehrungsformAttributeRechts = LinearReferenzierteAttribute
					.projiziereAuschnittLinearReferenzierterAttributeAufLineareReferenz(
						immutableFuehrungsformAttributeRechtsDerGesplittetenKante,
						linearReferenzierterAbschnitt,
						vonUndBisVertauscht);
				if (vonUndBisVertauscht) {
					teilKante.getFuehrungsformAttributGruppe()
						.replaceFuehrungsformAttribute(zugeschnitteneFuehrungsformAttributeRechts,
							zugeschnitteneFuehrungsformAttributeLinks);
				} else {
					teilKante.getFuehrungsformAttributGruppe()
						.replaceFuehrungsformAttribute(zugeschnitteneFuehrungsformAttributeLinks,
							zugeschnitteneFuehrungsformAttributeRechts);
				}
			} else {
				teilKante.getFuehrungsformAttributGruppe()
					.replaceFuehrungsformAttribute(zugeschnitteneFuehrungsformAttributeLinks);
			}

			// Zustaendigkeitsattribute

			List<ZustaendigkeitAttribute> zugeschnitteneZustaendigkeitsAttribute = LinearReferenzierteAttribute
				.projiziereAuschnittLinearReferenzierterAttributeAufLineareReferenz(
					immutableZustaendigkeitAttributeDerGesplittetenKante,
					linearReferenzierterAbschnitt,
					vonUndBisVertauscht);
			teilKante.getZustaendigkeitAttributGruppe()
				.replaceZustaendigkeitAttribute(zugeschnitteneZustaendigkeitsAttribute);

			alleBearbeitetenKanten.add(teilKante);
		});
		return alleBearbeitetenKanten;
	}
}
