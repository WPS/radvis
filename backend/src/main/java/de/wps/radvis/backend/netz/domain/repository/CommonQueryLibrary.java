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

package de.wps.radvis.backend.netz.domain.repository;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;

public class CommonQueryLibrary {

	public static Set<Netzklasse> getNetzklassenParameter(Set<NetzklasseFilter> netzklassen) {
		return netzklassen.stream()
			.filter(netzklasse -> netzklasse != NetzklasseFilter.NICHT_KLASSIFIZIERT)
			.flatMap(filter -> {
				switch (filter) {
				case RADNETZ:
					return Stream.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT,
						Netzklasse.RADNETZ_ZIELNETZ);
				case KREISNETZ:
					return Stream.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT);
				case KOMMUNALNETZ:
					return Stream.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT);
				case RADSCHNELLVERBINDUNG:
					return Stream.of(Netzklasse.RADSCHNELLVERBINDUNG);
				case RADVORRANGROUTEN:
					return Stream.of(Netzklasse.RADVORRANGROUTEN);
				default:
					throw new RuntimeException("NetzklasseFilter in getMapView für Kante nicht gemappt!");
				}
			})
			.collect(
				Collectors.toSet());
	}

	public static String whereClauseFuerNetzklassen(boolean includeNichtKlassifiziert) {
		StringBuilder hqlBuilder = new StringBuilder();
		hqlBuilder.append(" (nk in :netzklassen");
		if (includeNichtKlassifiziert) {
			hqlBuilder.append(" OR kag.netzklassen IS EMPTY");
		}
		hqlBuilder.append(")");

		return hqlBuilder.toString();
	}

	public static String whereClauseFuerNetzklassenInView(boolean includeNichtKlassifiziert) {
		StringBuilder hqlBuilder = new StringBuilder();
		hqlBuilder.append(" (nk in :netzklassen");
		if (includeNichtKlassifiziert) {
			hqlBuilder.append(" OR kante.netzklassen IS EMPTY");
		}
		hqlBuilder.append(")");

		return hqlBuilder.toString();
	}

	public static String whereClauseGrundnetzInView(boolean showDlm) {
		return whereClauseGrundnetz(showDlm);
	}

	public static String whereClauseGrundnetz(boolean showDlm) {
		StringBuilder hqlBuilder = new StringBuilder();
		if (showDlm) {
			//@formatter:off
			hqlBuilder
				.append(" (")
					.append("kante.quelle = ").append("'").append(QuellSystem.DLM.toString()).append("'")
				.append(" OR ")
				.	append(" kante.quelle = ").append("'").append(QuellSystem.RadVis.toString()).append("'")
				.append(")");
		} else {
			//@formatter:off
			hqlBuilder
				.append(" kante.isGrundnetz = true");
		}
		return hqlBuilder.toString();
	}

	public static String whereClauseFuerBereichKante() {
		return " intersects(CAST(kante.geometry AS org.locationtech.jts.geom.Geometry), CAST(:bereich as org.locationtech.jts.geom.Geometry)) = true";
	}

	public static String whereClauseFuerBereichKnoten() {
		return " intersects(CAST(knoten.point AS org.locationtech.jts.geom.Geometry), CAST(:bereich as org.locationtech.jts.geom.Geometry)) = true";
	}

	public static String eagerFetchVonKnoten() {
		return " LEFT OUTER JOIN FETCH kante.vonKnoten as k1";
	}

	public static String eagerFetchNachKnoten() {
		return " LEFT OUTER JOIN FETCH kante.nachKnoten as k2";
	}

	public static String eagerFetchFahrtrichtung() {
		return " LEFT OUTER JOIN FETCH kante.fahrtrichtungAttributGruppe as fahrtrichtung";
	}

	public static String eagerFetchFuehrungsform() {
		return " LEFT OUTER JOIN FETCH kante.fuehrungsformAttributGruppe as fuehrungsform";
	}

	public static String eagerFetchFuehrungsformAttributeLinks() {
		return " LEFT OUTER JOIN FETCH fuehrungsform.fuehrungsformAttributeLinks as fuehrungsformAttributeLinks";
	}

	public static String eagerFetchFuehrungsformAttributeRechts() {
		return " LEFT OUTER JOIN FETCH fuehrungsform.fuehrungsformAttributeRechts as fuehrungsformAttributeRechts";
	}

	public static String joinNetzklassen() {
		return " LEFT JOIN kante.kantenAttributGruppe as kag "
			+ " LEFT JOIN kag.netzklassen as nk ";
	}
}
