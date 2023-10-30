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

/**
 * Die Funktionen in dieser Datei wurden aus BIS2 übernommen und weichen daher ggf. etwas von den RadVIS-Konventionen ab
 */

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FastMath {
  export const PI = 3.1415927;
  export const HALF_PI = PI / 2;
  export const RAD_FULL = PI * 2;

  const SIN_COUNT = 360;
  const RAD_TO_INDEX = SIN_COUNT / RAD_FULL;

  const SIN = [...new Array(SIN_COUNT).keys()].map(i => Math.sin(((i + 0.5) / SIN_COUNT) * RAD_FULL));

  // Gibt das Ergebnis für x modulo m zurück.
  export const mod = (x: number, m: number): number => ((x % m) + m) % m;

  // Gibt den Sinus für die Zahl zurück. Ist nur auf 1° genau, aber dafür schnell.
  export const sin = (radians: number): number => SIN[Math.trunc(mod(radians * RAD_TO_INDEX, SIN_COUNT))];

  // Gibt den Cosinus für die Zahl zurück. Ist nur auf 1° genau, aber dafür schnell.
  export const cos = (radians: number): number => SIN[Math.trunc(mod((radians + HALF_PI) * RAD_TO_INDEX, SIN_COUNT))];

  // Berechnet näherungsweise den normalisierten Richtungsvektor zwischen den beiden Punkten.
  export const getDirectionVector = (sX: number, sY: number, eX: number, eY: number): [number, number] => {
    const vX = eX - sX;
    const vY = eY - sY;
    let ratio;
    let aX;
    let aY;
    if (vX < 0) {
      aX = -vX;
    } else {
      aX = vX;
    }
    if (vY < 0) {
      aY = -vY;
    } else {
      aY = vY;
    }
    if (aX > aY) {
      ratio = 1 / aX;
    } else {
      ratio = 1 / aY;
    }
    ratio *= 1.29289 - (aX + aY) * ratio * 0.29289;
    return [vX * ratio, vY * ratio];
  };

  // Berechnet näherungsweise den atan2 in Radians. Ist deutlich schneller als Math.atan2, aber dafür auch ungenauer.
  // Der durchschnittliche Fehler liegt bei 0.00231 Radians bzw. 0.1323 Grad.
  // Der maximale Fehler liegt 0.00488 Radians bzw. 0.2796 Grad.
  export const atan2 = (y: number, x: number): number => {
    if (x === 0) {
      if (y > 0) {
        return HALF_PI;
      }
      if (y === 0) {
        return 0;
      }
      return -HALF_PI;
    }
    const z = y / x;
    let atan;
    if (Math.abs(z) < 1) {
      atan = z / (1 + 0.28 * z * z);
      if (x < 0) {
        return atan + (y < 0 ? -PI : PI);
      }
      return atan;
    }
    atan = PI / 2 - z / (z * z + 0.28);
    return y < 0 ? atan - PI : atan;
  };
}
