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

package de.wps.radvis.backend.furtKreuzung.schnittstelle;

import java.util.Set;

import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungNetzBezug;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.schnittstelle.NetzbezugCommandConverter;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;

public class SaveFurtKreuzungCommandConverter extends NetzbezugCommandConverter<FurtKreuzungNetzBezug> {

	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	public SaveFurtKreuzungCommandConverter(KanteResolver kantenResolver, KnotenResolver knotenResolver,
		VerwaltungseinheitResolver verwaltungseinheitResolver) {
		super(kantenResolver, knotenResolver);
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	public FurtKreuzung convert(SaveFurtKreuzungCommand command) {
		return new FurtKreuzung(createNetzbezug(command.getNetzbezug()),
			verwaltungseinheitResolver.resolve(command.getVerantwortlicheOrganisation()), command.getTyp(),
			command.isRadnetzKonform(), command.getKommentar(), command.getKnotenForm(),
			command.getFurtKreuzungMusterloesung(), command.getLichtsignalAnlageEigenschaften(),
			command.getQuerungshilfeDetails(), command.getBauwerksmangel(), command.getBauwerksmangelArt());
	}

	public void apply(FurtKreuzung furtKreuzung, SaveFurtKreuzungCommand command) {
		furtKreuzung.update(createNetzbezug(command.getNetzbezug()),
			verwaltungseinheitResolver.resolve(command.getVerantwortlicheOrganisation()), command.getTyp(),
			command.isRadnetzKonform(), command.getKommentar(), command.getKnotenForm(),
			command.getFurtKreuzungMusterloesung(), command.getLichtsignalAnlageEigenschaften(),
			command.getQuerungshilfeDetails(), command.getBauwerksmangel(), command.getBauwerksmangelArt());
	}

	@Override
	protected FurtKreuzungNetzBezug buildNetzbezug(Set<AbschnittsweiserKantenSeitenBezug> abschnitte,
		Set<PunktuellerKantenSeitenBezug> punkte, Set<Knoten> knoten) {
		return new FurtKreuzungNetzBezug(abschnitte, punkte, knoten);
	}
}
