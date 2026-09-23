package dev.apitestkit.codegen;

import java.util.List;
import java.util.Map;

/**
 * Zet Java-runtime-waarden (String, getallen, booleans, Map, List, null) om naar geldige
 * Java-broncode-expressies, zodat gegenereerde testklassen letterlijke, leesbare testdata bevatten.
 */
class JavaLiteralRenderer {

    String render(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return "\"" + escape(s) + "\"";
        }
        if (value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        if (value instanceof Double d) {
            return d + "d";
        }
        if (value instanceof Boolean b) {
            return b.toString();
        }
        if (value instanceof Map<?, ?> map) {
            return renderMap(map);
        }
        if (value instanceof List<?> list) {
            return renderList(list);
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private String renderMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "Map.of()";
        }
        StringBuilder sb = new StringBuilder("Map.of(");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(render(String.valueOf(entry.getKey()))).append(", ").append(render(entry.getValue()));
            first = false;
        }
        return sb.append(")").toString();
    }

    private String renderList(List<?> list) {
        if (list.isEmpty()) {
            return "List.of()";
        }
        StringBuilder sb = new StringBuilder("List.of(");
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(render(item));
            first = false;
        }
        return sb.append(")").toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
