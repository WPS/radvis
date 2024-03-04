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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractAssert;

import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehler;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import lombok.NonNull;

public class MassnahmenImportZuordnungAssert
	extends AbstractAssert<MassnahmenImportZuordnungAssert, MassnahmenImportZuordnung> {

	public MassnahmenImportZuordnungAssert(MassnahmenImportZuordnung actual) {
		super(actual, MassnahmenImportZuordnungAssert.class);
	}

	public MassnahmenImportZuordnungAssert hasExactlyMappingFehler(@NonNull MappingFehler fehler) {
		isNotNull();
		verifyMappingFehler(List.of(fehler));
		return this;
	}

	public MassnahmenImportZuordnungAssert hasExactlyInAnyOrderMappingFehler(@NonNull List<MappingFehler> fehler) {
		isNotNull();
		verifyMappingFehler(fehler);
		return this;
	}

	public MassnahmenImportZuordnungAssert hasExactlyNetzbezugsHinweise(
		@NonNull NetzbezugHinweis... netzbezugsHinweise) {
		isNotNull();
		List<NetzbezugHinweis> expected = Arrays.asList(netzbezugsHinweise);
		List<NetzbezugHinweis> notFound = expected.stream()
			.filter(h -> !actual.getNetzbezugHinweise().contains(h))
			.toList();
		List<NetzbezugHinweis> notExpected = actual.getNetzbezugHinweise().stream()
			.filter(h -> !expected.contains(h))
			.toList();

		if (!notFound.isEmpty() || !notExpected.isEmpty()) {
			failWithCollectionContainingWrongElements(NetzbezugHinweis.class, notFound, notExpected, expected);
		}
		return this;
	}

	private <T> void failWithCollectionContainingWrongElements(Class<T> elementClass, List<T> notFound,
		List<T> notExpected,
		List<T> expected) {
		failWithMessage(
			"Expected MassnahmenImportZuordnung to have %s\n %s \nbut found\n %s.\n\n%s%s",
			elementClass.getSimpleName(),
			expected,
			actual.getMappingFehler().isEmpty() ? "none" : actual.getMappingFehler(),
			notFound.isEmpty() ? "" : "\nNot found:\n " + notFound,
			notExpected.isEmpty() ? "" : "\nNot expected:\n " + notExpected
		);
	}

	private void verifyMappingFehler(List<MappingFehler> mappingFehler) {
		List<MappingFehler> notFound = mappingFehler.stream()
			.filter(h -> !actual.getMappingFehler().contains(h))
			.toList();
		List<MappingFehler> notExpected = actual.getMappingFehler().stream()
			.filter(h -> !mappingFehler.contains(h))
			.toList();

		if (!notFound.isEmpty() || !notExpected.isEmpty()) {
			failWithCollectionContainingWrongElements(MappingFehler.class, notFound, notExpected,
				mappingFehler);
		}
	}

	public MassnahmenImportZuordnungAssert hasFehlerSize(int amount) {
		isNotNull();
		if (actual.getMappingFehler().size() != amount) {
			failWithMessage("Expected MassnahmenImportZuordnung to have %s Fehler but only found %s: %s",
				amount, actual.getMappingFehler().size(), actual.getMappingFehler());
		}
		return this;
	}

	public MassnahmenImportZuordnungAssert doesNotHaveAnyFehler() {
		isNotNull();
		if (!actual.getMappingFehler().isEmpty()) {
			failWithMessage("Expected MassnahmenImportZuordnung to have not have any Fehler but found %s",
				actual.getMappingFehler());
		}
		return this;
	}

	public MassnahmenImportZuordnungAssert hasStatus(@NonNull MassnahmenImportZuordnungStatus status) {
		isNotNull();
		if (!actual.getStatus().equals(status)) {
			failWithMessage("Expected MassnahmenImportZuordnung to have Status %s but was %s",
				status, actual.getStatus());
		}
		return this;
	}

	public MassnahmenImportZuordnungAssert hasMassnahme(@NonNull Massnahme massnahme) {
		isNotNull();
		if (actual.getMassnahme().isEmpty()) {
			failWithMessage(
				"Expected MassnahmenImportZuordnung to have Massnahme %s but no Massnahme was present",
				massnahme);
		} else if (!actual.getMassnahme().get().equals(massnahme)) {
			failWithMessage("Expected MassnahmenImportZuordnung to have Massnahme %s but was %s",
				massnahme, actual.getMassnahme().get());
		}
		return this;
	}

	public MassnahmenImportZuordnungAssert doesNotHaveAnyMassnahme() {
		isNotNull();
		if (actual.getMassnahme().isPresent()) {
			failWithMessage("Expected MassnahmenImportZuordnung to not have any Massnahme but was %s",
				actual.getMassnahme().get());
		}
		return this;
	}

	public MassnahmenImportZuordnungAssert hasMassnahmeId(MassnahmeKonzeptID massnahmeID) {
		isNotNull();
		if (actual.getId().isEmpty()) {
			failWithMessage("Expected MassnahmenImportZuordnung to have MassnahmeID %s but was empty",
				actual.getId());
		} else if (!actual.getId().get().equals(massnahmeID)) {
			failWithMessage("Expected MassnahmenImportZuordnung to have MassnahmeID %s but was %s",
				massnahmeID, actual.getId().get());
		}
		return this;
	}

	public MassnahmenImportZuordnungAssert doesNotHaveAnyMassnahmeID() {
		isNotNull();
		if (actual.getId().isPresent()) {
			failWithMessage("Expected MassnahmenImportZuordnung to not have any MassnahmeID but was %s",
				actual.getId().get());
		}
		return this;
	}

	public static MassnahmenImportZuordnungAssert assertThatMassnahmenImportZuordnung(
		MassnahmenImportZuordnung actual) {
		return new MassnahmenImportZuordnungAssert(actual);
	}

}
