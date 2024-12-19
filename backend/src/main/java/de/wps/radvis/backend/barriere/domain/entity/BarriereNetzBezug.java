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

package de.wps.radvis.backend.barriere.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.AbstractNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class BarriereNetzBezug extends AbstractNetzBezug {

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "barriere_kantenseitenabschnitte")
	private Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezug;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "barriere_kantenpunkte")
	private Set<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug;

	@OneToMany
	@JoinTable(name = "barriere_knoten", joinColumns = { @JoinColumn(name = "barriere_id") }, inverseJoinColumns = {
		@JoinColumn(name = "knoten_id") })
	private Set<Knoten> knotenBezug;

	public BarriereNetzBezug(Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezug,
		Set<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug,
		Set<Knoten> knotenBezug) {
		require(isValid(abschnittsweiserKantenSeitenBezug, punktuellerKantenSeitenBezug, knotenBezug),
			"Parameter nicht valide");

		this.abschnittsweiserKantenSeitenBezug = defragment(abschnittsweiserKantenSeitenBezug);
		this.punktuellerKantenSeitenBezug = new HashSet<>(punktuellerKantenSeitenBezug);
		this.knotenBezug = new HashSet<>(knotenBezug);
	}

	public BarriereNetzBezug withKanteErsetzt(Kante zuErsetzendeKante, Set<Kante> zuErsetzenDurch,
		double erlaubteAbweichung) {
		require(zuErsetzenDurch, notNullValue());
		require(!zuErsetzenDurch.isEmpty());

		return new BarriereNetzBezug(
			AbschnittsweiserKantenSeitenBezug.ersetzeKanteInAbschnitten(abschnittsweiserKantenSeitenBezug,
				zuErsetzendeKante,
				zuErsetzenDurch, erlaubteAbweichung),
			AbschnittsweiserKantenSeitenBezug.ersetzeKanteInPunkten(punktuellerKantenSeitenBezug, zuErsetzendeKante,
				zuErsetzenDurch,
				erlaubteAbweichung),
			knotenBezug);
	}

	@Override
	public Set<AbschnittsweiserKantenSeitenBezug> getImmutableKantenAbschnittBezug() {
		return Collections.unmodifiableSet(abschnittsweiserKantenSeitenBezug);
	}

	@Override
	public Set<PunktuellerKantenSeitenBezug> getImmutableKantenPunktBezug() {
		return Collections.unmodifiableSet(punktuellerKantenSeitenBezug);
	}

	@Override
	public Set<Knoten> getImmutableKnotenBezug() {
		return Collections.unmodifiableSet(knotenBezug);
	}

	public BarriereNetzBezug withoutKanten(Set<Long> kantenIds) {
		BarriereNetzBezug result = new BarriereNetzBezug();
		result.abschnittsweiserKantenSeitenBezug = abschnittsweiserKantenSeitenBezug
			.stream().filter(
				kantenBezug -> !kantenIds.contains(kantenBezug.getKante().getId()))
			.collect(Collectors.toSet());
		result.punktuellerKantenSeitenBezug = punktuellerKantenSeitenBezug.stream()
			.filter(punkt -> !kantenIds.contains(punkt.getKante().getId()))
			.collect(Collectors.toSet());
		result.knotenBezug = new HashSet<>(knotenBezug);
		return result;
	}

	public BarriereNetzBezug withoutKnoten(Set<Long> knotenIds) {
		BarriereNetzBezug result = new BarriereNetzBezug();
		result.abschnittsweiserKantenSeitenBezug = new HashSet<>(abschnittsweiserKantenSeitenBezug);
		result.punktuellerKantenSeitenBezug = new HashSet<>(punktuellerKantenSeitenBezug);
		result.knotenBezug = knotenBezug.stream().filter(k -> !knotenIds.contains(k.getId()))
			.collect(Collectors.toSet());
		return result;
	}

	public BarriereNetzBezug withKnotenErsetzt(Map<Long, Knoten> ersatzKnoten) {
		return new BarriereNetzBezug(getImmutableKantenAbschnittBezug(),
			getImmutableKantenPunktBezug(), ersetzeKnoten(getImmutableKnotenBezug(), ersatzKnoten));
	}
}
