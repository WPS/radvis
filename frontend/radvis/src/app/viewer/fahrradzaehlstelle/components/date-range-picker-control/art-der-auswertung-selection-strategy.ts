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

import { Injectable } from '@angular/core';
import {
  DateRange,
  DefaultMatCalendarRangeStrategy,
  MatDateRangeSelectionStrategy,
} from '@angular/material/datepicker';
import { DateAdapter } from '@angular/material/core';
import { ArtDerAuswertung } from 'src/app/viewer/fahrradzaehlstelle/models/art-der-auswertung';

@Injectable()
export class ArtDerAuswertungSelectionStrategy<D> implements MatDateRangeSelectionStrategy<D> {
  private static strategy: ArtDerAuswertung = ArtDerAuswertung.DURCHSCHNITT_PRO_STUNDE;
  private defaultMatCalendarRangeStrategy: DefaultMatCalendarRangeStrategy<D>;

  constructor(private _dateAdapter: DateAdapter<D>) {
    this.defaultMatCalendarRangeStrategy = new DefaultMatCalendarRangeStrategy<D>(_dateAdapter);
  }

  public static setStrategy(strategy: ArtDerAuswertung): void {
    this.strategy = strategy;
  }

  selectionFinished(date: D, currentRange: DateRange<D>): DateRange<D> {
    switch (ArtDerAuswertungSelectionStrategy.strategy) {
      case ArtDerAuswertung.DURCHSCHNITT_PRO_STUNDE:
        return this.defaultMatCalendarRangeStrategy.selectionFinished(date, currentRange);
      case ArtDerAuswertung.DURCHSCHNITT_PRO_WOCHENTAG: {
        if (
          currentRange &&
          currentRange.start &&
          !currentRange.end &&
          (!date || this._dateAdapter.compareDate(currentRange.start, date) <= 0)
        ) {
          return this.createFullWeekRange(currentRange.start, date);
        }
        return this.defaultMatCalendarRangeStrategy.selectionFinished(date, currentRange);
      }
    }

    return this.defaultMatCalendarRangeStrategy.selectionFinished(date, currentRange);
  }

  createPreview(activeDate: D, currentRange: DateRange<D>): DateRange<D> {
    return this.selectionFinished(activeDate, currentRange);
  }

  private createFullWeekRange(startDate: D, endDate: D): DateRange<D> {
    if (startDate) {
      const day = this._dateAdapter.getDayOfWeek(startDate);
      startDate = this._dateAdapter.addCalendarDays(startDate, -day + 1);
    }
    if (endDate) {
      const dayEnd = this._dateAdapter.getDayOfWeek(endDate);
      endDate = this._dateAdapter.addCalendarDays(endDate, 7 - dayEnd);
    }

    return new DateRange<D>(startDate, endDate);
  }
}
