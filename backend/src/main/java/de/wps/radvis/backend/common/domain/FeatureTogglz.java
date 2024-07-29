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
	// Die Togglz müssen in der internen application.yml mit Initialwert definiert werden,
	// damit sie über Umgebungsvariablen überschrieben werden können.

	// Die '_' im Enum-Werten müssen in den Umgebungsvariablen entfernt werden.

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-2614")
	FAHRRADROUTE,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-3397")
	FAHRRADROUTE_FEHLER_INFOS,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-2717")
	FAHRRADROUTE_IMPORTPROTOKOLLE,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-2617")
	FEHLERPROTOKOLL,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4100")
	USE_LGL_HOEHENDATEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4183")
	DLM_REIMPORT_FIX,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-6118")
	IMPORT_MASSNAHMEN,
	@InfoLink("https://radviswps.atlassian.net/browse/RAD-6118")
	IMPORT_DATEIANHAENGE_MASSNAHMEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-6431")
	DEFAULT_VERSIONSINFO_DIALOG,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4211")
	ANPASSUNGEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4788")
	LOESCHEN_VON_ANPASSUNGSWUENSCHEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-2726")
	UMGESETZT_STATUS_AN_ANPASSUNGSWUENSCHEN_SCHREIBEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-553")
	FURTEN_KREUZUNGEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-553")
	BARRIEREN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4456")
	KONSISTENZREGELN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-2665")
	WEITERE_KARTENEBENEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4841")
	SERVICESTATIONEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4842")
	LEIHSTATIONEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4805")
	ABSTELLANLAGEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4773")
	WEGWEISENDE_BESCHILDERUNG,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4472")
	VERNETZUNG_KORREKTUR,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4813")
	LEIHSTATIONEN_CSV_IMPORT,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4811")
	SERVICESTATIONEN_CSV_IMPORT,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4812")
	ABSTELLANLAGEN_CSV_IMPORT,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4655")
	ORGANISATIONEN_ERSTELLEN_UND_BEARBEITEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-5252")
	DLM_REIMPORT_ANPASSUNG_FUER_INITIALEN_IMPORT,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-5151")
	VORDEFINIERTE_EXPORTE,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-5051")
	UMSETZUNGSSTANDABFRAGE_KREISKOORDINATOREN_BENACHRICHTIGEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-5064")
	KANTE_LOESCHEN_ENDPUNKT,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-5901")
	REFRESH_MATERIALIZED_VIEWS_ENDPUNKT,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-4719")
	SICHERHEITSTRENNSTREIFEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-2659")
	DATEILAYER_HOCHLADEN_ANZEIGEN,

	@InfoLink("https://radviswps.atlassian.net/browse/RAD-5105")
	BASIC_AUTH_VERWALTEN_ANZEIGEN;

	public boolean isActive() {
		return FeatureContext.getFeatureManager().isActive(this);
	}
}
