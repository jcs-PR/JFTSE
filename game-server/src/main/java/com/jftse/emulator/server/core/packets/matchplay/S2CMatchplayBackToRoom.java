package com.jftse.emulator.server.core.packets.matchplay;

import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

public class S2CMatchplayBackToRoom extends Packet {
    public S2CMatchplayBackToRoom() {
        super(PacketOperations.S2CMatchplayBackToRoom);

        this.write((char) 0);
    }
}