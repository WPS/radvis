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

import static de.wps.radvis.backend.konsistenz.regeln.domain.RegelGruppe.FAHRTRICHTUNG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;

public class FahrtrichtungKonsistenzregel implements Konsistenzregel {
	public static String VERLETZUNGS_TYP = "FAHRTRICHTUNG_AN_KNOTEN_GEGENSAETZLICH";
	public static String TITEL = "Knoten mit gegensätzlicher Fahrtrichtung";
	public static String BESCHREIBUNG = "Die Einrichtungsführung könnte unplausibel sein. Dies kann zum Beispiel der Fall sein, weil mehrere Kanten"
		+ " sich in einem Knoten treffen oder weil mehrere Kanten von diesem Knoten wegführen.";
	private final KantenRepository kantenRepository;

	public FahrtrichtungKonsistenzregel(KantenRepository kantenRepository) {
		this.kantenRepository = kantenRepository;
	}

	public enum RichtungAusKnotenPerspektive {
		INCOMING,
		OUTGOING
	}

	@Override
	public List<KonsistenzregelVerletzungsDetails> pruefen() {
		// aus Sicht des Knotens ist die fahrtrichtung entweder zu ihm hin oder von ihm weg
		Map<Knoten, List<RichtungAusKnotenPerspektive>> knotenAufRichtungAnliegenderKanten = new HashMap<>();

		kantenRepository.getEinseitigBefahrbareKanten()
			.forEach(k -> {
				Richtung fahrtrichtungZuStationierungsrichtung = k.getFahrtrichtungAttributGruppe()
					.getFahrtrichtungLinks();

				RichtungAusKnotenPerspektive ausVonKnotenPerspektive = mapRichtungZuKnotenperspektive(
					fahrtrichtungZuStationierungsrichtung, true);

				knotenAufRichtungAnliegenderKanten.compute(k.getVonKnoten(), (key, val) -> {
					if (val == null) {
						ArrayList<RichtungAusKnotenPerspektive> list = new ArrayList<>();
						list.add(ausVonKnotenPerspektive);
						return list;
					} else {
						val.add(ausVonKnotenPerspektive);
						return val;
					}
				});

				RichtungAusKnotenPerspektive ausNachKnotenPerspektive = mapRichtungZuKnotenperspektive(
					fahrtrichtungZuStationierungsrichtung, false);

				knotenAufRichtungAnliegenderKanten.compute(k.getNachKnoten(),
					(Knoten key, List<RichtungAusKnotenPerspektive> val) -> {
						if (val == null) {
							ArrayList<RichtungAusKnotenPerspektive> list = new ArrayList<>();
							list.add(ausNachKnotenPerspektive);
							return list;
						} else {
							val.add(ausNachKnotenPerspektive);
							return val;
						}
					});

			});

		return knotenAufRichtungAnliegenderKanten.entrySet().stream()
			.filter(
				entry -> entry.getValue().stream().filter(r -> r == RichtungAusKnotenPerspektive.INCOMING).count() > 1
					|| entry.getValue().stream().filter(r -> r == RichtungAusKnotenPerspektive.OUTGOING).count() > 1)
			.map(Map.Entry::getKey)
			.map(this::createVerletzung)
			.collect(Collectors.toList());
	}

	private KonsistenzregelVerletzungsDetails createVerletzung(Knoten knoten) {
		return new KonsistenzregelVerletzungsDetails(knoten.getPoint(), BESCHREIBUNG,
			knoten.getId().toString());
	}

	public RichtungAusKnotenPerspektive mapRichtungZuKnotenperspektive(Richtung richtung, boolean istVonKnoten) {
		if (!istVonKnoten) {
			richtung = richtung.umgedreht();
		}
		switch (richtung) {
		case IN_RICHTUNG:
			return RichtungAusKnotenPerspektive.OUTGOING;
		case GEGEN_RICHTUNG:
			return RichtungAusKnotenPerspektive.INCOMING;
		default:
			throw new RuntimeException("Fahrtrichtung nicht einseitig!");
		}
	}

	@Override
	public String getVerletzungsTyp() {
		return VERLETZUNGS_TYP;
	}

	@Override
	public String getTitel() {
		return TITEL;
	}

	@Override
	public RegelGruppe getGruppe() {
		return FAHRTRICHTUNG;
	}
}
