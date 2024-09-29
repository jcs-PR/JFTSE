package com.jftse.emulator.server.core.packets.messenger;

import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S2CAcceptParcelAnswer extends Packet {
    public S2CAcceptParcelAnswer(short status) {
        super(PacketOperations.S2CAcceptParcelAnswer);

        this.write(status);
    }
}
