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

import { HttpClient, HttpResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { BenutzerName } from 'src/app/shared/models/benutzer-name';
import { CreateDateiLayerCommand } from 'src/app/viewer/weitere-kartenebenen/models/create-datei-layer-command';
import { DateiLayer } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer';
import { DateiLayerFormat } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer-format';
import { DateiLayerService } from 'src/app/viewer/weitere-kartenebenen/services/datei-layer.service';
import { anything, deepEqual, instance, mock, when } from 'ts-mockito';

describe(DateiLayerService.name, () => {
  let service: DateiLayerService;
  let httpClientMock: HttpClient;

  const testDateiLayers: DateiLayer[] = [
    {
      id: 1,
      name: 'name',
      quellangabe: 'quellangabe',
      erstelltAm: '01.01.1001',
      format: DateiLayerFormat.SHAPE,
      benutzer: {
        vorname: 'vorname',
        nachname: 'nachname',
      } as BenutzerName,
    } as DateiLayer,
  ];

  beforeEach(() => {
    httpClientMock = mock(HttpClient);
    when(httpClientMock.get(deepEqual(`${DateiLayerService.API}/list`))).thenReturn(of(testDateiLayers));
    when(httpClientMock.post(anything(), anything())).thenReturn(of(new HttpResponse({ status: 200 })));

    service = new DateiLayerService(instance(httpClientMock));
  });

  it('should trigger allDateiLayers$ after refreshDateiLayers is called', fakeAsync(() => {
    service.refreshDateiLayers();
    tick();
    service.allDateiLayers$.subscribe(dateiLayers => {
      expect(dateiLayers).toEqual(testDateiLayers);
    });
    tick();
  }));

  it('should trigger allDateiLayers$ after create is called', fakeAsync(() => {
    service.create({} as CreateDateiLayerCommand, instance(mock(File)));
    tick();

    service.allDateiLayers$.subscribe(dateiLayers => {
      expect(dateiLayers).toEqual(testDateiLayers);
    });
    tick();
  }));
});
