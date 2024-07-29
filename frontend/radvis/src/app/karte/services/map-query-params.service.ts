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

import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Extent } from 'ol/extent';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { AbstractQueryParams } from 'src/app/shared/models/abstract-query-params';
import { LayerId } from 'src/app/shared/models/layers/rad-vis-layer';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { Signatur } from 'src/app/shared/models/signatur';
import { AbstractQueryParamsService } from 'src/app/shared/services/abstract-query-params.service';

@Injectable({
  providedIn: 'root',
})
export class MapQueryParamsService extends AbstractQueryParamsService<MapQueryParams> {
  mapQueryParams$: Observable<MapQueryParams>;
  layers$: Observable<LayerId[]>;
  view$: Observable<Extent | null>;
  netzklassen$: Observable<Netzklassefilter[]>;
  hintergrund$: Observable<LayerId>;
  signatur$: Observable<Signatur | null>;
  mitVerlauf$: Observable<boolean>;

  constructor(
    private activRoute: ActivatedRoute,
    router: Router
  ) {
    super(router);
    this.mapQueryParams$ = this.activRoute.queryParams.pipe(
      map(params => {
        return MapQueryParams.fromRoute(params);
      })
    );
    this.layers$ = this.activRoute.queryParams.pipe(
      map(params => params.layers),
      distinctUntilChanged(),
      map((layers: string) => {
        return MapQueryParams.paramToLayers(layers);
      })
    );
    this.view$ = this.activRoute.queryParams.pipe(
      map(params => params.view),
      distinctUntilChanged(),
      map((view: string) => {
        return MapQueryParams.paramToExtent(view);
      })
    );
    this.netzklassen$ = this.activRoute.queryParams.pipe(
      map(params => params.netzklassen),
      distinctUntilChanged(),
      map((netzklassen: string) => {
        return MapQueryParams.paramToNetzklassen(netzklassen);
      })
    );
    this.hintergrund$ = this.activRoute.queryParams.pipe(
      map(params => params.hintergrund),
      distinctUntilChanged(),
      map((hintergrundLayer: string) => {
        return MapQueryParams.paramToLayers(hintergrundLayer)[0];
      })
    );
    this.signatur$ = this.activRoute.queryParams.pipe(
      map(params => params.signatur),
      distinctUntilChanged(),
      map(signatur => MapQueryParams.paramToSignatur(signatur) || null)
    );
    this.mitVerlauf$ = this.activRoute.queryParams.pipe(
      map(params => params.mitVerlauf),
      distinctUntilChanged(),
      map(param => AbstractQueryParams.paramToBoolean(param) ?? false)
    );
  }

  public get mapQueryParamsSnapshot(): MapQueryParams {
    return MapQueryParams.fromRoute(this.activRoute.snapshot.queryParams);
  }

  public update(
    opt: {
      layers?: LayerId[];
      netzklassen?: Netzklassefilter[];
      view?: Extent;
      hintergrund?: string;
      mitVerlauf?: boolean;
      signatur?: Signatur;
    },
    merge = true
  ): void {
    const queryParams = merge
      ? MapQueryParams.merge(opt, this.mapQueryParamsSnapshot)
      : new MapQueryParams(
          opt.layers || [],
          opt.netzklassen || [],
          opt.view || null,
          opt.mitVerlauf || false,
          opt.hintergrund || null,
          opt.signatur || null
        );
    this.updateInUrl(queryParams);
  }
}
