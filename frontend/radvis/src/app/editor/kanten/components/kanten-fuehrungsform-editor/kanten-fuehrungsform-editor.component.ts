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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  UntypedFormArray,
  UntypedFormControl,
  UntypedFormGroup,
  ValidatorFn,
} from '@angular/forms';
import { Feature } from 'ol';
import { LineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { StyleFunction } from 'ol/style/Style';
import { Observable, of } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { AbstractLinearReferenzierteAttributGruppeEditor } from 'src/app/editor/kanten/components/abstract-linear-referenzierte-attribut-gruppe-editor';
import { Benutzungspflicht } from 'src/app/editor/kanten/models/benutzungspflicht';
import { Bordstein } from 'src/app/editor/kanten/models/bordstein';
import { FuehrungsformAttributGruppe } from 'src/app/editor/kanten/models/fuehrungsform-attribut-gruppe';
import { FuehrungsformAttribute } from 'src/app/editor/kanten/models/fuehrungsform-attribute';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { KfzParkenForm } from 'src/app/editor/kanten/models/kfz-parken-form';
import { KfzParkenTyp } from 'src/app/editor/kanten/models/kfz-parken-typ';
import { Oberflaechenbeschaffenheit } from 'src/app/editor/kanten/models/oberflaechenbeschaffenheit';
import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { SaveFuehrungsformAttributGruppeCommand } from 'src/app/editor/kanten/models/save-fuehrungsform-attribut-gruppe-command';
import { SaveFuehrungsformAttributeCommand } from 'src/app/editor/kanten/models/save-fuehrungsform-attribute-command';
import { TrennstreifenForm } from 'src/app/editor/kanten/models/trennstreifen-form';
import { TrennstreifenTrennungZu } from 'src/app/editor/kanten/models/trennstreifen-trennung-zu';
import { fillFormWithMultipleValues } from 'src/app/editor/kanten/services/fill-form-with-multiple-values';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';
import { TrennstreifenSeite } from 'src/app/shared/models/trennstreifen-seite';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardGuardService } from 'src/app/shared/services/discard-guard.service';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-kanten-fuehrungsform-editor',
  templateUrl: './kanten-fuehrungsform-editor.component.html',
  styleUrls: [
    './kanten-fuehrungsform-editor.component.scss',
    '../../../../form-elements/components/attribute-editor/attribut-editor.scss',
    '../abstract-attribut-gruppe-editor-mit-auswahl.scss',
    '../lineare-referenz-tabelle.scss',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KantenFuehrungsformEditorComponent
  extends AbstractLinearReferenzierteAttributGruppeEditor<FuehrungsformAttribute, FuehrungsformAttributGruppe>
  implements DiscardableComponent, OnDestroy, OnInit
{
  public LINKS = KantenSeite.LINKS;
  public RECHTS = KantenSeite.RECHTS;

  public Richtung = Richtung;

  public bordsteinOptions = Bordstein.options;
  public oberflaechenbeschaffenheitOptions = Oberflaechenbeschaffenheit.options;
  public belagartOptions = BelagArt.options;
  public radverkehrsfuehrungOptions = Radverkehrsfuehrung.options;
  public benutzungspflicht = Benutzungspflicht.options;
  public parkenTypOptions = KfzParkenTyp.options;
  public parkenFormOptions = KfzParkenForm.options;

  // Hält die linearen Referenzen. Verbunden mit den LineareReferenzControls
  public lineareReferenzenLinksFormArray: UntypedFormArray = new UntypedFormArray([]);
  public lineareReferenzenRechtsFormArray: UntypedFormArray = new UntypedFormArray([]);

  private validateTrennungZu: ValidatorFn = ctrl => {
    if (
      ctrl.value &&
      !(ctrl.value instanceof UndeterminedValue) &&
      !this.trennstreifenTrennungZuOptions.some(opt => opt.name === ctrl.value)
    ) {
      return {
        trennungZuInvalid: `Aktueller Wert "${TrennstreifenTrennungZu.displayText(ctrl.value)}" ist für die gewählte Radverkehrsführung nicht erlaubt, bitte Auswahl ändern.`,
      };
    }

    return null;
  };

  // Sicherheitstrennstreifen
  public trennstreifenFormGroupLinks = new FormGroup({
    trennstreifenFormLinks: new FormControl<TrennstreifenForm | null | UndeterminedValue>(null, [
      RadvisValidators.isNotNullOrEmpty,
    ]),
    trennstreifenTrennungZuLinks: new FormControl<TrennstreifenTrennungZu | null | UndeterminedValue>(
      null,
      this.validateTrennungZu
    ),
    trennstreifenBreiteLinks: new FormControl<number | null | UndeterminedValue>(null, [
      RadvisValidators.isNotNullOrEmpty,
      RadvisValidators.isPositiveFloatNumber,
      RadvisValidators.maxDecimalPlaces(2),
    ]),
  });
  public trennstreifenFormGroupRechts = new FormGroup({
    trennstreifenFormRechts: new FormControl<TrennstreifenForm | null | UndeterminedValue>(null, [
      RadvisValidators.isNotNullOrEmpty,
    ]),
    trennstreifenTrennungZuRechts: new FormControl<TrennstreifenTrennungZu | null | UndeterminedValue>(
      null,
      this.validateTrennungZu
    ),
    trennstreifenBreiteRechts: new FormControl<number | null | UndeterminedValue>(null, [
      RadvisValidators.isNotNullOrEmpty,
      RadvisValidators.isPositiveFloatNumber,
      RadvisValidators.maxDecimalPlaces(2),
    ]),
  });

  public trennstreifenSeiteOptions: EnumOption[] = [];
  public trennstreifenFormOptions = TrennstreifenForm.options;
  public trennstreifenTrennungZuOptions = TrennstreifenTrennungZu.options;

  public trennstreifenEinseitig?: boolean;
  public trennstreifenRichtungRechts?: Richtung;
  public trennstreifenRichtungLinks?: Richtung;
  public trennstreifenSeiteSelected: TrennstreifenSeite | undefined;
  public trennstreifenBearbeiteteSeiten: Set<TrennstreifenSeite> = new Set<TrennstreifenSeite>();

  private stationierungsrichtungSource: VectorSource;
  private stationierungsrichtungLayer: VectorLayer;

  // Nur für diese Radverkehrsführungen gibt es überhaupt Trennstreifen:
  private relevanteRadverkehrsfuehrungen: Radverkehrsfuehrung[] = [
    Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER,
    Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.SCHUTZSTREIFEN,
    Radverkehrsfuehrung.RADFAHRSTREIFEN,
    Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,
    Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
    Radverkehrsfuehrung.MEHRZWECKSTREIFEN,
  ];

  // Für diese Radverkehrsführungen gibt es nur "zum Parken" als "Trennung Zu"-Wert:
  private trennstreifenNurParkenRadverkehrsfuehrungen: Radverkehrsfuehrung[] = [
    Radverkehrsfuehrung.SCHUTZSTREIFEN,
    Radverkehrsfuehrung.RADFAHRSTREIFEN,
    Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,
    Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
    Radverkehrsfuehrung.MEHRZWECKSTREIFEN,
  ];

  constructor(
    private netzService: NetzService,
    private olMapService: OlMapService,
    private featureTogglzService: FeatureTogglzService,
    private discardGuardService: DiscardGuardService,
    changeDetectorRef: ChangeDetectorRef,
    notifyUserService: NotifyUserService,
    kantenSelektionService: KantenSelektionService,
    benutzerDetailsService: BenutzerDetailsService
  ) {
    super(changeDetectorRef, notifyUserService, kantenSelektionService, benutzerDetailsService);

    this.stationierungsrichtungSource = new VectorSource();
    this.stationierungsrichtungLayer = new VectorLayer({
      source: this.stationierungsrichtungSource,
      style: this.arrowStyleFn,
    });
    this.olMapService.addLayer(this.stationierungsrichtungLayer);

    this.subscriptions.push(
      this.lineareReferenzenLinksFormArray.valueChanges.subscribe((value: LinearReferenzierterAbschnitt[][]) =>
        this.updateCurrentAttributgruppenWithLineareReferenzen(value, KantenSeite.LINKS)
      ),
      this.lineareReferenzenRechtsFormArray.valueChanges.subscribe((value: LinearReferenzierterAbschnitt[][]) => {
        this.updateCurrentAttributgruppenWithLineareReferenzen(value, KantenSeite.RECHTS);
      }),
      this.trennstreifenFormGroupLinks.valueChanges.subscribe(() => {
        this.onTrennstreifenFormValueChanged(this.trennstreifenFormGroupLinks);
      }),
      this.trennstreifenFormGroupRechts.valueChanges.subscribe(() => {
        this.onTrennstreifenFormValueChanged(this.trennstreifenFormGroupRechts);
      }),
      this.displayedAttributeformGroup
        .get('radverkehrsfuehrung')!
        .valueChanges.subscribe((value: Radverkehrsfuehrung) => {
          this.onRadverkehrsfuehrungChanged(value);
        })
    );
  }

  ngOnInit(): void {
    super.subscribeToKantenSelektion();
    this.subscriptions.push(
      this.kantenSelektionService.selektierteKanten$.subscribe(kanten => {
        this.stationierungsrichtungSource.clear();
        this.stationierungsrichtungSource.addFeatures(
          kanten.map(k => new Feature(new LineString(k.geometry.coordinates)))
        );
        this.stationierungsrichtungSource.changed();
        this.changeDetectorRef.markForCheck();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  // eslint-disable-next-line prettier/prettier
  public override onClose(): void {
    super.onClose();
    this.unsetTrennstreifenForm();
    this.trennstreifenSeiteSelected = undefined;
    this.trennstreifenEinseitig = undefined;
  }

  // eslint-disable-next-line prettier/prettier
  public override onSave(): void {
    if (
      !this.displayedAttributeformGroup.valid ||
      (this.trennstreifenFormGroupLinks.dirty && !this.trennstreifenFormGroupLinks.valid) ||
      (this.trennstreifenFormGroupRechts.dirty && !this.trennstreifenFormGroupRechts.valid)
    ) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    super.onSave();
  }

  // eslint-disable-next-line prettier/prettier
  public override get pristine(): boolean {
    return (
      super.pristine &&
      this.lineareReferenzenLinksFormArray.pristine &&
      this.lineareReferenzenRechtsFormArray.pristine &&
      this.trennstreifenBearbeiteteSeiten.size === 0
    );
  }

  public get isTrennstreifenFeatureEnabled(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_SICHERHEITSTRENNSTREIFEN);
  }

  public isTrennstreifenFormVisible(): boolean {
    return this.getAttributeForSelektion(this.currentSelektion).every(attribute =>
      this.relevanteRadverkehrsfuehrungen.includes(attribute.radverkehrsfuehrung)
    );
  }

  public isAnyTrennstreifenLinksSelektiert(): boolean {
    return (
      !!this.trennstreifenSeiteSelected &&
      (this.trennstreifenSeiteSelected === TrennstreifenSeite.A ||
        this.trennstreifenSeiteSelected === TrennstreifenSeite.C)
    );
  }

  public isAnyTrennstreifenRechtsSelektiert(): boolean {
    return (
      !!this.trennstreifenSeiteSelected &&
      (this.trennstreifenSeiteSelected === TrennstreifenSeite.B ||
        this.trennstreifenSeiteSelected === TrennstreifenSeite.D)
    );
  }

  public getLineareReferenzenLinksFormControlAt(index: number): UntypedFormControl {
    return this.lineareReferenzenLinksFormArray.at(index) as UntypedFormControl;
  }

  public getLineareReferenzenRechtsFormControlAt(index: number): UntypedFormControl {
    return this.lineareReferenzenRechtsFormArray.at(index) as UntypedFormControl;
  }

  public onInsertAtIndex(kantenIndex: number, segmentIndex: number, kantenSeite?: KantenSeite): void {
    let arrayToChange: FuehrungsformAttribute[];
    if (kantenSeite === KantenSeite.LINKS || kantenSeite === undefined) {
      arrayToChange = this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeLinks;
      const deepCopy = JSON.parse(JSON.stringify(arrayToChange[segmentIndex - 1])) as FuehrungsformAttribute;
      arrayToChange.splice(segmentIndex, 0, deepCopy);
    }
    if (kantenSeite === KantenSeite.RECHTS || kantenSeite === undefined) {
      arrayToChange = this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeRechts;
      const deepCopy = JSON.parse(JSON.stringify(arrayToChange[segmentIndex - 1])) as FuehrungsformAttribute;
      arrayToChange.splice(segmentIndex, 0, deepCopy);
    }
    this.kantenSelektionService.adjustSelectionForSegmentInsertion(
      (this.currentSelektion as KantenSelektion[])[kantenIndex].kante.id,
      segmentIndex,
      kantenSeite
    );
  }

  public onDeleteAtIndex(kantenIndex: number, segmentIndex: number, kantenSeite?: KantenSeite): void {
    let arrayToChange: FuehrungsformAttribute[];
    if (kantenSeite === KantenSeite.LINKS || kantenSeite === undefined) {
      arrayToChange = this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeLinks;
      arrayToChange.splice(segmentIndex, 1);
    }
    if (kantenSeite === KantenSeite.RECHTS || kantenSeite === undefined) {
      arrayToChange = this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeRechts;
      arrayToChange.splice(segmentIndex, 1);
    }
    this.kantenSelektionService.adjustSelectionForSegmentDeletion(
      (this.currentSelektion as KantenSelektion[])[kantenIndex].kante.id,
      segmentIndex,
      kantenSeite
    );
  }

  public override canDiscard(): boolean {
    return (
      super.canDiscard() && this.trennstreifenFormGroupRechts.pristine && this.trennstreifenFormGroupLinks.pristine
    );
  }

  onTrennstreifenSeiteSelectionChanged(seite: TrennstreifenSeite): void {
    // Seitenbezug ändert sich (initiale Selektion oder z.B. Wechsel von Trennstreifen B zu C)
    const seitenbezugChanged =
      (!this.trennstreifenSeiteSelected && !this.trennstreifenEinseitig) ||
      (seite === TrennstreifenSeite.A || seite === TrennstreifenSeite.B) !==
        (this.trennstreifenSeiteSelected === TrennstreifenSeite.A ||
          this.trennstreifenSeiteSelected === TrennstreifenSeite.B);

    // Manche Wechsel (z.B. A -> B) sind ok, manche können aber dazu führen, dass Kanten(seiten) deselektiert werden,
    // wodurch Änderungen verloren gingen. Der Wechsel von Trennstreifen-Seiten (also A/B <-> C/D) triggert also bei
    // angefassten Forms immer den Discard-Guard um sicher zu gehen, dass keine Änderungen verloren gehen.
    let canDiscardObservable: Observable<boolean>;
    if (this.currentSelektion && seitenbezugChanged && seite != null && this.trennstreifenSeiteSelected != null) {
      canDiscardObservable = this.discardGuardService.canDeactivate(this) as Observable<boolean>;
    } else {
      canDiscardObservable = of(true);
    }

    this.subscriptions.push(
      canDiscardObservable.subscribe(proceed => {
        if (!proceed) {
          return;
        }

        this.trennstreifenSeiteSelected = seite;

        // Zunächst alle Werte im Drop-Down zulassen, damit eine vorherige Einschränkung (z.B. auf "nur Parken") nicht dazu
        // führt, dass das Drop-Down leer bleibt, sollte sich die Menge der Auswahlmöglichkeiten durch die neue Selektion
        // geändert haben (z.B. auf "alle", wodurch Werte abseits "nur Parken" möglich sind, die aber ggf. nicht im Drop-
        // Down angezeigt würden).
        this.trennstreifenTrennungZuOptions = TrennstreifenTrennungZu.options;

        if (this.currentSelektion) {
          if (seitenbezugChanged) {
            this.resetTrennstreifenAttribute(this.currentSelektion);
          }

          let zuSelektierendeKanten: Kante[];
          let zuSelektierendeSeite: KantenSeite;
          if (
            this.trennstreifenSeiteSelected === TrennstreifenSeite.C ||
            this.trennstreifenSeiteSelected === TrennstreifenSeite.D
          ) {
            zuSelektierendeKanten = this.currentSelektion.map(s => s.kante).filter(k => k.zweiseitig);
            zuSelektierendeSeite = KantenSeite.RECHTS;
          } else {
            zuSelektierendeKanten = this.currentSelektion.map(s => s.kante);
            zuSelektierendeSeite = KantenSeite.LINKS;
          }

          // Selektiere korrekte Kanten(seiten) für die Seite des ausgewählten Trennstreifens.
          this.currentSelektion.forEach(selektion => {
            if (!zuSelektierendeKanten.includes(selektion.kante)) {
              return;
            }

            // Ggf. haben wir schon auf der zu selektierenden Seite einen ausgewählten Abschnitt -> dann Auswahl nicht verändern.
            if (!selektion.istSeiteSelektiert(zuSelektierendeSeite)) {
              if (selektion.kante.zweiseitig) {
                this.kantenSelektionService.select(selektion.kante.id, true, zuSelektierendeSeite);
              } else {
                this.kantenSelektionService.select(selektion.kante.id, true);
              }
            }
          });

          // Deselektiere die Kanten(seiten), die nicht mehr relevant sind, weil ein Trennstreifen der anderen Seite ausgewählt wurde.
          const zuDeselektierendeSeite =
            zuSelektierendeSeite === KantenSeite.RECHTS ? KantenSeite.LINKS : KantenSeite.RECHTS;
          this.currentSelektion.forEach(selektion => {
            if (selektion.kante.zweiseitig) {
              if (this.kantenSelektionService.isSelektiert(selektion.kante.id, zuDeselektierendeSeite)) {
                this.kantenSelektionService.deselect(selektion.kante.id, zuDeselektierendeSeite);
              }
            } else if (!zuSelektierendeKanten.includes(selektion.kante)) {
              if (this.kantenSelektionService.isSelektiert(selektion.kante.id)) {
                this.kantenSelektionService.deselect(selektion.kante.id);
              }
            }
          });
        }

        this.updateTrennungZuOptions(
          this.getFuehrungsformAttributeForTrennstreifenSeite(
            this.currentSelektion,
            this.trennstreifenSeiteSelected
          ).map(attr => attr.radverkehrsfuehrung)
        );
        this.updateTrennstreifenControlActivation(this.trennstreifenSeiteSelected);
      })
    );
  }

  protected saveAttributgruppe(attributgruppen: FuehrungsformAttributGruppe[]): Promise<Kante[]> {
    const commands = attributgruppen.map(attributgruppe => this.convertAttributgruppeToCommand(attributgruppe));
    return this.netzService.saveKanteFuehrungsform(commands);
  }

  protected createDisplayedAttributeFormGroup(): UntypedFormGroup {
    return new UntypedFormGroup({
      belagArt: new UntypedFormControl(null),
      oberflaechenbeschaffenheit: new UntypedFormControl(null),
      bordstein: new UntypedFormControl(null),
      radverkehrsfuehrung: new UntypedFormControl(null),
      benutzungspflicht: new UntypedFormControl(null),
      parkenTyp: new UntypedFormControl(null),
      parkenForm: new UntypedFormControl(null),
      breite: new UntypedFormControl(null, [
        RadvisValidators.isPositiveFloatNumber,
        RadvisValidators.maxDecimalPlaces(2),
        RadvisValidators.max(100),
      ]),
    });
  }

  protected override enableControls(): void {
    super.enableControls();
    this.lineareReferenzenLinksFormArray.enable({ emitEvent: false });
    this.lineareReferenzenRechtsFormArray.enable({ emitEvent: false });
    this.trennstreifenFormGroupLinks.enable({ emitEvent: false });
    this.trennstreifenFormGroupRechts.enable({ emitEvent: false });
  }

  protected override disableControls(): void {
    super.disableControls();
    this.lineareReferenzenLinksFormArray.disable({ emitEvent: false });
    this.lineareReferenzenRechtsFormArray.disable({ emitEvent: false });
    this.trennstreifenFormGroupLinks.disable({ emitEvent: false });
    this.trennstreifenFormGroupRechts.disable({ emitEvent: false });
  }

  protected override resetDisplayedAttribute(selektion: KantenSelektion[]): void {
    super.resetDisplayedAttribute(selektion);

    // Hat man einen Trennstreifen selektiert, aber wählt den einzigen Abschnitt ab, der für diesen Trennstreifen
    // Werte haben kann, dann soll die Selektion der Trennstreifen aufgehoben werden.
    // Beispiel: Man hat beide Seiten einer zweiseitigen Kante selektiert und "C" ausgewählt. Wählt man die rechte
    // Seite der Kante ab, gibt es keine Kantenauswahl mehr, die für "C" Werte haben kann, daher wird der
    // Trennstreifen auch abgewählt.
    if (
      (this.trennstreifenSeiteSelected === TrennstreifenSeite.A ||
        this.trennstreifenSeiteSelected === TrennstreifenSeite.B) &&
      selektion?.every(s => s.kante.zweiseitig && !s.istSeiteSelektiert(KantenSeite.LINKS))
    ) {
      this.trennstreifenSeiteSelected = undefined;
    } else if (
      (this.trennstreifenSeiteSelected === TrennstreifenSeite.C ||
        this.trennstreifenSeiteSelected === TrennstreifenSeite.D) &&
      selektion?.every(s => !s.kante.zweiseitig || !s.istSeiteSelektiert(KantenSeite.RECHTS))
    ) {
      this.trennstreifenSeiteSelected = undefined;
    }

    this.updateTrennungZuOptions(
      this.getFuehrungsformAttributeForTrennstreifenSeite(selektion, this.trennstreifenSeiteSelected).map(
        attr => attr.radverkehrsfuehrung
      )
    );
    this.resetTrennstreifenFormToOriginalKantenValues(selektion);
    this.trennstreifenBearbeiteteSeiten = new Set();
  }

  protected getAttributeForSelektion(selektion: KantenSelektion[] | null): FuehrungsformAttribute[] {
    const result: FuehrungsformAttribute[] = [];
    selektion?.forEach((kantenSelektion, kantenIndex) => {
      kantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS).forEach(selectedSegmentIndex => {
        result.push(this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeLinks[selectedSegmentIndex]);
      });
      kantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS).forEach(selectedSegmentIndex => {
        result.push(this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeRechts[selectedSegmentIndex]);
      });
    });
    return result;
  }

  protected resetLineareReferenzenFormArrays(newSelektion: KantenSelektion[]): void {
    const valuesLinks = newSelektion.map(kantenSelektion =>
      this.extractLineareReferenzenFromKante(kantenSelektion.kante, KantenSeite.LINKS)
    );
    const valuesRechts = newSelektion.map(kantenSelektion =>
      this.extractLineareReferenzenFromKante(kantenSelektion.kante, KantenSeite.RECHTS)
    );

    this.resetFormArray(this.lineareReferenzenFormArray, valuesLinks);
    this.resetFormArray(this.lineareReferenzenLinksFormArray, valuesLinks);
    this.resetFormArray(this.lineareReferenzenRechtsFormArray, valuesRechts);
  }

  protected convertAttributgruppeToCommand(
    fuehrungsformAttributGruppe: FuehrungsformAttributGruppe
  ): SaveFuehrungsformAttributGruppeCommand {
    const attributeCommandLinks: SaveFuehrungsformAttributeCommand[] =
      fuehrungsformAttributGruppe.fuehrungsformAttributeLinks as SaveFuehrungsformAttributeCommand[];
    const attributeCommandRechts: SaveFuehrungsformAttributeCommand[] =
      fuehrungsformAttributGruppe.fuehrungsformAttributeRechts as SaveFuehrungsformAttributeCommand[];
    const associatedKantenSelektion = this.currentSelektion?.find(
      kantenSelektion => kantenSelektion.kante.fuehrungsformAttributGruppe.id === fuehrungsformAttributGruppe.id
    ) as KantenSelektion;

    return {
      gruppenID: fuehrungsformAttributGruppe.id,
      gruppenVersion: fuehrungsformAttributGruppe.version,
      fuehrungsformAttributeLinks: attributeCommandLinks,
      fuehrungsformAttributeRechts: attributeCommandRechts,
      kanteId: associatedKantenSelektion.kante.id,
    };
  }

  protected updateCurrentAttributgruppenWithLineareReferenzen(
    newLineareReferenzenArrays: LinearReferenzierterAbschnitt[][],
    kantenSeite?: KantenSeite
  ): void {
    newLineareReferenzenArrays.forEach((lineareReferenzen, kantenIndex) => {
      lineareReferenzen.forEach((lineareReferenz, segmentIndex) => {
        if (kantenSeite === undefined) {
          if (!this.kantenSelektionService.selektierteKanten[kantenIndex].zweiseitig) {
            this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeLinks[
              segmentIndex
            ].linearReferenzierterAbschnitt = lineareReferenz;
            this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeRechts[
              segmentIndex
            ].linearReferenzierterAbschnitt = lineareReferenz;
          }
        } else if (kantenSeite === KantenSeite.LINKS) {
          if (this.kantenSelektionService.selektierteKanten[kantenIndex].zweiseitig) {
            this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeLinks[
              segmentIndex
            ].linearReferenzierterAbschnitt = lineareReferenz;
          }
        } else if (kantenSeite === KantenSeite.RECHTS) {
          if (this.kantenSelektionService.selektierteKanten[kantenIndex].zweiseitig) {
            this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeRechts[
              segmentIndex
            ].linearReferenzierterAbschnitt = lineareReferenz;
          }
        }
      });
    });
  }

  protected updateCurrentAttributgruppenWithAttribute(changedAttributePartial: { [id: string]: any }): void {
    const trennstreifenFormVisibleBefore = this.isTrennstreifenFormVisible();

    this.currentSelektion?.forEach(kantenSelektion => {
      const attributgruppeToChange = this.currentAttributgruppen.find(
        gruppe => gruppe.id === kantenSelektion.kante.fuehrungsformAttributGruppe.id
      ) as FuehrungsformAttributGruppe;
      kantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS).forEach(selectedSegmentIndex => {
        attributgruppeToChange.fuehrungsformAttributeLinks[selectedSegmentIndex] = {
          ...attributgruppeToChange.fuehrungsformAttributeLinks[selectedSegmentIndex],
          ...changedAttributePartial,
        };
      });
      kantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS).forEach(selectedSegmentIndex => {
        attributgruppeToChange.fuehrungsformAttributeRechts[selectedSegmentIndex] = {
          ...attributgruppeToChange.fuehrungsformAttributeRechts[selectedSegmentIndex],
          ...changedAttributePartial,
        };
      });
    });

    const trennstreifenFormVisibleAfter = this.isTrennstreifenFormVisible();

    if (!trennstreifenFormVisibleAfter) {
      // Setzt Trennstreifen zurück, da es keine Trennstreifen gibt. Also im Sinne von: Setzt alle Felder auf null.

      this.trennstreifenFormGroupLinks.reset();
      this.trennstreifenFormGroupRechts.reset();
      this.trennstreifenBearbeiteteSeiten = new Set();

      this.currentAttributgruppen.forEach(attributgruppe => {
        [...attributgruppe.fuehrungsformAttributeLinks, ...attributgruppe.fuehrungsformAttributeRechts].forEach(
          attribute => {
            if (!this.relevanteRadverkehrsfuehrungen.includes(attribute.radverkehrsfuehrung)) {
              attribute.trennstreifenFormLinks = null;
              attribute.trennstreifenTrennungZuLinks = null;
              attribute.trennstreifenBreiteLinks = null;

              attribute.trennstreifenFormRechts = null;
              attribute.trennstreifenTrennungZuRechts = null;
              attribute.trennstreifenBreiteRechts = null;
            }
          }
        );
      });
    } else if (!trennstreifenFormVisibleBefore && trennstreifenFormVisibleAfter) {
      // Resettet Trennstreifen-Form auf Werte der Kantenselektion. Es kann nämlich sein, dass die Auswahl einer
      // Radverkehrsführung ohne Trennstreifen (z.B. Piktogrammkette) nur temporär war, weil sich z.B. der Nutzer
      // verklickt hat. In dem Fall, dass also die Auswahl wieder zurückgesetzt wird, stellen wir die derzeitigen Werte
      // wieder her, damit nicht "undefined" (also nichts) gespeichert wird.
      this.resetTrennstreifenFormToOriginalKantenValues(this.currentSelektion);
    }
  }

  protected getAttributGruppeFrom(kante: Kante): FuehrungsformAttributGruppe {
    return kante.fuehrungsformAttributGruppe;
  }

  private extractLineareReferenzenFromKante(kante: Kante, kantenSeite: KantenSeite): LinearReferenzierterAbschnitt[] {
    let fuehrungsformAttributeArray: FuehrungsformAttribute[];
    if (kantenSeite === KantenSeite.LINKS) {
      fuehrungsformAttributeArray = kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks;
    } else if (kantenSeite === KantenSeite.RECHTS) {
      fuehrungsformAttributeArray = kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts;
    } else {
      throw Error('Kein gültiger Seitenbezug');
    }
    return fuehrungsformAttributeArray.map(
      fuehrungsformAttribute => fuehrungsformAttribute.linearReferenzierterAbschnitt
    );
  }

  private onTrennstreifenFormValueChanged(form: UntypedFormGroup): void {
    if (!this.currentSelektion || !this.trennstreifenSeiteSelected) {
      return;
    }

    const relevanteAttributGruppen = this.getFuehrungsformAttributeForTrennstreifenSeite(
      this.currentSelektion,
      this.trennstreifenSeiteSelected
    );

    relevanteAttributGruppen.forEach(attribute =>
      this.applyFormValuesToAttribute(attribute, form, this.trennstreifenSeiteSelected)
    );

    if (!form.pristine) {
      this.trennstreifenBearbeiteteSeiten = new Set(this.trennstreifenBearbeiteteSeiten);
      this.trennstreifenBearbeiteteSeiten.add(this.trennstreifenSeiteSelected);
    }
  }

  // Aktualisiert "Trennung Zu"-Feld, da Werte abhängig von Radverkehrsführung sind.
  private updateTrennungZuOptions(radverkehrsfuehrungValues: Radverkehrsfuehrung[]): void {
    if (radverkehrsfuehrungValues.some(rvf => this.trennstreifenNurParkenRadverkehrsfuehrungen.includes(rvf))) {
      this.trennstreifenTrennungZuOptions = TrennstreifenTrennungZu.optionsParken;
    } else {
      this.trennstreifenTrennungZuOptions = TrennstreifenTrennungZu.options;
    }
  }

  private unsetTrennstreifenForm(): void {
    this.trennstreifenFormGroupLinks.reset(undefined, { emitEvent: false });
    this.trennstreifenFormGroupRechts.reset(undefined, { emitEvent: false });
    this.trennstreifenBearbeiteteSeiten = new Set();
  }

  // Setzt das Trennstreifen-Form auf die Werte der Kante zurück, also auf die unveränderten Originalwerte.
  private resetTrennstreifenFormToOriginalKantenValues(selektion: KantenSelektion[] | null): void {
    selektion?.forEach((kantenSelektion, kantenIndex) => {
      this.resetToOriginalTrennstreifenAttribute(
        kantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS),
        kantenSelektion.kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
        this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeLinks
      );
      this.resetToOriginalTrennstreifenAttribute(
        kantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS),
        kantenSelektion.kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
        this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeRechts
      );
    });

    this.resetTrennstreifenAttribute(selektion);
    this.updateTrennstreifenControlActivation(this.trennstreifenSeiteSelected);
  }

  // Überträgt Werte aus der Trennstreifen-Form für die gegebene Seite in die FührungsformAttribute
  private applyFormValuesToAttribute(
    attribute: FuehrungsformAttribute,
    form: UntypedFormGroup,
    seite: TrennstreifenSeite | undefined
  ): void {
    // Erst Form-Felder aktualisieren, bevor deren Werte in die FührungsformAttribute übertragen werden.
    this.updateTrennstreifenControlActivation(seite);
    this.updateTrennungZuOptions([attribute.radverkehrsfuehrung]);

    Object.keys(form.getRawValue()).forEach(formKey => {
      const field = form.get(formKey);
      if (!(field?.value instanceof UndeterminedValue)) {
        // @ts-expect-error Migration von ts-ignore
        attribute[formKey] = field?.value;
      }
    });
  }

  private resetToOriginalTrennstreifenAttribute(
    selectedSegmentIndices: number[],
    originalAttribute: FuehrungsformAttribute[],
    currentFuehrungsformAttribute: FuehrungsformAttribute[]
  ): void {
    originalAttribute.forEach((attribute, selectedSegmentIndex) => {
      if (selectedSegmentIndices.includes(selectedSegmentIndex)) {
        const trennstreifenAttribute = {
          trennstreifenFormLinks: attribute.trennstreifenFormLinks,
          trennstreifenTrennungZuLinks: attribute.trennstreifenTrennungZuLinks,
          trennstreifenBreiteLinks: attribute.trennstreifenBreiteLinks,
          trennstreifenFormRechts: attribute.trennstreifenFormRechts,
          trennstreifenTrennungZuRechts: attribute.trennstreifenTrennungZuRechts,
          trennstreifenBreiteRechts: attribute.trennstreifenBreiteRechts,
        };

        currentFuehrungsformAttribute[selectedSegmentIndex] = {
          ...currentFuehrungsformAttribute[selectedSegmentIndex],
          ...trennstreifenAttribute,
        };
      }
    });
  }

  private updateTrennstreifenControlActivation(seite: TrennstreifenSeite | undefined): void {
    const linkerTrennstreifen = seite === TrennstreifenSeite.A || seite === TrennstreifenSeite.C;

    let trennstreifenFormValue: TrennstreifenForm | UndeterminedValue | null;
    let trennstreifenTrennungZuControl: AbstractControl | null;
    let trennstreifenBreiteControl: AbstractControl | null;

    if (linkerTrennstreifen) {
      trennstreifenFormValue = this.trennstreifenFormGroupLinks.value.trennstreifenFormLinks ?? null;
      trennstreifenTrennungZuControl = this.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks;
      trennstreifenBreiteControl = this.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks;
    } else {
      trennstreifenFormValue = this.trennstreifenFormGroupRechts.value.trennstreifenFormRechts ?? null;
      trennstreifenTrennungZuControl = this.trennstreifenFormGroupRechts.controls.trennstreifenTrennungZuRechts;
      trennstreifenBreiteControl = this.trennstreifenFormGroupRechts.controls.trennstreifenBreiteRechts;
    }

    if (
      trennstreifenFormValue === TrennstreifenForm.UNBEKANNT ||
      trennstreifenFormValue === TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN
    ) {
      trennstreifenTrennungZuControl.setValue(null, { emitEvent: false });
      trennstreifenTrennungZuControl.disable({ emitEvent: false });
      trennstreifenBreiteControl.setValue(null, { emitEvent: false });
      trennstreifenBreiteControl.disable({ emitEvent: false });
    } else {
      trennstreifenTrennungZuControl.enable({ emitEvent: false });
      trennstreifenBreiteControl.enable({ emitEvent: false });
    }
  }

  private resetTrennstreifenAttribute(selektionen: KantenSelektion[] | null): void {
    if (selektionen && selektionen.length >= 1) {
      this.trennstreifenEinseitig = !selektionen.some(selektion => selektion.kante.zweiseitig);

      if (this.trennstreifenEinseitig) {
        this.trennstreifenSeiteOptions = TrennstreifenSeite.optionsLinks;
      } else {
        this.trennstreifenSeiteOptions = [...TrennstreifenSeite.optionsLinks, ...TrennstreifenSeite.optionsRechts];
      }

      const fahrtrichtungenLinks = [
        ...new Set(selektionen.map(selektion => selektion.kante.fahrtrichtungAttributGruppe.fahrtrichtungLinks)),
      ];
      this.trennstreifenRichtungLinks = fahrtrichtungenLinks.length === 1 ? fahrtrichtungenLinks[0] : undefined;

      const fahrtrichtungenRechts = [
        ...new Set(
          selektionen
            .filter(selektion => selektion.kante.zweiseitig)
            .map(selektion => selektion.kante.fahrtrichtungAttributGruppe.fahrtrichtungRechts)
        ),
      ];
      this.trennstreifenRichtungRechts = fahrtrichtungenRechts.length === 1 ? fahrtrichtungenRechts[0] : undefined;
    }

    const relevanteAttribute = this.getFuehrungsformAttributeForTrennstreifenSeite(
      selektionen,
      this.trennstreifenSeiteSelected
    );
    if (relevanteAttribute.length === 0) {
      this.trennstreifenFormGroupLinks.reset(undefined, { emitEvent: false });
      this.trennstreifenFormGroupRechts.reset(undefined, { emitEvent: false });
      return;
    }

    // Gleiche "relevanteAttribute", da die Namen der Form-Felder passend zu den rechts/links Trennstreifen-Feldern in den Attributen sind.
    fillFormWithMultipleValues(this.trennstreifenFormGroupLinks, relevanteAttribute, false);
    fillFormWithMultipleValues(this.trennstreifenFormGroupRechts, relevanteAttribute, false);
  }

  private getFuehrungsformAttributeForTrennstreifenSeite(
    selektion: KantenSelektion[] | null,
    seite: TrennstreifenSeite | undefined
  ): FuehrungsformAttribute[] {
    const relevanteAttributGruppen: FuehrungsformAttribute[] = [];

    selektion?.forEach((kantenSelektion, kantenIndex) => {
      const linkerRadweg = seite === TrennstreifenSeite.A || seite === TrennstreifenSeite.B;
      const rechterRadweg = seite === TrennstreifenSeite.C || seite === TrennstreifenSeite.D;

      if (linkerRadweg) {
        kantenSelektion.getSelectedSegmentIndices(KantenSeite.LINKS).forEach(selectedSegmentIndex => {
          relevanteAttributGruppen.push(
            this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeLinks[selectedSegmentIndex]
          );
        });
      }
      if ((kantenSelektion.kante.zweiseitig && rechterRadweg) || (!kantenSelektion.kante.zweiseitig && linkerRadweg)) {
        kantenSelektion.getSelectedSegmentIndices(KantenSeite.RECHTS).forEach(selectedSegmentIndex => {
          relevanteAttributGruppen.push(
            this.currentAttributgruppen[kantenIndex].fuehrungsformAttributeRechts[selectedSegmentIndex]
          );
        });
      }
    });

    return relevanteAttributGruppen;
  }

  private onRadverkehrsfuehrungChanged(radverkehrsfuehrung: Radverkehrsfuehrung): void {
    this.updateTrennungZuOptions([radverkehrsfuehrung]);
    this.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.updateValueAndValidity({ emitEvent: false });
    this.trennstreifenFormGroupRechts.controls.trennstreifenTrennungZuRechts.updateValueAndValidity({
      emitEvent: false,
    });
  }

  // eslint-disable-next-line no-unused-vars
  private arrowStyleFn: StyleFunction = (f, r) => {
    const coordinates = (f.getGeometry() as LineString).getCoordinates();
    return MapStyles.createArrowBegleitend(coordinates, MapStyles.FEATURE_COLOR);
  };
}
