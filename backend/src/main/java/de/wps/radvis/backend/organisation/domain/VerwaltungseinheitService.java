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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.Mailadresse;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerwaltungseinheitService implements VerwaltungseinheitResolver {

	private final VerwaltungseinheitRepository verwaltungseinheitRepository;
	private final GebietskoerperschaftRepository gebietskoerperschaftRepository;
	private final OrganisationRepository organisationRepository;
	private final OrganisationsArt obersteGebietskoerperschaftOrganisationsArt;
	private final String obersteGebietskoerperschaftName;
	private final Map<Integer, Mailadresse> funktionspostfaecher;

	public VerwaltungseinheitService(VerwaltungseinheitRepository verwaltungseinheitRepository,
		GebietskoerperschaftRepository gebietskoerperschaftRepository,
		OrganisationRepository organisationRepository,
		OrganisationsArt obersteGebietskoerperschaftOrganisationsArt,
		String obersteGebietskoerperschaftName, Map<Integer, Mailadresse> funktionspostfaecher) {
		require(verwaltungseinheitRepository, notNullValue());
		require(gebietskoerperschaftRepository, notNullValue());
		require(organisationRepository, notNullValue());
		require(funktionspostfaecher, notNullValue());
		this.funktionspostfaecher = funktionspostfaecher;
		this.organisationRepository = organisationRepository;
		this.gebietskoerperschaftRepository = gebietskoerperschaftRepository;
		this.verwaltungseinheitRepository = verwaltungseinheitRepository;
		this.obersteGebietskoerperschaftOrganisationsArt = obersteGebietskoerperschaftOrganisationsArt;
		this.obersteGebietskoerperschaftName = obersteGebietskoerperschaftName;
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

	public String getAllNames(List<Long> gebietskoerperschaftIds) {
		return verwaltungseinheitRepository.findAllDbViewsById(gebietskoerperschaftIds)
			.stream()
			.map(gK -> String.format("%s (%s)", gK.getName(), gK.getOrganisationsArt()))
			.collect(Collectors.joining(", "));
	}

	public MultiPolygon getVereintenBereich(List<Long> gebietskoerperschaftIds) {
		return verwaltungseinheitRepository.getVereintenBereich(gebietskoerperschaftIds);
	}

	/**
	 * Verwaltungseinheit nicht casten! Stattdessen dedizierte Repositories fuer Organisation / Gebietskoerperschaft
	 * verwenden!
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

	public Optional<Verwaltungseinheit> getObersteGebietskoerperschaft() {
		List<Verwaltungseinheit> list = verwaltungseinheitRepository.findByNameAndOrganisationsArt(
			obersteGebietskoerperschaftName,
			obersteGebietskoerperschaftOrganisationsArt);
		if (list.isEmpty()) {
			return Optional.empty();
		}

		if (list.size() > 1) {
			log.warn("Oberste Gebietskörperschaft anhand {} ({}) nicht eindeutig", obersteGebietskoerperschaftName,
				obersteGebietskoerperschaftOrganisationsArt);
		}
		return Optional.of(list.get(0));
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
				gebietskoerperschaft -> istUebergeordnet(gebietskoerperschaft, untergeordnet));
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
	 *     Die Organisation von der die uebergordneten gefunden werden sollen
	 * @return List<Verwaltungseinheit> ausgangsorganisation + alle uebergeordneten
	 */
	public List<Verwaltungseinheit> findeAlleZustaendigenVerwaltungseinheiten(Verwaltungseinheit verwaltungseinheit) {
		Verwaltungseinheit aktuelleVerwaltungseinheit = verwaltungseinheit;
		List<Verwaltungseinheit> result = new ArrayList<>();
		result.add(verwaltungseinheit);

		while (aktuelleVerwaltungseinheit.getUebergeordneteVerwaltungseinheit().isPresent()) {
			Verwaltungseinheit naechsteVerwaltungseinheit = aktuelleVerwaltungseinheit
				.getUebergeordneteVerwaltungseinheit()
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
	 * Findet anhand von Names und Organisationsart einer Verwaltugnseinheit die dazugehörige Entity.
	 *
	 * @param name
	 *     inklusive Organisationsart als String, organisationsArt als OrganisationsArt
	 * @return Verwaltungseinheit
	 * @throws OrganisationsartUndNameNichtEindeutigException
	 */
	public Optional<Verwaltungseinheit> getVerwaltungseinheitNachNameUndArt(String name,
		OrganisationsArt organisationsArt) throws OrganisationsartUndNameNichtEindeutigException {
		List<Verwaltungseinheit> findByNameAndOrganisationsArt = verwaltungseinheitRepository
			.findByNameAndOrganisationsArt(name, organisationsArt);
		if (findByNameAndOrganisationsArt.size() > 1) {
			throw new OrganisationsartUndNameNichtEindeutigException(name, organisationsArt);
		}
		return findByNameAndOrganisationsArt.stream().findAny();
	}

	public boolean hasVerwaltungseinheitNachNameUndArt(String name, OrganisationsArt organisationsArt) {
		return !verwaltungseinheitRepository.findByNameAndOrganisationsArt(name, organisationsArt).isEmpty();
	}

	public PreparedGeometry getBundeslandBereichPrepared() {
		Verwaltungseinheit bundesland = getBundesland();
		require(bundesland.getBereich().isPresent());
		return PreparedGeometryFactory.prepare(bundesland.getBereich().get());
	}

	public List<Long> findAllUntergeordnetIds(Long verwaltungseinheitId) {
		return verwaltungseinheitRepository.findAllUntergeordnetIds(verwaltungseinheitId);
	}

	public long getAnzahlVerwaltungseinheitOfOrganisationsArt(OrganisationsArt organisationsArt) {
		return verwaltungseinheitRepository.countByOrganisationsArt(organisationsArt);
	}

	public long getAnzahlVerwaltungseinheitOfOrganisationsArtMitAktivenBenutzern(OrganisationsArt organisationsArt) {
		return verwaltungseinheitRepository.countVerwaltungseinheitenWithAtLeastOneActiveBenutzer(organisationsArt);
	}

	public List<Verwaltungseinheit> getAllKreiseWithKreisnetzGreaterOrEqual(Integer laengeInMetern) {
		return verwaltungseinheitRepository.findAllKreiseWithKreisnetzGreaterOrEqual(laengeInMetern);
	}

	public List<Verwaltungseinheit> getAllKommunenWIthKommunalnetzGreaterOrEqual(Integer laengeInMetern) {
		return verwaltungseinheitRepository.findAllKommunenWithKommunalnetzGreaterOrEqual(laengeInMetern);
	}

	public Optional<Mailadresse> findFunktionspostfach(Verwaltungseinheit verwaltungseinheit) {
		if (verwaltungseinheit instanceof Gebietskoerperschaft) {
			Integer fachId = ((Gebietskoerperschaft) verwaltungseinheit).getFachId();
			if (funktionspostfaecher.containsKey(fachId)) {
				return Optional.of(funktionspostfaecher.get(fachId));
			}
		}
		return Optional.empty();
	}
}
