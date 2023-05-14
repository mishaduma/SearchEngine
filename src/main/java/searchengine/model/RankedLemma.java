package searchengine.model;

import lombok.Data;

@Data
public class RankedLemma {
    private String url;
    private String lemma;
    private float rank;
}
