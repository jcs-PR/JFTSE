package com.jftse.emulator.server.core.packets.inventory;

import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

import java.util.List;

public class S2CInventoryWearBattlemonAnswerPacket extends Packet {
    public S2CInventoryWearBattlemonAnswerPacket(List<Integer> battlemonSlotList) {
        super(PacketOperations.S2CInventoryWearBattlemonAnswer);

        battlemonSlotList.forEach(this::write);
    }
}