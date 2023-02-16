package com.jftse.emulator.server.core.packets.guild;

import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

public class S2CGuildDismissMemberAnswerPacket extends Packet {
    public S2CGuildDismissMemberAnswerPacket(short result) {
        super(PacketOperations.S2CGuildDismissMemberAnswer);

        this.write(result);
    }
}
