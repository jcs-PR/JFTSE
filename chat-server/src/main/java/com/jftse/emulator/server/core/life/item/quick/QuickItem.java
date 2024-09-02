package com.jftse.emulator.server.core.life.item.quick;

import com.jftse.emulator.server.core.life.item.BaseItem;
import com.jftse.emulator.server.core.manager.ServiceManager;
import com.jftse.emulator.server.core.packets.inventory.S2CInventoryItemCountPacket;
import com.jftse.entities.database.model.player.Player;
import com.jftse.entities.database.model.player.QuickSlotEquipment;
import com.jftse.entities.database.model.pocket.PlayerPocket;
import com.jftse.entities.database.model.pocket.Pocket;
import com.jftse.server.core.item.EItemCategory;
import com.jftse.server.core.service.PlayerPocketService;
import com.jftse.server.core.service.PlayerService;
import com.jftse.server.core.service.PocketService;
import com.jftse.server.core.service.QuickSlotEquipmentService;
import com.jftse.server.core.shared.packets.inventory.S2CInventoryItemRemoveAnswerPacket;

public class QuickItem extends BaseItem {
    private final PocketService pocketService;
    private final PlayerPocketService playerPocketService;
    private final PlayerService playerService;
    private final QuickSlotEquipmentService quickSlotEquipmentService;

    public QuickItem(int itemIndex) {
        super(itemIndex, "QuickItem", EItemCategory.QUICK.getName());

        pocketService = ServiceManager.getInstance().getPocketService();
        playerPocketService = ServiceManager.getInstance().getPlayerPocketService();
        playerService = ServiceManager.getInstance().getPlayerService();
        quickSlotEquipmentService = ServiceManager.getInstance().getQuickSlotEquipmentService();
    }

    @Override
    public boolean processPlayer(Player player) {
        player = playerService.findById(player.getId());
        if (player == null)
            return false;

        this.localPlayerId = player.getId();

        return true;
    }

    @Override
    public boolean processPocket(Pocket pocket) {
        pocket = pocketService.findById(pocket.getId());
        if (pocket == null)
            return false;

        PlayerPocket playerPocket = playerPocketService.getItemAsPocketByItemIndexAndCategoryAndPocket(this.getItemIndex(), this.getCategory(), pocket);
        if (playerPocket == null)
            return false;

        int itemCount = playerPocket.getItemCount() - 1;
        if (itemCount <= 0) {
            playerPocketService.remove(playerPocket.getId());
            pocketService.decrementPocketBelongings(pocket);

            Player player = playerService.findById(this.localPlayerId);
            if (player != null) {
                QuickSlotEquipment quickSlotEquipment = player.getQuickSlotEquipment();
                quickSlotEquipmentService.updateQuickSlots(quickSlotEquipment, Math.toIntExact(playerPocket.getId()));
            }

            S2CInventoryItemRemoveAnswerPacket inventoryItemRemoveAnswerPacket = new S2CInventoryItemRemoveAnswerPacket(Math.toIntExact(playerPocket.getId()));
            this.packetsToSend.add(this.localPlayerId, inventoryItemRemoveAnswerPacket);

        } else {
            playerPocket.setItemCount(itemCount);
            playerPocketService.save(playerPocket);

            S2CInventoryItemCountPacket inventoryItemCountPacket = new S2CInventoryItemCountPacket(playerPocket);
            this.packetsToSend.add(this.localPlayerId, inventoryItemCountPacket);
        }

        return true;
    }
}
