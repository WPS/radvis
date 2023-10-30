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

/* eslint-disable @typescript-eslint/dot-notation */
import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, ActivatedRouteSnapshot, Router, RouterModule } from '@angular/router';
import { MockComponent } from 'ng-mocks';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { of, Subscription } from 'rxjs';
import { ImportAttributeAbschliessenComponent } from 'src/app/editor/manueller-import/components/import-attribute-abschliessen/import-attribute-abschliessen.component';
import { ImportAttributeAbschliessenTestAdapter } from 'src/app/editor/manueller-import/components/import-attribute-abschliessen/import-attribute-abschliessen.test-adapter.spec';
import { ImportAttributeKonflikteLayerComponent } from 'src/app/editor/manueller-import/components/import-attribute-konflikte-layer/import-attribute-konflikte-layer.component';
import { AttributeImportSessionView } from 'src/app/editor/manueller-import/models/attribute-import-session-view';
import { AutomatischerImportSchritt } from 'src/app/editor/manueller-import/models/automatischer-import-schritt';
import { ImportSessionStatus } from 'src/app/editor/manueller-import/models/import-session-status';
import { ImportLogEintrag, Severity } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { Property } from 'src/app/editor/manueller-import/models/property';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, instance, mock, when } from 'ts-mockito';
import { AttributeImportFormat } from 'src/app/editor/manueller-import/models/attribute-import-format';

describe('ImportAttributeAbschliessenComponent', () => {
  let component: ImportAttributeAbschliessenComponent;
  let fixture: ComponentFixture<ImportAttributeAbschliessenComponent>;
  let manuellerImportService: ManuellerImportService;
  let radVisNetzFeatureService: NetzausschnittService;
  let activatedRoute: ActivatedRoute;
  let adapter: ImportAttributeAbschliessenTestAdapter;
  let organisationenService;

  const defaultAttributeImportSessionView: AttributeImportSessionView = {
    typ: ImportTyp.ATTRIBUTE_UEBERNEHMEN,
    aktuellerImportSchritt: AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
    log: [],
    status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
    organisationsID: 1,
    attribute: ['attributx'],
    anzahlKantenMitUneindeutigerAttributzuordnung: 0,
    anzahlFeaturesOhneMatch: 0,
    attributeImportFormat: AttributeImportFormat.LUBW,
  };

  beforeEach(async () => {
    manuellerImportService = mock(ManuellerImportService);
    radVisNetzFeatureService = mock(NetzausschnittService);
    activatedRoute = mock(ActivatedRoute);
    organisationenService = mock(OrganisationenService);

    when(activatedRoute.snapshot).thenReturn(createDummySnapshotRoute());
    when(manuellerImportService.getImportSession()).thenReturn(of(defaultAttributeImportSessionView));
    when(radVisNetzFeatureService.getKantenFuerZustaendigkeitsbereich(anything())).thenResolve({
      type: 'FeatureCollection',
      features: [],
    } as GeoJSONFeatureCollection);
    when(organisationenService.getOrganisation(anything())).thenResolve(defaultOrganisation);

    await TestBed.configureTestingModule({
      declarations: [ImportAttributeAbschliessenComponent, MockComponent(ImportAttributeKonflikteLayerComponent)],
      imports: [MatIconModule, RouterModule, MatProgressSpinnerModule, MatFormFieldModule, MaterialDesignModule],
      providers: [
        {
          provide: ManuellerImportService,
          useValue: instance(manuellerImportService),
        },
        {
          provide: NetzausschnittService,
          useValue: instance(radVisNetzFeatureService),
        },
        {
          provide: ActivatedRoute,
          useValue: instance(activatedRoute),
        },
        {
          provide: Router,
          useValue: instance(mock(Router)),
        },
        {
          provide: NotifyUserService,
          useValue: instance(mock(NotifyUserService)),
        },
        {
          provide: OrganisationenService,
          useValue: instance(organisationenService),
        },
        {
          provide: FehlerprotokollService,
          useValue: instance(mock(FehlerprotokollService)),
        },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportAttributeAbschliessenComponent);
    component = fixture.componentInstance;
    component.netzFetching = false;
    fixture.detectChanges();
  });

  beforeEach(() => {
    adapter = new ImportAttributeAbschliessenTestAdapter(fixture.debugElement);
  });

  afterEach(() => {
    fixture.destroy();
  });

  describe('session update is done on startup', () => {
    const updateDoneAttributeImportSessionView: AttributeImportSessionView = {
      ...defaultAttributeImportSessionView,
      status: ImportSessionStatus.UPDATE_DONE,
      anzahlKantenMitUneindeutigerAttributzuordnung: 301,
      anzahlFeaturesOhneMatch: 300,
    };

    beforeEach(fakeAsync(() => {
      fixture.destroy();

      when(manuellerImportService.getImportSession()).thenReturn(of(updateDoneAttributeImportSessionView));

      fixture = TestBed.createComponent(ImportAttributeAbschliessenComponent);
      component = fixture.componentInstance;
      component.netzFetching = false;

      adapter = new ImportAttributeAbschliessenTestAdapter(fixture.debugElement);
      tick();
      discardPeriodicTasks();
    }));

    it('polling should not yet start', () => {
      expect(component['pollingSubscription']).toEqual(Subscription.EMPTY);
    });

    it('should have error statistics', () => {
      expect(component.session?.anzahlFeaturesOhneMatch).toBe(300);
      expect(component.session?.anzahlKantenMitUneindeutigerAttributzuordnung).toBe(301);
    });
  });

  describe('session update is executing on startup', () => {
    const updateExecutingAttributeImportSessionView: AttributeImportSessionView = {
      ...defaultAttributeImportSessionView,
      status: ImportSessionStatus.UPDATE_EXECUTING,
    };

    beforeEach(fakeAsync(() => {
      fixture.destroy();

      when(manuellerImportService.getImportSession()).thenReturn(of(updateExecutingAttributeImportSessionView));

      fixture = TestBed.createComponent(ImportAttributeAbschliessenComponent);
      component = fixture.componentInstance;
      component.netzFetching = false;

      adapter = new ImportAttributeAbschliessenTestAdapter(fixture.debugElement);
      tick();
      discardPeriodicTasks();
    }));

    it('polling should start', () => {
      expect(component['pollingSubscription']).not.toEqual(Subscription.EMPTY);
    });

    it('should have no error statistics', () => {
      expect(component.session?.anzahlFeaturesOhneMatch).toBeFalsy();
      expect(component.session?.anzahlKantenMitUneindeutigerAttributzuordnung).toBeFalsy();
    });
  });

  describe('error statistics', () => {
    it('should not be set if no update done', () => {
      expect(component.session?.anzahlFeaturesOhneMatch).toBeFalsy();
      expect(component.session?.anzahlKantenMitUneindeutigerAttributzuordnung).toBeFalsy();
    });
  });

  describe('fehler', () => {
    it('should not display fehler until result is loaded', () => {
      expect(adapter.doesFehlerExist()).toBeFalse();
    });

    it('should display fehler when done if there is one', () => {
      component.netzFetching = false;
      component.session = {
        ...defaultAttributeImportSessionView,
        status: ImportSessionStatus.UPDATE_DONE,
        log: [{ fehlerBeschreibung: 'dies ist ein Fehler', severity: Severity.ERROR }],
      };
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isFehlerShown('dies ist ein Fehler')).toBeTrue();
    });

    it('should not display fehler when done if there is none', () => {
      component.netzFetching = false;
      component.session = {
        ...defaultAttributeImportSessionView,
        status: ImportSessionStatus.UPDATE_DONE,
        log: [],
      };
      component['changeDetectorRef'].detectChanges();

      expect(adapter.doesFehlerExist()).toBeFalse();
    });
  });

  describe('status', () => {
    it('should not display speichern button until result is loaded', () => {
      expect(adapter.doesUebernehmenButtonExist()).toBeFalse();
      expect(adapter.doesBeendenButtonExist()).toBeFalse();

      component.netzFetching = false;
      component.session = defaultAttributeImportSessionView;
      component['changeDetectorRef'].detectChanges();

      expect(adapter.doesUebernehmenButtonExist()).toBeTrue();
      expect(adapter.isUebernehmenButtonDisabled()).toBeFalse();
      expect(adapter.doesBeendenButtonExist()).toBeFalse();
    });

    it('should display disabled fertig button when executing', () => {
      expect(adapter.doesUebernehmenButtonExist()).toBeFalse();
      expect(adapter.doesBeendenButtonExist()).toBeFalse();

      component.netzFetching = false;
      component.session = {
        ...defaultAttributeImportSessionView,
        status: ImportSessionStatus.UPDATE_EXECUTING,
      };
      component['changeDetectorRef'].detectChanges();

      expect(adapter.doesUebernehmenButtonExist()).toBeFalse();
      expect(adapter.isBeendenButtonDisabled()).toBeTrue();
    });

    it('should enable fertig button when done executing', () => {
      expect(adapter.doesUebernehmenButtonExist()).toBeFalse();
      expect(adapter.doesBeendenButtonExist()).toBeFalse();

      component.netzFetching = false;
      component.session = {
        ...defaultAttributeImportSessionView,
        status: ImportSessionStatus.UPDATE_DONE,
      };
      component['changeDetectorRef'].detectChanges();

      expect(adapter.doesUebernehmenButtonExist()).toBeFalse();
      expect(adapter.doesBeendenButtonExist()).toBeTrue();
      expect(adapter.isBeendenButtonDisabled()).toBeFalse();
    });
  });

  describe('konfliktDetailview', () => {
    it('should not display in template when no conflicts selected', () => {
      expect(adapter.doesKonflikteViewExists()).toBeFalse();
    });
    it('should display in template when conflicts selected', () => {
      component.onKonfliktkanteAusgewaehlt(createDummyKonflikte());
      component['changeDetectorRef'].detectChanges();
      expect(adapter.doesKonflikteViewExists()).toBeTrue();
    });
    it('should display selected conflicts', () => {
      component.onKonfliktkanteAusgewaehlt(createDummyKonflikte());
      component['changeDetectorRef'].detectChanges();

      expect(adapter.doesKonflikteViewExists()).toBeTrue();
      expect(adapter.doesRowExistInConflict(0, 'Betroffener Abschnitt', '10m bis 30,45m')).toBeTrue();
      expect(adapter.doesRowExistInConflict(1, 'Betroffener Abschnitt', '')).toBeFalse();
      expect(adapter.doesRowExistInConflict(0, 'Attributname', 'vereinbaru')).toBeTrue();
      expect(adapter.doesRowExistInConflict(1, 'Attributname', 'Belagart')).toBeTrue();
      expect(adapter.doesRowExistInConflict(0, 'Übernommener Wert', 'sowasvon vereinbart')).toBeTrue();
      expect(adapter.doesRowExistInConflict(1, 'Übernommener Wert', 'Zu viel Belag auf den Zähnen')).toBeTrue();
      expect(
        adapter.doesRowExistInConflict(0, 'Nicht Übernommene Werte', 'nicht so vereinbart, nicht so mega vereinbart')
      ).toBeTrue();
      expect(adapter.doesRowExistInConflict(1, 'Nicht Übernommene Werte', 'Besser Zähne putzen!')).toBeTrue();
    });
    it('should exit konfliktview on back button press', () => {
      component.onKonfliktkanteAusgewaehlt(createDummyKonflikte());
      component['changeDetectorRef'].detectChanges();
      expect(adapter.doesKonflikteViewExists()).toBeTrue();

      adapter.pressBackButton();

      component['changeDetectorRef'].detectChanges();

      expect(adapter.doesKonflikteViewExists()).toBeFalse();
    });
  });

  describe('handleSessionupdate', () => {
    it('should not trigger onChange when Updating', () => {
      const detectChangesSpy = spyOn(component['changeDetectorRef'], 'markForCheck');

      component['handleSessionupdate']({
        ...defaultAttributeImportSessionView,
        status: ImportSessionStatus.UPDATE_EXECUTING,
        anzahlFeaturesOhneMatch: 300,
        anzahlKantenMitUneindeutigerAttributzuordnung: 301,
      });

      expect(detectChangesSpy).not.toHaveBeenCalled();
    });

    it('should trigger onChange when Done', () => {
      const detectChangesSpy = spyOn(component['changeDetectorRef'], 'markForCheck');

      component['handleSessionupdate']({
        ...defaultAttributeImportSessionView,
        status: ImportSessionStatus.UPDATE_DONE,
        log: [
          {
            fehlerBeschreibung: 'Das ist ja gestern nicht so gut gelaufen. Aber lass dich davon nicht unterkriegen.',
            severity: Severity.ERROR,
          } as ImportLogEintrag,
        ],
      });

      expect(detectChangesSpy).toHaveBeenCalled();
    });
    it('should trigger onChange when returning to previous status', () => {
      component.session = {
        ...defaultAttributeImportSessionView,
        status: ImportSessionStatus.UPDATE_EXECUTING,
      };
      component['changeDetectorRef'].detectChanges();

      const detectChangesSpy = spyOn(component['changeDetectorRef'], 'markForCheck');
      const notifyUserSpy = spyOn(component['notifyUserService'], 'warn');
      component['handleSessionupdate']({
        ...defaultAttributeImportSessionView,
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });

      expect(detectChangesSpy).toHaveBeenCalled();
      expect(notifyUserSpy).toHaveBeenCalledWith(
        'Kanten in der Organisation wurden während des Speicherns aus einer anderen Quelle verändert. Bitte versuchen Sie es erneut.'
      );
    });
  });
});

const createDummySnapshotRoute = (): ActivatedRouteSnapshot => {
  return ({ data: { step: 1 } } as unknown) as ActivatedRouteSnapshot;
};

const createDummyKonflikte = (): Property[][] => {
  return [
    [
      { key: 'Betroffener Abschnitt', value: '10m bis 30,45m' } as Property,
      { key: 'Attributname', value: 'vereinbaru' } as Property,
      { key: 'Übernommener Wert', value: 'sowasvon vereinbart' } as Property,
      { key: 'Nicht Übernommene Werte', value: 'nicht so vereinbart, nicht so mega vereinbart' } as Property,
    ],
    [
      { key: 'Betroffener Abschnitt', value: '' } as Property,
      { key: 'Attributname', value: 'Belagart' } as Property,
      { key: 'Übernommener Wert', value: 'Zu viel Belag auf den Zähnen' } as Property,
      { key: 'Nicht Übernommene Werte', value: 'Besser Zähne putzen!' } as Property,
    ],
  ];
};
