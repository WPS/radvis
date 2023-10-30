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
import { LineString, Point } from 'ol/geom';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import invariant from 'tiny-invariant';

export class LineStringOperations {
  public static getSubLineString(
    lineString: LineString,
    linearReferenzierterAbschnitt: LinearReferenzierterAbschnitt
  ): LineString {
    invariant(lineString.getCoordinates().length >= 2, 'Kein valider LineString');
    invariant(
      linearReferenzierterAbschnitt.von >= 0 && linearReferenzierterAbschnitt.von <= 1,
      'Kein valider von-Wert'
    );
    invariant(
      linearReferenzierterAbschnitt.bis >= 0 && linearReferenzierterAbschnitt.bis <= 1,
      'Kein valider bis-Wert'
    );
    invariant(
      linearReferenzierterAbschnitt.von < linearReferenzierterAbschnitt.bis,
      'Keine valider linear-referenzierter Abschnitt'
    );

    const lineStringFractions = lineString
      .getCoordinates()
      .map(c => this.getFractionOfPointOnLineString(c, lineString));
    const startOfSubLineString = lineString.getCoordinateAt(linearReferenzierterAbschnitt.von);
    const endOfSubLineString = lineString.getCoordinateAt(linearReferenzierterAbschnitt.bis);
    const subLineStringCoordinates: Coordinate[] = [startOfSubLineString];
    for (let i = 0; i < lineStringFractions.length - 1; i++) {
      // Liegt Stützpunkt innerhalb des lin. ref. Bereichs der Kante?
      if (
        lineStringFractions[i] > linearReferenzierterAbschnitt.von &&
        lineStringFractions[i] < linearReferenzierterAbschnitt.bis
      ) {
        subLineStringCoordinates.push(lineString.getCoordinates()[i]);
      }
    }
    subLineStringCoordinates.push(endOfSubLineString);
    return new LineString(subLineStringCoordinates);
  }

  public static splitLinestring(segmentierung: number[], geometry: LineString): LineString[] {
    const result: LineString[] = [];
    const lineStringFractions = geometry.getCoordinates().map(c => this.getFractionOfPointOnLineString(c, geometry));
    for (let i = 0; i < segmentierung.length - 1; i++) {
      const startFraction = segmentierung[i];
      const endFraction = segmentierung[i + 1];
      const pointsBetween: Coordinate[] = [];
      // eslint-disable-next-line @typescript-eslint/prefer-for-of
      for (let j = 0; j < lineStringFractions.length; j++) {
        if (lineStringFractions[j] >= startFraction && lineStringFractions[j] <= endFraction) {
          pointsBetween.push(geometry.getCoordinates()[j]);
        }
      }
      const startPoint = geometry.getCoordinateAt(segmentierung[i]);
      const endPoint = geometry.getCoordinateAt(segmentierung[i + 1]);
      result.push(new LineString([startPoint, ...pointsBetween, endPoint]));
    }
    return result;
  }

  public static getFractionOfPointOnLineString(coordinate: Coordinate, linestring: LineString): number {
    const snappedCoordinates: Coordinate = linestring.getClosestPoint(coordinate);
    let currentDistance = 0;
    let resultDistance = 0;
    const lineStringCoordinates = linestring.getCoordinates();
    for (let i = 0; i < lineStringCoordinates.length - 1; i++) {
      const start = lineStringCoordinates[i];
      const end = lineStringCoordinates[i + 1];
      const segment = new LineString([start, end]);
      const snapTestCoordinate = segment.getClosestPoint(snappedCoordinates);
      if (new LineString([snapTestCoordinate, snappedCoordinates]).getLength() < 0.0001) {
        const subsegment = new LineString([start, snappedCoordinates]);
        resultDistance = currentDistance + subsegment.getLength();
        return resultDistance / linestring.getLength();
      } else {
        currentDistance += segment.getLength();
      }
    }
    throw new Error('Could not compute segment fraction');
  }

  public static getSegmentOfPointOnLineString(point: Point, lineString: LineString): LineString {
    const snappedCoordinates: Coordinate = lineString.getClosestPoint(point.getCoordinates());
    const lineStringCoordinates = lineString.getCoordinates();
    for (let i = 0; i < lineStringCoordinates.length - 1; i++) {
      const start = lineStringCoordinates[i];
      const end = lineStringCoordinates[i + 1];
      const segment = new LineString([start, end]);
      const snapTestCoordinate = segment.getClosestPoint(snappedCoordinates);
      if (new LineString([snapTestCoordinate, snappedCoordinates]).getLength() < 0.0001) {
        return new LineString([start, end]);
      }
    }
    throw new Error('Error while determining segment of point');
  }

  public static getPerpendicularAngleOfLine(line: LineString): number {
    const start = line.getCoordinates()[0];
    const end = line.getCoordinates()[1];
    const dx = end[0] - start[0];
    const dy = end[1] - start[1];
    return -Math.atan2(dy, dx) + Math.PI / 2;
  }
}
