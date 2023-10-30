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

import { Params } from '@angular/router';
import { Extent } from 'ol/extent';
import { AbstractQueryParams } from 'src/app/shared/models/abstract-query-params';
import { LayerId } from 'src/app/shared/models/layers/rad-vis-layer';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';

export class MapQueryParams extends AbstractQueryParams {
  get layers(): LayerId[] {
    return this._layers;
  }

  get view(): Extent | null {
    return this._view;
  }

  get netzklassen(): Netzklassefilter[] {
    return this._netzklassen;
  }

  get signatur(): Signatur | null {
    return this._signatur;
  }

  get mitVerlauf(): boolean | null {
    return this._mitVerlauf;
  }

  get hintergrund(): LayerId | null {
    return this._hintergrund;
  }

  constructor(
    private _layers: LayerId[],
    private _netzklassen: Netzklassefilter[],
    private _view: Extent | null,
    private _mitVerlauf: boolean | null,
    private _hintergrund: LayerId | null = null,
    private _signatur: Signatur | null = null
  ) {
    super();
  }

  public static fromRoute(params: Params): MapQueryParams {
    return new MapQueryParams(
      this.paramToLayers(params.layers),
      this.paramToNetzklassen(params.netzklassen),
      this.paramToExtent(params.view),
      this.paramToBoolean(params.mitVerlauf),
      params.hintergrund || null,
      this.paramToSignatur(params.signatur)
    );
  }

  public static paramToExtent(param: string | undefined): Extent | null {
    let view: Extent | null = this.paramToList(param).map(e => +e) as Extent;
    if (view.length !== 4) {
      view = null;
    }
    return view;
  }

  public static paramToLayers(param: string | undefined): string[] {
    return this.paramToList(param);
  }

  public static paramToNetzklassen(param: string | undefined): Netzklassefilter[] {
    return this.paramToList(param).map(p => (Netzklassefilter as any)[p]);
  }

  public static paramToSignatur(param: string | undefined): Signatur | null {
    if (!param) {
      return null;
    }
    const paramList = this.paramToList(param);
    return { name: paramList[0], typ: paramList[1] as SignaturTyp };
  }

  public static merge(
    params:
      | {
          layers?: LayerId[];
          netzklassen?: Netzklassefilter[];
          view?: Extent;
          mitVerlauf?: boolean;
          hintergrund?: string;
          signatur?: Signatur;
        }
      | MapQueryParams,
    into: MapQueryParams
  ): MapQueryParams {
    return new MapQueryParams(
      params.layers || into.layers,
      params.netzklassen || into.netzklassen,
      params.view || into.view,
      this.mergeBooleans(params.mitVerlauf, into.mitVerlauf),
      params.hintergrund !== undefined && params.hintergrund !== null ? params.hintergrund : into.hintergrund,
      params.signatur !== undefined ? params.signatur : into.signatur
    );
  }

  public toRouteParams(): Params {
    return {
      layers: AbstractQueryParams.listToParam(this.layers),
      view: AbstractQueryParams.listToParam(this.view?.map(e => e.toString()) || null),
      netzklassen: AbstractQueryParams.listToParam(this.netzklassen.map(nk => nk.toString()) || null),
      hintergrund: this.hintergrund === null ? '' : this.hintergrund,
      signatur: this.signatur ? AbstractQueryParams.listToParam([this.signatur.name, this.signatur.typ]) : '',
      mitVerlauf: AbstractQueryParams.booleanToParam(this.mitVerlauf),
    };
  }
}
