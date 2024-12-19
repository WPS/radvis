/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.common.domain;

import java.util.List;

/**
 * Hilfsklasse um eine Liste Blockweise zu verarbeiten.
 */
public class BatchedCollectionIterator {
	public interface IteratorCallback<T> {
		/**
		 * @param subList Der Batch, der bearbeitet werden soll. Hat maximal die im Iterator angegebene Batch-Size, ggf. kürzer.
		 * @param startIndexInOriginalList Startindex in der originalen Liste.
		 * @param endIndexInOriginalList Endindex in der Originalen Liste. Dieser Index ist exklusiv, das Element mit diesem Index ist also nicht in der subList vorhanden.
		 */
		public void handle(List<T> subList, int startIndexInOriginalList, int endIndexInOriginalList);
	}

	/**
	 * Ruft die angegebene callback Funktion Batchweise auf. Jeder Batch hat genau die Länge der batchSize, außer der
	 * letzte Batch, der ggf. kürzer ist und bis zum letzten Element der Liste reicht.
	 */
	public static <T> void iterate(List<T> list, int batchSize, IteratorCallback<T> callback) {
		int numberOfBatches = list.size() / batchSize + 1;

		for (int i = 0; i < numberOfBatches; i++) {
			int startIndex = i * batchSize;
			int endIndex = (i + 1) * batchSize;
			if (endIndex > list.size()) {
				endIndex = list.size();
			}

			List<T> subList = list.subList(startIndex, endIndex);
			callback.handle(subList, startIndex, endIndex);
		}
	}
}
