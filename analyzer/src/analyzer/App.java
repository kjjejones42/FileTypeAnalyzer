package analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class SearchPattern {
    SearchPattern(String priority, String pattern, String name){
        this.priority = Integer.parseInt(priority);
        this.pattern = pattern;
        this.name = name;
        this.length = pattern.length();
    }
    public int priority; 
    public String pattern;
    public String name;
    public long hash = 0;
    public int length;
}

public class App {

    public static List<SearchPattern> patterns;
    
    static long mod = 1_000_000_000 + 9;
    static int pow = 53;
    enum ST {KMP, RK}
    static ST SEARCHTYPE = ST.RK;

    public static int[] KMPPrefixFunction(String str) {
        int[] prefixFunc = new int[str.length()];
        for (int i = 1; i < str.length(); i++) {
            int j = prefixFunc[i - 1];
            while (j > 0 && str.charAt(i) != str.charAt(j)) {
                j = prefixFunc[j - 1];
            }
            if (str.charAt(i) == str.charAt(j)) {
                j += 1;
            }
            prefixFunc[i] = j;
        }
        return prefixFunc;
    }

    public static boolean KMPLineSearch(String text, SearchPattern patternObj) {
        String pattern = patternObj.pattern;
        int[] prefixFunc = KMPPrefixFunction(pattern);
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = prefixFunc[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) {
                j += 1;
            }
            if (j == pattern.length()) {
                return true;
            }
        }
        return false;
    }    

    public static boolean RKLineSearch(String fileContents, long[] prefixes, SearchPattern pattern){
        final int length = fileContents.length();
              
        long substringHash = prefixes[pattern.length];
        long patternHash = pattern.hash;
        long mult = 1;
        for (int i = 0; i <= length - pattern.length; i++){
            substringHash = (prefixes[i + pattern.length] - prefixes[i] + mod) % mod;
            patternHash = (pattern.hash * mult) % mod;
            if (patternHash == substringHash){
                for (int j = 0; j < pattern.length; j++){
                    char a = pattern.pattern.charAt(j);
                    char b = fileContents.charAt(i + j);
                    if (a != b){
                        break;
                    }
                }
                return true;                
            }            
            mult = (mult * pow) % mod;
        }
        return false;
    }
    
    private static long[] getPrefixHashes(String input) {
        long[] result = new long[input.length() + 1];
        result[0] = 0L;

        long hash = 0;
        long tempPow = 1;
        for (int i = 0; i < input.length(); i++) {
            hash += charToLong(input.charAt(i)) * tempPow;
            hash %= mod;
            result[i + 1] = hash;
            tempPow = (tempPow * pow) % mod;
        }
        return result;
    }

    public static String checkFileType(File file){
        String fileName = file.getName();
        try {
            String fileContents = new String(Files.readAllBytes(file.toPath()));
            switch (App.SEARCHTYPE){
                case KMP: {                    
                    for (SearchPattern pattern : patterns){
                        boolean contains = KMPLineSearch(fileContents, pattern);
                        if (contains) {
                            return fileName + ": " + pattern.name;
                        }
                    }
                    break;
                }
                case RK: {
                    long[] RKPrefixes = getPrefixHashes(fileContents);                    
                    for (SearchPattern pattern : patterns){
                        boolean contains = RKLineSearch(fileContents, RKPrefixes, pattern);
                        if (contains) {
                            return fileName + ": " + pattern.name;
                        }
                    }
                    break;
                }
            }
        } catch (Exception err){
            err.printStackTrace();
        }        
        return fileName + ": " +"Unknown file type";
    }

    public static void loadPatternsFromPath(String directory){
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(directory)));

            List<SearchPattern> patterns = new ArrayList<>();
            String line;
            while((line = br.readLine()) != null){
                String[] p = line.replace("\"","").split(";");
                SearchPattern pattern = new SearchPattern(p[0],p[1],p[2]);
                pattern.hash = getRKHash(pattern.pattern);
                patterns.add(pattern);
            }
            br.close();

            patterns.sort(Comparator.comparingInt(i -> i.priority));
            Collections.reverse(patterns);

            App.patterns = patterns;

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static long charToLong(char ch) {
        return ch + 1;
    }

    public static long getRKHash(String input){
        long hash = 0;
        long tempPow = 1;
        for (int i = 0; i < input.length(); i++) {
            hash += charToLong(input.charAt(i)) * tempPow;
            hash %= mod;
            tempPow = (tempPow * pow) % mod;
        }
        return hash % mod;
    }

    public static void main(String[] args) {
        if (args.length < 2){
            return;
        }
        String patternDatabasePath = args[0];
        String directory = args[1];

        loadPatternsFromPath(patternDatabasePath);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<String>> futures = new ArrayList<>();

        File folder = new File(directory);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    Future<String> future = executorService.submit(() -> checkFileType(file));
                    futures.add(future);
                }
            }
            for (Future<String> future : futures) {
                try {
                    System.out.println(future.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        executorService.shutdown();
    }
}