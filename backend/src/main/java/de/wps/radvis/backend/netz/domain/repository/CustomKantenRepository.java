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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Slice;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.dbView.KanteOsmMatchWithAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.entity.NahegelegeneneKantenDbView;
import de.wps.radvis.backend.netz.domain.valueObject.KanteElevationUpdate;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public interface CustomKantenRepository {
	/**
	 * @return Alle RadVIS- und DLM-Kanten im angegebenen Bereich.
	 */
	Set<Kante> getKantenInBereich(Envelope bereich);

	/**
	 * @return Alle RadVIS- und DLM-Kanten im angegebenen Bereich.
	 */
	Set<Kante> getKantenInBereich(Geometry bereich);

	Set<Kante> getKantenInBereichNachNetzklasse(Envelope bereich, Set<NetzklasseFilter> netzklassen, boolean showDLM);

	Set<Kante> getKantenInBereichNachQuelleUndIsAbgebildet(Envelope bereich, QuellSystem quelle);

	Stream<Kante> getKantenInBereichNachQuellen(Envelope bereich, Collection<QuellSystem> quellen);

	List<Kante> getKantenForNetzklassenEagerFetchKnoten(
		Set<Netzklasse> netzklassen);

	Stream<Kante> getKantenInBereichNachQuellenEagerFetchFahrtrichtungEagerFetchFuehrungsformAttributeLinks(
		Envelope bereich, Set<QuellSystem> quellen);

	void buildIndex();

	/**
	 * Ermittelt für einen Abschnitt einer gegebenen Kante alle anderen Kanten, die sich innerhalb eines gewissen
	 * Abstandes auf der angegebenen Seite befinden. Hierbei wird NICHT auf Parallelität, Anteil in der Nähe oder
	 * sonstige Güte-Metriken geschaut.
	 *
	 * @return Liste von nahegelegenen Kanten inklusive der Abschnitte, die sich tatsächlich in der Nähe befinden.
	 */
	List<NahegelegeneneKantenDbView> getNahegelegeneKantenAufSeite(Kante basiskante,
		LinearReferenzierterAbschnitt abschnitt, Seitenbezug seite, Laenge abstandInM);

	Set<Kante> getKantenInOrganisationsbereichEagerFetchNetzklassen(Verwaltungseinheit organisation);

	Stream<Kante> getKantenInOrganisationsbereichEagerFetchKnoten(Verwaltungseinheit organisation);

	void insertOsmWayIds(List<KanteOsmWayIdsInsert> inserts);

	void truncateOsmWayIds();

	List<KanteGeometryView> getFuerOsmAbbildungRelevanteKanten(Envelope bereich);

	Stream<KanteOsmMatchWithAttribute> getKanteOsmMatchesWithOsmAttributes(
		double minimaleUeberdeckungFuerAttributAuszeichnung);

	Stream<Kante> getEinseitigBefahrbareKanten();

	void refreshNetzMaterializedViews();

	void updateKanteElevation(Slice<KanteElevationUpdate> kanteElevationInserts);

	/**
	 * Ergänzt fehlende Auditing-Einträge an Kanten und ihren Attributgruppen.
	 *
	 * @return Anzahl an Kanten, bei denen Auditing-Einträge ergänzt wurden.
	 */
	HashMap<String, Integer> addMissingAuditingEntries(Long benutzerId, int batchSize, String auditingContextName,
		Long jobExecutionDescriptionId);
}
