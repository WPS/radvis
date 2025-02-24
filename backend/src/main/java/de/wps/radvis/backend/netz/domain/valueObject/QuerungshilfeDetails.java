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

package de.wps.radvis.backend.netz.domain.valueObject;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum QuerungshilfeDetails {
	KEINE_VORHANDEN("Keine baulichen Querungshilfen (mit / ohne Zebra), keine Furt",
		List.of(
			KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
			KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE,
			KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
			KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE)),
	VORHANDEN_OHNE_FURT("Bauliche Querungshilfen vorhanden, aber ohne Furt", List.of(
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE)),
	VORHANDEN_MIT_FURT_ZEBRA("Bauliche Querungshilfen und Zebra + Furten vorhanden", List.of(
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE)),
	VORHANDEN_MIT_FURT("Bauliche Querungshilfen und Furten vorhanden", List.of(
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE)),
	VORHANDEN_OHNE_FURT_ZEBRA("Bauliche Querungshilfen und Zebra vorhanden (ohne Furt)", List.of(
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE)),
	UNBEKANNT("Unbekannt", List.of(
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
		KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE, KnotenForm.MITTELINSEL_EINFACH,
		KnotenForm.MITTELINSEL_GETEILT)),
	MITTELINSEL_OK("Mittelinsel in Ordnung (Aufstellfläche = 4 x 2,5 m & keine Umwege)",
		List.of(KnotenForm.MITTELINSEL_EINFACH)),
	AUFSTELLFLAECHE_ZU_SCHMAL("Aufstellfläche zu schmal (< 4 m und/oder < 2,5 m)",
		List.of(KnotenForm.MITTELINSEL_EINFACH)),
	UMWEGE("Mittelinsel nur über Umwege zu nutzen",
		List.of(KnotenForm.MITTELINSEL_EINFACH)),
	AUFSTELLFLAECHE_ZU_SCHMAL_UMWEGE("Aufstellfläche zu schmal und Mittelinsel zu umwegig",
		List.of(KnotenForm.MITTELINSEL_EINFACH)),
	ANDERE_ANMERKUNG_MITTELINSEL("Andere Anmerkung zu Mittelinsel(n)",
		List.of(KnotenForm.MITTELINSEL_EINFACH, KnotenForm.MITTELINSEL_GETEILT)),
	AUFSTELLBEREICH_OK("Aufstellbereich ist mindestens 2,5 m breit", List.of(KnotenForm.MITTELINSEL_GETEILT)),
	AUFSTELLBEREICH_ZU_SCHMAL("Aufstellbereich zu schmal", List.of(KnotenForm.MITTELINSEL_GETEILT)),
	AUSTELLFLAECHE_SCHWER_ERKENNBAR(
		"Aufstellfläche zwischen den Mittelinseln schwer erkennbar (z.B. keine Markierung)",
		List.of(KnotenForm.MITTELINSEL_GETEILT)),
	AUFSTELLFLAECHE_ZU_SCHMAL_SCHWER_ERKENNBAR(
		"Aufstellfläche zu schmal und zwischen den Mittelinseln schwer erkennbar",
		List.of(KnotenForm.MITTELINSEL_GETEILT)),
		;

	@NonNull
	private final String displayText;

	private final List<KnotenForm> validKnotenformen;

	@Override
	public String toString() {
		return this.displayText;
	}

	public boolean isValidForKnotenform(KnotenForm knotenForm) {
		return validKnotenformen.contains(knotenForm);
	}

	public static boolean isRequiredForKnotenform(KnotenForm knotenForm) {
		return List.of(
			KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
			KnotenForm.GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE,
			KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE,
			KnotenForm.KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE, KnotenForm.MITTELINSEL_EINFACH,
			KnotenForm.MITTELINSEL_GETEILT).contains(knotenForm);
	}
}
