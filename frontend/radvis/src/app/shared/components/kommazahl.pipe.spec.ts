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

import { KommazahlPipe } from 'src/app/shared/components/kommazahl.pipe';

describe('KommazahlPipe', () => {
  let pipe: KommazahlPipe;

  beforeEach(() => {
    pipe = new KommazahlPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should append zeros and unit', () => {
    expect(pipe.transform(123, 'm')).toEqual('123,00m');
  });

  it('should use comma for floats', () => {
    expect(pipe.transform(12.3, 'm')).toEqual('12,30m');
  });

  it('should round after two digits', () => {
    expect(pipe.transform(12.345, 'm')).toEqual('12,35m');
  });
});
