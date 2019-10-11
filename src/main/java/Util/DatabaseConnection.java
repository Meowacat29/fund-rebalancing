package Util;

import modules.Portfolio;
import modules.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DatabaseConnection contains essential functions related to hsbc_db_server database connection and data queries
 *
 * @author  Team Rest
 * @Since   Mar 9, 2019
 *
 */
public class DatabaseConnection {

    private static final String LOCAL_DB_URL = "jdbc:mysql://localhost:3306/hsbc_db_server?useSSL=false";
    private static final String LOCAL_USERNAME = "root";
    private static final String LOCAL_PASSWORD = "password";

    private static final String GOOGLE_DB_URL = "jdbc:mysql://35.222.71.173:3306/hsbc_db_server?useSSL=false";
    private static final String GOOGLE_DB_USERNAME = "stan";
    private static final String GOOGLE_DB_PASSWORD = "teamrest";


    public static Connection setUpConnection() throws SQLException {
            // String myDriver = "com.mysql.jdbc.Driver";
            Connection conn = DriverManager.getConnection(GOOGLE_DB_URL, GOOGLE_DB_USERNAME, GOOGLE_DB_PASSWORD);
            return conn;
    }

    /**
     *  Create a new portfolio data entry in portfolio table and
     *  for allocations of this portfolio, create data entries in allocations table
     *
     *  @param  portfolio the portfolio data is going to be saved into database
     *  @return      the saved portfolio data
     */
    public static Portfolio insertPortfolioRecord(Portfolio portfolio) throws SQLException {

        Connection conn = DatabaseConnection.setUpConnection();

        // mysql insert statement for portfolio table
        String query = " insert into portfolio (id, deviation, type)"
                + " values (?, ?, ?)";

        // create the mysql insert preparedstatement for portfolio
        PreparedStatement preparedStmt = conn.prepareStatement(query);
        preparedStmt.setInt(1, Integer.parseInt(portfolio.getId()));
        preparedStmt.setDouble(2, Double.parseDouble(portfolio.getDeviation()));
        preparedStmt.setString(3, portfolio.getType());
        preparedStmt.execute();

        // create the mysql insert preparedstatement for allocations
        Map<String,Integer> allocationMaps = HSBCProvidedUtil.convertToAllocationMap(portfolio.getAllocations());
        for (String fundId : allocationMaps.keySet()) {
            if (allocationMaps.get(fundId) > 0) {
                String allocationsInsertQuery = " insert into allocations (fund_id, portfolio_id, percentage)"
                        + " values (?, ?, ?)";
                PreparedStatement preparedStmtAllocation = conn.prepareStatement(allocationsInsertQuery);
                preparedStmtAllocation.setInt(1, Integer.parseInt(fundId));
                preparedStmtAllocation.setInt(2, Integer.parseInt(portfolio.getId()));
                preparedStmtAllocation.setInt(3, allocationMaps.get(fundId));
                preparedStmtAllocation.execute();
            }
        }
        conn.close();
        return portfolio;
    }

    /**
     *  Delete a portfolio data entry from portfolio table and
     *  for allocations of this portfolio, delete data entries from allocations table
     *
     *  @param  portfolioId the portfolioId of a portfolio data is going to be deleted from database
     */
    public static void deletePortfolioRecord(String portfolioId){
        try {
            Connection conn = DatabaseConnection.setUpConnection();
            String query = " delete from portfolio WHERE id = ? ;";
            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setInt(1, Integer.parseInt(portfolioId));
            preparedStmt.execute();
            String allocationQuery = " delete from allocations WHERE portfolio_id = ? ;";
            PreparedStatement allocationPreparedStmt = conn.prepareStatement(allocationQuery);
            allocationPreparedStmt.setInt(1, Integer.parseInt(portfolioId));
            allocationPreparedStmt.execute();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Check if an initial preference is created under a portfolio
     *
     *  @param  id the id of the portfolio to check whether has an initial preference
     */
    public static boolean doesInitialPreferenceExist(String id) {
        boolean existed  = false;
        try {
            Connection conn = DatabaseConnection.setUpConnection();

            String selectQuery = "select * from portfolio where id = " + id;
            PreparedStatement preparedStmtAllocation = conn.prepareStatement(selectQuery);
            ResultSet resultSet = preparedStmtAllocation.executeQuery();
            resultSet.last();
            if (resultSet.getRow() == 1) existed = true;
            conn.close();
        } catch (Exception e) {
            System.err.println("Got an exception in doesInitialPreferenceExist!");
            System.err.println(e.getMessage());
        }
        return existed;
    }

    /**
     *  Delete data entries from allocations table
     *
     *  @param  id the allocations udner this portfolio id are going to be deleted from database
     */
    public static void deleteAllocations(String id) {
        try {
            Connection conn = DatabaseConnection.setUpConnection();
            if (conn == null) return;
            String deleteQuery = "delete from allocations where portfolio_id = ?";
            PreparedStatement preparedStmtAllocation = conn.prepareStatement(deleteQuery);
            preparedStmtAllocation.setInt(1, Integer.parseInt(id));
            preparedStmtAllocation.execute();
        } catch (Exception e) {
            System.err.println("Got an exception in deleteAllocations!");
            System.err.println(e.getMessage());
        }
    }

    /**
     *  Insert data entries into allocations table
     *
     *  @param  id the allocations udner this portfolio id are going to be added to database
     */
    public static void insertAllocations(String id, Map<String,Integer> allocations) throws SQLException {
            Connection conn = DatabaseConnection.setUpConnection();
            if (conn == null) return;
            for (String fundId: allocations.keySet()) {
                String insertQuery = "insert into allocations (fund_id, portfolio_id, percentage) values (?, ?, ?)";
                PreparedStatement preparedStmtAllocation = conn.prepareStatement(insertQuery);
                preparedStmtAllocation.setInt(1, Integer.parseInt(fundId));
                preparedStmtAllocation.setInt(2, Integer.parseInt(id));
                preparedStmtAllocation.setInt(3, allocations.get(fundId));
                preparedStmtAllocation.execute();
        }
    }

    /**
     *  Deletes recommendation entries from recommendation table
     *
     *  @param recommendationId the recommendation id to be deleted
     */
    public static void deleteRecommendations(String recommendationId) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();
        String query = "DELETE from recommendations WHERE recommendation_id = " + recommendationId;
        conn.prepareStatement(query).execute();
    }

    /**
     *  Deletes transaction entries from transaction table
     *
     *  @param recommendationId the recommendation id for the transactions to be deleted
     */
    public static void deleteTransactions(String recommendationId) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();
        String query = "DELETE from transactions WHERE recommendation_id = " + recommendationId;
        conn.prepareStatement(query).execute();
    }

    /**
     *  Inserts recommendation entries into recommendation table
     *
     *  @param recommendationId the recommendation id to be inserted
     */
    public static void insertRecommendations(String recommendationId, String portfolioId) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();
        String query = "INSERT INTO recommendations VALUES (" + portfolioId + "," + recommendationId + ")";
        conn.prepareStatement(query).execute();
    }

    /**
     *  Inserts transaction entries into transactions table
     *
     *  @param recommendationId the recommendation id for the transactions to be inserted
     *  @param transactions the list of transactions to be inserted
     */
    public static void insertTransactions(String recommendationId, List<Transaction> transactions) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();
        for (Transaction transaction: transactions) {
            String query = "INSERT INTO transactions VALUES (" +recommendationId + "," + transaction.getFundId() + ",\'" + transaction.getAction() + "\'," + transaction.getUnits() + ")";
            conn.prepareStatement(query).execute();
        }
    }

    /**
     *  Retrieves the list of transaction entries from the transactions table
     *
     *  @param recommendationId the recommendation id for the transactions to be retrieved
     */
    public static List<Transaction> getTransactions(String recommendationId) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();
        PreparedStatement getStatement = conn.prepareStatement("SELECT * from transactions WHERE recommendation_id = " + recommendationId);
        ResultSet res = getStatement.executeQuery();

        List<Transaction> transactions = new ArrayList<>();
        while (res.next()) {
            Transaction transaction = new Transaction();
            transaction.setAction(res.getString("action"));
            transaction.setFundId(res.getInt("fund_id"));
            transaction.setUnits(res.getInt("units"));
            transactions.add(transaction);
        }

        return transactions;
    }

    /**
     *  Retrieves the list of recommendations entries from the recommendations table
     *
     *  @param portfolioId the portfolio id for the recommendations to be retrieved
     */
    public static List<Integer> getRecommendationIds(String portfolioId) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();
        PreparedStatement getStatement = conn.prepareStatement("SELECT recommendation_id from recommendations WHERE portfolio_id = " + portfolioId);
        ResultSet res = getStatement.executeQuery();

        List<Integer> recommendationIds = new ArrayList<>();
        while (res.next()) {
            recommendationIds.add(res.getInt("recommendation_id"));
        }

        return recommendationIds;
    }

    public static Transaction getTransaction(String recommendationId, int fundId) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();
        PreparedStatement getStatement = conn.prepareStatement("SELECT * from transactions WHERE recommendation_id = " + recommendationId + " AND fund_id = " + fundId);
        ResultSet res = getStatement.executeQuery();

        if (res.next()) {
            Transaction transaction = new Transaction();
            transaction.setAction(res.getString("action"));
            transaction.setFundId(res.getInt("fund_id"));
            transaction.setUnits(res.getInt("units"));
            return transaction;
        }

        return null;
    }

    /**
     *  Updates the list of transaction entries from the transactions table
     *
     *  @param recommendationId the recommendation id for the transactions to be updated
     */
    public static void updateTransactions(String recommendationId, List<Transaction> transactions) throws SQLException {
        Connection conn = DatabaseConnection.setUpConnection();

        for (Transaction transaction: transactions) {
            String query = "UPDATE transactions SET action='" + transaction.getAction() + "', units=" + transaction.getUnits() + " WHERE recommendation_id=" + recommendationId + " AND fund_id=" + transaction.getFundId();
            conn.prepareStatement(query).executeUpdate();
        }
    }
}
