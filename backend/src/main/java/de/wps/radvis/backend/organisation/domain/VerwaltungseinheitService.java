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

package de.wps.radvis.backend.organisation.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;

public class VerwaltungseinheitService implements VerwaltungseinheitResolver {

	private final VerwaltungseinheitRepository verwaltungseinheitRepository;
	private final GebietskoerperschaftRepository gebietskoerperschaftRepository;
	private final OrganisationRepository organisationRepository;

	public VerwaltungseinheitService(VerwaltungseinheitRepository verwaltungseinheitRepository,
		GebietskoerperschaftRepository gebietskoerperschaftRepository,
		OrganisationRepository organisationRepository) {
		require(verwaltungseinheitRepository, notNullValue());
		require(gebietskoerperschaftRepository, notNullValue());
		require(organisationRepository, notNullValue());
		this.organisationRepository = organisationRepository;
		this.gebietskoerperschaftRepository = gebietskoerperschaftRepository;
		this.verwaltungseinheitRepository = verwaltungseinheitRepository;
	}

	public List<Verwaltungseinheit> getAll() {
		return verwaltungseinheitRepository.findAll();
	}

	public List<VerwaltungseinheitDbView> getAllAsView() {
		return verwaltungseinheitRepository.findAllAsView();
	}

	public List<VerwaltungseinheitDbView> getAllAktiveAsView() {
		return verwaltungseinheitRepository.findAllAktiveAsView();
	}

	/**
	 * Verwaltungseinheit nicht casten!
	 * Stattdessen dedizierte Repositories fuer Organisation / Gebietskoerperschaft verwenden!
	 */
	public Optional<Verwaltungseinheit> findById(long id) {
		return verwaltungseinheitRepository.findById(id);
	}

	public List<Verwaltungseinheit> getGemeinden() {
		return verwaltungseinheitRepository.findByOrganisationsArt(OrganisationsArt.GEMEINDE);
	}

	public List<Verwaltungseinheit> getKreise() {
		return verwaltungseinheitRepository.findByOrganisationsArt(OrganisationsArt.KREIS);
	}

	public Verwaltungseinheit getBundesland() {
		return verwaltungseinheitRepository.findByOrganisationsArt(OrganisationsArt.BUNDESLAND).get(0);
	}

	public boolean istUebergeordnet(@NonNull Verwaltungseinheit uebergeordnet,
		@NonNull Verwaltungseinheit untergeordnet) {
		if (untergeordnet.equals(uebergeordnet)) {
			return true;
		}

		if (!uebergeordnet.getOrganisationsArt().istGebietskoerperschaft()) {
			Organisation uebergeordneteOrganisation = organisationRepository.findById(uebergeordnet.getId())
				.orElseThrow();
			return uebergeordneteOrganisation.getZustaendigFuerBereichOf().stream().anyMatch(
				gebietskoerperschaft -> istUebergeordnet(gebietskoerperschaft, untergeordnet)
			);
		} else {
			Verwaltungseinheit aktuelleOrganisation = untergeordnet;
			while (aktuelleOrganisation.getUebergeordneteVerwaltungseinheit().isPresent()) {
				Verwaltungseinheit naechsteOrganisation = aktuelleOrganisation.getUebergeordneteVerwaltungseinheit()
					.get();
				if (naechsteOrganisation.equals(uebergeordnet)) {
					return true;
				}
				aktuelleOrganisation = naechsteOrganisation;
			}
			return false;
		}
	}

	/**
	 * Findet zu einer bestimmten Organisation alle uebergeordneten Organisationen. Dies beinhaltet auch die
	 * urspruengliche Ausgangsorganisation.
	 *
	 * @param verwaltungseinheit
	 * 	Die Organisation von der die uebergordneten gefunden werden sollen
	 * @return List<Verwaltungseinheit> ausgangsorganisation + alle uebergeordneten
	 */
	public List<Verwaltungseinheit> findeAlleZustaendigenVerwaltungseinheiten(Verwaltungseinheit verwaltungseinheit) {
		Verwaltungseinheit aktuelleVerwaltungseinheit = verwaltungseinheit;
		List<Verwaltungseinheit> result = new ArrayList<>();
		result.add(verwaltungseinheit);

		while (aktuelleVerwaltungseinheit.getUebergeordneteVerwaltungseinheit().isPresent()) {
			Verwaltungseinheit naechsteVerwaltungseinheit = aktuelleVerwaltungseinheit.getUebergeordneteVerwaltungseinheit()
				.get();
			result.add(naechsteVerwaltungseinheit);
			aktuelleVerwaltungseinheit = naechsteVerwaltungseinheit;
		}
		return result;
	}

	public void markGebietskoerperschaftAsQualitaetsgesichert(Long id) {
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.findById(id)
			.orElseThrow(EntityNotFoundException::new);
		require(OrganisationsArt.KREIS.equals(gebietskoerperschaft.getOrganisationsArt()),
			"OrganisationsArt ist nicht Kreis.");
		gebietskoerperschaft.markOrganisationAsQualitaetsgesichert();
		gebietskoerperschaftRepository.save(gebietskoerperschaft);
	}

	public List<Verwaltungseinheit> getOrganisationenByOrganisationsArtFuerGeometrie(
		OrganisationsArt organisationsArt,
		Geometry geometry) {
		return verwaltungseinheitRepository.findAllByOrganisationsArtContainingGeometry(organisationsArt, geometry);
	}

	public Verwaltungseinheit getUnbekannteOrganisation() {
		return verwaltungseinheitRepository.findByName("Unbekannt");
	}

	public Verwaltungseinheit getBundesrepublikDeutschland() {
		return verwaltungseinheitRepository.findByName("Bundesrepublik Deutschland");
	}

	public Verwaltungseinheit getDritter() {
		return verwaltungseinheitRepository.findByName("Bundesrepublik Deutschland");
	}

	public Verwaltungseinheit getToubiz() {
		return verwaltungseinheitRepository.findByName("Toubiz");
	}

	@Override
	public Verwaltungseinheit resolve(long id) {
		return verwaltungseinheitRepository.findById(id).orElseThrow();
	}

	/***
	 * Findet anhand von Names und Organisationsart einer Verwaltugnseinheit die dazugehörige Entity
	 * @param name inklusive Organisationsart als String, organisationsArt als OrganisationsArt
	 * @return Verwaltungseinheit
	 */
	public Optional<Verwaltungseinheit> getVerwaltungseinheitnachNameUndArt(String name,
		OrganisationsArt organisationsArt) {
		return verwaltungseinheitRepository.findByNameAndOrganisationsArt(name, organisationsArt);
	}

	public PreparedGeometry getBundeslandBereichPrepared() {
		Verwaltungseinheit bundesland = getBundesland();
		require(bundesland.getBereich().isPresent());
		return PreparedGeometryFactory.prepare(bundesland.getBereich().get());
	}

	public List<Long> findAllUntergeordnetIds(Long verwaltungseinheitId) {
		return verwaltungseinheitRepository.findAllUntergeordnetIds(verwaltungseinheitId);
	}
}
