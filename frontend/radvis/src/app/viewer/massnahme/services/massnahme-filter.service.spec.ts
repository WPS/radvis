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

import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { skip } from 'rxjs/operators';
import {
  defaultBundeslandOrganisation,
  defaultOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { getTestMassnahmeListenViews } from 'src/app/viewer/massnahme/models/massnahme-listen-view-test-data-provider.spec';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { FilterQueryParams } from 'src/app/viewer/viewer-shared/models/filter-query-params';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, resetCalls, verify, when } from 'ts-mockito';

describe(MassnahmeFilterService.name, () => {
  let massnahmeFilterService: MassnahmeFilterService;
  let massnahmeService: MassnahmeService;
  let benutzerDetailsService: BenutzerDetailsService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let selektierteInfrastrukturen$: BehaviorSubject<Infrastruktur[]>;
  let filterQueryParamsService: FilterQueryParamsService;
  let filter$: BehaviorSubject<Map<Infrastruktur, FieldFilter[]>>;
  const testMassnahmenListenViews: MassnahmeListenView[] = getTestMassnahmeListenViews();

  beforeEach(() => {
    massnahmeService = mock(MassnahmeService);
    benutzerDetailsService = mock(BenutzerDetailsService);
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    filterQueryParamsService = mock(FilterQueryParamsService);
    when(massnahmeService.getAll(anything())).thenResolve(testMassnahmenListenViews);
    selektierteInfrastrukturen$ = new BehaviorSubject<Infrastruktur[]>([]);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(
      selektierteInfrastrukturen$.asObservable()
    );
    filter$ = new BehaviorSubject(new Map<Infrastruktur, FieldFilter[]>());
    when(filterQueryParamsService.filter$).thenReturn(filter$.asObservable());
    when(filterQueryParamsService.filterQueryParamsSnapshot).thenReturn(
      new FilterQueryParams(new Map<string, FieldFilter[]>())
    );

    return MockBuilder(MassnahmeFilterService, ViewerModule)
      .provide({
        provide: MassnahmeService,
        useValue: instance(massnahmeService),
      })
      .provide({
        provide: InfrastrukturenSelektionService,
        useValue: instance(infrastrukturenSelektionService),
      })
      .provide({
        provide: FilterQueryParamsService,
        useValue: instance(filterQueryParamsService),
      })
      .provide({
        provide: BenutzerDetailsService,
        useValue: instance(benutzerDetailsService),
      });
  });

  describe('activeFilter$', () => {
    it('should be false', () => {
      massnahmeFilterService = TestBed.inject(MassnahmeFilterService);

      massnahmeFilterService.updateErweiterterFilter({
        historischeMassnahmenAnzeigen: true,
        fahrradrouteFilter: null,
        organisation: null,
      });

      expect(massnahmeFilterService['erweiterterFilterAktiv$$'].value).toBeFalse();
    });

    it('should be true if default organisation filtered', () => {
      when(benutzerDetailsService.aktuellerBenutzerOrganisation()).thenReturn(defaultOrganisation);
      massnahmeFilterService = TestBed.inject(MassnahmeFilterService);

      massnahmeFilterService.updateErweiterterFilter({
        historischeMassnahmenAnzeigen: true,
        fahrradrouteFilter: null,
        organisation: defaultOrganisation,
      });

      expect(massnahmeFilterService['erweiterterFilterAktiv$$'].value).toBeTrue();
    });

    it('should be true if veraltete Massnahmen filtered', () => {
      when(benutzerDetailsService.aktuellerBenutzerOrganisation()).thenReturn(defaultBundeslandOrganisation);
      massnahmeFilterService = TestBed.inject(MassnahmeFilterService);

      massnahmeFilterService.updateErweiterterFilter({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilter: null,
        organisation: null,
      });

      expect(massnahmeFilterService['erweiterterFilterAktiv$$'].value).toBeTrue();
    });

    it('should be true if Fahrradroute filtered', () => {
      when(benutzerDetailsService.aktuellerBenutzerOrganisation()).thenReturn(defaultBundeslandOrganisation);
      massnahmeFilterService = TestBed.inject(MassnahmeFilterService);

      massnahmeFilterService.updateErweiterterFilter({
        historischeMassnahmenAnzeigen: true,
        fahrradrouteFilter: {
          fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
          fahrradroute: null,
          fahrradroutenIds: [],
        },
        organisation: null,
      });

      expect(massnahmeFilterService['erweiterterFilterAktiv$$'].value).toBeTrue();
    });
  });

  describe('without initial Values from BenutzerDetailService', () => {
    beforeEach(() => {
      massnahmeFilterService = TestBed.inject(MassnahmeFilterService);
    });

    it('should reset erweiterterFilter to have no organisation ', fakeAsync(() => {
      massnahmeFilterService.updateErweiterterFilter({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilter: null,
        organisation: defaultOrganisation,
      });

      resetCalls(massnahmeService);
      massnahmeFilterService.reset();

      expect(massnahmeFilterService.erweiterterFilter.organisation).toBeNull();
      verify(massnahmeService.getAll(anything())).once();
    }));

    describe('selektierteInfrastrukturen$', () => {
      it('should refetch if massnahmen got activated', fakeAsync(() => {
        selektierteInfrastrukturen$.next([MASSNAHMEN]);

        tick();

        verify(massnahmeService.getAll(anything())).once();
        verify(filterQueryParamsService.filterQueryParamsSnapshot).once();
        expect().nothing();
      }));

      it('should clear if massnahmen got deactivated', fakeAsync(() => {
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen).toEqual([]);
        });

        tick();
        selektierteInfrastrukturen$.next([]);

        tick();
        subscription.unsubscribe();
      }));
    });

    describe('refetchMassnahmen', () => {
      it('should assign fetched massnahmen to alleMassnahmeListenViews', fakeAsync(() => {
        let index = 0;
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen).toEqual([[], testMassnahmenListenViews][index]);
          index++;
        });

        tick();
        massnahmeFilterService.refetchData();

        tick();
        subscription.unsubscribe();
      }));
    });

    describe('filterField', () => {
      it('no filter should not filter the list', fakeAsync(() => {
        let index = 0;
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen).toEqual([[], testMassnahmenListenViews, testMassnahmenListenViews][index]);
          index++;
        });

        tick();
        massnahmeFilterService.refetchData();

        tick();
        massnahmeFilterService.filterField('bezeichnung', '');

        tick();
        verify(filterQueryParamsService.update(anything(), anything())).once();
        subscription.unsubscribe();
      }));

      it('should filter out the missmatching massnahme', fakeAsync(() => {
        let index = 0;
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen).toEqual([[], testMassnahmenListenViews, [testMassnahmenListenViews[1]]][index]);
          index++;
        });

        tick();
        massnahmeFilterService.refetchData();

        tick();
        massnahmeFilterService.filterField('bezeichnung', 'nochEineMassnahme');

        tick();
        verify(filterQueryParamsService.update(anything(), anything())).once();
        subscription.unsubscribe();
      }));

      it('should reset', fakeAsync(() => {
        let index = 0;
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen).toEqual(
            [[], testMassnahmenListenViews, [testMassnahmenListenViews[1]], testMassnahmenListenViews][index]
          );
          index++;
        });
        tick();

        massnahmeFilterService.refetchData();
        tick();

        massnahmeFilterService.filterField('bezeichnung', 'nochEineMassnahme');
        tick();

        massnahmeFilterService.reset();
        tick();

        verify(filterQueryParamsService.reset(MASSNAHMEN)).once();
        subscription.unsubscribe();
      }));

      it('should filter out missmatching massnahmen', fakeAsync(() => {
        let index = 0;
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen).toEqual(
            [[], testMassnahmenListenViews, testMassnahmenListenViews, [testMassnahmenListenViews[0]]][index]
          );
          index++;
        });

        tick();
        massnahmeFilterService.refetchData();

        tick();
        massnahmeFilterService.filterField('bezeichnung', 'Massnahme');

        tick();
        massnahmeFilterService.filterField('umsetzungsstatus', 'IDEE');

        tick();
        verify(filterQueryParamsService.update(anything(), anything())).twice();
        subscription.unsubscribe();
      }));

      it('should not filter out anything, cause both are Freizeit (Kreisnetz)', fakeAsync(() => {
        let index = 0;
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen).toEqual([[], testMassnahmenListenViews, testMassnahmenListenViews][index]);
          index++;
        });

        tick();
        massnahmeFilterService.refetchData();

        tick();
        massnahmeFilterService.filterField('netzklassen', 'Freizeit (Kreisnetz)');

        tick();
        verify(filterQueryParamsService.update(anything(), anything())).once();
        subscription.unsubscribe();
      }));

      it('should filter out every massnahme', fakeAsync(() => {
        let index = 0;
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen).toEqual([[], testMassnahmenListenViews, []][index]);
          index++;
        });

        tick();
        massnahmeFilterService.refetchData();

        tick();
        massnahmeFilterService.filterField('bezeichnung', 'diesesWortKommtNichtInDenMassnahmenVor');

        tick();
        verify(filterQueryParamsService.update(anything(), anything())).once();
        subscription.unsubscribe();
      }));

      it('should filter Massnahmenkategorien by Oberkategorien', fakeAsync(() => {
        const markierungsMassnahme: MassnahmeListenView = { ...testMassnahmenListenViews[0] };
        markierungsMassnahme.massnahmenkategorien = ['FURT_STVO_KONFORM'];
        const knotenMassnahme: MassnahmeListenView = { ...testMassnahmenListenViews[0] };
        knotenMassnahme.massnahmenkategorien = ['AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDNUNG'];

        const newTestMassnahmen = testMassnahmenListenViews.concat([markierungsMassnahme, knotenMassnahme]);
        when(massnahmeService.getAll(anything())).thenResolve(newTestMassnahmen);

        let index = 0;
        const subscription = massnahmeFilterService.filteredList$.subscribe(massnahmen => {
          expect(massnahmen)
            .withContext('SubscriptionIndex: ' + index)
            .toEqual([[], newTestMassnahmen, [markierungsMassnahme], newTestMassnahmen, [knotenMassnahme]][index]);
          index++;
        });

        tick();
        massnahmeFilterService.refetchData();

        tick();
        massnahmeFilterService.filterField('massnahmenkategorien', 'markier');

        tick();
        massnahmeFilterService.filterField('massnahmenkategorien', '');

        tick();
        massnahmeFilterService.filterField('massnahmenkategorien', 'knote');

        tick();
        verify(filterQueryParamsService.update(anything(), anything())).thrice();
        subscription.unsubscribe();
      }));
    });

    describe('updateErweiterterFilter', () => {
      it('should not refetch data if unchanged', () => {
        massnahmeFilterService.updateErweiterterFilter(massnahmeFilterService.erweiterterFilter);

        verify(massnahmeService.getAll(anything())).never();
        expect().nothing();
      });

      it('should refetch data', () => {
        const neuerErweiterterFilter = {
          historischeMassnahmenAnzeigen: true,
          fahrradrouteFilter: {
            fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
            fahrradroute: null,
            fahrradroutenIds: [1, 2],
          },
          organisation: defaultOrganisation,
        };
        massnahmeFilterService.updateErweiterterFilter(neuerErweiterterFilter);

        verify(massnahmeService.getAll(anything())).once();
        expect(capture(massnahmeService.getAll).last()[0]).toEqual(neuerErweiterterFilter);
      });

      it('should trigger filterActive$', (done: DoneFn) => {
        massnahmeFilterService.updateErweiterterFilter({
          historischeMassnahmenAnzeigen: true,
          fahrradrouteFilter: null,
          organisation: null,
        });
        massnahmeFilterService.erweiterterFilterAktiv$.pipe(skip(1)).subscribe(v => {
          expect(v).toBe(true);
          done();
        });
        //observable triggert nur, wenn sich der wert ändert
        massnahmeFilterService.updateErweiterterFilter({
          historischeMassnahmenAnzeigen: false,
          fahrradrouteFilter: null,
          organisation: defaultOrganisation,
        });
      });
    });
  });

  describe('with initial Values from BenutzerDetailService', () => {
    describe('with Non-Bundesland-Organisation', () => {
      beforeEach(() => {
        when(benutzerDetailsService.aktuellerBenutzerOrganisation()).thenReturn(defaultOrganisation);
        massnahmeFilterService = TestBed.inject(MassnahmeFilterService);
      });

      it('should activate Verwaltungseinheit filter initially', () => {
        expect(massnahmeFilterService.erweiterterFilter.organisation).toEqual(defaultOrganisation);
        expect(massnahmeFilterService['erweiterterFilterAktiv$$'].value).toBe(true);
      });

      it('should not activate Fahrradroute filters initially', () => {
        expect(massnahmeFilterService.erweiterterFilter.fahrradrouteFilter).toBeNull();
      });

      it('should reset erweiterterFilter to have the initial organisation ', fakeAsync(() => {
        massnahmeFilterService.updateErweiterterFilter({
          historischeMassnahmenAnzeigen: false,
          fahrradrouteFilter: null,
          organisation: null,
        });

        resetCalls(massnahmeService);
        massnahmeFilterService.reset();

        expect(massnahmeFilterService.erweiterterFilter.organisation).toEqual(defaultOrganisation);
        verify(massnahmeService.getAll(anything())).once();
      }));
    });

    describe('with Bundesland-Organisation', () => {
      beforeEach(() => {
        when(benutzerDetailsService.aktuellerBenutzerOrganisation()).thenReturn(defaultBundeslandOrganisation);
        massnahmeFilterService = TestBed.inject(MassnahmeFilterService);
      });

      it('should not activate Verwaltungseinheit filter initially', fakeAsync(() => {
        expect(massnahmeFilterService.erweiterterFilter.organisation).toBeNull();
      }));

      it('should not activate Fahrradroute filters initially', () => {
        expect(massnahmeFilterService.erweiterterFilter.fahrradrouteFilter).toBeNull();
      });

      it('should reset erweiterterFilter to have no organisation ', fakeAsync(() => {
        massnahmeFilterService.updateErweiterterFilter({
          historischeMassnahmenAnzeigen: false,
          fahrradrouteFilter: null,
          organisation: defaultOrganisation,
        });

        massnahmeFilterService.reset();

        expect(massnahmeFilterService.erweiterterFilter.organisation).toBeNull();
      }));
    });
  });
});
