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

import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { AbstellanlageLayerComponent } from 'src/app/viewer/abstellanlage/components/abstellanlage-layer/abstellanlage-layer.component';
import { AbstellanlageModule } from 'src/app/viewer/abstellanlage/abstellanlage.module';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { defaultAbstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage-testdata-provider.spec';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Icon, Style } from 'ol/style';
import { Color } from 'ol/color';
import { instance, mock, when } from 'ts-mockito';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { AbstellanlageFilterService } from 'src/app/viewer/abstellanlage/services/abstellanlage-filter.service';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { of } from 'rxjs';

describe('AbstellanlageLayerComponent', () => {
  let component: AbstellanlageLayerComponent;
  let fixture: MockedComponentFixture<AbstellanlageLayerComponent>;

  beforeEach(() => {
    const abstellanlageRoutingService = mock(AbstellanlageRoutingService);
    when(abstellanlageRoutingService.selectedInfrastrukturId$).thenReturn(of(null));

    const abstellanlageFilterService = mock(AbstellanlageFilterService);
    when(abstellanlageFilterService.filteredList$).thenReturn(of([]));

    const featureHighlightService = mock(FeatureHighlightService);
    when(featureHighlightService.highlightedFeature$).thenReturn(of());
    when(featureHighlightService.unhighlightedFeature$).thenReturn(of());

    return MockBuilder(AbstellanlageLayerComponent, AbstellanlageModule)
      .provide({ provide: AbstellanlageRoutingService, useValue: instance(abstellanlageRoutingService) })
      .provide({ provide: AbstellanlageFilterService, useValue: instance(abstellanlageFilterService) })
      .provide({ provide: FeatureHighlightService, useValue: instance(featureHighlightService) })
      .provide({ provide: OlMapService, useValue: instance(mock(OlMapComponent)) });
  });

  beforeEach(() => {
    fixture = MockRender(AbstellanlageLayerComponent);
    fixture.detectChanges();
    component = fixture.point.componentInstance;
  });

  it('should style Mobidata differently', () => {
    const abstellanlageRadvis: Abstellanlage = {
      ...defaultAbstellanlage,
      quellSystem: AbstellanlagenQuellSystem.RADVIS,
    };

    const abstellanlageMobiData: Abstellanlage = {
      ...defaultAbstellanlage,
      quellSystem: AbstellanlagenQuellSystem.MOBIDATABW,
    };
    const featureQuelleRadvis = component['convertToFeature'](abstellanlageRadvis)[0];
    const featureQuelleMobidata = component['convertToFeature'](abstellanlageMobiData)[0];

    const styleMobiData = component['styleFn'](featureQuelleMobidata, MapStyles.RESOLUTION_SMALL);
    const styleRadvis = component['styleFn'](featureQuelleRadvis, MapStyles.RESOLUTION_SMALL);

    const mapIconStyleRadvis = findMapIconStyle(styleRadvis);
    expect(mapIconStyleRadvis).toBeDefined();
    expect(getMapIconColor(mapIconStyleRadvis!)).toEqual(MapStyles.INFRASTRUKTUR_ICON_COLOR);

    const mapIconStyleMobiData = findMapIconStyle(styleMobiData);
    expect(mapIconStyleMobiData).toBeDefined();
    expect(getMapIconColor(mapIconStyleMobiData!)).toEqual(MapStyles.MOBIDATA_COLOR);
  });

  function findMapIconStyle(style: Style[] | Style): Style | undefined {
    if (style instanceof Style) {
      style = [style];
    }

    return style.find(it => (it.getImage() as Icon).getSrc() === './assets/map-icon.svg');
  }

  function getMapIconColor(style: Style): Color {
    return (style.getImage() as Icon).getColor();
  }
});
