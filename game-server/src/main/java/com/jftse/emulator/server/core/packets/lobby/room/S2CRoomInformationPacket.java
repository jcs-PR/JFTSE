package com.jftse.emulator.server.core.packets.lobby.room;

import com.jftse.emulator.server.core.life.room.Room;
import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

public class S2CRoomInformationPacket extends Packet {
    public S2CRoomInformationPacket(Room room) {
        super(PacketOperations.S2CRoomInformation.getValue());

        this.write(room.getRoomId());
        this.write(room.getRoomName());
        this.write(room.getAllowBattlemon());
        this.write(room.getMode());
        this.write(room.getRule());
        this.write((byte) 0); // betting mode
        this.write((byte) 0); // betting coins
        this.write(room.getBettingAmount());
        this.write(room.getPlayers());
        this.write(room.isPrivate());
        this.write(room.getLevel());
        this.write(room.getLevelRange());
        this.write((byte) 0); // allow battlemon
        this.write(room.getMap());
        this.write(room.isSkillFree());
        this.write(room.isQuickSlot());
        this.write(room.getBall());
    }
}