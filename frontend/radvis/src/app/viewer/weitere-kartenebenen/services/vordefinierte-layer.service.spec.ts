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
import { predefinedWeitereKartenebenenBaseZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { PredefinedKartenMenu } from 'src/app/viewer/weitere-kartenebenen/models/predefined-karten-menu';
import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene-typ';
import { instance, mock } from 'ts-mockito';
import { PredefinedKartenEbene } from '../models/predefined-karten-ebene';
import { VordefinierteLayerService } from './vordefinierte-layer.service';

describe(VordefinierteLayerService.name, () => {
  let service: VordefinierteLayerService;

  beforeEach(() => {
    service = new VordefinierteLayerService(instance(mock(HttpClient)));
  });

  describe('createMenu', () => {
    it('should work', () => {
      const testData: PredefinedKartenEbene[] = [
        {
          command: {
            name: 'Bevölkerungszahlen',
            url: 'https://www.wms.nrw.de/wms/zensusatlas?LAYERS=bevoelkerungszahl',
            weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
            farbe: undefined,
            deckkraft: 0.7,
            zoomstufe: 8.7,
            zindex: predefinedWeitereKartenebenenBaseZIndex + 1,
            id: null,
            quellangabe: 'https://www.wms.nrw.de/wms/zensusatlas?REQUEST=GetCapabilities',
            dateiLayerId: null,
            defaultLayer: false,
          },
          path: [],
        },
        {
          command: {
            name: 'Bevölkerungszahlen2',
            url: 'https://www.wms.nrw.de/wms/zensusatlas?LAYERS=bevoelkerungszahl',
            weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
            farbe: undefined,
            deckkraft: 0.7,
            zoomstufe: 8.7,
            zindex: predefinedWeitereKartenebenenBaseZIndex + 1,
            id: null,
            quellangabe: 'https://www.wms.nrw.de/wms/zensusatlas?REQUEST=GetCapabilities',
            dateiLayerId: null,
            defaultLayer: false,
          },
          path: [],
        },
        {
          command: {
            name: 'TT-SIB Straßenachsen mit Straßenklassen',
            url: window.location.origin + '/api/geoserver/saml/radvis/wms?layers=radvis%3Att_sib_mittelstreifen',
            weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
            farbe: undefined,
            deckkraft: 1.0,
            zoomstufe: 8.7,
            zindex: predefinedWeitereKartenebenenBaseZIndex + 3,
            id: null,
            quellangabe: window.location.origin + '/api/geoserver/saml/radvis/wms?REQUEST=GetCapabilities',
            dateiLayerId: null,
            defaultLayer: false,
          },
          path: ['TT-SIB'],
        },
        {
          command: {
            name: 'TT-SIB Straßenachsen mit Straßenklassen2',
            url: window.location.origin + '/api/geoserver/saml/radvis/wms?layers=radvis%3Att_sib_mittelstreifen',
            weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
            farbe: undefined,
            deckkraft: 1.0,
            zoomstufe: 8.7,
            zindex: predefinedWeitereKartenebenenBaseZIndex + 3,
            id: null,
            quellangabe: window.location.origin + '/api/geoserver/saml/radvis/wms?REQUEST=GetCapabilities',
            dateiLayerId: null,
            defaultLayer: false,
          },
          path: ['TT-SIB'],
        },
        {
          command: {
            name: 'Arbeitsstelle(Linie)',
            url: `https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2externwfs/wfs?typeNames=bis2externwfs:ArbeitsstelleLinie`,
            weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
            farbe: '#1c5b9a',
            deckkraft: 1.0,
            zoomstufe: 13.5,
            zindex: predefinedWeitereKartenebenenBaseZIndex + 12,
            id: null,
            quellangabe:
              'https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2externwfs/wfs?REQUEST=GetCapabilities',
            dateiLayerId: null,
            defaultLayer: false,
          },
          path: ['Arbeitsstellen BEMaS', 'WFS'],
        },
        {
          command: {
            name: 'Arbeitsstelle(Linie)2',
            url: `https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2externwfs/wfs?typeNames=bis2externwfs:ArbeitsstelleLinie`,
            weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
            farbe: '#1c5b9a',
            deckkraft: 1.0,
            zoomstufe: 13.5,
            zindex: predefinedWeitereKartenebenenBaseZIndex + 12,
            id: null,
            quellangabe:
              'https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2externwfs/wfs?REQUEST=GetCapabilities',
            dateiLayerId: null,
            defaultLayer: false,
          },
          path: ['Arbeitsstellen BEMaS', 'WFS'],
        },
        {
          command: {
            name: 'Arbeitsstelle(Linie)3',
            url: `https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2externwfs/wfs?typeNames=bis2externwfs:ArbeitsstelleLinie`,
            weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
            farbe: '#1c5b9a',
            deckkraft: 1.0,
            zoomstufe: 13.5,
            zindex: predefinedWeitereKartenebenenBaseZIndex + 12,
            id: null,
            quellangabe:
              'https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2externwfs/wfs?REQUEST=GetCapabilities',
            dateiLayerId: null,
            defaultLayer: false,
          },
          path: ['Arbeitsstellen BEMaS', 'WMS'],
        },
      ];

      const menu = service.createMenu(testData.slice().map(t => ({ command: t.command, path: t.path.slice() })));

      expect(menu.length).toBe(4);
      expect(menu[0].item).toEqual(testData[0].command);
      expect(menu[1].item).toEqual(testData[1].command);
      expect(menu[2].item).toEqual(testData[2].path[0]);
      expect(menu[3].item).toEqual(testData[4].path[0]);

      expect(menu[0].children).toEqual([]);
      expect(menu[1].children).toEqual([]);
      expect(menu[2].children).toEqual([
        new PredefinedKartenMenu(testData[2].command, []),
        new PredefinedKartenMenu(testData[3].command, []),
      ]);
      expect(menu[3].children.map(c => c.item)).toEqual([testData[4].path[1], testData[6].path[1]]);

      expect(menu[3].children[0].children).toEqual([
        new PredefinedKartenMenu(testData[4].command, []),
        new PredefinedKartenMenu(testData[5].command, []),
      ]);

      expect(menu[3].children[1].children).toEqual([new PredefinedKartenMenu(testData[6].command, [])]);
    });
  });
});
