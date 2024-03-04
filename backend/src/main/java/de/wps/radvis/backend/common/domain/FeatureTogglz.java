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

package de.wps.radvis.backend.common.domain;

import org.togglz.core.Feature;
import org.togglz.core.annotation.InfoLink;
import org.togglz.core.context.FeatureContext;

public enum FeatureTogglz implements Feature {
	// Die Togglz müssen in der internen application.yml mit Initialwert definert
	// werden,
	// damit sie über Umgebungsvariablen überschrieben werden können.

	// Die '_' im Enum-Werten müssen in den Umgebungsvariablen entfernt werden.

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-2614")
	FAHRRADROUTE,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4229")
	FAHRRADROUTE_JOBS,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-3397")
	FAHRRADROUTE_FEHLER_INFOS,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-2717")
	FAHRRADROUTE_IMPORTPROTOKOLLE,

	// Dieses FeatureToggle steuert, ob der RadNETZ-Strecken-Cache gebaut und
	// verwendet werden soll.
	// Ist es deaktiviert, ist das RadNETZ auf weiter Zoomstufe nicht nichtbar.
	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-3530")
	RADNETZ_STRECKEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-2617")
	FEHLERPROTOKOLL,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4100")
	USE_LGL_HOEHENDATEN,

	// Hiermit kann naechtlich im Rahmen des ProfiInformationenUpdateJob bei Routen
	// ohne
	// NetzbezugLineString versucht werden ebendiesen neu zu erstellen
	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4234")
	NETZBEZUGLINESTRING_RETRY,

	DLM_REIMPORT_NAECHTLICH_AUSFUEHREN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4183")
	DLM_REIMPORT_FIX,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-6118")
	IMPORT_MASSNAHMEN,
	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-6118")
	IMPORT_DATEIANHAENGE_MASSNAHMEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4211")
	ANPASSUNGEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4788")
	LOESCHEN_VON_ANPASSUNGSWUENSCHEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-2726")
	UMGESETZT_STATUS_AN_ANPASSUNGSWUENSCHEN_SCHREIBEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-553")
	FURTEN_KREUZUNGEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-553")
	BARRIEREN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4456")
	KONSISTENZREGELN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4246")
	RUN_DLM_PBF_ON_STARTUP,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-2665")
	WEITERE_KARTENEBENEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4841")
	SERVICESTATIONEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4842")
	LEIHSTATIONEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4805")
	ABSTELLANLAGEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4773")
	WEGWEISENDE_BESCHILDERUNG,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4472")
	VERNETZUNG_KORREKTUR,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4813")
	LEIHSTATIONEN_CSV_IMPORT,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4811")
	SERVICESTATIONEN_CSV_IMPORT,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4812")
	ABSTELLANLAGEN_CSV_IMPORT,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4655")
	ORGANISATIONEN_ERSTELLEN_UND_BEARBEITEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-5212")
	NETZ_AUF_OSM_MATCHING_NAECHTLICH_AUSFUEHREN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-5252")
	DLM_REIMPORT_ANPASSUNG_FUER_INITIALEN_IMPORT,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-5151")
	VORDEFINIERTE_EXPORTE,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-5051")
	UMSETZUNGSSTANDABFRAGE_KREISKOORDINATOREN_BENACHRICHTIGEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-5064")
	KANTE_LOESCHEN_ENDPUNKT,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-5901")
	REFRESH_MATERIALIZED_VIEWS_ENDPUNKT,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-5052")
	D_ROUTEN_NAECHTLICH_NETZBEZUG_MATCHVERSUCH,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-4719")
	SICHERHEITSTRENNSTREIFEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-2659")
	DATEILAYER_HOCHLADEN_ANZEIGEN,

	@InfoLink("https://bis2wps.atlassian.net/browse/RAD-5105")
	BASIC_AUTH_VERWALTEN_ANZEIGEN;

	public boolean isActive() {
		return FeatureContext.getFeatureManager().isActive(this);
	}
}
