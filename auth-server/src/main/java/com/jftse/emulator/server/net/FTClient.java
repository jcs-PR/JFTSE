package com.jftse.emulator.server.net;

import com.jftse.emulator.server.core.manager.ServiceManager;
import com.jftse.entities.database.model.account.Account;
import com.jftse.entities.database.model.player.Player;
import com.jftse.server.core.net.Client;

public class FTClient extends Client<FTConnection> {
    private Long accountId;
    private Long activePlayerId;

    public void setPlayer(Long id) {
        this.activePlayerId = id;
    }

    public void setAccount(Long id) {
        this.accountId = id;
    }

    public Player getPlayer() {
        if (this.activePlayerId == null)
            return null;
        return ServiceManager.getInstance().getPlayerService().findById(activePlayerId);
    }

    public void savePlayer(Player player) {
        ServiceManager.getInstance().getPlayerService().save(player);
    }

    public Account getAccount() {
        if (this.accountId == null && this.activePlayerId != null) {
            final Player player = getPlayer();
            return ServiceManager.getInstance().getAuthenticationService().findAccountById(player.getAccount().getId());
        }
        if (this.accountId == null)
            return null;
        return ServiceManager.getInstance().getAuthenticationService().findAccountById(this.accountId);
    }

    public void saveAccount(Account account) {
        ServiceManager.getInstance().getAuthenticationService().updateAccount(account);
    }
}