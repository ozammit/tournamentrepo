package com.rivertech.repositories;

import java.util.logging.Logger;

import com.rivertech.services.LogFactory;

/***
 * Repository that exposes the required methods to load data from staging to the
 * data warehouse.
 * 
 * @author Omar Zammit
 *
 */
public class DatawarehouseRepository extends AbstractRepository {

	final Logger logger = LogFactory.getConsoleHandler("DatawarehouseRepository");

	/***
	 * Constructor that overrides the abstract class.
	 * 
	 * @param url      Database URL
	 * @param username Database username
	 * @param password Database password.
	 */
	public DatawarehouseRepository(String url, String username, String password) {
		super(url, username, password);
	}

	/***
	 * Method to update the dw_status to 1 for staging data. This is used to
	 * identify the rows that need to be processed.
	 */
	public void FlagStagingForProcess() {
		String query = """
					ALTER TABLE Staging_GameRound
					UPDATE dw_status = 1
					WHERE dw_status = 0;
				""";
		logger.fine(query);
		super.ExecuteUpdate(query);
	}

	/***
	 * Method used to truncate all data warehouse tables. Was used during testing.
	 */
	public void TruncateAll() {
		String query = """
				TRUNCATE TABLE DW_FactTable;
				TRUNCATE TABLE DW_GameDimension ;
				TRUNCATE TABLE DW_TimeDimension ;
				TRUNCATE TABLE DW_UserDimension ;
				""";
		logger.fine(query);
		super.ExecuteUpdate(query);
	}

	/***
	 * Method to update the dw_status to 2 for staging data. This is used to
	 * identify the rows that has been processed.
	 */
	public void FlagStagingProcessed() {
		String query = """
				ALTER TABLE Staging_GameRound
				UPDATE dw_status = 1
				WHERE dw_status = 2;
				""";
		logger.fine(query);
		super.ExecuteUpdate(query);
	}

	/***
	 * Update the game dimension.
	 */
	public void UpdateGameDimension() {
		String query = """
				INSERT INTO DW_GameDimension
				SELECT 	DISTINCT
					game_id,
					game_name,
					provider
				FROM
					Staging_GameRound
				WHERE
					dw_status = 1;
								""";
		logger.fine(query);
		super.ExecuteUpdate(query);
		super.ExecuteUpdate("OPTIMIZE TABLE DW_GameDimension");
	}

	/***
	 * Update the time dimension.
	 */
	public void UpdateTimeDimension() {
		String query = """
				INSERT INTO DW_TimeDimension
				SELECT
					generateUUIDv4() AS id,
					timestamp_value,
					DAY(timestamp_value) AS day,
					MONTH(timestamp_value) AS month,
					YEAR(timestamp_value) AS year,
					HOUR(timestamp_value) AS hour 	FROM
				(SELECT
					DISTINCT CAST(created_timestamp AS DATE) AS d_day,
					arrayJoin(arrayMap(x -> toDateTime(x),
					range(toUInt32(toStartOfDay(d_day)),
				  	toUInt32(toStartOfDay(d_day+1)), 3600))) AS timestamp_value
				FROM Staging_GameRound WHERE dw_status = 1);
								""";
		logger.fine(query);
		super.ExecuteUpdate(query);
		super.ExecuteUpdate("OPTIMIZE TABLE DW_TimeDimension");
	}

	/***
	 * Update the user dimension.
	 */
	public void UpdateUserDimension() {
		String query = """
				INSERT INTO DW_UserDimension
				SELECT 	DISTINCT
					user_id
				FROM
					Staging_GameRound
				WHERE dw_status = 1;
				""";
		logger.fine(query);
		super.ExecuteUpdate(query);
		super.ExecuteUpdate("OPTIMIZE TABLE DW_UserDimension");
	}

	/***
	 * Update the fact table data.
	 */
	public void UpdateFactTable() {
		String query = """
				INSERT INTO DW_FactTable
				(		created_timestamp_id,
						user_id,
						game_instance_id,
						game_id,
						real_amount_bet,
						bonus_amount_bet,
						real_amount_win,
						bonus_amount_win )
				SELECT DISTINCT
						dtd.id AS created_timestamp_id,
						user_id,
						game_instance_id,
						game_id,
						real_amount_bet,
						bonus_amount_bet,
						real_amount_win,
						bonus_amount_win
					FROM
						Staging_GameRound AS gr LEFT JOIN
						DW_TimeDimension AS dtd ON toStartOfHour(gr.created_timestamp) = dtd.timestamp_value
					WHERE dw_status = 1;

				""";
		logger.fine(query);
		super.ExecuteUpdate(query);
		super.ExecuteUpdate("OPTIMIZE TABLE DW_FactTable;");
	}
}
