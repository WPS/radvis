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

package de.wps.radvis.backend.netz.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.hibernate.envers.Audited;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Audited
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
public class FuehrungsformAttributGruppe extends VersionierteEntity {

	@ElementCollection
	@CollectionTable(name = "fuehrungsform_attribut_gruppe_attribute_links")
	private Set<FuehrungsformAttribute> fuehrungsformAttributeLinks;

	@ElementCollection
	@CollectionTable(name = "fuehrungsform_attribut_gruppe_attribute_rechts")
	private Set<FuehrungsformAttribute> fuehrungsformAttributeRechts;

	@Getter
	private boolean isZweiseitig;

	public FuehrungsformAttributGruppe(List<FuehrungsformAttribute> fuehrungsformAttributeBeideSeiten,
		boolean isZweiseitig) {
		require(fuehrungsformAttributeBeideSeiten, notNullValue());
		require(fuehrungsformAttributeBeideSeiten, Matchers.not(Matchers.empty()));
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(fuehrungsformAttributeBeideSeiten.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.collect(Collectors.toList())));

		this.fuehrungsformAttributeLinks = new HashSet<>(fuehrungsformAttributeBeideSeiten);
		this.fuehrungsformAttributeRechts = new HashSet<>(fuehrungsformAttributeBeideSeiten);
		this.isZweiseitig = isZweiseitig;
	}

	@Builder(builderMethodName = "privateBuilder")
	private FuehrungsformAttributGruppe(Long id, List<FuehrungsformAttribute> fuehrungsformAttributeLinks,
		List<FuehrungsformAttribute> fuehrungsformAttributeRechts, boolean isZweiseitig, Long version) {
		super(id, version);
		require(fuehrungsformAttributeLinks, notNullValue());
		require(fuehrungsformAttributeLinks, Matchers.not(Matchers.empty()));
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(fuehrungsformAttributeLinks.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.collect(Collectors.toList())));
		require(fuehrungsformAttributeRechts, notNullValue());
		require(fuehrungsformAttributeRechts, Matchers.not(Matchers.empty()));
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(fuehrungsformAttributeRechts.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.collect(Collectors.toList())));
		require(isSeitenBezugValid(fuehrungsformAttributeLinks, fuehrungsformAttributeRechts, isZweiseitig));

		this.fuehrungsformAttributeLinks = new HashSet<>(fuehrungsformAttributeLinks);
		this.fuehrungsformAttributeRechts = new HashSet<>(fuehrungsformAttributeRechts);
		this.isZweiseitig = isZweiseitig;
	}

	public static FuehrungsformAttributGruppeBuilder builder() {
		return privateBuilder()
			.fuehrungsformAttributeRechts(List.of(FuehrungsformAttribute.builder().build()))
			.fuehrungsformAttributeLinks(List.of(FuehrungsformAttribute.builder().build()))
			.isZweiseitig(false);
	}

	public static boolean isSeitenBezugValid(List<FuehrungsformAttribute> fuehrungsformAttributeLinks,
		List<FuehrungsformAttribute> fuehrungsformAttributeRechts, boolean isZweiseitig) {
		if (!isZweiseitig) {
			if (fuehrungsformAttributeLinks.size() != fuehrungsformAttributeRechts.size()) {
				return false;
			} else {
				for (int i = 0; i < fuehrungsformAttributeLinks.size(); i++) {
					if (!fuehrungsformAttributeLinks.get(i).equals(fuehrungsformAttributeRechts.get(i))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public List<FuehrungsformAttribute> getImmutableFuehrungsformAttributeLinks() {
		return Collections.unmodifiableList(new ArrayList<>(fuehrungsformAttributeLinks));
	}

	public List<FuehrungsformAttribute> getImmutableFuehrungsformAttributeRechts() {
		return Collections.unmodifiableList(new ArrayList<>(fuehrungsformAttributeRechts));
	}

	public Set<FuehrungsformAttribute> getImmutableFuehrungsformAttributeLinksSet() {
		return Collections.unmodifiableSet(fuehrungsformAttributeLinks);
	}

	public void replaceFuehrungsformAttribute(List<FuehrungsformAttribute> fuehrungsformAttributeLinks,
		List<FuehrungsformAttribute> fuehrungsformAttributeRechts) {
		require(isSeitenBezugValid(fuehrungsformAttributeLinks, fuehrungsformAttributeRechts, isZweiseitig),
			"Nur für zweiseitige Führungsformen kann die rechte Seite seperat geändert werden");

		replaceFuehrungsformAttributeLinks(fuehrungsformAttributeLinks);
		replaceFuehrungsformAttributeRechts(fuehrungsformAttributeRechts);
	}

	public void replaceFuehrungsformAttribute(List<FuehrungsformAttribute> fuehrungsformAttribute) {
		replaceFuehrungsformAttributeLinks(fuehrungsformAttribute);
		replaceFuehrungsformAttributeRechts(new ArrayList<FuehrungsformAttribute>(fuehrungsformAttribute));
	}

	public void changeSeitenbezug(boolean isZweiseitig) {
		this.isZweiseitig = isZweiseitig;
		// Wenn die Attributgruppe nicht zweiseitig ist, sollen die Werte auf der linken und der rechten Seite gleich
		// sein.
		if (!isZweiseitig) {
			replaceFuehrungsformAttributeRechts(new ArrayList<>(fuehrungsformAttributeLinks));
		}
	}

	private void replaceFuehrungsformAttributeLinks(List<FuehrungsformAttribute> fuehrungsformAttribute) {
		require(fuehrungsformAttribute, notNullValue());
		require(fuehrungsformAttribute, Matchers.not(Matchers.empty()));
		List<LinearReferenzierterAbschnitt> lineareReferenzen = fuehrungsformAttribute.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.collect(Collectors.toList());
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(lineareReferenzen),
			"Fehlerhafte Referenzen: " + lineareReferenzen);

		this.fuehrungsformAttributeLinks.clear();
		this.fuehrungsformAttributeLinks.addAll(fuehrungsformAttribute);
	}

	private void replaceFuehrungsformAttributeRechts(List<FuehrungsformAttribute> fuehrungsformAttribute) {
		require(fuehrungsformAttribute, notNullValue());
		require(fuehrungsformAttribute, Matchers.not(Matchers.empty()));
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(fuehrungsformAttribute.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
			.collect(Collectors.toList())));

		this.fuehrungsformAttributeRechts.clear();
		this.fuehrungsformAttributeRechts.addAll(fuehrungsformAttribute);
	}

	public void reset() {
		List<FuehrungsformAttribute> fuehrungsformAttributeBeideSeiten = List.of(
			FuehrungsformAttribute.builder().build());
		this.replaceFuehrungsformAttributeLinks(fuehrungsformAttributeBeideSeiten);
		this.replaceFuehrungsformAttributeRechts(fuehrungsformAttributeBeideSeiten);
		this.isZweiseitig = false;
	}

	public BelagArt getBelagArtWertMitGroesstemAnteilLinks() {
		return LinearReferenzierteAttribute.getWertMitGroesstemAnteil(fuehrungsformAttributeLinks,
			FuehrungsformAttribute::getBelagArt);
	}

	public BelagArt getBelagArtWertMitGroesstemAnteilRechts() {
		return LinearReferenzierteAttribute.getWertMitGroesstemAnteil(fuehrungsformAttributeRechts,
			FuehrungsformAttribute::getBelagArt);
	}

	public Radverkehrsfuehrung getRadverkehrsfuehrungWertMitGroesstemAnteilLinks() {
		return LinearReferenzierteAttribute.getWertMitGroesstemAnteil(fuehrungsformAttributeLinks,
			FuehrungsformAttribute::getRadverkehrsfuehrung);
	}

	public Radverkehrsfuehrung getRadverkehrsfuehrungWertMitGroesstemAnteilRechts() {
		return LinearReferenzierteAttribute.getWertMitGroesstemAnteil(fuehrungsformAttributeRechts,
			FuehrungsformAttribute::getRadverkehrsfuehrung);
	}

	public Optional<Laenge> getBreiteWertMitGroesstemAnteilLinks() {
		return LinearReferenzierteAttribute.getWertMitGroesstemAnteil(fuehrungsformAttributeLinks,
			FuehrungsformAttribute::getBreite);
	}

	public Optional<Laenge> getBreiteWertMitGroesstemAnteilRechts() {
		return LinearReferenzierteAttribute.getWertMitGroesstemAnteil(fuehrungsformAttributeRechts,
			FuehrungsformAttribute::getBreite);
	}

	public Oberflaechenbeschaffenheit getOberflaechenbeschaffenheitWertMitGroesstemAnteilLinks() {
		return LinearReferenzierteAttribute.getWertMitGroesstemAnteil(fuehrungsformAttributeLinks,
			FuehrungsformAttribute::getOberflaechenbeschaffenheit);
	}

	public Oberflaechenbeschaffenheit getOberflaechenbeschaffenheitWertMitGroesstemAnteilRechts() {
		return LinearReferenzierteAttribute.getWertMitGroesstemAnteil(fuehrungsformAttributeRechts,
			FuehrungsformAttribute::getOberflaechenbeschaffenheit);
	}
}
