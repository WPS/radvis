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
import { Injectable } from '@angular/core';
import { predefinedWeitereKartenebenenBaseZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { PredefinedKartenEbene } from '../models/predefined-karten-ebene';
import { PredefinedKartenMenu } from '../models/predefined-karten-menu';
import { VordefinierterLayer } from '../models/vordefinierter-layer';

@Injectable({ providedIn: 'root' })
export class VordefinierteLayerService {
  public predefinedKartenMenu: PredefinedKartenMenu[] = [];

  constructor(private http: HttpClient) {}

  public initPredefinedLayer(): Promise<void> {
    return this.http
      .get<VordefinierterLayer[]>('/api/weitere-kartenebenen/vordefiniert')
      .toPromise()
      .then(vordefinierteLayer => {
        const predefLayers: PredefinedKartenEbene[] = vordefinierteLayer.map((view, index) => {
          return {
            path: view.path ?? [],
            command: {
              deckkraft: view.deckkraft,
              dateiLayerId: null,
              id: null,
              name: view.name,
              quellangabe: view.quelle === 'RADVIS' ? `${window.location.origin}${view.quellangabe}` : view.quellangabe,
              url: view.quelle === 'RADVIS' ? `${window.location.origin}${view.url}` : view.url,
              weitereKartenebeneTyp: view.typ,
              zindex: predefinedWeitereKartenebenenBaseZIndex + index,
              zoomstufe: view.zoomstufe,
              farbe: view.farbe,
            },
          };
        });
        this.predefinedKartenMenu = this.createMenu(predefLayers);
      });
  }

  public createMenu(predefinedKartenEbenen: PredefinedKartenEbene[]): PredefinedKartenMenu[] {
    const result: PredefinedKartenMenu[] = [];

    const addToMenu = (predefLayer: PredefinedKartenEbene, menu: PredefinedKartenMenu[]): void => {
      if (predefLayer.path.length === 0) {
        menu.push(new PredefinedKartenMenu(predefLayer.command, []));
      } else {
        let existingMenu = menu.find(m => m.hasSubMenu() && m.item === predefLayer.path[0]);
        if (!existingMenu) {
          existingMenu = new PredefinedKartenMenu(predefLayer.path[0], []);
          menu.push(existingMenu);
        }
        predefLayer.path.splice(0, 1);

        addToMenu(predefLayer, existingMenu.children);
      }
    };

    predefinedKartenEbenen.forEach(predefLayer => {
      addToMenu(predefLayer, result);
    });

    return result;
  }
}
