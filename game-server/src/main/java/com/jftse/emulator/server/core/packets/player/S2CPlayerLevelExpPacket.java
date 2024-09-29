package com.jftse.emulator.server.core.packets.player;

import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

public class S2CPlayerLevelExpPacket extends Packet {
    public S2CPlayerLevelExpPacket(byte level, int expValue) {
        super(PacketOperations.S2CPlayerLevelExpData);

        this.write(level);
        this.write(expValue);
    }
}
