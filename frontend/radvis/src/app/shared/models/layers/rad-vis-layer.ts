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

import Style, { StyleFunction } from 'ol/style/Style';
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';

export type LayerId = string;

export type Color = [number, number, number, number];
/**
 * @deprecated Struktur zum generischen Erzeugen von Layern, wird nicht mehr gebraucht
 */
export class RadVisLayer {
  constructor(
    private _id: LayerId,
    private _bezeichnung: string,
    private _url: string,
    private _typ: RadVisLayerTyp,
    private _style: Style | StyleFunction | undefined,
    private _baseColor: Color | undefined,
    private _layerType: LayerTypes,
    private _minZoom?: number,
    private _maxZoom?: number,
    private _origin?: [number, number],
    private _quelle?: string
  ) {
    if (_layerType !== LayerTypes.HINTERGRUND) {
      if (_style === undefined) {
        throw new Error('Style muss für alle Layer außer Hintergrund gesetzt sein.');
      }
      if (_baseColor === undefined) {
        throw new Error('Color muss für alle Layer außer Hintergrund gesetzt sein.');
      }
    }
  }

  get bezeichnung(): string {
    return this._bezeichnung;
  }

  get layerType(): LayerTypes {
    return this._layerType;
  }

  get id(): LayerId {
    return this._id;
  }

  get url(): string {
    return this._url;
  }

  set url(url: string) {
    this._url = url;
  }

  get typ(): RadVisLayerTyp {
    return this._typ;
  }

  get style(): Style | StyleFunction | undefined {
    return this._style;
  }

  set style(style: Style | StyleFunction | undefined) {
    this._style = style;
  }

  get minZoom(): number | undefined {
    return this._minZoom;
  }

  get maxZoom(): number | undefined {
    return this._maxZoom;
  }

  get baseColor(): Color | undefined {
    return this._baseColor;
  }

  get origin(): [number, number] | undefined {
    return this._origin;
  }

  get quelle(): string | undefined {
    return this._quelle;
  }
}
