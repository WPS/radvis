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

package de.wps.radvis.backend.netz.schnittstelle.view;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class KnotenEditView {
	private final Long id;
	private final Geometry geometry;
	private final String ortslage;
	private final Kommentar kommentar;
	private final Zustandsbeschreibung zustandsbeschreibung;
	private final KnotenForm knotenForm;
	private VerwaltungseinheitView gemeinde;
	private VerwaltungseinheitView landkreis;
	private final Long knotenVersion;
	private final QuellSystem quelle;
	private final boolean liegtInZustaendigkeitsbereich;

	public KnotenEditView(Knoten knoten, KnotenOrtslage berechneteOrtslage, boolean liegtInZustaendigkeitsbereich) {
		id = knoten.getId();
		geometry = knoten.getPoint();
		ortslage = berechneteOrtslage != null ? berechneteOrtslage.toString() : null;
		kommentar = knoten.getKnotenAttribute().getKommentar().orElse(null);
		zustandsbeschreibung = knoten.getKnotenAttribute().getZustandsbeschreibung().orElse(null);
		knotenForm = knoten.getKnotenAttribute().getKnotenForm().orElse(null);

		quelle = knoten.getQuelle();
		this.liegtInZustaendigkeitsbereich = liegtInZustaendigkeitsbereich;

		knoten.getKnotenAttribute().getGemeinde().ifPresent(g -> gemeinde = new VerwaltungseinheitView(g));
		knoten.getKnotenAttribute().getGemeinde().flatMap(Verwaltungseinheit::getUebergeordneteVerwaltungseinheit)
			.ifPresent(l -> landkreis = new VerwaltungseinheitView(l));

		knotenVersion = knoten.getVersion();
	}
}
