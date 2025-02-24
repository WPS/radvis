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

package de.wps.radvis.backend.massnahme;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.thymeleaf.TemplateEngine;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.repository.FahrradrouteFilterRepository;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeRueckstufungStornierungService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenBenachrichtigungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.MassnahmenExporterService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenImportJob;
import de.wps.radvis.backend.massnahme.domain.MassnahmenMappingService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenZustaendigkeitsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenblaetterImportJob;
import de.wps.radvis.backend.massnahme.domain.RandomMassnahmenGenerierenJob;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandImportJob;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandabfrageService;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandsabfrageConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeNetzBezugAenderungRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeUmsetzungsstandViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.massnahme.schnittstelle.CreateMassnahmeCommandConverter;
import de.wps.radvis.backend.massnahme.schnittstelle.MassnahmeGuard;
import de.wps.radvis.backend.massnahme.schnittstelle.SaveMassnahmeCommandConverter;
import de.wps.radvis.backend.massnahme.schnittstelle.SaveUmsetzungsstandCommandConverter;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class MassnahmeConfiguration {

	@Autowired
	private MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository;

	@Autowired
	private MassnahmeRepository massnahmeRepository;

	@Autowired
	private UmsetzungsstandRepository umsetzungsstandRepository;

	@Autowired
	private MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	private JobConfigurationProperties jobConfigurationProperties;

	@Autowired
	private CommonConfigurationProperties commonConfigurationProperties;

	@Autowired
	private PostgisConfigurationProperties postgisConfigurationProperties;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private ShapeFileRepository shapeFileRepository;

	@Autowired
	private SimpleMatchingService simpleMatchingService;

	@Autowired
	private BenutzerService benutzerService;

	@Autowired
	private MassnahmeViewRepository massnahmeListRepository;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private MailService mailService;

	@Autowired
	private MailConfigurationProperties mailConfigurationProperties;

	@Autowired
	private UmsetzungsstandsabfrageConfigurationProperties umsetzungsstandsabfrageConfigurationProperties;

	@Autowired
	private TemplateEngine templateEngine;

	@Autowired
	private NetzService netzService;

	@Autowired
	private FahrradrouteFilterRepository fahrradrouteFilterRepository;

	@Autowired
	private MassnahmenConfigurationProperties massnahmenConfigurationProperties;

	private final NetzService kantenUndKnotenResolver;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final BenutzerResolver benutzerResolver;

	public MassnahmeConfiguration(@NonNull NetzService kantenUndKnotenResolver,
		@NonNull VerwaltungseinheitService verwaltungseinheitService,
		@NonNull ZustaendigkeitsService zustaendigkeitsService,
		@NonNull BenutzerResolver benutzerResolver) {
		this.kantenUndKnotenResolver = kantenUndKnotenResolver;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.benutzerResolver = benutzerResolver;
	}

	@Bean
	public CreateMassnahmeCommandConverter createMassnahmeCommandConverter() {
		return new CreateMassnahmeCommandConverter(kantenUndKnotenResolver, kantenUndKnotenResolver,
			verwaltungseinheitService, benutzerResolver);
	}

	@Bean
	public SaveMassnahmeCommandConverter saveMassnahmeCommandConverter() {
		return new SaveMassnahmeCommandConverter(kantenUndKnotenResolver, kantenUndKnotenResolver,
			verwaltungseinheitService, benutzerResolver);
	}

	@Bean
	public SaveUmsetzungsstandCommandConverter saveUmsetzungsstandCommandConverter() {
		return new SaveUmsetzungsstandCommandConverter(benutzerResolver);
	}

	@Bean
	public MassnahmeService massnahmeService() {
		return new MassnahmeService(massnahmeRepository, massnahmeListRepository,
			massnahmeUmsetzungsstandViewRepository, umsetzungsstandRepository, kantenRepository,
			massnahmeNetzbezugAenderungProtokollierungsService(), benutzerService, fahrradrouteFilterRepository,
			netzService,
			massnahmenConfigurationProperties.getDistanzZuFahrradrouteInMetern(),
			commonConfigurationProperties.getErlaubteAbweichungFuerKantenNetzbezugRematch());
	}

	@Bean
	public MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService() {
		return new MassnahmenZustaendigkeitsService(benutzerRepository);
	}

	@Bean
	public UmsetzungsstandabfrageService umsetzungsstandabfrageService() {
		return new UmsetzungsstandabfrageService(massnahmeRepository, massnahmenZustaendigkeitsService(),
			verwaltungseinheitService, mailService,
			mailConfigurationProperties, commonConfigurationProperties, umsetzungsstandsabfrageConfigurationProperties,
			postgisConfigurationProperties, templateEngine);
	}

	@Bean
	public MassnahmeRueckstufungStornierungService massnahmeRueckstufungStornierungService() {
		return new MassnahmeRueckstufungStornierungService(
			massnahmeService(),
			massnahmenZustaendigkeitsService(),
			benutzerService,
			kantenRepository,
			mailService,
			mailConfigurationProperties,
			commonConfigurationProperties,
			templateEngine);
	}

	@Bean
	public MassnahmenBenachrichtigungsService massnahmenBenachrichtigungsService() {
		return new MassnahmenBenachrichtigungsService(massnahmeService(), mailService,
			mailConfigurationProperties, commonConfigurationProperties, templateEngine);
	}

	@Bean
	public MassnahmeGuard massnahmeGuard() {
		return new MassnahmeGuard(zustaendigkeitsService, kantenUndKnotenResolver, benutzerResolver,
			massnahmeService());
	}

	@Bean
	public MassnahmenExporterService massnahmeExporterService() {
		return new MassnahmenExporterService(massnahmeListRepository);
	}

	@Bean
	public RandomMassnahmenGenerierenJob randomMassnahmenGenerierenJob() {
		return new RandomMassnahmenGenerierenJob(massnahmeService(), kantenRepository,
			verwaltungseinheitRepository, jobExecutionDescriptionRepository, benutzerResolver);
	}

	@Bean
	@SuppressWarnings("deprecation")
	public MassnahmenImportJob massnahmenImportJob() {
		Path shpFileFolderRoot = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getRadnetzMassnahmenImportPath());
		return new MassnahmenImportJob(jobExecutionDescriptionRepository, shpFileFolderRoot, shapeFileRepository,
			simpleMatchingService, netzService, massnahmenMappingService(), massnahmeService(), benutzerService);
	}

	@Bean
	public MassnahmenblaetterImportJob massnahmenblaetterImportJob() {
		Path dokukatasterFileFolder = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getMassnahmenBlaetterImportPath(), "dokukataster");
		Path massnahmenkatasterFileFolder = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getMassnahmenBlaetterImportPath(), "massnahmenkataster");
		return new MassnahmenblaetterImportJob(jobExecutionDescriptionRepository, dokukatasterFileFolder,
			massnahmenkatasterFileFolder,
			massnahmeService(), benutzerService);
	}

	@Bean
	public UmsetzungsstandImportJob umsetzungsstandImportJob() {
		Path csvFilePath = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getUmsetzungsstandabfragenCsvImportFilePath());
		return new UmsetzungsstandImportJob(jobExecutionDescriptionRepository, csvFilePath, massnahmeService(),
			benutzerService, verwaltungseinheitService);
	}

	@Bean
	@SuppressWarnings("deprecation")
	public MassnahmenMappingService massnahmenMappingService() {
		return new MassnahmenMappingService(verwaltungseinheitService);
	}

	@Bean
	public MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService() {
		return new MassnahmeNetzbezugAenderungProtokollierungsService(massnahmeNetzBezugAenderungRepository);
	}

}
