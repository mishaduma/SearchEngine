package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "page_id", nullable = false)
    private int pageId;

    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @Column(name = "lemma_rank", nullable = false)
    private float rank;
}
