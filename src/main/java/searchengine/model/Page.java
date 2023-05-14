package searchengine.model;

import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(indexes = {@Index(columnList = "path")})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(length = 250, nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "mediumtext", nullable = false)
    private String content;

    @OneToMany
    @JoinColumn(name = "page_id", updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<SearchIndex> searchIndices;
}
