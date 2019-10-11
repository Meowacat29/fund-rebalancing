package modules;

public class HSBCPortfolio {
    private String customerId;
    private String id; //portfolio id
    private Holding[] holdings;

    public HSBCPortfolio() {}

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return this.customerId;
    }

    public void setPortfolioId(String id) {
        this.id = id;
    }

    public String getPortfolioId() {
        return this.id;
    }

    public void setHoldings(Holding[] holdings) {
        this.holdings = holdings;
    }

    public Holding[] getHoldings() {
        return this.holdings;
    }

}
