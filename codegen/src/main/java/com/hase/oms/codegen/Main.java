package com.hase.oms.codegen;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates Java enums from the OCG-C data dictionary.
 *
 * <p>Usage: {@code Main <fields.yaml> <messages.yaml> <outputDir>}
 *
 * <p>Emits {@code MsgType} plus one value enum per field that declares a
 * numeric/character enum, so the typed constants always match the dictionary
 * (single source of truth).
 */
public final class Main {

    private static final String PKG = "com.hase.oms.codec.enums";

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: Main <fields.yaml> <messages.yaml> <outputDir>");
            System.exit(2);
        }
        Path fieldsPath = Path.of(args[0]);
        Path messagesPath = Path.of(args[1]);
        Path outDir = Path.of(args[2]);

        Yaml yaml = new Yaml();
        Map<String, Object> fieldsDoc = yaml.load(Files.readString(fieldsPath));
        Map<String, Object> messagesDoc = yaml.load(Files.readString(messagesPath));

        Path pkgDir = outDir.resolve(PKG.replace('.', '/'));
        Files.createDirectories(pkgDir);

        emitMsgType(pkgDir, messagesDoc);
        for (Object o : (List<?>) fieldsDoc.get("fields")) {
            Map<?, ?> m = (Map<?, ?>) o;
            Object enums = m.get("enums");
            if (enums instanceof Map<?, ?> em && !em.isEmpty() && isEnumType(String.valueOf(m.get("type")))) {
                emitValueEnum(pkgDir, m, em);
            }
        }
        System.out.println("Generated enums into " + pkgDir);
    }

    /** Only numeric and byte (character) types map cleanly to Java enums. */
    private static boolean isEnumType(String type) {
        return switch (type) {
            case "uint8", "int8", "uint16", "int16", "uint32", "int32", "byte" -> true;
            default -> false;
        };
    }

    private static void emitMsgType(Path pkgDir, Map<String, Object> messagesDoc) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(PKG).append(";\n\n");
        sb.append("/** OCG-C message types (HKEX spec 7.1). */\n");
        sb.append("public enum MsgType {\n");
        List<String> constants = new ArrayList<>();
        for (Object o : (List<?>) messagesDoc.get("messages")) {
            Map<?, ?> m = (Map<?, ?>) o;
            String name = String.valueOf(m.get("name"));
            int type = ((Number) m.get("type")).intValue();
            constants.add("    " + sanitizeIdent(name) + "(" + type + ")");
        }
        sb.append(String.join(",\n", constants)).append(";\n\n");
        sb.append("    private final int code;\n\n");
        sb.append("    MsgType(int code) { this.code = code; }\n\n");
        sb.append("    public int code() { return code; }\n\n");
        sb.append("    public static MsgType fromCode(int code) {\n");
        sb.append("        for (MsgType t : values()) {\n");
        sb.append("            if (t.code == code) return t;\n");
        sb.append("        }\n");
        sb.append("        throw new IllegalArgumentException(\"Unknown MsgType: \" + code);\n");
        sb.append("    }\n");
        sb.append("}\n");
        Files.writeString(pkgDir.resolve("MsgType.java"), sb.toString(), StandardCharsets.UTF_8);
    }

    private static void emitValueEnum(Path pkgDir, Map<?, ?> field, Map<?, ?> enums) throws IOException {
        String fieldName = String.valueOf(field.get("name"));
        boolean isByte = "byte".equals(String.valueOf(field.get("type")));
        String enumName = pascal(fieldName);

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(PKG).append(";\n\n");
        sb.append("/** Values for field {@code ").append(fieldName).append("} (HKEX data dictionary). */\n");
        sb.append("public enum ").append(enumName).append(" {\n");

        Set<String> used = new HashSet<>();
        List<String> constants = new ArrayList<>();
        for (Map.Entry<?, ?> e : enums.entrySet()) {
            String key = String.valueOf(e.getKey());
            String semantic = sanitizeIdent(String.valueOf(e.getValue()));
            String constName = semantic;
            if (!used.add(constName)) {
                constName = semantic + "_" + key; // disambiguate duplicate labels (e.g. reject codes 16 vs 102)
            }
            String code = isByte ? "'" + escapeChar(key) + "'" : key;
            constants.add("    " + constName + "(" + code + ")");
        }
        sb.append(String.join(",\n", constants)).append(";\n\n");

        String codeType = isByte ? "char" : "long";
        sb.append("    private final ").append(codeType).append(" code;\n\n");
        sb.append("    ").append(enumName).append("(").append(codeType).append(" code) { this.code = code; }\n\n");
        sb.append("    public ").append(codeType).append(" code() { return code; }\n");
        sb.append("}\n");
        Files.writeString(pkgDir.resolve(enumName + ".java"), sb.toString(), StandardCharsets.UTF_8);
    }

    private static String pascal(String s) {
        StringBuilder out = new StringBuilder();
        for (String part : s.split("_")) {
            if (!part.isEmpty()) {
                out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return out.toString();
    }

    private static String sanitizeIdent(String s) {
        String cleaned = s.replaceAll("[^A-Za-z0-9]", "_");
        if (cleaned.isEmpty()) {
            cleaned = "VALUE";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "_" + cleaned;
        }
        return cleaned;
    }

    private static String escapeChar(String key) {
        return key.equals("'") ? "\\'" : key;
    }
}
