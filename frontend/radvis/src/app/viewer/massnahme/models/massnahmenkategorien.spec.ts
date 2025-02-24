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
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';

describe(Massnahmenkategorien.name, () => {
  describe('isValidMassnahmenKategorienCombination', () => {
    it('should not allow multiple Kategorien from same OberKategorie ', () => {
      expect(
        Massnahmenkategorien.isValidMassnahmenKategorienCombination(
          new UntypedFormControl([
            Massnahmenkategorien.ALL[0].options[0].options[0].name,
            Massnahmenkategorien.ALL[0].options[0].options[1].name,
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

  describe('RADNETZ_2024_KATEGORIEN_ONLY', () => {
    it('should filter all options only for RadNETZ 2024', () => {
      const allKategorieValues = Massnahmenkategorien.ALL.flatMap(oberGroup =>
        oberGroup.options.flatMap(subGroup => subGroup.options)
      ).map(opt => opt.name);
      const totalNumberOfKategorien = allKategorieValues.length;

      expect(
        Massnahmenkategorien.kategorienNotAllowedForRadnetz2024.filter(kat => !allKategorieValues.includes(kat))
      ).toEqual([]);
      expect(
        Massnahmenkategorien.RADNETZ_2024_KATEGORIEN_ONLY.flatMap(oberGroup =>
          oberGroup.options.flatMap(subGroup => subGroup.options)
        ).length
      ).toBe(totalNumberOfKategorien - Massnahmenkategorien.kategorienNotAllowedForRadnetz2024.length);
    });
  });

  describe('validateKonzeptionsquelle', () => {
    [
      Konzeptionsquelle.KOMMUNALES_KONZEPT,
      Konzeptionsquelle.KREISKONZEPT,
      Konzeptionsquelle.RADNETZ_MASSNAHME,
      Konzeptionsquelle.SONSTIGE,
    ].forEach(quelle => {
      it(`should be valid if not allowed kategorie for Radnetz 2024 and konzeptionsquelle=${quelle}`, () => {
        expect(
          Massnahmenkategorien.validateKonzeptionsquelle(
            [Massnahmenkategorien.kategorienNotAllowedForRadnetz2024[0]],
            quelle
          )
        ).toBeNull();
      });
    });

    it('should be invalid if not allowed kategorie for Radnetz 2024 and konzeptionsquelle=RADNETZ_2024', () => {
      expect(
        Massnahmenkategorien.validateKonzeptionsquelle(
          [Massnahmenkategorien.kategorienNotAllowedForRadnetz2024[0]],
          Konzeptionsquelle.RADNETZ_MASSNAHME_2024
        )
      ).not.toBeNull();
    });

    it('should be valid if allowed kategorie for Radnetz 2024 and konzeptionsquelle=RADNETZ_2024', () => {
      expect(
        Massnahmenkategorien.validateKonzeptionsquelle(
          [Massnahmenkategorien.RADNETZ_2024_KATEGORIEN_ONLY[0].options[0].options[0].name],
          Konzeptionsquelle.RADNETZ_MASSNAHME_2024
        )
      ).toBeNull();
    });

    it('should be valid if kategorie is null', () => {
      expect(Massnahmenkategorien.validateKonzeptionsquelle(null, Konzeptionsquelle.RADNETZ_MASSNAHME_2024)).toBeNull();
    });

    it('should be valid if konzeptionsquelle is null', () => {
      expect(
        Massnahmenkategorien.validateKonzeptionsquelle(
          [Massnahmenkategorien.RADNETZ_2024_KATEGORIEN_ONLY[0].options[0].options[0].name],
          null
        )
      ).toBeNull();
    });
  });
});
