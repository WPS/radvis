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

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'rad-statistik-anzeige',
  templateUrl: './statistik-anzeige.component.html',
  styleUrls: ['./statistik-anzeige.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatistikAnzeigeComponent {
  @Input()
  public statistik: { [key: string]: number } | undefined;

  public getEntriesForStatistik(statistik: { [key: string]: number } | undefined): [string, number][] {
    if (!statistik) {
      return [];
    }
    return Object.entries(statistik)
      .filter(([[], value]) => !isNaN(value))
      .map(([key, value]) => [this.camel2title(key), value]);
  }

  private camel2title = (camelCase: string): string =>
    camelCase
      .replace(/([A-Z])/g, (match: string) => ` ${match}`)
      .replace(/^./, (match: string) => match.toUpperCase())
      .trim();
}
