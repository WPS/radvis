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

package de.wps.radvis.backend.netz.schnittstelle.command;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;

public class SaveNetzBezugCommandConverter {

	protected final KanteResolver kantenResolver;

	public SaveNetzBezugCommandConverter(KanteResolver kantenResolver) {
		this.kantenResolver = kantenResolver;
	}

	protected AbschnittsweiserKantenSeitenBezug createKantenSeitenAbschnitt(
		SeitenabschnittsKantenBezugCommand seitenabschnittsKantenBezugCommand) {

		Kante kante = kantenResolver.getKante(seitenabschnittsKantenBezugCommand.getKanteId());
		LinearReferenzierterAbschnitt abschnitt = seitenabschnittsKantenBezugCommand.getLinearReferenzierterAbschnitt();
		Seitenbezug seitenbezug = seitenabschnittsKantenBezugCommand.getSeitenbezug();
		return new AbschnittsweiserKantenSeitenBezug(kante, abschnitt, seitenbezug);
	}

	protected PunktuellerKantenSeitenBezug createPunktuellerKantenSeitenBezug(
		PunktuellerKantenSeitenBezugCommand punktuellerKantenSeitenBezugCommand) {

		Kante kante = kantenResolver.getKante(punktuellerKantenSeitenBezugCommand.getKanteId());
		LineareReferenz lineareReferenz = LineareReferenz.of(punktuellerKantenSeitenBezugCommand.getLineareReferenz());
		Seitenbezug seitenbezug = Seitenbezug.BEIDSEITIG;
		return new PunktuellerKantenSeitenBezug(kante, lineareReferenz, seitenbezug);
	}

}
