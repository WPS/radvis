package de.wps.radvis.backend.benutzer.domain.repository;

import static de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider.benutzerDBListViewComparator;
import static de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider.getDbListView;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerDBListView;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group4")
@ContextConfiguration(classes = {
	BenutzerConfiguration.class,
	OrganisationConfiguration.class
})
@MockBeans({
	@MockBean(GeoConverterConfiguration.class),
	@MockBean(VerwaltungseinheitImportRepository.class)
})
@EnableConfigurationProperties(value = {
	TechnischerBenutzerConfigurationProperties.class
})
class BenutzerRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Test
	public void testFindAllDBListViews() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		Benutzer admin = benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build());
		Benutzer kreiskoordinator = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(gebietskoerperschaft).build());

		// act
		List<BenutzerDBListView> dbListViews = benutzerRepository.findAllDBListViews();

		// assert
		assertThat(dbListViews)
			.usingElementComparator(benutzerDBListViewComparator)
			.containsExactlyInAnyOrder(getDbListView(admin), getDbListView(kreiskoordinator));
	}

	@Test
	public void testFindAllDBListViewsInVerwaltungseinheitWithId() {
		// arrange
		Gebietskoerperschaft obereGebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.build());

		Gebietskoerperschaft mittlereGebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.uebergeordneteOrganisation(obereGebietskoerperschaft)
				.build());

		Benutzer adminAufObersterStufe = benutzerRepository.save(
			BenutzerTestDataProvider.admin(obereGebietskoerperschaft).build());
		Benutzer benutzerAuObersterStufe = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(obereGebietskoerperschaft).build());
		Benutzer kreiskoordinatorAufMittlererStufe = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(mittlereGebietskoerperschaft).build());

		// act
		List<BenutzerDBListView> dbListViewsObereGK = benutzerRepository.findAllDBListViewsInVerwaltungseinheitWithId(
			obereGebietskoerperschaft.getId());

		List<BenutzerDBListView> dbListViewsMittlereGK = benutzerRepository.findAllDBListViewsInVerwaltungseinheitWithId(
			mittlereGebietskoerperschaft.getId());

		// assert
		assertThat(dbListViewsObereGK)
			.usingElementComparator(benutzerDBListViewComparator)
			.containsExactlyInAnyOrder(getDbListView(adminAufObersterStufe),
				getDbListView(benutzerAuObersterStufe));

		// assert
		assertThat(dbListViewsMittlereGK)
			.usingElementComparator(benutzerDBListViewComparator)
			.containsExactlyInAnyOrder(getDbListView(kreiskoordinatorAufMittlererStufe));
	}

}