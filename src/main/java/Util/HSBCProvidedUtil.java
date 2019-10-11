package Util;

import com.google.gson.Gson;
import modules.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * HSBCProvidedUtil contains API calls to the endpoints provided by HSBC sponsors
 *
 * @author  Team Rest
 * @Since   Mar 9, 2019
 *
 */

public class HSBCProvidedUtil {

    private final static String ID_KEY = "id";
    private final static String TRANSACTION_ID = "transactionId";
    private final static String HOLDINGS = "holdings";
    private final static String TRANSACTIONS = "transactions";
    private final static String FUND_ID = "fundId";
    private final static String UNITS = "units";
    private final static String BALANCE= "balance";
    private final static String AMOUNT = "amount";
    private final static String STATUS = "status";
    private final static String ACTION = "action";

    private final static String CUSTOMER_ID_KEY = "x-custid";
    private final static String PORTFOLIOS_URL = "https://us-central1-useful-memory-229303.cloudfunctions.net/portfolios2";
    private final static String TRANSACTIONS_URL = "https://us-central1-useful-memory-229303.cloudfunctions.net/transaction2";
    private final static String FUND_URL = "https://us-central1-useful-memory-229303.cloudfunctions.net/fund2";
    private final static String FUNDS_URL = "https://us-central1-useful-memory-229303.cloudfunctions.net/funds2";


    public HSBCProvidedUtil(){
        //
    }

    /**
     * Return true if headers contain customer-id field, otherwise false
     *
     *  @param headers the headers of a request
     */
    public static boolean isValidHeader(HttpHeaders headers){
        if(headers == null || !headers.containsKey(CUSTOMER_ID_KEY) || headers.get(CUSTOMER_ID_KEY).size() == 0 ||
                headers.get(CUSTOMER_ID_KEY).get(0).equals("")){
            return false;
        }
        return true;
    }

    public static String getCustomerId(HttpHeaders headers) {
        return headers.get(CUSTOMER_ID_KEY).get(0);
    }

    /**
     * Get the portfolios for a given customer
     *
     * @param custId the customer id of which we're retrieving the portfolios for
     *
     * @return the portfolios for the customer
     *
     */
    public static HSBCPortfolio[] getPortfolios(String custId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Holding> holdings = new HashMap<>();
        ResponseEntity<String> response;
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-custid", custId);
        HttpEntity entity = new HttpEntity(headers);
        response = restTemplate.exchange(PORTFOLIOS_URL,
                HttpMethod.GET, entity, String.class);
        Gson gson = new Gson();

        //parse response and get customer's holdings
        HSBCPortfolio[] portfolios = gson.fromJson(response.getBody(), HSBCPortfolio[].class);

        return portfolios;
    }


    /**
     * Get the holdings of a portfolio for a customer
     *
     * @param headers the headers of a request that contains customer id field
     * @param id the portfolio id of holdings we are looking for
     *
     * @return the holdings of the portfolio
     *
     */
    public static Holding[] getHoldings(HttpHeaders headers, String id) throws Exception{
        //retrieve the current customer's holdings
        //send request to HSBC mock-up system. GET /portfolios
        HSBCPortfolio[] portfolios = getPortfolios(headers.get(CUSTOMER_ID_KEY).get(0));
        if (portfolios != null && portfolios.length != 0){
            for(int i=0; i< portfolios.length; i++){
                if(portfolios[i].getPortfolioId().equals(id)){
                    return portfolios[i].getHoldings();
                }
            }
        }
        return null;
    }

    /**
     * Conduct the transactions by calling HSBC transaction API endpoint
     *
     * @param headers the headers of a request that contains customer id field
     * @param id the portfolio id that we are going to conduct the transactions
     * @param transactions the transactions we are going to conduct on this portfolio
     *
     * @return the transactions we conducted
     *
     */
    public static Map<Integer, List<Transaction>> setTransactions(HttpHeaders headers, String id, List<Transaction> transactions) {

        Map<Integer, List<Transaction>> result = new HashMap<>();
        // Send POST request
        RestTemplate restTemplate = new RestTemplate();
        Gson gson = new Gson();
        Instructions ins = new Instructions(Integer.valueOf(id), transactions);
        String instructionsJson = gson.toJson(ins);
        HttpEntity<String> entity = new HttpEntity<>(instructionsJson, headers);
        ResponseEntity<String> response = restTemplate.exchange(TRANSACTIONS_URL, HttpMethod.POST, entity, String.class);

        // Parse response
        try{
            JSONObject json = new JSONObject(response.getBody());
            // Check status
            int status = json.getInt(STATUS);
            if (status != 0) {
                return null;
            }

            // Populate returned transactions
            int transactionId = json.getInt(TRANSACTION_ID);
            List<Transaction> executedTrans = new ArrayList<>();
            JSONArray arr = json.getJSONArray(TRANSACTIONS);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Transaction transaction = new Transaction();
                transaction.setFundId(obj.getInt(FUND_ID));
                transaction.setAction(obj.getString(ACTION));
                transaction.setUnits(obj.getInt(UNITS));
                executedTrans.add(transaction);
            }

            result.put(transactionId, executedTrans);
        }catch (Exception e){
            //
        }
        return result;
    }

    /**
     * Get the holding amounts from holdings
     *
     * @param holdings the holdings we are going to extract amounts from
     *
     * @return the holding amounts extracted
     *
     */
    public static HashMap<String, Holding> getHoldingAmount(Holding[] holdings){
        HashMap<String,Holding> holdingsMap = new HashMap<>();
        if(holdings == null || holdings.length == 0){
            return null;
        }
        for(int i=0; i<holdings.length; i++){
            holdingsMap.put(Integer.valueOf(holdings[i].getFundId()).toString(), holdings[i]);
        }
        return holdingsMap;
    }


    /**
     * Convert an allocations from a List into a HashMap data structure
     *
     * @param allocations the allocations to be converted
     *
     * @return allocations converted
     *
     */
    public static Map<String, Integer> convertToAllocationMap(List<Map<String, Integer>> allocations){
        Map<String, Integer> result = new HashMap<>();
        for (Map<String, Integer> map : allocations) {
            if (map.get("percentage") > 0 ) {
                int key = map.containsKey("category") ? map.get("category") : map.get("fundId");
                result.put(Integer.toString(key), map.get("percentage"));
            }
        }
        return result;
    }

    /**
     * Get the holding fundId from holdings
     *
     * @param holdings the holdings we are going to extract fundIds from
     *
     * @return the fundIds extracted from the holdings
     *
     */
    public static HashSet<Integer> getFundIdsFromHoldings(Holding[] holdings){
        HashSet<Integer> fundIds = new HashSet<>();
        if(holdings == null || holdings.length == 0){
            return null;
        }
        for(int i=0; i<holdings.length; i++){
            fundIds.add(holdings[i].getFundId());
        }
        return fundIds;
    }

    public static FundInfo getFundInfo(String customerId, int fundId) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response;
        HttpHeaders headers = new HttpHeaders();
        headers.set(CUSTOMER_ID_KEY, customerId);
        HttpEntity entity = new HttpEntity(headers);
        response = restTemplate.exchange(FUND_URL + "/" + fundId,
                HttpMethod.GET, entity, String.class);

        String json = response.getBody();
        Gson gson = new Gson();
        FundInfo fundInfo = gson.fromJson(json, FundInfo.class);

        return fundInfo;
    }

    public static FundInfo[] getFundsInfo(String customerId, int category) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response;
        HttpHeaders headers = new HttpHeaders();
        headers.set(CUSTOMER_ID_KEY, customerId);
        HttpEntity entity = new HttpEntity(headers);
        String url = FUNDS_URL;
        if (category != -1) {
            url += "?category=" + category;
        }
        response = restTemplate.exchange(url,
                HttpMethod.GET, entity, String.class);

        String json = response.getBody();
        Gson gson = new Gson();
        FundInfo[] fundInfo = gson.fromJson(json, FundInfo[].class);

        fundInfo = Arrays.stream(fundInfo).filter(info -> info.getCategory() == category).toArray(FundInfo[]::new);

        return fundInfo;
    }
}
