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

package de.wps.radvis.backend.fahrradroute.schnittstelle;

import static de.wps.radvis.backend.common.domain.Validators.isValidEmail;
import static de.wps.radvis.backend.common.domain.Validators.isValidURL;

import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@ToString
@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveFahrradrouteCommand {

	private static final int MAX_LENGTH_KURZBESCHREIBUNG = 500;
	public static final int MAX_LENGTH_BESCHREIBUNG = 5000;
	private static final int MAX_LENGTH_TEXT = 255;

	@NotNull
	private Long id;
	@NotNull
	private Long version;

	private String name;
	private String kurzbeschreibung;
	private String beschreibung;
	private Kategorie kategorie;
	private ToubizId toubizId;
	private Tourenkategorie tourenkategorie;
	private Laenge offizielleLaenge;
	private String homepage;
	private Long verantwortlichId;
	private String emailAnsprechpartner;
	private String lizenz;
	private String lizenzNamensnennung;
	private List<LinearReferenzierteProfilEigenschaftenCommand> profilEigenschaften;
	private Long customProfileId;

	private List<SaveFahrradrouteVarianteCommand> varianten;

	private Geometry stuetzpunkte;
	private List<Long> kantenIDs;

	private org.geojson.LineString routenVerlauf;

	@AssertTrue(message = "Die Kurzbeschreibung darf maximal " + MAX_LENGTH_KURZBESCHREIBUNG + " Zeichen lang sein.")
	public boolean isKurzbeschreibungValid() {
		return kurzbeschreibung == null || kurzbeschreibung.length() <= MAX_LENGTH_KURZBESCHREIBUNG;
	}

	@AssertTrue(message = "Die Beschreibung darf maximal " + MAX_LENGTH_BESCHREIBUNG + " Zeichen lang sein.")
	public boolean isBeschreibungValid() {
		return beschreibung == null || beschreibung.length() <= MAX_LENGTH_BESCHREIBUNG;
	}

	@AssertTrue(message = "Die offizielle Länge darf nicht negativ sein.")
	public boolean isOffizielleLaengeValid() {
		return offizielleLaenge == null || offizielleLaenge.getValue() > 0;
	}

	@AssertTrue(message = "Die Homepage muss eine gültige URL mit maximal " + MAX_LENGTH_TEXT + " Zeichen sein.")
	public boolean isHomepageValid() {
		return homepage == null || isValidURL(homepage) && homepage.length() <= MAX_LENGTH_TEXT;
	}

	@AssertTrue(message = "Die E-Mail des Ansprechpartners muss eine gültige E-Mail Adresse mit maximal "
		+ MAX_LENGTH_TEXT + " Zeichen sein.")
	public boolean isEmailValid() {
		return emailAnsprechpartner == null
			|| isValidEmail(emailAnsprechpartner) && emailAnsprechpartner.length() <= MAX_LENGTH_TEXT;
	}

	@AssertTrue(message = "Die Lizenz darf maximal " + MAX_LENGTH_TEXT + " Zeichen lang sein.")
	public boolean isLizenzValid() {
		return lizenz == null || lizenz.length() <= MAX_LENGTH_TEXT;
	}

	@AssertTrue(message = "Die Namensnennung der Lizenz darf maximal " + MAX_LENGTH_TEXT
		+ " Zeichen lang sein.")
	public boolean isLizenzNamensnennungValid() {
		return lizenzNamensnennung == null || lizenzNamensnennung.length() <= MAX_LENGTH_TEXT;
	}

	@AssertTrue(message = "toubizId darf nur für LRFW gesetzt sein.")
	public boolean isToubizIdOnlyForLandesradfernWegeValid() {
		return toubizId == null || kategorie == Kategorie.LANDESRADFERNWEG;
	}
}
