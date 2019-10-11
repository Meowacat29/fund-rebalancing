package modules;

import java.util.Map;

public class FundInfo {
    private int fundId;
    private String fundName;
    private int category;
    private Map<String, Double> averageReturns;
    private Price price;

    public FundInfo() {}

    public void setAverageReturns(Map<String, Double> averageReturns) {
        this.averageReturns = averageReturns;
    }

    public double getAverageScore() {
        double sum = 0;
        for (Map.Entry<String, Double> score : this.averageReturns.entrySet()) {
            sum += score.getValue();
        }

        return sum / this.averageReturns.size();
    }

    public int getFundId() {
        return this.fundId;
    }

    public String getFundName() {
        return this.fundName;
    }

    public Price getPrice() {
        return this.price;
    }

    public Map<String, Double> getAverageReturns() {
        return this.averageReturns;
    }

    public int getCategory() {
        return this.category;
    }
}
