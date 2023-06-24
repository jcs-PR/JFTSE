package com.jftse.emulator.server.core.handler.lobby;

import com.jftse.emulator.server.net.FTClient;
import com.jftse.server.core.handler.AbstractPacketHandler;
import com.jftse.emulator.server.core.manager.GameManager;
import com.jftse.server.core.handler.PacketOperationIdentifier;
import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

@PacketOperationIdentifier(PacketOperations.C2SLobbyJoin)
public class LobbyJoinPacketHandler extends AbstractPacketHandler {
    @Override
    public boolean process(Packet packet) {
        return true;
    }

    @Override
    public void handle() {
        FTClient client = (FTClient) connection.getClient();

        while (!client.getIsJoiningOrLeavingLobby().compareAndSet(false, true)) { }

        if (!client.isInLobby()) {
            client.setInLobby(true);
        }
        client.setLobbyCurrentRoomListPage(-1);

        GameManager.getInstance().handleRoomPlayerChanges(client.getConnection(), true);
        GameManager.getInstance().refreshLobbyPlayerListForAllClients();

        System.out.println("LobbyJoinPacketHandler " + client.isInLobby());

        client.getIsJoiningOrLeavingLobby().set(false);
    }
}
