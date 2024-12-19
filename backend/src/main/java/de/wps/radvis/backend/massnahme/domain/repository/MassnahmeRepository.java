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

package de.wps.radvis.backend.massnahme.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;

public interface MassnahmeRepository extends CrudRepository<Massnahme, Long> {

	Optional<Massnahme> findByUmsetzungsstandAndGeloeschtFalse(Umsetzungsstand umsetzungsstand);

	Optional<Massnahme> findByIdAndGeloeschtFalse(Long massnahmeId);

	Stream<Massnahme> findAllByIdInAndGeloeschtFalse(Iterable<Long> massnahmeIds);

	List<Massnahme> findByMassnahmenPaketId(MassnahmenPaketId massnahmenPaketId);

	List<Massnahme> findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(MassnahmeKonzeptID massnahmeKonzeptID,
		Konzeptionsquelle konzeptionsquelle);

	List<Massnahme> findByMassnahmeKonzeptIdAndKonzeptionsquelleAndSollStandardAndGeloeschtFalse(
		MassnahmeKonzeptID massnahmeKonzeptID,
		Konzeptionsquelle konzeptionsquelle, SollStandard sollStandard);

	@Query(
		"SELECT massnahme.massnahmenPaketId FROM Massnahme massnahme " +
			"WHERE massnahme.massnahmenPaketId IS NOT NULL")
	Set<MassnahmenPaketId> findAllMassnahmenPaketIds();

	@Query(
		"SELECT distinct massnahme FROM Massnahme massnahme " +
			"LEFT JOIN massnahme.netzbezug.abschnittsweiserKantenSeitenBezug aksb " +
			"LEFT JOIN massnahme.netzbezug.punktuellerKantenSeitenBezug pksb " +
			"WHERE aksb.kante.id IN :kantenIds OR pksb.kante.id IN :kantenIds")
	List<Massnahme> findByKantenInNetzBezug(Collection<Long> kantenIds);

	@Query(
		"SELECT distinct massnahme FROM Massnahme massnahme " +
			"LEFT JOIN massnahme.netzbezug.knotenBezug kb " +
			"WHERE kb.id IN :knotenIds")
	List<Massnahme> findByKnotenInNetzBezug(List<Long> knotenIds);
}
