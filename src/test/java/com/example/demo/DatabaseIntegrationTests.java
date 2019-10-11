package com.example.demo;

import Util.DatabaseConnection;
import Util.HSBCProvidedUtil;
import modules.Allocation;
import modules.Portfolio;
import modules.Transaction;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import static junit.framework.TestCase.*;

public class DatabaseIntegrationTests {
    private final static String PORTFOLIO = "portfolio";
    private final static String RECOMMENDATIONS = "recommendations";
    private final static String TRANSACTIONS = "transactions";

    @Test
    public void insertAndDeletePortfolioRecord() {
        String portfolioId = "4235234";
        Portfolio portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setDeviation("1");
        portfolio.setType("fund");
        portfolio.setAllocations(prepareAllocationsFor2517972());

        int counts = getTableRowCount(PORTFOLIO);;

        try {
            DatabaseConnection.insertPortfolioRecord(portfolio);
        } catch (Exception e) {
            fail("Please re-run test to make sure it is not data connection issue");
        }

        int newCounts = getTableRowCount(PORTFOLIO);;

        if (counts != -1 && newCounts != -1) {
            assertTrue(newCounts - counts == 1);
        }

        DatabaseConnection.deletePortfolioRecord(portfolioId);

        int countAfterDeletion = getTableRowCount(PORTFOLIO);;

        if (countAfterDeletion != -1 && newCounts != -1) {
            assertTrue(newCounts - countAfterDeletion == 1);
        }

        if (countAfterDeletion == -1 || newCounts == -1 || counts == -1) {
            fail("Please re-run this test to make sure database connection issue won't influence test result");
        }
    }

    private List<Map<String, Integer>> prepareAllocationsFor2517972(){
        List<Map<String, Integer>> allocations = new ArrayList<>();
        Map<String, Integer> allocationmap = new HashMap<>();
        allocationmap.put("fundId", 23456);
        allocationmap.put("percentage", 30);

        Map<String, Integer> allocationmaptwo = new HashMap<>();
        allocationmaptwo.put("fundId", 23457);
        allocationmaptwo.put("percentage", 70);

        allocations.add(allocationmap);
        allocations.add(allocationmaptwo);
        return allocations;
    }

    @Test
    public void deletePortfolioInitialPreference(){
        String portfolioId = "2517972";
        DatabaseConnection.deletePortfolioRecord(portfolioId);
        String portfolioId1 = "9876531";
        DatabaseConnection.deletePortfolioRecord(portfolioId1);
        String portfolioId2 = "56786761";
        DatabaseConnection.deletePortfolioRecord(portfolioId2);
        String portfolioId3 = "579565575";
        DatabaseConnection.deletePortfolioRecord(portfolioId3);
    }

    @Test
    public void doesInitialPreferenceExistTest() {
        String portfolioId = "123456789";
        DatabaseConnection.deletePortfolioRecord(portfolioId);
        assertFalse(DatabaseConnection.doesInitialPreferenceExist(portfolioId));

        Portfolio portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setDeviation("1");
        portfolio.setType("fund");
        portfolio.setAllocations(prepareAllocationsFor2517972());

        try {
            DatabaseConnection.insertPortfolioRecord(portfolio);
            assertTrue(DatabaseConnection.doesInitialPreferenceExist(portfolioId));
        } catch (Exception e) {
            fail("Please re-run test to make sure it is not data connection issue");
        }
    }

    @Test
    public void deleteAndInsertAllocationsTest() {
        String portfolioId = "123456789";
        DatabaseConnection.deletePortfolioRecord(portfolioId);

        List<Map<String, Integer>> allocations = prepareAllocationsFor2517972();
        Portfolio portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setDeviation("1");
        portfolio.setType("fund");
        portfolio.setAllocations(allocations);
        try {
            DatabaseConnection.insertPortfolioRecord(portfolio);
            DatabaseConnection.deleteAllocations(portfolioId);
            Connection conn = DatabaseConnection.setUpConnection();
            String selectQuery = "select * from allocations where portfolio_id = " + portfolioId;
            PreparedStatement preparedStmtAllocation = conn.prepareStatement(selectQuery);
            ResultSet resultSet = preparedStmtAllocation.executeQuery();
            resultSet.last();
            assertTrue(resultSet.getRow() == 0);

            allocations.clear();
            Map<String, Integer> allocationmap = new HashMap<>();
            allocationmap.put("23458", 25);
            allocationmap.put("23459", 75);

            DatabaseConnection.insertAllocations(portfolioId, allocationmap);
            selectQuery = "select * from allocations where portfolio_id = " + portfolioId;
            preparedStmtAllocation = conn.prepareStatement(selectQuery);
            resultSet = preparedStmtAllocation.executeQuery();
            resultSet.last();
            assertTrue(resultSet.getRow() == 2);
            conn.close();
        } catch (Exception e) {
            fail("Please re-run test to make sure it is not data connection issue");
        }
    }

    @Test
    public void insertAndDeleteRecommendationsTest() {
        String portfolioId = "654321";
        String recommendationId = "654321";

        try {
            int count = getTableRowCount(RECOMMENDATIONS);

            // Insert recommendation
            DatabaseConnection.insertRecommendations(recommendationId, portfolioId);
            assertTrue(getTableRowCount(RECOMMENDATIONS) == count + 1);

            // Delete recommendation
            DatabaseConnection.deleteRecommendations(recommendationId);
            assertTrue(getTableRowCount(RECOMMENDATIONS) == count);
        } catch (Exception e) {
            fail("Please re-run test to make sure it is not data connection issue");
        }
    }

    @Test
    public void insertAndDeleteTransactionsTest() {
        String recommendationId = "765432";
        List<Transaction> transactions = new ArrayList<>(Arrays.asList(new Transaction("buy", 23456, 1)));

        try {
            int count = getTableRowCount(TRANSACTIONS);

            // Insert transactions
            DatabaseConnection.insertTransactions(recommendationId, transactions);
            assertTrue(getTableRowCount(TRANSACTIONS) == count + transactions.size());

            // Delete transactions
            DatabaseConnection.deleteTransactions(recommendationId);
            assertTrue(getTableRowCount(TRANSACTIONS) == count);
        } catch (Exception e) {
            fail("Please re-run test to make sure it is not data connection issue");
        }
    }

    @Test
    public void updateTransactionsTest() {
        String recommendationId = "56789";
        List<Transaction> oldTransaction = new ArrayList<>(Arrays.asList(new Transaction("buy", 23456, 1)));
        List<Transaction> newTransaction = new ArrayList<>(Arrays.asList(new Transaction("sell", 23456, 2)));

        try {
            // Insert transaction
            DatabaseConnection.insertTransactions(recommendationId, oldTransaction);

            // Update transaction
            DatabaseConnection.updateTransactions(recommendationId, newTransaction);
            List<Transaction> transactions = DatabaseConnection.getTransactions(recommendationId);
            Transaction currTransaction = transactions.get(0);
            assertTrue(currTransaction.equals(newTransaction.get(0)));

            // Delete transaction
            DatabaseConnection.deleteTransactions(recommendationId);
        } catch (Exception e) {
            fail("Please re-run test to make sure it is not data connection issue");
        }
    }

    private int getTableRowCount(String table) {
        int count = 0;

        try {
            Connection conn = DatabaseConnection.setUpConnection();
            String query = "SELECT COUNT(*) AS counting FROM " + table;
            ResultSet res = conn.createStatement().executeQuery(query);
            while (res.next()) {
                count = res.getInt("counting");
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }
}
