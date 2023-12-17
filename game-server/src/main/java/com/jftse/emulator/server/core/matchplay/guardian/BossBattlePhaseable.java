package com.jftse.emulator.server.core.matchplay.guardian;

import com.jftse.emulator.server.net.FTConnection;
import com.jftse.server.core.matchplay.battle.PlayerBattleState;

public interface BossBattlePhaseable {
    String getPhaseName();
    void start();
    void update(FTConnection connection);
    void end();
    // time in milliseconds
    long phaseTime();
    // time in milliseconds
    long playTime();
    boolean hasEnded();
    void setPhaseCallback(PhaseCallback phaseCallback);
    long getGuardianAttackLoopTime(AdvancedGuardianState guardian);
    int onHeal(int targetGuardian, int healAmount);
    int onDealDamage(int attackingPlayer, int targetGuardian, int damage, boolean hasAttackerDmgBuff, boolean hasTargetDefBuff);
    int onDealDamageToPlayer(int attackingGuardian, int targetPlayer, int damageAmount, boolean hasAttackerDmgBuff, boolean hasTargetDefBuff);
    int onDealDamageOnBallLoss(int attackerPos, int targetPos, boolean hasAttackerWillBuff);
    int onDealDamageOnBallLossToPlayer(int attackerPos, int targetPos, boolean hasAttackerWillBuff);
}