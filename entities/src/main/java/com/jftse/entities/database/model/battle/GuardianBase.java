package com.jftse.entities.database.model.battle;

import com.jftse.entities.database.model.AbstractIdBaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@Getter
@Setter
@MappedSuperclass
public class GuardianBase extends AbstractIdBaseModel {
    private String name;
    private Integer hpBase;
    private Integer hpPer;
    private Integer level;
    private Integer baseStr;
    private Integer baseSta;
    private Integer baseDex;
    private Integer baseWill;
    private Integer addStr;
    private Integer addSta;
    private Integer addDex;
    private Integer addWill;
    private Integer rewardExp;
    private Integer rewardGold;
    private Integer btItemID;
    private Integer guardIndex;

    @Column(columnDefinition = "int DEFAULT 0")
    private Integer rewardRankingPoint = 0;

    private Boolean earth;
    private Boolean wind;
    private Boolean fire;
    private Boolean water;
    private Integer elementGrade;
}
