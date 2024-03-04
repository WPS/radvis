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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.geotools.api.feature.simple.SimpleFeature;

import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehler;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import lombok.Getter;

public class MassnahmenImportZuordnung {

	@Getter
	private final SimpleFeature feature;

	private final Massnahme massnahme;

	private final MassnahmeKonzeptID id;

	@Getter
	private final MassnahmenImportZuordnungStatus status;

	@Getter
	private final List<MappingFehler> mappingFehler;

	@Getter
	private final List<NetzbezugHinweis> netzbezugHinweise;

	public MassnahmenImportZuordnung(MassnahmeKonzeptID id, SimpleFeature feature, Massnahme massnahme,
		MassnahmenImportZuordnungStatus status) {
		require(Objects.isNull(massnahme) || MassnahmenImportZuordnungStatus.statusMitZugeordneterMassnahme()
				.contains(status),
			"Nur bei Status GELOESCHT oder GEMAPPT darf eine Massnahme zugeordnet sein");

		this.id = id;
		this.feature = feature;
		this.massnahme = massnahme;
		this.status = status;
		this.mappingFehler = new ArrayList<>();
		this.netzbezugHinweise = new ArrayList<>();
	}

	public void addMappingFehler(MappingFehler mappingFehler) {
		require(mappingFehler, notNullValue());
		this.mappingFehler.add(mappingFehler);
	}

	public void addNetzbezugHinweis(NetzbezugHinweis netzbezugHinweis) {
		require(netzbezugHinweis, notNullValue());
		this.netzbezugHinweise.add(netzbezugHinweis);
	}

	public Optional<Massnahme> getMassnahme() {
		return Optional.ofNullable(massnahme);
	}

	public Optional<MassnahmeKonzeptID> getId() {
		return Optional.ofNullable(id);
	}
}