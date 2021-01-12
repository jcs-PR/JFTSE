package com.ft.emulator.server.game.core.packet.packets.lobby.room;

import com.ft.emulator.server.networking.packet.Packet;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class C2SRoomLevelRangeChangeRequestPacket extends Packet {
    private byte levelRange;

    public C2SRoomLevelRangeChangeRequestPacket(Packet packet) {
        super(packet);

        this.levelRange = this.readByte();
    }
}