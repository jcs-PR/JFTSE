package com.jftse.emulator.server.core.handler.lobby.room;

import com.jftse.emulator.server.core.constants.MiscConstants;
import com.jftse.emulator.server.core.constants.RoomStatus;
import com.jftse.emulator.server.core.matchplay.GameSessionManager;
import com.jftse.emulator.server.core.packets.lobby.room.S2CRoomInformationPacket;
import com.jftse.emulator.server.core.packets.lobby.room.S2CRoomPlayerListInformationPacket;
import com.jftse.emulator.server.core.packets.player.S2CCouplePointsDataPacket;
import com.jftse.emulator.server.core.packets.player.S2CPlayerInfoPlayStatsPacket;
import com.jftse.emulator.server.core.packets.player.S2CPlayerStatusPointChangePacket;
import com.jftse.emulator.server.core.service.impl.ClothEquipmentServiceImpl;
import com.jftse.emulator.server.net.FTClient;
import com.jftse.server.core.handler.AbstractPacketHandler;
import com.jftse.emulator.server.core.manager.ServiceManager;
import com.jftse.emulator.server.core.life.room.Room;
import com.jftse.emulator.server.core.life.room.RoomPlayer;
import com.jftse.server.core.handler.PacketOperationIdentifier;
import com.jftse.server.core.protocol.PacketOperations;
import com.jftse.server.core.protocol.Packet;
import com.jftse.entities.database.model.player.Player;
import com.jftse.entities.database.model.player.PlayerStatistic;
import com.jftse.entities.database.model.player.StatusPointsAddedDto;
import com.jftse.server.core.service.PlayerStatisticService;
import com.jftse.server.core.shared.packets.S2CDisconnectAnswerPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@PacketOperationIdentifier(PacketOperations.C2SMatchplayClientBackInRoom)
public class ClientBackInRoomPacketHandler extends AbstractPacketHandler {
    private final PlayerStatisticService playerStatisticService;
    private final ClothEquipmentServiceImpl clothEquipmentService;

    public ClientBackInRoomPacketHandler() {
        playerStatisticService = ServiceManager.getInstance().getPlayerStatisticService();
        clothEquipmentService = ServiceManager.getInstance().getClothEquipmentService();
    }

    @Override
    public boolean process(Packet packet) {
        return true;
    }

    @Override
    public void handle() {
        FTClient client = (FTClient) connection.getClient();
        if (client == null || client.getPlayer() == null) {
            connection.close();
            return;
        }

        Player player = client.getPlayer();

        Room currentClientRoom = client.getActiveRoom();
        RoomPlayer roomPlayer = client.getRoomPlayer();
        if (currentClientRoom == null || roomPlayer == null) { // shouldn't happen
            connection.close();
            return;
        }

        int roomId = currentClientRoom.getRoomId();
        short position = roomPlayer.getPosition();

        Packet backInRoomAckPacket = new Packet(PacketOperations.S2CMatchplayClientBackInRoomAck);
        backInRoomAckPacket.write(position);
        connection.sendTCP(backInRoomAckPacket);

        Packet unsetHostPacket = new Packet(PacketOperations.S2CUnsetHost);
        unsetHostPacket.write((byte) 0);
        connection.sendTCP(unsetHostPacket);

        roomPlayer.setReady(false);
        roomPlayer.setGameAnimationSkipReady(false);
        roomPlayer.getConnectedToRelay().set(false);

        synchronized (currentClientRoom) {
            currentClientRoom.setStatus(RoomStatus.NotRunning);
        }

        if (GameSessionManager.getInstance().hasMatchplayReward(roomId)) {
            GameSessionManager.getInstance().removeMatchplayReward(roomId);
        }

        PlayerStatistic playerStatistic = playerStatisticService.findPlayerStatisticById(player.getPlayerStatistic().getId());
        player.setPlayerStatistic(playerStatistic);
        client.savePlayer(player);

        StatusPointsAddedDto statusPointsAddedDto = clothEquipmentService.getStatusPointsFromCloths(player);

        S2CCouplePointsDataPacket couplePointsDataPacket = new S2CCouplePointsDataPacket(player.getCouplePoints());
        S2CPlayerStatusPointChangePacket playerStatusPointChangePacket = new S2CPlayerStatusPointChangePacket(player, statusPointsAddedDto);
        S2CPlayerInfoPlayStatsPacket playerInfoPlayStatsPacket = new S2CPlayerInfoPlayStatsPacket(playerStatistic);
        S2CRoomInformationPacket roomInformationPacket = new S2CRoomInformationPacket(currentClientRoom);
        S2CRoomPlayerListInformationPacket roomPlayerInformationPacket = new S2CRoomPlayerListInformationPacket(new ArrayList<>(currentClientRoom.getRoomPlayerList()));

        List<RoomPlayer> filteredRoomPlayerList = currentClientRoom.getRoomPlayerList().stream()
                .filter(x -> x.getPosition() != MiscConstants.InvisibleGmSlot)
                .collect(Collectors.toList());
        S2CRoomPlayerListInformationPacket roomPlayerInformationPacketWithoutInvisibleGm =
                new S2CRoomPlayerListInformationPacket(new ArrayList<>(filteredRoomPlayerList));

        connection.sendTCP(couplePointsDataPacket);
        connection.sendTCP(playerStatusPointChangePacket, playerInfoPlayStatsPacket);
        connection.sendTCP(roomInformationPacket, position == MiscConstants.InvisibleGmSlot ? roomPlayerInformationPacket : roomPlayerInformationPacketWithoutInvisibleGm);
    }
}
