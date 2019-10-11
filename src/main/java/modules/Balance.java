package modules;

public class Balance {
    private String currency;
    private double amount;

    public Balance(double amount, String currency) {
        this.currency = currency;
        this.amount = amount;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

}
