package util;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data;
    
    public ModelView(String view) {
        if (view.contains(".")) {
            this.view = view;
        } else {
            this.view = view + ".jsp";
        }
        this.data = new HashMap<>();
    }
    
    public void addObject(String key, Object value) {
        data.put(key, value);
    }
    
    public String getView() {
        return view;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
}