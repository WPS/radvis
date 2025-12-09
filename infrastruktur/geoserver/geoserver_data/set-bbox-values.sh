#!/usr/bin/env bash

# Ersetzt alle BBOX-Einstellungen aller "featuretype.xml" Dateien auf die Werte aus "bbox-values.xml"

for f in $(find -name "featuretype.xml")
do
    echo "$f"
    sed -i "/<nativeBoundingBox>/,/<\/latLonBoundingBox>/c$(sed 's/[\/&]/\\&/' ./bbox-values.xml | sed ':a;N;$!ba;s/\n/\\\n/g')" $f && sed -i "s/^<nativeBoundingBox/  <nativeBoundingBox/" $f
done
