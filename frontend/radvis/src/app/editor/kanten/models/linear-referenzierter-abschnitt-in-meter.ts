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

import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import invariant from 'tiny-invariant';

export class LinearReferenzierterAbschnittInMeter {
  public static readonly MINIMUM_ELEMENT_LENGTH = 0.01;

  private constructor(
    private _von: number,
    private _bis: number
  ) {}

  public static of(
    linearReferenzierterAbschnitt: LinearReferenzierterAbschnitt,
    laenge: number
  ): LinearReferenzierterAbschnittInMeter {
    invariant(linearReferenzierterAbschnitt);
    invariant(laenge);
    return new LinearReferenzierterAbschnittInMeter(
      linearReferenzierterAbschnitt.von * laenge,
      linearReferenzierterAbschnitt.bis * laenge
    );
  }

  public static ofMeter(von: number, bis: number): LinearReferenzierterAbschnittInMeter {
    return new LinearReferenzierterAbschnittInMeter(von, bis);
  }

  public getLinearReferenzierterAbschnittRelativZuLaenge(laenge: number): LinearReferenzierterAbschnitt {
    // Wir erlauben eine Eingabegenauigkeit in cm, darum addieren wir 2 Kommanachstellen (+ 3)
    const numberOfDigits = Math.round(laenge).toString().length + 3;
    const roundToDigits = (value: number, digits: number): number => {
      const precision = Math.pow(10, digits);
      const prunedValue = value * precision;
      return Math.round(prunedValue) / precision;
    };
    const vonRound = roundToDigits(this._von / laenge, numberOfDigits);
    const bisRound = roundToDigits(this._bis / laenge, numberOfDigits);
    return { von: vonRound, bis: bisRound };
  }

  get von(): number {
    return this._von;
  }

  get bis(): number {
    return this._bis;
  }
}
