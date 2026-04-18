package com.grouper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.err.println("Usage: java -jar grouper.jar <input-file>"); System.exit(1); }

        long t0 = System.currentTimeMillis();
        System.out.println("Читаем файл: " + args[0]);

        ArrayList<String> rowList = new ArrayList<>(1 << 20);
        HashSet<String> seen = new HashSet<>(1 << 20);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(args[0]), StandardCharsets.UTF_8), 1 << 17)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || !isValidLine(line)) continue;
                if (seen.add(line)) rowList.add(line);
            }
        }
        seen = null;

        int n = rowList.size();
        System.out.println("Уникальных строк: " + n);
        String[] rows = rowList.toArray(new String[0]);
        rowList = null;


        UnionFind uf = new UnionFind(n);

        String[] colPrefix = new String[16];
        for (int c = 0; c < colPrefix.length; c++) colPrefix[c] = c + ":";
        
        final HashMap<String, Integer> colVal = new HashMap<>(n * 4, 0.5f);
        final StringBuilder sb = new StringBuilder(32);

        for (int i = 0; i < n; i++) {
            String row = rows[i];
            int len = row.length(), col = 0, start = 0;
            for (int p = 0; p <= len; p++) {
                if (p == len || row.charAt(p) == ';') {
                    if (p > start) {

                        int vs = start, ve = p;
                        if (row.charAt(vs) == '"' && row.charAt(ve-1) == '"' && ve-vs >= 2) { vs++; ve--; }
                        if (ve > vs) {
                            String prefix = col < colPrefix.length ? colPrefix[col] : col + ":";

                            sb.setLength(0);
                            sb.append(prefix).append(row, vs, ve);
                            String key = sb.toString();
                            Integer prev = colVal.putIfAbsent(key, i);
                            if (prev != null && prev != i) uf.union(i, prev);
                        }
                    }
                    col++;
                    start = p + 1;
                }
            }
        }


        HashMap<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < n; i++)
            groups.computeIfAbsent(uf.find(i), k -> new ArrayList<>()).add(i);

        List<List<Integer>> sorted = new ArrayList<>(groups.values());
        sorted.sort((a, b) -> Integer.compare(b.size(), a.size()));

        long multi = 0;
        for (List<Integer> g : sorted) if (g.size() > 1) multi++;
        System.out.println("Групп с более чем одним элементом: " + multi);


        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream("result.txt"), StandardCharsets.UTF_8), 1 << 17)) {
            bw.write("Количество групп с более чем одним элементом: " + multi);
            bw.newLine(); bw.newLine();
            int gn = 1;
            for (List<Integer> g : sorted) {
                bw.write("Группа " + gn++); bw.newLine();
                for (int idx : g) { bw.write(rows[idx]); bw.newLine(); }
                bw.newLine();
            }
        }

        System.out.printf("Решение записано в result.txt%nЗатраченное время: %.3f секунд%n",
                (System.currentTimeMillis() - t0) / 1000.0);
    }

    private static boolean isValidLine(String line) {
        if (line.indexOf('"') == -1) return true;
        int len = line.length(), start = 0;
        for (int i = 0; i <= len; i++) {
            if (i == len || line.charAt(i) == ';') {
                if (!isValidToken(line, start, i)) return false;
                start = i + 1;
            }
        }
        return true;
    }

    private static boolean isValidToken(String s, int from, int to) {
        if (to == from) return true;
        boolean sq = s.charAt(from) == '"', eq = s.charAt(to-1) == '"';
        if (!sq && !eq) { for (int i=from;i<to;i++) if(s.charAt(i)=='"') return false; return true; }
        if (sq && eq) { if(to-from<2) return false; for(int i=from+1;i<to-1;i++) if(s.charAt(i)=='"') return false; return true; }
        return false;
    }
}
