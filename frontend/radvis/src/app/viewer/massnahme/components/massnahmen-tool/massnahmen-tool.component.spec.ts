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
import { fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Data } from '@angular/router';
import { Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { defaultMassnahmeToolView } from 'src/app/viewer/massnahme/models/massnahme-tool-view-test-data-provider';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';

describe(MassnahmenToolComponent.name, () => {
  let massnahmenToolComponent: MassnahmenToolComponent;

  let activatedRoute: ActivatedRoute;
  let viewerRoutingService: ViewerRoutingService;
  let massnahmenRoutingService: MassnahmenRoutingService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let massnahmeService: MassnahmeService;
  let notifyUserService: NotifyUserService;
  let massnahmeFilterService: MassnahmeFilterService;
  let dialog: MatDialog;
  let errorHandlingService: ErrorHandlingService;
  let olMapService: OlMapService;
  let fileHandlingService: FileHandlingService;

  let data: Subject<Data>;

  beforeEach(() => {
    massnahmenRoutingService = mock(MassnahmenRoutingService);
    viewerRoutingService = mock(ViewerRoutingService);
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    massnahmeService = mock(MassnahmeService);
    activatedRoute = mock(ActivatedRoute);
    notifyUserService = mock(NotifyUserService);
    massnahmeFilterService = mock(MassnahmeFilterService);
    dialog = mock(MatDialog);
    errorHandlingService = mock(ErrorHandlingService);
    olMapService = mock(OlMapComponent);
    fileHandlingService = mock(FileHandlingService);

    data = new Subject();
    when(activatedRoute.data).thenReturn(data.asObservable());

    const changeDetectorRef = {
      detectChanges: (): void => {},
    };
    massnahmenToolComponent = new MassnahmenToolComponent(
      instance(activatedRoute),
      instance(viewerRoutingService),
      instance(massnahmenRoutingService),
      instance(infrastrukturenSelektionService),
      instance(massnahmeService),
      instance(notifyUserService),
      instance(massnahmeFilterService),
      instance(dialog),
      instance(errorHandlingService),
      instance(fileHandlingService),
      changeDetectorRef as ChangeDetectorRef,
      instance(olMapService)
    );
  });

  beforeEach(waitForAsync(() => {
    when(massnahmenRoutingService.getIdFromRoute()).thenReturn(3);
    when(massnahmeService.getMassnahmeToolView(3)).thenResolve(defaultMassnahmeToolView);
    when(massnahmeService.getBenachrichtigungsFunktion(3)).thenResolve(true);
  }));

  describe('constructor', () => {
    it('should set MassnahmeToolView from route data and retrieve benachrichtigungsfunktion when another massnahme was selected', fakeAsync(() => {
      expect(massnahmenToolComponent.benachrichtigungAktiv).toBeNull();

      const massnahme = {
        id: 3,
        version: 1,
        canDelete: true,
        hasUmsetzungsstand: false,
        netzbezug: defaultNetzbezug,
        originalGeometrie: null,
      };
      data.next({
        massnahme,
      });

      tick();

      expect(massnahmenToolComponent.benachrichtigungAktiv).toBeTrue();
      expect(massnahmenToolComponent.massnahmeToolView).toEqual(massnahme);
    }));

    it('should retrieve MassnahmeToolView when massnahme modified', fakeAsync(() => {
      expect(massnahmenToolComponent.massnahmeToolView).toBeNull();
      massnahmenToolComponent.updateMassnahme();
      tick();
      expect(massnahmenToolComponent.massnahmeToolView).toEqual(defaultMassnahmeToolView);
    }));
  });

  describe('onChangeBenachrichtigung', () => {
    it('should make correct Api call and notify user', fakeAsync(() => {
      when(massnahmeService.stelleBenachrichtigungsFunktionEin(3, anything())).thenCall((id, aktiv) =>
        Promise.resolve(aktiv)
      );
      massnahmenToolComponent.onChangeBenachrichtigung(true);

      verify(massnahmeService.stelleBenachrichtigungsFunktionEin(3, true)).once();
      tick();
      verify(notifyUserService.inform(anything())).once();
      expect(capture(notifyUserService.inform).first()[0]).toContain(' aktiviert');

      massnahmenToolComponent.onChangeBenachrichtigung(false);

      verify(massnahmeService.stelleBenachrichtigungsFunktionEin(3, false)).once();
      tick();
      verify(notifyUserService.inform(anything())).twice();
      expect(capture(notifyUserService.inform).second()[0]).toContain(' deaktiviert');
    }));
  });
});
