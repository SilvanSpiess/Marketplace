package me.PSK1103.GUIMarketplaceDirectory.database.entities;

import javax.persistence.*;
import java.util.Map;

@Entity
@Table(name = "shop")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "name")
    String name;

    @Column(name = "desc")
    String desc;

    private String loc;

    private String owner;

    @Column(name = "display_item")
    private String displayItem = "WRITTEN_BOOK";
}
