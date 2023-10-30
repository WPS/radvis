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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.MehrdeutigeLinearReferenzierteAttributeException;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;

public class KantenAttributeMergeService {

	public KantenAttribute mergeKantenAttribute(KantenAttribute grundnetzAttribute,
		KantenAttribute attributeZumMergen, QuellSystem quelleDerZuMergendenAttribute) {
		if (QuellSystem.RadNETZ.equals(quelleDerZuMergendenAttribute)) {
			return mergeRadNetzKantenAttribute(grundnetzAttribute, attributeZumMergen);
		} else if (QuellSystem.RadwegeDB.equals(quelleDerZuMergendenAttribute)) {
			return mergeRadwegeDBKantenAttribute(grundnetzAttribute, attributeZumMergen);
		} else {
			throw new RuntimeException(
				"Es wurde versucht, Attribute zweier Quellsysteme zu mergen, für die keine Mergestrategie bekannt ist.");
		}
	}

	public Richtung mergeFahrtrichtung(Richtung fahrtrichtung, Richtung neueFahrtrichtung,
		QuellSystem quelleDerZuMergendenAttribute) {
		if (QuellSystem.RadNETZ.equals(quelleDerZuMergendenAttribute)) {
			return neueFahrtrichtung;
		} else if (QuellSystem.RadwegeDB.equals(quelleDerZuMergendenAttribute)) {
			return fahrtrichtung != Richtung.defaultWert() ? fahrtrichtung : neueFahrtrichtung;
		} else {
			throw new RuntimeException(
				"Es wurde versucht, Attribute zweier Quellsysteme zu mergen, für die keine Mergestrategie bekannt ist.");
		}
	}

	public GeschwindigkeitAttribute mergeGeschwindigkeitAttribute(GeschwindigkeitAttribute grundnetzAttribute,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		List<GeschwindigkeitAttribute> neueGeschwindigkeitAttribute, QuellSystem quelleDerZuMergendenAttribute)
		throws MehrdeutigeLinearReferenzierteAttributeException {

		if (neueGeschwindigkeitAttribute.isEmpty()) {
			return grundnetzAttribute.withLinearReferenzierterAbschnitt(linearReferenzierterAbschnitt);
		}

		if (quelleDerZuMergendenAttribute.equals(QuellSystem.RadNETZ)) {
			return mergeRadNetzGeschwindigkeitsAttributeValues(neueGeschwindigkeitAttribute,
				linearReferenzierterAbschnitt);
		} else if (quelleDerZuMergendenAttribute.equals(QuellSystem.RadwegeDB)) {
			return mergeRadwegeDBGeschwindigkeitsAttributeValues(grundnetzAttribute, neueGeschwindigkeitAttribute,
				linearReferenzierterAbschnitt);
		} else {
			throw new RuntimeException(
				"Es wurde versucht, die Geschwindigkeit zweier Quellsysteme zu mergen, für die keine Mergestrategie bekannt ist");
		}
	}

	private GeschwindigkeitAttribute mergeRadNetzGeschwindigkeitsAttributeValues(
		List<GeschwindigkeitAttribute> neueGeschwindigkeitAttribute,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt)
		throws MehrdeutigeLinearReferenzierteAttributeException {
		try {
			GeschwindigkeitAttribute reduzierteAttribute = neueGeschwindigkeitAttribute.stream()
				.reduce(neueGeschwindigkeitAttribute.get(0), this::reduzierNichtWiderspruechlichesSonstThrow);

			return GeschwindigkeitAttribute.builder()
				.ortslage(reduzierteAttribute.getOrtslage().orElse(null))
				.hoechstgeschwindigkeit(reduzierteAttribute.getHoechstgeschwindigkeit())
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
					reduzierteAttribute.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung().orElse(null))
				.linearReferenzierterAbschnitt(linearReferenzierterAbschnitt)
				.build();
		} catch (RuntimeException e) {
			throw new MehrdeutigeLinearReferenzierteAttributeException(GeschwindigkeitAttribute.class.getSimpleName());
		}
	}

	private GeschwindigkeitAttribute mergeRadwegeDBGeschwindigkeitsAttributeValues(
		GeschwindigkeitAttribute grundnetzAttribute,
		List<GeschwindigkeitAttribute> neueGeschwindigkeitAttribute,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt)
		throws MehrdeutigeLinearReferenzierteAttributeException {
		try {
			GeschwindigkeitAttribute reduzierteAttribute = neueGeschwindigkeitAttribute.stream()
				.reduce(neueGeschwindigkeitAttribute.get(0), this::reduzierNichtWiderspruechlichesSonstThrow);
			return grundnetzAttribute.mergeAttributeNimmErstenNichtDefaultWert(reduzierteAttribute,
				linearReferenzierterAbschnitt);
		} catch (RuntimeException e) {
			throw new MehrdeutigeLinearReferenzierteAttributeException(GeschwindigkeitAttribute.class.getSimpleName());
		}
	}

	public FuehrungsformAttribute mergeFuehrungsformAttribute(FuehrungsformAttribute grundnetzAttribut,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, List<FuehrungsformAttribute> attributeZumMergen,
		QuellSystem quelleDerZuMergendenAttribute) throws MehrdeutigeLinearReferenzierteAttributeException {

		if (QuellSystem.RadNETZ.equals(quelleDerZuMergendenAttribute)) {
			return mergeRadNetzFuehrungsformAttributeValues(grundnetzAttribut, attributeZumMergen,
				linearReferenzierterAbschnitt);
		} else if (QuellSystem.RadwegeDB.equals(quelleDerZuMergendenAttribute)) {
			return mergeRadwegeDBFuehrungsformAttributeValues(grundnetzAttribut, attributeZumMergen,
				linearReferenzierterAbschnitt);
		} else {
			throw new RuntimeException(
				"Es wurde versucht, Attribute zweier Quellsysteme zu mergen, für die keine Mergestrategie bekannt ist.");
		}
	}

	public ZustaendigkeitAttribute mergeZustaendigkeitAttribute(ZustaendigkeitAttribute grundnetzAttribut,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, List<ZustaendigkeitAttribute> attributeZumMergen,
		QuellSystem quelleDerZuMergendenAttribute) throws MehrdeutigeLinearReferenzierteAttributeException {

		if (QuellSystem.RadNETZ.equals(quelleDerZuMergendenAttribute)) {
			return mergeRadNetzZustaendigkeitattributeValues(grundnetzAttribut, attributeZumMergen,
				linearReferenzierterAbschnitt);
		} else if (QuellSystem.RadwegeDB.equals(quelleDerZuMergendenAttribute)) {
			return mergeRadwegeDBZustaendigkeitattributeValues(grundnetzAttribut, attributeZumMergen,
				linearReferenzierterAbschnitt);
		} else {
			throw new RuntimeException(
				"Es wurde versucht, Attribute zweier Quellsysteme zu mergen, für die keine Mergestrategie bekannt ist.");
		}
	}

	private KantenAttribute mergeRadNetzKantenAttribute(KantenAttribute grundnetzAttribute,
		KantenAttribute radNetzAttribute) {

		return KantenAttribute.builder()
			.wegeNiveau(radNetzAttribute.getWegeNiveau()
				.orElse(null))
			.beleuchtung(radNetzAttribute.getBeleuchtung())
			.strassenquerschnittRASt06(radNetzAttribute.getStrassenquerschnittRASt06())
			.umfeld(radNetzAttribute.getUmfeld())
			.laengeManuellErfasst(radNetzAttribute.getLaengeManuellErfasst()
				.orElse(null))
			.dtvFussverkehr(radNetzAttribute.getDtvFussverkehr()
				.orElse(null))
			.dtvRadverkehr(radNetzAttribute.getDtvRadverkehr()
				.orElse(null))
			.dtvPkw(radNetzAttribute.getDtvPkw()
				.orElse(null))
			.sv(radNetzAttribute.getSv().orElse(null))
			.kommentar(radNetzAttribute.getKommentar()
				.orElse(null))
			// Strassenname und strassennummer wird beim DLM Import gesetzt und kann deshalb schon Werte enthalten
			.strassenName(radNetzAttribute.getStrassenName()
				.orElse(grundnetzAttribute.getStrassenName().orElse(null)))
			.strassenNummer(radNetzAttribute.getStrassenNummer()
				.orElse(grundnetzAttribute.getStrassenNummer().orElse(null)))
			.gemeinde(radNetzAttribute.getGemeinde().orElse(null))
			.status(radNetzAttribute.getStatus())
			.build();
	}

	FuehrungsformAttribute mergeRadNetzFuehrungsformAttributeValues(
		FuehrungsformAttribute matchingGrundnetzAttribute,
		List<FuehrungsformAttribute> matchingProjizierteAttribute,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt)
		throws MehrdeutigeLinearReferenzierteAttributeException {
		if (matchingProjizierteAttribute.isEmpty()) {
			return matchingGrundnetzAttribute.withLinearReferenzierterAbschnitt(linearReferenzierterAbschnitt);
		}
		try {
			FuehrungsformAttribute reduzierteAttribute = matchingProjizierteAttribute.stream()
				.reduce(matchingProjizierteAttribute.get(0), this::reduzierNichtWiderspruechlichesSonstThrow);

			return FuehrungsformAttribute.builder()
				.belagArt(reduzierteAttribute.getBelagArt())
				.bordstein(reduzierteAttribute.getBordstein())
				.oberflaechenbeschaffenheit(reduzierteAttribute.getOberflaechenbeschaffenheit())
				.radverkehrsfuehrung(reduzierteAttribute.getRadverkehrsfuehrung())
				.parkenTyp(reduzierteAttribute.getParkenTyp())
				.parkenForm(reduzierteAttribute.getParkenForm())
				.breite(reduzierteAttribute.getBreite().orElse(null))
				.linearReferenzierterAbschnitt(linearReferenzierterAbschnitt)
				.benutzungspflicht(reduzierteAttribute.getBenutzungspflicht())
				.build();
		} catch (RuntimeException e) {
			throw new MehrdeutigeLinearReferenzierteAttributeException(FuehrungsformAttribute.class.getSimpleName());
		}
	}

	ZustaendigkeitAttribute mergeRadNetzZustaendigkeitattributeValues(
		ZustaendigkeitAttribute matchingGrundnetzAttribute,
		List<ZustaendigkeitAttribute> matchingProjizierteAttribute,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt)
		throws MehrdeutigeLinearReferenzierteAttributeException {

		if (matchingProjizierteAttribute.isEmpty()) {
			return matchingGrundnetzAttribute.withLinearReferenzierterAbschnitt(linearReferenzierterAbschnitt);
		}

		try {
			ZustaendigkeitAttribute reduzierteAttribute = matchingProjizierteAttribute.stream()
				.reduce(matchingProjizierteAttribute.get(0), this::reduzierNichtWiderspruechlichesSonstThrow);

			return ZustaendigkeitAttribute.builder()
				.baulastTraeger(reduzierteAttribute.getBaulastTraeger().orElse(null))
				.unterhaltsZustaendiger(reduzierteAttribute.getUnterhaltsZustaendiger().orElse(null))
				.erhaltsZustaendiger(reduzierteAttribute.getErhaltsZustaendiger().orElse(null))
				.vereinbarungsKennung(reduzierteAttribute.getVereinbarungsKennung().orElse(null))
				.linearReferenzierterAbschnitt(linearReferenzierterAbschnitt)
				.build();
		} catch (RuntimeException e) {
			throw new MehrdeutigeLinearReferenzierteAttributeException(ZustaendigkeitAttribute.class.getSimpleName());
		}
	}

	private KantenAttribute mergeRadwegeDBKantenAttribute(KantenAttribute grundnetzAttribute,
		KantenAttribute radwegeDBattribute) {
		return KantenAttribute.builder()
			.wegeNiveau(grundnetzAttribute.getWegeNiveau()
				.orElse(radwegeDBattribute.getWegeNiveau().orElse(null)))
			.beleuchtung(grundnetzAttribute.getBeleuchtung()
				.nichtUnbekanntOrElse(radwegeDBattribute.getBeleuchtung()))
			.strassenquerschnittRASt06(grundnetzAttribute.getStrassenquerschnittRASt06()
				.nichtUnbekanntOrElse(radwegeDBattribute.getStrassenquerschnittRASt06()))
			.umfeld(grundnetzAttribute.getUmfeld().nichtUnbekanntOrElse(radwegeDBattribute.getUmfeld()))
			.laengeManuellErfasst(grundnetzAttribute.getLaengeManuellErfasst()
				.orElse(radwegeDBattribute.getLaengeManuellErfasst().orElse(null)))
			.dtvFussverkehr(grundnetzAttribute.getDtvFussverkehr()
				.orElse(radwegeDBattribute.getDtvFussverkehr().orElse(null)))
			.dtvRadverkehr(grundnetzAttribute.getDtvRadverkehr()
				.orElse(radwegeDBattribute.getDtvRadverkehr().orElse(null)))
			.dtvPkw(grundnetzAttribute.getDtvPkw()
				.orElse(radwegeDBattribute.getDtvPkw().orElse(null)))
			.sv(grundnetzAttribute.getSv().orElse(radwegeDBattribute.getSv().orElse(null)))
			.kommentar(grundnetzAttribute.getKommentar()
				.orElse(radwegeDBattribute.getKommentar().orElse(null)))
			.gemeinde(grundnetzAttribute.getGemeinde().orElse(radwegeDBattribute.getGemeinde().orElse(null)))
			.strassenName(grundnetzAttribute.getStrassenName()
				.orElse(radwegeDBattribute.getStrassenName().orElse(null)))
			.strassenNummer(grundnetzAttribute.getStrassenNummer()
				.orElse(radwegeDBattribute.getStrassenNummer().orElse(null)))
			.status(grundnetzAttribute.getStatus().nichtDefaultOrElse(radwegeDBattribute.getStatus()))
			.build();
	}

	private FuehrungsformAttribute mergeRadwegeDBFuehrungsformAttributeValues(FuehrungsformAttribute grundnetzAttribut,
		List<FuehrungsformAttribute> quellnetzAttribute, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt)
		throws MehrdeutigeLinearReferenzierteAttributeException {
		if (quellnetzAttribute.isEmpty()) {
			return grundnetzAttribut.withLinearReferenzierterAbschnitt(linearReferenzierterAbschnitt);
		}
		try {
			FuehrungsformAttribute reduzierteAttribute = quellnetzAttribute.stream()
				.reduce(quellnetzAttribute.get(0), this::reduzierNichtWiderspruechlichesSonstThrow);

			return grundnetzAttribut.mergeAttributeNimmErstenNichtDefaultWert(reduzierteAttribute,
				linearReferenzierterAbschnitt);
		} catch (RuntimeException e) {
			throw new MehrdeutigeLinearReferenzierteAttributeException(FuehrungsformAttribute.class.getSimpleName());
		}
	}

	private ZustaendigkeitAttribute mergeRadwegeDBZustaendigkeitattributeValues(
		ZustaendigkeitAttribute grundnetzAttribut, List<ZustaendigkeitAttribute> attributeZumMergen,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt)
		throws MehrdeutigeLinearReferenzierteAttributeException {
		if (attributeZumMergen.isEmpty()) {
			return grundnetzAttribut.withLinearReferenzierterAbschnitt(linearReferenzierterAbschnitt);
		}

		try {
			ZustaendigkeitAttribute reduzierteAttribute = attributeZumMergen.stream()
				.reduce(attributeZumMergen.get(0), this::reduzierNichtWiderspruechlichesSonstThrow);

			return grundnetzAttribut.mergeAttributeNimmErstenNichtDefaultWert(reduzierteAttribute,
				linearReferenzierterAbschnitt);
		} catch (RuntimeException e) {
			throw new MehrdeutigeLinearReferenzierteAttributeException(ZustaendigkeitAttribute.class.getSimpleName());
		}
	}

	public Set<Netzklasse> mergeNetzklassen(Set<Netzklasse> alteNetzklassen,
		Set<Netzklasse> potentiellInkonsistenteProjizierteNetzklassen, QuellSystem quelleDerProjiziertenAttribute) {

		if (QuellSystem.RadwegeDB.equals(quelleDerProjiziertenAttribute)) {
			return alteNetzklassen;
		}

		Set<Netzklasse> neueNetzklassen = new HashSet<>(alteNetzklassen);
		neueNetzklassen.addAll(potentiellInkonsistenteProjizierteNetzklassen);

		return neueNetzklassen;
	}

	public Set<IstStandard> mergeIstStandards(Set<IstStandard> alteIstStandards,
		Set<IstStandard> potentiellInkonsistenteProjizierteIstStandards, QuellSystem quelleDerProjiziertenAttribute) {

		if (QuellSystem.RadwegeDB.equals(quelleDerProjiziertenAttribute)) {
			return alteIstStandards;
		}

		Set<IstStandard> neueIstStandards = new HashSet<>(alteIstStandards);
		neueIstStandards.addAll(potentiellInkonsistenteProjizierteIstStandards);

		return neueIstStandards;
	}

	private <T extends LinearReferenzierteAttribute> T reduzierNichtWiderspruechlichesSonstThrow(T attribut1,
		T attribute2)
		throws RuntimeException {
		if (!attribut1.widersprechenSichAttribute(attribute2)) {
			return attribut1.mergeAttributeNimmErstenNichtDefaultWert(attribute2,
				attribut1.getLinearReferenzierterAbschnitt());
		} else {
			throw new RuntimeException("Attribute sind widerspruechlich");
		}
	}
}
