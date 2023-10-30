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

import { FastMath } from 'src/app/shared/services/fast-math';

describe('FastMath', () => {
  it('should calculate mod', () => {
    expect(FastMath.mod(1337, 2)).toEqual(1);
    expect(FastMath.mod(42, 3)).toEqual(0);
    // JS Berechnungen sind ungenau, jedoch genau genug
    expect(FastMath.mod(12.345, 4)).toEqual(0.34500000000000064);
    expect(FastMath.mod(-42, 5)).toEqual(3);
    expect(FastMath.mod(-999999, 6)).toEqual(3);
    expect(FastMath.mod(0, 1)).toEqual(0);
    // TODO ist NaN ok, oder sollte das auch 0 sein? Sollte jedenfalls unwahrscheinlich sein.
    expect(FastMath.mod(0, 0)).toEqual(NaN);
  });

  it('should calculate sin', () => {
    expect(FastMath.sin(1337)).toEqual(-0.968147622011753);
    expect(FastMath.sin(42)).toEqual(-0.9170600997280978);
    expect(FastMath.sin(12.345)).toEqual(-0.21643952646443776);
    expect(FastMath.sin(0)).toEqual(0.008726535627286267);
    expect(FastMath.sin(-42)).toEqual(0.9170600627160457);
    expect(FastMath.sin(-999999)).toEqual(0.9799247086560378);
  });

  it('should calculate cos', () => {
    expect(FastMath.cos(1337)).toEqual(0.2503800076739582);
    expect(FastMath.cos(42)).toEqual(-0.3987489893599173);
    expect(FastMath.cos(12.345)).toEqual(0.9762960114448669);
    expect(FastMath.cos(0)).toEqual(0.9999619228605459);
    expect(FastMath.cos(-42)).toEqual(-0.3987491170427614);
    expect(FastMath.cos(-999999)).toEqual(0.1993678918442603);
  });

  it('should get DirectionVector', () => {
    expect(FastMath.getDirectionVector(42, 12.345, 1337, 9999)).toEqual([0.12474807417484365, 0.962020060771099]);
    // TODO ist [NaN, NaN] ok, oder sollten wir da z.B. [0, 0] zurückgeben? Sollte sehr unwahrscheinlich sein.
    expect(FastMath.getDirectionVector(42, 42, 42, 42)).toEqual([NaN, NaN]);
  });

  it('should calculate atan2', () => {
    expect(FastMath.atan2(1337, 3)).toEqual(1.568552523694242);
    expect(FastMath.atan2(42, 4)).toEqual(1.4757995165611146);
    expect(FastMath.atan2(12.345, 5)).toEqual(1.1835606062430981);
    expect(FastMath.atan2(0, 0)).toEqual(0);
    expect(FastMath.atan2(0, 6)).toEqual(0);
    expect(FastMath.atan2(-42, 7)).toEqual(-1.405415975137817);
    expect(FastMath.atan2(-999999, 8)).toEqual(-1.5707883499920001);
  });
});
