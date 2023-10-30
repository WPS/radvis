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

package de.wps.radvis.backend.netz.domain.bezug;

import static org.valid4j.Assertive.require;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

import de.wps.radvis.backend.netz.domain.entity.AbstractNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class MassnahmeNetzBezug extends AbstractNetzBezug {

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "massnahme_kantenseitenabschnitte")
	private Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezug;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "massnahme_kantenpunkte")
	private Set<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug;

	@OneToMany
	@JoinTable(name = "massnahme_knoten", joinColumns = { @JoinColumn(name = "massnahme_id") }, inverseJoinColumns = {
		@JoinColumn(name = "knoten_id") })
	private Set<Knoten> knotenBezug;

	public MassnahmeNetzBezug(Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezug,
		Set<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug,
		Set<Knoten> knotenBezug) {
		require(isValid(abschnittsweiserKantenSeitenBezug, punktuellerKantenSeitenBezug, knotenBezug),
			"Parameter nicht valide");

		this.abschnittsweiserKantenSeitenBezug = defragment(abschnittsweiserKantenSeitenBezug);
		this.punktuellerKantenSeitenBezug = new HashSet<>(punktuellerKantenSeitenBezug);
		this.knotenBezug = new HashSet<>(knotenBezug);
	}

	@Override
	protected Set<AbschnittsweiserKantenSeitenBezug> getMutableAbschnittsweiserKantenSeitenBezug() {
		return abschnittsweiserKantenSeitenBezug;
	}

	@Override
	protected Set<PunktuellerKantenSeitenBezug> getMutablePunktuellerKantenSeitenBezug() {
		return punktuellerKantenSeitenBezug;
	}

	@Override
	protected Set<Knoten> getMutableKnotenBezug() {
		return knotenBezug;
	}
}
