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

import { HttpClient } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene-typ';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { deepEqual, instance, mock, when } from 'ts-mockito';

describe(WeitereKartenebenenService.name, () => {
  let service: WeitereKartenebenenService;
  let httpClientMock: HttpClient;

  beforeEach(() => {
    httpClientMock = mock(HttpClient);

    service = new WeitereKartenebenenService(instance(httpClientMock));
  });

  describe('with weitere kartenebenen', () => {
    const kartenebenen = [
      {
        id: 1,
        name: 'Nameloser Layer',
        url: 'https://whatever.com',
        farbe: 'grau-ish',
        deckkraft: 123,
        zoomstufe: 234,
        quellangabe: 'Qualle',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
      },
      {
        id: 2,
        name: 'Namenhafter Layer',
        url: 'https://namenhafter-layer.com',
        farbe: 'bunt',
        deckkraft: 1,
        zoomstufe: 2,
        quellangabe: 'IR-GE-ND-WAS 2.0',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      },
    ] as WeitereKartenebene[];
    let emittedKartenebenen: WeitereKartenebene[];
    let emittedSelektierteKartenebenen: WeitereKartenebene[];

    beforeEach(() => {
      service.weitereKartenebenen$.subscribe(ebenen => {
        emittedKartenebenen?.push(...ebenen);
      });
      service.selectedWeitereKartenebenen$.subscribe(selektierteEbenen => {
        emittedSelektierteKartenebenen?.push(...selektierteEbenen);
      });
      emittedKartenebenen = [];
      emittedSelektierteKartenebenen = [];
      when(httpClientMock.get(deepEqual(`${WeitereKartenebenenService.API}/list`))).thenReturn(of(kartenebenen));
    });

    it('should load Kartenebenen and  when initialized', fakeAsync(() => {
      service.initWeitereKartenebenen();
      tick();
      service.toggleLayerSelection(kartenebenen[1]);
      tick();

      expect(emittedKartenebenen).toEqual(kartenebenen);
      expect(emittedSelektierteKartenebenen).toEqual([kartenebenen[1]]);
    }));

    it('should not fire selection with deleted layer', fakeAsync(() => {
      // Arrange
      service.initWeitereKartenebenen();
      tick();
      service.toggleLayerSelection(kartenebenen[1]);
      tick();

      // Reset weil in diesem Test ein zweites mal initialisiert wird
      emittedKartenebenen = [];
      emittedSelektierteKartenebenen = [];

      // Szenario: Ein Layer wird vom Nutzer im Datei-Layer Abschnitt entfernt
      const newKartenebenen = [kartenebenen[0]];
      when(httpClientMock.get(deepEqual(`${WeitereKartenebenenService.API}/list`))).thenReturn(of(newKartenebenen));

      // Act
      service.initWeitereKartenebenen();
      tick();

      // Assert
      expect(emittedKartenebenen).toEqual(newKartenebenen);
      expect(emittedSelektierteKartenebenen).toEqual([]);
    }));
  });
});
