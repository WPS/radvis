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

import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';

export interface NetzklasseFlat {
  radnetzAlltag: boolean;
  radnetzFreizeit: boolean;
  radnetzZielnetz: boolean;
  kreisnetzAlltag: boolean;
  kreisnetzFreizeit: boolean;
  kommunalnetzAlltag: boolean;
  kommunalnetzFreizeit: boolean;
  radschnellverbindung: boolean;
  radvorrangrouten: boolean;
}

export interface NetzklasseFlatUndertermined {
  radnetzAlltag: boolean | UndeterminedValue;
  radnetzFreizeit: boolean | UndeterminedValue;
  radnetzZielnetz: boolean | UndeterminedValue;
  kreisnetzAlltag: boolean | UndeterminedValue;
  kreisnetzFreizeit: boolean | UndeterminedValue;
  kommunalnetzAlltag: boolean | UndeterminedValue;
  kommunalnetzFreizeit: boolean | UndeterminedValue;
  radschnellverbindung: boolean | UndeterminedValue;
  radvorrangrouten: boolean | UndeterminedValue;
}
