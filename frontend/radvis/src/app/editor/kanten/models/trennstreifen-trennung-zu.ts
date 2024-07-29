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

import { EnumOption } from 'src/app/form-elements/models/enum-option';

export enum TrennstreifenTrennungZu {
  SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN = 'SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN',
  SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN = 'SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN',
  SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR = 'SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace TrennstreifenTrennungZu {
  const toOption = (k: string): EnumOption => {
    switch (k) {
      case TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN:
        return { name: k, displayText: 'Sicherheitstrennstreifen zur Fahrbahn' };
      case TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN:
        return { name: k, displayText: 'Sicherheitstrennstreifen zum Parken' };
      case TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR:
        return { name: k, displayText: 'Sicherheitstrennstreifen zum Fußverkehr' };
    }
    throw new Error('Beschreibung für enum TrennstreifenTrennungZu fehlt: ' + k);
  };

  export const options: EnumOption[] = Object.keys(TrennstreifenTrennungZu).map(k => toOption(k));

  export const optionsParken: EnumOption[] = [TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN].map(k =>
    toOption(k)
  );

  export const displayText = (value: TrennstreifenTrennungZu): string => {
    return TrennstreifenTrennungZu.options.find(opt => opt.name === value)!.displayText;
  };
}
