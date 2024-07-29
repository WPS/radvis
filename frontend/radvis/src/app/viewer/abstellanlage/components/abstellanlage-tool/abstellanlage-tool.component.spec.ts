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

import { fakeAsync, tick, waitForAsync } from '@angular/core/testing';

import { AbstellanlageToolComponent } from 'src/app/viewer/abstellanlage/components/abstellanlage-tool/abstellanlage-tool.component';
import { ActivatedRoute, Data } from '@angular/router';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { MatDialog } from '@angular/material/dialog';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { Subject } from 'rxjs';
import { instance, mock, when } from 'ts-mockito';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { ChangeDetectorRef } from '@angular/core';
import { AbstellanlageService } from 'src/app/viewer/abstellanlage/services/abstellanlage.service';
import { defaultAbstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage-testdata-provider.spec';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { AbstellanlageFilterService } from 'src/app/viewer/abstellanlage/services/abstellanlage-filter.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';

describe(AbstellanlageToolComponent.name, () => {
  let abstellanlageToolComponent: AbstellanlageToolComponent;

  let activatedRoute: ActivatedRoute;
  let viewerRoutingService: ViewerRoutingService;
  let abstellanlageRoutingService: AbstellanlageRoutingService;
  let abstellanlageFilterService: AbstellanlageFilterService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let abstellanlageService: AbstellanlageService;
  let errorHandlingService: ErrorHandlingService;
  let dialog: MatDialog;
  let olMapService: OlMapService;

  let data: Subject<Data>;

  beforeEach(() => {
    abstellanlageRoutingService = mock(AbstellanlageRoutingService);
    viewerRoutingService = mock(ViewerRoutingService);
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    abstellanlageService = mock(AbstellanlageService);
    abstellanlageFilterService = mock(AbstellanlageFilterService);
    errorHandlingService = mock(ErrorHandlingService);
    activatedRoute = mock(ActivatedRoute);
    dialog = mock(MatDialog);
    olMapService = mock(OlMapComponent);

    data = new Subject();
    when(activatedRoute.data).thenReturn(data.asObservable());

    const changeDetectorRef = {
      detectChanges: (): void => {},
    };
    abstellanlageToolComponent = new AbstellanlageToolComponent(
      instance(activatedRoute),
      instance(viewerRoutingService),
      instance(infrastrukturenSelektionService),
      instance(abstellanlageService),
      instance(abstellanlageFilterService),
      instance(errorHandlingService),
      instance(dialog),
      changeDetectorRef as ChangeDetectorRef,
      instance(olMapService)
    );
  });

  beforeEach(waitForAsync(() => {
    when(abstellanlageRoutingService.getIdFromRoute()).thenReturn(3);
  }));

  describe('constructor', () => {
    it('should set AbstellanlageToolView from route data and retrieve benachrichtigungsfunktion when another abstellanlage was selected', fakeAsync(() => {
      const abstellanlage: Abstellanlage = { ...defaultAbstellanlage, id: 3 };
      data.next({
        abstellanlage,
      });

      tick();

      expect(abstellanlageToolComponent.abstellanlage).toEqual(abstellanlage);
    }));

    it('should get Abstellanlage on update', fakeAsync(() => {
      const abstellanlage = { ...defaultAbstellanlage, externeId: 'Sehr neue und geänderte externe ID' };
      data.next({
        abstellanlage: defaultAbstellanlage,
      });
      when(abstellanlageService.get(defaultAbstellanlage.id)).thenReturn(Promise.resolve(abstellanlage));

      abstellanlageToolComponent.updateAbstellanlage();
      tick();

      expect(abstellanlageToolComponent.abstellanlage).toEqual(abstellanlage);
    }));
  });
});
