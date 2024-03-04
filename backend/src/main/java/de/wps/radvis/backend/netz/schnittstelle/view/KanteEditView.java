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
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class KanteEditView {
	private final Long id;
	private final Long kantenVersion;
	private final Geometry geometry;
	private final Geometry verlaufLinks;
	private final Geometry verlaufRechts;
	private final Laenge laengeBerechnet;
	private final boolean isZweiseitig;
	private final boolean isGeometrieAenderungErlaubt;
	private final boolean liegtInZustaendigkeitsbereich;
	private final boolean isLoeschenErlaubt;
	private final QuellSystem quelle;

	private final ZustaendigkeitAttributGruppeEditView zustaendigkeitAttributGruppe;
	private final GeschwindigkeitAttributGruppeEditView geschwindigkeitAttributGruppe;
	private final FuehrungsformAttributGruppeEditView fuehrungsformAttributGruppe;
	private final FahrtrichtungAttributGruppeEditView fahrtrichtungAttributGruppe;
	private final KantenAttributGruppeEditView kantenAttributGruppe;

	public KanteEditView(Kante kante, boolean liegtInZustaendigkeitsbereich, boolean isLoeschenErlaubt) {
		id = kante.getId();
		kantenVersion = kante.getVersion();
		geometry = kante.getGeometry();
		verlaufLinks = kante.getVerlaufLinks().orElse(null);
		verlaufRechts = kante.getVerlaufRechts().orElse(null);

		laengeBerechnet = kante.getLaengeBerechnet();
		isZweiseitig = kante.isZweiseitig();
		isGeometrieAenderungErlaubt = kante.isManuelleGeometrieAenderungErlaubt();
		this.liegtInZustaendigkeitsbereich = liegtInZustaendigkeitsbereich;
		this.isLoeschenErlaubt = isLoeschenErlaubt;

		quelle = kante.getQuelle();

		this.kantenAttributGruppe = new KantenAttributGruppeEditView(kante.getKantenAttributGruppe());

		this.zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppeEditView(
			kante.getZustaendigkeitAttributGruppe());

		this.geschwindigkeitAttributGruppe = new GeschwindigkeitAttributGruppeEditView(
			kante.getGeschwindigkeitAttributGruppe());

		this.fuehrungsformAttributGruppe = new FuehrungsformAttributGruppeEditView(
			kante.getFuehrungsformAttributGruppe());

		this.fahrtrichtungAttributGruppe = new FahrtrichtungAttributGruppeEditView(
			kante.getFahrtrichtungAttributGruppe());
	}
}
