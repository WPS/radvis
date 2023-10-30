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
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { first, take } from 'rxjs/operators';
import { AbstractQueryParams } from 'src/app/shared/models/abstract-query-params';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

describe(InfrastrukturenSelektionService.name, () => {
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;

  let activatedRoute: ActivatedRoute;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [],
      imports: [RouterTestingModule],
      providers: [
        InfrastrukturenSelektionService,
        {
          provide: InfrastrukturToken,
          useValue: [MASSNAHMEN, FAHRRADROUTE],
        },
      ],
    }).compileComponents();
  });

  beforeEach(fakeAsync(() => {
    infrastrukturenSelektionService = TestBed.inject(InfrastrukturenSelektionService);
    activatedRoute = TestBed.inject(ActivatedRoute);
  }));

  describe('selection & deselection', () => {
    it('should hold active infrastrukturen ', done => {
      infrastrukturenSelektionService.selectInfrastrukturen(MASSNAHMEN);
      setTimeout(() => infrastrukturenSelektionService.selectInfrastrukturen(FAHRRADROUTE));
      setTimeout(() => infrastrukturenSelektionService.deselectInfrastrukturen(FAHRRADROUTE));

      let i = 0;
      const expected = [[], [MASSNAHMEN], [MASSNAHMEN, FAHRRADROUTE], [MASSNAHMEN]];
      infrastrukturenSelektionService.selektierteInfrastrukturen$.pipe(take(4)).subscribe(
        selectedInfrastrukturen => {
          expect(selectedInfrastrukturen).toEqual(expected[i++]);
        },
        () => {},
        () => done()
      );
    });

    it('should not select the same Infrastruktur twice', done => {
      infrastrukturenSelektionService.selectInfrastrukturen(MASSNAHMEN);
      setTimeout(() => infrastrukturenSelektionService.selectInfrastrukturen(MASSNAHMEN));
      setTimeout(() => infrastrukturenSelektionService.selectInfrastrukturen(FAHRRADROUTE));
      infrastrukturenSelektionService.selektierteInfrastrukturen$
        .pipe(first(selectedInfrastrukturen => selectedInfrastrukturen.includes(FAHRRADROUTE)))
        .subscribe(selectedInfrastrukturen => {
          expect(selectedInfrastrukturen).toEqual([MASSNAHMEN, FAHRRADROUTE]);
          expect(activatedRoute.snapshot.queryParams.infrastrukturen).toEqual(
            AbstractQueryParams.listToParam([MASSNAHMEN.pathElement, FAHRRADROUTE.pathElement])
          );
          done();
        });
    });

    it('should only update once when deselected twice', fakeAsync(() => {
      let i = 0;
      infrastrukturenSelektionService.selektierteInfrastrukturen$.subscribe(() => {
        i++;
      });
      tick();
      expect(i).toBe(1);
      infrastrukturenSelektionService.selectInfrastrukturen(MASSNAHMEN);
      tick();
      expect(i).toBe(2);
      infrastrukturenSelektionService.deselectInfrastrukturen(MASSNAHMEN);
      tick();
      expect(i).toBe(3);
      infrastrukturenSelektionService.deselectInfrastrukturen(MASSNAHMEN);
      tick();
      expect(i).toBe(3);
    }));

    it('should open Tabelle, wenn infrastruktur is selected', done => {
      let i = 0;
      const expectedValues = [false, true];
      infrastrukturenSelektionService.tabellenVisible$.pipe(take(2)).subscribe(
        visible => {
          expect(visible).toEqual(expectedValues[i++]);
        },
        () => {},
        () => done()
      );
      infrastrukturenSelektionService.selectInfrastrukturen(MASSNAHMEN);
    });
  });

  describe('isSelected', () => {
    it('should return correct values for isSelected', fakeAsync(() => {
      infrastrukturenSelektionService.selectInfrastrukturen(MASSNAHMEN);
      tick();
      infrastrukturenSelektionService.selectInfrastrukturen(FAHRRADROUTE);
      tick();
      infrastrukturenSelektionService.deselectInfrastrukturen(FAHRRADROUTE);

      tick();

      expect(infrastrukturenSelektionService.isSelected(MASSNAHMEN)).toBeTrue();
      expect(infrastrukturenSelektionService.isSelected(FAHRRADROUTE)).toBeFalse();
    }));
  });
});
