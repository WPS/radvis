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

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Envelope;
import org.springframework.data.domain.Slice;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.dbView.KanteOsmMatchWithAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.KanteElevationUpdate;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public interface CustomKantenRepository {
	Set<Kante> getKantenInBereichNachNetzklasse(Envelope bereich, Set<NetzklasseFilter> netzklassen, boolean showDLM);

	Set<Kante> getKantenInBereichNachQuelleUndIsAbgebildet(Envelope bereich, QuellSystem quelle);

	Stream<Kante> getKantenInBereichNachQuelle(Envelope bereich, QuellSystem quelle);

	Stream<Kante> getKantenInBereichNachQuellen(Envelope bereich, Set<QuellSystem> quellen);

	List<Kante> getKantenForNetzklassenEagerFetchKnoten(
		Set<Netzklasse> netzklassen);

	Stream<Kante> getKantenInBereichNachQuellenEagerFetchFahrtrichtungEagerFetchFuehrungsformAttributeLinks(
		Envelope bereich, Set<QuellSystem> quellen);

	Stream<Kante> getKantenInBereichNachQuelleEagerFetchKnoten(Envelope bereich, QuellSystem quelle);

	Stream<Kante> getKantenInBereichNachQuelleEagerFetchKantenAttribute(Envelope bereich, QuellSystem quelle);

	List<Kante> getKantenInBereichNachQuelleList(Envelope bereich, QuellSystem quelle);

	void buildIndex();

	Set<Kante> getAlleKantenEinesKnotens(Knoten knoten);

	Set<Kante> getKantenInOrganisationsbereich(Verwaltungseinheit organisation);

	Set<Kante> getKantenInOrganisationsbereichEagerFetchNetzklassen(Verwaltungseinheit organisation);

	Set<Kante> getKantenimBereich(Envelope bereich);

	Stream<Kante> getKantenInOrganisationsbereichEagerFetchKnoten(Verwaltungseinheit organisation);

	void insertOsmWayIds(List<KanteOsmWayIdsInsert> inserts);

	void truncateOsmWayIds();

	List<KanteGeometryView> getFuerOsmAbbildungRelevanteKanten(Envelope bereich);

	Stream<KanteOsmMatchWithAttribute> getKanteOsmMatchesWithOsmAttributes(
		double minimaleUeberdeckungFuerAttributAuszeichnung);

	Stream<Kante> getEinseitigBefahrbareKanten();

	void refreshRadVisNetzMaterializedView();

	void refreshRadVisNetzAbschnitteMaterializedView();

	void updateKanteElevation(Slice<KanteElevationUpdate> kanteElevationInserts);
}
