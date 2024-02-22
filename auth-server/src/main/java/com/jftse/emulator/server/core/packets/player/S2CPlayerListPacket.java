package com.jftse.emulator.server.core.packets.player;

import com.jftse.entities.database.model.account.Account;
import com.jftse.entities.database.model.player.ClothEquipment;
import com.jftse.entities.database.model.player.Player;
import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

import java.util.List;

public class S2CPlayerListPacket extends Packet {
    public S2CPlayerListPacket(Account account, List<Player> playerList, int tutorialCount) {
        super(PacketOperations.S2CPlayerList);

        this.write(0);
        this.write(0);
        this.write((byte) tutorialCount);
        this.write(Math.toIntExact(account.getId()));
        this.write(account.getGameMaster()); // GM

        if (playerList != null) {
            this.write((byte) playerList.size());

            for (Player player : playerList) {

                this.write(Math.toIntExact(player.getId()));
                this.write(player.getName());
                this.write(player.getLevel());
                this.write(player.getAlreadyCreated());
                this.write(!player.getFirstPlayer()); // forCharacter delete: true/false
                this.write(player.getGold());
                this.write(player.getPlayerType());
                this.write(player.getStrength());
                this.write(player.getStamina());
                this.write(player.getDexterity());
                this.write(player.getWillpower());
                this.write(player.getStatusPoints());
                this.write(false); // old, "Change Nickname"
                this.write(player.getNameChangeAllowed()); // new, change nickname item/icon

                ClothEquipment clothEquipment = player.getClothEquipment();
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
}
