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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Objects;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonExportConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.GeoJsonImportRepositoryImpl;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.MassnahmenImportAttribute;
import de.wps.radvis.backend.massnahme.domain.MassnahmenExporterService;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeListenDbViewTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.organisation.domain.OrganisationsartUndNameNichtEindeutigException;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import jakarta.persistence.EntityManager;

public class ManuellerMassnahmenImportServiceIntegrationTest {
	private MassnahmenExporterService massnahmenExporterService;

	private ManuellerMassnahmenImportService massnahmenImportService;

	@Mock
	private MassnahmeViewRepository massnahmeViewRepository;

	private GeoJsonImportRepository geoJsonImportRepository;

	@Mock
	private ManuellerImportService manuellerImportService;

	private MassnahmeNetzbezugService massnahmeNetzbezugService;

	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;

	@Mock
	private EntityManager entityManager;

	@Mock
	private CsvRepository csvRepository;

	@Mock
	private MassnahmeRepository massnahmeRepository;

	@Mock
	private NetzService netzService;

	@Mock
	private SimpleMatchingService simpleMatchingService;

	private GeoJsonExportConverter geoJsonExportConverter;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		geoJsonImportRepository = new GeoJsonImportRepositoryImpl(
			new CoordinateReferenceSystemConverter(new Envelope(0, 0, 100, 100)));
		massnahmeNetzbezugService = new MassnahmeNetzbezugService(simpleMatchingService, netzService);
		geoJsonExportConverter = new GeoJsonExportConverter();

		massnahmenExporterService = new MassnahmenExporterService(massnahmeViewRepository);
		massnahmenImportService = new ManuellerMassnahmenImportService(manuellerImportService,
			massnahmeNetzbezugService, geoJsonImportRepository, verwaltungseinheitService, massnahmeRepository,
			entityManager, csvRepository, 10.0);
	}

	@Test
	public void exportImport_simple() throws OrganisationsartUndNameNichtEindeutigException {
		Kante kante = KanteTestDataProvider.withDefaultValues().id(12l).build();
		final MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
			Set.of(), Collections.emptySet());

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).sonstigeKonzeptionsquelle(null).netzbezug(netzbezug)
			.id(23l).build();
		Massnahme savedMassnahme = exportAndReimport(massnahme);

		assertThat(savedMassnahme).usingRecursiveComparison()
			.ignoringFields("id", "version", "letzteAenderung", "benutzerLetzteAenderung")
			.isEqualTo(massnahme);
	}

	@Test
	public void exportImport_punktAufKante() throws OrganisationsartUndNameNichtEindeutigException {
		Kante kante = KanteTestDataProvider.withDefaultValues().id(12l).build();
		final MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(), Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.5), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).sonstigeKonzeptionsquelle(null).netzbezug(netzbezug)
			.id(23l).build();
		Massnahme savedMassnahme = exportAndReimport(massnahme);

		assertThat(savedMassnahme.getNetzbezug()).isEqualTo(netzbezug);
	}

	@Test
	public void exportImport_punktAufKnoten() throws OrganisationsartUndNameNichtEindeutigException {
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(12l).build();
		final MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(), Set.of(),
			Set.of(knoten));

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).sonstigeKonzeptionsquelle(null).netzbezug(netzbezug)
			.id(23l).build();
		Massnahme savedMassnahme = exportAndReimport(massnahme);

		assertThat(savedMassnahme.getNetzbezug()).isEqualTo(netzbezug);
	}

	private Massnahme exportAndReimport(Massnahme massnahme) throws OrganisationsartUndNameNichtEindeutigException {
		long organisationsId = 1l;
		Set<Kante> abschnittKanten = massnahme.getNetzbezug().getImmutableKantenAbschnittBezug().stream()
			.map(ab -> ab.getKante()).collect(Collectors.toSet());
		Set<Kante> punktKanten = massnahme.getNetzbezug().getImmutableKantenPunktBezug().stream()
			.map(ab -> ab.getKante())
			.collect(Collectors.toSet());
		Set<Kante> allKanten = abschnittKanten;
		allKanten.addAll(punktKanten);

		assertThat(allKanten).allMatch(k -> k.getId() != null, "Bitte Kanten mit IDs ausstatten");

		when(verwaltungseinheitService.getVereintenBereich(any()))
			.thenReturn(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100));
		when(massnahmeViewRepository.findAllByIdIn(any())).thenReturn(List.of(MassnahmeListenDbViewTestDataProvider
			.withMassnahme(massnahme).build()));
		when(verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(
			eq(massnahme.getZustaendiger().get().getName()),
			eq(massnahme.getZustaendiger().get().getOrganisationsArt()))).thenReturn(massnahme.getZustaendiger());
		when(verwaltungseinheitService.getAllNames(any())).thenReturn("bereiche");

		when(simpleMatchingService.matche(any(), any())).thenAnswer(i -> {
			OsmMatchResult matchResult = mock(OsmMatchResult.class);
			when(matchResult.getGeometrie()).thenReturn(i.getArgument(0));
			when(matchResult.getOsmWayIds())
				.thenReturn(abschnittKanten.stream().map(k -> OsmWayId.of(k.getId())).collect(Collectors.toSet()));
			return Optional.of(matchResult);
		});

		when(netzService.getRadVisNetzKantenInBereich(any()))
			.thenReturn(punktKanten.stream().collect(Collectors.toList()));
		when(netzService.getRadVisNetzKnotenInBereich(any()))
			.thenReturn(massnahme.getNetzbezug().getImmutableKnotenBezug().stream().collect(Collectors.toList()));
		when(netzService.getKante(anyLong()))
			.thenAnswer(
				i -> abschnittKanten.stream().filter(k -> Objects.equal(k.getId(), i.getArguments()[0])).findAny()
					.get());

		ArgumentCaptor<Massnahme> savedMassnahmeCaptor = ArgumentCaptor.forClass(Massnahme.class);
		when(massnahmeRepository.save(savedMassnahmeCaptor.capture())).thenAnswer(i -> i.getArguments()[0]);

		List<ExportData> export = massnahmenExporterService.export(List.of());
		byte[] geoJson = geoJsonExportConverter.convert(export);

		MassnahmenImportSession session = massnahmenImportService.createSession(
			BenutzerTestDataProvider.defaultBenutzer().build(), List.of(organisationsId),
			massnahme.getKonzeptionsquelle(), null);
		massnahmenImportService.ladeFeatures(session, geoJson);
		massnahmenImportService.attributeValidieren(session, List.of(MassnahmenImportAttribute.values()));
		massnahmenImportService.erstelleNetzbezuege(session);
		massnahmenImportService.speichereMassnahmenDerZuordnungen(session,
			session.getZuordnungen().stream().map(z -> z.getId()).collect(Collectors.toList()));

		Massnahme savedMassnahme = savedMassnahmeCaptor.getValue();
		return savedMassnahme;
	}
}
