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

import { ServicestationToolComponent } from './servicestation-tool.component';
import { ActivatedRoute, Data } from '@angular/router';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { MatDialog } from '@angular/material/dialog';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { Subject } from 'rxjs';
import { instance, mock, when } from 'ts-mockito';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { ChangeDetectorRef } from '@angular/core';
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { Servicestation } from 'src/app/viewer/servicestation/models/servicestation';
import { defaultServicestation } from 'src/app/viewer/servicestation/models/servicestation-testdata-provider.spec';
import { ServicestationFilterService } from 'src/app/viewer/servicestation/services/servicestation-filter.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';

describe('ServicestationToolComponent', () => {
  let servicestationToolComponent: ServicestationToolComponent;

  let activatedRoute: ActivatedRoute;
  let viewerRoutingService: ViewerRoutingService;
  let servicestationRoutingService: ServicestationRoutingService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let servicestationService: ServicestationService;
  let servicestationFilterService: ServicestationFilterService;
  let dialog: MatDialog;
  let olMapService: OlMapService;
  let errorHandlingService: ErrorHandlingService;

  let data: Subject<Data>;

  beforeEach(() => {
    servicestationRoutingService = mock(ServicestationRoutingService);
    viewerRoutingService = mock(ViewerRoutingService);
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    servicestationService = mock(ServicestationService);
    servicestationFilterService = mock(ServicestationFilterService);
    activatedRoute = mock(ActivatedRoute);
    dialog = mock(MatDialog);
    olMapService = mock(OlMapComponent);
    errorHandlingService = mock(ErrorHandlingService);

    data = new Subject();
    when(activatedRoute.data).thenReturn(data.asObservable());

    const changeDetectorRef = {
      detectChanges: (): void => {},
    };
    servicestationToolComponent = new ServicestationToolComponent(
      instance(activatedRoute),
      instance(viewerRoutingService),
      instance(infrastrukturenSelektionService),
      instance(servicestationService),
      instance(servicestationFilterService),
      instance(dialog),
      changeDetectorRef as ChangeDetectorRef,
      instance(olMapService),
      instance(errorHandlingService)
    );
  });

  beforeEach(waitForAsync(() => {
    when(servicestationRoutingService.getIdFromRoute()).thenReturn(3);
  }));

  describe('constructor', () => {
    it('should set ServicestationToolView from route data and retrieve benachrichtigungsfunktion when another servicestation was selected', fakeAsync(() => {
      const servicestation: Servicestation = { ...defaultServicestation, id: 3 };
      data.next({
        servicestation,
      });

      tick();

      expect(servicestationToolComponent.servicestation).toEqual(servicestation);
    }));

    it('should get Servicestation on update', fakeAsync(() => {
      const servicestation = { ...defaultServicestation, oeffnungszeiten: 'Grundsätzlich immer geöffnet ... IMMER!' };
      data.next({
        servicestation: defaultServicestation,
      });
      when(servicestationService.get(defaultServicestation.id)).thenReturn(Promise.resolve(servicestation));

      servicestationToolComponent.updateServicestation();
      tick();

      expect(servicestationToolComponent.servicestation).toEqual(servicestation);
    }));
  });
});
