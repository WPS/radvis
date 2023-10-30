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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs/operators';
import { FahrradzaehlstelleDetailView } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle-detail-view';
import { FahrradzaehlstelleService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle.service';
import { FormControl } from '@angular/forms';
import { ArtDerAuswertung } from 'src/app/viewer/fahrradzaehlstelle/models/art-der-auswertung';
import { ChannelDetailView } from 'src/app/viewer/fahrradzaehlstelle/models/channel-detail-view';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { Chart, ChartDataset, ChartOptions } from 'chart.js';
import invariant from 'tiny-invariant';
import { ColorToCssPipe } from 'src/app/shared/components/color-to-css.pipe';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';

@Component({
  selector: 'rad-fahrradzaehlstelle-statistik',
  templateUrl: './fahrradzaehlstelle-statistik.component.html',
  styleUrls: ['./fahrradzaehlstelle-statistik.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FahrradzaehlstelleStatistikComponent implements OnInit {
  @ViewChild('chartCanvas', { static: true })
  chartCanvas: ElementRef | undefined;

  fahrradzaehlstelle: FahrradzaehlstelleDetailView | undefined;

  artDerAuswertungOptions = ArtDerAuswertung.options;
  richtung: ChannelDetailView | undefined;
  channelOptions: EnumOption[] = [];
  artDerAuswertungFormControl: FormControl;
  channelFormControl: FormControl;
  rangeFormControl: FormControl;
  jahresAuswahlModus = false;
  chart: Chart | null = null;
  datenVorhanden = false;
  loading = false;
  barChartDatasets: ChartDataset[] = [
    {
      data: [],
      backgroundColor: ColorToCssPipe.convertToCss(MapStyles.FEATURE_COLOR),
      minBarLength: 2,
    },
  ];
  barChartOptions: ChartOptions = {
    layout: {
      padding: {
        right: 20,
        left: 20,
        top: 10,
      },
    },
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      x: {
        type: 'category',
      },
      y: {
        beginAtZero: true,
      },
    },
  };
  durchschnitt = 0;
  gesamtsumme = 0;
  spitzenwert = 0;
  spitze = '';
  private barChartLabels: string[] = [];

  get auswertungsEinheit(): string {
    switch (this.artDerAuswertungFormControl.value) {
      case ArtDerAuswertung.DURCHSCHNITT_PRO_STUNDE:
        return 'Stunde';
      case ArtDerAuswertung.DURCHSCHNITT_PRO_WOCHENTAG:
        return 'Tag';
      case ArtDerAuswertung.DURCHSCHNITT_PRO_MONAT:
        return 'Monat';
      case ArtDerAuswertung.SUMME_PRO_JAHR:
        return 'Jahr';
      default:
        return '';
    }
  }

  constructor(
    private activatedRoute: ActivatedRoute,
    private fahrradzaehlstelleService: FahrradzaehlstelleService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.channelFormControl = new FormControl();
    this.artDerAuswertungFormControl = new FormControl();
    this.rangeFormControl = new FormControl();

    this.artDerAuswertungFormControl.valueChanges.subscribe(value => {
      this.rangeFormControl.reset();
      this.datenVorhanden = false;
      if (value === ArtDerAuswertung.DURCHSCHNITT_PRO_STUNDE || value === ArtDerAuswertung.DURCHSCHNITT_PRO_WOCHENTAG) {
        this.jahresAuswahlModus = false;
      } else {
        this.jahresAuswahlModus = true;
      }
      changeDetectorRef.detectChanges();
    });

    this.rangeFormControl.valueChanges.subscribe(() => this.updateData());
    this.channelFormControl.valueChanges.subscribe(() => this.updateData());
  }

  get eingabeVollstaendig(): boolean {
    return this.zeitraumValid && this.channelFormControl.value && this.artDerAuswertungFormControl.value;
  }

  get zeitraumValid(): boolean {
    return (
      this.zeitraumSelected && this.rangeFormControl.value.end.valueOf() >= this.rangeFormControl.value.start.valueOf()
    );
  }

  get zeitraumSelected(): boolean {
    return this.rangeFormControl.value?.end && this.rangeFormControl.value?.start;
  }

  ngOnInit(): void {
    this.activatedRoute.parent?.data
      .pipe(map(data => data.fahrradzaehlstelleDetailView as FahrradzaehlstelleDetailView))
      .subscribe(fahrradzaehlstelleDetailView => {
        this.fahrradzaehlstelle = fahrradzaehlstelleDetailView;
        this.channelOptions.push({ name: 'ALLE', displayText: 'Alle' });
        this.channelOptions.push(
          ...this.fahrradzaehlstelle?.channels.map(channel => {
            return { name: '' + channel.id, displayText: channel.channelBezeichnung };
          })
        );
        this.channelFormControl.patchValue('ALLE');
        this.changeDetectorRef.detectChanges();
      });
  }

  private initChart(): void {
    const context = this.chartCanvas?.nativeElement.getContext('2d');
    invariant(context);
    if (this.chart) {
      this.chart.destroy();
    }
    this.chart = new Chart(context, {
      type: 'bar',
      data: {
        labels: this.barChartLabels,
        datasets: this.barChartDatasets,
      },
      options: this.barChartOptions,
    });
    this.chart?.update();
  }

  private updateData(): void {
    const range = this.rangeFormControl.value;
    const start = range?.start;
    const end = range?.end;
    const artDerAuswertung = this.artDerAuswertungFormControl.value;
    const channel = this.channelFormControl.value;

    if (this.zeitraumValid && artDerAuswertung && channel) {
      const channelIds = [];
      if (channel === 'ALLE') {
        channelIds.push(this.fahrradzaehlstelle?.channels.map(ch => ch.id));
      } else {
        channelIds.push(channel);
      }
      this.loading = true;
      this.fahrradzaehlstelleService
        .getDataForChannel(channelIds, artDerAuswertung, start, end)
        .then(auswertung => {
          this.datenVorhanden = auswertung.daten.length > 0;
          this.changeDetectorRef.detectChanges();

          this.barChartDatasets[0].data = [];
          this.barChartLabels = [];
          auswertung.daten.forEach(({ zeitlabel, zaehlstand }) => {
            this.barChartDatasets[0].data.push(zaehlstand);
            this.barChartLabels.push(zeitlabel);
          });
          this.gesamtsumme = auswertung.gesamtsumme;
          this.durchschnitt = auswertung.durchschnitt;
          this.spitze = auswertung.spitze;
          this.spitzenwert = auswertung.spitzenwert;
          this.initChart();
          this.chart?.draw();
        })
        .finally(() => {
          this.loading = false;
          this.changeDetectorRef.detectChanges();
        });
    }
  }
}
