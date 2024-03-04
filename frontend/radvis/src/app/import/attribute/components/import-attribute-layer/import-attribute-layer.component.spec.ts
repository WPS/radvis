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

import { waitForAsync } from '@angular/core/testing';
import { Collection, MapBrowserEvent } from 'ol';
import Feature from 'ol/Feature';
import { GeoJSONFeature, GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Geometry, LineString } from 'ol/geom';
import { Modify } from 'ol/interaction';
import { ModifyEvent } from 'ol/interaction/Modify';
import { Subject, of } from 'rxjs';
import {
  FeatureTyp,
  ImportAttributeLayerComponent,
} from 'src/app/import/attribute/components/import-attribute-layer/import-attribute-layer.component';
import { MappedGrundnetzkante } from 'src/app/import/attribute/models/mapped-grundnetzkante';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(ImportAttributeLayerComponent.name, () => {
  let component: ImportAttributeLayerComponent;
  let olMapService: OlMapService;
  let attributeImportService: AttributeImportService;

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    attributeImportService = mock(AttributeImportService);
    component = new ImportAttributeLayerComponent(instance(olMapService), instance(attributeImportService));
  });

  describe('createDummyFeatureColleaction', () => {
    beforeEach(() => {
      component.radvisFeatures = createDummyRadVisFeatureCollection();
      component.featureMappings = createDummyMappingsFeatureCollection();
    });

    describe('ngOnInit', () => {
      it('should add all features from input to radvisVectorSource', () => {
        component.ngOnInit();

        expect(component['radvisVectorSource'].getFeatures().length).toBe(2);
        expect(component['radvisVectorSource'].getFeatures()[0].getId()).toBe('1');
        expect(component['radvisVectorSource'].getFeatures()[1].getId()).toBe('2');
      });

      it('should create features in mappingsLayer correctly', () => {
        component.ngOnInit();

        expect(component['mappingsVectorSource'].getFeatures().length).toBe(6);

        expect(component['mappingsVectorSource'].getFeatures()[0].get('featureTyp')).toEqual(
          FeatureTyp.MAPPED_GRUNDNETZKANTE
        );
        expect(
          (component['mappingsVectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()
        ).toEqual([
          [1, 1],
          [1.5, 1.5],
        ]);

        expect(component['mappingsVectorSource'].getFeatures()[1].get('featureTyp')).toEqual(
          FeatureTyp.IMPORTIERT_MIT_MAPPING
        );
        expect(
          (component['mappingsVectorSource'].getFeatures()[1].getGeometry() as LineString).getCoordinates()
        ).toEqual([
          [1, 1],
          [2, 2],
        ]);

        expect(component['mappingsVectorSource'].getFeatures()[2].get('featureTyp')).toEqual(
          FeatureTyp.MAPPED_GRUNDNETZKANTE
        );
        expect(
          (component['mappingsVectorSource'].getFeatures()[2].getGeometry() as LineString).getCoordinates()
        ).toEqual([
          [1.5, 1.5],
          [2, 2],
        ]);

        expect(component['mappingsVectorSource'].getFeatures()[3].get('featureTyp')).toEqual(
          FeatureTyp.MAPPED_GRUNDNETZKANTE
        );
        expect(
          (component['mappingsVectorSource'].getFeatures()[3].getGeometry() as LineString).getCoordinates()
        ).toEqual([
          [3, 3],
          [4, 4],
        ]);

        expect(component['mappingsVectorSource'].getFeatures()[4].get('featureTyp')).toEqual(
          FeatureTyp.IMPORTIERT_MIT_MAPPING
        );
        expect(
          (component['mappingsVectorSource'].getFeatures()[4].getGeometry() as LineString).getCoordinates()
        ).toEqual([
          [3, 3],
          [4, 4],
        ]);

        expect(component['mappingsVectorSource'].getFeatures()[5].get('featureTyp')).toEqual(
          FeatureTyp.IMPORTIERT_OHNE_MAPPING
        );
        expect(
          (component['mappingsVectorSource'].getFeatures()[5].getGeometry() as LineString).getCoordinates()
        ).toEqual([
          [10, 10],
          [12, 12],
        ]);
      });
    });

    describe('with mapClickedSubject', () => {
      let mapClicked: Subject<MapBrowserEvent>;

      beforeEach(() => {
        component.editable = true;
        mapClicked = new Subject<MapBrowserEvent>();
        when(olMapService.click$()).thenReturn(mapClicked);

        component.ngOnInit();
      });

      describe('Remove highlighting', () => {
        it('should remove highlight on click if no feature is present', () => {
          const clickEvent = ({
            originalEvent: {
              ctrlKey: false,
              metaKey: false,
            },
          } as unknown) as MapBrowserEvent;
          const clickedFeature = component['mappingsVectorSource'].getFeatureById('3');

          when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([clickedFeature]).thenReturn([]);

          mapClicked.next(clickEvent);

          expect(component['modifySource'].getFeatures()).toHaveSize(1);
          expect(component['highlightedFeatureIds']).toHaveSize(2);

          mapClicked.next(clickEvent);

          expect(component['modifySource'].getFeatures()).toHaveSize(0);
          expect(component['highlightedFeatureIds']).toHaveSize(0);
        });
      });

      describe('Highlight the Mapping', () => {
        let clickedFeature: Feature;

        beforeEach(() => {
          const clickEvent = ({
            originalEvent: {
              ctrlKey: false,
              metaKey: false,
            },
          } as unknown) as MapBrowserEvent;

          clickedFeature = component['mappingsVectorSource'].getFeatureById('3');
          when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([clickedFeature]);

          mapClicked.next(clickEvent);
        });

        it('should highlight the associated Kante on click on the imported Kante', () => {
          const associatedFeature = component['mappingsVectorSource'].getFeatureById('3-1');
          expect(associatedFeature.get(ImportAttributeLayerComponent['ASSOCIATED_KEY'])).toBeTrue();
          expect(component['highlightedFeatureIds']).toContain('3-1');
        });

        it('should add modifyInteraction', () => {
          verify(olMapService.addInteraction(anything())).once();
          const interaction = capture(olMapService.addInteraction).last()[0];
          expect(interaction).toBeInstanceOf(Modify);

          expect(component['modifySource'].getFeatures()).toHaveSize(1);
          const modifyFeature = component['modifySource'].getFeatures()[0];
          expect(modifyFeature.getId()).toEqual(clickedFeature.getId());
        });
      });

      describe('with modifyEnd event', () => {
        let modifyEvent: ModifyEvent;

        beforeEach(done => {
          const feature = component['mappingsVectorSource'].getFeatureById('3');
          const features = new Collection<Feature<Geometry>>();
          features.push(feature);
          modifyEvent = {
            features,
          } as ModifyEvent;

          const clickEvent = ({
            originalEvent: {
              ctrlKey: false,
              metaKey: false,
            },
          } as unknown) as MapBrowserEvent;

          when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([feature]).thenReturn([]);

          mapClicked.next(clickEvent);

          const resultGeoJSONFeature: GeoJSONFeature = {
            type: 'Feature',
            properties: {
              mappedGrundnetzkanten: [
                {
                  kanteId: 1,
                  linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
                },
              ] as MappedGrundnetzkante[],
            },
            geometry: {
              type: 'LineString',
              coordinates: [
                [1, 2],
                [2, 3],
              ],
            },
            id: '3',
          };
          when(attributeImportService.updateMappedGrundnetzkanten(anything())).thenReturn(of(resultGeoJSONFeature));

          component['rematchingRequests$'].subscribe(() => {
            done();
          });
          component['onModifyEnd'](modifyEvent);
        });

        it('should invoke updateMappedGrundnetzkanten', () => {
          verify(attributeImportService.updateMappedGrundnetzkanten(anything())).once();
          const command = capture(attributeImportService.updateMappedGrundnetzkanten).last()[0];
          expect(command).toEqual({
            featuremappingID: 3,
            updatedLinestring: {
              coordinates: (modifyEvent.features.item(0).getGeometry() as LineString).getCoordinates(),
              type: 'LineString',
            },
          });
        });

        it('should update Feature', () => {
          const kante = component['mappingsVectorSource'].getFeatureById('3-1');
          expect(kante).toBeTruthy();
          expect(kante.get(ImportAttributeLayerComponent['ASSOCIATED_KEY'])).toEqual(true);
          expect(component['highlightedFeatureIds']).toContain('3-1');

          expect(
            (component['mappingsVectorSource'].getFeatureById('3').getGeometry() as LineString).getCoordinates()
          ).toEqual([
            [1, 2],
            [2, 3],
          ]);
        });
      });

      describe('deleteMappedGrundnetzkantenAtPositionOf', () => {
        let clickEvent: MapBrowserEvent;

        beforeEach(() => {
          clickEvent = ({
            originalEvent: {
              ctrlKey: true,
              metaKey: false,
            },
            coordinate: [1.5, 1.5],
          } as unknown) as MapBrowserEvent;
        });

        describe('with feature without mappedGrundnetzkante', () => {
          beforeEach(() => {
            const clickedFeature = component['mappingsVectorSource'].getFeatureById('3');
            when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([clickedFeature]);
            when(olMapService.getFeaturesAtCoordinate(anything(), anything(), anything())).thenReturn([clickedFeature]);

            mapClicked.next(clickEvent);
          });

          it('should not deleteMappedGrundnetzkante', () => {
            verify(attributeImportService.deleteMappedGrundnetzkanten(anything())).never();
            expect().nothing();
          });
        });

        describe('with feature with mappedGrundnetzkante', () => {
          beforeEach(
            waitForAsync(() => {
              const clickedFeature = component['mappingsVectorSource'].getFeatureById('3-1');
              when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([clickedFeature]);
              when(olMapService.getFeaturesAtCoordinate(anything(), anything(), anything())).thenReturn([
                clickedFeature,
              ]);

              const resultFeatureCollection: GeoJSONFeatureCollection = {
                type: 'FeatureCollection',
                features: [
                  {
                    type: 'Feature',
                    properties: {},
                    geometry: {
                      type: 'LineString',
                      coordinates: [
                        [1, 1],
                        [2, 2],
                      ],
                    },
                    id: '3',
                  },
                ],
              };
              when(attributeImportService.deleteMappedGrundnetzkanten(anything())).thenReturn(
                of(resultFeatureCollection).toPromise()
              );

              mapClicked.next(clickEvent);
            })
          );

          it('should delete a mappedGrundnetzkante', () => {
            verify(attributeImportService.deleteMappedGrundnetzkanten(anything())).once();
            const command = capture(attributeImportService.deleteMappedGrundnetzkanten).last()[0];
            expect(command).toHaveSize(1);
            expect(command[0]).toEqual({ featureMappingId: 3, kanteId: 1 });

            const kante = component['mappingsVectorSource'].getFeatureById('3');
            expect(kante).toBeTruthy();
            expect(kante.get(ImportAttributeLayerComponent['FEATURE_TYP_KEY'])).toEqual(
              FeatureTyp.IMPORTIERT_OHNE_MAPPING
            );
          });
        });
      });
    });
  });
});

const createDummyRadVisFeatureCollection = (): GeoJSONFeatureCollection => {
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: [
            [1, 1],
            [2, 2],
          ],
        },
        id: '1',
      },
      {
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: [
            [3, 3],
            [4, 4],
          ],
        },
        id: '2',
      },
    ],
  } as GeoJSONFeatureCollection;
};

const createDummyMappingsFeatureCollection = (): GeoJSONFeatureCollection => {
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: {
          mappedGrundnetzkanten: [
            {
              kanteId: 1,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
          ] as MappedGrundnetzkante[],
        },
        geometry: {
          type: 'LineString',
          coordinates: [
            [1, 1],
            [2, 2],
          ],
        },
        id: '3',
      },
      {
        type: 'Feature',
        properties: {
          mappedGrundnetzkanten: [
            { kanteId: 1, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            {
              kanteId: 2,
              linearReferenzierterAbschnitt: { von: 0, bis: 1 },
            },
          ] as MappedGrundnetzkante[],
        },
        geometry: {
          type: 'LineString',
          coordinates: [
            [3, 3],
            [4, 4],
          ],
        },
        id: '4',
      },
      {
        type: 'Feature',
        properties: {
          mappedGrundnetzkanten: [] as MappedGrundnetzkante[],
        },
        geometry: {
          type: 'LineString',
          coordinates: [
            [10, 10],
            [12, 12],
          ],
        },
        id: '5',
      },
    ],
  } as GeoJSONFeatureCollection;
};
