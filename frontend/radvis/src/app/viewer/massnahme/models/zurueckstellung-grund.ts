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

export enum ZurueckstellungsGrund {
  FINANZIELLE_RESSOURCEN = 'FINANZIELLE_RESSOURCEN',
  PERSONELLE_ZEITLICHE_RESSOURCEN = 'PERSONELLE_ZEITLICHE_RESSOURCEN',
  WEITERE_PLANUNGEN_IM_ZUSAMMENHANG = 'WEITERE_PLANUNGEN_IM_ZUSAMMENHANG',
  WEITERE_GRUENDE = 'WEITERE_GRUENDE',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace ZurueckstellungsGrund {
  export const options: EnumOption[] = Object.keys(ZurueckstellungsGrund).map((k: string): EnumOption => {
    switch (k) {
      case ZurueckstellungsGrund.WEITERE_GRUENDE:
        return { name: k, displayText: 'Weitere Gründe' };
      case ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN:
        return { name: k, displayText: 'Finanzielle Ressourcen (Haushalt)' };
      case ZurueckstellungsGrund.PERSONELLE_ZEITLICHE_RESSOURCEN:
        return { name: k, displayText: 'Personelle/zeitliche Ressourcen' };
      case ZurueckstellungsGrund.WEITERE_PLANUNGEN_IM_ZUSAMMENHANG:
        return { name: k, displayText: 'Weitere im Zusammenhang stehende Planungen' };
    }
    throw new Error('Beschreibung für enum Umsetzungsstatus fehlt: ' + k);
  });
}
