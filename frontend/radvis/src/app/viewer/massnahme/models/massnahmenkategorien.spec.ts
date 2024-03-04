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

import { UntypedFormControl } from '@angular/forms';
import { MASSNAHMENKATEGORIEN, Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';

describe(Massnahmenkategorien.isValidMassnahmenKategorienCombination.name, () => {
  it('should not allow multiple Kategorien from same OberKategorie ', () => {
    expect(
      Massnahmenkategorien.isValidMassnahmenKategorienCombination(
        new UntypedFormControl([
          MASSNAHMENKATEGORIEN[0].options[0].options[0].name,
          MASSNAHMENKATEGORIEN[0].options[0].options[1].name,
        ])
      )
    ).not.toBeNull();

    expect(
      Massnahmenkategorien.isValidMassnahmenKategorienCombination(
        new UntypedFormControl([
          'AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDUNG',
          'BARRIERE_SICHERN_BZW_PRUEFUNG_AUF_VERZICHT',
        ])
      )
    ).not.toBeNull();

    expect(
      Massnahmenkategorien.isValidMassnahmenKategorienCombination(
        new UntypedFormControl(['BAU_EINER_FAHRBAHNEINENGUNG', 'MARKIERUNGSTECHNISCHE_MASSNAHME'])
      )
    ).toBeNull();
  });

  it('should allow null and empty value', () => {
    expect(Massnahmenkategorien.isValidMassnahmenKategorienCombination(new UntypedFormControl([]))).toBeNull();
    expect(Massnahmenkategorien.isValidMassnahmenKategorienCombination(new UntypedFormControl(null))).toBeNull();
  });
});
