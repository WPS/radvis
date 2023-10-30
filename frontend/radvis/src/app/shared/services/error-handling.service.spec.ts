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

import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { instance, mock } from 'ts-mockito';

describe('ErrorHandlingService', () => {
  let errorHandlingService: ErrorHandlingService;

  const notifyUserService = instance(mock(NotifyUserService));

  beforeEach(() => {
    return MockBuilder(ErrorHandlingService, SharedModule).provide({
      provide: NotifyUserService,
      useValue: notifyUserService,
    });
  });

  beforeEach(() => {
    errorHandlingService = TestBed.inject(ErrorHandlingService);
  });

  it('should be created', () => {
    expect(errorHandlingService).toBeTruthy();
  });

  describe('General Errors', () => {
    it('should pass correct user message', () => {
      spyOn(notifyUserService, 'warn');
      spyOn(window.console, 'error').and.callFake(() => {});

      errorHandlingService.handleError(new Error(), 'User Message');

      expect(window.console.error).toHaveBeenCalled();
      expect(notifyUserService.warn).toHaveBeenCalledWith('User Message');
    });
  });

  describe('Http Errors', () => {
    it('should pass a general message for http 500 error code', () => {
      spyOn(notifyUserService, 'warn');

      errorHandlingService.handleHttpError(new HttpErrorResponse({ status: 500 }));

      expect(notifyUserService.warn).toHaveBeenCalledWith(ErrorHandlingService.DEFAULT_SERVER_MESSAGE);
    });

    it('should pass a message for http 400 error code', () => {
      spyOn(notifyUserService, 'warn');

      errorHandlingService.handleHttpError(new HttpErrorResponse({ status: 400, error: { message: 'Hallo Test' } }));

      expect(notifyUserService.warn).toHaveBeenCalledWith('Hallo Test');
    });

    it('should pass a custom message for http errors with status code 0', () => {
      spyOn(notifyUserService, 'warn');

      errorHandlingService.handleHttpError(new HttpErrorResponse({ status: 0 }));

      expect(notifyUserService.warn).toHaveBeenCalledWith('Es ist ein Verbindungsfehler aufgetreten.');
    });
  });
});
