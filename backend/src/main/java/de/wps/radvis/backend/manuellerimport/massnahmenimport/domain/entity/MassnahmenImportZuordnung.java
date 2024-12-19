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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Severity;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MappingFehler;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportZuordnungStatus;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweisText;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import lombok.Getter;

public class MassnahmenImportZuordnung {

	private static int nextId = 1;

	@Getter
	private final int id;

	private final MassnahmeKonzeptID massnahmeKonzeptId;

	@Getter
	private final SimpleFeature feature;

	private final Massnahme massnahme;

	@Getter
	private final MassnahmenImportZuordnungStatus zuordnungStatus;

	private MassnahmeNetzBezug netzbezug;

	@Getter
	private final Set<MappingFehler> mappingFehler;

	@Getter
	private final List<NetzbezugHinweis> netzbezugHinweise;

	private boolean selected;

	public MassnahmenImportZuordnung(MassnahmeKonzeptID massnahmeKonzeptId, SimpleFeature feature, Massnahme massnahme,
		MassnahmenImportZuordnungStatus zuordnungStatus) {
		require(feature, notNullValue());
		require(zuordnungStatus, notNullValue());
		require(
			Objects.isNull(massnahme) || MassnahmenImportZuordnungStatus.statusMitZugeordneterMassnahme()
				.contains(zuordnungStatus),
			String.format("Nur bei Status %s oder %s darf eine Massnahme zugeordnet sein",
				MassnahmenImportZuordnungStatus.GELOESCHT.name(),
				MassnahmenImportZuordnungStatus.ZUGEORDNET.name()));

		this.id = nextId++;
		this.massnahmeKonzeptId = massnahmeKonzeptId;
		this.feature = feature;
		this.massnahme = massnahme;
		this.zuordnungStatus = zuordnungStatus;
		this.mappingFehler = new HashSet<>();
		this.netzbezugHinweise = new ArrayList<>();
		this.selected = false;
	}

	public Optional<MassnahmeKonzeptID> getMassnahmeKonzeptId() {
		return Optional.ofNullable(massnahmeKonzeptId);
	}

	public Optional<Massnahme> getMassnahme() {
		return Optional.ofNullable(massnahme);
	}

	public Optional<MassnahmeNetzBezug> getNetzbezug() {
		return Optional.ofNullable(netzbezug);
	}

	public void addMappingFehler(MappingFehler mappingFehler) {
		require(mappingFehler, notNullValue());
		this.mappingFehler.add(mappingFehler);
		deselect();
	}

	public void addNetzbezugHinweis(NetzbezugHinweis netzbezugHinweis) {
		require(netzbezugHinweis, notNullValue());
		this.netzbezugHinweise.add(netzbezugHinweis);
		if (hasNetzbezugHinweisFehler()) {
			deselect();
		}
	}

	public boolean hasNetzbezugHinweisFehler() {
		return getNetzbezugHinweise()
			.stream()
			.anyMatch(hinweis -> hinweis.getSeverity() == Severity.ERROR);
	}

	public boolean hasMappingFehler() {
		return !getMappingFehler().isEmpty();
	}

	public boolean canBeSaved() {
		return zuordnungStatus != MassnahmenImportZuordnungStatus.FEHLERHAFT && !hasNetzbezugHinweisFehler()
			&& !hasMappingFehler();
	}

	public void select() {
		require(canBeSaved(), "Zuordnung " + id
			+ " darf nicht selektiert werden, da sie fehlerhaft ist oder Fehler-Einträge enthält.");

		this.selected = true;
	}

	public void deselect() {
		this.selected = false;
	}

	public boolean isSelected() {
		return selected;
	}

	public void aktualisiereNetzbezug(MassnahmeNetzBezug netzbezug, boolean clearNetzbezugHinweise) {
		// Hier ist es auch erlaubt, den Netzbezug auf null zu setzen!
		this.netzbezug = netzbezug;

		if (clearNetzbezugHinweise) {
			getNetzbezugHinweise().clear();
		}

		if (netzbezug == null) {
			addNetzbezugHinweis(NetzbezugHinweis.ofError(NetzbezugHinweisText.NETZBEZUG_NICHT_GEFUNDEN));
		}
	}

	public Optional<Geometry> getNetzbezugGeometrie() {
		if (zuordnungStatus == MassnahmenImportZuordnungStatus.GELOESCHT) {
			return getMassnahme()
				.map(Massnahme::getNetzbezug)
				.map(MassnahmeNetzBezug::getGeometrie);
		} else {
			return getNetzbezug()
				.map(MassnahmeNetzBezug::getGeometrie);
		}
	}
}