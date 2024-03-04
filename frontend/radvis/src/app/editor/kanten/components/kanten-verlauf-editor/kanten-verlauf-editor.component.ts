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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormArray, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Color } from 'ol/color';
import { Geometry, LineString } from 'ol/geom';
import { Observable } from 'rxjs';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { AbstractAttributGruppeEditor } from 'src/app/editor/kanten/components/abstract-attribut-gruppe-editor';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { SaveKanteVerlaufCommand } from 'src/app/editor/kanten/models/save-kante-verlauf-command';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

@Component({
  selector: 'rad-kanten-verlauf-editor',
  templateUrl: './kanten-verlauf-editor.component.html',
  styleUrls: ['./kanten-verlauf-editor.component.scss', '../../../../form-elements/components/attribute-editor/attribut-editor.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KantenVerlaufEditorComponent extends AbstractAttributGruppeEditor implements OnInit {
  public colorBaseGeometry: Color = MapStyles.FEATURE_SELECT_COLOR;
  public colorVerlaufLinks: Color = MapStyles.VERLAUF_LINKS;
  public colorVerlaufRechts: Color = MapStyles.VERLAUF_RECHTS;
  public colorUrsprungsGeometrie: Color = MapStyles.FEATURE_SELECT_COLOR_TRANSPARENT;

  // eslint-disable-next-line prettier/prettier
  public override isFetching = false;
  formGroup: UntypedFormGroup;
  public showRadNetzHinweis = false;
  selektierteKanten$: Observable<Kante[]>;

  constructor(
    netzService: NetzService,
    changeDetectorRef: ChangeDetectorRef,
    editorRoutingService: EditorRoutingService,
    errorHandlingService: ErrorHandlingService,
    notifyUserService: NotifyUserService,
    kanteSelektionService: KantenSelektionService,
    private notifyGeometryChangedService: NotifyGeometryChangedService,
    private benutzerDetailsService: BenutzerDetailsService
  ) {
    super(
      netzService,
      errorHandlingService,
      notifyUserService,
      changeDetectorRef,
      editorRoutingService,
      kanteSelektionService
    );
    this.formGroup = KantenVerlaufEditorComponent.createForm();
    this.selektierteKanten$ = this.kanteSelektionService.selektierteKanten$;
  }

  get geometryFormControls(): UntypedFormControl[] {
    return (this.formGroup.get('geometries') as UntypedFormArray).controls as UntypedFormControl[];
  }

  get verlaufLinksFormControls(): UntypedFormControl[] {
    return (this.formGroup.get('verlaeufeLinks') as UntypedFormArray).controls as UntypedFormControl[];
  }

  get verlaufRechtsFormControls(): UntypedFormControl[] {
    return (this.formGroup.get('verlauefeRechts') as UntypedFormArray).controls as UntypedFormControl[];
  }

  get verlaufEinseitigFormControls(): UntypedFormControl[] {
    return (this.formGroup.get('verlaeufeEinseitig') as UntypedFormArray).controls as UntypedFormControl[];
  }

  get mindestensEinVerlaufFehlt(): boolean {
    return !this.kanteSelektionService.selektierteKanten.every((kante, i) => {
      if (kante.zweiseitig) {
        return this.verlaufLinksFormControls[i].value !== null && this.verlaufRechtsFormControls[i].value !== null;
      } else {
        return this.verlaufEinseitigFormControls[i].value !== null;
      }
    });
  }

  get mindestensEinVerlaufVorhanden(): boolean {
    return this.mindestensEinZweiseitigerVerlaufVorhanden || this.mindestensEinEinseitigerVerlaufVorhanden;
  }

  private get mindestensEinEinseitigerVerlaufVorhanden(): boolean {
    return this.verlaufEinseitigFormControls
      .map(formControl => formControl.value)
      .some(kantenGeometrie => kantenGeometrie);
  }

  private get mindestensEinZweiseitigerVerlaufVorhanden(): boolean {
    return (
      this.verlaufLinksFormControls.map(formControl => formControl.value).some(kantenGeometrie => kantenGeometrie) ||
      this.verlaufRechtsFormControls.map(formControl => formControl.value).some(kantenGeometrie => kantenGeometrie)
    );
  }

  private static createForm(): UntypedFormGroup {
    return new UntypedFormGroup({
      geometries: new UntypedFormArray([]),
      verlaeufeLinks: new UntypedFormArray([]),
      verlauefeRechts: new UntypedFormArray([]),
      verlaeufeEinseitig: new UntypedFormArray([]),
    });
  }

  ngOnInit(): void {
    this.kanteSelektionService.selektion$.subscribe(newSelektion => {
      const noRadNetzKanteSelected = !newSelektion.some(
        kantenSelektion => kantenSelektion.kante.quelle === QuellSystem.RadNETZ
      );
      this.showRadNetzHinweis = !noRadNetzKanteSelected;
      const allKantenInZustaendigkeitsbereich = newSelektion.every(
        kantenSelektion => kantenSelektion.kante.liegtInZustaendigkeitsbereich
      );
      const editingAllowed =
        noRadNetzKanteSelected &&
        (allKantenInZustaendigkeitsbereich || this.benutzerDetailsService.canEditGesamtesNetz());
      if (editingAllowed) {
        this.formGroup.enable({ emitEvent: false });
      }
      this.resetForm(newSelektion);
      if (!editingAllowed) {
        this.formGroup.disable({ emitEvent: false });
      }

      this.changeDetectorRef.markForCheck();
    });
  }

  public onKanteDeselected(kante: Kante): void {
    this.kanteSelektionService.deselect(kante.id);
  }

  getGeometryAtIndex(index: number): Geometry {
    const kantenGeometrie = (this.formGroup.get('geometries') as UntypedFormArray).at(index).value as LineStringGeojson;
    return new LineString(kantenGeometrie.coordinates);
  }

  public verlaeufeHinzufuegen(): void {
    const verlaeufeLinksPromises: Promise<LineStringGeojson | null>[] = [];
    const verlaeufeRechtsPromises: Promise<LineStringGeojson | null>[] = [];
    const verlaeufeEinseitigPromises: Promise<LineStringGeojson | null>[] = [];
    this.kanteSelektionService.selektierteKanten.forEach(kante => {
      if (kante.zweiseitig) {
        verlaeufeLinksPromises.push(this.netzService.berechneVerlaufLinks(kante.id));
        verlaeufeRechtsPromises.push(this.netzService.berechneVerlaufRechts(kante.id));
        verlaeufeEinseitigPromises.push(Promise.resolve(null));
      } else {
        verlaeufeLinksPromises.push(Promise.resolve(null));
        verlaeufeRechtsPromises.push(Promise.resolve(null));
        verlaeufeEinseitigPromises.push(Promise.resolve(kante.geometry));
      }
    });
    this.updateVerlaufControls(verlaeufeLinksPromises, this.verlaufLinksFormControls);
    this.updateVerlaufControls(verlaeufeRechtsPromises, this.verlaufRechtsFormControls);
    this.updateVerlaufControls(verlaeufeEinseitigPromises, this.verlaufEinseitigFormControls);
  }

  public verlaeufeLoeschen(): void {
    this.deleteVerlaufControlValues(this.verlaufLinksFormControls);
    this.deleteVerlaufControlValues(this.verlaufRechtsFormControls);
    this.deleteVerlaufControlValues(this.verlaufEinseitigFormControls);
    this.changeDetectorRef.markForCheck();
  }

  protected override onAfterSave(): void {
    this.notifyGeometryChangedService.notify();
  }

  protected resetForm(selektion: KantenSelektion[]): void {
    (this.formGroup.get('geometries') as UntypedFormArray).clear({ emitEvent: false });
    (this.formGroup.get('verlaeufeLinks') as UntypedFormArray).clear({ emitEvent: false });
    (this.formGroup.get('verlauefeRechts') as UntypedFormArray).clear({ emitEvent: false });
    (this.formGroup.get('verlaeufeEinseitig') as UntypedFormArray).clear({ emitEvent: false });
    selektion.forEach(kantenSelektion => {
      (this.formGroup.get('geometries') as UntypedFormArray).push(new UntypedFormControl(kantenSelektion.kante.geometry), {
        emitEvent: false,
      });
      (this.formGroup.get('verlaeufeLinks') as UntypedFormArray).push(new UntypedFormControl(kantenSelektion.kante.verlaufLinks), {
        emitEvent: false,
      });
      (this.formGroup.get('verlauefeRechts') as UntypedFormArray).push(new UntypedFormControl(kantenSelektion.kante.verlaufRechts), {
        emitEvent: false,
      });
      (this.formGroup.get('verlaeufeEinseitig') as UntypedFormArray).push(
        new UntypedFormControl(kantenSelektion.kante.verlaufLinks),
        {
          emitEvent: false,
        }
      );
    });
    this.formGroup.markAsPristine();
  }

  protected getForm(): AbstractControl {
    return this.formGroup;
  }

  protected save(): Promise<Kante[]> {
    const commands = this.kanteSelektionService.selektierteKanten.map((kante, index) => {
      return {
        id: kante.id,
        geometry: this.geometryFormControls[index].value,
        verlaufLinks: kante.zweiseitig
          ? this.verlaufLinksFormControls[index].value
          : this.verlaufEinseitigFormControls[index].value,
        verlaufRechts: kante.zweiseitig
          ? this.verlaufRechtsFormControls[index].value
          : this.verlaufEinseitigFormControls[index].value,
        kantenVersion: kante.kantenVersion,
      } as SaveKanteVerlaufCommand;
    });
    return this.netzService.saveKanteVerlauf(commands);
  }

  private deleteVerlaufControlValues(formControls: UntypedFormControl[]): void {
    formControls.forEach(formControl => {
      formControl.patchValue(null);
      formControl.markAsDirty();
    });
  }

  private updateVerlaufControls(promises: Promise<LineStringGeojson | null>[], formControls: UntypedFormControl[]): void {
    Promise.all(promises).then(verlaeufe => {
      verlaeufe.forEach((verlauf, index) => {
        if (!formControls[index].value) {
          formControls[index].patchValue(verlauf);
          formControls[index].markAsDirty();
        }
      });
      this.changeDetectorRef.markForCheck();
    });
  }
}
