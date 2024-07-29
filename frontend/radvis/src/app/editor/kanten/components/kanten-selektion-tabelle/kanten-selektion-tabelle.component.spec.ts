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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { of } from 'rxjs';
import { EditorModule } from 'src/app/editor/editor.module';
import { KantenSelektionTabelleComponent } from 'src/app/editor/kanten/components/kanten-selektion-tabelle/kanten-selektion-tabelle.component';
import { defaultKante } from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { KantenSelektionHoverService } from 'src/app/editor/kanten/services/kanten-selektion-hover.service';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe('KantenSelektionTabelleComponent', () => {
  let component: KantenSelektionTabelleComponent;
  let fixture: ComponentFixture<KantenSelektionTabelleComponent>;
  let kantenSelektionService: KantenSelektionService;
  let kantenSelektionHoverService: KantenSelektionHoverService;

  beforeEach(() => {
    kantenSelektionHoverService = mock(KantenSelektionHoverService);
    kantenSelektionService = mock(KantenSelektionService);
    when(kantenSelektionHoverService.hoverKante$).thenReturn(
      of({
        kanteId: 1,
        kantenSeite: KantenSeite.LINKS,
      })
    );
    when(kantenSelektionHoverService.unhoverKante$).thenReturn(of());
    return MockBuilder(KantenSelektionTabelleComponent, EditorModule)
      .provide({ provide: KantenSelektionService, useValue: instance(kantenSelektionService) })
      .provide({ provide: KantenSelektionHoverService, useValue: instance(kantenSelektionHoverService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(KantenSelektionTabelleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('onSelect', () => {
    describe('seitenbezug', () => {
      it('should deselect Seite when Seite already selected', () => {
        component.selektion = [KantenSelektion.ofGesamteKante(defaultKante)];

        component.onSelect(1, KantenSeite.LINKS);

        verify(kantenSelektionService.deselect(anything(), anything())).called();
        expect(capture(kantenSelektionService.deselect).last()).toEqual([1, KantenSeite.LINKS]);
        verify(kantenSelektionService.select(anything(), anything(), anything(), anything())).never();
      });

      it('should select Seite when Seite not already selected', () => {
        component.selektion = [KantenSelektion.ofSeite(defaultKante, KantenSeite.RECHTS)];

        component.onSelect(1, KantenSeite.LINKS);

        verify(kantenSelektionService.select(1, true, KantenSeite.LINKS)).called();
        verify(kantenSelektionService.deselect(anything(), anything(), anything())).never();
        expect().nothing();
      });
    });

    describe('kein seitenbezug', () => {
      it('should deselect Kante', () => {
        component.selektion = [KantenSelektion.ofGesamteKante(defaultKante)];

        component.onSelect(1);

        verify(kantenSelektionService.deselect(1)).called();
        verify(kantenSelektionService.select(anything(), anything(), anything(), anything())).never();
        expect().nothing();
      });
    });
  });
});
