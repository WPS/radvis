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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import de.wps.radvis.backend.abfrage.auswertung.domain.entity.AuswertungsFilter;
import de.wps.radvis.backend.abfrage.auswertung.domain.repository.AuswertungRepository;

public class AuswertungRepositoryImpl implements AuswertungRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public BigInteger getCmAnzahl(AuswertungsFilter auswertungsFilter) {
		require(auswertungsFilter, notNullValue());

		String additionalWhereClauses = "";
		String additionalFrom = "";
		String additionalJoins = "";
		String berechneterAnteil = "1";
		Map<String, Object> additionalParameters = new HashMap<>();

		boolean addSqlForZustaendigkeit = false;

		// GemeindeKreisBezirk
		if (auswertungsFilter.getGemeindeKreisBezirkId() != null) {
			additionalFrom += " ,Organisation org";
			additionalWhereClauses += " AND org.id = :organisationsId";
			additionalWhereClauses += " AND st_intersects(kante.geometry, org.bereich)";
			additionalParameters.put("organisationsId", auswertungsFilter.getGemeindeKreisBezirkId());
		}

		// Baulast
		if (auswertungsFilter.getBaulastId() != null) {
			addSqlForZustaendigkeit = true;
			additionalWhereClauses += " AND za.baulast_traeger_id = :baulastId";
			additionalParameters.put("baulastId", auswertungsFilter.getBaulastId());
		}

		// Unterhalt
		if (auswertungsFilter.getUnterhaltId() != null) {
			addSqlForZustaendigkeit = true;
			additionalWhereClauses += " AND za.unterhalts_zustaendiger_id = :unterhaltId";
			additionalParameters.put("unterhaltId", auswertungsFilter.getUnterhaltId());
		}
		// Erhalt
		if (auswertungsFilter.getErhaltId() != null) {
			addSqlForZustaendigkeit = true;
			additionalWhereClauses += " AND za.erhalts_zustaendiger_id = :erhaltId";
			additionalParameters.put("erhaltId", auswertungsFilter.getErhaltId());
		}

		// Netzklassen
		if (auswertungsFilter.getNetzklassen() != null || auswertungsFilter.isBeachteNichtKlassifizierteKanten()) {
			additionalWhereClauses += " AND (nk.netzklasse in :netzklassen";
			additionalWhereClauses += auswertungsFilter.isBeachteNichtKlassifizierteKanten() ? " OR nk IS NULL" : "";
			additionalWhereClauses += ")";

			Set<String> netzklassenParam = Collections.emptySet();
			if (auswertungsFilter.getNetzklassen() != null) {
				netzklassenParam = auswertungsFilter.getNetzklassen().stream()
					.map(Enum::name)
					.collect(Collectors.toSet());
			}
			additionalParameters.put("netzklassen", netzklassenParam);
		}

		// IstStandards
		if (auswertungsFilter.getIstStandards() != null || auswertungsFilter.isBeachteKantenOhneStandards()) {
			additionalWhereClauses += " AND (standard.standard in :iststandards";
			additionalWhereClauses += auswertungsFilter.isBeachteKantenOhneStandards() ? " OR standard IS NULL" : "";
			additionalWhereClauses += ")";
			Set<String> iststandardsParam = Collections.emptySet();
			if (auswertungsFilter.getIstStandards() != null) {
				iststandardsParam = auswertungsFilter.getIstStandards().stream()
					.map(Enum::name)
					.collect(Collectors.toSet());
			}
			additionalParameters.put("iststandards", iststandardsParam);
			additionalJoins += " LEFT OUTER JOIN kanten_attribut_gruppe_ist_standards standard"
				+ " ON standard.kanten_attribut_gruppe_id = kag.id";
		}

		// Der Join für die ZuständigkeitsAttribute muss nach dem Join für die IstStandars kommen,
		// da der IstStandard-Join anhängig ist vom Join für die Kantenattributgruppe
		if (addSqlForZustaendigkeit) {
			additionalJoins += " LEFT OUTER JOIN zustaendigkeit_attribut_gruppe zag"
				+ " ON zag.id = kante.zustaendigkeit_attributgruppe_id"
				+ " LEFT OUTER JOIN zustaendigkeit_attribut_gruppe_zustaendigkeit_attribute za"
				+ " ON za.zustaendigkeit_attribut_gruppe_id = zag.id";

			berechneterAnteil = "COALESCE(SUM(DISTINCT za.bis) - SUM(DISTINCT za.von), 1)";
		}

		//@formatter:off
		String sqlString =
			// Wir müssen das Aufsummieren im Nachgang machen, um Kanten nicht ungewollt
			// mehrfach in die Summe aufzunehmen
			"SELECT CAST(COALESCE(SUM(effektive_kanten_laenge * anteil), 0) as bigint) FROM ("
				// In diesem Subselect gruppieren wir auf der KanteId, beachten die Filter und berechnen die korrekte Länge
				// über das Feld kanten_laenge unter Einbeziehung der Zweiseitigkeit
				+ "SELECT "
				// Falls an der Kante Zustaendigkeitsattribute hängen, die dem Filter (baulast, unterhalt, erhalt) entsprechen,
				// müssen wir die kanten_laenge anteilig berechnen, weil Zustaendigkeitsattribute linear referenziert sind.
				// Da wir dieselben Zustaendigkeitsattribute in mehreren Rows haben können, falls die Kante z.B. mehrere Netzklassen
				// oder Iststandards hat, müssen wir über 'DISTINCT' eine Deduplizierung vornehmen
				+ berechneterAnteil + " AS anteil, "
				// Wir berechnen die kanten_laenge doppelt, falls die Kante zweisetig ist
				+ "kante.kanten_laenge_in_cm * CASE WHEN kante.is_zweiseitig THEN 2 ELSE 1 END AS effektive_kanten_laenge"
				+ " FROM kante kante"
				+ " LEFT OUTER JOIN kanten_attribut_gruppe kag"
				+ " ON kag.id = kante.kanten_attributgruppe_id"
				+ " LEFT OUTER JOIN kanten_attribut_gruppe_netzklassen nk"
				+ " ON nk.kanten_attribut_gruppe_id = kag.id"
				+ additionalJoins
				+ additionalFrom
				// Wir betrachten nur Kanten mit Status.UNTER_VERKEHR
				+ " WHERE kag.status = 'UNTER_VERKEHR'"
				// Wir betrachten nur GrundnetzKanten, d.h.:
				// In qualitätsgesicherten Landkreisen: Kanten mit Quelle DLM oder Radvis
				// Sonst: Kanten mit Quelle RadNETZ sowie Kanten mit Quelle DLM oder RadVis,
				//        die keine RadNetzKlasse gesetzt haben
				+ " AND kante.is_grundnetz = true "
				+ additionalWhereClauses
				+ " GROUP BY kante.id) T1";

		Query query = entityManager
			.createNativeQuery(sqlString);
		additionalParameters.forEach(query::setParameter);

		return BigInteger.valueOf((Long)query.getSingleResult());
	}
}
