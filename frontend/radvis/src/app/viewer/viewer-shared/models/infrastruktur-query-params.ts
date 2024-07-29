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
import { AbstractQueryParams } from 'src/app/shared/models/abstract-query-params';

export class InfrastrukturenQueryParams extends AbstractQueryParams {
  get infrastrukturen(): string[] {
    return this._infrastrukturen;
  }

  get tabellenVisible(): boolean | null {
    return this._tabellenVisible;
  }

  constructor(
    private _infrastrukturen: string[],
    private _tabellenVisible: boolean | null
  ) {
    super();
  }

  public static fromRoute(params: Params): InfrastrukturenQueryParams {
    return new InfrastrukturenQueryParams(
      this.paramToInfrastrukturen(params.infrastrukturen),
      this.paramToBoolean(params.tabellenVisible)
    );
  }

  public static paramToInfrastrukturen(param: string | undefined): string[] {
    return this.paramToList(param);
  }

  public static merge(
    params:
      | {
          infrastrukturen?: string[];
          tabellenVisible?: boolean;
        }
      | InfrastrukturenQueryParams,
    into: InfrastrukturenQueryParams
  ): InfrastrukturenQueryParams {
    return new InfrastrukturenQueryParams(
      params.infrastrukturen || into.infrastrukturen,
      this.mergeBooleans(params.tabellenVisible, into.tabellenVisible)
    );
  }

  public toRouteParams(): Params {
    return {
      infrastrukturen: AbstractQueryParams.listToParam(this.infrastrukturen),
      tabellenVisible: AbstractQueryParams.booleanToParam(this.tabellenVisible),
    };
  }
}
