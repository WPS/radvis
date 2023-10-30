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

package de.wps.radvis.backend.konsistenz.regeln.domain;

import java.util.List;

import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;

public interface Konsistenzregel {
	/**
	 * Gibt alle Verletzungen im aktuellen Datenbestand für diese Regel zurück
	 * 
	 * @return
	 */
	List<KonsistenzregelVerletzungsDetails> pruefen();

	/**
	 * Dieser String wird als Layer-Bezeichner verwendet und sollte keine Sonder- oder Leerzeichen enthalten
	 * 
	 * @return
	 */
	String getVerletzungsTyp();

	/**
	 * Dieser String wird in der Web-Oberfläche neben der Checkbox angezeigt
	 * 
	 * @return
	 */
	String getTitel();

	/**
	 * Wird zur Gruppierung der Checkboxen in der Web-Oberfläche verwendet
	 * 
	 * @return
	 */
	RegelGruppe getGruppe();
}
