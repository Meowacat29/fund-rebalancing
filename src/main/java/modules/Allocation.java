package modules;

public class Allocation {
    int fundId;
    int percentage;

    public Allocation(int fundId, int percentage){
        this.fundId = fundId;
        this.percentage = percentage;
    }

    public int getFundId() {
        return fundId;
    }

    public void setFundId(int fundId) {
        this.fundId = fundId;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }
}
