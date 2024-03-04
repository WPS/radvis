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

package de.wps.radvis.backend.quellimport.ttsib.domain.entity;

import static org.valid4j.Assertive.require;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.Attribute;
import de.wps.radvis.backend.quellimport.ttsib.domain.KeinMittelstreifenException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@RequiredArgsConstructor
@Slf4j
public class TtSibAbschnittOderAst extends TtSibAbstractEntity {
	@Setter
	@Getter
	private String abschnittOderAstId;

	@Setter
	@Getter
	private Attribute attribute;

	@Setter
	@Getter
	private LineString geometry;

	@Getter
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "abschnitt_oder_ast_id", nullable = false)
	private final Set<TtSibTeilabschnitt> teilabschnitte = new HashSet<>();

	public void addTeilabschnitt(TtSibTeilabschnitt teilabschnitt) {
		require(teilabschnitte.stream().noneMatch(abschitt -> abschitt.ueberschneidet(teilabschnitt)),
			"Teilabschnitte überschneiden sich");

		teilabschnitte.add(teilabschnitt);
	}

	public Set<LineString> ermittleRadwegverlaeufe() {
		Set<LineString> radwegverlaeufe = new HashSet<>();
		this.teilabschnitte.forEach(teilabschnitt -> {
			try {
				Set<LineString> radwegverlaeufeProTeilabschnitt = teilabschnitt
					.ermittleRadwegverlaeufe(ermittleGeometrieFuerTeilabschnitt(teilabschnitt));
				radwegverlaeufe.addAll(radwegverlaeufeProTeilabschnitt);
			} catch (KeinMittelstreifenException e) {
				log.warn(
					"Kein Mittelstreifen gefunden für Teilabschnitt " + teilabschnitt.getId() + " an AoA: " + this.id);
			}
		});
		return radwegverlaeufe;
	}

	private LineString ermittleGeometrieFuerTeilabschnitt(TtSibTeilabschnitt teilabschnitt) {
		return (LineString) new LengthIndexedLine(this.geometry)
			.extractLine(teilabschnitt.getVonStation(), teilabschnitt.getBisStation());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TtSibAbschnittOderAst ttSibAoA = (TtSibAbschnittOderAst) o;
		return abschnittOderAstId.equals(ttSibAoA.abschnittOderAstId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(abschnittOderAstId);
	}
}
