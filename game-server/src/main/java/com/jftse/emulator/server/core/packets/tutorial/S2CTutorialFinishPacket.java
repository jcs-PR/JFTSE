package com.jftse.emulator.server.core.packets.tutorial;

import com.jftse.server.core.item.EItemCategory;
import com.jftse.server.core.item.EItemUseType;
import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class S2CTutorialFinishPacket extends Packet {
    public S2CTutorialFinishPacket(boolean win, byte newLevel, int exp, int gold, int secondsNeeded, List<Map<String, Object>> rewardItemList) {
        super(PacketOperations.S2CTutorialEnd);

        this.write(win, newLevel, exp, gold, secondsNeeded);

        this.write((char) rewardItemList.size());
        for (Map<String, Object> rewardItem : rewardItemList) {

            long id = (long) rewardItem.get("id");
            String category = (String) rewardItem.get("category");
            int itemIndex = (int) rewardItem.get("itemIndex");
            String useType = (String) rewardItem.get("useType");
            int itemCount = (int) rewardItem.get("itemCount");
            Date created = (Date) rewardItem.get("created");

            this.write((int) id);
            this.write(EItemCategory.valueOf(category).getValue());
            this.write(itemIndex);
            this.write(useType.equals("N/A") ? (byte) 0 : EItemUseType.valueOf(useType.toUpperCase()).getValue());
            this.write(itemCount);

            long timeLeft = (created.getTime() * 10000) - (new Date().getTime() * 10000);
            this.write(timeLeft);

            this.write((byte) 0); // enchant str
            this.write((byte) 0); // enchant sta
            this.write((byte) 0); // enchant dex
            this.write((byte) 0); // enchant wil
            // ??
            this.write((byte) 0);
            this.write((byte) 0);
        }
    }
}
