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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { Feature, MapBrowserEvent } from 'ol';
import { Extent } from 'ol/extent';
import { GeoJSON } from 'ol/format';
import { GeoJSONFeature, GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { LineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { KantenSelektionComponent } from 'src/app/editor/editor-shared/components/kanten-selektion/kanten-selektion.component';
import { EditorModule } from 'src/app/editor/editor.module';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { anotherKante, defaultKante } from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { KantenSelektionHoverService } from 'src/app/editor/kanten/services/kanten-selektion-hover.service';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { StreckenNetzVectorlayer } from 'src/app/shared/models/strecken-netz-vectorlayer';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';

describe(KantenSelektionComponent.name, () => {
  let component: KantenSelektionComponent;
  let fixture: ComponentFixture<KantenSelektionComponent>;

  let olMapService: OlMapService;
  let errorHandlingService: ErrorHandlingService;
  let notifyGeometryChangedService: NotifyGeometryChangedService;
  let radNetzFeatureService: NetzausschnittService;
  let kantenSelektionService: KantenSelektionService;
  let bearbeitungsModusService: NetzBearbeitungModusService;
  let kantenSelektionHoverService: KantenSelektionHoverService;
  const aktiveGruppeSubject = new BehaviorSubject<AttributGruppe | null>(null);
  let kantenLayerNichtklassifiziert: VectorLayer;
  let netzklassenAuswahlSubject$: Subject<Netzklassefilter[]>;
  let netzklassenAuswahlService: NetzklassenAuswahlService;

  const loadingNetzForNichtKlassifiziert = (extent: Extent): void => {
    kantenLayerNichtklassifiziert
      .getSource()
      ?.loadFeatures(extent, 20, kantenLayerNichtklassifiziert.getSource()!.getProjection()!);
  };

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    errorHandlingService = mock(ErrorHandlingService);
    notifyGeometryChangedService = mock(NotifyGeometryChangedService);
    radNetzFeatureService = mock(NetzausschnittService);
    kantenSelektionService = mock(KantenSelektionService);
    bearbeitungsModusService = mock(NetzBearbeitungModusService);
    kantenSelektionHoverService = mock(KantenSelektionHoverService);
    netzklassenAuswahlSubject$ = new Subject<Netzklassefilter[]>();
    netzklassenAuswahlService = mock(NetzklassenAuswahlService);

    when(olMapService.click$()).thenReturn(of());
    when(olMapService.pointerMove$()).thenReturn(of());
    when(olMapService.pointerLeave$()).thenReturn(of());
    when(radNetzFeatureService.getKantenForView(anything(), anything(), anything())).thenReturn(
      of(createDummyFeatureCollection())
    );
    when(notifyGeometryChangedService.geometryChanged$).thenReturn(of());
    when(kantenSelektionService.selektierteKanten$).thenReturn(of([]));
    when(kantenSelektionService.selektierteKanten).thenReturn([]);
    when(kantenSelektionService.selektion).thenReturn([]);
    when(kantenSelektionService.selektion$).thenReturn(of([]));
    when(bearbeitungsModusService.getAktiveKantenGruppe()).thenReturn(aktiveGruppeSubject);
    when(kantenSelektionHoverService.hoverKante$).thenReturn(
      of({
        kanteId: 1,
        kantenSeite: KantenSeite.LINKS,
      })
    );
    when(kantenSelektionHoverService.unhoverKante$).thenReturn(of());
    when(netzklassenAuswahlService.currentAuswahl$).thenReturn(netzklassenAuswahlSubject$);
    return MockBuilder(KantenSelektionComponent, EditorModule)
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({ provide: ErrorHandlingService, useValue: instance(errorHandlingService) })
      .provide({ provide: NotifyGeometryChangedService, useValue: instance(notifyGeometryChangedService) })
      .provide({ provide: NetzausschnittService, useValue: instance(radNetzFeatureService) })
      .provide({ provide: KantenSelektionService, useValue: instance(kantenSelektionService) })
      .provide({ provide: NetzBearbeitungModusService, useValue: instance(bearbeitungsModusService) })
      .provide({ provide: KantenSelektionHoverService, useValue: instance(kantenSelektionHoverService) })
      .provide({ provide: NetzklassenAuswahlService, useValue: instance(netzklassenAuswahlService) });
  });

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(KantenSelektionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    aktiveGruppeSubject.next(AttributGruppe.ALLGEMEIN);
    tick(1);
    component['netzklassen'] = [Netzklassefilter.NICHT_KLASSIFIZIERT];
    const kantenLayerNichtklassifiziertSearch = component['kantenLayers'].find(
      kl => kl.getProperties().netzklasse === Netzklassefilter.NICHT_KLASSIFIZIERT
    );
    invariant(kantenLayerNichtklassifiziertSearch);
    kantenLayerNichtklassifiziert = kantenLayerNichtklassifiziertSearch;
    loadingNetzForNichtKlassifiziert([0, 0, 5, 5]);

    tick(1);
  }));

  describe('change active Gruppe', () => {
    it('should set active property correctly', fakeAsync(() => {
      aktiveGruppeSubject.next(null);
      tick(1);
      expect(component['active']).toBeFalse();

      aktiveGruppeSubject.next(AttributGruppe.FAHRTRICHTUNG);
      tick(1);
      expect(component['active']).toBeTrue();
    }));

    for (const gruppe of [
      AttributGruppe.VERLAUF,
      AttributGruppe.GESCHWINDIGKEIT,
      AttributGruppe.FUEHRUNGSFORM,
      AttributGruppe.ZUSTAENDIGKEIT,
    ]) {
      it(`should set hidden property for ${gruppe}`, fakeAsync(() => {
        aktiveGruppeSubject.next(AttributGruppe.ALLGEMEIN);
        tick(1);
        expect(component['selektionLayer'].getVisible()).toBeTrue();

        aktiveGruppeSubject.next(gruppe);
        tick(1);
        expect(component['selektionLayer'].getVisible()).toBeFalse();
      }));
    }

    for (const gruppe of [AttributGruppe.ALLGEMEIN, AttributGruppe.FAHRTRICHTUNG]) {
      it(`should not set hidden property for ${gruppe}`, fakeAsync(() => {
        aktiveGruppeSubject.next(AttributGruppe.ALLGEMEIN);
        tick(1);
        expect(component['selektionLayer'].getVisible()).toBeTrue();

        aktiveGruppeSubject.next(gruppe);
        tick(1);
        expect(component['selektionLayer'].getVisible()).toBeTrue();
      }));
    }
  });

  describe('onMapClick', () => {
    it('should do nothing when no features are under cursor', () => {
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<PointerEvent | KeyboardEvent | WheelEvent>);

      verify(kantenSelektionService.select(anything(), anything(), anything(), anything())).never();
      verify(kantenSelektionService.deselect(anything(), anything(), anything())).never();
      expect().nothing();
    });

    it('should do nothing when nearest feature is not on correct layer', () => {
      const someFeature = new Feature(
        new LineString([
          [23, 77],
          [34, 66],
        ])
      );
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([someFeature]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<PointerEvent | KeyboardEvent | WheelEvent>);

      verify(kantenSelektionService.select(anything(), anything(), anything(), anything())).never();
      verify(kantenSelektionService.deselect(anything(), anything(), anything())).never();
      expect().nothing();
    });

    it('should select nearest Kante non-additiv without Seitenbezug on normal click', () => {
      const firstFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0];
      const secondFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1];
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([firstFeature, secondFeature]);

      component['onMapClick']({
        pixel: [0, 0],
        originalEvent: { ctrlKey: false, metaKey: false } as PointerEvent,
      } as unknown as MapBrowserEvent<PointerEvent>);

      verify(kantenSelektionService.select(Number(firstFeature.getId()), false, undefined)).called();
      verify(kantenSelektionService.deselect(anything(), anything(), anything())).never();
      expect().nothing();
    });

    it('should select nearest Seite non-additiv on normal click', () => {
      const firstFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0];
      firstFeature.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.LINKS);
      const secondFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1];
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([firstFeature, secondFeature]);

      component['onMapClick']({
        pixel: [0, 0],
        originalEvent: { ctrlKey: false, metaKey: false } as PointerEvent,
      } as unknown as MapBrowserEvent<PointerEvent>);

      verify(kantenSelektionService.select(Number(firstFeature.getId()), false, KantenSeite.LINKS)).called();
      verify(kantenSelektionService.deselect(anything(), anything(), anything())).never();
      expect().nothing();
    });

    it('should select nearest Kante additiv without Seitenbezug on Kante selection with toggle', () => {
      const firstFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0];
      const secondFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1];
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([firstFeature, secondFeature]);
      when(kantenSelektionService.isSelektiert(Number(firstFeature.getId()), undefined)).thenReturn(false);

      component['onMapClick']({
        pixel: [0, 0],
        originalEvent: { ctrlKey: true, metaKey: false } as PointerEvent,
      } as unknown as MapBrowserEvent<PointerEvent>);

      verify(kantenSelektionService.select(Number(firstFeature.getId()), true, undefined)).called();
      verify(kantenSelektionService.deselect(anything(), anything(), anything())).never();
      expect().nothing();
    });

    it('should select nearest Seite additiv on Seite selection with toggle', () => {
      const firstFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0];
      firstFeature.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.LINKS);
      const secondFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1];
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([firstFeature, secondFeature]);
      when(kantenSelektionService.isSelektiert(Number(firstFeature.getId()), undefined)).thenReturn(false);

      component['onMapClick']({
        pixel: [0, 0],
        originalEvent: { ctrlKey: false, metaKey: true } as PointerEvent,
      } as unknown as MapBrowserEvent<PointerEvent>);

      verify(kantenSelektionService.select(Number(firstFeature.getId()), true, KantenSeite.LINKS)).called();
      verify(kantenSelektionService.deselect(anything(), anything(), anything())).never();
      expect().nothing();
    });

    it('should deselect nearest Kante without Seitenbezug on toggle deselection of kante', () => {
      const firstFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0];
      const secondFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1];
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([firstFeature, secondFeature]);
      when(kantenSelektionService.isSelektiert(Number(firstFeature.getId()), undefined)).thenReturn(true);

      component['onMapClick']({
        pixel: [0, 0],
        originalEvent: { ctrlKey: true, metaKey: false } as PointerEvent,
      } as unknown as MapBrowserEvent<PointerEvent>);

      verify(kantenSelektionService.deselect(Number(firstFeature.getId()), undefined)).called();
      verify(kantenSelektionService.select(anything(), anything(), anything(), anything())).never();
      expect().nothing();
    });

    it('should deselect nearest Seite on toggle deselection of seite', fakeAsync(() => {
      const firstFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0];
      firstFeature.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.LINKS);
      firstFeature.set(FeatureProperties.ZWEISEITIG_PROPERTY_NAME, true);
      const secondFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1];
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([firstFeature, secondFeature]);
      when(kantenSelektionService.isSelektiert(Number(firstFeature.getId()), KantenSeite.LINKS)).thenReturn(true);

      component['onMapClick']({
        pixel: [0, 0],
        originalEvent: { ctrlKey: true, metaKey: false } as PointerEvent,
      } as unknown as MapBrowserEvent<PointerEvent>);

      verify(kantenSelektionService.deselect(Number(firstFeature.getId()), KantenSeite.LINKS)).called();
      verify(kantenSelektionService.select(anything(), anything(), anything(), anything())).never();
      expect().nothing();
    }));

    describe('with real Features and Seitenbezug', () => {
      it('should deselect nearest Seite on toggle deselection of seite', fakeAsync(() => {
        const dummyFeatureCollectionWithSeitenAttribute = createDummyFeatureCollectionWithSeitenAttribute();
        when(radNetzFeatureService.getKantenForView(anything(), anything(), anything())).thenReturn(
          of(dummyFeatureCollectionWithSeitenAttribute)
        );
        aktiveGruppeSubject.next(AttributGruppe.FAHRTRICHTUNG);
        tick(1);
        loadingNetzForNichtKlassifiziert([0, 0, 5, 5]);
        tick(1);

        const feature = new GeoJSON().readFeatures(dummyFeatureCollectionWithSeitenAttribute)[0];

        when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([
          component['cloneFeatureForSeitenbezug'](feature, KantenSeite.LINKS),
        ]);

        when(kantenSelektionService.isSelektiert(Number(feature.getId()), KantenSeite.LINKS)).thenReturn(true);

        component['onMapClick']({
          pixel: [0, 0],
          originalEvent: { ctrlKey: true, metaKey: false } as PointerEvent,
        } as unknown as MapBrowserEvent<PointerEvent>);

        verify(kantenSelektionService.deselect(Number(feature.getId()), KantenSeite.LINKS)).called();
        verify(kantenSelektionService.select(anything(), anything(), anything(), anything())).never();
        expect().nothing();
      }));
    });
  });

  describe('refreshSelection', () => {
    it('should add Feature from selected Kante without Seitenbezug', () => {
      when(kantenSelektionService.selektion).thenReturn([KantenSelektion.ofGesamteKante(defaultKante)]);
      expect(component['selektionLayer'].getSource()?.getFeatures().length).toBe(0);

      component['loadingSelection']();
      expect(component['selektionLayer'].getSource()?.getFeatures().length).toBe(1);
      expect(component['selektionLayer'].getSource()?.getFeatures()[0].getId()).toBe(1);
    });

    it('should add single Seite of Feature from selected Seite', fakeAsync(() => {
      when(radNetzFeatureService.getKantenForView(anything(), anything(), anything())).thenReturn(
        of(createDummyFeatureCollectionWithSeitenAttribute())
      );
      aktiveGruppeSubject.next(AttributGruppe.FAHRTRICHTUNG);
      tick(1);
      loadingNetzForNichtKlassifiziert([0, 0, 5, 5]);
      tick(1);
      when(kantenSelektionService.selektion).thenReturn([KantenSelektion.ofSeite(defaultKante, KantenSeite.LINKS)]);
      expect(component['selektionLayer'].getSource()?.getFeatures().length).toBe(0);

      component['loadingSelection']();

      expect(component['selektionLayer'].getSource()?.getFeatures().length).toBe(1);
      expect(
        component['selektionLayer'].getSource()?.getFeatures()[0].get(FeatureProperties.KANTE_ID_PROPERTY_NAME)
      ).toBe(1);
      expect(component['selektionLayer'].getSource()?.getFeatures()[0].get(FeatureProperties.SEITE_PROPERTY_NAME)).toBe(
        KantenSeite.LINKS
      );
    }));

    it('should add both Seiten of Feature from selected Kante', fakeAsync(() => {
      when(radNetzFeatureService.getKantenForView(anything(), anything(), anything())).thenReturn(
        of(createDummyFeatureCollectionWithSeitenAttribute())
      );
      aktiveGruppeSubject.next(AttributGruppe.FAHRTRICHTUNG);
      tick(1);
      loadingNetzForNichtKlassifiziert([0, 0, 5, 5]);
      tick(1);
      when(kantenSelektionService.selektion).thenReturn([KantenSelektion.ofGesamteKante(defaultKante)]);
      expect(component['selektionLayer'].getSource()?.getFeatures().length).toBe(0);

      component['loadingSelection']();

      expect(component['selektionLayer'].getSource()?.getFeatures().length).toBe(2);
      expect(
        component['selektionLayer'].getSource()?.getFeatures()[0].get(FeatureProperties.KANTE_ID_PROPERTY_NAME)
      ).toBe(1);
      expect(component['selektionLayer'].getSource()?.getFeatures()[0].get(FeatureProperties.SEITE_PROPERTY_NAME)).toBe(
        KantenSeite.LINKS
      );
      expect(
        component['selektionLayer'].getSource()?.getFeatures()[1].get(FeatureProperties.KANTE_ID_PROPERTY_NAME)
      ).toBe(1);
      expect(component['selektionLayer'].getSource()?.getFeatures()[1].get(FeatureProperties.SEITE_PROPERTY_NAME)).toBe(
        KantenSeite.RECHTS
      );
    }));
  });

  describe('loadingFunction', () => {
    it('should clear vector source on empty GeoJSON', fakeAsync(() => {
      when(radNetzFeatureService.getKantenForView(anything(), anything(), anything())).thenReturn(
        of(createEmptyFeatureCollection())
      );
      loadingNetzForNichtKlassifiziert([0, 0, 10, 10]);
      tick(1);
      verify(radNetzFeatureService.getKantenForView(anything(), anything(), anything())).called();
      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures().length).toBe(0);
    }));

    it('should add one feature per feature in GeoJSON without Seitenbezug', () => {
      // default -> See beforeEach()
      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures().length).toBe(2);
      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0].getId()).toBe('1');
      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1].getId()).toBe('2');
    });

    it('should add two features per feature in GeoJSON with Seitenbezug', fakeAsync(() => {
      when(radNetzFeatureService.getKantenForView(anything(), anything(), anything())).thenReturn(
        of(createDummyFeatureCollectionWithSeitenAttribute())
      );
      aktiveGruppeSubject.next(AttributGruppe.FAHRTRICHTUNG);
      tick(1);
      loadingNetzForNichtKlassifiziert([0, 0, 5, 5]);
      tick(1);

      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures().length).toBe(4);
      expect(
        kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0].get(FeatureProperties.KANTE_ID_PROPERTY_NAME)
      ).toBe(1);
      expect(
        kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0].get(FeatureProperties.SEITE_PROPERTY_NAME)
      ).toBe(KantenSeite.LINKS);
      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0].getId()).toBe('1' + KantenSeite.LINKS);
      expect(
        kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1].get(FeatureProperties.KANTE_ID_PROPERTY_NAME)
      ).toBe(1);
      expect(
        kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1].get(FeatureProperties.SEITE_PROPERTY_NAME)
      ).toBe(KantenSeite.RECHTS);
      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1].getId()).toBe('1' + KantenSeite.RECHTS);
      expect(
        kantenLayerNichtklassifiziert.getSource()?.getFeatures()[2].get(FeatureProperties.KANTE_ID_PROPERTY_NAME)
      ).toBe(2);
      expect(
        kantenLayerNichtklassifiziert.getSource()?.getFeatures()[2].get(FeatureProperties.SEITE_PROPERTY_NAME)
      ).toBe(KantenSeite.LINKS);
      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures()[2].getId()).toBe('2' + KantenSeite.LINKS);
      expect(
        kantenLayerNichtklassifiziert.getSource()?.getFeatures()[3].get(FeatureProperties.KANTE_ID_PROPERTY_NAME)
      ).toBe(2);
      expect(
        kantenLayerNichtklassifiziert.getSource()?.getFeatures()[3].get(FeatureProperties.SEITE_PROPERTY_NAME)
      ).toBe(KantenSeite.RECHTS);
      expect(kantenLayerNichtklassifiziert.getSource()?.getFeatures()[3].getId()).toBe('2' + KantenSeite.RECHTS);
    }));
  });

  describe('Hover', () => {
    describe('onMapPointerMove', () => {
      it('should not highlight when nothing is selected', () => {
        when(kantenSelektionService.selektion).thenReturn([]);

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<PointerEvent>);

        verify(kantenSelektionHoverService.notifyHover(anything())).never();
        verify(kantenSelektionHoverService.notifyUnhover()).never();
        expect().nothing();
      });

      it('should not highlight when attributgruppe is not seitenbezogen', () => {
        when(kantenSelektionService.selektion).thenReturn([KantenSelektion.ofGesamteKante(defaultKante)]);
        aktiveGruppeSubject.next(AttributGruppe.ZUSTAENDIGKEIT);

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<PointerEvent>);

        verify(kantenSelektionHoverService.notifyHover(anything())).never();
        verify(kantenSelektionHoverService.notifyUnhover()).never();
        expect().nothing();
      });

      it('should not highlight when attributgruppe is lin. ref', () => {
        when(kantenSelektionService.selektion).thenReturn([KantenSelektion.ofGesamteKante(defaultKante)]);
        aktiveGruppeSubject.next(AttributGruppe.FUEHRUNGSFORM);

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<PointerEvent>);

        verify(kantenSelektionHoverService.notifyHover(anything())).never();
        verify(kantenSelektionHoverService.notifyUnhover()).never();
        expect().nothing();
      });

      it('should notifyUnhover when no features under pointer', () => {
        when(kantenSelektionService.selektion).thenReturn([KantenSelektion.ofGesamteKante(defaultKante)]);
        aktiveGruppeSubject.next(AttributGruppe.FAHRTRICHTUNG);
        when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([]);

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<PointerEvent>);

        verify(kantenSelektionHoverService.notifyHover(anything())).never();
        verify(kantenSelektionHoverService.notifyUnhover()).once();
        expect().nothing();
      });

      it('should notifyHover when feature under pointer is selektiert', () => {
        when(kantenSelektionService.selektion).thenReturn([KantenSelektion.ofGesamteKante(defaultKante)]);
        when(kantenSelektionService.isSelektiert(defaultKante.id)).thenReturn(true);
        aktiveGruppeSubject.next(AttributGruppe.FAHRTRICHTUNG);
        const firstFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0];
        const secondFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1];
        when(olMapService.getFeaturesAtPixel(anything(), anything(), anything())).thenReturn([
          firstFeature,
          secondFeature,
        ]);

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<PointerEvent>);

        verify(kantenSelektionHoverService.notifyHover(deepEqual({ kanteId: 1, kantenSeite: undefined }))).once();
        verify(kantenSelektionHoverService.notifyUnhover()).never();
        expect().nothing();
      });

      it('should notifyUnhover when feature under pointer is not selektiert', () => {
        when(kantenSelektionService.selektion).thenReturn([KantenSelektion.ofGesamteKante(anotherKante)]);
        when(kantenSelektionService.isSelektiert(anotherKante.id)).thenReturn(true);
        aktiveGruppeSubject.next(AttributGruppe.FAHRTRICHTUNG);
        const firstFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[0];
        const secondFeature = kantenLayerNichtklassifiziert.getSource()?.getFeatures()[1];
        when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([firstFeature, secondFeature]);

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<PointerEvent>);

        verify(kantenSelektionHoverService.notifyHover(anything())).never();
        verify(kantenSelektionHoverService.notifyUnhover()).once();
        expect().nothing();
      });
    });

    describe('onHover', () => {
      it('should do nothing when hoveredKante has not been changed', () => {
        const hoveredKante = { kanteId: 1, kantenSeite: KantenSeite.LINKS };
        component['hoveredKante'] = hoveredKante;
        const setColorForKanteSpy = spyOn<any>(component, 'setColorForKante');

        component['onHover'](hoveredKante);

        expect(setColorForKanteSpy).not.toHaveBeenCalled();
      });

      it('should set hoveredKante and change colors when hoveredKante has been changed', () => {
        const previousHoveredKante = { kanteId: 1, kantenSeite: KantenSeite.LINKS };
        const hoveredKante = { kanteId: 2, kantenSeite: KantenSeite.RECHTS };
        component['hoveredKante'] = previousHoveredKante;
        const setColorForKanteSpy = spyOn<any>(component, 'setColorForKante');

        component['onHover'](hoveredKante);

        const actual = component['hoveredKante'] as unknown as { kanteId: number; kantenSeite?: KantenSeite };
        expect(actual).toEqual(hoveredKante);
        expect(setColorForKanteSpy).toHaveBeenCalledWith(
          1,
          KantenSeite.LINKS,
          MapStyles.FEATURE_COLOR,
          MapStyles.FEATURE_SELECT_COLOR
        );
        expect(setColorForKanteSpy).toHaveBeenCalledWith(
          2,
          KantenSeite.RECHTS,
          MapStyles.FEATURE_HOVER_COLOR,
          MapStyles.FEATURE_HOVER_COLOR
        );
      });
    });

    describe('onUnhover', () => {
      it('should do nothing when hoveredKante is null', () => {
        component['hoveredKante'] = null;
        const setColorForKanteSpy = spyOn<any>(component, 'setColorForKante');

        component['onUnhover']();

        expect(setColorForKanteSpy).not.toHaveBeenCalled();
      });

      it('should unset hoveredKante and change colors when hoveredKante is not null', () => {
        component['hoveredKante'] = { kanteId: 1, kantenSeite: KantenSeite.LINKS };
        const setColorForKanteSpy = spyOn<any>(component, 'setColorForKante');

        component['onUnhover']();

        const actual = component['hoveredKante'] as unknown as null;
        expect(actual).toEqual(null);
        expect(setColorForKanteSpy).toHaveBeenCalledWith(
          1,
          KantenSeite.LINKS,
          MapStyles.FEATURE_COLOR,
          MapStyles.FEATURE_SELECT_COLOR
        );
      });
    });
  });

  describe('netzklassen-zoomstufen', () => {
    it('should set correct min-zoom on layers', fakeAsync(() => {
      component['netzklassen'] = Netzklassefilter.getAll();

      component['kantenLayers'].forEach(kl => {
        let expectedMinZoom;
        const netzklasse = kl.getProperties().netzklasse;
        const isStreckenNetzVectorLayer = kl instanceof StreckenNetzVectorlayer;
        switch (netzklasse) {
          case Netzklassefilter.RADNETZ:
            expectedMinZoom = isStreckenNetzVectorLayer ? -Infinity : Netzklassefilter.RADNETZ.minZoom;
            break;
          case Netzklassefilter.KREISNETZ:
            expectedMinZoom = Netzklassefilter.KREISNETZ.minZoom;
            break;
          case Netzklassefilter.KOMMUNALNETZ:
            expectedMinZoom = Netzklassefilter.KOMMUNALNETZ.minZoom;
            break;
          case Netzklassefilter.NICHT_KLASSIFIZIERT:
            expectedMinZoom = Netzklassefilter.NICHT_KLASSIFIZIERT.minZoom;
            break;
          case Netzklassefilter.RADSCHNELLVERBINDUNG:
            expectedMinZoom = Netzklassefilter.RADSCHNELLVERBINDUNG.minZoom;
            break;
          case Netzklassefilter.RADVORRANGROUTEN:
            expectedMinZoom = Netzklassefilter.RADVORRANGROUTEN.minZoom;
            break;
          default:
            expectedMinZoom = 15.25;
        }
        expect(kl.getMinZoom())
          .withContext(`for netzklasse ${netzklasse}${isStreckenNetzVectorLayer ? ' (StreckenNetzVectorLayer!)' : ''}`)
          .toEqual(expectedMinZoom);
      });
    }));

    describe('onChanges', () => {
      it('should show layer when corresponding netzklasse is added', fakeAsync(() => {
        netzklassenAuswahlSubject$.next([]);
        tick();
        expect(kantenLayerNichtklassifiziert.getVisible()).toBeFalse();

        netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        tick();
        expect(kantenLayerNichtklassifiziert.getVisible()).toBeTrue();
      }));

      it('should hide layer when corresponding netzklasse is removed', fakeAsync(() => {
        netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        tick();
        expect(kantenLayerNichtklassifiziert.getVisible()).toBeTrue();

        netzklassenAuswahlSubject$.next([]);
        tick();
        expect(kantenLayerNichtklassifiziert.getVisible()).toBeFalse();
      }));

      it('should not refresh source when layer is neither selected nor deselected', fakeAsync(() => {
        const knotenLayerRadnetz = component['kantenLayers'].find(
          kl => kl.getProperties().netzklasse === Netzklassefilter.RADNETZ
        );
        invariant(knotenLayerRadnetz);
        const refreshSpy = spyOn(knotenLayerRadnetz.getSource(), 'refresh' as never);

        component['netzklassen'] = [];
        netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        tick();

        expect(refreshSpy).not.toHaveBeenCalled();
      }));
    });
  });
});

function createEmptyFeatureCollection(): GeoJSONFeatureCollection {
  return {
    type: 'FeatureCollection',
    features: [],
  };
}

function createDummyFeatureCollection(): GeoJSONFeatureCollection {
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: {
          kanteZweiseitig: false,
        },
        geometry: {
          type: 'LineString',
          coordinates: [
            [1, 1],
            [2, 2],
            [3, 3],
          ],
        },
        id: '1',
      },
      {
        type: 'Feature',
        properties: {
          kanteZweiseitig: false,
        },
        geometry: {
          type: 'LineString',
          coordinates: [
            [4, 4],
            [5, 5],
          ],
        },
        id: '2',
      },
    ],
  } as GeoJSONFeatureCollection;
}

function createDummyFeatureCollectionWithSeitenAttribute(): GeoJSONFeatureCollection {
  const collection = createDummyFeatureCollection();
  collection.features = collection.features.map((feature: GeoJSONFeature) => {
    if (!feature.properties) {
      feature.properties = {};
    }
    feature.properties.kanteZweiseitig = true;
    return feature;
  });
  return collection;
}
