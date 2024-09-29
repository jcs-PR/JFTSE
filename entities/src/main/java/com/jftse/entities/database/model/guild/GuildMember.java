package com.jftse.entities.database.model.guild;

import com.jftse.entities.database.model.AbstractBaseModel;
import com.jftse.entities.database.model.player.Player;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Audited
@Entity
public class GuildMember extends AbstractBaseModel {
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH, optional = false)
    @JoinColumn(name = "guild_id", referencedColumnName = "id")
    private Guild guild;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH, optional = false)
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    private Player player;

    private Byte memberRank;
    private Integer contributionPoints = 0;
    private Date requestDate;
    private Boolean waitingForApproval;
}