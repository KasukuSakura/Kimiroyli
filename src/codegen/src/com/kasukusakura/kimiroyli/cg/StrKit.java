package com.kasukusakura.kimiroyli.cg;

public class StrKit {
    public static String spscn(String cnn) {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(cnn.charAt(0)));
        for (var i = 1; i < cnn.length(); i++) {
            char c;
            sb.append(Character.toUpperCase(c = cnn.charAt(i)));
            if (c == '_') continue;
            if (i + 1 == cnn.length()) continue;
            var nc = cnn.charAt(i + 1);
            if (Character.isUpperCase(nc)) {
                sb.append('_');
            }
        }
        return sb.toString();
    }
}
