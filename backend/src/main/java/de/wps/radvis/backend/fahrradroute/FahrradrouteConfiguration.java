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

package de.wps.radvis.backend.fahrradroute;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.repository.FahrradrouteFilterRepository;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.fahrradroute.domain.DRouteImportJob;
import de.wps.radvis.backend.fahrradroute.domain.DRouteMatchingJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteConfigurationProperties;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteService;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenExporterService;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenMatchingService;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenTfisImportJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenTfisUpdateJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenToubizImportJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenVariantenTfisImportJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenVariantenTfisUpdateJob;
import de.wps.radvis.backend.fahrradroute.domain.LandesradfernwegeTfisImportJob;
import de.wps.radvis.backend.fahrradroute.domain.LandesradfernwegeVariantenTfisImportJob;
import de.wps.radvis.backend.fahrradroute.domain.ProfilInformationenUpdateJob;
import de.wps.radvis.backend.fahrradroute.domain.RecreateFahrradrouteImportDiffViewJob;
import de.wps.radvis.backend.fahrradroute.domain.TfisImportService;
import de.wps.radvis.backend.fahrradroute.domain.UpdateAbgeleiteteRoutenInformationJob;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteNetzBezugAenderungRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteViewRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.ToubizRepository;
import de.wps.radvis.backend.fahrradroute.schnittstelle.CreateFahrradrouteCommandConverter;
import de.wps.radvis.backend.fahrradroute.schnittstelle.FahrradrouteGuard;
import de.wps.radvis.backend.fahrradroute.schnittstelle.SaveFahrradrouteCommandConverter;
import de.wps.radvis.backend.fahrradroute.schnittstelle.ToubizConfigurationProperties;
import de.wps.radvis.backend.fahrradroute.schnittstelle.ToubizRepositoryImpl;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.repository.GraphhopperRoutingRepository;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.SackgassenService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class FahrradrouteConfiguration {
	private final RestTemplate restTemplate;
	private final GeoConverterConfiguration geoConverterConfiguration;
	private final BenutzerService benutzerService;
	private final KanteResolver kanteResolver;
	private final NetzService netzService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	private JobConfigurationProperties jobConfigurationProperties;
	@Autowired
	private FahrradrouteConfigurationProperties fahrradrouteConfigurationProperties;

	@Autowired
	private FahrradrouteNetzBezugAenderungRepository fahrradrouteNetzBezugAenderungRepository;

	@Autowired
	private ShapeFileRepository shapeFileRepository;

	@NonNull
	private BenutzerResolver benutzerResolver;

	@NonNull
	private CommonConfigurationProperties commonConfigurationProperties;
	@NonNull
	private SackgassenService sackgassenService;

	public FahrradrouteConfiguration(
		@NonNull GeoConverterConfiguration geoConverterConfiguration,
		@NonNull CommonConfigurationProperties commonConfigurationProperties,
		@NonNull NetzService netzService,
		@NonNull BenutzerService benutzerService,
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull KanteResolver kanteResolver,
		@NonNull ZustaendigkeitsService zustaendigkeitsService,
		@NonNull VerwaltungseinheitService verwaltungseinheitService,
		@NonNull SackgassenService sackgassenService) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

		this.commonConfigurationProperties = commonConfigurationProperties;
		this.geoConverterConfiguration = geoConverterConfiguration;
		this.netzService = netzService;
		this.benutzerService = benutzerService;
		this.benutzerResolver = benutzerResolver;
		this.kanteResolver = kanteResolver;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.sackgassenService = sackgassenService;

		if (commonConfigurationProperties.getProxyAdress() != null) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP,
				new InetSocketAddress(commonConfigurationProperties.getProxyAdress(),
					commonConfigurationProperties.getProxyPort()));
			requestFactory.setProxy(proxy);
			restTemplate = new RestTemplate(requestFactory);
		} else {
			restTemplate = new RestTemplate();
		}
	}

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private FahrradrouteRepository fahrradrouteRepository;

	@Autowired
	private FahrradrouteViewRepository fahrradrouteViewRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private ToubizConfigurationProperties toubizConfigurationProperties;

	@Lazy
	@Autowired
	private DlmMatchingRepository dlmMatchingRepository;

	@Lazy
	@Autowired
	private GraphhopperRoutingRepository graphhopperRoutingRepository;

	@Bean
	public ToubizRepository toubizRepository() {
		return new ToubizRepositoryImpl(geoConverterConfiguration.coordinateReferenceSystemConverter(), restTemplate,
			toubizConfigurationProperties);
	}

	@Bean
	public FahrradrouteService fahrradrouteService() {
		return new FahrradrouteService(fahrradrouteRepository, fahrradrouteViewRepository,
			org.springframework.data.util.Lazy.of(() -> graphhopperRoutingRepository),
			fahrradrouteNetzBezugAenderungRepository, jobExecutionDescriptionRepository, benutzerService,
			sackgassenService, commonConfigurationProperties.getErlaubteAbweichungFuerKantenNetzbezugRematch());
	}

	@Bean
	public FahrradrouteGuard fahrradrouteGuard() {
		return new FahrradrouteGuard(netzService, benutzerResolver, zustaendigkeitsService, fahrradrouteService());
	}

	@Bean
	public FahrradroutenMatchingService fahrradroutenMatchingService() {
		return new FahrradroutenMatchingService(kantenRepository,
			org.springframework.data.util.Lazy.of(() -> dlmMatchingRepository),
			org.springframework.data.util.Lazy.of(() -> graphhopperRoutingRepository),
			verwaltungseinheitService);
	}

	@Bean
	public FahrradroutenToubizImportJob fahrradroutenToubizImportJob() {
		return new FahrradroutenToubizImportJob(jobExecutionDescriptionRepository, toubizRepository(),
			verwaltungseinheitService, fahrradrouteRepository, fahrradroutenMatchingService(),
			Duration.ofSeconds(fahrradrouteConfigurationProperties.getTimeoutToubizImportMatchingInSeconds()),
			fahrradrouteConfigurationProperties.getToubizIgnoreList());
	}

	@Bean
	public ProfilInformationenUpdateJob profiInformationenUpdateJob() {
		return new ProfilInformationenUpdateJob(jobExecutionDescriptionRepository, fahrradrouteService());
	}

	@Bean
	public UpdateAbgeleiteteRoutenInformationJob updateAbgeleiteteRoutenInformationJob() {
		return new UpdateAbgeleiteteRoutenInformationJob(jobExecutionDescriptionRepository, fahrradrouteService());
	}

	@Bean
	public FahrradroutenTfisUpdateJob fahrradroutenTfisUpdateJob() {
		Path shpFolder = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getTfisRadwegePath());

		return new FahrradroutenTfisUpdateJob(jobExecutionDescriptionRepository, tfisImportService(), kantenRepository,
			shapeFileRepository, shpFolder, fahrradrouteRepository);
	}

	@Bean
	public SaveFahrradrouteCommandConverter saveFahrradrouteCommandConverter() {
		return new SaveFahrradrouteCommandConverter(verwaltungseinheitService, kanteResolver);
	}

	@Bean
	public CreateFahrradrouteCommandConverter createFahrradrouteCommandConverter() {
		return new CreateFahrradrouteCommandConverter(benutzerResolver, kanteResolver);
	}

	@Bean
	public LandesradfernwegeTfisImportJob landesradfernwegeTFISImportJob() {
		Path shpFolder = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getTfisRadwegePath());

		return new LandesradfernwegeTfisImportJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			shapeFileRepository, kantenRepository, tfisImportService(), shpFolder);
	}

	@Bean
	public FahrradroutenTfisImportJob routenTFISImportJob() {
		Path shpFolder = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getTfisRadwegePath());

		return new FahrradroutenTfisImportJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			shapeFileRepository,
			kantenRepository,
			tfisImportService(),
			shpFolder);
	}

	@Bean
	public FahrradroutenVariantenTfisImportJob variantenTFISImportJob() {
		Path shpFolder = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getTfisRadwegePath());

		return new FahrradroutenVariantenTfisImportJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			shapeFileRepository,
			kantenRepository,
			tfisImportService(),
			shpFolder);
	}

	@Bean
	public LandesradfernwegeVariantenTfisImportJob landesradfernwegeVariantenTfisImportJob() {
		Path shpFolder = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getTfisRadwegePath());

		return new LandesradfernwegeVariantenTfisImportJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			shapeFileRepository,
			kantenRepository,
			tfisImportService(),
			shpFolder);
	}

	@Bean
	public FahrradroutenVariantenTfisUpdateJob fahrradroutenVariantenTfisUpdateJob() {
		Path shpFolder = Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
			jobConfigurationProperties.getTfisRadwegePath());

		return new FahrradroutenVariantenTfisUpdateJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			shapeFileRepository,
			kantenRepository,
			tfisImportService(),
			shpFolder);
	}

	@Bean
	public DRouteImportJob dRouteImportJob() {
		return new DRouteImportJob(jobExecutionDescriptionRepository, fahrradrouteRepository, shapeFileRepository,
			Paths.get(commonConfigurationProperties.getExterneResourcenBasisPfad(),
				jobConfigurationProperties.getDRoutenPath()));
	}

	@Bean
	public DRouteMatchingJob dRouteMatchingJob() {
		return new DRouteMatchingJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			fahrradroutenMatchingService());
	}

	@Bean
	public TfisImportService tfisImportService() {
		return new TfisImportService(verwaltungseinheitService, shapeFileRepository,
			kantenRepository, org.springframework.data.util.Lazy.of(() -> dlmMatchingRepository));
	}

	@Bean
	public FahrradroutenExporterService fahrradroutenExporterService() {
		return new FahrradroutenExporterService(fahrradrouteViewRepository);
	}

	@Bean
	public RecreateFahrradrouteImportDiffViewJob recreateFahrradrouteImportDiffViewJob() {
		return new RecreateFahrradrouteImportDiffViewJob(jobExecutionDescriptionRepository, fahrradrouteRepository,
			commonConfigurationProperties.getAnzahlTageImportprotokolleVorhalten(),
			fahrradrouteConfigurationProperties.getMaximaleAnzahlKoordinatenFuerImportDiff());
	}

	@Bean
	public FahrradrouteFilterRepository fahrradrouteFilterRepository() {
		return fahrradrouteRepository;
	}
}
