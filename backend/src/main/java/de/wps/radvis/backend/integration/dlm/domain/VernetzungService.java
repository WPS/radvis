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

package de.wps.radvis.backend.integration.dlm.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.dlm.domain.entity.RadvisKantenVernetzungStatistik;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class VernetzungService {
	private final KantenRepository kantenRepository;
	private final KnotenRepository knotenRepository;
	private final NetzService netzService;

	/**
	 * Achtung: Diese Methode geht davon aus, dass keine verwaisten Dlm Knoten existieren. Die aufrufende Methode ist
	 * dafuer verantwortlich, dass dieses require erfuellt ist:
	 * require(knotenRepository.findVerwaisteDLMKnoten().isEmpty());
	 */
	public void vernetzeAlleRadvisKantenNeu(RadvisKantenVernetzungStatistik radvisKantenVernetzungStatistik,
		double maximalerKorrekturAbstand, NetzAenderungAusloeser ausloeser) {
		log.info("Prüft und korrigiert Vernetzung aller RadVIS-Kanten");

		List<Knoten> dlmKnotenWithoutDlmKanten = knotenRepository.findDlmKnotenWithoutDlmKanten();

		kantenRepository.findKanteByQuelle(QuellSystem.RadVis).forEach(radvisKante -> {
			log.debug("Vernetzung für RadVIS-Kante {} wird überprüft...", radvisKante.getId());
			radvisKantenVernetzungStatistik.anzahlRadVisKantenBetrachtet++;

			boolean nachKnotenMussNeuVernetztWerden = dlmKnotenWithoutDlmKanten.contains(radvisKante.getNachKnoten());
			boolean vonKnotenMussNeuVernetztWerden = dlmKnotenWithoutDlmKanten.contains(radvisKante.getVonKnoten());

			Optional<Knoten> newVonKnoten = Optional.empty();
			if (vonKnotenMussNeuVernetztWerden) {
				newVonKnoten = findNewKnoten(radvisKante.getGeometry().getStartPoint(),
					radvisKante.getVonKnoten(), dlmKnotenWithoutDlmKanten, radvisKantenVernetzungStatistik,
					maximalerKorrekturAbstand);
			}

			Optional<Knoten> newNachKnoten = Optional.empty();
			if (nachKnotenMussNeuVernetztWerden) {
				newNachKnoten = findNewKnoten(radvisKante.getGeometry().getEndPoint(),
					radvisKante.getNachKnoten(), dlmKnotenWithoutDlmKanten, radvisKantenVernetzungStatistik,
					maximalerKorrekturAbstand);
			}

			LineString oldGeometry = radvisKante.getGeometry();

			Knoten vonKnoten = newVonKnoten.orElse(radvisKante.getVonKnoten());
			Knoten nachKnoten = newNachKnoten.orElse(radvisKante.getNachKnoten());
			if (!vonKnoten.getPoint().equals(nachKnoten.getPoint())) {
				radvisKante.updateTopologie(vonKnoten, nachKnoten);
				kantenRepository.save(radvisKante);
			} else {
				log.warn(
					"Die Topologie der RadVis Kante {} wird nicht geupdated, da ansonsten ein Loop entstehen würde. Die alte Geometrie und Topologie wird beibehalten",
					radvisKante.getId());
				newVonKnoten = Optional.empty();
				newNachKnoten = Optional.empty();
				radvisKantenVernetzungStatistik.anzahlRadVisKantenNichtVeraendertDaSonstLoop++;
			}

			// Ab hier nur noch ein bisschen logging und statistik fuehren
			if (!oldGeometry.equalsExact(radvisKante.getGeometry())) {
				radvisKantenVernetzungStatistik.anzahlRadVisKantenGeometrieKorrigiert++;
			}
			if ((vonKnotenMussNeuVernetztWerden && newVonKnoten.isEmpty())
				|| (nachKnotenMussNeuVernetztWerden && newNachKnoten.isEmpty())) {
				radvisKantenVernetzungStatistik.anzahlRadVisKantenVerwaistOderMitAnderenRadVisKantenVerbunden++;
			} else if ((vonKnotenMussNeuVernetztWerden && newVonKnoten.isPresent())
				|| (nachKnotenMussNeuVernetztWerden && newNachKnoten.isPresent())) {
				radvisKantenVernetzungStatistik.anzahlRadVisKantenTopologieKorrigiert++;
			}
		});

		radvisKantenVernetzungStatistik.anzahlKnotenGeloescht += netzService.deleteVerwaisteDLMKnoten(ausloeser,
			radvisKantenVernetzungStatistik.knotenDeleteStatistik);
		log.info("Vernetzungskorrektur von RadVIS-Kanten abgeschlossen");
	}

	private Optional<Knoten> findNewKnoten(Point atPosition, Knoten alterKnoten,
		List<Knoten> dlmKnotenWithoutDlmKanten, RadvisKantenVernetzungStatistik radvisKantenVernetzungStatistik,
		double maximalerKorrekturAbstand) {
		Optional<Knoten> newNetzknoten = knotenRepository
			.getKnotenInBereichFuerQuelle(createEnvelope(atPosition, maximalerKorrekturAbstand), QuellSystem.DLM)
			.stream().filter(k -> !k.equals(alterKnoten) && !dlmKnotenWithoutDlmKanten.contains(k))
			.filter(knoten -> knoten.getPoint().distance(atPosition) <= maximalerKorrekturAbstand)
			.min(Comparator.comparing(knoten -> knoten.getPoint().distance(atPosition)));

		if (!newNetzknoten.isPresent()) {
			radvisKantenVernetzungStatistik.anzahlKnotenBehalten++;
			log.debug("Von-Knoten " + alterKnoten + " wird noch von RadVIS-Kante referenziert und behalten");
		}
		return newNetzknoten;
	}

	private Envelope createEnvelope(Point point, double radius) {
		Coordinate coordinate = point.getCoordinate();
		return new Envelope(coordinate.x - radius, coordinate.x + radius, coordinate.y - radius, coordinate.y + radius);
	}
}
