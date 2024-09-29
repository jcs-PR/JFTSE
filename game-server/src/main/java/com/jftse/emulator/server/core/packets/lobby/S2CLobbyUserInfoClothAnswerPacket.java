package com.jftse.emulator.server.core.packets.lobby;

import com.jftse.entities.database.model.player.ClothEquipment;
import com.jftse.entities.database.model.player.Player;
import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

public class S2CLobbyUserInfoClothAnswerPacket extends Packet {
    public S2CLobbyUserInfoClothAnswerPacket(char result, Player player) {
        super(PacketOperations.S2CLobbyUserInfoClothAnswer);

        this.write(result);
        if (player != null) {
            ClothEquipment clothEquipment = player.getClothEquipment();

            this.write(player.getPlayerType());

            this.write(clothEquipment.getHair());
            this.write(clothEquipment.getFace());
            this.write(clothEquipment.getDress());
            this.write(clothEquipment.getPants());
            this.write(clothEquipment.getSocks());
            this.write(clothEquipment.getShoes());
            this.write(clothEquipment.getGloves());
            this.write(clothEquipment.getRacket());
            this.write(clothEquipment.getGlasses());
            this.write(clothEquipment.getBag());
            this.write(clothEquipment.getHat());
            this.write(clothEquipment.getDye());
        }
    }
}