package com.jftse.entities.database.model.item;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Getter
@Setter
@Entity
public class ItemEnchant extends Item {
    private String useType;

    private Integer maxUse;

    private String kind;
    private String elementalKind;

    private Integer sellPrice;
}
