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

import { fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { KantenVerlaufEditorComponent } from 'src/app/editor/kanten/components/kanten-verlauf-editor/kanten-verlauf-editor.component';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { Kante } from 'src/app/editor/kanten/models/kante';
import {
  anotherKante,
  defaultFuehrungsformAttribute,
  defaultKante,
  defaultZustaendigkeitAttribute,
} from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { DiscardGuardService } from 'src/app/shared/services/discard-guard.service';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';
import { anything, capture, instance, mock, resetCalls, verify, when } from 'ts-mockito';

describe(KantenSelektionService.name, () => {
  let service: KantenSelektionService;
  let netzService: NetzService;
  let bearbeitungModusService: NetzBearbeitungModusService;
  const bearbeitungModusSubject = new BehaviorSubject<AttributGruppe | null>(null);

  let discardGuardService: DiscardGuardService;

  beforeEach(() => {
    netzService = mock(NetzService);
    bearbeitungModusService = mock(NetzBearbeitungModusService);
    discardGuardService = mock(DiscardGuardService);

    when(bearbeitungModusService.getAktiveKantenGruppe()).thenReturn(bearbeitungModusSubject);
    service = new KantenSelektionService(
      instance(netzService),
      instance(mock(LadeZustandService)),
      instance(discardGuardService),
      instance(bearbeitungModusService)
    );
    service.registerForDiscardGuard(instance(mock(KantenVerlaufEditorComponent)));
  });

  describe('selectKante', () => {
    it('should check discard', () => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));
      service.select(1, false);
      verify(discardGuardService.canDeactivate(anything())).once();
      expect(capture(discardGuardService.canDeactivate).last()[0]).toBe(service['discardableComponent']!);
    });

    it('should not change selection when cannot discard', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));
      const kanteId = 1;
      service.select(kanteId, false);
      tick();
      expect(service.selektierteKanten.map(k => k.id).includes(kanteId)).toBeFalse();
    }));

    it('should append respective replace selection', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      const kanteId1 = 1;
      const kante1 = { ...defaultKante, id: kanteId1 };
      when(netzService.getKanteForEdit(kanteId1)).thenReturn(Promise.resolve(kante1));

      const kanteId2 = 2;
      const kante2 = { ...defaultKante, id: kanteId2 };
      when(netzService.getKanteForEdit(kanteId2)).thenReturn(Promise.resolve(kante2));

      service.select(kanteId1, false);
      tick();

      expect(service.selektierteKanten).toEqual([kante1]);

      service.select(kanteId2, false);
      tick();

      expect(service.selektierteKanten).toEqual([kante2]);

      service.select(kanteId1, true);
      tick();

      expect(service.selektierteKanten).toEqual([kante2, kante1]);
    }));

    it('should set anzahlSegmente correctly', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));
      const kanteId1 = 1;
      const kante1 = {
        ...defaultKante,
        id: kanteId1,
        fuehrungsformAttributGruppe: {
          ...defaultKante.fuehrungsformAttributGruppe,
          fuehrungsformAttributeLinks: [
            defaultFuehrungsformAttribute,
            defaultFuehrungsformAttribute,
            defaultFuehrungsformAttribute,
          ],
        },
      };
      when(netzService.getKanteForEdit(kanteId1)).thenReturn(Promise.resolve(kante1));
      service['activeAttributGruppe'] = AttributGruppe.FUEHRUNGSFORM;

      service.select(kanteId1, false);
      tick();

      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).length).toEqual(3);
      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS).length).toEqual(1);
    }));

    it('should reset selektion to consistent state when canDiscard after insert segment', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));
      bearbeitungModusSubject.next(AttributGruppe.ZUSTAENDIGKEIT);
      tick();

      const kanteId1 = 1;
      const kante1 = { ...defaultKante, id: kanteId1 };
      when(netzService.getKanteForEdit(kanteId1)).thenReturn(Promise.resolve(kante1));

      const kanteId2 = 2;
      const kante2 = { ...defaultKante, id: kanteId2 };
      when(netzService.getKanteForEdit(kanteId2)).thenReturn(Promise.resolve(kante2));

      service.select(kanteId1, false);
      tick();

      expect(service.selektierteKanten).toEqual([kante1]);
      expect(service.selektion[0].getSelectedSegmentIndices().sort()).toEqual([0]);
      service.adjustSelectionForSegmentInsertion(kanteId1, 1);
      tick();
      expect(
        service.selektion
          .find(s => s.kante.id === kanteId1)
          ?.getSelectedSegmentIndices()
          .sort()
      ).toEqual([0, 1]);

      service.select(kanteId2, true);
      tick();

      expect(service.selektion.find(s => s.kante.id === kanteId1)?.getSelectedSegmentIndices()).toEqual([0]);
    }));
  });

  describe('selectKante mit seitenbezug', () => {
    it('should check discard if Seite of new Kante is selected', () => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));
      service.select(1, false, KantenSeite.LINKS);
      verify(discardGuardService.canDeactivate(anything())).once();
      expect(capture(discardGuardService.canDeactivate).last()[0]).toBe(service['discardableComponent']!);
    });

    it('should check discard if Kante is implicitly deselected', () => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));
      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofSeite(anotherKante, KantenSeite.LINKS),
      ]);
      service.select(anotherKante.id, false, KantenSeite.RECHTS);
      verify(discardGuardService.canDeactivate(anything())).once();
      expect(capture(discardGuardService.canDeactivate).last()[0]).toBe(service['discardableComponent']!);
    });

    it('should not change selection when cannot discard', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));
      const kanteId = 1;
      service.select(kanteId, false, KantenSeite.LINKS);
      tick();
      expect(service.selektierteKanten.map(k => k.id).includes(kanteId)).toBeFalse();
    }));

    it('should set correct selection with no reload and not additiv', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      const kanteId1 = 1;
      const kante1 = { ...defaultKante, id: kanteId1 };
      const kanteId2 = 2;
      const kante2 = { ...anotherKante, id: kanteId2 };
      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(kante1),
        KantenSelektion.ofGesamteKante(kante2),
      ]);

      service.select(kanteId1, false, KantenSeite.LINKS);
      tick();

      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.RECHTS)).toBeFalse();
      verify(netzService.getKanteForEdit(anything())).never();
    }));

    it('should set correct selection with no reload and additiv', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      const kanteId1 = 1;
      const kante1 = { ...defaultKante, id: kanteId1 };
      const kanteId2 = 2;
      const kante2 = { ...anotherKante, id: kanteId2 };
      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(kante1),
        KantenSelektion.ofSeite(kante2, KantenSeite.LINKS),
      ]);

      service.select(kanteId2, true, KantenSeite.RECHTS);
      tick();

      expect(service.selektion.length).toEqual(2);
      expect(service.selektion[1].istBeidseitigSelektiert()).toBeTrue();
      verify(netzService.getKanteForEdit(anything())).never();
    }));

    it('should set correct selection with reload and not additiv', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      const kanteId1 = 1;
      const kante1 = { ...defaultKante, id: kanteId1 };
      const kanteId2 = 2;
      const kante2 = { ...anotherKante, id: kanteId2 };
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(kante1)]);
      tick();

      when(netzService.getKanteForEdit(kanteId2)).thenReturn(Promise.resolve(kante2));
      service.select(kanteId2, false, KantenSeite.RECHTS);
      tick();

      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.RECHTS)).toBeTrue();
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.LINKS)).toBeFalse();
      verify(netzService.getKanteForEdit(anything())).once();
    }));

    it('should set correct selection with reload and additiv', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      const kanteId1 = 1;
      const kante1 = { ...defaultKante, id: kanteId1 };
      const kanteId2 = 2;
      const kante2 = { ...anotherKante, id: kanteId2 };
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(kante1)]);
      when(netzService.getKanteForEdit(kanteId2)).thenReturn(Promise.resolve(kante2));

      service.select(kanteId2, true, KantenSeite.RECHTS);
      tick();

      expect(service.selektion.length).toEqual(2);
      expect(service.selektion[0].istBeidseitigSelektiert()).toBeTrue();
      verify(netzService.getKanteForEdit(anything())).once();
    }));

    it('should set anzahlSegmente correctly', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      const kanteId1 = 1;
      const kante1 = {
        ...defaultKante,
        id: kanteId1,
        fuehrungsformAttributGruppe: {
          ...defaultKante.fuehrungsformAttributGruppe,
          fuehrungsformAttributeLinks: [
            defaultFuehrungsformAttribute,
            defaultFuehrungsformAttribute,
            defaultFuehrungsformAttribute,
          ],
        },
      };
      when(netzService.getKanteForEdit(kanteId1)).thenReturn(Promise.resolve(kante1));
      service['activeAttributGruppe'] = AttributGruppe.FUEHRUNGSFORM;

      service.select(kanteId1, false, KantenSeite.LINKS);
      tick();

      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).length).toEqual(3);
    }));

    it('should reset selektion to consistent state when canDiscard after insert segment', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      bearbeitungModusSubject.next(AttributGruppe.ZUSTAENDIGKEIT);
      tick();

      const kanteId1 = 1;
      const kante1 = { ...defaultKante, id: kanteId1 };
      when(netzService.getKanteForEdit(kanteId1)).thenReturn(Promise.resolve(kante1));

      const kanteId2 = 2;
      const kante2 = { ...defaultKante, id: kanteId2 };
      when(netzService.getKanteForEdit(kanteId2)).thenReturn(Promise.resolve(kante2));

      service.select(kanteId1, false);
      tick();

      expect(service.selektierteKanten).toEqual([kante1]);
      expect(service.selektion[0].getSelectedSegmentIndices().sort()).toEqual([0]);
      service.adjustSelectionForSegmentInsertion(kanteId1, 1);
      tick();
      expect(
        service.selektion
          .find(s => s.kante.id === kanteId1)
          ?.getSelectedSegmentIndices()
          .sort()
      ).toEqual([0, 1]);

      service.select(kanteId2, true, KantenSeite.LINKS);
      tick();

      expect(service.selektion.find(s => s.kante.id === kanteId1)?.getSelectedSegmentIndices()).toEqual([0]);
    }));
  });

  describe('selectKante mit segment', () => {
    const kanteId1 = 1;
    const kanteId2 = 2;
    beforeEach(waitForAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      const kante1 = {
        ...defaultKante,
        id: kanteId1,
        fuehrungsformAttributGruppe: {
          ...defaultKante.fuehrungsformAttributGruppe,
          fuehrungsformAttributeLinks: [
            defaultFuehrungsformAttribute,
            defaultFuehrungsformAttribute,
            defaultFuehrungsformAttribute,
          ],
        },
      };
      const kante2 = {
        ...defaultKante,
        id: kanteId2,
      };
      when(netzService.getKanteForEdit(kanteId1)).thenReturn(Promise.resolve(kante1));
      when(netzService.getKanteForEdit(kanteId2)).thenReturn(Promise.resolve(kante2));
      service.select(kanteId1, true, KantenSeite.RECHTS);
    }));

    it('should not change selection when cannot discard', fakeAsync(() => {
      service.select(kanteId2, true);
      tick();

      resetCalls(discardGuardService);
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));
      service.select(kanteId1, false, KantenSeite.LINKS, 0);
      tick();

      verify(discardGuardService.canDeactivate(anything())).once();
      expect(capture(discardGuardService.canDeactivate).last()[0]).toBe(service['discardableComponent']!);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.LINKS)).toBeFalse();
    }));

    it('should set correct selection when not additiv', fakeAsync(() => {
      service.select(kanteId2, true);
      tick();
      service.select(kanteId1, false, KantenSeite.LINKS, 0);
      tick();

      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0]);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.RECHTS)).toBeFalse();
    }));

    it('should set correct selection when additiv', fakeAsync(() => {
      service.select(kanteId2, true);
      tick();
      service.select(kanteId1, true, KantenSeite.LINKS, 0);
      tick();

      expect(service.selektion.length).toEqual(2);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0]);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.RECHTS)).toBeTrue();
    }));

    it('should append selected indices', fakeAsync(() => {
      service.select(kanteId1, true, KantenSeite.LINKS, 0);
      tick();
      service.select(kanteId1, true, KantenSeite.LINKS, 1);
      tick();

      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.LINKS)).toBeTrue();
      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0, 1]);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.RECHTS)).toBeTrue();
    }));
  });

  describe('deselectKante - Seitenbezug', () => {
    it('should check discard', () => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofSeite(anotherKante, KantenSeite.LINKS),
      ]);
      service.deselect(anotherKante.id, KantenSeite.LINKS);
      verify(discardGuardService.canDeactivate(anything())).once();
      expect(capture(discardGuardService.canDeactivate).last()[0]).toBe(service['discardableComponent']!);
    });

    it('should not change selection when cannot discard', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofSeite(anotherKante, KantenSeite.LINKS),
      ]);
      service.deselect(anotherKante.id, KantenSeite.LINKS);
      tick();
      expect(service.selektion.length).toEqual(2);
    }));

    it('should deselect gesamte kante', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofSeite(anotherKante, KantenSeite.LINKS),
      ]);
      service.deselect(anotherKante.id, KantenSeite.LINKS);
      tick();
      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].kante.id).toEqual(defaultKante.id);
    }));

    it('should deselect seite', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(defaultKante)]);
      service.deselect(defaultKante.id, KantenSeite.LINKS);
      tick();
      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.LINKS)).toBeFalse();
      expect(service.selektion[0].istSeiteSelektiert(KantenSeite.RECHTS)).toBeTrue();
    }));
  });

  describe('deselectKante', () => {
    it('should check discard', () => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofGesamteKante(anotherKante),
      ]);
      service.deselect(anotherKante.id);
      verify(discardGuardService.canDeactivate(anything())).once();
      expect(capture(discardGuardService.canDeactivate).last()[0]).toBe(service['discardableComponent']!);
    });

    it('should not change selection when cannot discard', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofGesamteKante(anotherKante),
      ]);
      service.deselect(anotherKante.id);
      tick();
      expect(service.selektion.length).toEqual(2);
    }));

    it('should deselect gesamte kante', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofGesamteKante(anotherKante),
      ]);
      service.deselect(anotherKante.id);
      tick();
      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].kante.id).toEqual(defaultKante.id);
    }));
  });

  describe('deselectKantenelement', () => {
    it('should check discard', () => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofSeite(anotherKante, KantenSeite.LINKS),
      ]);
      service.deselect(anotherKante.id, KantenSeite.LINKS, 0);
      verify(discardGuardService.canDeactivate(anything())).once();
      expect(capture(discardGuardService.canDeactivate).last()[0]).toBe(service['discardableComponent']!);
    });

    it('should not change selection when cannot discard', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofSeite(anotherKante, KantenSeite.LINKS),
      ]);
      service.deselect(anotherKante.id, KantenSeite.LINKS, 0);
      tick();
      expect(service.selektion.length).toEqual(2);
    }));

    it('should deselect gesamte kante', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(defaultKante),
        KantenSelektion.ofSeite(anotherKante, KantenSeite.LINKS),
      ]);
      service.deselect(anotherKante.id, KantenSeite.LINKS, 0);
      tick();
      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].kante.id).toEqual(defaultKante.id);
    }));

    it('should deselect element', fakeAsync(() => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(defaultKante, 3, 1)]);
      service.deselect(defaultKante.id, undefined, 1);
      tick();
      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].getSelectedSegmentIndices()).toEqual([0, 2]);
    }));
  });

  describe('adjustSelectionForSegmentInsertion', () => {
    it('should adjust indices without seitenbezug', fakeAsync(() => {
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(defaultKante, 2, 2)]);

      service.adjustSelectionForSegmentInsertion(defaultKante.id, 1);

      expect(service.selektion[0].getSelectedSegmentIndices()).toEqual([1, 0, 2]);
    }));

    it('should adjust indices with seitenbezug', fakeAsync(() => {
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(defaultKante, 2, 1)]);

      service.adjustSelectionForSegmentInsertion(defaultKante.id, 1, KantenSeite.LINKS);

      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([1, 0, 2]);
      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0]);
    }));
  });

  describe('adjustSelectionForSegmentDeletion', () => {
    it('should adjust indices without seitenbezug', fakeAsync(() => {
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(defaultKante, 3, 3)]);

      service.adjustSelectionForSegmentDeletion(defaultKante.id, 1);

      expect(service.selektion[0].getSelectedSegmentIndices()).toEqual([0, 1]);
    }));

    it('should adjust indices with seitenbezug', fakeAsync(() => {
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(defaultKante, 3, 1)]);

      service.adjustSelectionForSegmentDeletion(defaultKante.id, 1, KantenSeite.LINKS);

      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0, 1]);
      expect(service.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0]);
    }));

    it('should prevent deselection of gesamte kante', fakeAsync(() => {
      service['selektionSubject'].next([createSelektionOfSegment(defaultKante, 1, KantenSeite.LINKS)]);

      service.adjustSelectionForSegmentDeletion(defaultKante.id, 1, KantenSeite.LINKS);

      expect(service.selektion.length).toEqual(1);
      expect(service.selektion[0].getSelectedSegmentIndices()).toEqual([0]);
    }));
  });

  describe('resetSelectionToConsistentState', () => {
    it('cap selection for zustaendigkeit', fakeAsync(() => {
      service['activeAttributGruppe'] = AttributGruppe.ZUSTAENDIGKEIT;
      const kanteId1 = 1;
      const kante1 = {
        ...defaultKante,
        id: kanteId1,
        zustaendigkeitAttributGruppe: {
          ...defaultKante.zustaendigkeitAttributGruppe,
          zustaendigkeitAttribute: [defaultZustaendigkeitAttribute, defaultZustaendigkeitAttribute],
        },
      };
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(kante1, 4, 4)]);

      service.resetSelectionToConsistentState();

      expect(service.selektion[0].getSelectedSegmentIndices()).toEqual([0, 1]);
    }));

    it('cap selection for fuehrungsform', fakeAsync(() => {
      service['activeAttributGruppe'] = AttributGruppe.FUEHRUNGSFORM;
      const kanteId1 = 1;
      const kante1 = {
        ...defaultKante,
        id: kanteId1,
        fuehrungsformAttributGruppe: {
          ...defaultKante.fuehrungsformAttributGruppe,
          fuehrungsformAttributeLinks: [defaultFuehrungsformAttribute, defaultFuehrungsformAttribute],
        },
      };
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(kante1, 4, 4)]);

      service.resetSelectionToConsistentState();

      expect(service.selektion[0].getSelectedSegmentIndices()).toEqual([0, 1]);
    }));

    it('prevent deselection of gesamte kante - select first segement instead', fakeAsync(() => {
      service['activeAttributGruppe'] = AttributGruppe.FUEHRUNGSFORM;
      const kanteId1 = 1;
      const kante1 = {
        ...defaultKante,
        id: kanteId1,
        fuehrungsformAttributGruppe: {
          ...defaultKante.fuehrungsformAttributGruppe,
          fuehrungsformAttributeLinks: [defaultFuehrungsformAttribute, defaultFuehrungsformAttribute],
        },
      };
      service['selektionSubject'].next([createSelektionOfSegment(kante1, 3)]);

      service.resetSelectionToConsistentState();

      expect(service.selektion[0].getSelectedSegmentIndices()).toEqual([0]);
    }));
  });

  describe('cleanUp', () => {
    it('should check discard', () => {
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));
      service.cleanUp(false);
      verify(discardGuardService.canDeactivate(anything())).once();
      expect(capture(discardGuardService.canDeactivate).last()[0]).toBe(service['discardableComponent']!);
    });

    it('should return false when cannot discard', (done: DoneFn) => {
      const spy = spyOn(service['selektionSubject'], 'next');
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(false));
      service.cleanUp(true).then(returnValue => {
        expect(returnValue).toBeFalse();
        expect(spy).not.toHaveBeenCalled();
        done();
      });
    });

    it('should replace if parameter true', fakeAsync(() => {
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(defaultKante)]);
      tick();
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));
      service.cleanUp(true);
      tick();
      expect(service.selektion.length).toBe(0);
    }));

    it('should not replace if parameter false', fakeAsync(() => {
      service['selektionSubject'].next([KantenSelektion.ofGesamteKante(defaultKante)]);
      tick();
      when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));
      service.cleanUp(false);
      tick();
      expect(service.selektion.length).toBe(1);
    }));
  });

  describe('update', () => {
    it('should throw if update kanten are not selected', fakeAsync(() => {
      const kanteId1 = 1;
      const kante1 = { ...defaultKante, id: kanteId1 };

      const kanteId2 = 2;
      const kante2 = { ...defaultKante, id: kanteId2 };

      service['selektionSubject'].next([
        KantenSelektion.ofGesamteKante(kante1),
        KantenSelektion.ofGesamteKante(kante2),
      ]);

      expect(() => service.updateKanten([kante1])).toThrow();
      expect(() => service.updateKanten([{ ...kante1, id: kanteId1 + 10 }, kante2])).toThrow();
    }));
  });
});

function createSelektionOfSegment(kante: Kante, segmentIndex: number, kantenSeite?: KantenSeite): KantenSelektion {
  const basicSelektion = KantenSelektion.ofGesamteKante(kante);
  return basicSelektion.selectSegment(segmentIndex, kantenSeite);
}
