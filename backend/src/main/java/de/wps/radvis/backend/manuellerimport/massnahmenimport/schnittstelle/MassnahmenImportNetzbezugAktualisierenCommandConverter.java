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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle;

import java.util.Optional;
import java.util.Set;

import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.MassnahmenImportNetzbezugAktualisierenCommand;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.schnittstelle.NetzbezugCommandConverter;

public class MassnahmenImportNetzbezugAktualisierenCommandConverter
	extends NetzbezugCommandConverter<MassnahmeNetzBezug> {

	public MassnahmenImportNetzbezugAktualisierenCommandConverter(
		KanteResolver kantenResolver, KnotenResolver knotenResolver) {
		super(kantenResolver, knotenResolver);
	}

	public Optional<MassnahmeNetzBezug> convert(MassnahmenImportNetzbezugAktualisierenCommand command) {
		if (command.getNetzbezug().isPresent()) {
			return Optional.ofNullable(createNetzbezug(command.getNetzbezug().get()));
		} else {
			return Optional.empty();
		}
	}

	@Override
	protected MassnahmeNetzBezug buildNetzbezug(Set<AbschnittsweiserKantenSeitenBezug> abschnitte,
		Set<PunktuellerKantenSeitenBezug> punkte, Set<Knoten> knoten) {
		if (MassnahmeNetzBezug.mindestensEinenBezug(abschnitte, punkte, knoten)) {
			return new MassnahmeNetzBezug(abschnitte, punkte, knoten);
		}
		return null;
	}
}
