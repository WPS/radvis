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
import { BreakpointObserver } from '@angular/cdk/layout';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, Router, RouterState, RouterStateSnapshot } from '@angular/router';
import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { Feature, MapBrowserEvent, Overlay } from 'ol';
import { FeatureLike } from 'ol/Feature';
import { Coordinate } from 'ol/coordinate';
import Geometry from 'ol/geom/Geometry';
import Interaction from 'ol/interaction/Interaction';
import BaseLayer from 'ol/layer/Base';
import Layer from 'ol/layer/Layer';
import { Pixel } from 'ol/pixel';
import Source from 'ol/source/Source';
import { Observable, Subject, of } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KantenFuehrungsformEditorComponent } from 'src/app/editor/kanten/components/kanten-fuehrungsform-editor/kanten-fuehrungsform-editor.component';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { Bordstein } from 'src/app/editor/kanten/models/bordstein';
import { FuehrungsformAttribute } from 'src/app/editor/kanten/models/fuehrungsform-attribute';
import { Kante } from 'src/app/editor/kanten/models/kante';
import {
  defaultFuehrungsformAttribute,
  defaultKante,
} from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { SaveFuehrungsformAttributGruppeCommand } from 'src/app/editor/kanten/models/save-fuehrungsform-attribut-gruppe-command';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { LayerQuelle } from 'src/app/shared/models/layer-quelle';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { SignaturLegende } from 'src/app/shared/models/signatur-legende';
import { WMSLegende } from 'src/app/shared/models/wms-legende';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

/* eslint-disable no-unused-vars */
class TestOlMapService extends OlMapService {
  addInteraction(interaction: Interaction): void {}

  addLayer(olLayer: BaseLayer, quelle?: LayerQuelle, legende?: SignaturLegende | WMSLegende): void {}

  addWMSFeatureLayer(
    olLayer: BaseLayer,
    getFeaturesCallback: (coordinate: number[], resolution: number) => Promise<Feature<Geometry>[]>,
    quelle?: LayerQuelle,
    legende?: SignaturLegende | WMSLegende
  ): void {}

  addOverlay(olPopup: Overlay): void {}

  click$(): Observable<MapBrowserEvent<UIEvent>> {
    return of();
  }

  getCurrentResolution(): number | undefined {
    return undefined;
  }

  getFeaturesAtCoordinate(
    coordinate: Coordinate,
    layerFilter?: (l: Layer<Source>) => boolean,
    hitTolerance?: number
  ): FeatureLike[] | undefined {
    return undefined;
  }

  getFeaturesAtPixel(
    pixel: Pixel,
    layerFilter?: (l: Layer<Source>) => boolean,
    hitTolerance?: number
  ): FeatureLike[] | undefined {
    return undefined;
  }

  getResolution$(): Observable<number> {
    return of();
  }

  getZoomForResolution(resolution: number): number | undefined {
    return undefined;
  }

  locationSelected$(): Observable<LocationSelectEvent> {
    return of();
  }

  onceOnPostRender(listener: () => void): void {}

  outsideMapClick$(): Observable<void> {
    return of();
  }

  pointerLeave$(): Observable<void> {
    return of();
  }

  pointerMove$(): Observable<MapBrowserEvent<UIEvent>> {
    return of();
  }

  removeInteraction(interaction: Interaction): void {}

  removeLayer(olLayer: BaseLayer): void {}

  removeOverlay(olPopup: Overlay): void {}

  resetCursor(): void {}

  scrollIntoViewByCoordinate(coordinate: Coordinate): void {}

  scrollIntoViewByGeometry(geometry: Geometry): void {}

  setCursor(cssClass: string): void {}

  updateLegende(layer: BaseLayer, legende: SignaturLegende | WMSLegende | null): void {}
}

/* eslint-enable no-unused-vars */
describe(KantenFuehrungsformEditorComponent.name, () => {
  let component: KantenFuehrungsformEditorComponent;
  let fixture: MockedComponentFixture<KantenFuehrungsformEditorComponent>;
  let netzService: NetzService;
  let kantenSelektionService: KantenSelektionService;
  let kantenSubject$: Subject<Kante[]>;
  let kantenSelektionSubject$: Subject<KantenSelektion[]>;
  let benutzerDetails: BenutzerDetailsService;
  let olMapService: OlMapService;
  let route: ActivatedRoute;
  let router: Router;

  beforeEach(() => {
    netzService = mock(NetzService);
    benutzerDetails = mock(BenutzerDetailsService);
    olMapService = mock(TestOlMapService);
    route = mock(ActivatedRoute);
    router = mock(Router);

    kantenSelektionService = mock(KantenSelektionService);
    kantenSubject$ = new Subject();
    kantenSelektionSubject$ = new Subject();
    when(kantenSelektionService.selektierteKanten$).thenReturn(kantenSubject$);
    when(kantenSelektionService.selektion$).thenReturn(kantenSelektionSubject$);

    return MockBuilder(KantenFuehrungsformEditorComponent, EditorModule)
      .provide({
        provide: NetzService,
        useValue: instance(netzService),
      })
      .provide({
        provide: BenutzerDetailsService,
        useValue: instance(benutzerDetails),
      })
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: ActivatedRoute,
        useValue: instance(route),
      })
      .provide({
        provide: Router,
        useValue: instance(router),
      })
      .provide({
        provide: KantenSelektionService,
        useValue: instance(kantenSelektionService),
      })
      .keep(BreakpointObserver);
  });

  beforeEach(() => {
    fixture = MockRender(KantenFuehrungsformEditorComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('fillForm', () => {
    it('should set undetermined correct, seite', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.ASPHALT }],
            fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
          },
        }),
      ]);
      tick();

      const { belagArt, ...equalValues } = component.displayedAttributeformGroup.value;

      expect(belagArt).toBeInstanceOf(UndeterminedValue);
      expect(equalValues).toEqual({
        oberflaechenbeschaffenheit: defaultFuehrungsformAttribute.oberflaechenbeschaffenheit,
        bordstein: defaultFuehrungsformAttribute.bordstein,
        radverkehrsfuehrung: defaultFuehrungsformAttribute.radverkehrsfuehrung,
        benutzungspflicht: defaultFuehrungsformAttribute.benutzungspflicht,
        breite: defaultFuehrungsformAttribute.breite,
        parkenTyp: defaultFuehrungsformAttribute.parkenTyp,
        parkenForm: defaultFuehrungsformAttribute.parkenForm,
      });
    }));

    it('should set undetermined correct, segment', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeLinks: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.ASPHALT },
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          Seitenbezug.LINKS,
          2
        ),
      ]);
      tick();

      const { belagArt, ...equalValues } = component.displayedAttributeformGroup.value;

      expect(belagArt).toBeInstanceOf(UndeterminedValue);
      expect(equalValues).toEqual({
        oberflaechenbeschaffenheit: defaultFuehrungsformAttribute.oberflaechenbeschaffenheit,
        bordstein: defaultFuehrungsformAttribute.bordstein,
        radverkehrsfuehrung: defaultFuehrungsformAttribute.radverkehrsfuehrung,
        benutzungspflicht: defaultFuehrungsformAttribute.benutzungspflicht,
        breite: defaultFuehrungsformAttribute.breite,
        parkenTyp: defaultFuehrungsformAttribute.parkenTyp,
        parkenForm: defaultFuehrungsformAttribute.parkenForm,
      });
    }));

    it('should set undetermined correct, multiple kanten', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeLinks: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.ASPHALT }],
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          Seitenbezug.LINKS
        ),
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
              fuehrungsformAttributeLinks: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          Seitenbezug.RECHTS
        ),
      ]);
      tick();

      const { belagArt, ...equalValues } = component.displayedAttributeformGroup.value;

      expect(belagArt).toBeInstanceOf(UndeterminedValue);
      expect(equalValues).toEqual({
        oberflaechenbeschaffenheit: defaultFuehrungsformAttribute.oberflaechenbeschaffenheit,
        bordstein: defaultFuehrungsformAttribute.bordstein,
        radverkehrsfuehrung: defaultFuehrungsformAttribute.radverkehrsfuehrung,
        benutzungspflicht: defaultFuehrungsformAttribute.benutzungspflicht,
        breite: defaultFuehrungsformAttribute.breite,
        parkenTyp: defaultFuehrungsformAttribute.parkenTyp,
        parkenForm: defaultFuehrungsformAttribute.parkenForm,
      });
    }));

    it('should set segmentierung correct, zweiseitig', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.7 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
            ],
          },
        }),
      ]);
      tick();

      const lrLinks: LinearReferenzierterAbschnitt[][] = component.lineareReferenzenLinksFormArray.value;
      const lrEinseitig: LinearReferenzierterAbschnitt[][] = component.lineareReferenzenFormArray.value;
      const lrRechts: LinearReferenzierterAbschnitt[][] = component.lineareReferenzenRechtsFormArray.value;

      expect(lrLinks).toEqual([
        [
          { von: 0, bis: 0.5 },
          { von: 0.5, bis: 1 },
        ],
        [{ von: 0, bis: 1 }],
      ]);

      expect(lrEinseitig).toEqual([
        [
          { von: 0, bis: 0.5 },
          { von: 0.5, bis: 1 },
        ],
        [{ von: 0, bis: 1 }],
      ]);

      expect(lrRechts).toEqual([
        [
          { von: 0, bis: 0.7 },
          { von: 0.7, bis: 1 },
        ],
        [
          { von: 0, bis: 0.9 },
          { von: 0.9, bis: 1 },
        ],
      ]);
    }));
  });

  describe('onSave', () => {
    beforeEach(() => {
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve();
    });

    it('should read undefined values correct, multiple kanten', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            id: 1,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeLinks: [
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.ASPHALT,
                  bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
                },
              ],
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          Seitenbezug.LINKS
        ),
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            id: 2,
            fuehrungsformAttributGruppe: {
              id: 2,
              version: 3,
              fuehrungsformAttributeLinks: [
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.NATURSTEINPFLASTER,
                  bordstein: Bordstein.KEINE_ABSENKUNG,
                },
              ],
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          Seitenbezug.LINKS
        ),
      ];
      setupSelektion(selektion);

      tick();
      component.displayedAttributeformGroup.patchValue({
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
      });
      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.ASPHALT,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
            },
          ],
          fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
        } as SaveFuehrungsformAttributGruppeCommand,
        {
          kanteId: 2,
          gruppenID: 2,
          gruppenVersion: 3,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.NATURSTEINPFLASTER,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
            },
          ],
          fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));

    it('should read undefined values correct, seite', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          id: 1,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                belagArt: BelagArt.ASPHALT,
                bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
              },
            ],
            fuehrungsformAttributeRechts: [
              {
                ...defaultFuehrungsformAttribute,
                belagArt: BelagArt.NATURSTEINPFLASTER,
                bordstein: Bordstein.KEINE_ABSENKUNG,
              },
            ],
          },
        }),
      ];
      setupSelektion(selektion);

      tick();
      component.displayedAttributeformGroup.patchValue({
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
      });
      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.ASPHALT,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.NATURSTEINPFLASTER,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
            },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));

    it('should read undefined values correct, segment', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            id: 1,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeLinks: [
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.ASPHALT,
                  bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
                },
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.NATURSTEINPFLASTER,
                  bordstein: Bordstein.KEINE_ABSENKUNG,
                },
              ],
              fuehrungsformAttributeRechts: [
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.NATURSTEINPFLASTER,
                },
              ],
            },
          },
          Seitenbezug.LINKS,
          2
        ),
      ];
      setupSelektion(selektion);

      tick();
      component.displayedAttributeformGroup.patchValue({
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
      });
      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.ASPHALT,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
              trennstreifenFormLinks: null,
              trennstreifenTrennungZuLinks: null,
              trennstreifenBreiteLinks: null,
              trennstreifenFormRechts: null,
              trennstreifenTrennungZuRechts: null,
              trennstreifenBreiteRechts: null,
            },
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.NATURSTEINPFLASTER,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
              trennstreifenFormLinks: null,
              trennstreifenTrennungZuLinks: null,
              trennstreifenBreiteLinks: null,
              trennstreifenFormRechts: null,
              trennstreifenTrennungZuRechts: null,
              trennstreifenBreiteRechts: null,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.NATURSTEINPFLASTER,
            },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));

    it('should read lineare referenzen, einseitig', fakeAsync(() => {
      setupSelektion([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.7 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
            ],
          },
        }),
      ]);
      tick();

      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.76 },
        { von: 0.76, bis: 1 },
      ]);

      component.lineareReferenzenRechtsFormArray.controls[1].setValue([
        { von: 0, bis: 0.8 },
        { von: 0.8, bis: 1 },
      ]);

      component.lineareReferenzenLinksFormArray.markAsDirty();
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      component.onSave();
      tick();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(
        capture(netzService.saveKanteFuehrungsform)
          .last()[0]
          .map(command => command.fuehrungsformAttributeLinks.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.76 },
          { von: 0.76, bis: 1 },
        ],
        [{ von: 0, bis: 1 }],
      ]);
      expect(
        capture(netzService.saveKanteFuehrungsform)
          .last()[0]
          .map(command => command.fuehrungsformAttributeRechts.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.7 },
          { von: 0.7, bis: 1 },
        ],
        [
          { von: 0, bis: 0.8 },
          { von: 0.8, bis: 1 },
        ],
      ]);
    }));

    it('should read lineare referenzen, zweiseitig', fakeAsync(() => {
      setupSelektion([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zweiseitig: false,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.7 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zweiseitig: true,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
            ],
          },
        }),
      ]);
      tick();

      component.lineareReferenzenFormArray.controls[0].setValue([
        { von: 0, bis: 0.76 },
        { von: 0.76, bis: 1 },
      ]);
      tick();

      component.lineareReferenzenRechtsFormArray.controls[1].setValue([
        { von: 0, bis: 0.8 },
        { von: 0.8, bis: 1 },
      ]);
      tick();

      component.lineareReferenzenFormArray.markAsDirty();
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      component.onSave();
      tick();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(
        capture(netzService.saveKanteFuehrungsform)
          .last()[0]
          .map(command => command.fuehrungsformAttributeLinks.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.76 },
          { von: 0.76, bis: 1 },
        ],
        [{ von: 0, bis: 1 }],
      ]);
      expect(
        capture(netzService.saveKanteFuehrungsform)
          .last()[0]
          .map(command => command.fuehrungsformAttributeRechts.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.76 },
          { von: 0.76, bis: 1 },
        ],
        [
          { von: 0, bis: 0.8 },
          { von: 0.8, bis: 1 },
        ],
      ]);
    }));
  });

  describe('edit RadNETZ', () => {
    it('should disable control if RadNETZ-Kante is selected', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      setupSelektion([KantenSelektion.ofGesamteKante(kante)]);

      tick();

      expect(component.displayedAttributeformGroup.disabled).toBeTrue();
      expect(component.lineareReferenzenFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenLinksFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenRechtsFormArray.disabled).toBeTrue();
    }));

    it('should reanable controls when last RadNETZ-Kante is deselected', fakeAsync(() => {
      const kanteRadNETZ: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      const kanteDLM: Kante = {
        ...defaultKante,
        quelle: QuellSystem.DLM,
      };
      setupSelektion([kanteRadNETZ, kanteDLM].map(k => KantenSelektion.ofGesamteKante(k)));

      tick();

      expect(component.displayedAttributeformGroup.disabled).toBeTrue();
      expect(component.lineareReferenzenFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenLinksFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenRechtsFormArray.disabled).toBeTrue();

      setupSelektion([KantenSelektion.ofGesamteKante(kanteDLM)]);

      tick();

      expect(component.displayedAttributeformGroup.disabled).toBeFalse();
      expect(component.lineareReferenzenFormArray.disabled).toBeFalse();
      expect(component.lineareReferenzenLinksFormArray.disabled).toBeFalse();
      expect(component.lineareReferenzenRechtsFormArray.disabled).toBeFalse();
    }));
  });

  const setupSelektion = (selektion: KantenSelektion[]): void => {
    when(kantenSelektionService.selektion).thenReturn(selektion);
    when(kantenSelektionService.selektierteKanten).thenReturn(selektion.map(s => s.kante));
    kantenSubject$.next(selektion.map(s => s.kante));
    kantenSelektionSubject$.next(selektion);
  };
});

describe(KantenFuehrungsformEditorComponent.name + ' - embedded', () => {
  let component: KantenFuehrungsformEditorComponent;
  let fixture: MockedComponentFixture<KantenFuehrungsformEditorComponent>;
  let kantenSelektionService: KantenSelektionService;
  let netzService: NetzService;
  let benutzerDetailsService: BenutzerDetailsService;
  let olMapService: OlMapService;
  let route: ActivatedRoute;
  let router: Router;

  beforeEach(() => {
    const netzbearbeitungsModusService = mock(NetzBearbeitungModusService);

    benutzerDetailsService = mock(BenutzerDetailsService);
    when(netzbearbeitungsModusService.getAktiveKantenGruppe()).thenReturn(of(AttributGruppe.FUEHRUNGSFORM));

    netzService = mock(NetzService);
    when(netzService.saveKanteFuehrungsform(anything())).thenResolve([]);

    olMapService = mock(TestOlMapService);
    route = mock(ActivatedRoute);
    router = mock(Router);

    when(route.snapshot).thenReturn(instance(mock(ActivatedRouteSnapshot)));
    const routerState = mock(RouterState);
    when(routerState.snapshot).thenReturn(instance(mock(RouterStateSnapshot)));
    when(router.routerState).thenReturn(instance(routerState));

    return MockBuilder(KantenFuehrungsformEditorComponent, EditorModule)
      .keep(KantenSelektionService)
      .provide({ provide: NetzService, useValue: instance(netzService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(mock(ErrorHandlingService)) })
      .provide({ provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) })
      .provide({ provide: LadeZustandService, useValue: instance(mock(LadeZustandService)) })
      .provide({ provide: NetzBearbeitungModusService, useValue: instance(netzbearbeitungsModusService) })
      .provide({ provide: BenutzerDetailsService, useValue: instance(benutzerDetailsService) })
      .provide({ provide: OlMapService, useValue: instance(olMapService) })
      .provide({ provide: ActivatedRoute, useValue: instance(route) })
      .provide({ provide: Router, useValue: instance(router) })
      .keep(BreakpointObserver);
  });

  beforeEach(() => {
    fixture = MockRender(KantenFuehrungsformEditorComponent);
    fixture.detectChanges();
    component = fixture.point.componentInstance;
    kantenSelektionService = component['kantenSelektionService'];
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('should not fill lineare referenzen if only segment selection changed', fakeAsync(() => {
    const kante = {
      ...defaultKante,
      fuehrungsformAttributGruppe: {
        id: 1,
        version: 1,
        fuehrungsformAttributeLinks: [
          { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
          { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
        ] as FuehrungsformAttribute[],
        fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
      },
    };
    setupKanten([kante]);

    kantenSelektionService.select(kante.id, true, Seitenbezug.LINKS, undefined);
    tick();

    const clearRechtsSpy = spyOn(component.lineareReferenzenRechtsFormArray, 'clear');
    const clearLinksSpy = spyOn(component.lineareReferenzenLinksFormArray, 'clear');
    const clearEinseitigSpy = spyOn(component.lineareReferenzenFormArray, 'clear');

    kantenSelektionService.deselect(kante.id, Seitenbezug.LINKS, 0);
    tick();

    expect(clearEinseitigSpy).not.toHaveBeenCalled();
    expect(clearLinksSpy).not.toHaveBeenCalled();
    expect(clearRechtsSpy).not.toHaveBeenCalled();
  }));

  describe('onSelectLinearesSegment', () => {
    let kante: Kante;
    beforeEach(() => {
      kante = {
        ...defaultKante,
        zweiseitig: true,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [defaultFuehrungsformAttribute],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);
    });

    it('should select segment without discard guard', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, Seitenbezug.LINKS);
      tick();

      component.onSelectLinearesSegment({ additiv: true, index: 0 }, kante.id, Seitenbezug.RECHTS);
      tick();

      // FIXME:
      // verify(discardGuardService.canDeactivate(anything())).never();
      expect(kantenSelektionService.isSelektiert(kante.id, Seitenbezug.RECHTS)).toBeTrue();
    }));

    it('should select both segments when no seitenbezug', fakeAsync(() => {
      kante = {
        ...defaultKante,
        zweiseitig: false,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
          fuehrungsformAttributeRechts: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
        },
      };
      setupKanten([kante]);

      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();

      component.onDeselectLinearesSegment(0, kante.id);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.RECHTS)).toEqual([1]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS)).toEqual([1]);

      component.onSelectLinearesSegment({ additiv: true, index: 0 }, kante.id);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.RECHTS).sort()).toEqual([0, 1]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([0, 1]);
    }));
  });

  describe('onDeselectLinearesSegment', () => {
    let kante: Kante;
    beforeEach(() => {
      kante = {
        ...defaultKante,
        zweiseitig: true,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [defaultFuehrungsformAttribute],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);
    });

    it('should remove kante from selection if it is last selected element', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, Seitenbezug.LINKS);
      tick();

      component.onDeselectLinearesSegment(0, kante.id, Seitenbezug.LINKS);
      tick();

      // FIXME:
      // verify(discardGuardService.canDeactivate(anything())).once();
      expect(kantenSelektionService.isSelektiert(kante.id)).toBeFalse();
    }));

    it('should remove kante from selection if einseitig', fakeAsync(() => {
      kante.zweiseitig = false;

      kantenSelektionService.select(kante.id, true);
      tick();

      component.onDeselectLinearesSegment(0, kante.id);
      tick();

      // FIXME:
      // verify(discardGuardService.canDeactivate(anything())).once();
      expect(kantenSelektionService.isSelektiert(kante.id)).toBeFalse();
    }));

    it('should not remove kante from selection if other seite also selected', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true);
      tick();

      component.onDeselectLinearesSegment(0, kante.id, Seitenbezug.LINKS);
      tick();

      // FIXME:
      // verify(discardGuardService.canDeactivate(anything())).never();
      expect(kantenSelektionService.isSelektiert(kante.id)).toBeTrue();
    }));
  });

  describe('onReset', () => {
    it('should reset selected Segment', fakeAsync(() => {
      const kante = {
        ...defaultKante,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ] as FuehrungsformAttribute[],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);

      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS)).toEqual([0, 1]);

      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.76 },
        { von: 0.76, bis: 1 },
      ]);

      component.onReset();
      tick();

      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS)).toEqual([0, 1]);
      expect(component.lineareReferenzenLinksFormArray.value).toEqual([
        [
          { von: 0, bis: 0.5 },
          { von: 0.5, bis: 1 },
        ],
      ]);
    }));

    it('should not work on reference from selektionService', fakeAsync(() => {
      const kante1 = {
        ...defaultKante,
        id: 1,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
              belagArt: BelagArt.ASPHALT,
            },
          ],
          fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
        },
      };
      const kante2 = {
        ...defaultKante,
        id: 2,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
              belagArt: BelagArt.NATURSTEINPFLASTER,
            },
          ],
          fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
        },
      };

      setupKanten([kante1, kante2]);
      kantenSelektionService.select(kante1.id, true, Seitenbezug.LINKS, undefined);
      tick();
      kantenSelektionService.select(kante2.id, true, Seitenbezug.LINKS, undefined);
      tick();

      component.displayedAttributeformGroup.patchValue({
        belagArt: BelagArt.BETON,
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
      });

      component.onReset();
      tick();

      expect(component.displayedAttributeformGroup.value.belagArt).toBeInstanceOf(UndeterminedValue);
      expect(component.displayedAttributeformGroup.value.bordstein).toEqual(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER);
    }));

    it('it should disable controls if editing is not allowed', fakeAsync(() => {
      component.editingAllowed = false;
      setupKanten([defaultKante]);
      tick();

      component.onReset();
      tick();

      expect(component.displayedAttributeformGroup.disabled).toBeTrue();
      expect(component.lineareReferenzenFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenLinksFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenRechtsFormArray.disabled).toBeTrue();
    }));
  });

  describe('onInsertSegment', () => {
    let kante: Kante;
    beforeEach(() => {
      kante = {
        ...defaultKante,
        zweiseitig: true,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);
    });

    it('should insert and select new Segment with old values', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      component.onInsertAtIndex(0, 1, Seitenbezug.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 0.75 },
        { von: 0.75, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices().sort()).toEqual([0, 1, 2]);

      component.onSave();
      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: kante.id,
          gruppenID: kante.fuehrungsformAttributGruppe.id,
          gruppenVersion: kante.fuehrungsformAttributGruppe.id,
          fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.75, bis: 1 } },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));

    it('should change Selection to first element after reset', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      component.onInsertAtIndex(0, 1, Seitenbezug.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 0.75 },
        { von: 0.75, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices().sort()).toEqual([0, 1, 2]);

      component.onReset();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices().sort()).toEqual([0, 1]);
    }));

    it('should work einseitig', fakeAsync(() => {
      kante = {
        ...defaultKante,
        zweiseitig: false,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
          fuehrungsformAttributeRechts: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
        },
      };
      setupKanten([kante]);

      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      component.onInsertAtIndex(0, 1);
      component.lineareReferenzenFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 0.75 },
        { von: 0.75, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([
        0,
        1,
        2,
      ]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.RECHTS).sort()).toEqual([
        0,
        1,
        2,
      ]);

      component.onSave();
      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: kante.id,
          gruppenID: kante.fuehrungsformAttributGruppe.id,
          gruppenVersion: kante.fuehrungsformAttributGruppe.id,
          fuehrungsformAttributeRechts: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.75, bis: 1 } },
          ],
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.75, bis: 1 } },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));
  });

  describe('onDeleteSegment', () => {
    let kante: Kante;
    beforeEach(() => {
      kante = {
        ...defaultKante,
        zweiseitig: true,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KEINE_ABSENKUNG,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 },
            },
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KEINE_ABSENKUNG,
              linearReferenzierterAbschnitt: { von: 0.75, bis: 1 },
            },
          ],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);
    });

    it('should delete segment and update undetermined state', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, Seitenbezug.LINKS, undefined);
      tick();
      expect(component.displayedAttributeformGroup.value.bordstein).toBeInstanceOf(UndeterminedValue);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([
        0,
        1,
        2,
      ]);

      component.onDeleteAtIndex(0, 1, Seitenbezug.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([0, 1]);
      expect(component.displayedAttributeformGroup.value.bordstein).toEqual(Bordstein.KEINE_ABSENKUNG);

      component.onSave();
      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: kante.id,
          gruppenID: kante.fuehrungsformAttributGruppe.id,
          gruppenVersion: kante.fuehrungsformAttributGruppe.id,
          fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KEINE_ABSENKUNG,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KEINE_ABSENKUNG,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));

    it('should change selection to first element if the only selected element is deleted', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, Seitenbezug.LINKS, undefined);
      tick();
      kantenSelektionService.select(kante.id, false, Seitenbezug.LINKS, 1);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([1]);

      component.onDeleteAtIndex(0, 1, Seitenbezug.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();
      tick();

      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([0]);
    }));

    it('should change Selection to first element after reset', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, Seitenbezug.LINKS, undefined);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([
        0,
        1,
        2,
      ]);

      component.onDeleteAtIndex(0, 1, Seitenbezug.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();
      tick();

      component.onReset();
      tick();

      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([0, 1]);
    }));

    it('should work einseitig', fakeAsync(() => {
      kante = {
        ...defaultKante,
        zweiseitig: false,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
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
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
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
        },
      };
      setupKanten([kante]);

      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([
        0,
        1,
        2,
      ]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.RECHTS).sort()).toEqual([
        0,
        1,
        2,
      ]);

      component.onDeleteAtIndex(0, 1);
      component.lineareReferenzenFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.lineareReferenzenFormArray.markAsDirty();
      tick();

      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.LINKS).sort()).toEqual([0, 1]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(Seitenbezug.RECHTS).sort()).toEqual([0, 1]);

      component.onSave();
      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: kante.id,
          gruppenID: kante.fuehrungsformAttributGruppe.id,
          gruppenVersion: kante.fuehrungsformAttributGruppe.id,
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            },
          ],
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));
  });

  const setupKanten = (kanten: Kante[]): void => {
    kanten.forEach(kante => {
      when(netzService.getKanteForEdit(kante.id)).thenResolve(kante);
    });
  };
});
