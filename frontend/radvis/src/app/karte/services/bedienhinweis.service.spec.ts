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

import { skip } from 'rxjs/operators';
import { BedienhinweisService } from './bedienhinweis.service';

describe(BedienhinweisService.name, () => {
  let service: BedienhinweisService;

  beforeEach(() => {
    service = new BedienhinweisService();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fire change event with correct bedienhinweis', (done: DoneFn) => {
    const expectedBedienhinweis = 'Foo bar';

    service.bedienhinweis$.pipe(skip(1)).subscribe(bedienhinweis => {
      expect(bedienhinweis).toEqual(expectedBedienhinweis);
      expect(service.getBedienhinweis()).toEqual(expectedBedienhinweis);
      done();
    });

    service.showBedienhinweis(expectedBedienhinweis);
  });

  it('should fire change event when hiding bedienhinweis', (done: DoneFn) => {
    service.bedienhinweis$.subscribe(bedienhinweis => {
      expect(bedienhinweis).toEqual(null);
      expect(service.getBedienhinweis()).toBeNull();
      done();
    });

    service.hideBedienhinweis();
  });
});
