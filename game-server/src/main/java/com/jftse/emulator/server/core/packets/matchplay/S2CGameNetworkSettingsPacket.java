package com.jftse.emulator.server.core.packets.matchplay;

import com.jftse.emulator.server.core.life.room.Room;
import com.jftse.emulator.server.net.FTClient;
import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

import java.util.List;

public class S2CGameNetworkSettingsPacket extends Packet {
    public S2CGameNetworkSettingsPacket(String host, int port, int gameSessionId, Room room, List<FTClient> clientsInRoom) {
        super(PacketOperations.S2CGameNetworkSettings);

        this.write(host);
        this.write((char) port);

        this.write(gameSessionId);

        int clientsInRoomSize = clientsInRoom.size();
        int maxClientsInRoom = 4;
        int missingClientsCount = maxClientsInRoom - clientsInRoomSize;

        clientsInRoom.forEach(c -> {
            if (c.getPlayer() != null)
                this.write(Math.toIntExact(c.getPlayer().getId()));
        });

        for (int i = 1; i <= missingClientsCount; i++) {
            this.write(0);
        }
    }
}
