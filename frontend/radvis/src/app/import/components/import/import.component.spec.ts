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

import { ChangeDetectorRef } from '@angular/core';
import {
  ActivatedRoute,
  ActivatedRouteSnapshot,
  ChildActivationEnd,
  Event,
  NavigationEnd,
  NavigationStart,
  Router,
} from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { Subject } from 'rxjs';
import { ImportComponent } from 'src/app/import/components/import/import.component';
import { ImportModule } from 'src/app/import/import.module';
import { ImportStep } from 'src/app/import/models/import-step';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';
import { instance, mock, when } from 'ts-mockito';

describe(ImportComponent.name, () => {
  let fixture: MockedComponentFixture<ImportComponent>;
  let component: ImportComponent;

  let router: Router;
  let activatedRoute: ActivatedRoute;
  let activatedRouteSnapshot: ActivatedRouteSnapshot;

  let routerEvents: Subject<Event>;

  const steps: Map<number, ImportStep> = new Map([
    [
      1,
      {
        bezeichnung: 'Test Datei hochladen',
        path: 'upload-test',
      },
    ],
    [
      2,
      {
        bezeichnung: 'Test Parameter Eingeben',
        path: 'parameter-eingeben-test',
      },
    ],
    [
      3,
      {
        bezeichnung: 'Test Abbildung',
        path: 'abbildung-test',
      },
    ],
    [
      4,
      {
        bezeichnung: 'Test Abbildung bearbeiten',
        path: 'korrektur-test',
      },
    ],
    [
      5,
      {
        bezeichnung: 'Test Import abschließen',
        path: 'abschluss-test',
      },
    ],
  ]);

  beforeEach(() => {
    router = mock(Router);

    activatedRoute = mock(ActivatedRoute);
    activatedRouteSnapshot = mock(ActivatedRouteSnapshot);
    const activatedChildRouteSnapshot = mock(ActivatedRouteSnapshot);
    when(activatedChildRouteSnapshot.data).thenReturn({ steps: steps });
    when(activatedRouteSnapshot.firstChild).thenReturn(instance(activatedChildRouteSnapshot));

    routerEvents = new Subject();
    when(router.events).thenReturn(routerEvents);

    return MockBuilder(ImportComponent, ImportModule)
      .provide({ provide: Router, useValue: instance(router) })
      .provide({ provide: ActivatedRoute, useValue: instance(activatedRoute) })
      .provide({ provide: ChangeDetectorRef, useValue: instance(mock(ChangeDetectorRef)) })
      .provide({ provide: FeatureTogglzService, useValue: instance(mock(FeatureTogglzService)) })
      .provide({ provide: LadeZustandService, useValue: instance(mock(LadeZustandService)) });
  });

  beforeEach(() => {
    fixture = MockRender(ImportComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe('with routerEvents', () => {
    beforeEach(() => {
      component.activeStepIndex = -1;
    });

    [
      { url: steps.get(1)!.path, stepIndex: 1 },
      { url: steps.get(2)!.path, stepIndex: 2 },
      { url: steps.get(3)!.path, stepIndex: 3 },
      { url: steps.get(4)!.path, stepIndex: 4 },
      { url: steps.get(5)!.path, stepIndex: 5 },
    ].forEach(({ url, stepIndex }) => {
      describe(`with NavigationEnd event and url ${url}`, () => {
        beforeEach(() => {
          when(router.url).thenReturn(url);
          routerEvents.next(new ChildActivationEnd(instance(activatedRouteSnapshot)));
          routerEvents.next(new NavigationEnd(1, 'egal', ''));
        });

        it(`should set activeStepIndex: ${stepIndex}`, () => {
          expect(component.activeStepIndex).toEqual(stepIndex);
        });
      });
    });

    describe('with invalid event', () => {
      beforeEach(() => {
        when(router.url).thenReturn('abschluss-fehler');
        routerEvents.next(new NavigationStart(1, 'egal'));
      });

      it('should not set activeStepIndex', () => {
        expect(component.activeStepIndex).toEqual(-1);
      });
    });
  });
});
