package br.com.danielchipolesch.domain.util.tiptap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TipTapMark {

    private String type;
    private Map<String, Object> attrs;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getAttrs() { return attrs; }
    public void setAttrs(Map<String, Object> attrs) { this.attrs = attrs; }

    public String getAttr(String key) {
        if (attrs == null) return null;
        Object v = attrs.get(key);
        return v != null ? String.valueOf(v) : null;
    }
}
