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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain;

import java.util.Set;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.GeometrienVerlaufMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteFuehrungsformAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteGeschwindigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteZustaendigkeitAttributeView;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;

public interface KantenAbfrageRepository {
	Set<KanteMapView> getKantenMapViewInBereich(Envelope bereich, Set<NetzklasseFilter> netzklassen);

	Set<KanteMapView> getKantenMapViewInBereichFuerQuelle(Envelope bereich, QuellSystem quelle);

	Set<GeometrienVerlaufMapView> getGeometrienVerlaufMapViewInBereich(Envelope bereich,
		Set<NetzklasseFilter> netzklassen);

	Set<KanteGeschwindigkeitAttributeView> getKanteGeschwindigkeitAttributeViewInBereichNachNetzklasse(Envelope bereich,
		Set<NetzklasseFilter> netzklassen, boolean showDLM);

	Set<KanteFuehrungsformAttributeView> getKanteFuehrungsformAttributeViewInBereichNachNetzklasse(
		Envelope bereich,
		Set<NetzklasseFilter> netzklassen, boolean showDLM);

	Set<KanteZustaendigkeitAttributeView> getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse(
		Envelope bereich,
		Set<NetzklasseFilter> netzklassen, boolean showDLM);

	Set<KanteNetzklasseMapView> getKantenMapViewInBereichDlm(Envelope bereich);

	Set<KanteNetzklasseMapView> getKantenMapViewInBereichDlmIstRadNETZZugeordnet(Envelope bereich);
}