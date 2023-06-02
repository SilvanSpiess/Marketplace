package me.PSK1103.GUIMarketplaceDirectory.database.entities;

import javax.persistence.*;

@Entity
@Table(name = "owner")
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "name")
    String name;

    @Column(name = "uuid")
    String uuid;
}
