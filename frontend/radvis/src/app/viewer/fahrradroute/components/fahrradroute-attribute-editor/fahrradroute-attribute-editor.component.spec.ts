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
import { DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { MatButtonModule } from '@angular/material/button';
import { MatOption } from '@angular/material/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Data } from '@angular/router';
import { MockBuilder } from 'ng-mocks';
import { Coordinate } from 'ol/coordinate';
import { BehaviorSubject, Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { ConfirmationDialogComponent } from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { FahrradrouteAttributeEditorComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-attribute-editor/fahrradroute-attribute-editor.component';
import { AbschnittsweiserKantenNetzbezug } from 'src/app/viewer/fahrradroute/models/abschnittsweiser-kanten-netzbezug';
import { ChangeFahrradrouteVeroeffentlichtCommand } from 'src/app/viewer/fahrradroute/models/change-fahrradroute-veroeffentlicht-command';
import { FahrradrouteDetailView } from 'src/app/viewer/fahrradroute/models/fahrradroute-detail-view';
import { defaultFahrradroute } from 'src/app/viewer/fahrradroute/models/fahrradroute-detail-view-test-data-provider.spec';
import { defaultFahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute-netzbezug-test-data-provider.spec';
import { FahrradrouteVariante } from 'src/app/viewer/fahrradroute/models/fahrradroute-variante';
import { FahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute.netzbezug';
import { SaveFahrradrouteCommand } from 'src/app/viewer/fahrradroute/models/save-fahrradroute-command';
import { SaveFahrradrouteVarianteCommand } from 'src/app/viewer/fahrradroute/models/save-fahrradroute-variante-command';
import { VarianteKategorie } from 'src/app/viewer/fahrradroute/models/variante-kategorie';
import { FahrradrouteFilterService } from 'src/app/viewer/fahrradroute/services/fahrradroute-filter.service';
import { FahrradrouteProfilService } from 'src/app/viewer/fahrradroute/services/fahrradroute-profil.service';
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { FahrradrouteTyp } from 'src/app/viewer/viewer-shared/models/fahrradroute-typ';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, deepEqual, instance, mock, verify, when } from 'ts-mockito';

describe(FahrradrouteAttributeEditorComponent.name, () => {
  let fixture: ComponentFixture<FahrradrouteAttributeEditorComponent>;
  let component: FahrradrouteAttributeEditorComponent;

  let dataSubject: Subject<Data>;
  let fahrradroute: FahrradrouteDetailView;

  let organisationenService: OrganisationenService;
  let notifyUserService: NotifyUserService;
  let activatedRoute: ActivatedRoute;
  let fahrradrouteService: FahrradrouteService;
  let fahrradrouteFilterService: FahrradrouteFilterService;
  let dialog: MatDialog;
  let fahrradrouteProfilService: FahrradrouteProfilService;

  beforeEach(async () => {
    dataSubject = new Subject();

    fahrradroute = defaultFahrradroute;

    organisationenService = mock(OrganisationenService);
    notifyUserService = mock(NotifyUserService);
    activatedRoute = mock(ActivatedRoute);
    fahrradrouteService = mock(FahrradrouteService);
    fahrradrouteFilterService = mock(FahrradrouteFilterService);
    dialog = mock(MatDialog);
    fahrradrouteProfilService = mock(FahrradrouteProfilService);

    when(activatedRoute.data).thenReturn(dataSubject.asObservable());

    return MockBuilder(FahrradrouteAttributeEditorComponent, ViewerModule)
      .provide({
        provide: OrganisationenService,
        useValue: instance(organisationenService),
      })
      .provide({
        provide: ActivatedRoute,
        useValue: instance(activatedRoute),
      })
      .provide({
        provide: NotifyUserService,
        useValue: instance(notifyUserService),
      })
      .provide({
        provide: FahrradrouteService,
        useValue: instance(fahrradrouteService),
      })
      .provide({
        provide: FahrradrouteFilterService,
        useValue: instance(fahrradrouteFilterService),
      })
      .provide({
        provide: OlMapService,
        useValue: instance(mock(OlMapComponent)),
      })
      .provide({
        provide: MatDialog,
        useValue: instance(dialog),
      })
      .provide({
        provide: FahrradrouteProfilService,
        useValue: instance(fahrradrouteProfilService),
      })
      .keep(MatButtonModule)
      .keep(BreakpointObserver);
  });

  it('should show "keine Route berechenbar" message', waitForAsync(() => {
    const expectedFahrradroute: FahrradrouteDetailView = {
      ...fahrradroute,
      geometrie: undefined,
      stuetzpunkte: undefined,
      varianten: [],
    };
    const dataBehaviorSubject = new BehaviorSubject<Data>({
      fahrradrouteDetailView: expectedFahrradroute,
    });
    when(activatedRoute.data).thenReturn(dataBehaviorSubject.asObservable());

    // Konstruktor der Komponente ausfuehren
    fixture = TestBed.createComponent(FahrradrouteAttributeEditorComponent);

    verify(notifyUserService.warn(anything())).once();
    expect().nothing();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FahrradrouteAttributeEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('mit fahrradroute inkl 2 varianten', () => {
    let variante1: FahrradrouteVariante;
    let variante2: FahrradrouteVariante;
    let expectedFahrradroute: FahrradrouteDetailView;
    beforeEach(() => {
      variante1 = {
        id: 1,
        kantenIDs: [0, 1],
        kategorie: VarianteKategorie.ALTERNATIVSTRECKE,
        stuetzpunkte: {
          coordinates: [
            [0, 0],
            [0, 10],
          ],
          type: 'LineString',
        } as LineStringGeojson,
        geometrie: {
          coordinates: [
            [0, 0],
            [0, 10],
          ],
          type: 'LineString',
        } as LineStringGeojson,
        profilEigenschaften: [],
        kantenBezug: [],
        customProfileId: 11,
      };
      variante2 = {
        id: 2,
        kantenIDs: [2, 3],
        kategorie: VarianteKategorie.ALTERNATIVSTRECKE,
        stuetzpunkte: {
          coordinates: [
            [100, 0],
            [100, 10],
          ],
          type: 'LineString',
        } as LineStringGeojson,
        geometrie: {
          coordinates: [
            [100, 0],
            [90, 3],
            [100, 10],
          ],
          type: 'LineString',
        } as LineStringGeojson,
        profilEigenschaften: [],
        kantenBezug: [],
        customProfileId: 22,
      };
      expectedFahrradroute = {
        ...fahrradroute,
        geometrie: {
          coordinates: [
            [0, 0],
            [23754, 2345],
          ],
          type: 'LineString',
        },
        stuetzpunkte: {
          coordinates: [
            [10, 10],
            [23754, 2345],
          ],
          type: 'LineString',
        },
        varianten: [variante1, variante2],
      };
    });

    it('should fill form correctly', () => {
      // act
      dataSubject.next({
        fahrradrouteDetailView: expectedFahrradroute,
      });

      // assert
      expect(component.currentFahrradroute).toEqual(expectedFahrradroute);
      expect(component.formGroup.getRawValue()).toEqual({
        name: fahrradroute.name,
        kurzbeschreibung: fahrradroute.kurzbeschreibung,
        beschreibung: fahrradroute.beschreibung,
        fahrradrouteKategorie: fahrradroute.fahrradrouteKategorie,
        tourenkategorie: fahrradroute.tourenkategorie,
        laengeHauptstrecke: fahrradroute.laengeHauptstrecke / 1000,
        offizielleLaenge: fahrradroute.offizielleLaenge / 1000,
        verantwortlich: fahrradroute.verantwortlich,
        homepage: fahrradroute.homepage,
        emailAnsprechpartner: fahrradroute.emailAnsprechpartner,
        lizenz: fahrradroute.lizenz,
        lizenzNamensnennung: fahrradroute.lizenzNamensnennung,
        varianten: [
          {
            id: variante1.id,
            kategorie: variante1.kategorie,
            netzbezug: {
              stuetzpunkte: variante1.stuetzpunkte!.coordinates,
              kantenIDs: variante1.kantenIDs,
              geometrie: variante1.geometrie!,
              profilEigenschaften: [],
              customProfileId: variante1.customProfileId,
            },
            kantenBezug: [],
          },
          {
            id: variante2.id,
            kategorie: variante2.kategorie,
            netzbezug: {
              stuetzpunkte: variante2.stuetzpunkte!.coordinates,
              kantenIDs: variante2.kantenIDs,
              geometrie: variante2.geometrie!,
              profilEigenschaften: [],
              customProfileId: variante2.customProfileId,
            },
            kantenBezug: [],
          },
        ],
        anstieg: '2,34',
        abstieg: '12,00',
        info: fahrradroute.info,
        toubizId: fahrradroute.toubizId,
        zuletztBearbeitet: '10.01.22 15:28',
        netzbezug: {
          geometrie: expectedFahrradroute.geometrie,
          stuetzpunkte: expectedFahrradroute.stuetzpunkte?.coordinates,
          kantenIDs: expectedFahrradroute.kantenBezug.map(k => k.kanteId),
          profilEigenschaften: [],
          customProfileId: 123,
        } as FahrradrouteNetzbezug,
      });
      expect(component.formGroup.enabled).toBeTrue();
    });

    it('selectedVariante Change - Netzbezug+Kantenbezug aktualisiert sich', fakeAsync(() => {
      const variante1KantenBezug: AbschnittsweiserKantenNetzbezug[] = [
        {
          kanteId: 1,
          geometrie: {
            coordinates: [
              [0, 0],
              [0, 10],
            ],
            type: 'LineString',
          } as LineStringGeojson,
          linearReferenzierterAbschnitt: { von: 0, bis: 1 } as LinearReferenzierterAbschnitt,
        },
      ];

      const variante2KantenBezug: AbschnittsweiserKantenNetzbezug[] = [
        {
          kanteId: 2,
          geometrie: {
            coordinates: [
              [100, 0],
              [100, 10],
            ],
            type: 'LineString',
          } as LineStringGeojson,
          linearReferenzierterAbschnitt: { von: 0, bis: 1 } as LinearReferenzierterAbschnitt,
        },
      ];

      expectedFahrradroute.varianten[0].kantenBezug = variante1KantenBezug;
      expectedFahrradroute.varianten[1].kantenBezug = variante2KantenBezug;

      dataSubject.next({
        fahrradrouteDetailView: expectedFahrradroute,
      });
      tick();

      // act variante 1 Auswaehlen
      component.selectedVarianteControl.setValue(0);
      tick();

      // assert
      expect(component.selectedVarianteNetzbezug).toEqual({
        stuetzpunkte: variante1.stuetzpunkte?.coordinates,
        kantenIDs: variante1.kantenIDs,
        geometrie: variante1.geometrie,
        profilEigenschaften: [],
        customProfileId: variante1.customProfileId,
      } as FahrradrouteNetzbezug);
      expect(component.selectedVarianteKantenBezug).toEqual(variante1KantenBezug);

      // act variante 2 Auswaehlen
      component.selectedVarianteControl.setValue(1);
      tick();

      // assert
      expect(component.selectedVarianteNetzbezug).toEqual({
        stuetzpunkte: variante2.stuetzpunkte?.coordinates,
        kantenIDs: variante2.kantenIDs,
        geometrie: variante2.geometrie,
        profilEigenschaften: [],
        customProfileId: variante2.customProfileId,
      } as FahrradrouteNetzbezug);
      expect(component.selectedVarianteKantenBezug).toEqual(variante2KantenBezug);

      // act hauptroute Auswaehlen
      component.selectedVarianteControl.setValue('Hauptstrecke');
      tick();

      // assert
      expect(component.selectedVarianteNetzbezug).toEqual(null);
      expect(component.selectedVarianteKantenBezug).toEqual(null);
    }));
  });

  it('should disable form when editing attributes not allowed', () => {
    dataSubject.next({ fahrradrouteDetailView: { ...fahrradroute, canEditAttribute: false } });

    expect(component.formGroup.enabled).toBeFalse();
  });

  it('should always disable certain fields', () => {
    dataSubject.next({ fahrradrouteDetailView: { ...fahrradroute, canEditAttribute: true } });

    expect(component.formGroup.get('laengeHauptstrecke')?.disabled).toBeTrue();
    expect(component.formGroup.get('toubizId')?.disabled).toBeTrue();
    expect(component.formGroup.get('anstieg')?.disabled).toBeTrue();
    expect(component.formGroup.get('abstieg')?.disabled).toBeTrue();
    expect(component.formGroup.get('info')?.disabled).toBeTrue();
    expect(component.formGroup.get('zuletztBearbeitet')?.disabled).toBeTrue();
  });

  it('should enable toubizId for LRFW', () => {
    const expectedFahrradroute: FahrradrouteDetailView = {
      ...fahrradroute,
      geometrie: {
        coordinates: [
          [0, 0],
          [23754, 2345],
        ],
        type: 'LineString',
      },
      stuetzpunkte: {
        coordinates: [
          [10, 10],
          [23754, 2345],
        ],
        type: 'LineString',
      },
      fahrradrouteTyp: FahrradrouteTyp.RADVIS_ROUTE,
      fahrradrouteKategorie: FahrradrouteKategorie.LANDESRADFERNWEG,
      canEditAttribute: true,
    };
    dataSubject.next({
      fahrradrouteDetailView: expectedFahrradroute,
    });
    expect(component.formGroup.value).toEqual({
      name: expectedFahrradroute.name,
      kurzbeschreibung: expectedFahrradroute.kurzbeschreibung,
      beschreibung: expectedFahrradroute.beschreibung,
      fahrradrouteKategorie: expectedFahrradroute.fahrradrouteKategorie,
      tourenkategorie: expectedFahrradroute.tourenkategorie,
      offizielleLaenge: expectedFahrradroute.offizielleLaenge / 1000,
      verantwortlich: expectedFahrradroute.verantwortlich,
      homepage: expectedFahrradroute.homepage,
      emailAnsprechpartner: expectedFahrradroute.emailAnsprechpartner,
      lizenz: expectedFahrradroute.lizenz,
      lizenzNamensnennung: expectedFahrradroute.lizenzNamensnennung,
      varianten: expectedFahrradroute.varianten,
      toubizId: expectedFahrradroute.toubizId,
      netzbezug: {
        geometrie: expectedFahrradroute.geometrie,
        stuetzpunkte: expectedFahrradroute.stuetzpunkte?.coordinates,
        kantenIDs: expectedFahrradroute.kantenBezug.map(k => k.kanteId),
        profilEigenschaften: [],
        customProfileId: 123,
      } as FahrradrouteNetzbezug,
    });
  });

  describe(FahrradrouteAttributeEditorComponent.prototype.onSelectedStreckeLoeschen.name, () => {
    it('should delete correct Variante', () => {
      const variante1 = {
        id: 1,
        kantenIDs: [0, 1],
        kategorie: VarianteKategorie.ALTERNATIVSTRECKE,
        stuetzpunkte: {
          coordinates: [
            [0, 0],
            [0, 10],
          ],
          type: 'LineString',
        } as LineStringGeojson,
      };
      const variante2 = {
        id: 2,
        kantenIDs: [2, 3],
        kategorie: VarianteKategorie.GEGENRICHTUNG,
        stuetzpunkte: {
          coordinates: [
            [100, 0],
            [100, 10],
          ],
          type: 'LineString',
        } as LineStringGeojson,
      };
      const expectedFahrradroute = {
        ...fahrradroute,
        varianten: [variante1, variante2],
      };

      const closeSubject = new Subject<boolean>();

      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => closeSubject.asObservable(),
      } as MatDialogRef<ConfirmationDialogComponent>);

      dataSubject.next({
        fahrradrouteDetailView: expectedFahrradroute,
      });

      component['changeDetector'].detectChanges();
      expect(component.formGroup.controls.varianten).toHaveSize(2);
      component.selectedVarianteControl.patchValue(0);
      component['changeDetector'].detectChanges();
      expect(component.deleteSelectedStreckeForbidden).toBeFalse();
      expect(isLoeschenButtonDisabled(fixture.debugElement)).toBeFalse();
      component.onSelectedStreckeLoeschen();
      closeSubject.next(true);
      expect(component.formGroup.controls.varianten).toHaveSize(1);
      expect(component.formGroup.controls.varianten.at(0).get('kategorie')?.value).toEqual(variante2.kategorie);
      expect(component.deleteSelectedStreckeForbidden).toBeFalse();
      verify(fahrradrouteService.deleteFahrradroute(anything())).never();
    });

    it('should not disable Löschen-Button when not-LRFW Haupstrecke selected', () => {
      const variante1 = {
        id: 1,
        kantenIDs: [0, 1],
        kategorie: VarianteKategorie.ALTERNATIVSTRECKE,
        stuetzpunkte: {
          coordinates: [
            [0, 0],
            [0, 10],
          ],
          type: 'LineString',
        } as LineStringGeojson,
      };
      const variante2 = {
        id: 2,
        kantenIDs: [2, 3],
        kategorie: VarianteKategorie.ALTERNATIVSTRECKE,
        stuetzpunkte: {
          coordinates: [
            [100, 0],
            [100, 10],
          ],
          type: 'LineString',
        } as LineStringGeojson,
      };
      const expectedFahrradroute = {
        ...fahrradroute,
        varianten: [variante1, variante2],
      };
      dataSubject.next({
        fahrradrouteDetailView: expectedFahrradroute,
      });
      component['changeDetector'].detectChanges();
      expect(component.selectedVarianteControl.value).toEqual(component.HAUPTSTRECKE);
      expect(component.deleteSelectedStreckeForbidden).toBeFalse();
      expect(isLoeschenButtonDisabled(fixture.debugElement)).toBeFalse();
    });

    it('should disable Löschen-Button when LRFW Haupstrecke selected', () => {
      const expectedFahrradroute: FahrradrouteDetailView = {
        ...fahrradroute,
        fahrradrouteKategorie: FahrradrouteKategorie.LANDESRADFERNWEG,
      };
      dataSubject.next({
        fahrradrouteDetailView: expectedFahrradroute,
      });
      component['changeDetector'].detectChanges();
      expect(component.selectedVarianteControl.value).toEqual(component.HAUPTSTRECKE);
      expect(component.deleteSelectedStreckeForbidden).toBeTrue();
      expect(isLoeschenButtonDisabled(fixture.debugElement)).toBeTruthy();
    });

    it('should disable Löschen-Button when D-Route Haupstrecke selected', () => {
      const expectedFahrradroute: FahrradrouteDetailView = {
        ...fahrradroute,
        fahrradrouteKategorie: FahrradrouteKategorie.D_ROUTE,
      };
      dataSubject.next({
        fahrradrouteDetailView: expectedFahrradroute,
      });
      component['changeDetector'].detectChanges();
      expect(component.selectedVarianteControl.value).toEqual(component.HAUPTSTRECKE);
      expect(component.deleteSelectedStreckeForbidden).toBeTrue();
      expect(isLoeschenButtonDisabled(fixture.debugElement)).toBeTruthy();
    });

    it('should delete not-LRFW Hauptroute correctly', () => {
      const expectedFahrradroute = {
        ...fahrradroute,
      };

      const closeSubject = new Subject<boolean>();

      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => closeSubject.asObservable(),
      } as MatDialogRef<ConfirmationDialogComponent>);
      when(fahrradrouteService.deleteFahrradroute(anything())).thenReturn(Promise.resolve());

      dataSubject.next({
        fahrradrouteDetailView: expectedFahrradroute,
      });

      component['changeDetector'].detectChanges();
      expect(component.deleteSelectedStreckeForbidden).toBeFalse();
      expect(isLoeschenButtonDisabled(fixture.debugElement)).toBeFalse();
      component.onSelectedStreckeLoeschen();
      closeSubject.next(true);
      verify(fahrradrouteService.deleteFahrradroute(anything())).once();
    });
  });

  describe('onSave', () => {
    beforeEach(() => {
      dataSubject.next({ fahrradrouteDetailView: fahrradroute });
      when(fahrradrouteService.saveFahrradroute(anything())).thenReturn(Promise.resolve(fahrradroute));
    });

    it('should create command correctly', () => {
      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);
      component.selectedNetzbezugControl?.setValue({
        kantenIDs: [0, 1],
        stuetzpunkte: [
          [0, 0],
          [10, 10],
        ],
        geometrie: {
          coordinates: [
            [0, 0],
            [5, 5],
            [10, 10],
          ],
          type: 'LineString',
        },
        profilEigenschaften: [],
        customProfileId: 123,
      } as FahrradrouteNetzbezug);
      component.formGroup.patchValue({
        name: 'MyNewName',
      });

      const fahrradrouteNetzbezug: FahrradrouteNetzbezug = {
        geometrie: {
          coordinates: [
            [0, 0],
            [34, 253],
          ],
          type: 'LineString',
        },
        kantenIDs: [3674, 235],
        stuetzpunkte: [
          [0, 0],
          [100, 100],
        ],
        profilEigenschaften: [],
        customProfileId: 234,
      };
      component.formGroup.patchValue({ netzbezug: fahrradrouteNetzbezug });

      component.onSave();

      verify(fahrradrouteService.saveFahrradroute(anything())).once();
      const command = capture(fahrradrouteService.saveFahrradroute).last()[0];

      expect(command).toEqual({
        id: fahrradroute.id,
        version: fahrradroute.version,
        name: 'MyNewName',
        kurzbeschreibung: fahrradroute.kurzbeschreibung,
        beschreibung: fahrradroute.beschreibung,
        kategorie: fahrradroute.fahrradrouteKategorie,
        tourenkategorie: fahrradroute.tourenkategorie,
        offizielleLaenge: fahrradroute.offizielleLaenge,
        homepage: fahrradroute.homepage,
        verantwortlichId: fahrradroute.verantwortlich?.id,
        emailAnsprechpartner: fahrradroute.emailAnsprechpartner,
        lizenz: fahrradroute.lizenz,
        lizenzNamensnennung: fahrradroute.lizenzNamensnennung,
        toubizId: undefined,
        varianten: [
          {
            id: null,
            kantenIDs: [0, 1],
            kategorie: VarianteKategorie.ALTERNATIVSTRECKE,
            stuetzpunkte: {
              coordinates: [
                [0, 0],
                [10, 10],
              ],
              type: 'LineString',
            },
            geometrie: {
              coordinates: [
                [0, 0],
                [5, 5],
                [10, 10],
              ],
              type: 'LineString',
            },
            profilEigenschaften: [],
            customProfileId: 123,
          } as SaveFahrradrouteVarianteCommand,
        ],
        kantenIDs: fahrradrouteNetzbezug.kantenIDs,
        routenVerlauf: fahrradrouteNetzbezug.geometrie,
        stuetzpunkte: { coordinates: fahrradrouteNetzbezug.stuetzpunkte, type: 'LineString' },
        profilEigenschaften: [],
        customProfileId: 234,
      } as SaveFahrradrouteCommand);
    });

    it('should notify user after save', fakeAsync(() => {
      component.formGroup.markAsDirty();
      component.onSave();

      tick();

      verify(notifyUserService.inform(anything())).once();
      expect().nothing();
    }));

    it('should refetch data after save', fakeAsync(() => {
      component.formGroup.markAsDirty();
      component.onSave();

      tick();

      verify(fahrradrouteFilterService.refetchData()).once();
      expect().nothing();
    }));
  });

  describe('onVeroeffentlichtChanged', () => {
    beforeEach(() => {
      dataSubject.next({ fahrradrouteDetailView: fahrradroute });
      when(fahrradrouteService.updateVeroeffentlicht(anything())).thenReturn(Promise.resolve(fahrradroute));
    });

    it('should create command correctly', () => {
      component.veroeffentlicht = true;

      component.onVeroeffentlichtChanged(true);

      verify(fahrradrouteService.updateVeroeffentlicht(anything())).once();
      const command = capture(fahrradrouteService.updateVeroeffentlicht).last()[0];

      expect(command).toEqual({
        id: fahrradroute.id,
        version: fahrradroute.version,
        veroeffentlicht: true,
      } as ChangeFahrradrouteVeroeffentlichtCommand);
    });

    it('should notify user after veroeffentlicht changed', fakeAsync(() => {
      component.onVeroeffentlichtChanged(false);

      tick();

      verify(notifyUserService.inform(anything())).once();
      expect().nothing();
    }));
  });

  describe('update fahrradrouteprofil', () => {
    it('should update Profil and trigger showProfile if editStrecke enabled and geometry changes', fakeAsync(() => {
      dataSubject.next({
        fahrradrouteDetailView: fahrradroute,
      });

      tick();

      const values = {
        netzbezug: defaultFahrradrouteNetzbezug,
      };

      component.onEditVerlauf();

      component.formGroup.patchValue(values);
      component.formGroup.markAsDirty();

      verify(
        fahrradrouteProfilService.updateCurrentRouteProfil(
          deepEqual({
            geometrie: defaultFahrradrouteNetzbezug.geometrie,
            name: fahrradroute.name,
            profilEigenschaften: defaultFahrradrouteNetzbezug.profilEigenschaften,
          })
        )
      ).once();

      expect().nothing();
    }));

    it('should update Profil and trigger showProfile if variante is added', fakeAsync(() => {
      dataSubject.next({
        fahrradrouteDetailView: {
          ...fahrradroute,
          geometrie: {
            coordinates: [
              [42, 42, 42],
              [314, 314, 314],
            ],
            type: 'LineString',
          } as LineStringGeojson,
          stuetzpunkte: {
            coordinates: [
              [42, 42],
              [314, 314],
            ],
            type: 'LineString',
          },
        } as FahrradrouteDetailView,
      });

      tick();

      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);

      verify(
        fahrradrouteProfilService.updateCurrentRouteProfil(
          deepEqual({
            geometrie: undefined,
            name: fahrradroute.name,
            profilEigenschaften: undefined,
          })
        )
      ).once();

      expect().nothing();
    }));

    describe(FahrradrouteAttributeEditorComponent.prototype.ngOnDestroy.name, () => {
      it('should trigger closing of profile', fakeAsync(() => {
        component.ngOnDestroy();

        verify(fahrradrouteProfilService.hideCurrentRouteProfile()).once();
        expect().nothing();
      }));
    });
  });

  it('onEditVerlauf should empty netzbezug and kantenbezug', fakeAsync(() => {
    component.onEditVerlauf();

    expect(component.selectedVarianteNetzbezug).toEqual(null);
    expect(component.selectedVarianteKantenBezug).toEqual(null);
  }));

  describe('selectedNetzbezugControl', () => {
    it('should return correct variante netzbezug control', () => {
      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);
      expect(component.selectedNetzbezugControl).toBe(component.formGroup.controls.varianten.at(0).controls.netzbezug);
    });

    it('should return Hauptstrecke netzbezug control', () => {
      component.selectedVarianteControl.setValue(component.HAUPTSTRECKE);
      expect(component.selectedNetzbezugControl).toBe(component.formGroup.controls.netzbezug);
    });
  });

  describe('selectedVarianteControl', () => {
    it('should be HAUPTSTRECKE after reset', () => {
      component.currentFahrradroute = fahrradroute;
      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);
      component.onReset();
      expect(component.selectedVarianteControl.value).toEqual(component.HAUPTSTRECKE);
    });
  });

  describe('onVarianteAdd', () => {
    it('should increase number on same kategorie', () => {
      dataSubject.next({
        fahrradrouteDetailView: fahrradroute,
      });
      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);
      expect(component.formGroup.controls.varianten.length).toBe(1);

      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);
      expect(component.formGroup.controls.varianten.length).toBe(2);

      component['changeDetector'].detectChanges();

      expect(fixture.debugElement.queryAll(By.directive(MatOption)).map(opt => opt.nativeElement.innerText)).toEqual([
        'Hauptstrecke',
        'Alternativstrecke',
        'Alternativstrecke (1)',
      ]);
    });

    it('should make form dirty', () => {
      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);
      expect(component.formGroup.dirty).toBeTrue();
    });

    it('should select newly added variante automatically and select edit mode', () => {
      component.editStreckeEnabled = false;

      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);
      expect(component.selectedVarianteControl.value).toEqual(0);
      expect(component.editStreckeEnabled).toBeTrue();

      component.onVarianteAdded(VarianteKategorie.ALTERNATIVSTRECKE);
      expect(component.selectedVarianteControl.value).toEqual(1);
    });

    it('should prefill when gegenroute', async () => {
      const routingResult = {
        kantenIDs: [234, 2],
        routenGeometrie: {
          coordinates: [
            [0, 0],
            [0, 100],
            [0, 200],
          ],
          type: 'LineString',
        } as LineStringGeojson,
        profilEigenschaften: [],
        customProfileId: 123,
      };
      when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything())).thenResolve(routingResult);
      const fahrradrouteNetzbezug: FahrradrouteNetzbezug = {
        geometrie: {
          coordinates: [
            [0, 0],
            [34, 253],
          ],
          type: 'LineString',
        },
        kantenIDs: [3674, 235],
        stuetzpunkte: [
          [0, 0],
          [357546, 345],
        ],
        profilEigenschaften: [],
        customProfileId: 123,
      };
      component.formGroup.patchValue({ netzbezug: fahrradrouteNetzbezug });
      const reversedStuetzpunkte: Coordinate[] = fahrradrouteNetzbezug.stuetzpunkte.slice() || [];
      reversedStuetzpunkte?.reverse();

      await component.onVarianteAdded(VarianteKategorie.GEGENRICHTUNG);

      expect(component.selectedNetzbezugControl?.value).toEqual({
        geometrie: routingResult.routenGeometrie,
        kantenIDs: routingResult.kantenIDs,
        stuetzpunkte: reversedStuetzpunkte,
        profilEigenschaften: [],
        customProfileId: 123,
      } as FahrradrouteNetzbezug);
      verify(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything())).once();
      expect(capture(fahrradrouteService.routeFahrradroutenVerlauf).last()[0]).toEqual(reversedStuetzpunkte);
    });
  });

  describe('should create display text from control', () => {
    it('should show correct texts for variants', () => {
      const expectedFahrradroute = {
        ...fahrradroute,
        varianten: [
          {
            id: 1,
            kantenIDs: [0, 1],
            kategorie: VarianteKategorie.ALTERNATIVSTRECKE,
            stuetzpunkte: {
              coordinates: [
                [0, 0],
                [0, 10],
              ],
              type: 'LineString',
            } as LineStringGeojson,
            profilEigenschaften: [],
            kantenBezug: [],
            customProfileId: 11,
          } as FahrradrouteVariante,
          {
            id: 2,
            kantenIDs: [2, 3],
            kategorie: VarianteKategorie.GEGENRICHTUNG,
            stuetzpunkte: {
              coordinates: [
                [100, 0],
                [100, 10],
              ],
              type: 'LineString',
            } as LineStringGeojson,
            profilEigenschaften: [],
            kantenBezug: [],
            customProfileId: 22,
          } as FahrradrouteVariante,
          {
            id: 3,
            kantenIDs: [2, 3],
            kategorie: VarianteKategorie.ZUBRINGERSTRECKE,
            stuetzpunkte: {
              coordinates: [
                [100, 0],
                [100, 10],
              ],
              type: 'LineString',
            } as LineStringGeojson,
            profilEigenschaften: [],
            kantenBezug: [],
            customProfileId: 33,
          } as FahrradrouteVariante,
        ],
      };
      dataSubject.next({
        fahrradrouteDetailView: expectedFahrradroute,
      });

      expect(component.currentFahrradroute).toEqual(expectedFahrradroute);
      expect(component.getDisplayText(component.formGroup.controls.varianten.controls[0])).toEqual('Alternativstrecke');
      expect(component.getDisplayText(component.formGroup.controls.varianten.controls[1])).toEqual('Gegenrichtung');
      expect(component.getDisplayText(component.formGroup.controls.varianten.controls[2])).toEqual('Zubringerstrecke');
    });
  });

  describe('getProfilAuswertung', () => {
    it('should get correct values', () => {
      const expectedProzentProfil1 = 52.0;
      const expectedLaengeProfil1 = 2.196;
      const expectedProzentProfil2 = 48.0;
      const expectedLaengeProfil2 = 2.028;
      const neueProfileigenschaften = [
        {
          vonLR: 0,
          bisLR: 0.52,
          belagArt: BelagArt.BETON,
          radverkehrsfuehrung: 'Unbekannt',
        },
        {
          vonLR: 0.52,
          bisLR: 1,
          belagArt: BelagArt.ASPHALT,
          radverkehrsfuehrung: 'Sonstiger Betriebsweg',
        },
      ];

      component.currentFahrradroute = {
        ...fahrradroute,
        laengeHauptstrecke: 4224,
        profilEigenschaften: neueProfileigenschaften,
      };

      expect(component.getProfilAuswertungFuerRadverkehrsfuehrung('Unbekannt')).toEqual({
        prozent: expectedProzentProfil1,
        kilometer: expectedLaengeProfil1,
      });
      expect(component.getProfilAuswertungFuerBelagArt(BelagArt.ASPHALT)).toEqual({
        prozent: expectedProzentProfil2,
        kilometer: expectedLaengeProfil2,
      });
    });
  });

  const isLoeschenButtonDisabled = (debugElement: DebugElement): boolean =>
    debugElement.query(By.css('.variante-loeschen-button')).nativeElement.disabled;
});
