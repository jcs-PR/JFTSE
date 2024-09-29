package com.jftse.emulator.server.core.packets.messenger;

import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S2CSendParcelAnswerPacket extends Packet {
    public S2CSendParcelAnswerPacket(short status) {
        super(PacketOperations.S2CSendParcelAnswer);

        this.write(status);
    }
}
