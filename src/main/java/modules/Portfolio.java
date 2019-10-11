package modules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolio {
    private String id;
    private String deviation; //TODO: deviation should be double
    private String type;
    private List<Map<String, Integer>> allocations;

    public Portfolio(){
        //
    }

    public Portfolio(String id, String deviation, String type, List<Map<String, Integer>> allocations) {
        this.id = id;
        this.deviation = deviation;
        this.type = type;
        this.allocations = allocations;
    }

    public String getId(){
        return this.id;
    }

    public String getDeviation(){
        return this.deviation;
    }

    public String getType(){
        return this.type;
    }

    public List<Map<String, Integer>> getAllocations(){
        return this.allocations;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setType(String type){
        this.type = type;
    }

    public void setDeviation(String deviation){
        this.deviation = deviation;
    }

    public void setAllocations(List<Map<String, Integer>> allocations) {
        this.allocations = allocations;
    }

    public boolean categoryType() {
        return this.type.equals("category");
    }
}
