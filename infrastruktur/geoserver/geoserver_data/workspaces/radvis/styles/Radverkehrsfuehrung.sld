<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" version="1.1.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:ogc="http://www.opengis.net/ogc" xmlns:se="http://www.opengis.net/se" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.1.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <se:Name>Radverkehrsführung</se:Name>
    <UserStyle>
      <se:Name>Radverkehrsführung</se:Name>
      <se:FeatureTypeStyle>
        <se:Rule>
          <se:Name>Führung im Mischverkehr </se:Name>
          <se:Description>
            <se:Title>FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN,FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#a50026</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Markierungstechnische Führungsformen </se:Name>
          <se:Description>
            <se:Title>PIKTOGRAMMKETTE,SCHUTZSTREIFEN,RADFAHRSTREIFEN,RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,MEHRZWECKSTREIFEN
			</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>PIKTOGRAMMKETTE</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>SCHUTZSTREIFEN</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>RADFAHRSTREIFEN</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>MEHRZWECKSTREIFEN</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#d73027</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Führung im Mischverkehr (verkehrsberuhigt) </se:Name>
          <se:Description>
            <se:Title>FUEHRUNG_IN_T20_ZONE
,FUEHRUNG_IN_T30_ZONE
,FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH
</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_IN_T20_ZONE</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_IN_T30_ZONE</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#f46d43</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Führung in Fahrradstraße/-zone</se:Name>
          <se:Description>
            <se:Title>FUEHRUNG_IN_FAHRRADSTRASSE
,FUEHRUNG_IN_FAHRRADZONE
</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_IN_FAHRRADSTRASSE</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_IN_FAHRRADZONE</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#fdae61</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Führung in Einbahnstraßen</se:Name>
          <se:Description>
            <se:Title>EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30,EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#fee090</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Führung in Fußgängerzonen</se:Name>
          <se:Description>
            <se:Title>BEGEGNUNGSZONE
,FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI
,FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI
,FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI
</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>BEGEGNUNGSZONE</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#e6f598</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Führung über Betriebswege</se:Name>
          <se:Description>
            <se:Title>OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER
,BETRIEBSWEG_FORST
,BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG
,BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND
,BETRIEBSWEG_WASSERWIRTSCHAFT
,SONSTIGER_BETRIEBSWEG
</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>BETRIEBSWEG_FORST</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>BETRIEBSWEG_WASSERWIRTSCHAFT</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>SONSTIGER_BETRIEBSWEG</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#66c2a5</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Selbstständig geführte Radwege</se:Name>
          <se:Description>
            <se:Title>SONDERWEG_RADWEG_SELBSTSTAENDIG,GEHWEG_RAD_FREI_SELBSTSTAENDIG
,GEH_RADWEG_GETRENNT_SELBSTSTAENDIG,GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG
,GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG
</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>SONDERWEG_RADWEG_SELBSTSTAENDIG</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>GEHWEG_RAD_FREI_SELBSTSTAENDIG</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>GEH_RADWEG_GETRENNT_SELBSTSTAENDIG</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#3288bd</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Straßenbegleitende Radwege</se:Name>
          <se:Description>
            <se:Title>SONDERWEG_RADWEG_STRASSENBEGLEITEND
,GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND
,GEHWEG_RAD_FREI_STRASSENBEGLEITEND
,GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND
,GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND
</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>SONDERWEG_RADWEG_STRASSENBEGLEITEND</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>GEHWEG_RAD_FREI_STRASSENBEGLEITEND</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND</ogc:Literal>
              </ogc:PropertyIsEqualTo>
			  <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#313695</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
        <se:Rule>
          <se:Name>Sonstige Straße / Weg</se:Name>
          <se:Description>
            <se:Title>SONSTIGE_STRASSE_WEG</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
			<ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>SONSTIGE_STRASSE_WEG</ogc:Literal>
              </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#e0f3f8</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">bevel</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
		<se:Rule>
          <se:Name>Unbekannt</se:Name>
          <se:Description>
            <se:Title>UNBEKANNT</se:Title>
          </se:Description>
          <ogc:Filter xmlns:ogc="http://www.opengis.net/ogc">
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
                <ogc:Literal>UNBEKANNT</ogc:Literal>
              </ogc:PropertyIsEqualTo>
              <ogc:PropertyIsNull>
                <ogc:PropertyName>radverkehrsfuehrung</ogc:PropertyName>
              </ogc:PropertyIsNull>
            </ogc:Or>
          </ogc:Filter>
          <se:LineSymbolizer>
            <se:Stroke>
              <se:SvgParameter name="stroke">#34debc</se:SvgParameter>
              <se:SvgParameter name="stroke-width">4</se:SvgParameter>
              <se:SvgParameter name="stroke-linejoin">bevel</se:SvgParameter>
              <se:SvgParameter name="stroke-linecap">round</se:SvgParameter>
            </se:Stroke>
          </se:LineSymbolizer>
        </se:Rule>
      </se:FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>