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
import { WMSCapabilities } from 'ol/format';

@Injectable({
  providedIn: 'root',
})
export class WmsCapabilitiesService {
  private wmsCapabilities: Promise<any> | null = null;

  constructor(private http: HttpClient) {}

  isStyleForLayerAvailable(layerName: string, styleName: string): Promise<boolean> {
    if (!this.wmsCapabilities) {
      this.wmsCapabilities = this.fetchWmsCapabilities();
    }
    return this.wmsCapabilities.then(wmsCapabilities => {
      return !!wmsCapabilities?.Capability?.Layer?.Layer?.find((l: any) => l.Name === layerName)?.Style?.find(
        (s: any) => s.Name === styleName
      );
    });
  }

  private fetchWmsCapabilities(): Promise<any> {
    return this.http
      .get('/api/geoserver/saml/radvis/wms?REQUEST=GetCapabilities', { responseType: 'text' })
      .toPromise()
      .then(text => {
        return new WMSCapabilities().read(text);
      });
  }
}
