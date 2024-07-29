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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { By } from '@angular/platform-browser';
import { MockComponent } from 'ng-mocks';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { ValidationErrorAnzeigeComponent } from 'src/app/form-elements/components/validation-error-anzeige/validation-error-anzeige.component';
import { ImportMassnahmenImportUeberpruefenLayerComponent } from 'src/app/import/massnahmen/components/import-massnahmen-import-ueberpruefen-layer/import-massnahmen-import-ueberpruefen-layer.component';
import { MassnahmenImportModule } from 'src/app/import/massnahmen/massnahmen-import.module';
import { MassnahmenImportAttribute } from 'src/app/import/massnahmen/models/massnahmen-import-attribute';
import { MassnahmenImportMassnahmenAuswaehlenCommand } from 'src/app/import/massnahmen/models/massnahmen-import-massnahmen-auswaehlen-command';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportZuordnungStatus } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-status';
import { MassnahmenImportZuordnungUeberpruefung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung';
import { getDefaultZuordnung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung-test-data-provider.spec';
import { MassnahmenImportZuordnungenService } from 'src/app/import/massnahmen/services/massnahmen-import-zuordnungen.service';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { ConfirmationDialogComponent } from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { KantenSeitenbezug } from 'src/app/shared/models/netzbezug';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { ImportMassnahmenImportUeberpruefenComponent } from './import-massnahmen-import-ueberpruefen.component';

describe(ImportMassnahmenImportUeberpruefenComponent.name, () => {
  let component: ImportMassnahmenImportUeberpruefenComponent;
  let fixture: ComponentFixture<ImportMassnahmenImportUeberpruefenComponent>;

  let olMapService: OlMapService;
  let massnahmenImportService: MassnahmenImportService;
  let massnahmenImportRoutingService: MassnahmenImportRoutingService;
  let massnahmenImportZuordnungenService: MassnahmenImportZuordnungenService;
  let dialog: MatDialog;

  let sessionSubject: Subject<MassnahmenImportSessionView | null>;
  let zuordnungenSubject: BehaviorSubject<MassnahmenImportZuordnungUeberpruefung[]>;
  let selektierteZuordnungsIdSubject: BehaviorSubject<number | undefined>;

  const defaultSession: MassnahmenImportSessionView = {
    gebietskoerperschaften: [1, 2],
    konzeptionsquelle: Konzeptionsquelle.KREISKONZEPT,
    attribute: [MassnahmenImportAttribute.BEZEICHNUNG, MassnahmenImportAttribute.PRIORITAET],
    log: [],
    schritt: 4,
    executing: false,
  };

  beforeEach(async () => {
    massnahmenImportService = mock(MassnahmenImportService);
    massnahmenImportRoutingService = mock(MassnahmenImportRoutingService);
    massnahmenImportZuordnungenService = mock(MassnahmenImportZuordnungenService);
    dialog = mock(MatDialog);

    sessionSubject = new Subject();
    when(massnahmenImportService.getImportSession()).thenReturn(sessionSubject.asObservable());

    olMapService = mock(OlMapComponent);

    zuordnungenSubject = new BehaviorSubject<MassnahmenImportZuordnungUeberpruefung[]>([]);
    when(massnahmenImportZuordnungenService.zuordnungen$).thenReturn(zuordnungenSubject.asObservable());

    selektierteZuordnungsIdSubject = new BehaviorSubject<number | undefined>(undefined);
    when(massnahmenImportZuordnungenService.selektierteZuordnungsId$).thenReturn(
      selektierteZuordnungsIdSubject.asObservable()
    );
    when(massnahmenImportZuordnungenService.selektierteZuordnungsId).thenCall(() =>
      selektierteZuordnungsIdSubject.getValue()
    );
    when(massnahmenImportZuordnungenService.selektiereZuordnung(anything())).thenCall(args =>
      selektierteZuordnungsIdSubject.next(args)
    );

    TestBed.overrideProvider(MassnahmenImportZuordnungenService, {
      useValue: instance(massnahmenImportZuordnungenService),
    });

    await TestBed.configureTestingModule({
      declarations: [
        ImportMassnahmenImportUeberpruefenComponent,
        MockComponent(ImportMassnahmenImportUeberpruefenLayerComponent),
        MockComponent(ValidationErrorAnzeigeComponent),
      ],
      imports: [
        MassnahmenImportModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatFormFieldModule,
        MaterialDesignModule,
      ],
      providers: [
        { provide: OlMapService, useValue: instance(olMapService) },
        { provide: MassnahmenImportService, useValue: instance(massnahmenImportService) },
        { provide: MassnahmenImportRoutingService, useValue: instance(massnahmenImportRoutingService) },
        { provide: MatDialog, useValue: instance(dialog) },
      ],
    }).compileComponents();
  });

  describe('with single Zuordnung', () => {
    let zuordnungen: MassnahmenImportZuordnungUeberpruefung[];

    beforeEach(async () => {
      zuordnungen = [getDefaultZuordnung()] as MassnahmenImportZuordnungUeberpruefung[];
      when(massnahmenImportService.getZuordnungUeberpruefung()).thenReturn(of(zuordnungen));

      sessionSubject.next(defaultSession);

      fixture = TestBed.createComponent(ImportMassnahmenImportUeberpruefenComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should load zuordnungen and update service', () => {
      verify(massnahmenImportZuordnungenService.updateZuordnungen(zuordnungen)).once();
      expect().nothing();
    });

    it('should update table rows on zuordnungen-update', () => {
      // Act
      zuordnungenSubject.next(zuordnungen);

      // Assert
      expect(component.dataSource.data.length).toEqual(1);
      expect(component.dataSource.data[0].zuordnungId).toEqual(zuordnungen[0].id);
      expect(component.dataSource.data[0].massnahmeKonzeptId).toEqual(zuordnungen[0].massnahmeKonzeptId);
      expect(component.dataSource.data[0].netzbezugHinweise).toEqual([zuordnungen[0].netzbezugHinweise[0].text]);
      expect(component.dataSource.data[0].tooltip).toEqual(zuordnungen[0].netzbezugHinweise[0].tooltip);
      expect(component.dataSource.data[0].status).toEqual(zuordnungen[0].status);
      expect(component.dataSource.data[0].hasFehler).toBeFalse();
    });
  });

  describe('with different Zuordnungen', () => {
    let zuordnungen: MassnahmenImportZuordnungUeberpruefung[];

    beforeEach(async () => {
      const baseZuordnung = getDefaultZuordnung();
      zuordnungen = [
        {
          ...baseZuordnung,
          id: 1,
          massnahmeKonzeptId: 'f',
        },
        {
          ...baseZuordnung,
          netzbezugHinweise: [{ tooltip: 'Ein Tooltip', text: 'Ein Text für ID d', severity: 'WARN' }],
          id: 2,
          massnahmeKonzeptId: 'd',
          netzbezug: {
            kantenBezug: [
              {
                geometrie: {
                  coordinates: [
                    [0, 1],
                    [0, 10],
                  ],
                  type: 'LineString',
                },
                kanteId: 1,
                linearReferenzierterAbschnitt: {
                  bis: 1,
                  von: 0,
                },
                kantenSeite: KantenSeitenbezug.BEIDSEITIG,
              },
            ],
            knotenBezug: [],
            punktuellerKantenBezug: [],
          },
        },
        {
          ...baseZuordnung,
          id: 3,
          massnahmeKonzeptId: 'e',
          status: MassnahmenImportZuordnungStatus.GELOESCHT,
        },
        {
          ...baseZuordnung,
          netzbezugHinweise: [{ tooltip: 'Ein Tooltip', text: 'Ein Text für ID b', severity: 'ERROR' }],
          id: 4,
          massnahmeKonzeptId: 'b',
        },
        {
          ...baseZuordnung,
          netzbezugHinweise: [{ tooltip: 'Ein Tooltip', text: 'Ein Text für ID a', severity: 'ERROR' }],
          id: 5,
          massnahmeKonzeptId: 'a',
        },
        {
          ...baseZuordnung,
          netzbezugHinweise: [
            { tooltip: 'Ein Tooltip', text: 'Ein Text für ID c', severity: 'WARN' },
            { tooltip: 'Ein anderer Tooltip', text: 'Ein Text für ID c', severity: 'ERROR' },
            { tooltip: 'Ein Tooltip', text: 'Ein anderer Text für ID c', severity: 'WARN' },
            { tooltip: 'Ein Tooltip', text: 'Dieser Text für ID c ist am andernsten', severity: 'ERROR' },
          ],
          id: 6,
          massnahmeKonzeptId: 'c',
        },
      ] as MassnahmenImportZuordnungUeberpruefung[];
      when(massnahmenImportService.getZuordnungUeberpruefung()).thenReturn(of(zuordnungen));

      sessionSubject.next(defaultSession);

      fixture = TestBed.createComponent(ImportMassnahmenImportUeberpruefenComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      // Act
      zuordnungenSubject.next(zuordnungen);
      fixture.detectChanges();
      component['changeDetectorRef'].detectChanges();
    });

    it('should sort zuordnungen', () => {
      // Assert
      expect(component.dataSource.data.length).toEqual(zuordnungen.length);
      expect(component.dataSource.data[0].massnahmeKonzeptId).toEqual('a');
      expect(component.dataSource.data[1].massnahmeKonzeptId).toEqual('b');
      expect(component.dataSource.data[2].massnahmeKonzeptId).toEqual('c');
      expect(component.dataSource.data[3].massnahmeKonzeptId).toEqual('d');
      expect(component.dataSource.data[4].massnahmeKonzeptId).toEqual('e');
      expect(component.dataSource.data[5].massnahmeKonzeptId).toEqual('f');
    });

    it('should remove duplicate messages', () => {
      // Assert
      expect(component.dataSource.data[2].netzbezugHinweise.length).toEqual(3);
      expect(component.dataSource.data[2].netzbezugHinweise).toContain('Ein Text für ID c');
      expect(component.dataSource.data[2].netzbezugHinweise).toContain('Ein anderer Text für ID c');
      expect(component.dataSource.data[2].netzbezugHinweise).toContain('Dieser Text für ID c ist am andernsten');

      expect(component.dataSource.data[2].tooltip).toEqual('Ein Tooltip\n\nEin anderer Tooltip');
    });

    it('Keine Selektion möglich wenn Netzbezugbearbeiten aktiv', () => {
      // Arrange
      component.netzbezugSelektionAktiv = true;

      // Act
      const rows = fixture.debugElement.queryAll(By.css('tr.hinweis-row'));
      const firstRow = rows[5]; // siehe sortierung oben
      firstRow.triggerEventHandler('click', null);

      component.netzbezugSelektionAktiv = true; // wird sonst von dem Netzbezug FormControl aktiviert

      // Assert
      expect(selektierteZuordnungsIdSubject.value).not.toEqual(zuordnungen[0].id);
      verify(massnahmenImportZuordnungenService.deselektiereZuordnung()).never();
      verify(massnahmenImportZuordnungenService.selektiereZuordnung(anything())).never();
      expect().nothing();
    });

    it('sollte das Editieren von Netzbezügen für zu löschende Maßnahmen nicht ermöglichen', () => {
      // Arrange
      when(massnahmenImportZuordnungenService.selektierteZuordnung).thenReturn(zuordnungen[2]);

      // Act
      const isSelektierteZuordnungEditierbar = component.isSelektierteZuordnungEditierbar;

      // Assert
      expect(isSelektierteZuordnungEditierbar).toBe(false);
    });

    it('sollte das Editieren von Netzbezügen für nicht zu löschende Maßnahmen ermöglichen', () => {
      // Arrange
      when(massnahmenImportZuordnungenService.selektierteZuordnung).thenReturn(zuordnungen[0]);

      // Act
      const isSelektierteZuordnungEditierbar = component.isSelektierteZuordnungEditierbar;

      // Assert
      expect(isSelektierteZuordnungEditierbar).toBe(true);
    });

    describe('with selection and click on save', () => {
      beforeEach(() => {
        // Selektion auf genau zwei Einträge beschränken.
        component.dataSource.data.forEach(zuordnung => {
          zuordnung.selectionControl.setValue(false);
        });
        component.dataSource.data[1].selectionControl.setValue(true);
        component.dataSource.data[3].selectionControl.setValue(true);

        when(dialog.open(anything(), anything())).thenReturn({
          afterClosed: () => of(true),
        } as MatDialogRef<ConfirmationDialogComponent>);

        when(massnahmenImportService.massnahmenSpeichern(anything())).thenReturn(of());

        component.onStart();
      });

      it('should call import service correctly', () => {
        const expectedCommand = { zuordnungenIds: [4, 2] } as MassnahmenImportMassnahmenAuswaehlenCommand;
        verify(massnahmenImportService.massnahmenSpeichern(deepEqual(expectedCommand))).once();
        expect().nothing();
      });
    });

    describe('Selektieren einer Zuordnung über die Tabelle', () => {
      beforeEach(() => {
        // Arrange
        when(massnahmenImportZuordnungenService.selektierteZuordnungsOriginalGeometrie).thenReturn(
          zuordnungen[0].originalGeometrie
        );
        when(massnahmenImportZuordnungenService.selektierterZuordnungsNetzbezug).thenReturn(zuordnungen[0].netzbezug!);

        // Act
        const rows = fixture.debugElement.queryAll(By.css('tr.hinweis-row'));
        const firstRow = rows[5]; // siehe sortierung oben
        firstRow.triggerEventHandler('click', null);

        component.netzbezugSelektionAktiv = true; // wird sonst von dem Netzbezug FormControl aktiviert
      });

      it('sollte durch Klick auf die Tabellenzeile passieren', () => {
        // Assert
        expect(selektierteZuordnungsIdSubject.value).toEqual(zuordnungen[0].id);
        verify(massnahmenImportZuordnungenService.deselektiereZuordnung()).never();
        verify(massnahmenImportZuordnungenService.selektiereZuordnung(zuordnungen[0].id)).once();
        expect().nothing();
      });

      it('sollte den Netzbezug im Formular zurücksetzen', () => {
        // Assert
        expect(component.formGroup.get('netzbezug')?.value).toEqual(zuordnungen[0].netzbezug);
      });

      it('sollte die OriginalGeometrie aktualisieren', () => {
        // Assert
        expect(component.originalGeometrie).toEqual(zuordnungen[0].originalGeometrie);
      });

      describe('stopNetzbezugAuswahl', () => {
        it('Session wurde schon abgebrochen - nichts passiert', () => {
          expect(component.netzbezugSelektionAktiv).toBeTrue();
          component.sessionAbort = true;

          // act
          component.stopNetzbezugAuswahl();

          // Assert
          verify(massnahmenImportService.netzbezugAktualisieren(anything())).never();
          // wir brechen sofort ab und somit wird das auch nicht auf false gesetzt.
          // Sonst würde dieser Test auch durchlaufen, wenn wir uns in dem Fall editorHasNotBeenUsed befinden
          expect(component.netzbezugSelektionAktiv).toBeTrue();
        });

        it('editorHasNotBeenUsed - nichts passiert', () => {
          expect(component.netzbezugSelektionAktiv).toBeTrue();

          // act
          component.stopNetzbezugAuswahl();

          // Assert
          verify(massnahmenImportService.netzbezugAktualisieren(anything())).never();
          expect(component.netzbezugSelektionAktiv).toBeFalse();
          expect().nothing();
        });

        it('sollte den neuen Netzbezug speichern', () => {
          expect(component.netzbezugSelektionAktiv).toBeTrue();

          when(massnahmenImportService.netzbezugAktualisieren(anything())).thenReturn(of());

          // Netzbezug verändern, hier beispielsweise auf den zweiten setzten (muss nur != dem ersten sein)
          component.formGroup.patchValue({ netzbezug: zuordnungen[1].netzbezug });

          // act
          component.stopNetzbezugAuswahl();

          // Assert
          verify(
            massnahmenImportService.netzbezugAktualisieren(
              deepEqual({
                massnahmenImportZuordnungId: zuordnungen[0].id,
                netzbezug: zuordnungen[1].netzbezug,
              })
            )
          ).once();
          verify(massnahmenImportService.getZuordnungUeberpruefung()).once();
          verify(massnahmenImportZuordnungenService.updateZuordnungen(zuordnungen)).once();
          expect(component.netzbezugSelektionAktiv).toBeFalse();
        });
      });
    });
  });
});
