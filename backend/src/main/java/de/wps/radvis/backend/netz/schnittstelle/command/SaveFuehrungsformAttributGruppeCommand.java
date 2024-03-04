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

package de.wps.radvis.backend.netz.schnittstelle.command;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaveFuehrungsformAttributGruppeCommand {
	private static final String SEGMENTS_MUST_COVER_FULL_LINE_MSG = "Die Segmente einer linearreferenzierten AttributGruppe müssen die gesamte Kante abdecken.";
	@NotNull
	private Long gruppenID;
	@NotNull
	private Long gruppenVersion;
	@NotNull
	private Long kanteId;

	@Valid
	private List<SaveFuehrungsformAttributeCommand> fuehrungsformAttributeLinks;

	@Valid
	private List<SaveFuehrungsformAttributeCommand> fuehrungsformAttributeRechts;

	@AssertTrue(message = "Fuehrungsfrom fehlerhaft: " + SEGMENTS_MUST_COVER_FULL_LINE_MSG)
	public boolean isSegementsCoverFullLineTrue() {
		return LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			fuehrungsformAttributeLinks.stream()
				.map(SaveFuehrungsformAttributeCommand::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList())) && LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			fuehrungsformAttributeRechts.stream()
				.map(SaveFuehrungsformAttributeCommand::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));
	}
}
