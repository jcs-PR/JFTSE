package com.jftse.emulator.server.core.packets.messenger;

import com.jftse.entities.database.model.messenger.Parcel;
import com.jftse.server.core.item.EItemCategory;
import com.jftse.server.core.protocol.Packet;
import com.jftse.server.core.protocol.PacketOperations;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S2CReceivedParcelNotificationPacket extends Packet {
    public S2CReceivedParcelNotificationPacket(Parcel parcel) {
        super(PacketOperations.S2CReceivedParcelNotification);

        this.write(parcel.getId().intValue());
        this.write(parcel.getSender().getName());
        this.write(parcel.getCreated());
        this.write(parcel.getMessage());

        EItemCategory category = EItemCategory.valueOf(parcel.getCategory());
        this.write(category.getValue());
        this.write(parcel.getItemIndex());
        this.write((byte) 0); //UNK
        this.write(parcel.getItemCount());
        this.write(parcel.getEParcelType().getValue());
        this.write(parcel.getGold());
    }
}
