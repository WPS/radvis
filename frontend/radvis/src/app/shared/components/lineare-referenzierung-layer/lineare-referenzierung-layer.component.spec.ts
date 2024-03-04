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
import { Component, SimpleChange, SimpleChanges, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Collection, Feature, MapBrowserEvent } from 'ol';
import { Geometry, LineString, Point } from 'ol/geom';
import { ModifyEvent, ModifyEventType } from 'ol/interaction/Modify';
import { Subject, of } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { LineareReferenzierungLayerComponent } from 'src/app/shared/components/lineare-referenzierung-layer/lineare-referenzierung-layer.component';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, instance, mock, verify, when } from 'ts-mockito';

describe('LineareReferenzierungLayerComponent', () => {
  let component: LineareReferenzierungLayerComponent;
  let olMapService: OlMapService;

  const getResolutionSubject = new Subject<number>();

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    when(olMapService.click$()).thenReturn(of());
    when(olMapService.pointerMove$()).thenReturn(of());
    when(olMapService.pointerLeave$()).thenReturn(of());
    when(olMapService.getResolution$()).thenReturn(getResolutionSubject.asObservable());
    component = new LineareReferenzierungLayerComponent(instance(olMapService), instance(mock(NotifyUserService)));
  });

  describe('ngOnChanges', () => {
    it('should redraw on changed segmentierung', () => {
      const redrawSpy = spyOn<any>(component, 'redraw');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges(({ segmentierung: {} } as unknown) as SimpleChanges);

      expect(redrawSpy).toHaveBeenCalled();
    });

    it('should not redraw and reset indices on changed indices', () => {
      const redrawSpy = spyOn<any>(component, 'redraw');
      const selectSegmentsOnIndicesSpy = spyOn<any>(component, 'selectSegmentsOnIndices');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges(({ selectedIndices: {} } as unknown) as SimpleChanges);

      expect(redrawSpy).not.toHaveBeenCalled();
      expect(selectSegmentsOnIndicesSpy).toHaveBeenCalled();
    });

    it('should update minZoom of layers and interaction', () => {
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];
      component.minZoom = 5;

      when(olMapService.getCurrentResolution()).thenReturn(3);
      when(olMapService.getZoomForResolution(anything())).thenReturn(3);

      expect(component['interactionAdded']).toBeTrue();

      component.ngOnChanges(({ minZoom: new SimpleChange(undefined, 5, false) } as unknown) as SimpleChanges);

      expect(component['shiftableSegmentPointsLayer'].getMinZoom()).toEqual(5);
      expect(component['selektionLayer'].getMinZoom()).toEqual(5);
      expect(component['selectableSegmentLinesLayer'].getMinZoom()).toEqual(5);
      verify(olMapService.removeInteraction(component['modifyInteraction'])).once();
      expect(component['interactionAdded']).toBeFalse();
    });
  });

  describe('feature layer', () => {
    it('should project relative segments on geometry without start and endpoint', () => {
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges(({ segmentierung: {} } as unknown) as SimpleChanges);

      expect(
        component['shiftableSegmentPointsSource'].getFeatures().map(f => (f.getGeometry() as Point).getCoordinates())
      ).toEqual([
        [0, 2.5],
        [0, 7.5],
      ]);
    });
  });

  describe('onModifyStart', () => {
    it('should set correct modifiedIndex', () => {
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges(({ segmentierung: {} } as unknown) as SimpleChanges);

      const event = new ModifyEvent(
        'modifystart' as ModifyEventType,
        new Collection([component['shiftableSegmentPointsSource'].getFeatures()[0]]),
        (undefined as unknown) as MapBrowserEvent
      );

      component['onModifyStart'](event);

      expect(component['modifiedFeatureIndex']).toEqual(1);
    });
  });

  describe('onModifyEnd', () => {
    it('should calculate correct fraction', () => {
      const eventSpy = spyOn(component.segmentierungChanged, 'next');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges({});

      const event = new ModifyEvent(
        'modifyend' as ModifyEventType,
        new Collection([new Feature(new Point([0, 5]))]),
        (undefined as unknown) as MapBrowserEvent
      );
      component['modifiedFeatureIndex'] = 1;

      component['onModifyEnd'](event);

      expect(eventSpy).toHaveBeenCalled();
      expect(eventSpy.calls.mostRecent().args[0]).toEqual([0, 0.5, 0.75, 1]);
      expect(component['modifiedFeatureIndex']).toBeNull();
    });

    it('should calculate correct fraction when not on line', () => {
      const eventSpy = spyOn(component.segmentierungChanged, 'next');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges({});

      const event = new ModifyEvent(
        'modifyend' as ModifyEventType,
        new Collection([new Feature(new Point([5, 5]))]),
        (undefined as unknown) as MapBrowserEvent
      );
      component['modifiedFeatureIndex'] = 1;

      component['onModifyEnd'](event);

      expect(eventSpy).toHaveBeenCalled();
      expect(eventSpy.calls.mostRecent().args[0]).toEqual([0, 0.5, 0.75, 1]);
      expect(component['modifiedFeatureIndex']).toBeNull();
    });

    it('should reset segmentierung if user input not valid (left)', () => {
      const eventSpy = spyOn(component.segmentierungChanged, 'next');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges({});

      const event = new ModifyEvent(
        'modifyend' as ModifyEventType,
        new Collection([new Feature(new Point([0, 1]))]),
        (undefined as unknown) as MapBrowserEvent
      );
      component['modifiedFeatureIndex'] = 2;

      component['onModifyEnd'](event);

      expect(eventSpy).not.toHaveBeenCalled();
      expect(
        component['shiftableSegmentPointsSource'].getFeatures().map(f => (f.getGeometry() as Point).getCoordinates())
      ).toEqual([
        [0, 2.5],
        [0, 7.5],
      ]);
      expect(component['modifiedFeatureIndex']).toBeNull();
    });

    it('should reset segmentierung if user input not valid (right)', () => {
      const eventSpy = spyOn(component.segmentierungChanged, 'next');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges({});

      const event = new ModifyEvent(
        'modifyend' as ModifyEventType,
        new Collection([new Feature(new Point([0, 8.5]))]),
        (undefined as unknown) as MapBrowserEvent
      );
      component['modifiedFeatureIndex'] = 1;

      component['onModifyEnd'](event);

      expect(eventSpy).not.toHaveBeenCalled();
      expect(
        component['shiftableSegmentPointsSource'].getFeatures().map(f => (f.getGeometry() as Point).getCoordinates())
      ).toEqual([
        [0, 2.5],
        [0, 7.5],
      ]);
      expect(component['modifiedFeatureIndex']).toBeNull();
    });

    it('should reset segmentierung if user input not valid (exact right)', () => {
      const eventSpy = spyOn(component.segmentierungChanged, 'next');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges({});

      const event = new ModifyEvent(
        'modifyend' as ModifyEventType,
        new Collection([new Feature(new Point([0, 11]))]),
        (undefined as unknown) as MapBrowserEvent
      );
      component['modifiedFeatureIndex'] = 2;

      component['onModifyEnd'](event);

      expect(eventSpy).not.toHaveBeenCalledWith([0, 0.25, 1, 1]);
      expect(
        component['shiftableSegmentPointsSource'].getFeatures().map(f => (f.getGeometry() as Point).getCoordinates())
      ).toEqual([
        [0, 2.5],
        [0, 7.5],
      ]);
      expect(component['modifiedFeatureIndex']).toBeNull();
    });

    it('should reset segmentierung if user input not valid (exact left)', () => {
      const eventSpy = spyOn(component.segmentierungChanged, 'next');
      component.originalGeometry = new LineString([
        [0, 1],
        [0, 11],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      component.ngOnChanges({});

      const event = new ModifyEvent(
        'modifyend' as ModifyEventType,
        new Collection([new Feature(new Point([0, 0.5]))]),
        (undefined as unknown) as MapBrowserEvent
      );
      component['modifiedFeatureIndex'] = 1;

      component['onModifyEnd'](event);

      expect(eventSpy).not.toHaveBeenCalledWith([0, 0, 0.75, 1]);
      expect(
        component['shiftableSegmentPointsSource'].getFeatures().map(f => (f.getGeometry() as Point).getCoordinates())
      ).toEqual([
        [0, 3.5],
        [0, 8.5],
      ]);
      expect(component['modifiedFeatureIndex']).toBeNull();
    });
  });

  describe('modificationDisabled', () => {
    it('should inactivate ModifyInteraction when modification is disabled', () => {
      const spyOnModifyInteraction = spyOn(component['modifyInteraction'], 'setActive');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      expect(component.modificationDisabled).toBeFalse();

      component.modificationDisabled = true;

      component.ngOnChanges(({ modificationDisabled: true } as unknown) as SimpleChanges);

      expect(spyOnModifyInteraction).toHaveBeenCalledWith(false);
    });

    it('should activate ModifyInteraction when modification is enabled', () => {
      const spyOnModifyInteraction = spyOn(component['modifyInteraction'], 'setActive');
      component.originalGeometry = new LineString([
        [0, 0],
        [0, 10],
      ]);
      component.segmentierung = [0, 0.25, 0.75, 1];

      expect(component.modificationDisabled).toBeFalse();

      component.modificationDisabled = true;

      component.ngOnChanges(({ modificationDisabled: true } as unknown) as SimpleChanges);

      expect(spyOnModifyInteraction).toHaveBeenCalledWith(false);

      component.modificationDisabled = false;

      component.ngOnChanges(({ modificationDisabled: true } as unknown) as SimpleChanges);

      expect(spyOnModifyInteraction).toHaveBeenCalledWith(true);
    });
  });

  describe('onMapClick', () => {
    it('should do nothing when no features are under cursor', () => {
      const spyOnSelectElement = spyOn(component['selectElement'], 'emit');
      const spyOnDeselectElement = spyOn(component['deselectElement'], 'emit');
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<UIEvent>);

      expect(spyOnSelectElement).not.toHaveBeenCalled();
      expect(spyOnDeselectElement).not.toHaveBeenCalled();
    });

    it('should do nothing when nearest feature is not on correct layer', () => {
      const spyOnSelectElement = spyOn(component['selectElement'], 'emit');
      const spyOnDeselectElement = spyOn(component['deselectElement'], 'emit');
      const someFeature = new Feature(
        new LineString([
          [23, 77],
          [34, 66],
        ])
      );
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([someFeature]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<UIEvent>);

      expect(spyOnSelectElement).not.toHaveBeenCalled();
      expect(spyOnDeselectElement).not.toHaveBeenCalled();
    });

    it('should select nearest feature non-additiv on normal click', () => {
      const spyOnSelectElement = spyOn(component['selectElement'], 'emit');
      const spyOnDeselectElement = spyOn(component['deselectElement'], 'emit');
      const features = createDummyFeatures();
      component['selectableSegmentLinesSource'].addFeatures(features);
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn(features);

      component['onMapClick'](({
        pixel: [0, 0],
        coordinate: [100, 40],
        originalEvent: { ctrlKey: false, metaKey: false } as PointerEvent,
      } as unknown) as MapBrowserEvent<UIEvent>);

      expect(spyOnSelectElement).toHaveBeenCalledWith({ index: 0, additiv: false, clickedCoordinate: [100, 40] });
      expect(spyOnDeselectElement).not.toHaveBeenCalled();
    });

    it('should select nearest feature additiv on Kante selection with toggle', () => {
      const spyOnSelectElement = spyOn(component['selectElement'], 'emit');
      const spyOnDeselectElement = spyOn(component['deselectElement'], 'emit');
      const features = createDummyFeatures();
      component['selectableSegmentLinesSource'].addFeatures(features);
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn(features);
      component['selectedIndices'] = [1];

      component['onMapClick'](({
        pixel: [0, 0],
        coordinate: [100, 40],
        originalEvent: { ctrlKey: true, metaKey: false } as PointerEvent,
      } as unknown) as MapBrowserEvent<UIEvent>);

      expect(spyOnSelectElement).toHaveBeenCalledWith({ index: 0, additiv: true, clickedCoordinate: [100, 40] });
      expect(spyOnDeselectElement).not.toHaveBeenCalled();
    });

    it('should deselect nearest feature on toggle deselection of kante', () => {
      const spyOnSelectElement = spyOn(component['selectElement'], 'emit');
      const spyOnDeselectElement = spyOn(component['deselectElement'], 'emit');
      const features = createDummyFeatures();
      component['selectableSegmentLinesSource'].addFeatures(features);
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn(features);
      component['selectedIndices'] = [0];

      component['onMapClick'](({
        pixel: [0, 0],
        originalEvent: { ctrlKey: true, metaKey: false } as PointerEvent,
      } as unknown) as MapBrowserEvent<UIEvent>);

      expect(spyOnDeselectElement).toHaveBeenCalledWith(0);
      expect(spyOnSelectElement).not.toHaveBeenCalled();
    });
  });

  describe('resolutionChange', () => {
    it('should remove interaction if resolution below minimum', () => {
      component.minZoom = 16;

      expect(component['interactionAdded']).toBeTrue();
      verify(olMapService.addInteraction(component['modifyInteraction'])).once();

      when(olMapService.getZoomForResolution(anything())).thenReturn(15.99);
      getResolutionSubject.next(15.99);

      verify(olMapService.removeInteraction(component['modifyInteraction'])).once();
      expect(component['interactionAdded']).toBeFalse();

      when(olMapService.getZoomForResolution(anything())).thenReturn(16.01);
      getResolutionSubject.next(16.01);

      verify(olMapService.addInteraction(component['modifyInteraction'])).twice();
      expect(component['interactionAdded']).toBeTrue();
    });
  });

  describe('hover', () => {
    describe('onMapPointerMove', () => {
      it('should unhover when no features under pointer', () => {
        when(olMapService.getFeaturesAtPixel(anything(), anything(), anything())).thenReturn([]);
        const hoveredSegmentIndexChangedSpy = spyOn(component.hoveredSegmentIndexChanged, 'emit');
        component['hoveredSegmentIndex'] = 2;

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<UIEvent>);

        expect(component.hoveredSegmentIndex).toBeNull();
        expect(hoveredSegmentIndexChangedSpy).toHaveBeenCalledWith(null);
      });

      it('should do nothing when still no feature under pointer', () => {
        when(olMapService.getFeaturesAtPixel(anything(), anything(), anything())).thenReturn([]);
        const hoveredSegmentIndexChangedSpy = spyOn(component.hoveredSegmentIndexChanged, 'emit');
        component['hoveredSegmentIndex'] = null;

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<UIEvent>);

        expect(component.hoveredSegmentIndex).toBeNull();
        expect(hoveredSegmentIndexChangedSpy).not.toHaveBeenCalledWith(null);
      });

      it('should change hovered segment index', () => {
        const features = createDummyFeatures();
        component['selectableSegmentLinesSource'].addFeatures(features);
        const firstFeature = component['selectableSegmentLinesLayer'].getSource().getFeatures()[0];
        const secondFeature = component['selectableSegmentLinesLayer'].getSource().getFeatures()[1];
        when(olMapService.getFeaturesAtPixel(anything(), anything(), anything())).thenReturn([
          firstFeature,
          secondFeature,
        ]);
        const hoveredSegmentIndexChangedSpy = spyOn(component.hoveredSegmentIndexChanged, 'emit');
        component['hoveredSegmentIndex'] = 4;

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<UIEvent>);

        expect(component.hoveredSegmentIndex).toEqual(0);
        expect(hoveredSegmentIndexChangedSpy).toHaveBeenCalledWith(0);
      });

      it('should unhover when hovered feature not in selectableSegmentLinesLayer', () => {
        const features = createDummyFeatures();
        component['selectableSegmentLinesSource'].addFeature(features[0]);
        when(olMapService.getFeaturesAtPixel(anything(), anything(), anything())).thenReturn([features[1]]);
        const hoveredSegmentIndexChangedSpy = spyOn(component.hoveredSegmentIndexChanged, 'emit');
        component['hoveredSegmentIndex'] = 4;

        component['onMapPointerMove']({ pixel: [0, 1] } as MapBrowserEvent<UIEvent>);

        expect(component.hoveredSegmentIndex).toBeNull();
        expect(hoveredSegmentIndexChangedSpy).toHaveBeenCalledWith(null);
      });
    });

    describe('handleHoverChange', () => {
      it('should unhover previous hovered index', () => {
        const setColorForKanteSpy = spyOn<any>(component, 'setColorForSegment');

        component['handleHoverChange'](3, null);

        expect(setColorForKanteSpy).toHaveBeenCalledTimes(1);
        expect(setColorForKanteSpy.calls.mostRecent().args).toEqual([
          3,
          MapStyles.FEATURE_COLOR,
          MapStyles.FEATURE_SELECT_COLOR,
        ]);
      });

      it('should hover and unhover', () => {
        const setColorForKanteSpy = spyOn<any>(component, 'setColorForSegment');

        component['handleHoverChange'](2, 3);

        expect(setColorForKanteSpy).toHaveBeenCalledTimes(2);
        expect(setColorForKanteSpy.calls.first().args).toEqual([
          2,
          MapStyles.FEATURE_COLOR,
          MapStyles.FEATURE_SELECT_COLOR,
        ]);
        expect(setColorForKanteSpy.calls.mostRecent().args).toEqual([
          3,
          MapStyles.FEATURE_HOVER_COLOR,
          MapStyles.FEATURE_HOVER_COLOR,
        ]);
      });

      it('should hover new index', () => {
        const setColorForKanteSpy = spyOn<any>(component, 'setColorForSegment');

        component['handleHoverChange'](null, 3);

        expect(setColorForKanteSpy).toHaveBeenCalledTimes(1);
        expect(setColorForKanteSpy.calls.mostRecent().args).toEqual([
          3,
          MapStyles.FEATURE_HOVER_COLOR,
          MapStyles.FEATURE_HOVER_COLOR,
        ]);
      });
    });
  });
});

@Component({
  template:
    '<rad-lineare-referenzierung-layer [zIndex]="1" [hoveredSegmentIndex]="hoveredSegmentIndex"  [segmentierung]="segmentierung" [originalGeometry]="geometry"></rad-lineare-referenzierung-layer>',
})
export class LineareReferenzierungLayerTestWrapperComponent {
  @ViewChild(LineareReferenzierungLayerComponent)
  component!: LineareReferenzierungLayerComponent;

  geometry = new LineString([
    [0, 0],
    [0, 10],
  ]);
  segmentierung = [0, 1];
  hoveredSegmentIndex: number | null = null;
}

describe('LineareReferenzierungLayerComponent - embedded', () => {
  let fixture: ComponentFixture<LineareReferenzierungLayerTestWrapperComponent>;
  let component: LineareReferenzierungLayerComponent;
  let wrapper: LineareReferenzierungLayerTestWrapperComponent;
  const olMapService = mock(OlMapComponent);
  when(olMapService.getResolution$()).thenReturn(of());
  when(olMapService.click$()).thenReturn(of());
  when(olMapService.pointerMove$()).thenReturn(of());
  when(olMapService.pointerLeave$()).thenReturn(of());

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        {
          provide: OlMapService,
          useValue: instance(olMapService),
        },
        {
          provide: NotifyUserService,
          useValue: instance(mock(NotifyUserService)),
        },
      ],
      declarations: [LineareReferenzierungLayerComponent, LineareReferenzierungLayerTestWrapperComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LineareReferenzierungLayerTestWrapperComponent);
    wrapper = fixture.componentInstance;
    fixture.detectChanges();
    component = wrapper.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should hover', () => {
    const hoverChangeSpy = spyOn<any>(component, 'handleHoverChange');
    wrapper.hoveredSegmentIndex = 0;
    fixture.detectChanges();
    expect(hoverChangeSpy).toHaveBeenCalledTimes(1);
    expect(hoverChangeSpy.calls.mostRecent().args).toEqual([null, 0]);
    wrapper.hoveredSegmentIndex = 1;
    fixture.detectChanges();
    expect(hoverChangeSpy).toHaveBeenCalledTimes(2);
    expect(hoverChangeSpy.calls.mostRecent().args).toEqual([0, 1]);
    wrapper.hoveredSegmentIndex = null;
    fixture.detectChanges();
    expect(hoverChangeSpy).toHaveBeenCalledTimes(3);
    expect(hoverChangeSpy.calls.mostRecent().args).toEqual([1, null]);
  });
});

// eslint-disable-next-line prefer-arrow-functions/prefer-arrow-functions
function createDummyFeatures(): Feature<Geometry>[] {
  const firstFeature = new Feature(
    new LineString([
      [0, 1],
      [2, 3],
    ])
  );
  firstFeature.setId(1);
  const secondFeatue = new Feature(
    new LineString([
      [4, 5],
      [6, 7],
    ])
  );
  secondFeatue.setId(2);
  return [firstFeature, secondFeatue];
}
