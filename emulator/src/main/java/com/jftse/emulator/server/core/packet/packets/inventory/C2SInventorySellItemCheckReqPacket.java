package com.jftse.emulator.server.core.packet.packets.inventory;

import com.jftse.emulator.server.networking.packet.Packet;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class C2SInventorySellItemCheckReqPacket extends Packet {
    private int itemPocketId;

    public C2SInventorySellItemCheckReqPacket(Packet packet) {
        super(packet);

        this.itemPocketId = this.readInt();
    }
}