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

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OrtsSucheService } from 'src/app/karte/services/orts-suche.service';
import { OrtsSucheResult } from 'src/app/shared/models/orts-suche-result';

describe('OrtsSucheService', () => {
  let service: OrtsSucheService;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    await TestBed.compileComponents();
    service = TestBed.inject(OrtsSucheService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  ['', null, undefined].forEach(str => {
    it(`should give empty list if invalid, empty searchterm: ${str}`, (done: DoneFn) => {
      service.sucheOrt('').subscribe((ortsSucheResult: OrtsSucheResult[]) => {
        expect(ortsSucheResult).toEqual([]);
        done();
      });
    });
  });
});
