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

package de.wps.radvis.backend.wegweisendeBeschilderung.schnittstelle.view;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderung;
import lombok.Getter;

@Getter
public class WegweisendeBeschilderungListenView {
	private final long id;
	private final Point geometrie;
	private final String pfostenNr;
	private final String wegweiserTyp;
	private final String pfostenTyp;
	private final String zustandsbewertung;
	private final String defizit;
	private final String pfostenzustand;
	private final String pfostendefizit;
	private final String gemeinde;
	private final String kreis;
	private final String land;

	private final VerwaltungseinheitView zustaendig;

	public WegweisendeBeschilderungListenView(WegweisendeBeschilderung wegweisendeBeschilderung) {
		this.id = wegweisendeBeschilderung.getId();
		this.zustaendig = new VerwaltungseinheitView(wegweisendeBeschilderung.getZustaendigeVerwaltungseinheit());

		this.geometrie = (Point) wegweisendeBeschilderung.getGeometrie();
		this.pfostenNr = wegweisendeBeschilderung.getPfostenNr().getValue();
		this.wegweiserTyp = wegweisendeBeschilderung.getWegweiserTyp().getValue();
		this.pfostenTyp = wegweisendeBeschilderung.getPfostenTyp().getValue();
		this.zustandsbewertung = wegweisendeBeschilderung.getZustandsbewertung().getValue();
		this.defizit = wegweisendeBeschilderung.getDefizit().getValue();
		this.pfostenzustand = wegweisendeBeschilderung.getPfostenzustand().getValue();
		this.pfostendefizit = wegweisendeBeschilderung.getPfostendefizit().getValue();
		this.gemeinde = wegweisendeBeschilderung.getGemeinde().getValue();
		this.kreis = wegweisendeBeschilderung.getKreis().getValue();
		this.land = wegweisendeBeschilderung.getLand().getValue();
	}
}
