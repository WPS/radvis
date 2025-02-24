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

package de.wps.radvis.backend.netz.schnittstelle;

import java.util.List;
import java.util.stream.Collectors;

import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute.KantenAttributeBuilder;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveGeschwindigkeitAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveZustaendigkeitAttributeCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.validation.Valid;

public class SaveKanteCommandConverter {
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	public SaveKanteCommandConverter(VerwaltungseinheitResolver verwaltungseinheitResolver) {
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	public List<FuehrungsformAttribute> convertFuehrungsformAtttributeCommands(
		List<SaveFuehrungsformAttributeCommand> commands) {
		return commands.stream().map(this::convertFuehrungsformAttributeCommand).collect(Collectors.toList());
	}

	public GeschwindigkeitAttribute convertGeschwindigkeitsAttributeCommand(
		@Valid SaveGeschwindigkeitAttributeCommand command) {
		return new GeschwindigkeitAttribute(
			command.getLinearReferenzierterAbschnitt(),
			command.getOrtslage(),
			command.getHoechstgeschwindigkeit(),
			command.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung());
	}

	public ZustaendigkeitAttribute convertZustaendigkeitsAttributeCommand(
		@Valid SaveZustaendigkeitAttributeCommand command) {

		Verwaltungseinheit baulastTraegerLoaded = null;
		if (command.getBaulastTraeger() != null) {
			baulastTraegerLoaded = verwaltungseinheitResolver.resolve(command.getBaulastTraeger());
		}
		Verwaltungseinheit unterhaltsZustaendigerLoaded = null;
		if (command.getUnterhaltsZustaendiger() != null) {
			unterhaltsZustaendigerLoaded = verwaltungseinheitResolver.resolve(command.getUnterhaltsZustaendiger());
		}

		Verwaltungseinheit erhaltsZustaendigerLoaded = null;
		if (command.getErhaltsZustaendiger() != null) {
			erhaltsZustaendigerLoaded = verwaltungseinheitResolver.resolve(command.getErhaltsZustaendiger());
		}

		return new ZustaendigkeitAttribute(command.getLinearReferenzierterAbschnitt(), baulastTraegerLoaded,
			unterhaltsZustaendigerLoaded, erhaltsZustaendigerLoaded, command.getVereinbarungsKennung());
	}

	private FuehrungsformAttribute convertFuehrungsformAttributeCommand(
		SaveFuehrungsformAttributeCommand command) {
		return new FuehrungsformAttribute(
			command.getLinearReferenzierterAbschnitt(),
			command.getBelagArt(),
			command.getOberflaechenbeschaffenheit(),
			command.getBordstein(),
			command.getRadverkehrsfuehrung(),
			command.getParkenTyp(),
			command.getParkenForm(),
			command.getBreite(),
			command.getBenutzungspflicht(),
			command.getBeschilderung(),
			command.getSchaeden(),
			command.getAbsenkung(),
			command.getTrennstreifenBreiteRechts(),
			command.getTrennstreifenBreiteLinks(),
			command.getTrennstreifenTrennungZuRechts(),
			command.getTrennstreifenTrennungZuLinks(),
			command.getTrennstreifenFormRechts(),
			command.getTrennstreifenFormLinks());
	}

	public KantenAttributGruppe apply(KantenAttributGruppe kantenAttributGruppe, SaveKanteAttributeCommand command) {
		KantenAttributeBuilder kantenAttributeBuilder = kantenAttributGruppe.getKantenAttribute().toBuilder()
			.beleuchtung(command.getBeleuchtung())
			.dtvFussverkehr(command.getDtvFussverkehr())
			.dtvPkw(command.getDtvPkw())
			.dtvRadverkehr(command.getDtvRadverkehr())
			.kommentar(command.getKommentar())
			.laengeManuellErfasst(command.getLaengeManuellErfasst())
			.strassenquerschnittRASt06(command.getStrassenquerschnittRASt06())
			.sv(command.getSv())
			.umfeld(command.getUmfeld())
			.strassenkategorieRIN(command.getStrassenkategorieRIN())
			.wegeNiveau(command.getWegeNiveau())
			.status(command.getStatus());
		Verwaltungseinheit gemeindeLoaded = null;
		if (command.getGemeinde() != null) {
			gemeindeLoaded = verwaltungseinheitResolver.resolve(command.getGemeinde());
			kantenAttributeBuilder.gemeinde(gemeindeLoaded);
		}

		return KantenAttributGruppe.builder()
			.id(command.getGruppenId())
			.version(command.getGruppenVersion())
			.netzklassen(command.getNetzklassen())
			.istStandards(command.getIstStandards())
			.kantenAttribute(kantenAttributeBuilder.build())
			.build();
	}
}
