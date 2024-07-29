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
import { ActivatedRoute, ActivatedRouteSnapshot, convertToParamMap, ParamMap, Router } from '@angular/router';
import { MockBuilder } from 'ng-mocks';
import { Subject } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KnotenAttributeEditorComponent } from 'src/app/editor/knoten/components/knoten-attribute-editor/knoten-attribute-editor.component';
import { Knoten } from 'src/app/editor/knoten/models/knoten';
import { defaultKnoten } from 'src/app/editor/knoten/models/knoten-test-data-provider.spec';
import { SaveKnotenCommand } from 'src/app/editor/knoten/models/save-knoten-command';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import {
  defaultGemeinden,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe('KnotenAttributeEditorComponent', () => {
  let component: KnotenAttributeEditorComponent;
  let fixture: ComponentFixture<KnotenAttributeEditorComponent>;
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
    return MockBuilder(KnotenAttributeEditorComponent, EditorModule)
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
    fixture = TestBed.createComponent(KnotenAttributeEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('form', () => {
    it('should fill form with knoten correct', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, id: 2 };
      when(activatedRoute.snapshot).thenReturn({
        data: {
          knoten: currentKnoten,
        },
      } as unknown as ActivatedRouteSnapshot);
      paramsSubject.next(convertToParamMap({ id: 1 }));

      tick();
      fixture.detectChanges();

      // eslint-disable-next-line @typescript-eslint/dot-notation
      expect(component['currentKnoten']).toEqual(currentKnoten);
      expect(component.knotenFormGroup.getRawValue()).toEqual({
        ortslage: currentKnoten.ortslage,
        gemeinde: currentKnoten.gemeinde,
        landkreis: currentKnoten.landkreis?.name,
        kommentar: currentKnoten.kommentar,
        zustandsbeschreibung: currentKnoten.zustandsbeschreibung,
        knotenForm: currentKnoten.knotenForm,
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
      when(activatedRoute.snapshot).thenReturn({
        data: {
          knoten: currentKnoten,
        },
      } as unknown as ActivatedRouteSnapshot);
      paramsSubject.next(convertToParamMap({ id: 1 }));

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

      component.knotenFormGroup.get('gemeinde')?.setValue({
        id: 36,
        name: 'Holzhausen',
        organisationsArt: OrganisationsArt.GEMEINDE,
        idUebergeordneteOrganisation: 23,
        aktiv: true,
      });

      tick();
      fixture.detectChanges();

      expect(component.knotenFormGroup.get('landkreis')?.value).toEqual(landkreis.name);
    }));

    it('should read command from form correct', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, id: 2, knotenVersion: 42 };
      const nextKnoten: Knoten = { ...defaultKnoten, id: 2, knotenVersion: 43 };
      when(activatedRoute.snapshot).thenReturn({
        data: {
          knoten: currentKnoten,
        },
      } as unknown as ActivatedRouteSnapshot);
      when(netzService.saveKnoten(anything())).thenReturn(Promise.resolve(nextKnoten));

      paramsSubject.next(convertToParamMap({ id: 1 }));
      tick();
      fixture.detectChanges();
      component.knotenFormGroup.markAsDirty();
      component.onSave();

      verify(netzService.saveKnoten(anything())).once();
      const command = capture(netzService.saveKnoten).last()[0];
      expect(command).toEqual({
        id: 2,
        gemeinde: 1,
        kommentar: 'kommentar',
        zustandsbeschreibung: 'default Zustandsbeschreibung',
        knotenForm: 'RECHTS_VOR_LINKS_REGELUNG',
        knotenVersion: 42,
      } as SaveKnotenCommand);

      tick();
      // eslint-disable-next-line @typescript-eslint/dot-notation
      expect(component['currentKnoten']).toEqual(nextKnoten);
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

  describe('handle readOnly-Knoten', () => {
    it('should disable control if readonly-Knoten is selected', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, liegtInZustaendigkeitsbereich: false };
      when(activatedRoute.snapshot).thenReturn({
        data: {
          knoten: currentKnoten,
        },
      } as unknown as ActivatedRouteSnapshot);

      paramsSubject.next(convertToParamMap({ id: 1 }));
      tick();

      expect(component.knotenFormGroup.disabled).toBeTrue();
    }));

    it('should enable controls if editable Knoten is selected', fakeAsync(() => {
      const currentKnoten: Knoten = { ...defaultKnoten, liegtInZustaendigkeitsbereich: true };
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
});
