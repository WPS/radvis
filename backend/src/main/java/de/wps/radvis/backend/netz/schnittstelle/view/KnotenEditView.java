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

import java.util.Optional;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class KnotenEditView {
	private final Long id;
	private final Geometry geometry;
	private final String ortslage;
	private final Optional<Kommentar> kommentar;
	private final Optional<Zustandsbeschreibung> zustandsbeschreibung;
	private final Optional<KnotenForm> knotenForm;
	private final Optional<VerwaltungseinheitView> gemeinde;
	private final Optional<VerwaltungseinheitView> landkreis;
	private final Long knotenVersion;
	private final QuellSystem quelle;
	private final boolean liegtInZustaendigkeitsbereich;
	private final Optional<QuerungshilfeDetails> querungshilfeDetails;
	private final Optional<Bauwerksmangel> bauwerksmangel;
	private final Optional<Set<BauwerksmangelArt>> bauwerksmangelArt;

	public KnotenEditView(Knoten knoten, KnotenOrtslage berechneteOrtslage, boolean liegtInZustaendigkeitsbereich) {
		id = knoten.getId();
		geometry = knoten.getPoint();
		ortslage = berechneteOrtslage != null ? berechneteOrtslage.toString() : null;
		kommentar = knoten.getKnotenAttribute().getKommentar();
		zustandsbeschreibung = knoten.getKnotenAttribute().getZustandsbeschreibung();
		knotenForm = knoten.getKnotenAttribute().getKnotenForm();

		quelle = knoten.getQuelle();
		this.liegtInZustaendigkeitsbereich = liegtInZustaendigkeitsbereich;

		gemeinde = knoten.getKnotenAttribute().getGemeinde().map(g -> new VerwaltungseinheitView(g));
		landkreis = knoten.getKnotenAttribute().getGemeinde()
			.flatMap(Verwaltungseinheit::getUebergeordneteVerwaltungseinheit)
			.map(l -> new VerwaltungseinheitView(l));

		knotenVersion = knoten.getVersion();
		querungshilfeDetails = knoten.getKnotenAttribute().getQuerungshilfeDetails();
		bauwerksmangel = knoten.getKnotenAttribute().getBauwerksmangel();
		bauwerksmangelArt = knoten.getKnotenAttribute().getBauwerksmangelArt();
	}
}
