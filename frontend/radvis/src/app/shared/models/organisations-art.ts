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

export enum OrganisationsArt {
  BUNDESLAND = 'BUNDESLAND',
  GEMEINDE = 'GEMEINDE',
  KREIS = 'KREIS',
  REGIERUNGSBEZIRK = 'REGIERUNGSBEZIRK',

  TOURISMUSVERBAND = 'TOURISMUSVERBAND',
  EXTERNER_DIENSTLEISTER = 'EXTERNER_DIENSTLEISTER',
  REGIONALVERBAND = 'REGIONALVERBAND',
  SONSTIGES = 'SONSTIGES',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace OrganisationsArt {
  export const options: EnumOption[] = Object.keys(OrganisationsArt).map(
    (k: string): EnumOption => {
      switch (k) {
        case OrganisationsArt.BUNDESLAND:
          return { name: k, displayText: 'Bundesland' };
        case OrganisationsArt.GEMEINDE:
          return { name: k, displayText: 'Gemeinde' };
        case OrganisationsArt.KREIS:
          return { name: k, displayText: 'Kreis' };
        case OrganisationsArt.REGIERUNGSBEZIRK:
          return { name: k, displayText: 'Regierungsbezirk' };
        case OrganisationsArt.TOURISMUSVERBAND:
          return { name: k, displayText: 'Tourismusverband' };
        case OrganisationsArt.EXTERNER_DIENSTLEISTER:
          return { name: k, displayText: 'Externer Dienstleister' };
        case OrganisationsArt.REGIONALVERBAND:
          return { name: k, displayText: 'Regionalverband' };
        case OrganisationsArt.SONSTIGES:
          return { name: k, displayText: 'Sonstiges' };
      }
      throw new Error('Beschreibung für enum OrganisationsArt fehlt: ' + k);
    }
  );

  // Achtung, hier ist die Reihenfolge der Definitionen wichtig:
  // Das Feld istGebietskoerperschaft vom nameSpace OrganisationsArt soll nicht bei dem Aufruf
  // weiter oben "Object.keys(OrganisationsArt)" aufgelistet werden.
  export const istGebietskoerperschaft = (orgaArt: string): boolean => {
    // Achtung, die Zuordnung von gebietskoerperschaft zu den einzelnen Felder
    // findet auch nochmal im BE in OrganisationsArt.java statt
    switch (orgaArt) {
      case OrganisationsArt.BUNDESLAND:
      case OrganisationsArt.GEMEINDE:
      case OrganisationsArt.KREIS:
      case OrganisationsArt.REGIERUNGSBEZIRK:
        return true;
      case OrganisationsArt.TOURISMUSVERBAND:
      case OrganisationsArt.EXTERNER_DIENSTLEISTER:
      case OrganisationsArt.REGIONALVERBAND:
      case OrganisationsArt.SONSTIGES:
        return false;
    }
    throw new Error('Zuordnung von Gebietskörperschaft für enum OrganisationsArt fehlt: ' + orgaArt);
  };

  export const getDisplayName = (art: OrganisationsArt): string => {
    return options.find(o => o.name === art)?.displayText ?? art;
  };
}
