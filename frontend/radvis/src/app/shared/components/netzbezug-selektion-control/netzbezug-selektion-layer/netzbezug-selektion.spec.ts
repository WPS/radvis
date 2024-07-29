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

import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { NetzbezugSelektion } from 'src/app/shared/components/netzbezug-selektion-control/netzbezug-selektion-layer/netzbezug-selektion';
import { KantenSeitenbezug } from 'src/app/shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';

describe(NetzbezugSelektion.name, () => {
  describe('kante Schneiden', () => {
    let netzbezugSelektion: NetzbezugSelektion;
    const kanteId = 345;
    const geometrie = {
      coordinates: [
        [0, 0],
        [0, 100],
      ],
      type: 'LineString',
    } as LineStringGeojson;

    describe('Beidseitig', () => {
      beforeEach(() => {
        netzbezugSelektion = new NetzbezugSelektion();
        netzbezugSelektion.selectKante(kanteId, geometrie);
      });

      it('should insert in correct segment', () => {
        netzbezugSelektion.kanteSchneiden(kanteId, [0, 50]);
        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toEqual([
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            kantenSeite: KantenSeitenbezug.BEIDSEITIG,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            kantenSeite: KantenSeitenbezug.BEIDSEITIG,
          },
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung()).toEqual([0, 0.5, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([0, 1]);

        netzbezugSelektion.kanteSchneiden(kanteId, [0, 75]);
        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toEqual([
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            kantenSeite: KantenSeitenbezug.BEIDSEITIG,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 },
            kantenSeite: KantenSeitenbezug.BEIDSEITIG,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.75, bis: 1 },
            kantenSeite: KantenSeitenbezug.BEIDSEITIG,
          },
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung()).toEqual([0, 0.5, 0.75, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([0, 1, 2]);
      });
    });

    describe('Links selektiert - links schneiden', () => {
      beforeEach(() => {
        netzbezugSelektion = new NetzbezugSelektion();
        netzbezugSelektion.selectKante(kanteId, geometrie, KantenSeite.LINKS);
      });

      it('should insert in correct segment', () => {
        netzbezugSelektion.kanteSchneiden(kanteId, [0, 50], KantenSeite.LINKS);
        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toEqual([
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            kantenSeite: KantenSeitenbezug.LINKS,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            kantenSeite: KantenSeitenbezug.LINKS,
          },
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual([0, 0.5, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0, 1]);
      });
    });

    describe('Beide Seiten selektiert - erst Links schneiden, dann nochmal links und rechts - Kombiniert gleiche segmente zu Beidseitig', () => {
      beforeEach(() => {
        netzbezugSelektion = new NetzbezugSelektion();
        netzbezugSelektion.selectKante(kanteId, geometrie);
      });

      it('should insert in correct segment', () => {
        netzbezugSelektion.kanteSchneiden(kanteId, [0, 50], KantenSeite.LINKS);
        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toEqual([
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            kantenSeite: KantenSeitenbezug.LINKS,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            kantenSeite: KantenSeitenbezug.LINKS,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.0, bis: 1 },
            kantenSeite: KantenSeitenbezug.RECHTS,
          },
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual([0, 0.5, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0, 1]);

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.RECHTS)).toEqual([0, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.RECHTS)).toEqual([0]);

        netzbezugSelektion.kanteSchneiden(kanteId, [0, 75], KantenSeite.LINKS);
        netzbezugSelektion.kanteSchneiden(kanteId, [0, 75], KantenSeite.RECHTS);
        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toEqual([
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            kantenSeite: KantenSeitenbezug.LINKS,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 },
            kantenSeite: KantenSeitenbezug.LINKS,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.0, bis: 0.75 },
            kantenSeite: KantenSeitenbezug.RECHTS,
          },
          {
            kanteId,
            geometrie,
            linearReferenzierterAbschnitt: { von: 0.75, bis: 1 },
            kantenSeite: KantenSeitenbezug.BEIDSEITIG,
          },
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual([
          0, 0.5, 0.75, 1,
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0, 1, 2]);

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.RECHTS)).toEqual([
          0, 0.75, 1.0,
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.RECHTS)).toEqual([0, 1]);
      });
    });
  });

  describe('from netzbezug', () => {
    describe('Netzbezug komplett beidseitig', () => {
      it('should add segments and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung()).toEqual([0, 0.5, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([0, 1]);
      });

      it('should add partial kante and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } }],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung()).toEqual([0, 0.5, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([0]);
      });

      it('should add middle segment and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 } }],
        });

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung()).toEqual([0, 0.3, 0.7, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([1]);
      });

      it('should fill gaps and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung()).toEqual([0, 0.3, 0.7, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([0, 2]);
      });
    });

    describe('Netzbezug enthaelt nur eine Seite', () => {
      it('should add segments and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual([0, 0.5, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0, 1]);
      });
    });

    describe('Netzbezug enthaelt auf beiden Seiten unterschiedliche Kanten', () => {
      it('should add segments and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.0, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual([0, 0.5, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0, 1]);

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.RECHTS)).toEqual([0, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.RECHTS)).toEqual([0]);
      });

      it('should add partial kante and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1.0 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual([0, 0.5, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0]);

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.RECHTS)).toEqual([0, 0.7, 1]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.RECHTS)).toEqual([1]);
      });

      it('should add middle segment and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.2, bis: 0.6 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual([
          0, 0.3, 0.7, 1,
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([1]);

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.RECHTS)).toEqual([
          0, 0.2, 0.6, 1,
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.RECHTS)).toEqual([1]);
      });

      it('should fill gaps and recalculate segmentierung', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.2 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.6, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual([
          0, 0.3, 0.7, 1,
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0, 2]);

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.RECHTS)).toEqual([
          0, 0.2, 0.6, 1,
        ]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.RECHTS)).toEqual([0, 2]);
      });
    });
  });

  describe('toNetzbezug', () => {
    describe('Netzbezug komplett Beidseitig', () => {
      it('should map abschnitte correct', () => {
        const expectedNetzbezug = {
          ...defaultNetzbezug,
          kantenBezug: [
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
          ],
        };
        const netzbezugSelektion = new NetzbezugSelektion(expectedNetzbezug);

        expect(netzbezugSelektion.toNetzbezug()).toEqual(expectedNetzbezug);
      });
    });

    describe('Netzbezug nur linke Seite', () => {
      it('should map abschnitte correct', () => {
        const expectedNetzbezug = {
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
          ],
        };
        const netzbezugSelektion = new NetzbezugSelektion(expectedNetzbezug);

        expect(netzbezugSelektion.toNetzbezug()).toEqual(expectedNetzbezug);
      });
    });

    describe('Netzbezug Seitenbezogen unterschiedlich', () => {
      it('should map abschnitte correct', () => {
        const expectedNetzbezug = {
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.4 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.6, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        };
        const netzbezugSelektion = new NetzbezugSelektion(expectedNetzbezug);

        expect(netzbezugSelektion.toNetzbezug()).toEqual(expectedNetzbezug);
      });
    });
  });

  describe('deselect segment', () => {
    describe('deselect beidseitige Kante', () => {
      it('should remove entire kante, if singular segment', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 1 } }],
        });
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 0);

        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toHaveSize(0);
      });

      it('should remove entire kante if last segment is selected', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 } }],
        });
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 1);

        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toHaveSize(0);
      });

      it('should deselect and recalculate indices', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
          ],
        });
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 0);

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([2]);
        expect(netzbezugSelektion.toNetzbezug()).toEqual({
          ...defaultNetzbezug,
          kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } }],
        });
      });
    });
    describe('deselect nur einseitige Kante (links)', () => {
      it('should remove entire kante, if singular segment', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
          ],
        });
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 0, KantenSeite.LINKS);

        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toHaveSize(0);
      });

      it('should remove entire kante if last segment is selected', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
          ],
        });
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 1, KantenSeite.LINKS);

        expect(netzbezugSelektion.toNetzbezug().kantenBezug).toHaveSize(0);
      });

      it('should deselect and recalculate indices', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
          ],
        });
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 0, KantenSeite.LINKS);

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([2]);
        expect(netzbezugSelektion.toNetzbezug()).toEqual({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
          ],
        });
      });
    });
    describe('deselect links rechts gemischt', () => {
      it('should remove left segment', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 0, KantenSeite.LINKS);

        expect(netzbezugSelektion.toNetzbezug()).toEqual({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
      });
      it('should remove part of left segment and right segment', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 0, KantenSeite.LINKS);
        netzbezugSelektion.deselectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 0, KantenSeite.RECHTS);

        expect(netzbezugSelektion.toNetzbezug()).toEqual({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
          ],
        });
      });
    });
  });

  describe('select segment', () => {
    describe('netzbezug komplett Seitenbezug:beidseitig', () => {
      it('should select and recalculate indices', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
          ],
        });
        netzbezugSelektion.selectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 1);

        expect(netzbezugSelektion.toNetzbezug()).toEqual({
          ...defaultNetzbezug,
          kantenBezug: [
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 } },
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([0, 1, 2]);
      });
    });
    describe('netzbezug Seitenbezug:gemischt', () => {
      it('should select left segment and recalculate indices', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        netzbezugSelektion.selectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 1, KantenSeite.LINKS);

        expect(netzbezugSelektion.toNetzbezug()).toEqual({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.3, bis: 1.0 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1.0 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0, 1]);
      });

      it('should select right segment and recalculate indices', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        netzbezugSelektion.selectSegment(netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId, 0, KantenSeite.RECHTS);

        expect(netzbezugSelektion.toNetzbezug()).toEqual({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.0, bis: 0.7 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1.0 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.RECHTS)).toEqual([0, 1]);
      });
    });
  });

  describe('update segmentierung', () => {
    describe('netzbezug komplett seitenbezug:beidseitig', () => {
      it('should correct schnittmarken and recalculate indices', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
            { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
          ],
        });
        const expectedSegmentierung = [0, 0.2, 0.5, 1];
        netzbezugSelektion.updateSegmentierung(
          netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId,
          expectedSegmentierung
        );
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices()).toEqual([0, 2]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung()).toEqual(expectedSegmentierung);
      });
    });

    describe('netzbezug seitenbezug gemischt', () => {
      it('should correct schnittmarken and recalculate indices', () => {
        const netzbezugSelektion = new NetzbezugSelektion({
          ...defaultNetzbezug,
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
              kantenSeite: KantenSeitenbezug.LINKS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0, bis: 0.1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
            {
              ...defaultNetzbezug.kantenBezug[0],
              linearReferenzierterAbschnitt: { von: 0.8, bis: 1 },
              kantenSeite: KantenSeitenbezug.RECHTS,
            },
          ],
        });
        const expectedSegmentierungLinks = [0, 0.2, 0.5, 1];
        netzbezugSelektion.updateSegmentierung(
          netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId,
          expectedSegmentierungLinks,
          KantenSeite.LINKS
        );
        const expectedSegmentierungRechts = [0, 0.3, 0.7, 1];
        netzbezugSelektion.updateSegmentierung(
          netzbezugSelektion.kantenSeitenAbschnitte[0].kanteId,
          expectedSegmentierungRechts,
          KantenSeite.RECHTS
        );
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.LINKS)).toEqual([0, 2]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.LINKS)).toEqual(
          expectedSegmentierungLinks
        );

        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSelectedIndices(KantenSeite.RECHTS)).toEqual([0, 2]);
        expect(netzbezugSelektion.kantenSeitenAbschnitte[0].getSegmentierung(KantenSeite.RECHTS)).toEqual(
          expectedSegmentierungRechts
        );
      });
    });
  });
});
