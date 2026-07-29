package br.com.danielchipolesch.domain.util.tiptap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TipTapNode {

    private String type;
    private Map<String, Object> attrs;
    private List<TipTapNode> content;
    private List<TipTapMark> marks;
    private String text;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getAttrs() { return attrs; }
    public void setAttrs(Map<String, Object> attrs) { this.attrs = attrs; }

    public List<TipTapNode> getContent() { return content; }
    public void setContent(List<TipTapNode> content) { this.content = content; }

    public List<TipTapMark> getMarks() { return marks; }
    public void setMarks(List<TipTapMark> marks) { this.marks = marks; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAttrStr(String key) {
        if (attrs == null) return null;
        Object v = attrs.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    public int getAttrInt(String key, int defaultVal) {
        if (attrs == null) return defaultVal;
        Object v = attrs.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v != null) try { return Integer.parseInt(String.valueOf(v)); } catch (NumberFormatException ignored) {}
        return defaultVal;
    }
}
