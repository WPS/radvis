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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { Chart, ChartDataset, ChartOptions, registerables } from 'chart.js';
import { LineString } from 'ol/geom';
import { ColorToCssPipe } from 'src/app/shared/components/color-to-css.pipe';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import invariant from 'tiny-invariant';

Chart.register(...registerables);

@Component({
  selector: 'rad-hoehenprofil',
  templateUrl: './hoehenprofil.component.html',
  styleUrls: ['./hoehenprofil.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class HoehenprofilComponent implements OnInit, OnDestroy, OnChanges {
  @ViewChild('chartCanvas', { static: true })
  chartCanvas: ElementRef | undefined;

  @Input()
  route!: LineString;

  @Output() hoverPositionChange: EventEmitter<number> = new EventEmitter();
  @Output() closed: EventEmitter<void> = new EventEmitter();

  showCanvas = false;
  chart: Chart | null = null;

  distanzen: number[] = [];

  currentCaretX = 0;

  xAxisStart = 0;
  xAxisEnd = 0;
  currentHoehenmeter = 0;
  currentPosition = '';

  lineChartDatasets: ChartDataset[] = [
    {
      data: [],
      borderWidth: 5,
      borderColor: ColorToCssPipe.convertToCss(MapStyles.FEATURE_COLOR),
      backgroundColor: ColorToCssPipe.convertToCss(MapStyles.FEATURE_COLOR_TRANSPARENT),
      fill: 'start',
      spanGaps: true,
      normalized: true,
      parsing: false,
      indexAxis: 'x',
    },
  ];

  lineChartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    parsing: false,
    elements: {
      point: {
        radius: 0,
      },
    },
    layout: {
      padding: {
        right: 20,
        left: 20,
        top: 10,
      },
    },
    scales: {
      x: {
        type: 'linear',
        position: 'top',
        grid: {
          color: 'grey',
        },
        ticks: {
          autoSkip: true,
          minRotation: 0,
          maxRotation: 0,
          maxTicksLimit: 8,
          color: 'grey',
          callback: (value, index, values) => {
            if (values.length - 1 === index || index === 0) {
              return '';
            }
            return this.formatMeter(value as number);
          },
        },
      },
      y: {
        type: 'linear',
        grid: {
          color: 'grey',
        },
        ticks: {
          color: 'grey',
          maxTicksLimit: 6,
          callback: value => {
            return `${Math.round(value as number)} m`;
          },
        },
      },
    },
    hover: {
      intersect: false,
      mode: 'index',
    },
    plugins: {
      tooltip: {
        mode: 'index',
        intersect: false,
        position: 'average',
        enabled: false,
        external: tooltipModel => {
          const tooltip = tooltipModel.tooltip;

          if (tooltip.opacity === 0 || tooltip.dataPoints.length === 0) {
            return;
          }

          const dataPoint = tooltip.dataPoints[0];
          const px = (dataPoint.raw as any).x;
          const py = (dataPoint.raw as any).y;

          this.onHover(px, py);
        },
      },
      legend: {
        display: false,
      },
      decimation: {
        enabled: true,
        algorithm: 'lttb',
        samples: 100,
        threshold: 100,
      },
    },
    events: ['mousemove', 'mouseout'],
  };

  constructor(private changeDetector: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.initChart();
    this.chart?.draw();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.route) {
      invariant(this.route);
      const hoehen = this.route.getCoordinates().map(coor => coor[2]);
      this.distanzen = this.distancesToStart;
      invariant(this.lineChartOptions.scales?.x);
      this.lineChartOptions.scales.x.max = Math.max(...this.distanzen);
      this.lineChartDatasets[0].data = hoehen.map((hoehe, index) => {
        return { x: this.distanzen[index], y: hoehe };
      });
    } else {
      this.lineChartDatasets[0].data = [];
    }

    if (this.chart) {
      this.initChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  onKeyDown(event: KeyboardEvent): void {
    invariant(this.chart);
    invariant(this.chart.tooltip);
    this.chart.tooltip.title = [];
    this.chart.tooltip.beforeBody = [];
    this.chart.tooltip.afterBody = [];
    this.chart.tooltip.body = [];
    this.chart.tooltip.footer = [];

    this.chart.tooltip.opacity = 1;

    if (event.key === 'ArrowRight') {
      this.currentCaretX += 20;
    } else if (event.key === 'ArrowLeft') {
      this.currentCaretX -= 20;
    }

    if (['ArrowRight', 'ArrowLeft'].includes(event.key)) {
      this.currentCaretX = Math.max(this.chart.scales.x.left, Math.min(this.chart.scales.x.right, this.currentCaretX));
      this.chart.tooltip.caretX = this.currentCaretX;
      this.chart.draw();

      const max = this.chart.scales.x.max;
      const relativeCaretPosition =
        (this.currentCaretX - this.chart.scales.x.left) / (this.chart.scales.x.right - this.chart.scales.x.left);
      const relativeDistance = max * relativeCaretPosition;
      const relativeDistances = this.distanzen.map(distance => Math.abs(distance - relativeDistance));
      const index = relativeDistances.indexOf(Math.min(...relativeDistances));
      // this.hoverPosition.emit(relativeCaretPosition * this.route.getLength());
      this.onHover(this.distanzen[index], this.route.getCoordinates()[index][2]);
    }
  }

  private onHover(x: number, y: number): void {
    this.currentHoehenmeter = y;
    this.currentPosition = this.formatMeter(x);
    this.hoverPositionChange.emit(x);
    this.changeDetector.detectChanges();
  }

  private initChart(): void {
    const context = this.chartCanvas?.nativeElement.getContext('2d');
    invariant(context);
    if (this.chart) {
      this.chart.destroy();
    }
    this.chart = new Chart(context, {
      type: 'line',
      data: {
        datasets: this.lineChartDatasets,
      },
      options: this.lineChartOptions,
    });
    const oldDraw = this.chart.draw;
    this.chart.draw = (): void => {
      oldDraw.call(this.chart);

      const chart = this.chart;
      invariant(chart);
      const ctx = chart.ctx;
      if (chart.tooltip && chart.tooltip.opacity > 0) {
        const x = chart.tooltip?.caretX;
        invariant(x);
        invariant(this.chart);
        const topY = this.chart.scales.y.top;
        const bottomY = this.chart.scales.y.bottom;

        // draw line
        ctx.restore();
        ctx.save();
        ctx.beginPath();
        ctx.moveTo(x, topY);
        ctx.lineTo(x, bottomY);
        ctx.lineWidth = 2;
        ctx.strokeStyle = ColorToCssPipe.convertToCss(MapStyles.HOEHENPROFIL_HOVER_COLOR);
        ctx.stroke();
        ctx.closePath();
        ctx.beginPath();
        ctx.ellipse(x, topY, 8, 8, 0, 0, 360);
        ctx.fillStyle = ColorToCssPipe.convertToCss(MapStyles.HOEHENPROFIL_HOVER_COLOR);
        ctx.fill();
        ctx.stroke();
        ctx.restore();
      }
      this.updateAdditionalInfoPosition();
    };

    this.chart?.update();
    this.updateAdditionalInfoPosition();
  }

  private formatMeter(meter: number): string {
    if (meter < 1000) {
      return `${Math.round(meter).toFixed(0)} m`;
    } else {
      return this.meterToKilometerString(meter);
    }
  }

  private meterToKilometerString(meters: number): string {
    const kilometer = meters / 1000;
    if (meters % 1000 === 0) {
      return `${kilometer.toFixed(0)} km`;
    }
    return (
      kilometer.toLocaleString('de', {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
      }) + ' km'
    );
  }

  private get distancesToStart(): number[] {
    const distancesToStart: number[] = [0];
    const coordinates = this.route.getCoordinates();
    let previousCoord = coordinates[0];
    let distance = 0;
    for (let i = 1; i < coordinates.length; ++i) {
      const distanceToPrevious = new LineString([previousCoord, coordinates[i]]).getLength();
      distance += distanceToPrevious;
      distancesToStart.push(distance);
      previousCoord = coordinates[i];
    }
    return distancesToStart;
  }

  private updateAdditionalInfoPosition(): void {
    this.xAxisStart = this.chart?.scales.x.left ?? 0;
    this.xAxisEnd = this.chart?.width ? this.chart?.width - this.chart?.scales.x.right : 0;
    this.changeDetector.detectChanges();
  }
}
