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

import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { AdministrationModule } from 'src/app/administration/administration.module';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { instance, mock, when } from 'ts-mockito';
import { AdministrationMenuComponent } from './administration-menu.component';

describe('AdministrationMenuComponent', () => {
  let component: AdministrationMenuComponent;
  let benutzerDetailsService: BenutzerDetailsService;
  let featureTogglzService: FeatureTogglzService;

  let fixture: MockedComponentFixture<AdministrationMenuComponent>;

  beforeEach(() => {
    benutzerDetailsService = mock(BenutzerDetailsService);
    featureTogglzService = mock(FeatureTogglzService);

    return MockBuilder(AdministrationMenuComponent, AdministrationModule)
      .provide({
        provide: BenutzerDetailsService,
        useValue: instance(benutzerDetailsService),
      })
      .provide({
        provide: FeatureTogglzService,
        useValue: instance(featureTogglzService),
      });
  });

  it('should create', () => {
    fixture = MockRender(AdministrationMenuComponent);
    component = fixture.point.componentInstance;
    expect(component).toBeTruthy();
  });

  describe('showBenutzer', () => {
    it('should be false', () => {
      when(benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter()).thenReturn(false);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showBenutzer).toBe(false);
    });

    it('should be true', () => {
      when(benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter()).thenReturn(true);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showBenutzer).toBe(true);
    });
  });

  describe('showDateiLayer', () => {
    beforeEach(() => {
      when(featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_DATEILAYER_HOCHLADEN_ANZEIGEN)).thenReturn(
        true
      );
    });

    it('should be false', () => {
      when(benutzerDetailsService.canDateiLayerVerwalten()).thenReturn(false);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showDateiLayer).toBe(false);
    });

    it('should be true', () => {
      when(benutzerDetailsService.canDateiLayerVerwalten()).thenReturn(true);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showDateiLayer).toBe(true);
    });

    it('should be false if toggled off', () => {
      when(featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_DATEILAYER_HOCHLADEN_ANZEIGEN)).thenReturn(
        false
      );
      when(benutzerDetailsService.canDateiLayerVerwalten()).thenReturn(true);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showDateiLayer).toBe(false);
    });
  });

  describe('showOrganisation', () => {
    beforeEach(() => {
      when(
        featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_ORGANISATIONEN_ERSTELLEN_UND_BEARBEITEN)
      ).thenReturn(true);
    });

    it('should be false', () => {
      when(benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter()).thenReturn(false);
      when(benutzerDetailsService.canEditZustaendigkeitsBereichOfOrganisation()).thenReturn(false);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showOrganisation).toBe(false);
    });

    it('should be true if BenutzerAndOrgaVerwalter', () => {
      when(benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter()).thenReturn(true);
      when(benutzerDetailsService.canEditZustaendigkeitsBereichOfOrganisation()).thenReturn(false);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showOrganisation).toBe(true);
    });

    it('should be true if canEditZustaendigkeitsBereich', () => {
      when(benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter()).thenReturn(false);
      when(benutzerDetailsService.canEditZustaendigkeitsBereichOfOrganisation()).thenReturn(true);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showOrganisation).toBe(true);
    });

    it('should be false if toggl off', () => {
      when(
        featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_ORGANISATIONEN_ERSTELLEN_UND_BEARBEITEN)
      ).thenReturn(false);
      when(benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter()).thenReturn(true);
      when(benutzerDetailsService.canEditZustaendigkeitsBereichOfOrganisation()).thenReturn(true);
      fixture = MockRender(AdministrationMenuComponent);
      expect(fixture.point.componentInstance.showOrganisation).toBe(false);
    });
  });

  describe('showMenu', () => {
    beforeEach(() => {
      fixture = MockRender(AdministrationMenuComponent);
      component = fixture.point.componentInstance;
    });

    const test = (
      showBenutzer: boolean,
      showOrganisation: boolean,
      showDateiLayer: boolean,
      expectedResult: boolean
    ): void => {
      it(`should be ${expectedResult}, if ${[showBenutzer, showOrganisation, showDateiLayer]}`, () => {
        component.showBenutzer = showBenutzer;
        component.showOrganisation = showOrganisation;
        component.showDateiLayer = showDateiLayer;

        expect(component.showMenu).toBe(expectedResult);
      });
    };

    test(true, true, true, true);
    test(true, true, false, true);
    test(true, false, true, true);
    test(false, true, true, true);
    test(true, false, false, false);
    test(false, true, false, false);
    test(false, false, true, false);
    test(false, false, false, false);
  });
});
