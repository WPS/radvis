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

package de.wps.radvis.backend.organisation.schnittstelle;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;

@RestController
@RequestMapping("/api/organisationen")
public class OrganisationController {

	private final VerwaltungseinheitService verwaltungseinheitService;
	private final OrganisationConfigurationProperties organisationConfigurationProperties;

	public OrganisationController(
		@NonNull VerwaltungseinheitService verwaltungseinheitService,
		@NonNull OrganisationConfigurationProperties organisationConfigurationProperties
	) {
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.organisationConfigurationProperties = organisationConfigurationProperties;
	}

	@GetMapping
	public List<VerwaltungseinheitView> listAktiveOrganisationSelectViews() {
		return verwaltungseinheitService.getAllAktiveAsView().stream().map(VerwaltungseinheitView::new)
			.sorted(Comparator.comparing(VerwaltungseinheitView::getName))
			.collect(Collectors.toList());
	}

	@GetMapping("all")
	public List<VerwaltungseinheitView> listAllVerwaltungseinheitenSelectViews() {
		return verwaltungseinheitService.getAll()
			.stream()
			.map(VerwaltungseinheitView::new)
			.sorted(Comparator.comparing(VerwaltungseinheitView::getName))
			.toList();
	}

	@GetMapping("gebietskoerperschaften")
	public List<VerwaltungseinheitView> listAktiveGebietskoerperschaftenSelectViews() {
		return verwaltungseinheitService.getAllAktiveAsView().stream().map(VerwaltungseinheitView::new)
			.filter(t -> t.getOrganisationsArt().istGebietskoerperschaft())
			.sorted(Comparator.comparing(VerwaltungseinheitView::getName))
			.collect(Collectors.toList());
	}

	@GetMapping("gemeinden")
	public List<VerwaltungseinheitView> gemeindenSelectViews() {
		return verwaltungseinheitService.getGemeinden()
			.stream()
			.map(VerwaltungseinheitView::new)
			.sorted(Comparator.comparing(VerwaltungseinheitView::getName))
			.collect(Collectors.toList());
	}

	@GetMapping("kreiseAlsFeatures")
	@SuppressWarnings("deprecation")
	public FeatureCollection kreiseAlsFeatures() {
		GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		FeatureCollection features = new FeatureCollection();

		features.addAll(
			verwaltungseinheitService.getKreise().stream()
				.filter(organisation -> organisation.getBereich().isPresent())
				.map(kreis -> {
					Feature createFeature = GeoJsonConverter.createFeature(
						geometryFactory.createGeometry(kreis.getBereich().get()));
					createFeature.setProperty("id", kreis.getId());
					createFeature.setProperty("name", kreis.getName());
					createFeature.setProperty("istQualitaetsgesichert",
						((Gebietskoerperschaft) kreis).getIstQualitaetsgesichert());
					return createFeature;
				})
				.collect(Collectors.toList()));
		return features;
	}

	@GetMapping("{id}")
	public VerwaltungseinheitView verwaltungseinheitView(@PathVariable("id") Long id) {
		return new VerwaltungseinheitView(
			verwaltungseinheitService.findById(id).orElseThrow(EntityNotFoundException::new));
	}

	@GetMapping("/bereichAlsString/{id}")
	public String getBereichVonOrganisationAlsString(@PathVariable("id") Long id) {
		return verwaltungseinheitService.findById(id)
			.map(verwaltungseinheit -> verwaltungseinheit.getBereichBufferSimplified(
				organisationConfigurationProperties.getZustaendigkeitBufferInMeter(),
				organisationConfigurationProperties.getZustaendigkeitSimplificationToleranceInMeter()
			))
			.orElseThrow(EntityNotFoundException::new).toText();
	}

	@GetMapping("/bereichEnvelopeView/{id}")
	public VerwaltungseinheitBereichEnvelopeView verwaltungseinheitBereichEnvelopeView(@PathVariable("id") Long id) {
		return new VerwaltungseinheitBereichEnvelopeView(
			verwaltungseinheitService.findById(id).orElseThrow(EntityNotFoundException::new));
	}
}
