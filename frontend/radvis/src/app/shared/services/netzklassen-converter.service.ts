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

import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { NetzklasseFlat, NetzklasseFlatUndertermined } from 'src/app/shared/models/netzklasse-flat';

export class NetzklassenConverterService {
  public static createFormGroup(): UntypedFormGroup {
    return new UntypedFormGroup({
      radnetzAlltag: new UntypedFormControl(null),
      radnetzFreizeit: new UntypedFormControl(null),
      radnetzZielnetz: new UntypedFormControl(null),
      kreisnetzAlltag: new UntypedFormControl(null),
      kreisnetzFreizeit: new UntypedFormControl(null),
      kommunalnetzAlltag: new UntypedFormControl(null),
      kommunalnetzFreizeit: new UntypedFormControl(null),
      radschnellverbindung: new UntypedFormControl(null),
      radvorrangrouten: new UntypedFormControl(null),
    });
  }

  public static convertToFlat<T extends NetzklasseFlat | NetzklasseFlatUndertermined>(netzklassen: Netzklasse[]): T {
    return {
      radnetzAlltag: netzklassen.includes(Netzklasse.RADNETZ_ALLTAG),
      radnetzFreizeit: netzklassen.includes(Netzklasse.RADNETZ_FREIZEIT),
      radnetzZielnetz: netzklassen.includes(Netzklasse.RADNETZ_ZIELNETZ),
      kreisnetzAlltag: netzklassen.includes(Netzklasse.KREISNETZ_ALLTAG),
      kreisnetzFreizeit: netzklassen.includes(Netzklasse.KREISNETZ_FREIZEIT),
      kommunalnetzAlltag: netzklassen.includes(Netzklasse.KOMMUNALNETZ_ALLTAG),
      kommunalnetzFreizeit: netzklassen.includes(Netzklasse.KOMMUNALNETZ_FREIZEIT),
      radschnellverbindung: netzklassen.includes(Netzklasse.RADSCHNELLVERBINDUNG),
      radvorrangrouten: netzklassen.includes(Netzklasse.RADVORRANGROUTEN),
    } as T;
  }

  public static convertFlatToNetzklassen(flat: NetzklasseFlat): Netzklasse[] {
    const result: Netzklasse[] = [];
    if (flat.kommunalnetzAlltag === true) {
      result.push(Netzklasse.KOMMUNALNETZ_ALLTAG);
    }
    if (flat.kommunalnetzFreizeit === true) {
      result.push(Netzklasse.KOMMUNALNETZ_FREIZEIT);
    }
    if (flat.kreisnetzAlltag === true) {
      result.push(Netzklasse.KREISNETZ_ALLTAG);
    }
    if (flat.kreisnetzFreizeit === true) {
      result.push(Netzklasse.KREISNETZ_FREIZEIT);
    }
    if (flat.radnetzAlltag === true) {
      result.push(Netzklasse.RADNETZ_ALLTAG);
    }
    if (flat.radnetzFreizeit === true) {
      result.push(Netzklasse.RADNETZ_FREIZEIT);
    }
    if (flat.radnetzZielnetz === true) {
      result.push(Netzklasse.RADNETZ_ZIELNETZ);
    }
    if (flat.radschnellverbindung === true) {
      result.push(Netzklasse.RADSCHNELLVERBINDUNG);
    }
    if (flat.radvorrangrouten === true) {
      result.push(Netzklasse.RADVORRANGROUTEN);
    }
    return result;
  }

  public static convertFlatUndeterminedToNetzklassen(
    flat: NetzklasseFlatUndertermined,
    oldValues: Netzklasse[]
  ): Netzklasse[] {
    const result: Netzklasse[] = [];
    if (
      (flat.kommunalnetzAlltag instanceof UndeterminedValue && oldValues.includes(Netzklasse.KOMMUNALNETZ_ALLTAG)) ||
      flat.kommunalnetzAlltag === true
    ) {
      result.push(Netzklasse.KOMMUNALNETZ_ALLTAG);
    }

    if (
      (flat.kommunalnetzFreizeit instanceof UndeterminedValue &&
        oldValues.includes(Netzklasse.KOMMUNALNETZ_FREIZEIT)) ||
      flat.kommunalnetzFreizeit === true
    ) {
      result.push(Netzklasse.KOMMUNALNETZ_FREIZEIT);
    }

    if (
      (flat.kreisnetzAlltag instanceof UndeterminedValue && oldValues.includes(Netzklasse.KREISNETZ_ALLTAG)) ||
      flat.kreisnetzAlltag === true
    ) {
      result.push(Netzklasse.KREISNETZ_ALLTAG);
    }

    if (
      (flat.kreisnetzFreizeit instanceof UndeterminedValue && oldValues.includes(Netzklasse.KREISNETZ_FREIZEIT)) ||
      flat.kreisnetzFreizeit === true
    ) {
      result.push(Netzklasse.KREISNETZ_FREIZEIT);
    }

    if (
      (flat.radnetzAlltag instanceof UndeterminedValue && oldValues.includes(Netzklasse.RADNETZ_ALLTAG)) ||
      flat.radnetzAlltag === true
    ) {
      result.push(Netzklasse.RADNETZ_ALLTAG);
    }
    if (
      (flat.radnetzFreizeit instanceof UndeterminedValue && oldValues.includes(Netzklasse.RADNETZ_FREIZEIT)) ||
      flat.radnetzFreizeit === true
    ) {
      result.push(Netzklasse.RADNETZ_FREIZEIT);
    }

    if (
      (flat.radnetzZielnetz instanceof UndeterminedValue && oldValues.includes(Netzklasse.RADNETZ_ZIELNETZ)) ||
      flat.radnetzZielnetz === true
    ) {
      result.push(Netzklasse.RADNETZ_ZIELNETZ);
    }

    if (
      (flat.radschnellverbindung instanceof UndeterminedValue && oldValues.includes(Netzklasse.RADSCHNELLVERBINDUNG)) ||
      flat.radschnellverbindung === true
    ) {
      result.push(Netzklasse.RADSCHNELLVERBINDUNG);
    }

    if (
      (flat.radvorrangrouten instanceof UndeterminedValue && oldValues.includes(Netzklasse.RADVORRANGROUTEN)) ||
      flat.radvorrangrouten === true
    ) {
      result.push(Netzklasse.RADVORRANGROUTEN);
    }

    return result;
  }
}
