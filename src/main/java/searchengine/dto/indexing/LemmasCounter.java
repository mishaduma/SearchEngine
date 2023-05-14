package searchengine.dto.indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LemmasCounter {
    public Map<String, Integer> getLemmas(String text) throws IOException {
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        String[] allWords = text.toLowerCase(Locale.ROOT)
                .replaceAll("\\pP", " ").replace("ё", "е").trim().split("\\s+");

        ArrayList<String> wordBaseForms = new ArrayList<>();

        for (String word : allWords) {
            if (!word.isEmpty() && !word.isBlank() &&
                    !luceneMorphology.getMorphInfo(word).toString().contains("СОЮЗ") &&
                    !luceneMorphology.getMorphInfo(word).toString().contains("МЕЖД") &&
                    !luceneMorphology.getMorphInfo(word).toString().contains("ЧАСТ") &&
                    !luceneMorphology.getMorphInfo(word).toString().contains("ПРЕДЛ")) {
                wordBaseForms.addAll(luceneMorphology.getNormalForms(word));
            }
        }

        return wordBaseForms.stream().collect(Collectors.toMap(Function.identity(), v -> 1, Integer::sum));
    }
}
