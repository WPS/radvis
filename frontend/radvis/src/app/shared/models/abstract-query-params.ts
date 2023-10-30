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

export abstract class AbstractQueryParams {
  public static booleanToParam(bool: boolean | null): boolean | string {
    return bool === null ? '' : bool;
  }

  public static listToParam(list: string[] | null): string | undefined {
    if (list) {
      return list.join(';');
    }
    return '';
  }

  public static paramToList(param: string | undefined): string[] {
    if (param) {
      return param.split(';');
    }
    return [];
  }

  public static paramToBoolean(param: string | undefined): boolean | null {
    return param === '' || param === undefined ? null : JSON.parse(param);
  }

  public static mergeBooleans(param: boolean | undefined | null, into: boolean | null): boolean | null {
    return param !== undefined && param !== null ? param : into;
  }

  public toRoute(): string {
    const routeParams = this.toRouteParams();
    return Object.keys(routeParams)
      .filter(key => routeParams[key] !== undefined)
      .map(key => `${key}=${routeParams[key]}`)
      .join('&');
  }

  public replaceIn(params: Params): Params {
    return { ...params, ...this.toRouteParams() };
  }

  public abstract toRouteParams(): Params;
}
