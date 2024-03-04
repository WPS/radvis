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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionSchritt;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportAttribute;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class MassnahmenImportSession extends AbstractImportSession {

	public final static ImportSessionSchritt DATEI_HOCHLADEN = ImportSessionSchritt.of(1);
	public final static ImportSessionSchritt ATTRIBUTE_AUSWAEHLEN = ImportSessionSchritt.of(2);
	public final static ImportSessionSchritt ATTRIBUTFEHLER_UEBERPRUEFEN = ImportSessionSchritt.of(3);

	private final MultiPolygon bereich;

	private final String bereichName;

	@Getter
	private final List<Long> gebietskoerperschaftenIds;

	private final SollStandard sollStandard;

	@Getter
	private final Konzeptionsquelle konzeptionsquelle;

	@Getter
	@Setter
	private List<MassnahmenImportZuordnung> zuordnungen;

	@Getter
	@Setter
	private List<MassnahmenImportAttribute> attribute;

	@Builder
	public MassnahmenImportSession(@NonNull Benutzer benutzer, MultiPolygon bereich, String bereichName,
		List<Long> gebietskoerperschaftenIds, Konzeptionsquelle konzeptionsquelle, SollStandard sollStandard) {
		super(benutzer);
		require(konzeptionsquelle, notNullValue());
		require(bereich, notNullValue());
		require(bereichName, notNullValue());
		require(gebietskoerperschaftenIds, notNullValue());
		require(gebietskoerperschaftenIds, not(empty()));

		this.konzeptionsquelle = konzeptionsquelle;
		zuordnungen = new ArrayList<>();
		this.bereich = bereich;
		this.bereichName = bereichName;
		this.gebietskoerperschaftenIds = gebietskoerperschaftenIds;
		this.schritt = DATEI_HOCHLADEN;
		this.sollStandard = sollStandard;
	}

	@Override
	public long getAnzahlFeaturesOhneMatch() {
		// Implement the method here
		return 0; // replace with actual implementation
	}

	@Override
	public MultiPolygon getBereich() {
		return bereich;
	}

	@Override
	public String getBereichName() {
		return bereichName;
	}

	public Optional<SollStandard> getSollStandard() {
		return Optional.ofNullable(sollStandard);
	}
}