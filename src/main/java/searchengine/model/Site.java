package searchengine.model;

import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @OneToMany
    @JoinColumn(name = "site_id", updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Page> pages;

    @OneToMany
    @JoinColumn(name = "site_id", updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Lemma> lemmas;
}
