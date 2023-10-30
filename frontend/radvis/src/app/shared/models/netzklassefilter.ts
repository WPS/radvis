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

export class Netzklassefilter {
  public static readonly RADNETZ = new Netzklassefilter('RADNETZ', 'RadNETZ', 0);
  public static readonly NICHT_KLASSIFIZIERT = new Netzklassefilter(
    'NICHT_KLASSIFIZIERT',
    'Nicht Klassifiziert',
    15.25
  );
  public static readonly KOMMUNALNETZ = new Netzklassefilter('KOMMUNALNETZ', 'Kommunal-Netz', 12);
  public static readonly KREISNETZ = new Netzklassefilter('KREISNETZ', 'Kreis-Netz', 11);
  public static readonly RADSCHNELLVERBINDUNG = new Netzklassefilter('RADSCHNELLVERBINDUNG', 'Radschnellverbindung', 0);
  public static readonly RADVORRANGROUTEN = new Netzklassefilter('RADVORRANGROUTEN', 'Radvorrangrouten', 11);

  private constructor(
    public readonly name: string,
    public readonly displayText: string,
    public readonly minZoom: number
  ) {}

  public static getAll(): Netzklassefilter[] {
    return Object.values(Netzklassefilter);
  }

  public toString(): string {
    return this.name;
  }

  public isVisibleOnZoomlevel(zoom: number): boolean {
    return zoom > this.minZoom;
  }
}
