package com.jftse.emulator.server.core.packet.packets.inventory;

import com.jftse.emulator.server.networking.packet.Packet;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class C2SInventoryWearSpecialRequestPacket extends Packet {
    private List<Integer> specialSlotList;

    public C2SInventoryWearSpecialRequestPacket(Packet packet) {
        super(packet);

        specialSlotList = new ArrayList<>();

        for (int i = 0; i < 4; ++i)
            specialSlotList.add(this.readInt());
    }
}
