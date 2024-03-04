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

import { Coordinate } from 'ol/coordinate';
import { LineString } from 'ol/geom';
import { FastMath } from 'src/app/shared/services/fast-math';

/**
 * Die Funktionen in dieser Datei wurden aus BIS2 übernommen und weichen daher ggf. etwas von den RadVIS-Konventionen ab
 */

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace LineStringShifter {
  // eslint-disable-next-line prefer-arrow-functions/prefer-arrow-functions
  export function shiftLineStringByPixel(
    lineString: LineString,
    shiftDistanceInPixel: number,
    resolution: number
  ): LineString {
    const shiftDistanceInMeter = shiftDistanceInPixel * resolution;
    return shiftLineStringByMeter(lineString, shiftDistanceInMeter);
  }

  /**
   * Gibt einen LineString zurück, welcher um die angegebene Anzahl an Pixeln im rechten Winkel zur Linienrichtung
   * nach rechts verschoben ist.
   *
   * Diese Funktion ist stark für den IE 11 optimiert und daher schlechter lesbar. Die IE 11 JavaScript Engine macht
   * leider viele Optimierungen nicht, wie z.B. Inlining, nicht selbst, sodass wir sie manuell machen müssen.
   */
  // eslint-disable-next-line prefer-arrow-functions/prefer-arrow-functions
  export function shiftLineStringByMeter(lineString: LineString, shiftDistanceInMeter: number): LineString {
    const coords = lineString.getCoordinates();
    const coordsLength = coords.length;

    // Den Array vorzuinitialisieren und per Index-Assign statt push zu befüllen hatte die Performance eher verschlechtert.
    const resultCoords: Coordinate[] = [];
    let prevX = 0;
    let prevY = 0;
    let currentX = coords[0][0];
    let currentY = coords[0][1];
    for (let index = 0; index < coordsLength - 1; index++) {
      const nextPos = coords[index + 1];
      const nextX = nextPos[0];
      const nextY = nextPos[1];

      if (index === 0) {
        // Erster Punkt des Linienzugs
        const direction = FastMath.getDirectionVector(currentX, currentY, nextX, nextY);
        resultCoords.push([
          currentX + direction[1] * shiftDistanceInMeter,
          currentY + -direction[0] * shiftDistanceInMeter,
        ]);
      } else {
        // Ein innerer Punkt des Linienzugs
        const previousDirection = FastMath.getDirectionVector(currentX, currentY, prevX, prevY);
        const nextDirection = FastMath.getDirectionVector(currentX, currentY, nextX, nextY);
        const prevDirX = previousDirection[0];
        const prevDirY = previousDirection[1];

        let angle = FastMath.atan2(nextDirection[1], nextDirection[0]) - FastMath.atan2(prevDirY, prevDirX);
        if (angle < 0) {
          angle += FastMath.RAD_FULL;
        }
        angle = angle / 2;

        const cos = FastMath.cos(angle);
        const sin = FastMath.sin(angle);
        resultCoords.push([
          currentX + (cos * prevDirX - sin * prevDirY) * shiftDistanceInMeter,
          currentY + (sin * prevDirX + cos * prevDirY) * shiftDistanceInMeter,
        ]);
      }

      prevX = currentX;
      prevY = currentY;
      currentX = nextX;
      currentY = nextY;
    }

    // Letzter Punkt des Linienzugs
    if (prevX != null) {
      const direction = FastMath.getDirectionVector(prevX, prevY, currentX, currentY);
      resultCoords.push([
        currentX + direction[1] * shiftDistanceInMeter,
        currentY + -direction[0] * shiftDistanceInMeter,
      ]);
    }

    return new LineString(resultCoords);
  }
}
