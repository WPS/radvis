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
import { ErweiterterAnpassungswunschFilter } from 'src/app/viewer/anpassungswunsch/models/erweiterter-anpassungswunsch-filter';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';

describe('ErweiterterAnpassungswunschFilter', () => {
  describe('isEmpty', () => {
    it('should be true', () => {
      expect(
        ErweiterterAnpassungswunschFilter.isEmpty({ abgeschlosseneAusblenden: false, fahrradrouteFilter: null })
      ).toBeTrue();
    });

    it('should be false', () => {
      expect(
        ErweiterterAnpassungswunschFilter.isEmpty({ abgeschlosseneAusblenden: true, fahrradrouteFilter: null })
      ).toBeFalse();

      expect(
        ErweiterterAnpassungswunschFilter.isEmpty({
          abgeschlosseneAusblenden: false,
          fahrradrouteFilter: {
            fahrradroute: null,
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
            fahrradroutenIds: [1],
          },
        })
      ).toBeFalse();
    });
  });
});
