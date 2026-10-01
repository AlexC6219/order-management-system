package com.hase.oms.codec;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The OCG-C data dictionary: fields and per-message presence-map bit positions.
 * Loaded from {@code fields.yaml} / {@code messages.yaml} (single source of truth).
 */
public final class Dictionary {

    public record FieldDef(String name, FieldType type, int size, int maxLength, boolean multi,
                           Map<String, String> enums) {}

    public record FieldEntry(int bit, String field, boolean required) {}

    public record GroupDef(String countField, int bit, String presenceMapField, List<FieldEntry> fields) {}

    public record MessageDef(String name, int type, String sender, List<FieldEntry> fields,
                             List<GroupDef> groups) {}

    public static final int HEADER_BYTES = 54;   // STX + Length + MsgType + Seq + PossDup + PossResend + CompId(12) + PresenceMap(32)
    public static final int PRESENCE_MAP_BYTES = 32;
    public static final int TRAILER_BYTES = 4;    // checksum

    private final Map<String, FieldDef> fields;
    private final Map<Integer, MessageDef> messages;

    private Dictionary(Map<String, FieldDef> fields, Map<Integer, MessageDef> messages) {
        this.fields = fields;
        this.messages = messages;
    }

    public FieldDef field(String name) {
        FieldDef def = fields.get(name);
        if (def == null) {
            throw new IllegalArgumentException("Unknown field: " + name);
        }
        return def;
    }

    public MessageDef message(int type) {
        MessageDef def = messages.get(type);
        if (def == null) {
            throw new IllegalArgumentException("Unknown message type: " + type);
        }
        return def;
    }

    public static Dictionary load(InputStream fieldsYaml, InputStream messagesYaml) {
        Yaml yaml = new Yaml();

        Map<String, Object> fieldsDoc = yaml.load(fieldsYaml);
        Map<String, Object> messagesDoc = yaml.load(messagesYaml);

        Map<String, FieldDef> fieldMap = new LinkedHashMap<>();
        for (Object o : (List<?>) fieldsDoc.get("fields")) {
            Map<?, ?> m = (Map<?, ?>) o;
            String name = str(m.get("name"));
            FieldType type = FieldType.fromString(str(m.get("type")));
            int size = m.get("size") == null ? 0 : ((Number) m.get("size")).intValue();
            int maxLength = m.get("maxLength") == null ? 0 : ((Number) m.get("maxLength")).intValue();
            boolean multi = Boolean.TRUE.equals(m.get("multi"));
            Map<String, String> enums = new LinkedHashMap<>();
            Object e = m.get("enums");
            if (e instanceof Map<?, ?> em) {
                for (Map.Entry<?, ?> en : em.entrySet()) {
                    enums.put(String.valueOf(en.getKey()), String.valueOf(en.getValue()));
                }
            }
            fieldMap.put(name, new FieldDef(name, type, size, maxLength, multi, enums));
        }

        Map<Integer, MessageDef> messageMap = new LinkedHashMap<>();
        for (Object o : (List<?>) messagesDoc.get("messages")) {
            Map<?, ?> m = (Map<?, ?>) o;
            String name = str(m.get("name"));
            int type = ((Number) m.get("type")).intValue();
            String sender = str(m.get("sender"));
            List<FieldEntry> entries = new ArrayList<>();
            Object f = m.get("fields");
            if (f instanceof List<?> fl) {
                for (Object eo : fl) {
                    Map<?, ?> em = (Map<?, ?>) eo;
                    entries.add(new FieldEntry(
                            ((Number) em.get("bit")).intValue(),
                            str(em.get("field")),
                            Boolean.TRUE.equals(em.get("required"))));
                }
            }
            List<GroupDef> groups = new ArrayList<>();
            Object g = m.get("groups");
            if (g instanceof List<?> gl) {
                for (Object go : gl) {
                    Map<?, ?> gm = (Map<?, ?>) go;
                    List<FieldEntry> gFields = new ArrayList<>();
                    for (Object geo : (List<?>) gm.get("fields")) {
                        Map<?, ?> gem = (Map<?, ?>) geo;
                        gFields.add(new FieldEntry(
                                ((Number) gem.get("bit")).intValue(),
                                str(gem.get("field")),
                                Boolean.TRUE.equals(gem.get("required"))));
                    }
                    groups.add(new GroupDef(
                            str(gm.get("countField")),
                            ((Number) gm.get("bit")).intValue(),
                            str(gm.get("presenceMapField")),
                            gFields));
                }
            }
            messageMap.put(type, new MessageDef(name, type, sender, entries, groups));
        }

        return new Dictionary(fieldMap, messageMap);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
