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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.HashSet;
import java.util.Set;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionSchritt;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

public class NetzklasseImportSession extends AbstractImportSession {

	public static ImportSessionSchritt AUTOMATISCHE_ABBILDUNG = ImportSessionSchritt.of(3);
	public static ImportSessionSchritt ABBILDUNG_BEARBEITEN = ImportSessionSchritt.of(4);
	public static ImportSessionSchritt IMPORT_ABSCHLIESSEN = ImportSessionSchritt.of(5);

	@Getter
	private final Netzklasse netzklasse;

	@Getter
	private final Set<Long> kanteIds;

	@Getter
	private Set<LineString> nichtGematchteFeatureLineStrings;

	@Getter
	private final Verwaltungseinheit organisation;

	@Builder
	public NetzklasseImportSession(@NonNull Benutzer benutzer, @NonNull Verwaltungseinheit organisation,
		@NonNull Netzklasse netzklasse) {
		super(benutzer);
		require(!Netzklasse.RADNETZ_NETZKLASSEN.contains(netzklasse),
			"Eine RadNETZ Netzklasse darf nicht von Nutzern durch den manuellen Import gesetzt werden");
		this.kanteIds = new HashSet<>();
		this.schritt = AUTOMATISCHE_ABBILDUNG;
		this.nichtGematchteFeatureLineStrings = new HashSet<>();
		this.netzklasse = netzklasse;
		this.organisation = organisation;
	}

	public void toggleNetzklassenzugehoerigkeit(Long kanteId) {
		require(kanteId, notNullValue());
		if (this.kanteIds.contains(kanteId)) {
			this.kanteIds.remove(kanteId);
		} else {
			// Kein expliziter Check, ob Kante in Organisationsbereich existiert, weil später bei der Ausführung
			// der Abbildung sowieso nur Kanten im Organisationsbereich berücksichtigt werden
			this.kanteIds.add(kanteId);
		}
	}

	public void addNichtGematchteFeatureLineStrings(Set<LineString> nichtGematchteLineStrings) {
		this.nichtGematchteFeatureLineStrings = nichtGematchteLineStrings;
	}

	public long getAnzahlFeaturesOhneMatch() {
		return this.nichtGematchteFeatureLineStrings.size();
	}

	@Override
	public MultiPolygon getBereich() {
		return organisation.getBereich()
			.orElse(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createMultiPolygon());
	}

	@Override
	public String getBereichName() {
		return organisation.getName();
	}
}
