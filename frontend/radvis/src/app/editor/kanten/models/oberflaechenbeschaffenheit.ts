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

export enum Oberflaechenbeschaffenheit {
  NEUWERTIG = 'NEUWERTIG',
  SEHR_GUTER_BIS_GUTER_ZUSTAND = 'SEHR_GUTER_BIS_GUTER_ZUSTAND',
  GUTER_BIS_MITTLERER_ZUSTAND = 'GUTER_BIS_MITTLERER_ZUSTAND',
  ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE = 'ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE',
  EINLEITUNG_BAULICHER_ODER_VERKEHRSBESCHRAENKENDER_MASSNAHMEN = 'EINLEITUNG_BAULICHER_ODER_VERKEHRSBESCHRAENKENDER_MASSNAHMEN',
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Oberflaechenbeschaffenheit {
  export const options: EnumOption[] = Object.keys(Oberflaechenbeschaffenheit).map(
    (k: string): EnumOption => {
      switch (k) {
        case Oberflaechenbeschaffenheit.NEUWERTIG:
          return { name: k, displayText: 'Neuwertig' };
        case Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND:
          return { name: k, displayText: 'Sehr guter bis guter Zustand' };
        case Oberflaechenbeschaffenheit.GUTER_BIS_MITTLERER_ZUSTAND:
          return { name: k, displayText: 'Guter bis mittlerer Zustand' };
        case Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE:
          return { name: k, displayText: 'Anlass zur intensiven Beobachtung und Analyse' };
        case Oberflaechenbeschaffenheit.EINLEITUNG_BAULICHER_ODER_VERKEHRSBESCHRAENKENDER_MASSNAHMEN:
          return { name: k, displayText: 'Einleitung baulicher oder verkehrsbeschränkender Maßnahmen' };
        case Oberflaechenbeschaffenheit.UNBEKANNT:
          return { name: k, displayText: 'Unbekannt' };
      }
      throw new Error('Beschreibung für enum Oberflaechenbeschaffenheit fehlt: ' + k);
    }
  );
}
