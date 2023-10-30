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

package de.wps.radvis.backend.abfrage.fehlerprotokoll.domain;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import de.wps.radvis.backend.common.domain.SetToStringAttributeConverter;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ManuellerImportFehlerursache;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.SneakyThrows;

public class ManuellerImportFehlerLiquibaseMigration implements CustomTaskChange {

	@SneakyThrows
	@Override
	public void execute(Database database) throws CustomChangeException {
		JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();

		Statement selectStatement = databaseConnection.createStatement();
		selectStatement.execute("SELECT m.import_typ, m.id, m.kante_id, m.fehlerursache, m.import_zeitpunkt, "
			+ "CASE "
			+ "	WHEN m.kante_id IS NOT NULL THEN st_astext((SELECT knot.point "
			+ "										 FROM kante kant "
			+ "										JOIN knoten knot ON knot.id = kant.von_knoten_id "
			+ "										WHERE kant.id = m.kante_id)) "
			+ "	ELSE st_astext(st_centroid(m.original_geometrie)) "
			+ "END AS icon_position, "
			+ "st_astext(original_geometrie) AS original_geometry "
			+ "FROM manueller_import_fehler m");
		ResultSet resultSet = selectStatement.getResultSet();
		SetToStringAttributeConverter setToStringAttributeConverter = new SetToStringAttributeConverter();

		while (resultSet.next()) {
			long id = resultSet.getLong("id");
			ManuellerImportFehlerursache fehlerursache = ManuellerImportFehlerursache.valueOf(
				resultSet.getString("fehlerursache"));
			ImportTyp importTyp = ImportTyp.valueOf(resultSet.getString("import_typ"));
			long kanteId = resultSet.getLong("kante_id");

			Set<Konflikt> konflikte = new HashSet<>();

			if (fehlerursache.equals(ManuellerImportFehlerursache.ATTRIBUTE_NICHT_EINDEUTIG)) {
				Statement selectKonflikteStatement = databaseConnection.createStatement();
				selectKonflikteStatement.execute(
					"SELECT * FROM manueller_import_fehler_konflikt WHERE manueller_import_fehler_id = " + id);
				ResultSet resultSetKonflikte = selectKonflikteStatement.getResultSet();
				while (resultSetKonflikte.next()) {
					String attributName = resultSetKonflikte.getString("attribut_name");
					String uebernommenerWert = resultSetKonflikte.getString("uebernommener_wert");
					String nichtUebernommeneWerte = resultSetKonflikte.getString("nicht_uebernommene_werte");
					konflikte.add(new Konflikt(attributName, uebernommenerWert,
						setToStringAttributeConverter.convertToEntityAttribute(nichtUebernommeneWerte)));
				}
			}
			String beschreibung = ManuellerImportFehler.generateBeschreibung(konflikte, fehlerursache);
			String titel = ManuellerImportFehler.generateTitel(importTyp);
			String entityLink = ManuellerImportFehler.generateEntityLink(kanteId != 0 ? kanteId : null);
			String iconPosition = resultSet.getString("icon_position");

			String sql;
			if (iconPosition != null) {
				sql = "UPDATE manueller_import_fehler "
					+ "SET "
					+ "icon_position = st_geomfromtext(?), "
					+ "titel = ?, "
					+ "beschreibung = ?, "
					+ "entity_link = ? "
					+ "WHERE id = ?";
			} else {
				sql = "UPDATE manueller_import_fehler "
					+ "SET "
					+ "icon_position = ?, "
					+ "titel = ?, "
					+ "beschreibung = ?, "
					+ "entity_link = ? "
					+ "WHERE id = ?";
			}

			PreparedStatement ps = databaseConnection.prepareStatement(sql);
			ps.setString(1, iconPosition);
			ps.setString(2, titel);
			ps.setString(3, beschreibung);
			ps.setString(4, entityLink);
			ps.setLong(5, id);
			ps.executeUpdate();
			ps.close();
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "manueller_import_fehler migrated - generated missing values for icon_position,titel,beschreibung,entity_link";
	}

	@Override
	public void setUp() throws SetupException {

	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {

	}

	@Override
	public ValidationErrors validate(Database database) {
		return null;
	}
}
