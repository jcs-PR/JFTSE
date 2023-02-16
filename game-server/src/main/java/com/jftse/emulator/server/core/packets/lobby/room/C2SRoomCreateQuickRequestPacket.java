package com.jftse.emulator.server.core.packets.lobby.room;

import com.jftse.server.core.protocol.Packet;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class C2SRoomCreateQuickRequestPacket extends Packet {
    private byte allowBattlemon;
    private byte mode;
    private byte players;

    public C2SRoomCreateQuickRequestPacket(Packet packet) {
        super(packet);

        this.allowBattlemon = this.readByte();
        this.mode = this.readByte();
        this.players = this.readByte();
    }
}