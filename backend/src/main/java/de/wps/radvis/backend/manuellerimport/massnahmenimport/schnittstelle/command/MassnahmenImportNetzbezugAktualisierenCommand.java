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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command;

import java.util.Optional;

import de.wps.radvis.backend.netz.schnittstelle.command.NetzbezugCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder(access = AccessLevel.PUBLIC)
public class MassnahmenImportNetzbezugAktualisierenCommand {
	@Getter
	@NotNull
	private int massnahmenImportZuordnungId;
	@Valid
	private NetzbezugCommand netzbezug;

	public Optional<NetzbezugCommand> getNetzbezug() {
		return Optional.ofNullable(netzbezug);
	}
}
