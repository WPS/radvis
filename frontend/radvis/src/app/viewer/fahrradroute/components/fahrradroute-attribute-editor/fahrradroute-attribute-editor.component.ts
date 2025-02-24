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

import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, OnDestroy } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup, UntypedFormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { Feature } from 'ol';
import { Color } from 'ol/color';
import { Coordinate } from 'ol/coordinate';
import { GPX } from 'ol/format';
import { MultiLineString } from 'ol/geom';
import { Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { KommazahlPipe } from 'src/app/shared/components/kommazahl.pipe';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { isMultiLineString } from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import {
  belagArtLegende,
  flatRadverkehrsfuehrungOptions,
  radverkehrsfuehrungLegende,
} from 'src/app/viewer/fahrradroute/components/fahrradroute-profil/strecken-eigenschaften-legende.config';
import { AbschnittsweiserKantenNetzbezug } from 'src/app/viewer/fahrradroute/models/abschnittsweiser-kanten-netzbezug';
import { ChangeFahrradrouteVeroeffentlichtCommand } from 'src/app/viewer/fahrradroute/models/change-fahrradroute-veroeffentlicht-command';
import { DeleteFahrradrouteCommand } from 'src/app/viewer/fahrradroute/models/delete-fahrradroute-command';
import { FahrradrouteDetailView } from 'src/app/viewer/fahrradroute/models/fahrradroute-detail-view';
import { FahrradrouteProfil } from 'src/app/viewer/fahrradroute/models/fahrradroute-profil';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute.netzbezug';
import { SaveFahrradrouteCommand } from 'src/app/viewer/fahrradroute/models/save-fahrradroute-command';
import { Tourenkategorie } from 'src/app/viewer/fahrradroute/models/tourenkategorie';
import { VarianteKategorie } from 'src/app/viewer/fahrradroute/models/variante-kategorie';
import { FahrradrouteFilterService } from 'src/app/viewer/fahrradroute/services/fahrradroute-filter.service';
import { FahrradrouteProfilService } from 'src/app/viewer/fahrradroute/services/fahrradroute-profil.service';
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { FahrradrouteTyp } from 'src/app/viewer/viewer-shared/models/fahrradroute-typ';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-fahrradroute-attribute-editor',
  templateUrl: './fahrradroute-attribute-editor.component.html',
  styleUrls: ['./fahrradroute-attribute-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FahrradrouteAttributeEditorComponent implements OnDestroy, DiscardableComponent {
  public static readonly MAX_LENGTH_BESCHREIBUNG = 5000;
  public static readonly MAX_LENGTH_TEXT = 255;

  public VarianteKategorie = VarianteKategorie;

  public readonly MAX_LENGTH_KURZBESCHREIBUNG = 500;
  public readonly MAX_LENGTH_BESCHREIBUNG = FahrradrouteAttributeEditorComponent.MAX_LENGTH_BESCHREIBUNG;
  public readonly MAX_LENGTH_TEXT = FahrradrouteAttributeEditorComponent.MAX_LENGTH_TEXT;

  public readonly HAUPTSTRECKE = 'Hauptstrecke';

  public tourenkategorieOptions = Tourenkategorie.options;
  public kategorieOptions = FahrradrouteKategorie.options;

  formGroup = new FormGroup({
    name: new FormControl<string | null>(null, [
      RadvisValidators.isNotNullOrEmpty,
      RadvisValidators.maxLength(this.MAX_LENGTH_TEXT),
    ]),
    kurzbeschreibung: new FormControl<string | null>(null, [
      RadvisValidators.maxLength(this.MAX_LENGTH_KURZBESCHREIBUNG),
    ]),
    beschreibung: new FormControl<string | null>(null, [RadvisValidators.maxLength(this.MAX_LENGTH_BESCHREIBUNG)]),
    fahrradrouteKategorie: new FormControl<FahrradrouteKategorie | null>(null),
    tourenkategorie: new FormControl<Tourenkategorie | null>(null),
    laengeHauptstrecke: new FormControl<number | null>({ value: null, disabled: true }),
    offizielleLaenge: new FormControl<number | null>(null),
    verantwortlich: new FormControl<Verwaltungseinheit | null>(null),
    homepage: new FormControl<string | null>(null, [
      RadvisValidators.url,
      RadvisValidators.maxLength(this.MAX_LENGTH_TEXT),
    ]),
    emailAnsprechpartner: new FormControl<string | null>(null, [
      RadvisValidators.email,
      RadvisValidators.maxLength(this.MAX_LENGTH_TEXT),
    ]),
    lizenz: new FormControl<string | null>(null, [RadvisValidators.maxLength(this.MAX_LENGTH_TEXT)]),
    lizenzNamensnennung: new FormControl<string | null>(null, [RadvisValidators.maxLength(this.MAX_LENGTH_TEXT)]),
    anstieg: new FormControl<string | null>({ value: null, disabled: true }),
    abstieg: new FormControl<string | null>({ value: null, disabled: true }),
    toubizId: new FormControl<string | null>({ value: null, disabled: true }),
    info: new FormControl<string | null>({ value: null, disabled: true }),
    zuletztBearbeitet: new FormControl<string | null>({ value: '', disabled: true }),
    varianten: new FormArray<
      FormGroup<{
        id: FormControl<number | null>;
        kategorie: FormControl<VarianteKategorie | null>;
        netzbezug: FormControl<FahrradrouteNetzbezug | null>;
        kantenBezug: FormControl<AbschnittsweiserKantenNetzbezug[] | null>;
      }>
    >([]),
    netzbezug: new FormControl<FahrradrouteNetzbezug | null>(null),
  });

  public currentFahrradroute: FahrradrouteDetailView | null = null;
  public featureTogglzFehlerAnzeigen: boolean;
  public alleOrganisationenOptions: Promise<Verwaltungseinheit[]>;
  public originalGeometrieAnzeigen = false;
  public isFetching = false;
  public isSavingVeroeffentlichung = false;
  public belagArtLegende = belagArtLegende;
  public belagArtOptions = BelagArt.options;
  public veroeffentlicht = false;

  public radverkehrsfuehrungLegende = radverkehrsfuehrungLegende;
  public radverkehrsfuehrungOptions = flatRadverkehrsfuehrungOptions;

  public selectedVarianteControl = new UntypedFormControl(this.HAUPTSTRECKE);
  public editStreckeEnabled = false;

  isRouting = false;

  private subscriptions: Subscription[] = [];
  private gpxFormatter: GPX;
  private netzbezugSubscription: Subscription | undefined;

  constructor(
    organisationenService: OrganisationenService,
    private activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private infrastrukturenSelektionService: InfrastrukturenSelektionService,
    featureTogglzService: FeatureTogglzService,
    private notifyUserService: NotifyUserService,
    private fileHandlingService: FileHandlingService,
    private fahrradrouteService: FahrradrouteService,
    private fahrradrouteFilterService: FahrradrouteFilterService,
    private olMapService: OlMapService,
    private changeDetector: ChangeDetectorRef,
    private dialog: MatDialog,
    private fahrradrouteProfilService: FahrradrouteProfilService
  ) {
    this.alleOrganisationenOptions = organisationenService.getOrganisationen();

    this.subscriptions.push(
      this.selectedVarianteControl.valueChanges.subscribe(() => {
        this.updateNetzbezugSubscription();
        const fahrradrouteNetzbezug = this.selectedNetzbezugControl?.value as FahrradrouteNetzbezug;
        this.fahrradrouteProfilService.updateCurrentRouteProfil({
          name: this.currentFahrradroute?.name ?? '',
          geometrie: fahrradrouteNetzbezug?.geometrie ?? undefined,
          profilEigenschaften: fahrradrouteNetzbezug?.profilEigenschaften ?? undefined,
        });
        if (!fahrradrouteNetzbezug?.geometrie && !this.editStreckeEnabled) {
          this.showKeineRoutenInformationenHinweis();
        }
      })
    );

    this.subscriptions.push(
      this.activatedRoute.data
        .pipe(map(data => data.fahrradrouteDetailView))
        .subscribe((detailView: FahrradrouteDetailView) => {
          this.fahrradrouteProfilService.updateCurrentRouteProfil({
            name: detailView.name,
            geometrie: detailView.geometrie,
            profilEigenschaften: detailView.profilEigenschaften,
          } as FahrradrouteProfil);
          this.resetForm(detailView);
          this.focusFahrradrouteIntoView();
        })
    );

    this.subscriptions.push(
      (this.formGroup.controls.fahrradrouteKategorie as AbstractControl).valueChanges.subscribe(value => {
        const toubizIdField = this.formGroup.controls.toubizId;
        if (value === FahrradrouteKategorie.LANDESRADFERNWEG) {
          toubizIdField?.enable();
        } else {
          toubizIdField?.patchValue(null, { emitEvent: false });
          toubizIdField?.disable();
        }
      })
    );

    this.infrastrukturenSelektionService.selectInfrastrukturen(FAHRRADROUTE);
    this.featureTogglzFehlerAnzeigen = featureTogglzService.fahrradrouteFehlerInfos;
    this.gpxFormatter = new GPX();
  }

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  canDiscard(): boolean {
    return this.formGroup.pristine;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.netzbezugSubscription?.unsubscribe();
    this.fahrradrouteProfilService.hideCurrentRouteProfile();
  }

  get selectedNetzbezugControl(): FormControl | null {
    if (this.isHauptstreckeSelected) {
      return this.formGroup.controls.netzbezug;
    }

    return this.formGroup.controls.varianten.controls[this.selectedVarianteControl.value]?.controls.netzbezug ?? null;
  }

  get isHauptstreckeSelected(): boolean {
    return this.selectedVarianteControl.value === this.HAUPTSTRECKE;
  }

  get deleteSelectedStreckeForbidden(): boolean {
    return (
      (this.currentFahrradroute?.fahrradrouteKategorie === FahrradrouteKategorie.LANDESRADFERNWEG ||
        this.currentFahrradroute?.fahrradrouteKategorie === FahrradrouteKategorie.D_ROUTE) &&
      this.isHauptstreckeSelected
    );
  }

  get canEditAttribute(): boolean {
    return this.currentFahrradroute?.canEditAttribute ?? false;
  }

  get canChangeVeroeffentlicht(): boolean {
    return this.currentFahrradroute?.canChangeVeroeffentlicht ?? false;
  }

  get isFremdsystemLeading(): boolean {
    return (
      this.currentFahrradroute?.fahrradrouteTyp === FahrradrouteTyp.TFIS_ROUTE ||
      this.currentFahrradroute?.fahrradrouteTyp === FahrradrouteTyp.TOUBIZ_ROUTE
    );
  }

  get highlightColor(): Color {
    if (this.isHauptstreckeSelected) {
      return this.editStreckeEnabled ? MapStyles.FEATURE_SELECT_COLOR_TRANSPARENT : MapStyles.FEATURE_SELECT_COLOR;
    }

    return MapStyles.FEATURE_SELECT_COLOR_TRANSPARENT;
  }

  get fahrradrouteNetzbezug(): FahrradrouteNetzbezug | null {
    return this.formGroup.controls.netzbezug.value;
  }

  get selectedVarianteNetzbezug(): FahrradrouteNetzbezug | null {
    if (this.isHauptstreckeSelected) {
      return null;
    }

    if (this.editStreckeEnabled) {
      // dann wird das schon über das Control dargestellt
      return null;
    }

    if (!this.selectedNetzbezugControl?.value.geometrie) {
      return null;
    }

    return this.selectedNetzbezugControl?.value;
  }

  get selectedVarianteKantenBezug(): AbschnittsweiserKantenNetzbezug[] | null {
    if (this.isHauptstreckeSelected) {
      return null;
    }

    if (this.editStreckeEnabled) {
      // dann wird das schon über das Control dargestellt
      return null;
    }
    return this.formGroup.controls.varianten.controls[this.selectedVarianteControl.value]?.controls.kantenBezug.value;
  }

  get gpxDownloadTooltip(): string {
    return !this.fahrradrouteNetzbezug
      ? 'GPX Export nur verfügbar, wenn ein Routenverlauf ermittelt werden konnte'
      : 'Exportiere Routenverlauf von ausgewählter Variante als .GPX';
  }

  public getAnzahlKehrtwenden(fahrradrouteDetailView: FahrradrouteDetailView): number {
    if (!fahrradrouteDetailView.kehrtwenden) {
      return 0;
    }

    return fahrradrouteDetailView.kehrtwenden.length;
  }

  public getAnzahlAbweichenderSegmente(fahrradrouteDetailView: FahrradrouteDetailView): number {
    if (!fahrradrouteDetailView.abweichendeSegmente) {
      return 0;
    }

    return fahrradrouteDetailView.abweichendeSegmente.coordinates.length;
  }

  public getAbbildungErfolgreich(currentFahrradroute: FahrradrouteDetailView): string {
    return currentFahrradroute.kantenBezug?.length > 0 ? 'Ja' : 'Nein';
  }

  public getWelcheAbbildungWarErfolgreich(currentFahrradroute: FahrradrouteDetailView): string {
    if (!currentFahrradroute.abbildungDurchRouting && this.getAbbildungErfolgreich(currentFahrradroute) === 'Ja') {
      return 'Matching';
    }

    if (currentFahrradroute.abbildungDurchRouting && this.getAbbildungErfolgreich(currentFahrradroute) === 'Ja') {
      return 'Routing';
    }

    return 'Keiner erfolgreich';
  }

  public onToggleOriginalGeometrieAnzeigen(): void {
    this.originalGeometrieAnzeigen = !this.originalGeometrieAnzeigen;
  }

  onVerlaufUebernehmen(): void {
    this.editStreckeEnabled = false;
    this.netzbezugSubscription?.unsubscribe();
  }

  onEditVerlauf(): void {
    this.editStreckeEnabled = true;
    this.updateNetzbezugSubscription();
  }

  async onVarianteAdded(kategorie: VarianteKategorie): Promise<void> {
    let initialNetzbezug: FahrradrouteNetzbezug | null = null;
    this.netzbezugSubscription?.unsubscribe();
    if (kategorie === VarianteKategorie.GEGENRICHTUNG) {
      const fahrradrouteNetzbezug = this.formGroup.controls.netzbezug.value;
      if (!Boolean(fahrradrouteNetzbezug)) {
        return;
      }
      const stuetzpunkte: Coordinate[] = fahrradrouteNetzbezug!.stuetzpunkte?.slice() || [];
      stuetzpunkte.reverse();
      const routingResult = await this.fahrradrouteService.routeFahrradroutenVerlauf(
        stuetzpunkte,
        fahrradrouteNetzbezug!.customProfileId
      );
      initialNetzbezug = {
        geometrie: routingResult.routenGeometrie,
        kantenIDs: routingResult.kantenIDs,
        stuetzpunkte,
        profilEigenschaften: routingResult.profilEigenschaften,
        customProfileId: fahrradrouteNetzbezug!.customProfileId,
      };
    }
    this.formGroup.controls.varianten.push(
      new FormGroup({
        id: new FormControl<number | null>(null),
        kategorie: new FormControl(kategorie),
        netzbezug: new FormControl(initialNetzbezug, RadvisValidators.isNotNullOrEmpty),
        kantenBezug: new FormControl<AbschnittsweiserKantenNetzbezug[] | null>(null),
      })
    );
    // passiert offenbar nicht automatisch beim push
    this.formGroup.controls.varianten.markAsDirty();
    this.editStreckeEnabled = true;
    this.selectedVarianteControl.setValue(this.formGroup.controls.varianten.length - 1);

    // manchmal sind wir hier async unterwegs, weil wir den initialNetzbezug in Gegenrichtung aus dem Backend abwarten
    this.changeDetector.markForCheck();
  }

  public onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  onSave(): void {
    invariant(this.currentFahrradroute);

    if (this.formGroup.pristine) {
      return;
    }

    if (this.formGroup.invalid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    const {
      name,
      kurzbeschreibung,
      beschreibung,
      fahrradrouteKategorie,
      tourenkategorie,
      homepage,
      emailAnsprechpartner,
      lizenz,
      lizenzNamensnennung,
      toubizId,
      netzbezug,
    } = this.formGroup.value;

    const fahrradrouteNetzbezug = netzbezug!;
    const command: SaveFahrradrouteCommand = {
      id: this.currentFahrradroute.id,
      version: this.currentFahrradroute.version,
      name,
      kurzbeschreibung,
      beschreibung,
      kategorie: fahrradrouteKategorie,
      tourenkategorie,
      offizielleLaenge: this.formGroup.controls.offizielleLaenge.value
        ? this.formGroup.controls.offizielleLaenge.value * 1000
        : null,
      homepage: homepage || null,
      verantwortlichId: this.formGroup.controls.verantwortlich.value
        ? this.formGroup.controls.verantwortlich.value.id
        : null,
      emailAnsprechpartner: emailAnsprechpartner || null,
      lizenz,
      lizenzNamensnennung,
      toubizId: fahrradrouteKategorie === FahrradrouteKategorie.LANDESRADFERNWEG ? toubizId : undefined,
      stuetzpunkte: netzbezug ? { coordinates: fahrradrouteNetzbezug.stuetzpunkte, type: 'LineString' } : null,
      kantenIDs: fahrradrouteNetzbezug?.kantenIDs || [],
      routenVerlauf: fahrradrouteNetzbezug?.geometrie || null,
      profilEigenschaften: fahrradrouteNetzbezug?.profilEigenschaften || [],
      varianten: this.formGroup.controls.varianten.value.map((variante: any) => {
        return {
          id: variante.id,
          kategorie: variante.kategorie,
          kantenIDs: variante.netzbezug.kantenIDs,
          stuetzpunkte: variante.netzbezug.stuetzpunkte
            ? {
                coordinates: variante.netzbezug.stuetzpunkte,
                type: 'LineString',
              }
            : undefined,
          geometrie: variante.netzbezug.geometrie,
          profilEigenschaften: variante.netzbezug.profilEigenschaften,
          customProfileId: variante.netzbezug.customProfileId,
        };
      }),
      customProfileId: fahrradrouteNetzbezug?.customProfileId,
    } as SaveFahrradrouteCommand;

    this.isFetching = true;
    this.fahrradrouteService
      .saveFahrradroute(command)
      .then(detailView => {
        this.currentFahrradroute = detailView;
        this.notifyUserService.inform('Fahrradroute wurde erfolgreich gespeichert.');
        this.resetForm(detailView);
      })
      .finally(() => {
        this.isFetching = false;
        this.fahrradrouteFilterService.refetchData();
        this.editStreckeEnabled = false;
        this.changeDetector.markForCheck();
      });
  }

  onReset(): void {
    invariant(this.currentFahrradroute);
    this.resetForm(this.currentFahrradroute);
  }

  onDownloadAsGPX(): void {
    const coords = (this.selectedNetzbezugControl?.value as FahrradrouteNetzbezug).geometrie.coordinates;
    // Objekte vom Typ MultiLineString werden im GPX-Format von OpenLayers zu GPX-Tracks konvertiert. Normale
    // LineStrings werden jedoch zu GPX-Routen konvertiert, was wohl eher unüblich ist und viele Anwendungen
    // erwarten eher einen GPX-Track.
    const geometry = new MultiLineString([coords]).transform('EPSG:25832', 'EPSG:4326');
    const geometryAsGpxStr = this.gpxFormatter.writeFeatures([new Feature(geometry)]);
    let selectedVarianteName = '';
    if (this.isHauptstreckeSelected) {
      selectedVarianteName = this.HAUPTSTRECKE;
    } else {
      const selectedVarianteKategorie =
        this.formGroup.controls.varianten.controls[this.selectedVarianteControl.value]?.controls.kategorie.value;
      if (selectedVarianteKategorie) {
        selectedVarianteName = VarianteKategorie.getName(selectedVarianteKategorie);
      }
    }
    const fileName = this.currentFahrradroute?.name + ' - ' + selectedVarianteName + '.gpx';
    this.fileHandlingService.download(geometryAsGpxStr, fileName, 'text/gpx-xml');
  }

  getDisplayText(
    varianteControl: FormGroup<{
      id: FormControl<number | null>;
      kategorie: FormControl<VarianteKategorie | null>;
      netzbezug: FormControl<FahrradrouteNetzbezug | null>;
      kantenBezug: FormControl<AbschnittsweiserKantenNetzbezug[] | null>;
    }>
  ): string {
    const kategorie = varianteControl.controls.kategorie.value;
    invariant(kategorie);
    const index = this.formGroup.controls.varianten.controls
      .filter(control => control.controls.kategorie.value === kategorie)
      .indexOf(varianteControl);
    return VarianteKategorie.getName(kategorie) + (index > 0 ? ' (' + index + ')' : '');
  }

  onOeffneHoehenprofil(): void {
    this.fahrradrouteProfilService.showCurrentRouteProfile();
  }

  onSelectedStreckeLoeschen(): void {
    invariant(this.selectedNetzbezugControl);
    invariant(!this.deleteSelectedStreckeForbidden);

    let question: string;
    if (this.isHauptstreckeSelected) {
      question = 'Möchten Sie die Hauptstrecke und damit die gesamte Fahrradroute löschen?';
    } else {
      question = 'Möchten Sie die Variante löschen?';
    }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question,
        labelYes: 'Löschen',
        labelNo: 'Abbrechen',
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        if (this.isHauptstreckeSelected) {
          invariant(this.currentFahrradroute);
          const deleteCommand: DeleteFahrradrouteCommand = {
            id: this.currentFahrradroute?.id,
            version: this.currentFahrradroute?.version,
          };

          this.fahrradrouteService.deleteFahrradroute(deleteCommand).then(() => {
            this.fahrradrouteFilterService.refetchData();
            this.onClose();
          });
        } else {
          this.formGroup.controls.varianten.removeAt(this.selectedVarianteControl.value);
          this.formGroup.controls.varianten.markAsDirty();
          this.selectedVarianteControl.setValue(this.HAUPTSTRECKE);
        }
      }
    });
  }

  onVeroeffentlichtChanged(checked: boolean): void {
    invariant(this.currentFahrradroute);

    const command: ChangeFahrradrouteVeroeffentlichtCommand = {
      id: this.currentFahrradroute.id,
      version: this.currentFahrradroute.version,
      veroeffentlicht: checked,
    } as ChangeFahrradrouteVeroeffentlichtCommand;

    this.isSavingVeroeffentlichung = true;
    this.fahrradrouteService
      .updateVeroeffentlicht(command)
      .then(detailView => {
        this.currentFahrradroute = detailView;
        this.resetForm(detailView);

        if (this.veroeffentlicht) {
          this.notifyUserService.inform('Fahrradroute wurde erfolgreich als veröffentlicht markiert.');
        } else {
          this.notifyUserService.inform('Fahrradroute wurde erfolgreich als nicht-veröffentlicht markiert.');
        }
      })
      .finally(() => {
        this.isSavingVeroeffentlichung = false;
        this.changeDetector.markForCheck();
      });
  }

  getProfilAuswertungFuerRadverkehrsfuehrung(radverkehrsfuehrung: string): { prozent: number; kilometer: number } {
    return this.getProfilAuswertung('Radverkehrsfuehrung', radverkehrsfuehrung);
  }

  getProfilAuswertungFuerBelagArt(belagArt: BelagArt): { prozent: number; kilometer: number } {
    return this.getProfilAuswertung('BelagArt', belagArt);
  }

  private getProfilAuswertung(
    auszuwertendeEigenschaft: 'BelagArt' | 'Radverkehrsfuehrung',
    wert: BelagArt | string
  ): { prozent: number; kilometer: number } {
    let anteil = 0;
    if (this.currentFahrradroute) {
      for (const singleProfil of this.currentFahrradroute.profilEigenschaften) {
        if (
          (auszuwertendeEigenschaft === 'BelagArt' && singleProfil.belagArt === wert) ||
          (auszuwertendeEigenschaft === 'Radverkehrsfuehrung' && singleProfil.radverkehrsfuehrung === wert)
        ) {
          anteil = anteil + (singleProfil.bisLR - singleProfil.vonLR);
        }
      }
    }
    return {
      prozent: Math.round(anteil * 100 * 100) / 100,
      kilometer: Math.round(anteil * (this.currentFahrradroute?.laengeHauptstrecke ?? 0)) / 1000,
    };
  }

  private focusFahrradrouteIntoView(): void {
    let toFocus;
    if (this.currentFahrradroute?.geometrie?.coordinates) {
      toFocus = [this.currentFahrradroute?.geometrie?.coordinates];
    } else if (this.currentFahrradroute?.kantenBezug && this.currentFahrradroute.kantenBezug.length > 0) {
      toFocus = this.currentFahrradroute?.kantenBezug.map(kantenbezug => kantenbezug.geometrie.coordinates);
    } else if (this.currentFahrradroute?.originalGeometrie) {
      if (isMultiLineString(this.currentFahrradroute.originalGeometrie)) {
        toFocus = this.currentFahrradroute.originalGeometrie.coordinates;
      } else {
        toFocus = [this.currentFahrradroute.originalGeometrie.coordinates];
      }
    }
    if (toFocus && toFocus.length > 0) {
      this.olMapService.scrollIntoViewByGeometry(new MultiLineString(toFocus));
    }
  }

  private resetForm(fahrradrouteDetailView: FahrradrouteDetailView): void {
    this.currentFahrradroute = fahrradrouteDetailView;
    invariant(this.currentFahrradroute);

    let netzbezug: FahrradrouteNetzbezug | null = null;
    if (this.currentFahrradroute.geometrie) {
      netzbezug = {
        geometrie: this.currentFahrradroute.geometrie,
        kantenIDs: this.currentFahrradroute.kantenBezug.map(k => k.kanteId),
        stuetzpunkte: this.currentFahrradroute.stuetzpunkte?.coordinates ?? [],
        profilEigenschaften: this.currentFahrradroute.profilEigenschaften,
        customProfileId: this.currentFahrradroute.customProfileId,
      };
    }
    this.formGroup.reset({
      ...this.currentFahrradroute,
      offizielleLaenge: this.currentFahrradroute.offizielleLaenge
        ? this.currentFahrradroute.offizielleLaenge / 1000
        : null,
      laengeHauptstrecke: this.currentFahrradroute.laengeHauptstrecke
        ? this.currentFahrradroute.laengeHauptstrecke / 1000
        : null,
      anstieg: this.currentFahrradroute.anstieg ? KommazahlPipe.numberToString(this.currentFahrradroute.anstieg) : null,
      abstieg: this.currentFahrradroute.abstieg ? KommazahlPipe.numberToString(this.currentFahrradroute.abstieg) : null,
      zuletztBearbeitet: fahrradrouteDetailView.zuletztBearbeitet
        ? new DatePipe('en-US').transform(new Date(fahrradrouteDetailView.zuletztBearbeitet), 'dd.MM.yy HH:mm')!
        : null,
      netzbezug,
    });

    this.formGroup.controls.varianten.clear();

    this.currentFahrradroute.varianten.sort((vA, vB) => vA.id - vB.id);

    this.currentFahrradroute?.varianten.forEach(v => {
      this.formGroup.controls.varianten.push(
        new FormGroup({
          id: new FormControl(v.id),
          netzbezug: new FormControl(
            {
              kantenIDs: v.kantenIDs,
              stuetzpunkte: v.stuetzpunkte?.coordinates,
              geometrie: v.geometrie,
              profilEigenschaften: v.profilEigenschaften,
              customProfileId: v.customProfileId,
            } as FahrradrouteNetzbezug,
            RadvisValidators.isNotNullOrEmpty
          ),
          kantenBezug: new FormControl(v.kantenBezug),
          kategorie: new FormControl(v.kategorie),
        })
      );
    });

    this.formGroup.disable({ emitEvent: false });
    if (this.currentFahrradroute.canEditAttribute) {
      this.enableForm();
      if (this.currentFahrradroute.fahrradrouteKategorie === FahrradrouteKategorie.LANDESRADFERNWEG) {
        this.formGroup.controls.toubizId.enable({ emitEvent: false });
      }
    }

    this.selectedVarianteControl.setValue(this.HAUPTSTRECKE);

    if (!this.currentFahrradroute.canEditAttribute) {
      this.editStreckeEnabled = false;
    }

    this.veroeffentlicht = this.currentFahrradroute.veroeffentlicht;
  }

  private enableForm(): void {
    this.formGroup.enable({ emitEvent: false });
    this.formGroup.controls.laengeHauptstrecke.disable({ emitEvent: false });
    this.formGroup.controls.toubizId.disable({ emitEvent: false });
    this.formGroup.controls.anstieg.disable({ emitEvent: false });
    this.formGroup.controls.abstieg.disable({ emitEvent: false });
    this.formGroup.controls.info.disable({ emitEvent: false });
    this.formGroup.controls.zuletztBearbeitet.disable({ emitEvent: false });
  }

  private updateNetzbezugSubscription(): void {
    this.netzbezugSubscription?.unsubscribe();
    this.netzbezugSubscription = this.selectedNetzbezugControl?.valueChanges.subscribe(netzbezug => {
      if (this.currentFahrradroute && netzbezug?.geometrie) {
        this.fahrradrouteProfilService.updateCurrentRouteProfil({
          name: this.currentFahrradroute.name,
          geometrie: netzbezug.geometrie,
          profilEigenschaften: netzbezug.profilEigenschaften,
        });
      }
    });
  }

  private showKeineRoutenInformationenHinweis(): void {
    const keinRoutenVerlaufMeldung = 'Es konnte kein eindeutiger Routenverlauf ermittelt werden. ';
    if (this.isFremdsystemLeading) {
      this.notifyUserService.warn(keinRoutenVerlaufMeldung + 'Bitte korrigieren Sie die Daten im Quellsystem.');
    } else {
      this.notifyUserService.warn(
        keinRoutenVerlaufMeldung +
          'Bitte legen Sie den Verlauf in RadVIS fest oder korrigieren Sie die Daten im Quellsystem.'
      );
    }
  }
}
