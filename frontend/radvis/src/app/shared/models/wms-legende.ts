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

export class WMSLegende {
  public url: string;

  constructor(public titel: string, _url: string) {
    const url = new URL(_url.replace(/layers=/gi, 'layer='));

    url.searchParams.set('request', 'GetLegendGraphic');
    url.searchParams.set('version', '1.3.0');
    url.searchParams.set('service', 'WMS');
    url.searchParams.set('format', 'image/png');
    url.searchParams.set('width', '20');
    url.searchParams.set('height', '20');
    url.searchParams.set('LEGEND_OPTIONS', 'fontStyle:bold;fontAntiAliasing:true;labelMargin:12');

    this.url = url.toString();
  }
}
