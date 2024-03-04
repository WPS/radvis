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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import lombok.Getter;
import lombok.NonNull;

/**
 * Diese Klasse ist ein Zwischenartifakt der Attributeprojektion.
 * Hier werden für eine Grundnetzkante alle Attribute der Kanten einer Quelle, die mit ihr
 * eine Dublette bilden, gesammelt. Hier sind diese Attribute bereits auf die relevanten
 * Abschnitte zugeschnitten und auf die Grundnetzkante projiziert. Das heißt, die
 * Linearen Referenzen der Lineare Referenzierten Attribute gelten für die Grundnetzkante.
 * Diese gesammelten Attribute können jedoch inkonsistent sein, da potentiell mehrere Kanten
 * mit unterschiedlichen Attributen auf die selbe Grundnetzkante und den gleichen Bereich
 * projiziert werden könnten. Bevor Attribute auf die Grundnetzkante geschrieben werden können,
 * muss damit noch umgegangen werden.
 */
public class Attributprojektionsbeschreibung {

	@Getter
	@NonNull
	private final Kante zielnetzKante;

	@Getter
	@NonNull
	private final Map<KantenAttribute, Double> potentiellInkonsistenteProjizierteKantenattributeZuAnteil;

	@Getter
	@NonNull
	private final Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> potentiellInkonsistenteProjizierteNetzklassen;

	@Getter
	@NonNull
	private final Map<Set<IstStandard>, Double> potentiellInkonsistenteProjizierteIstStandards;

	@Getter
	@NonNull
	private final List<GeschwindigkeitAttribute> potentiellInkonsistenteProjizierteGeschwindigkeitsattribute;

	@Getter
	@NonNull
	private final List<ZustaendigkeitAttribute> potentiellInkonsistenteProjizierteZustaendigkeitAttribute;

	@NonNull
	@Getter
	final private List<SeitenbezogeneProjizierteAttribute> seitenbezogeneProjizierteAttribute;

	@Getter
	@NonNull
	private final Map<Long, Double> projizierteKanteIds;

	public Attributprojektionsbeschreibung(Kante kante) {
		this.zielnetzKante = kante;

		potentiellInkonsistenteProjizierteKantenattributeZuAnteil = new HashMap<>();
		potentiellInkonsistenteProjizierteNetzklassen = new HashMap<>();
		potentiellInkonsistenteProjizierteIstStandards = new HashMap<>();

		potentiellInkonsistenteProjizierteGeschwindigkeitsattribute = new ArrayList<>();
		potentiellInkonsistenteProjizierteZustaendigkeitAttribute = new ArrayList<>();

		seitenbezogeneProjizierteAttribute = new ArrayList<>();

		projizierteKanteIds = new HashMap<>();

	}

	public void addSegment(KantenSegment segment) {
		projizierteKanteIds.merge(segment.getQuellnetzKante().getId(),
			segment.getLinearReferenzierterAbschnittAufQuellnetzKante().relativeLaenge(), Double::sum);

		double anteilAnGrundnetzkante = segment.getLinearReferenzierterAbschnittAufZielnetzKante().relativeLaenge();

		// Zukünftig hier beide Fahrtrichtungen berücksichtigen (RAD-1082)
		seitenbezogeneProjizierteAttribute
			.add(new SeitenbezogeneProjizierteAttribute(segment.getLinearReferenzierterAbschnittAufZielnetzKante(),
				segment.getHaendigkeit(), segment.getFahrtrichtungLinks(),
				segment.getFuehrungsformAttributeZugeschnittenAufGrundnetzkante()));

		potentiellInkonsistenteProjizierteKantenattributeZuAnteil
			.merge(segment.getKantenAttribute(), anteilAnGrundnetzkante,
				Double::sum);

		potentiellInkonsistenteProjizierteNetzklassen
			.merge(segment.getNetzklassen(),
				new ArrayList<>(List.of(segment.getLinearReferenzierterAbschnittAufZielnetzKante())),
				(list1, list2) -> {
					list1.addAll(list2);
					return list1;
				});

		potentiellInkonsistenteProjizierteIstStandards
			.merge(segment.getIstStandards(), anteilAnGrundnetzkante, Double::sum);

		potentiellInkonsistenteProjizierteGeschwindigkeitsattribute
			.addAll(segment.getGeschwindigkeitAttributeZugeschnittenAufGrundnetzkante());

		potentiellInkonsistenteProjizierteZustaendigkeitAttribute
			.addAll(segment.getZustaendigkeitAttributeZugeschnittenAufGrundnetzkante());
	}

	public Map<Richtung, Double> getAllePotentiellInkonsistenteRichtungen() {
		Map<Richtung, Double> result = new HashMap<>();

		seitenbezogeneProjizierteAttribute.forEach(attribute -> {
			result.merge(attribute.getRichtung(),
				attribute.getLinearReferenzierterAbschnittAufZielnetzkante().relativeLaenge(),
				Double::sum);
		});
		return result;
	}

	public List<FuehrungsformAttribute> getAllePotentiellInkonsistenteFuehrungsformAttribute() {
		return seitenbezogeneProjizierteAttribute.stream()
			.map(SeitenbezogeneProjizierteAttribute::getFuehrungsformAttribute)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

}
