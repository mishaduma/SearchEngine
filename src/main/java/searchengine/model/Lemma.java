package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(length = 250, nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;
}
