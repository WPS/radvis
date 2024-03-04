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

package de.wps.radvis.backend.netzfehler.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.Optional;

import org.hibernate.envers.NotAudited;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.kommentar.domain.entity.Kommentar;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.netzfehler.domain.valueObject.KonsistenzregelVerletzungReferenz;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Anpassungswunsch extends AbstractEntity {
	private LocalDateTime erstellung;

	private LocalDateTime aenderung;

	@Enumerated(EnumType.STRING)
	private AnpassungswunschStatus status;

	private Point geometrie;

	private String beschreibung;

	@ManyToOne
	private Benutzer benutzerLetzteAenderung;

	@ManyToOne
	private Verwaltungseinheit verantwortlicheOrganisation;

	@Enumerated(EnumType.STRING)
	private AnpassungswunschKategorie kategorie;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@Getter
	@NotAudited
	private KommentarListe kommentarListe;

	@Embedded
	private KonsistenzregelVerletzungReferenz konsistenzregelVerletzungReferenz;

	@Builder
	private Anpassungswunsch(Long id, LocalDateTime erstellung, LocalDateTime aenderung, AnpassungswunschStatus status,
		Point geometrie, String beschreibung, AnpassungswunschKategorie kategorie, Benutzer benutzerLetzteAenderung,
		Verwaltungseinheit verantwortlicheOrganisation, KommentarListe kommentarListe,
		KonsistenzregelVerletzungReferenz konsistenzregelVerletzungReferenz) {
		super(id);
		require(geometrie, notNullValue());
		require(beschreibung != null && !beschreibung.isEmpty(), "Beschreibung muss vorhanden sein");
		require(beschreibung.length() <= 1000, "Beschreibung darf max. 1000 Zeichen haben");
		require(status, notNullValue());
		require(kategorie, notNullValue());
		require(benutzerLetzteAenderung, notNullValue());
		require(kommentarListe, notNullValue());
		this.erstellung = erstellung;
		this.aenderung = aenderung;
		this.status = status;
		this.geometrie = geometrie;
		this.beschreibung = beschreibung;
		this.kategorie = kategorie;
		this.benutzerLetzteAenderung = benutzerLetzteAenderung;
		this.verantwortlicheOrganisation = verantwortlicheOrganisation;
		this.kommentarListe = kommentarListe;
		this.konsistenzregelVerletzungReferenz = konsistenzregelVerletzungReferenz;
	}

	public Anpassungswunsch(Point geometrie, String beschreibung, AnpassungswunschStatus status,
		AnpassungswunschKategorie kategorie, Benutzer benutzerLetzteAenderung,
		Optional<Verwaltungseinheit> verantwortlicheOrganisation,
		Optional<KonsistenzregelVerletzungReferenz> konsistenzregelVerletzungReferenz) {
		this(null, LocalDateTime.now(), LocalDateTime.now(), status, geometrie, beschreibung, kategorie,
			benutzerLetzteAenderung, verantwortlicheOrganisation.orElse(null), new KommentarListe(),
			konsistenzregelVerletzungReferenz.orElse(null));
	}

	public void update(String beschreibung, AnpassungswunschStatus status, AnpassungswunschKategorie kategorie,
		Benutzer benutzerLetzteAenderung, Optional<Verwaltungseinheit> verantwortlicheOrganisation,
		Point geometrie) {
		require(beschreibung != null && !beschreibung.isEmpty(), "Beschreibung muss vorhanden sein");
		require(beschreibung.length() <= 1000, "Beschreibung darf max. 1000 Zeichen haben");
		require(kategorie, notNullValue());
		require(benutzerLetzteAenderung, notNullValue());
		require(status, notNullValue());
		require(geometrie, notNullValue());
		require(verantwortlicheOrganisation, notNullValue());

		this.aenderung = LocalDateTime.now();
		this.beschreibung = beschreibung;
		this.status = status;
		this.kategorie = kategorie;
		this.benutzerLetzteAenderung = benutzerLetzteAenderung;
		this.verantwortlicheOrganisation = verantwortlicheOrganisation.orElse(null);
		this.geometrie = geometrie;
	}

	public Optional<Verwaltungseinheit> getVerantwortlicheOrganisation() {
		return Optional.ofNullable(verantwortlicheOrganisation);
	}

	public Optional<KonsistenzregelVerletzungReferenz> getKonsistenzregelVerletzungReferenz() {
		return Optional.ofNullable(konsistenzregelVerletzungReferenz);
	}

	public void addKommentar(Kommentar kommentar) {
		this.kommentarListe.addKommentar(kommentar);
	}

	public void setzeStatusAufUmgesetzt() {
		require(!this.status.istAbgeschlossen(), "Anpassungswunsch darf noch nicht abgeschlossen sein");
		this.status = AnpassungswunschStatus.UMGESETZT;
	}
}
