package com.jftse.emulator.server.core.handler.messenger;

import com.jftse.emulator.server.core.packets.messenger.C2SSendParcelRequestPacket;
import com.jftse.emulator.server.core.packets.messenger.S2CParcelListPacket;
import com.jftse.emulator.server.core.packets.messenger.S2CReceivedParcelNotificationPacket;
import com.jftse.emulator.server.core.packets.messenger.S2CSendParcelAnswerPacket;
import com.jftse.emulator.server.net.FTClient;
import com.jftse.server.core.handler.AbstractPacketHandler;
import com.jftse.emulator.server.core.manager.GameManager;
import com.jftse.emulator.server.core.manager.ServiceManager;
import com.jftse.server.core.handler.PacketOperationIdentifier;
import com.jftse.server.core.protocol.Packet;
import com.jftse.entities.database.model.item.Product;
import com.jftse.entities.database.model.messenger.EParcelType;
import com.jftse.entities.database.model.messenger.Parcel;
import com.jftse.entities.database.model.player.Player;
import com.jftse.entities.database.model.pocket.PlayerPocket;
import com.jftse.server.core.protocol.PacketOperations;
import com.jftse.server.core.service.ParcelService;
import com.jftse.server.core.service.PlayerPocketService;
import com.jftse.server.core.service.PlayerService;
import com.jftse.server.core.service.ProductService;
import com.jftse.server.core.shared.packets.inventory.S2CInventoryItemRemoveAnswerPacket;

import java.util.List;

@PacketOperationIdentifier(PacketOperations.C2SSendParcelRequest)
public class SendParcelRequestHandler extends AbstractPacketHandler {
    private C2SSendParcelRequestPacket c2SSendParcelRequestPacket;

    private final PlayerService playerService;
    private final ProductService productService;
    private final PlayerPocketService playerPocketService;
    private final ParcelService parcelService;

    public SendParcelRequestHandler() {
        playerService = ServiceManager.getInstance().getPlayerService();
        productService = ServiceManager.getInstance().getProductService();
        playerPocketService = ServiceManager.getInstance().getPlayerPocketService();
        parcelService = ServiceManager.getInstance().getParcelService();
    }

    @Override
    public boolean process(Packet packet) {
        c2SSendParcelRequestPacket = new C2SSendParcelRequestPacket(packet);
        return true;
    }

    @Override
    public void handle() {
        FTClient ftClient = (FTClient) connection.getClient();
        if (ftClient == null)
            return;

        PlayerPocket item = playerPocketService.findById(c2SSendParcelRequestPacket.getPlayerPocketId().longValue());
        if (item == null) {
            S2CSendParcelAnswerPacket s2CSendParcelAnswerPacket = new S2CSendParcelAnswerPacket((short) -1);
            connection.sendTCP(s2CSendParcelAnswerPacket);
            return;
        }

        Product product = productService.findProductByItemAndCategoryAndEnabledIsTrue(item.getItemIndex(), item.getCategory());

        if (product != null && !product.getEnableParcel()) {
            S2CSendParcelAnswerPacket s2CSendParcelAnswerPacket = new S2CSendParcelAnswerPacket((short) -1);
            connection.sendTCP(s2CSendParcelAnswerPacket);
        } else {
            Player sender = ftClient.getPlayer();
            if (sender == null) {
                S2CSendParcelAnswerPacket s2CSendParcelAnswerPacket = new S2CSendParcelAnswerPacket((short) -1);
                connection.sendTCP(s2CSendParcelAnswerPacket);
                return;
            }

            if (sender.getLevel() < 20) {
                S2CSendParcelAnswerPacket s2CSendParcelAnswerPacket = new S2CSendParcelAnswerPacket((short) -4);
                connection.sendTCP(s2CSendParcelAnswerPacket);
            } else {
                Player receiver = playerService.findByName(c2SSendParcelRequestPacket.getReceiverName());
                if (receiver != null) {
                    // TODO: Parcels should have a retention of 7days. -> After 7 days delete parcels and return items back to senders pocket.
                    Parcel parcel = new Parcel();
                    parcel.setReceiver(receiver);
                    parcel.setSender(sender);
                    parcel.setMessage(c2SSendParcelRequestPacket.getMessage());
                    parcel.setGold(c2SSendParcelRequestPacket.getCashOnDelivery());

                    parcel.setItemCount(item.getItemCount());
                    parcel.setCategory(item.getCategory());
                    parcel.setItemIndex(item.getItemIndex());
                    parcel.setUseType(item.getUseType());

                    // TODO: Is this right?
                    if (receiver.getId().equals(sender.getId())) {
                        parcel.setEParcelType(EParcelType.Gold);
                    } else {
                        parcel.setEParcelType(EParcelType.CashOnDelivery);
                    }

                    parcelService.save(parcel);
                    playerPocketService.remove(item.getId());

                    FTClient receiverClient = GameManager.getInstance().getClients().stream()
                            .filter(x -> x.getPlayer() != null && x.getPlayer().getId().equals(receiver.getId()))
                            .findFirst()
                            .orElse(null);
                    if (receiverClient != null) {
                        S2CReceivedParcelNotificationPacket s2CReceivedParcelNotificationPacket = new S2CReceivedParcelNotificationPacket(parcel);
                        receiverClient.getConnection().sendTCP(s2CReceivedParcelNotificationPacket);
                    }

                    // TODO: Handle fee
                    // TODO: Handle all these cases
                    // 0 = Successfully sent
                    //-1 = Failed to send parcel
                    //-2 = You do not have enough gold
                    //-4 = Under level 20 user can not send parcel
                    //-5 = Gold transactions must be under 1.000.000
                    S2CSendParcelAnswerPacket s2CSendParcelAnswerPacket = new S2CSendParcelAnswerPacket((short) 0);
                    connection.sendTCP(s2CSendParcelAnswerPacket);

                    S2CInventoryItemRemoveAnswerPacket s2CInventoryItemRemoveAnswerPacket = new S2CInventoryItemRemoveAnswerPacket(item.getId().intValue());
                    connection.sendTCP(s2CInventoryItemRemoveAnswerPacket);

                    List<Parcel> sentParcels = parcelService.findBySender(parcel.getSender());
                    S2CParcelListPacket s2CSentParcelListPacket = new S2CParcelListPacket((byte) 1, sentParcels);
                    connection.sendTCP(s2CSentParcelListPacket);
                }
            }
        }
    }
}
