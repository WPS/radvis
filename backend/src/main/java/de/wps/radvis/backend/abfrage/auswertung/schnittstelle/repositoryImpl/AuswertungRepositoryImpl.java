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

package de.wps.radvis.backend.abfrage.auswertung.schnittstelle.repositoryImpl;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import de.wps.radvis.backend.abfrage.auswertung.domain.entity.AuswertungsFilter;
import de.wps.radvis.backend.abfrage.auswertung.domain.repository.AuswertungRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuswertungRepositoryImpl implements AuswertungRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public BigInteger getCmAnzahl(AuswertungsFilter auswertungsFilter) {
		require(auswertungsFilter, notNullValue());

		String additionalWhereClauses = "";
		String joins = "";
		Map<String, Object> additionalParameters = new HashMap<>();

		// GemeindeKreisBezirk
		if (auswertungsFilter.isWahlkreis()) {
			joins += " LEFT JOIN Wahlkreis wahlkreis ON wahlkreis.id = :wahlkreisId ";
			additionalWhereClauses += " AND st_intersects(abschnitt.geometry, wahlkreis.bereich)";
			additionalParameters.put("wahlkreisId", auswertungsFilter.getWahlkreisId());
		} else if (auswertungsFilter.isGemeindeKreisBezirk()) {
			joins += " LEFT JOIN Organisation org ON org.id = :organisationsId ";
			additionalWhereClauses += " AND st_intersects(abschnitt.geometry, org.bereich)";
			additionalParameters.put("organisationsId", auswertungsFilter.getGemeindeKreisBezirkId());
		}

		// Baulast
		if (auswertungsFilter.getBaulast() != null) {
			additionalWhereClauses += " AND abschnitt.baulast_traeger = :baulast";
			additionalParameters.put("baulast", getOrgaBezeichnungInMaterializedView(auswertungsFilter.getBaulast()));
		}

		// Unterhalt
		if (auswertungsFilter.getUnterhalt() != null) {
			additionalWhereClauses += " AND abschnitt.unterhalts_zustaendiger = :unterhalt";
			additionalParameters.put("unterhalt",
				getOrgaBezeichnungInMaterializedView(auswertungsFilter.getUnterhalt()));
		}

		// Erhalt
		if (auswertungsFilter.getErhalt() != null) {
			additionalWhereClauses += " AND abschnitt.erhalts_zustaendiger = :erhalt";
			additionalParameters.put("erhalt",
				getOrgaBezeichnungInMaterializedView(auswertungsFilter.getErhalt()));
		}

		// Netzklassen
		if (auswertungsFilter.getNetzklassen() != null || auswertungsFilter.isBeachteNichtKlassifizierteKanten()) {
			Set<String> netzklassenClauses = new HashSet<>();
			if (auswertungsFilter.getNetzklassen() != null) {
				auswertungsFilter.getNetzklassen().stream()
					.map(Enum::name)
					.map(nkStr -> "(abschnitt.netzklassen IS NOT NULL AND position('" + nkStr
						+ "' IN abschnitt.netzklassen ) > 0)")
					.forEach(netzklassenClauses::add);
			}

			if (auswertungsFilter.isBeachteNichtKlassifizierteKanten()) {
				netzklassenClauses.add("abschnitt.netzklassen IS NULL");
			}

			additionalWhereClauses +=
				" AND " + netzklassenClauses.stream().collect(Collectors.joining(" OR ", " (", ") "));
		}

		// IstStandards
		if (auswertungsFilter.getIstStandards() != null || auswertungsFilter.isBeachteKantenOhneStandards()) {
			Set<String> iststandardClauses = new HashSet<>();
			if (auswertungsFilter.getIstStandards() != null) {
				auswertungsFilter.getIstStandards().stream()
					.map(Enum::name)
					.map(standardStr -> "(abschnitt.standards IS NOT NULL AND position('" + standardStr
						+ "' IN abschnitt.standards ) > 0)")
					.forEach(iststandardClauses::add);
			}

			if (auswertungsFilter.isBeachteKantenOhneStandards()) {
				iststandardClauses.add("abschnitt.standards IS NULL");
			}

			additionalWhereClauses +=
				" AND " + iststandardClauses.stream().collect(Collectors.joining(" OR ", " (", ") "));

		}

		// BelagArt
		if (auswertungsFilter.getBelagArt() != null) {
			additionalWhereClauses += " AND abschnitt.belag_art = :belag";
			additionalParameters.put("belag", auswertungsFilter.getBelagArt().name());
		}

		// Radverkehrsführung
		if (auswertungsFilter.getRadverkehrsfuehrung() != null) {
			additionalWhereClauses += " AND abschnitt.radverkehrsfuehrung = :radverkehrsfuehrung";
			additionalParameters.put("radverkehrsfuehrung", auswertungsFilter.getRadverkehrsfuehrung().name());
		}

		String sqlString =
			"""
					SELECT COALESCE(sum(st_length(abschnitt.geometry)), 0) FROM geoserver_radvisnetz_kante_abschnitte_materialized_view abschnitt
				""" + joins + " WHERE abschnitt.status = 'UNTER_VERKEHR'" + additionalWhereClauses;

		Query query = entityManager
			.createNativeQuery(sqlString);
		additionalParameters.forEach(query::setParameter);

		return BigInteger.valueOf(Double.valueOf((double) query.getSingleResult() * 100.0).longValue());
	}

	@NotNull
	private static String getOrgaBezeichnungInMaterializedView(Verwaltungseinheit verwaltungseinheit) {
		return verwaltungseinheit.getName() + " (" + verwaltungseinheit.getOrganisationsArt().name() + ")";
	}
}
