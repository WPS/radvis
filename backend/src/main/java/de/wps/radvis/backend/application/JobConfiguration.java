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

package de.wps.radvis.backend.application;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageBRImportJob;
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageRepository;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.InaktivitaetConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.InitialAdminImportConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.InitialBenutzerImportJob;
import de.wps.radvis.backend.benutzer.domain.SetzeBenutzerInaktivJob;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributProjektionsJob;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributProjektionsService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributProjektionsStatistikService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributeAnreicherungsService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenDublettenPruefungService;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZNachbearbeitungsRepository;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZNetzbildungJob;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZSackgassenJob;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungJob;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.leihstation.domain.LeihstationMobiDataImportJob;
import de.wps.radvis.backend.leihstation.domain.LeihstationRepository;
import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.DlmPbfErstellungsJob;
import de.wps.radvis.backend.matching.domain.KanteUpdateElevationJob;
import de.wps.radvis.backend.matching.domain.KanteUpdateElevationService;
import de.wps.radvis.backend.matching.domain.MatchNetzAufDLMJob;
import de.wps.radvis.backend.matching.domain.MatchNetzAufOSMJob;
import de.wps.radvis.backend.matching.domain.OsmAbbildungsFehlerRepository;
import de.wps.radvis.backend.matching.domain.OsmAuszeichnungsJob;
import de.wps.radvis.backend.matching.domain.OsmMatchingRepository;
import de.wps.radvis.backend.matching.domain.OsmPbfDownloadJob;
import de.wps.radvis.backend.matching.schnittstelle.LoadGraphhopperJob;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GraphhopperUpdateService;
import de.wps.radvis.backend.matching.domain.service.MatchingJobProtokollService;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.matching.domain.service.OsmAuszeichnungsService;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.StreckenViewService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportJob;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.WahlkreisImportJob;
import de.wps.radvis.backend.organisation.domain.WahlkreisRepository;
import de.wps.radvis.backend.quellimport.common.domain.FeatureImportRepository;
import de.wps.radvis.backend.quellimport.common.domain.GenericQuellImportJob;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import de.wps.radvis.backend.quellimport.radnetz.domain.RadNETZQuellImportJob;
import de.wps.radvis.backend.quellimport.ttsib.domain.TTSibImportJob;
import de.wps.radvis.backend.quellimport.ttsib.domain.TtSibFahrradwegRepository;
import de.wps.radvis.backend.quellimport.ttsib.domain.TtSibRepository;
import de.wps.radvis.backend.servicestation.domain.ServicestationMobiDataImportJob;
import de.wps.radvis.backend.servicestation.domain.ServicestationRepository;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.WegweisendeBeschilderungConfigurationProperties;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.WegweisendeBeschilderungImportJob;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.repository.WegweisendeBeschilderungRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class JobConfiguration {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private ImportedFeaturePersistentRepository importedFeatureRepository;

	@Autowired
	private RadNetzNetzbildungService radNetzNetzbildungService;

	@Autowired
	private RadwegeDBNetzbildungService radwegeDBNetzbildungService;

	@Autowired
	private KantenDublettenPruefungService kantenDublettenPruefungService;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private MatchingJobProtokollService osmJobProtokollService;

	@Autowired
	private DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory;

	@Lazy
	@Autowired
	private OsmMatchingRepository osmMatchingRepository;

	@Lazy
	@Autowired
	private DlmMatchingRepository dlmMatchingRepository;

	@Lazy
	@Autowired
	private NetzService netzService;

	@Autowired
	private MatchingKorrekturService osmMatchingKorrekturService;

	@Autowired
	private StreckenViewService streckenViewService;

	@Lazy
	@Autowired
	private FeatureImportRepository featureImportRepository;

	@Lazy
	@Autowired
	private TtSibRepository ttSibRepository;

	@Lazy
	@Autowired
	private TtSibFahrradwegRepository ttSibFahrradwegRepository;

	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@Autowired
	private JobConfigurationProperties jobConfigurationProperties;

	@Autowired
	private CommonConfigurationProperties commonConfigurationProperties;

	@Autowired
	private DLMConfigurationProperties dlmConfigurationProperties;

	@Autowired
	private InitialAdminImportConfigurationProperties initialAdminImportConfigurationProperties;

	@Autowired
	private TechnischerBenutzerConfigurationProperties technischerBenutzerConfigurationProperties;

	@Autowired
	private AttributProjektionsService attributProjektionsService;

	@Autowired
	private AttributeAnreicherungsService attributAnreicherungsService;

	@Autowired
	private AttributProjektionsStatistikService attributProjektionsStatistikService;

	@Autowired
	private OsmAuszeichnungsService osmAuszeichnungsService;

	@Autowired
	private OsmPbfConfigurationProperties osmPbfConfigurationProperties;

	@Autowired
	private RadNETZNachbearbeitungsRepository radNETZNachbearbeitungsRepository;

	@Autowired
	private DlmPbfErstellungService dlmPbfErstellungService;

	@Autowired
	@Lazy
	private GraphhopperUpdateService graphhopperUpdateService;

	@Autowired
	private OrganisationRepository organisationRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private GeoJsonImportRepository geoJsonImportRepository;

	@Autowired
	private WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository;

	@Autowired
	private WegweisendeBeschilderungConfigurationProperties wegweisendeBeschilderungConfigurationProperties;

	@Autowired
	private CsvRepository csvRepository;

	@Autowired
	CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	@Autowired
	AbstellanlageRepository abstellanlageRepository;

	@Autowired
	private LeihstationRepository leihstationRepository;

	@Autowired
	ServicestationRepository servicestationRepository;

	@Autowired
	private VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	private OsmAbbildungsFehlerRepository osmAbbildungsFehlerRepository;

	@Autowired
	private KanteUpdateElevationService kanteUpdateElevationService;

	@Autowired
	private VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	private WahlkreisRepository wahlkreisRepository;

	@Autowired
	private ShapeFileRepository shapeFileRepository;

	@Autowired
	InaktivitaetConfigurationProperties inaktivitaetConfigurationProperties;

	@Autowired
	BenutzerService benutzerService;

	@Autowired
	@Lazy
	VerwaltungseinheitImportRepository organisationenImportRepository;

	@Bean
	public RadNETZQuellImportJob radNetzQuellImportJob() {
		File radNetzShapeFileRoot = new File(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getRadNetzShapeFilesPath());
		File radNetzStreckenShapeFileRoot = new File(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getRadNetzStreckenShapeFilesPath());
		return new RadNETZQuellImportJob(jobExecutionDescriptionRepository,
			featureImportRepository,
			importedFeatureRepository,
			radNetzShapeFileRoot,
			radNetzStreckenShapeFileRoot);
	}

	@Bean
	public GenericQuellImportJob radwegeLglTuttlingenImportJob() {
		File radwegeLglTuttlingenLinienFile = new File(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getRadwegeLglTuttlingenShpFilePath());
		return new GenericQuellImportJob(jobExecutionDescriptionRepository,
			featureImportRepository, importedFeatureRepository,
			"tuttlingen", radwegeLglTuttlingenLinienFile, QuellSystem.LGL, Art.Strecke, Geometry.TYPENAME_LINESTRING);
	}

	@Bean
	public GenericQuellImportJob radwegeDbImportJob() {
		File radwegeDBShapeFile = new File(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getRadWegeDBShapeFilePath());
		Predicate<ImportedFeature> filter = importedFeature -> importedFeature.getAttribute()
			.getOrDefault("radverkehr", "0").equals(1);
		return new GenericQuellImportJob(jobExecutionDescriptionRepository,
			featureImportRepository, importedFeatureRepository,
			"radwegeDB", radwegeDBShapeFile, QuellSystem.RadwegeDB, Art.Strecke, Geometry.TYPENAME_MULTILINESTRING,
			filter);
	}

	@Bean
	public GenericQuellImportJob rvkEsslingenImportJob() {
		File rvkEsslingenShapeFile = new File(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getRvkEsslingenShapeFilePath());
		return new GenericQuellImportJob(jobExecutionDescriptionRepository,
			featureImportRepository, importedFeatureRepository,
			"RvkEsslingen", rvkEsslingenShapeFile, QuellSystem.RvkEsslingen, Art.Strecke, Geometry.TYPENAME_LINESTRING);
	}

	@Bean
	public GenericQuellImportJob bietigheimBissingenImportJob() {
		File bietigheimBissingenShapeFile = new File(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getBietigheimBissingenShapeFilePath());
		return new GenericQuellImportJob(jobExecutionDescriptionRepository,
			featureImportRepository, importedFeatureRepository,
			"bietigheimBissingen", bietigheimBissingenShapeFile, QuellSystem.BietigheimBissingen, Art.Strecke,
			Geometry.TYPENAME_LINESTRING);
	}

	@Bean
	public TTSibImportJob ttSibImportJob() {
		File ttSibFolder = new File(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getTtSibFilesPath());
		return new TTSibImportJob(jobExecutionDescriptionRepository, ttSibRepository, ttSibFahrradwegRepository,
			ttSibFolder, entityManager);
	}

	@Bean
	public VerwaltungseinheitImportJob organisationenImportJob() {
		return new VerwaltungseinheitImportJob(
			jobExecutionDescriptionRepository,
			org.springframework.data.util.Lazy.of(() -> organisationenImportRepository),
			organisationRepository,
			gebietskoerperschaftRepository);
	}

	@Bean
	public InitialBenutzerImportJob initialAdminImportJob(BenutzerService benutzerService) {
		return new InitialBenutzerImportJob(jobExecutionDescriptionRepository,
			initialAdminImportConfigurationProperties,
			technischerBenutzerConfigurationProperties, benutzerService);
	}

	@Bean
	public RadNETZNetzbildungJob radNetzNetzbildungJob() {
		return new RadNETZNetzbildungJob(importedFeatureRepository, netzfehlerRepository, radNetzNetzbildungService,
			jobExecutionDescriptionRepository);
	}

	@Bean
	public RadwegeDBNetzbildungJob radwegeDBNetzbildungJob() {
		return new RadwegeDBNetzbildungJob(importedFeatureRepository, netzfehlerRepository, radwegeDBNetzbildungService,
			jobExecutionDescriptionRepository);
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public MatchNetzAufOSMJob matchNetzAufOSMJob() {
		return new MatchNetzAufOSMJob(jobExecutionDescriptionRepository, netzfehlerRepository,
			org.springframework.data.util.Lazy.of(() -> osmMatchingRepository),
			netzService, osmMatchingKorrekturService, osmJobProtokollService, entityManager,
			dlmConfigurationProperties, osmAbbildungsFehlerRepository);
	}

	@Bean
	public MatchNetzAufDLMJob matchRadwegeDbAufDLMJob() {
		return new MatchNetzAufDLMJob(jobExecutionDescriptionRepository, netzfehlerRepository,
			org.springframework.data.util.Lazy.of(() -> dlmMatchingRepository), netzService, streckenViewService,
			osmMatchingKorrekturService, osmJobProtokollService,
			entityManager, dlmConfigurationProperties, QuellSystem.RadwegeDB);
	}

	@Bean
	public MatchNetzAufDLMJob matchRadNETZAufDLMJob() {
		return new MatchNetzAufDLMJob(jobExecutionDescriptionRepository, netzfehlerRepository,
			org.springframework.data.util.Lazy.of(() -> dlmMatchingRepository), netzService, streckenViewService,
			osmMatchingKorrekturService, osmJobProtokollService,
			entityManager, dlmConfigurationProperties, QuellSystem.RadNETZ);
	}

	@Bean
	public AttributProjektionsJob radwegeDbAttributProjektionsJob() {
		return new AttributProjektionsJob(jobExecutionDescriptionRepository, attributProjektionsService,
			attributAnreicherungsService, attributProjektionsStatistikService, netzfehlerRepository,
			kantenDublettenPruefungService, netzService,
			importedFeatureRepository, dlmConfigurationProperties, entityManager, QuellSystem.RadwegeDB);
	}

	@Bean
	public AttributProjektionsJob radNETZAttributProjektionsJob() {
		return new AttributProjektionsJob(jobExecutionDescriptionRepository, attributProjektionsService,
			attributAnreicherungsService, attributProjektionsStatistikService, netzfehlerRepository,
			kantenDublettenPruefungService, netzService,
			importedFeatureRepository, dlmConfigurationProperties, entityManager, QuellSystem.RadNETZ);
	}

	@Bean
	public OsmPbfDownloadJob osmPbfDownloadJob() {
		File osmBasisDaten = new File(osmPbfConfigurationProperties.getOsmBasisDaten());
		return new OsmPbfDownloadJob(jobExecutionDescriptionRepository, osmBasisDaten,
			osmPbfConfigurationProperties.getOsmBasisDatenDownloadLink());
	}

	@Bean
	public OsmAuszeichnungsJob osmAuszeichnungsJob() {
		File osmBasisDaten = new File(osmPbfConfigurationProperties.getOsmBasisDaten());
		File osmAngereichertDaten = new File(osmPbfConfigurationProperties.getOsmAngereichertDaten());
		return new OsmAuszeichnungsJob(jobExecutionDescriptionRepository, osmAuszeichnungsService,
			osmBasisDaten, osmAngereichertDaten);
	}

	@Bean
	public DlmPbfErstellungsJob dlmPbfErstellungsJob() {
		return new DlmPbfErstellungsJob(
			dlmPbfErstellungService,
			jobExecutionDescriptionRepository,
			org.springframework.data.util.Lazy.of(graphhopperUpdateService));
	}

	@Bean
	public RadNETZSackgassenJob radNETZSackgassenJob() {
		return new RadNETZSackgassenJob(netzfehlerRepository, radNETZNachbearbeitungsRepository,
			jobExecutionDescriptionRepository);
	}

	@Bean
	public WegweisendeBeschilderungImportJob wegweisendeBeschilderungImportJob() {
		return new WegweisendeBeschilderungImportJob(
			wegweisendeBeschilderungConfigurationProperties.getImportGeoJsonUrl(),
			geoJsonImportRepository, wegweisendeBeschilderungRepository,
			org.springframework.data.util.Lazy.of(
				() -> gebietskoerperschaftRepository.findByNameAndOrganisationsArt(
					commonConfigurationProperties.getObersteGebietskoerperschaftName(),
					commonConfigurationProperties.getObersteGebietskoerperschaftOrganisationsArt()).orElseThrow(() -> {
						throw new NoSuchElementException(String.format(
							"Verwaltungseinheit \"%s (%s})\" fehlt.",
							commonConfigurationProperties.getObersteGebietskoerperschaftName(),
							commonConfigurationProperties.getObersteGebietskoerperschaftOrganisationsArt()));
					})),
			jobExecutionDescriptionRepository);
	}

	@Bean
	public AbstellanlageBRImportJob abstellanlageBRImportJob() {
		return new AbstellanlageBRImportJob(jobExecutionDescriptionRepository, jobConfigurationProperties,
			csvRepository, coordinateReferenceSystemConverter, abstellanlageRepository);
	}

	@Bean
	public LeihstationMobiDataImportJob leihstationMobiDataImportJob() {
		return new LeihstationMobiDataImportJob(
			jobExecutionDescriptionRepository,
			verwaltungseinheitService,
			leihstationRepository,
			geoJsonImportRepository,
			jobConfigurationProperties.getLeihstationImportUrl());
	}

	@Bean
	public ServicestationMobiDataImportJob servicestationMobiDataImportJob() {
		return new ServicestationMobiDataImportJob(
			jobExecutionDescriptionRepository,
			geoJsonImportRepository,
			servicestationRepository,
			verwaltungseinheitService,
			verwaltungseinheitRepository,
			jobConfigurationProperties.getServicestationImportUrl());
	}

	@Bean
	public KanteUpdateElevationJob kanteUpdateElevationJob() {
		return new KanteUpdateElevationJob(jobExecutionDescriptionRepository, kanteUpdateElevationService);
	}

	@Bean
	public WahlkreisImportJob wahlkreisImportJob() {
		return new WahlkreisImportJob(jobExecutionDescriptionRepository, wahlkreisRepository, shapeFileRepository,
			new File(commonConfigurationProperties.getExterneResourcenBasisPfad(),
				jobConfigurationProperties.getWahlkreisePath()));
	}

	@Bean
	public SetzeBenutzerInaktivJob setzeBenutzerInaktivJob() {
		return new SetzeBenutzerInaktivJob(
			jobExecutionDescriptionRepository,
			benutzerService,
			inaktivitaetConfigurationProperties.inaktivitaetsTimeoutInTagen());
	}

	@Bean
	public LoadGraphhopperJob reloadGraphhopperJob() {
		return new LoadGraphhopperJob(jobExecutionDescriptionRepository, dlmMatchedGraphHopperFactory);
	}
}
