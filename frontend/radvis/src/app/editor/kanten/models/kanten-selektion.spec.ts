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

/* eslint-disable @typescript-eslint/dot-notation */
import { FuehrungsformAttributGruppe } from 'src/app/editor/kanten/models/fuehrungsform-attribut-gruppe';
import { FuehrungsformAttribute } from 'src/app/editor/kanten/models/fuehrungsform-attribute';
import { Kante } from 'src/app/editor/kanten/models/kante';
import {
  defaultFuehrungsformAttribute,
  defaultKante,
  defaultZustaendigkeitAttribute,
} from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { ZustaendigkeitAttribute } from 'src/app/editor/kanten/models/zustaendigkeit-attribute';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';

describe('KantenSelektion', () => {
  describe('of-creation', () => {
    describe('ofSeite', () => {
      it('should create Selektion of Seite', () => {
        const newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.LINKS);

        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeFalse();
      });

      it('should create Selektion of Seite with Segmenten', () => {
        const newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.LINKS, 5);

        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeFalse();
        expect(newKantenSelektion.istSegmentSelektiert(3, KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(3, KantenSeite.RECHTS)).toBeFalse();
      });
    });

    describe('ofGesamteKante', () => {
      it('should create Selektion of both Seiten', () => {
        const newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante);

        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeTrue();
      });

      it('should create Selektion of Seite with Segmenten', () => {
        const newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante, 2, 5);

        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(1, KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(2, KantenSeite.RECHTS)).toBeTrue();
      });

      it('should correctly order segmented attributes', () => {
        // arrange
        const zustaendigkeitAttribute: ZustaendigkeitAttribute[] = [
          {
            ...defaultZustaendigkeitAttribute,
            linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
          },
          {
            ...defaultZustaendigkeitAttribute,
            linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 },
          },
          {
            ...defaultZustaendigkeitAttribute,
            linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
          },
        ];

        const fuehrungsformAttribute: { left: FuehrungsformAttribute[]; right: FuehrungsformAttribute[] } = {
          left: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.2 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.2, bis: 0.8 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.8, bis: 1 },
            },
          ],
          right: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.25 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.25, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.75, bis: 1 },
            },
          ],
        };

        // act
        const zustaendigkeitAttributeInSelektion: ZustaendigkeitAttribute[] = KantenSelektion.ofGesamteKante(
          {
            ...defaultKante,
            zustaendigkeitAttributGruppe: {
              id: 1,
              version: 1,
              zustaendigkeitAttribute: [
                // falscher Reihenfolge ist absicht
                zustaendigkeitAttribute[1],
                zustaendigkeitAttribute[0],
                zustaendigkeitAttribute[2],
              ],
            },
          } as Kante,
          3,
          0
        ).kante.zustaendigkeitAttributGruppe.zustaendigkeitAttribute;

        const fuehrungsformAttributGruppeInSelektion: FuehrungsformAttributGruppe = KantenSelektion.ofGesamteKante(
          {
            ...defaultKante,
            fuehrungsformAttributGruppe: {
              id: 2,
              version: 1,
              fuehrungsformAttributeLinks: [
                fuehrungsformAttribute.left[2],
                fuehrungsformAttribute.left[0],
                fuehrungsformAttribute.left[1],
              ],
              fuehrungsformAttributeRechts: [
                fuehrungsformAttribute.right[3],
                fuehrungsformAttribute.right[1],
                fuehrungsformAttribute.right[0],
                fuehrungsformAttribute.right[2],
              ],
            },
          } as Kante,
          3,
          4
        ).kante.fuehrungsformAttributGruppe;

        // assert
        expect(zustaendigkeitAttributeInSelektion).toEqual(zustaendigkeitAttribute);
        expect(fuehrungsformAttributGruppeInSelektion.fuehrungsformAttributeLinks).toEqual(fuehrungsformAttribute.left);
        expect(fuehrungsformAttributGruppeInSelektion.fuehrungsformAttributeRechts).toEqual(
          fuehrungsformAttribute.right
        );
      });
    });

    describe('ofSegment', () => {
      it('should create Selektion of Segment without Seitenbezug', () => {
        const newKantenSelektion = createSelektionOfSegment(defaultKante, 2);

        expect(newKantenSelektion.istSegmentSelektiert(2)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(1)).toBeFalse();
      });

      it('should create Selektion of Segment with Seitenbezug', () => {
        const newKantenSelektion = createSelektionOfSegment(defaultKante, 2, KantenSeite.LINKS);

        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeFalse();
        expect(newKantenSelektion.istSegmentSelektiert(2, KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(1, KantenSeite.LINKS)).toBeFalse();
        expect(newKantenSelektion.istSegmentSelektiert(2, KantenSeite.RECHTS)).toBeFalse();
      });
    });
  });

  describe('with-creation', () => {
    describe('withSeiteSelektiert', () => {
      it('should select seite and deselect the other', () => {
        let newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.LINKS);

        newKantenSelektion = newKantenSelektion.selectSeite(KantenSeite.RECHTS, 3);

        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeFalse();
        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeTrue();
      });
    });

    describe('withZusaetzlicherSeiteSelektiert', () => {
      it('should select zusaetzliche seite', () => {
        let newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.LINKS);

        newKantenSelektion = newKantenSelektion.selectSeite(KantenSeite.RECHTS, 3, true);

        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeTrue();
      });
    });

    describe('withSegmentSelektiert', () => {
      it('should select segment and deselect the all other', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2);

        newKantenSelektion = newKantenSelektion.selectSegment(0);

        expect(newKantenSelektion.istSegmentSelektiert(2)).toBeFalse();
        expect(newKantenSelektion.istSegmentSelektiert(0)).toBeTrue();
      });

      it('should select segment and deselect the all other with seitenbezug', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2, KantenSeite.LINKS);
        newKantenSelektion = newKantenSelektion.selectSegment(0, KantenSeite.RECHTS, true);

        newKantenSelektion = newKantenSelektion.selectSegment(0, KantenSeite.LINKS);

        expect(newKantenSelektion.istSegmentSelektiert(2, KantenSeite.LINKS)).toBeFalse();
        expect(newKantenSelektion.istSegmentSelektiert(0, KantenSeite.RECHTS)).toBeFalse();
        expect(newKantenSelektion.istSegmentSelektiert(0, KantenSeite.LINKS)).toBeTrue();
      });
    });

    describe('withZusaetzlichenSegmentSelektiert', () => {
      it('should select additional segment', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2);

        newKantenSelektion = newKantenSelektion.selectSegment(0, undefined, true);

        expect(newKantenSelektion.istSegmentSelektiert(2)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(0)).toBeTrue();
      });

      it('should select additional segment with seitenbezug', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2, KantenSeite.LINKS);
        newKantenSelektion = newKantenSelektion.selectSegment(0, KantenSeite.RECHTS, true);

        newKantenSelektion = newKantenSelektion.selectSegment(0, KantenSeite.LINKS, true);

        expect(newKantenSelektion.istSegmentSelektiert(2, KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(0, KantenSeite.RECHTS)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(0, KantenSeite.LINKS)).toBeTrue();
      });
    });

    describe('reduceSegmentAnzahl', () => {
      it('should cap selected indices above maximum', () => {
        let newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante, 5, 5);

        newKantenSelektion = newKantenSelektion.reduceSegmentAnzahl(3);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0, 1, 2]);
        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0, 1, 2]);
      });

      it('should cap selected indices above maximum seitenbezogen', () => {
        let newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante, 7, 9);

        newKantenSelektion = newKantenSelektion.reduceSegmentAnzahl(2, 4);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0, 1]);
        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0, 1, 2, 3]);
      });

      it('should let at least one element selected', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 3);

        newKantenSelektion = newKantenSelektion.reduceSegmentAnzahl(2);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0]);
        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0]);
      });

      it('should let at least one element selected seitenbezogen', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 3, KantenSeite.LINKS);

        newKantenSelektion = newKantenSelektion.reduceSegmentAnzahl(1, 2);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0]);
        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([]);
      });
    });

    describe('withEingefuegtemSegment', () => {
      it('should shift indices correctly when original segment was selected', () => {
        let newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante, 3, 3);

        newKantenSelektion = newKantenSelektion.insertSegment(1);

        expect(newKantenSelektion.getSelectedSegmentIndices()).toEqual([1, 0, 2, 3]);
      });

      it('should shift indices correctly when original segment was not selected', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2);

        newKantenSelektion = newKantenSelektion.insertSegment(1);

        expect(newKantenSelektion.getSelectedSegmentIndices()).toEqual([3]);
      });

      it('should shift indices correctly when original segment was selected with seitenbezug', () => {
        let newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.RECHTS, 3);

        newKantenSelektion = newKantenSelektion.insertSegment(1, KantenSeite.RECHTS);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([1, 0, 2, 3]);
      });

      it('should shift indices correctly when original segment was not selected with seitenbezug', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2, KantenSeite.LINKS);

        newKantenSelektion = newKantenSelektion.insertSegment(1, KantenSeite.LINKS);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([3]);
      });
    });

    describe('withGeloeschtemSegment', () => {
      it('should shift indices correctly when original segment was selected', () => {
        let newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante, 3, 3);

        newKantenSelektion = newKantenSelektion.deleteSegment(1);

        expect(newKantenSelektion.getSelectedSegmentIndices()).toEqual([0, 1]);
      });

      it('should shift indices correctly when original segment was not selected', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2);

        newKantenSelektion = newKantenSelektion.deleteSegment(1);

        expect(newKantenSelektion.getSelectedSegmentIndices()).toEqual([1]);
      });

      it('should shift indices correctly when original segment was selected with seitenbezug', () => {
        let newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.RECHTS, 3);

        newKantenSelektion = newKantenSelektion.deleteSegment(1, KantenSeite.RECHTS);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0, 1]);
      });

      it('should shift indices correctly when original segment was not selected with seitenbezug', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2, KantenSeite.LINKS);

        newKantenSelektion = newKantenSelektion.deleteSegment(1, KantenSeite.LINKS);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([1]);
      });
    });

    describe('withoutSeiteSelektiert', () => {
      it('should deselect Seite', () => {
        let newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante);

        newKantenSelektion = newKantenSelektion.deselectSeite(KantenSeite.RECHTS);

        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeFalse();
      });
    });

    describe('withoutSegmentSelektiert', () => {
      it('should deselect Segment', () => {
        let newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante, 3, 3);

        newKantenSelektion = newKantenSelektion.deselectSegment(1);

        expect(newKantenSelektion.istSegmentSelektiert(0)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(2)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(1)).toBeFalse();
      });

      it('should deselect Segment with Seitenbezug', () => {
        let newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.LINKS, 3);

        newKantenSelektion = newKantenSelektion.deselectSegment(1, KantenSeite.LINKS);

        expect(newKantenSelektion.istSegmentSelektiert(0, KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(2, KantenSeite.LINKS)).toBeTrue();
        expect(newKantenSelektion.istSegmentSelektiert(1, KantenSeite.LINKS)).toBeFalse();
      });
    });

    describe('withUpdatedKante', () => {
      it('should update', () => {
        let newKantenSelektion = KantenSelektion.ofGesamteKante(defaultKante);
        const newKante = { ...defaultKante, laengeBerechnet: 5 };

        newKantenSelektion = newKantenSelektion.updateKante(newKante);

        expect(newKantenSelektion.kante.laengeBerechnet).toBe(5);
      });
    });

    describe('withGesamteKanteSelektiert', () => {
      it('should select gesamte Kante', () => {
        let newKantenSelektion = createSelektionOfSegment(defaultKante, 2, KantenSeite.LINKS);

        newKantenSelektion = newKantenSelektion.selectGesamteKante(2, 3);

        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0, 1]);
        expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0, 1, 2]);
      });
    });
  });

  describe('getter', () => {
    it('should get correct information', () => {
      const newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.LINKS);

      expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
      expect(newKantenSelektion.istSeiteSelektiert(KantenSeite.RECHTS)).toBeFalse();
      expect(newKantenSelektion.istBeidseitigSelektiert()).toBeFalse();
    });

    it('getSelectedSegmentIndices should get selected indices without Seitenbezug', () => {
      let newKantenSelektion = createSelektionOfSegment(defaultKante, 2);
      newKantenSelektion = newKantenSelektion.selectSegment(0, undefined, true);

      expect(newKantenSelektion.getSelectedSegmentIndices()).toEqual([2, 0]);
    });

    it('getSelectedSegmentIndices should get selected indices with Seitenbezug', () => {
      let newKantenSelektion = KantenSelektion.ofSeite(defaultKante, KantenSeite.LINKS, 3);
      newKantenSelektion = newKantenSelektion.selectSegment(2, KantenSeite.RECHTS, true);

      expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0, 1, 2]);
      expect(newKantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([2]);
    });
  });
});

// eslint-disable-next-line prefer-arrow-functions/prefer-arrow-functions
function createSelektionOfSegment(kante: Kante, segmentIndex: number, kantenSeite?: KantenSeite): KantenSelektion {
  const basicSelektion = KantenSelektion.ofGesamteKante(kante);
  return basicSelektion.selectSegment(segmentIndex, kantenSeite);
}
