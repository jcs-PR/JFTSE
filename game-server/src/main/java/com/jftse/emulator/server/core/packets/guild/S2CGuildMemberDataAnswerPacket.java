package com.jftse.emulator.server.core.packets.guild;

import com.jftse.entities.database.model.guild.GuildMember;
import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

import java.util.List;

public class S2CGuildMemberDataAnswerPacket extends Packet {
    public S2CGuildMemberDataAnswerPacket(List<GuildMember> guildMemberList) {
        super(PacketOperations.S2CGuildMemberDataAnswer);

        this.write((short) 0);
        this.write((byte) guildMemberList.size());

        for (int i = 0; i < guildMemberList.size(); i++) {
            GuildMember guildMember = guildMemberList.get(i);
            
            this.write(i + 1);
            this.write(guildMember.getPlayer().getId().intValue());
            this.write(guildMember.getMemberRank());
            this.write(guildMember.getPlayer().getLevel());
            this.write(guildMember.getPlayer().getPlayerType());
            this.write(guildMember.getPlayer().getName());
            this.write(guildMember.getContributionPoints());
            this.write((short) 0); // Unknown
        }
    }
}
