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
import { FahrradrouteFilter } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { testFahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view-test-data-provider.spec';

describe('FahrradrouteFilter', () => {
  describe('equal', () => {
    it('should be true if kategorie is same', () => {
      expect(
        FahrradrouteFilter.equal(
          {
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
            fahrradroute: null,
            fahrradroutenIds: [],
          },
          {
            fahrradroute: null,
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
            fahrradroutenIds: [],
          }
        )
      ).toBeTrue();
    });

    it('should be true if einzelne route is same', () => {
      expect(
        FahrradrouteFilter.equal(
          {
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
            fahrradroute: testFahrradrouteListenView[0],
            fahrradroutenIds: [],
          },
          {
            fahrradroute: testFahrradrouteListenView[0],
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
            fahrradroutenIds: [],
          }
        )
      ).toBeTrue();
    });

    it('should be true if both null', () => {
      expect(FahrradrouteFilter.equal(null, null)).toBeTrue();
    });

    it('should be false if kategories differ', () => {
      expect(
        FahrradrouteFilter.equal(
          {
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
            fahrradroute: null,
            fahrradroutenIds: [],
          },
          {
            fahrradroute: null,
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
            fahrradroutenIds: [],
          }
        )
      ).toBeFalse();
    });

    it('should be false if einzelne route differs', () => {
      expect(
        FahrradrouteFilter.equal(
          {
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
            fahrradroute: { ...testFahrradrouteListenView[0], id: 1 },
            fahrradroutenIds: [],
          },
          {
            fahrradroute: { ...testFahrradrouteListenView[0], id: 2 },
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
            fahrradroutenIds: [],
          }
        )
      ).toBeFalse();
    });

    it('should be false if one is null', () => {
      expect(
        FahrradrouteFilter.equal(null, {
          fahrradroute: null,
          fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
          fahrradroutenIds: [],
        })
      ).toBeFalse();
    });
  });
});
