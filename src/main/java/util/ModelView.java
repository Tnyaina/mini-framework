package util;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data;
    
    public ModelView(String view) {
        this.view = view.endsWith(".jsp") ? view : view + ".jsp";
        this.data = new HashMap<>();
    }
    
    // Chaînage de méthodes
    public ModelView addObject(String key, Object value) {
        data.put(key, value);
        return this;
    }
    
    // Ajouter plusieurs objets
    public ModelView addAllObjects(Map<String, Object> objects) {
        if (objects != null) {
            data.putAll(objects);
        }
        return this;
    }
    
    public String getView() {
        return view;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    // Vérifier l'existence d'une clé
    public boolean hasData(String key) {
        return data.containsKey(key);
    }
    
    // Récupérer une valeur
    public Object getData(String key) {
        return data.get(key);
    }
}