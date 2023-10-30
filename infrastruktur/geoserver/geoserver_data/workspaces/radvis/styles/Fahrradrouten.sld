<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
                       xmlns="http://www.opengis.net/sld" 
                       xmlns:ogc="http://www.opengis.net/ogc" 
                       xmlns:xlink="http://www.w3.org/1999/xlink" 
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>generic</Name>
    <UserStyle>
      <Title>Generic</Title>
      <Abstract>Generic style</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <MaxScaleDenominator>100000</MaxScaleDenominator>
          <Name>Line</Name>
          <Title>Purple Line</Title>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#7e0045</CssParameter>
              <CssParameter name="stroke-opacity">1</CssParameter>
              <CssParameter name="stroke-width">3</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <MinScaleDenominator>100000</MinScaleDenominator>
          <MaxScaleDenominator>200000</MaxScaleDenominator>
          <Name>Line_far</Name>
          <Title>Purple Line</Title>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#7e0045</CssParameter>
              <CssParameter name="stroke-opacity">1</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <MinScaleDenominator>200000</MinScaleDenominator>
          <Name>Line_far</Name>
          <Title>Purple Line</Title>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#7e0045</CssParameter>
              <CssParameter name="stroke-opacity">1</CssParameter>
              <CssParameter name="stroke-width">1</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>