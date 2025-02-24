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

import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, convertToParamMap, ParamMap, Router } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { Subject } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KnotenAttributeEditorComponent } from 'src/app/editor/knoten/components/knoten-attribute-editor/knoten-attribute-editor.component';
import { Knoten } from 'src/app/editor/knoten/models/knoten';
import { defaultKnoten } from 'src/app/editor/knoten/models/knoten-test-data-provider.spec';
import { SharedKnotenFormGroupComponent } from 'src/app/shared/components/shared-knoten-form-group/shared-knoten-form-group.component';
import { Bauwerksmangel } from 'src/app/shared/models/bauwerksmangel';
import { BauwerksmangelArt } from 'src/app/shared/models/bauwerksmangel-art';
import {
  defaultGemeinden,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { QuerungshilfeDetails } from 'src/app/shared/models/querungshilfe-details';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe('KnotenAttributeEditorComponent', () => {
  let component: KnotenAttributeEditorComponent;
  let fixture: MockedComponentFixture<KnotenAttributeEditorComponent>;
  let activatedRoute: ActivatedRoute;
  let paramsSubject: Subject<ParamMap>;
  let netzService: NetzService;
  let organisationenService: OrganisationenService;

  beforeEach(() => {
    activatedRoute = mock(ActivatedRoute);
    paramsSubject = new Subject();
    when(activatedRoute.paramMap).thenReturn(paramsSubject.asObservable());
    when(activatedRoute.snapshot).thenReturn({
      data: {
        knoten: defaultKnoten,
      },
    } as unknown as ActivatedRouteSnapshot);

    netzService = mock(NetzService);
    organisationenService = mock(OrganisationenService);
    when(organisationenService.getGemeinden()).thenReturn(Promise.resolve(defaultGemeinden));
    when(organisationenService.getOrganisation(2)).thenReturn(Promise.resolve(defaultUebergeordneteOrganisation));
    return MockBuilder([KnotenAttributeEditorComponent, SharedKnotenFormGroupComponent], EditorModule)
      .provide({
        provide: Router,
        useValue: instance(mock(Router)),
      })
      .provide({
        provide: ActivatedRoute,
        useValue: instance(activatedRoute),
      })
      .provide({
        provide: NetzService,
        useValue: instance(netzService),
      })
      .provide({
        provide: OrganisationenService,
        useValue: instance(organisationenService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(KnotenAttributeEditorComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  const updateKnotenInRoute = (knoten: Knoten): void => {
    when(activatedRoute.snapshot).thenReturn({
      data: {
        knoten,
      },
    } as unknown as ActivatedRouteSnapshot);
    paramsSubject.next(convertToParamMap({ id: 1 }));
  };

  describe('form', () => {
    it('should fill form with knoten correct', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, id: 2 };
      updateKnotenInRoute(currentKnoten);

      tick();
      fixture.detectChanges();

      expect(component['currentKnoten']).toEqual(currentKnoten);
      expect(component.knotenFormGroup.getRawValue()).toEqual({
        ortslage: currentKnoten.ortslage,
        gemeinde: currentKnoten.gemeinde,
        landkreis: currentKnoten.landkreis?.name ?? null,
        kommentar: currentKnoten.kommentar,
        zustandsbeschreibung: currentKnoten.zustandsbeschreibung,
        shared: {
          knotenForm: currentKnoten.knotenForm,
          querungshilfeDetails: null,
          bauwerksmangel: { vorhanden: null, bauwerksmangelArt: null },
        },
      });
    }));

    it('should set Landkreis when Gemeinde was set', fakeAsync(() => {
      const currentKnoten: Knoten = {
        ...defaultKnoten,
        id: 2,
        gemeinde: {
          id: 22,
          name: 'Lippstadt',
          organisationsArt: OrganisationsArt.GEMEINDE,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      };
      updateKnotenInRoute(currentKnoten);

      tick();
      fixture.detectChanges();

      const landkreis: Verwaltungseinheit = {
        id: 23,
        name: 'Landkreis Holz',
        organisationsArt: OrganisationsArt.KREIS,
        idUebergeordneteOrganisation: 42,
        aktiv: true,
      };
      when(organisationenService.getOrganisation(23)).thenReturn(Promise.resolve(landkreis));

      component.knotenFormGroup.controls.gemeinde.setValue({
        id: 36,
        name: 'Holzhausen',
        organisationsArt: OrganisationsArt.GEMEINDE,
        idUebergeordneteOrganisation: 23,
        aktiv: true,
      });

      tick();
      fixture.detectChanges();

      expect(component.knotenFormGroup.controls.landkreis.value).toEqual(landkreis.name);
    }));

    it('should read command from form correct (inkl querungsdetails)', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, id: 2, knotenVersion: 42 };
      const nextKnoten: Knoten = { ...defaultKnoten, id: 2, knotenVersion: 43 };
      when(netzService.saveKnoten(anything())).thenReturn(Promise.resolve(nextKnoten));

      updateKnotenInRoute(currentKnoten);
      tick();
      fixture.detectChanges();
      component.knotenFormGroup.controls.shared.patchValue({
        knotenForm: 'MITTELINSEL_EINFACH',
        querungshilfeDetails: QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL,
      });
      component.knotenFormGroup.markAsDirty();
      component.onSave();

      verify(netzService.saveKnoten(anything())).once();
      const command = capture(netzService.saveKnoten).last()[0];
      expect(command).toEqual({
        id: 2,
        gemeinde: 1,
        kommentar: 'kommentar',
        zustandsbeschreibung: 'default Zustandsbeschreibung',
        knotenForm: 'MITTELINSEL_EINFACH',
        knotenVersion: 42,
        querungshilfeDetails: QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL,
        bauwerksmangel: null,
        bauwerksmangelArt: null,
      });

      tick();

      expect(component['currentKnoten']).toEqual(nextKnoten);
    }));

    it('should read command from form correct (inkl bauwerksmangel)', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, id: 2, knotenVersion: 42 };
      const nextKnoten: Knoten = {
        ...defaultKnoten,
        id: 2,
        knotenVersion: 43,
        knotenForm: 'UEBERFUEHRUNG',
        bauwerksmangel: Bauwerksmangel.VORHANDEN,
        bauwerksmangelArt: [BauwerksmangelArt.ANDERER_MANGEL],
      };
      when(netzService.saveKnoten(anything())).thenReturn(Promise.resolve(nextKnoten));

      updateKnotenInRoute(currentKnoten);
      tick();
      fixture.detectChanges();
      component.knotenFormGroup.controls.shared.patchValue({
        knotenForm: 'UEBERFUEHRUNG',
        bauwerksmangel: {
          vorhanden: Bauwerksmangel.VORHANDEN,
          bauwerksmangelArt: [BauwerksmangelArt.ANDERER_MANGEL],
        },
      });
      component.knotenFormGroup.markAsDirty();
      component.onSave();

      verify(netzService.saveKnoten(anything())).once();
      const command = capture(netzService.saveKnoten).last()[0];
      expect(command).toEqual({
        id: 2,
        gemeinde: 1,
        kommentar: 'kommentar',
        zustandsbeschreibung: 'default Zustandsbeschreibung',
        knotenForm: 'UEBERFUEHRUNG',
        knotenVersion: 42,
        querungshilfeDetails: null,
        bauwerksmangel: Bauwerksmangel.VORHANDEN,
        bauwerksmangelArt: [BauwerksmangelArt.ANDERER_MANGEL],
      });

      tick();

      expect(component['currentKnoten']).toEqual(nextKnoten);
      expect(component.knotenFormGroup.getRawValue()).toEqual({
        ortslage: currentKnoten.ortslage,
        gemeinde: currentKnoten.gemeinde,
        landkreis: currentKnoten.landkreis?.name ?? null,
        kommentar: currentKnoten.kommentar,
        zustandsbeschreibung: currentKnoten.zustandsbeschreibung,
        shared: {
          knotenForm: 'UEBERFUEHRUNG',
          querungshilfeDetails: null,
          bauwerksmangel: {
            vorhanden: Bauwerksmangel.VORHANDEN,
            bauwerksmangelArt: [BauwerksmangelArt.ANDERER_MANGEL],
          },
        },
      });
    }));
  });

  describe('edit RadNETZ', () => {
    it('should disable control if RadNETZ-Knoten is selected', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, quelle: QuellSystem.RadNETZ };
      when(activatedRoute.snapshot).thenReturn({
        data: {
          knoten: currentKnoten,
        },
      } as unknown as ActivatedRouteSnapshot);

      paramsSubject.next(convertToParamMap({ id: 1 }));
      tick();

      expect(component.knotenFormGroup.disabled).toBeTrue();
    }));

    it('should enable controls if non-RadNETZ-Knoten is selected', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, quelle: QuellSystem.DLM };
      when(activatedRoute.snapshot).thenReturn({
        data: {
          knoten: currentKnoten,
        },
      } as unknown as ActivatedRouteSnapshot);

      paramsSubject.next(convertToParamMap({ id: 1 }));
      tick();

      expect(component.knotenFormGroup.disabled).toBeFalse();
    }));
  });

  describe('onReset', () => {
    it('should set currentKnoten to Form', fakeAsync(() => {
      const currentKnoten: Knoten = {
        ...defaultKnoten,
        knotenForm: 'MITTELINSEL_EINFACH',
        bauwerksmangel: null,
        bauwerksmangelArt: null,
        id: 2,
        knotenVersion: 42,
      };
      updateKnotenInRoute(currentKnoten);
      tick();
      component.knotenFormGroup.controls.shared.patchValue({
        knotenForm: 'UEBERFUEHRUNG',
        bauwerksmangel: {
          vorhanden: Bauwerksmangel.VORHANDEN,
          bauwerksmangelArt: [BauwerksmangelArt.ANDERER_MANGEL],
        },
      });

      component.onReset();

      expect(component.knotenFormGroup.getRawValue()).toEqual({
        ortslage: currentKnoten.ortslage,
        gemeinde: currentKnoten.gemeinde,
        landkreis: currentKnoten.landkreis?.name ?? null,
        kommentar: currentKnoten.kommentar,
        zustandsbeschreibung: currentKnoten.zustandsbeschreibung,
        shared: {
          knotenForm: 'MITTELINSEL_EINFACH',
          querungshilfeDetails: currentKnoten.querungshilfeDetails,
          bauwerksmangel: { vorhanden: null, bauwerksmangelArt: null },
        },
      });
    }));
  });

  describe('handle readOnly-Knoten', () => {
    it('should disable control if readonly-Knoten is selected', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, liegtInZustaendigkeitsbereich: false };
      updateKnotenInRoute(currentKnoten);
      tick();

      expect(component.knotenFormGroup.disabled).toBeTrue();
    }));

    it('should enable controls if editable Knoten is selected', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, liegtInZustaendigkeitsbereich: true };
      updateKnotenInRoute(currentKnoten);
      tick();

      expect(component.knotenFormGroup.disabled).toBeFalse();

      expect(component.knotenFormGroup.controls.landkreis.disabled).toBeTrue();
      expect(component.knotenFormGroup.controls.ortslage.disabled).toBeTrue();
    }));
  });
});
