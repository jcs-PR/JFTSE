package com.jftse.emulator.server.game.core.game.handler.matchplay;

import com.jftse.emulator.server.database.model.battle.*;
import com.jftse.emulator.server.database.model.player.Player;
import com.jftse.emulator.server.database.model.player.QuickSlotEquipment;
import com.jftse.emulator.server.database.model.player.StatusPointsAddedDto;
import com.jftse.emulator.server.database.model.pocket.PlayerPocket;
import com.jftse.emulator.server.game.core.constants.GameFieldSide;
import com.jftse.emulator.server.game.core.constants.PacketEventType;
import com.jftse.emulator.server.game.core.constants.RoomStatus;
import com.jftse.emulator.server.game.core.matchplay.PlayerReward;
import com.jftse.emulator.server.game.core.matchplay.basic.MatchplayGuardianGame;
import com.jftse.emulator.server.game.core.matchplay.battle.GuardianBattleState;
import com.jftse.emulator.server.game.core.matchplay.battle.PlayerBattleState;
import com.jftse.emulator.server.game.core.matchplay.battle.SkillCrystal;
import com.jftse.emulator.server.game.core.matchplay.battle.SkillDrop;
import com.jftse.emulator.server.game.core.matchplay.event.PacketEventHandler;
import com.jftse.emulator.server.game.core.matchplay.event.RunnableEvent;
import com.jftse.emulator.server.game.core.matchplay.event.RunnableEventHandler;
import com.jftse.emulator.server.game.core.matchplay.room.GameSession;
import com.jftse.emulator.server.game.core.matchplay.room.Room;
import com.jftse.emulator.server.game.core.matchplay.room.RoomPlayer;
import com.jftse.emulator.server.game.core.packet.packets.lobby.room.S2CRoomMapChangeAnswerPacket;
import com.jftse.emulator.server.game.core.packet.packets.lobby.room.S2CRoomSetBossGuardiansStats;
import com.jftse.emulator.server.game.core.packet.packets.lobby.room.S2CRoomSetGuardianStats;
import com.jftse.emulator.server.game.core.packet.packets.lobby.room.S2CRoomSetGuardians;
import com.jftse.emulator.server.game.core.packet.packets.matchplay.*;
import com.jftse.emulator.server.game.core.service.*;
import com.jftse.emulator.server.game.core.utils.BattleUtils;
import com.jftse.emulator.server.networking.Connection;
import com.jftse.emulator.server.networking.packet.Packet;
import com.jftse.emulator.server.shared.module.Client;
import com.jftse.emulator.server.shared.module.GameHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class GuardianModeHandler {
    private final static long crystalDefaultRespawnTime = TimeUnit.SECONDS.toMillis(5);
    private final static long crystalDefaultDespawnTime = TimeUnit.SECONDS.toMillis(5);

    private final PacketEventHandler packetEventHandler;
    private final RunnableEventHandler runnableEventHandler;
    private final SkillService skillService;
    private final SkillDropRateService skillDropRateService;
    private final GuardianService guardianService;
    private final BossGuardianService bossGuardianService;
    private final GuardianStageService guardianStageService;
    private final PlayerPocketService playerPocketService;
    private final PocketService pocketService;
    private final LevelService levelService;
    private final ClothEquipmentService clothEquipmentService;

    private GameHandler gameHandler;

    public void init(GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    public void handleGuardianModeMatchplayPointPacket(Connection connection, C2SMatchplayPointPacket matchplayPointPacket, GameSession gameSession, MatchplayGuardianGame game) {
        boolean lastGuardianServeWasOnGuardianSide = game.getLastGuardianServeSide() == GameFieldSide.Guardian;
        if (!lastGuardianServeWasOnGuardianSide) {
            game.setLastGuardianServeSide(GameFieldSide.Guardian);
            S2CMatchplayTriggerGuardianServe triggerGuardianServePacket = new S2CMatchplayTriggerGuardianServe(GameFieldSide.Guardian, (byte) 0, (byte) 0);
            gameSession.getClients().forEach(x -> {
                x.getConnection().sendTCP(triggerGuardianServePacket);
            });
        } else {
            game.setLastGuardianServeSide(GameFieldSide.Players);
            S2CMatchplayTriggerGuardianServe triggerGuardianServePacket = new S2CMatchplayTriggerGuardianServe(GameFieldSide.Players, (byte) 0, (byte) 0);
            gameSession.getClients().forEach(x -> {
                x.getConnection().sendTCP(triggerGuardianServePacket);
            });
        }
    }

    public void handleStartGuardianMode(Connection connection, Room room) {
        GameSession gameSession = connection.getClient().getActiveGameSession();
        MatchplayGuardianGame game = (MatchplayGuardianGame) gameSession.getActiveMatchplayGame();
        game.setLastGuardianServeSide(GameFieldSide.Guardian);
        List<Client> clients = this.gameHandler.getClientsInRoom(room.getRoomId());
        S2CMatchplayTriggerGuardianServe triggerGuardianServePacket = new S2CMatchplayTriggerGuardianServe(GameFieldSide.Guardian, (byte) 0, (byte) 0);
        clients.forEach(c -> {
            packetEventHandler.push(packetEventHandler.createPacketEvent(c, triggerGuardianServePacket, PacketEventType.FIRE_DELAYED, 8), PacketEventHandler.ServerClient.SERVER);
        });

        this.placeCrystalRandomly(connection, game);
        this.triggerGuardianAttackLoop(connection);
    }

    private void triggerGuardianAttackLoop(Connection connection) {
        GameSession gameSession = connection.getClient().getActiveGameSession();
        if (gameSession == null) return;

        List<Integer> guardianSkills = Arrays.asList(2, 3, 5, 6, 7, 10, 11, 12);
        byte randIndex = (byte) (Math.random() * guardianSkills.size());
        byte skillId = guardianSkills.get(randIndex).byteValue();
        Point2D point = new Point2D.Float();

        // Crab, Small inferno, Big inferno
        if (skillId == 11 || skillId == 27 || skillId == 35) point = this.getRandomPoint();

        Point2D finalPoint = point;
        RunnableEvent runnableEvent = runnableEventHandler.createRunnableEvent(() -> {
            S2CMatchplayUseSkill packet = new S2CMatchplayUseSkill((byte) 10, (byte) 0, skillId, finalPoint);
            this.sendPacketToAllClientsInSameGameSession(packet, connection);
            this.triggerGuardianAttackLoop(connection);
        }, TimeUnit.SECONDS.toMillis(7));
        gameSession.getRunnableEvents().add(runnableEvent);
    }

    public void handlePrepareGuardianMode(Connection connection, Room room) {
        GameSession gameSession = connection.getClient().getActiveGameSession();
        MatchplayGuardianGame game = (MatchplayGuardianGame) gameSession.getActiveMatchplayGame();

        List<RoomPlayer> roomPlayers = room.getRoomPlayerList();

        float averagePlayerLevel = this.getAveragePlayerLevel(roomPlayers);
        this.handleMonsLavaMap(connection, room, averagePlayerLevel);

        GuardianStage guardianStage = this.guardianStageService.getGuardianStages().stream()
                .filter(x -> x.getMapId() == room.getMap() && !x.getIsBossStage())
                .findFirst()
                .orElse(null);
        game.setGuardianStage(guardianStage);

        GuardianStage bossGuardianStage = this.guardianStageService.getGuardianStages().stream()
                .filter(x -> x.getMapId() == room.getMap() && x.getIsBossStage())
                .findFirst()
                .orElse(null);
        game.setBossGuardianStage(bossGuardianStage);

        int guardianLevelLimit = this.getGuardianLevelLimit(averagePlayerLevel);
        game.setGuardianLevelLimit(guardianLevelLimit);

        List<PlayerBattleState> playerBattleStates = roomPlayers.stream()
                .filter(x -> x.getPosition() < 4)
                .map(rp -> this.createPlayerBattleState(rp))
                .collect(Collectors.toList());
        game.setPlayerBattleStates(playerBattleStates);

        int activePlayingPlayersCount = (int) roomPlayers.stream().filter(x -> x.getPosition() < 4).count();
        byte guardianStartPosition = 10;
        List<Byte> guardians = this.determineGuardians(game.getGuardianStage(), game.getGuardianLevelLimit());
        for (int i = 0; i < guardians.stream().count(); i++) {
            int guardianId = guardians.get(i);
            if (guardianId == 0) continue;

            short guardianPosition = (short) (i + guardianStartPosition);
            Guardian guardian = guardianService.findGuardianById((long) guardianId);
            GuardianBattleState guardianBattleState = this.createGuardianBattleState(guardian, guardianPosition, activePlayingPlayersCount);
            game.getGuardianBattleStates().add(guardianBattleState);
        }

        S2CRoomSetGuardians roomSetGuardians = new S2CRoomSetGuardians(guardians.get(0), guardians.get(1), guardians.get(2));
        S2CRoomSetGuardianStats roomSetGuardianStats = new S2CRoomSetGuardianStats(game.getGuardianBattleStates());
        List<Client> clients = this.gameHandler.getClientsInRoom(room.getRoomId());
        clients.forEach(c -> {
            c.getConnection().sendTCP(roomSetGuardians);
            c.getConnection().sendTCP(roomSetGuardianStats);
        });
    }

    public void handlePlayerPickingUpCrystal(Connection connection, C2SMatchplayPlayerPicksUpCrystal playerPicksUpCrystalPacket) {
        RoomPlayer roomPlayer = this.getRoomPlayerFromConnection(connection);
        short playerPosition = roomPlayer.getPosition();
        GameSession gameSession = connection.getClient().getActiveGameSession();
        MatchplayGuardianGame game = (MatchplayGuardianGame) gameSession.getActiveMatchplayGame();
        SkillCrystal skillCrystal = game.getSkillCrystals().stream()
                .filter(x -> x.getId() == playerPicksUpCrystalPacket.getCrystalId())
                .findFirst()
                .orElse(null);

        if (skillCrystal != null) {
            if (gameSession == null) return;
            S2CMatchplayLetCrystalDisappear letCrystalDisappearPacket = new S2CMatchplayLetCrystalDisappear(skillCrystal.getId());
            this.sendPacketToAllClientsInSameGameSession(letCrystalDisappearPacket, connection);

            short explicitSkillId = skillCrystal.getExplicitSkillId();
            int skillId = explicitSkillId != -1 ? explicitSkillId: this.getRandomSkillBasedOnProbability(roomPlayer.getPlayer());
            List<Short> playerSkills = game.assignSkillToPlayer(playerPosition, (short) skillId);
            S2CMatchplayGivePlayerSkills givePlayerSkillsPacket = new S2CMatchplayGivePlayerSkills(playerPosition, playerSkills.get(0), playerSkills.get(1));
            this.sendPacketToAllClientsInSameGameSession(givePlayerSkillsPacket, connection);
            game.getSkillCrystals().remove(skillCrystal);
            RunnableEvent runnableEvent = runnableEventHandler.createRunnableEvent(() -> this.placeCrystalRandomly(connection, game), this.crystalDefaultRespawnTime);
            gameSession.getRunnableEvents().add(runnableEvent);
        }
    }

    public void handlePlayerUseSkill(Connection connection, C2SMatchplayUsesSkill playerUseSkill) {
        byte playerPos = playerUseSkill.getPlayerPosition();
        if (playerPos > 3) return;

        if (playerUseSkill.isQuickSlot()) {
            this.handleQuickSlotItemUse(connection, playerUseSkill);
        }

        GameSession gameSession = connection.getClient().getActiveGameSession();
        MatchplayGuardianGame game = (MatchplayGuardianGame) gameSession.getActiveMatchplayGame();
        List<Short> playerSkills = game.removeSkillFromTopOfStackFromPlayer(playerUseSkill.getPlayerPosition());
        S2CMatchplayGivePlayerSkills givePlayerSkillsPacket =
                new S2CMatchplayGivePlayerSkills(playerUseSkill.getPlayerPosition(), playerSkills.get(0), playerSkills.get(1));
        this.sendPacketToAllClientsInSameGameSession(givePlayerSkillsPacket, connection);
    }

    public void handleSkillHitsTarget(Connection connection, C2SMatchplaySkillHitsTarget skillHitsTarget) {
        byte skillId = skillHitsTarget.getSkillId();
        GameSession gameSession = connection.getClient().getActiveGameSession();
        MatchplayGuardianGame game = (MatchplayGuardianGame) gameSession.getActiveMatchplayGame();
        Skill skill = skillService.findSkillById((long)skillId);

        if (skill != null && this.isUniqueSkill(skill)) {
            this.handleUniqueSkill(connection, game, skill, skillHitsTarget);
            return;
        }

        boolean isAoeSkill = skill != null && skill.getTargeting() == 14;
        if (skillId == 0) {
            this.handleBallLossDamage(connection, skillHitsTarget);
        } else if (isAoeSkill) {
            this.handleAoeSkillDamage(connection, skillHitsTarget, game, skill);
        } else {
            this.handleSkillDamage(connection, skillHitsTarget.getTargetPosition(), skillHitsTarget, game, skill);
        }

        this.handleAllGuardiansDead(connection, game);
        this.handleAllPlayersDead(connection, game);
    }

    private void handleBallLossDamage(Connection connection, C2SMatchplaySkillHitsTarget skillHitsTarget) {
        short receiverPosition = skillHitsTarget.getTargetPosition();
        short attackerPosition = skillHitsTarget.getAttackerPosition();
        boolean attackerHasWillBuff = skillHitsTarget.getAttackerBuffId() == 3;

        GameSession gameSession = connection.getClient().getActiveGameSession();
        MatchplayGuardianGame game = (MatchplayGuardianGame) gameSession.getActiveMatchplayGame();

        boolean guardianMadePoint = skillHitsTarget.getTargetPosition() < 4;
        short newHealth;
        if (guardianMadePoint) {
            newHealth = game.damagePlayerOnBallLoss(receiverPosition, attackerPosition, attackerHasWillBuff);
        } else {
            newHealth = game.damageGuardianOnBallLoss(receiverPosition, attackerPosition, attackerHasWillBuff);
        }

        S2CMatchplayDealDamage damageToGuardianPacket = new S2CMatchplayDealDamage(skillHitsTarget.getTargetPosition(), newHealth, (byte) 0, 0, 0);
        this.sendPacketToAllClientsInSameGameSession(damageToGuardianPacket, connection);
    }

    private void handleAoeSkillDamage(Connection connection, C2SMatchplaySkillHitsTarget skillHitsTarget, MatchplayGuardianGame game, Skill skill) {
        Room room = connection.getClient().getActiveRoom();
        List<RoomPlayer> activePlayingPlayers = room.getRoomPlayerList().stream()
                .filter(x -> x.getPosition() < 4)
                .collect(Collectors.toList());
        activePlayingPlayers.forEach(x -> this.handleSkillDamage(connection, x.getPosition(), skillHitsTarget, game, skill));
    }

    private void handleSkillDamage(Connection connection, short targetPosition, C2SMatchplaySkillHitsTarget skillHitsTarget, MatchplayGuardianGame game, Skill skill) {
        boolean denyDamage = skillHitsTarget.getDamageType() == 1;
        short attackerPosition = skillHitsTarget.getAttackerPosition();
        boolean attackerHasStrBuff = skillHitsTarget.getAttackerPosition() == 0;
        boolean receiverHasDefBuff = skillHitsTarget.getReceiverBuffId() == 1;

        short skillDamage = skill.getDamage().shortValue();
        short newHealth;
        if (targetPosition < 4) {
            if (skillDamage > 1) {
                newHealth = game.healPlayer(targetPosition, skillDamage);
            } else if (denyDamage) {
                newHealth = game.getPlayerHealth(targetPosition);
            } else {
                newHealth = game.damagePlayer(attackerPosition, targetPosition, skillDamage, attackerHasStrBuff, receiverHasDefBuff);
            }

            if (newHealth < 1) {
                this.handleSpawnReviveCrystalBasedOnProbability(connection, game);
            }
        } else {
            if (denyDamage) {
                newHealth = game.getGuardianHealth(targetPosition);
            } else {
                newHealth = game.damageGuardian(targetPosition, attackerPosition, skillDamage, attackerHasStrBuff, receiverHasDefBuff);
            }

            if (newHealth < 1) {
                this.increasePotsFromGuardiansDeath(game, targetPosition);
            }
        }

        S2CMatchplayDealDamage damageToPlayerPacket =
                new S2CMatchplayDealDamage(targetPosition, newHealth, skillHitsTarget.getSkillId(), skillHitsTarget.getXKnockbackPosition(), skillHitsTarget.getYKnockbackPosition());
        this.sendPacketToAllClientsInSameGameSession(damageToPlayerPacket, connection);
    }

    private void increasePotsFromGuardiansDeath(MatchplayGuardianGame game, int guardianPos) {
        GuardianBattleState guardianBattleState = game.getGuardianBattleStates().stream()
                .filter(x -> x.getPosition() == guardianPos)
                .findFirst()
                .orElse(null);
        if (guardianBattleState != null) {
            game.setExpPot(game.getExpPot() + guardianBattleState.getExp());
            game.setGoldPot(game.getGoldPot() + guardianBattleState.getGold());
        }
    }

    private boolean isUniqueSkill(Skill skill) {
        int skillId = skill.getId().intValue();
        return skillId == 5 || skillId == 38;
    }

    private void handleUniqueSkill(Connection connection, MatchplayGuardianGame game, Skill skill, C2SMatchplaySkillHitsTarget skillHitsTarget) {
        int skillId = skill.getId().intValue();
        switch (skillId) {
            case 5: // Revive
                this.handleRevivePlayer(connection, game, skill, skillHitsTarget);
                break;
            case 38: // Sandglass
                game.resetStageStartTime();
                break;
        }
    }

    private PlayerBattleState createPlayerBattleState(RoomPlayer roomPlayer) {
        short baseHp = (short) BattleUtils.calculatePlayerHp(roomPlayer.getPlayer());
        short baseStr = roomPlayer.getPlayer().getStrength();
        short baseSta = roomPlayer.getPlayer().getStamina();
        short baseDex = roomPlayer.getPlayer().getDexterity();
        short baseWill = roomPlayer.getPlayer().getWillpower();
        short totalHp = (short) (baseHp + roomPlayer.getStatusPointsAddedDto().getAddHp());
        short totalStr = (short) (baseStr + roomPlayer.getStatusPointsAddedDto().getStrength());
        short totalSta = (short) (baseSta + roomPlayer.getStatusPointsAddedDto().getStamina());
        short totalDex = (short) (baseDex + roomPlayer.getStatusPointsAddedDto().getDexterity());
        short totalWill = (short) (baseWill + roomPlayer.getStatusPointsAddedDto().getWillpower());
        PlayerBattleState playerBattleState = new PlayerBattleState(roomPlayer.getPosition(), totalHp, totalStr, totalSta, totalDex, totalWill);
        return playerBattleState;
    }

    private GuardianBattleState createGuardianBattleState(GuardianBase guardian, short guardianPosition, int activePlayingPlayersCount) {
        int extraHp = guardian.getHpPer() * activePlayingPlayersCount;
        int extraStr = guardian.getAddStr() * activePlayingPlayersCount;
        int extraSta = guardian.getAddSta() * activePlayingPlayersCount;
        int extraDex = guardian.getAddDex() * activePlayingPlayersCount;
        int extraWill = guardian.getAddWill() * activePlayingPlayersCount;
        int totalHp = guardian.getHpBase().shortValue() + extraHp;
        int totalStr = guardian.getBaseStr() + extraStr;
        int totalSta = guardian.getBaseSta() + extraSta;
        int totalDex = guardian.getBaseDex() + extraDex;
        int totalWill = guardian.getBaseWill() + extraWill;
        GuardianBattleState guardianBattleState =
                new GuardianBattleState(guardianPosition, totalHp, totalStr, totalSta, totalDex, totalWill, guardian.getRewardExp(), guardian.getRewardGold());
        return guardianBattleState;
    }

    private void handleAllPlayersDead(Connection connection, MatchplayGuardianGame game) {
        boolean allPlayersDead = game.getPlayerBattleStates().stream().allMatch(x -> x.getCurrentHealth() < 1);
        if (allPlayersDead && !game.isGameFinished()) {
            this.handleFinishGame(connection, game, false);
        }
    }

    private void handleAllGuardiansDead(Connection connection, MatchplayGuardianGame game) {
        boolean hasBossGuardianStage = game.getBossGuardianStage() != null;
        boolean allGuardiansDead = game.getGuardianBattleStates().stream().allMatch(x -> x.getCurrentHealth() < 1);
        long timePlayingInSeconds = game.getStageTimePlayingInSeconds();
        boolean triggerBossBattle = timePlayingInSeconds < 301;
        if (hasBossGuardianStage && allGuardiansDead && triggerBossBattle && !game.isBossBattleActive()) {
            int activePlayingPlayersCount = game.getPlayerBattleStates().size();
            List<Byte> guardians = this.determineGuardians(game.getBossGuardianStage(), game.getGuardianLevelLimit());
            byte bossGuardianIndex = game.getBossGuardianStage().getBossGuardian().byteValue();
            game.setBossBattleActive(true);
            game.getGuardianBattleStates().clear();

            BossGuardian bossGuardian = this.bossGuardianService.findBossGuardianById((long) bossGuardianIndex);
            GuardianBattleState bossGuardianBattleState = this.createGuardianBattleState(bossGuardian, (short) 10, activePlayingPlayersCount);
            game.getGuardianBattleStates().add(bossGuardianBattleState);

            byte guardianStartPosition = 11;
            for (int i = 0; i < guardians.stream().count(); i++) {
                int guardianId = guardians.get(i);
                if (guardianId == 0) continue;

                short guardianPosition = (short) (i + guardianStartPosition);
                Guardian guardian = guardianService.findGuardianById((long) guardianId);
                GuardianBattleState guardianBattleState = this.createGuardianBattleState(guardian, guardianPosition, activePlayingPlayersCount);
                game.getGuardianBattleStates().add(guardianBattleState);
            }

            game.resetStageStartTime();

            S2CRoomSetBossGuardiansStats setBossGuardiansStats = new S2CRoomSetBossGuardiansStats(game.getGuardianBattleStates());
            this.sendPacketToAllClientsInSameGameSession(setBossGuardiansStats, connection);

            S2CMatchplaySpawnBossBattle matchplaySpawnBossBattle = new S2CMatchplaySpawnBossBattle(bossGuardianIndex, guardians.get(0), guardians.get(1));
            this.sendPacketToAllClientsInSameGameSession(matchplaySpawnBossBattle, connection);
        } else if (allGuardiansDead && !game.isGameFinished()) {
            this.handleFinishGame(connection, game, true);
        }
    }

    private void handleFinishGame(Connection connection, MatchplayGuardianGame game, boolean wonGame) {
        game.setGameFinished(true);
        List<PlayerReward> playerRewards = game.getPlayerRewards();
        connection.getClient().getActiveRoom().setStatus(RoomStatus.NotRunning);
        GameSession gameSession = connection.getClient().getActiveGameSession();
        gameSession.getRunnableEvents().clear();
        gameSession.getClients().forEach(client -> {
            List<RoomPlayer> roomPlayerList = connection.getClient().getActiveRoom().getRoomPlayerList();
            RoomPlayer rp = roomPlayerList.stream()
                    .filter(x -> x.getPlayer().getId().equals(client.getActivePlayer().getId()))
                    .findFirst().orElse(null);

            PlayerReward playerReward = playerRewards.stream()
                    .filter(x -> x.getPlayerPosition() == rp.getPosition())
                    .findFirst()
                    .orElse(null);

            Player player = client.getActivePlayer();
            byte oldLevel = player.getLevel();
            if (playerReward != null) {
                byte level = levelService.getLevel(playerReward.getBasicRewardExp(), player.getExpPoints(), player.getLevel());
                player.setExpPoints(player.getExpPoints() + playerReward.getBasicRewardExp());
                player.setGold(player.getGold() + playerReward.getBasicRewardGold());
                player = levelService.setNewLevelStatusPoints(level, player);
                client.setActivePlayer(player);
            }

            byte playerLevel = client.getActivePlayer().getLevel();
            if (playerLevel != oldLevel) {
                StatusPointsAddedDto statusPointsAddedDto = clothEquipmentService.getStatusPointsFromCloths(player);
                rp.setStatusPointsAddedDto(statusPointsAddedDto);

                S2CGameEndLevelUpPlayerStatsPacket gameEndLevelUpPlayerStatsPacket = new S2CGameEndLevelUpPlayerStatsPacket(rp.getPosition(), player, rp.getStatusPointsAddedDto());
                packetEventHandler.push(packetEventHandler.createPacketEvent(client, gameEndLevelUpPlayerStatsPacket, PacketEventType.DEFAULT, 0), PacketEventHandler.ServerClient.SERVER);
            }

            byte resultTitle = (byte) (wonGame ? 1 : 0);
            S2CMatchplaySetExperienceGainInfoData setExperienceGainInfoData = new S2CMatchplaySetExperienceGainInfoData(resultTitle, (int) Math.ceil((double) game.getTimeNeeded() / 1000), playerReward, playerLevel);
            client.getConnection().sendTCP(setExperienceGainInfoData);

            S2CMatchplaySetGameResultData setGameResultData = new S2CMatchplaySetGameResultData(playerRewards);
            client.getConnection().sendTCP(setGameResultData);

            S2CMatchplayBackToRoom backToRoomPacket = new S2CMatchplayBackToRoom();
            packetEventHandler.push(packetEventHandler.createPacketEvent(client, backToRoomPacket, PacketEventType.FIRE_DELAYED, TimeUnit.SECONDS.toMillis(12)), PacketEventHandler.ServerClient.SERVER);
            client.setActiveGameSession(null);
        });
    }

    private void handleMonsLavaMap(Connection connection, Room room, float averagePlayerLevel) {
        boolean isMonsLava = room.getMap() == 7 || room.getMap() == 8;
        Random random = new Random();
        int monsLavaBProbability = random.nextInt(101);
        if (isMonsLava && averagePlayerLevel >= 40 && monsLavaBProbability <= 26) {
            room.setMap((byte) 8); // MonsLavaB
            S2CRoomMapChangeAnswerPacket roomMapChangeAnswerPacket = new S2CRoomMapChangeAnswerPacket(room.getMap());
            this.sendPacketToAllClientsInSameGameSession(roomMapChangeAnswerPacket, connection);
        } else if (room.getMap() == 8) {
            room.setMap((byte) 7); // MonsLava
            S2CRoomMapChangeAnswerPacket roomMapChangeAnswerPacket = new S2CRoomMapChangeAnswerPacket(room.getMap());
            this.sendPacketToAllClientsInSameGameSession(roomMapChangeAnswerPacket, connection);
        }
    }

    private List<Byte> determineGuardians(GuardianStage guardianStage, int guardianLevelLimit) {
        List<Guardian> guardiansLeft = this.getFilteredGuardians(guardianStage.getGuardiansLeft(), guardianLevelLimit);
        List<Guardian> guardiansRight = this.getFilteredGuardians(guardianStage.getGuardiansRight(), guardianLevelLimit);
        List<Guardian> guardiansMiddle = this.getFilteredGuardians(guardianStage.getGuardiansMiddle(), guardianLevelLimit);

        byte leftGuardian = this.getRandomGuardian(guardiansLeft, new ArrayList<>());
        byte rightGuardian = this.getRandomGuardian(guardiansRight, Arrays.asList(leftGuardian));
        byte middleGuardian = this.getRandomGuardian(guardiansMiddle, Arrays.asList(leftGuardian, rightGuardian));
        if (middleGuardian != 0) {
            return Arrays.asList(middleGuardian, leftGuardian, rightGuardian);
        } else if (rightGuardian != 0) {
            return Arrays.asList(leftGuardian, rightGuardian, (byte) 0);
        }

        return Arrays.asList(leftGuardian, (byte) 0, (byte) 0);
    }

    private byte getRandomGuardian(List<Guardian> guardians, List<Byte> idsToIgnore) {
        byte guardianId = 0;
        if (guardians.size() > 0) {
            int amountsOfGuardiansToChooseFrom = guardians.size();
            if (amountsOfGuardiansToChooseFrom == 1) {
                return guardians.get(0).getId().byteValue();
            }

            Random r = new Random();
            int guardianIndex = r.nextInt(amountsOfGuardiansToChooseFrom);
            guardianId = guardians.get(guardianIndex).getId().byteValue();
            if (idsToIgnore.contains(guardianId)) {
                return getRandomGuardian(guardians, idsToIgnore);
            }
        }

        return guardianId;
    }

    private List<Guardian> getFilteredGuardians(List<Integer> ids, int guardianLevelLimit) {
        if (ids == null) {
            return new ArrayList<>();
        }

        List<Guardian> guardians = this.guardianService.findGuardiansByIds(ids);
        int lowestGuardianLevel = guardians.stream().min(Comparator.comparingInt(x -> x.getLevel())).get().getLevel();
        if (guardianLevelLimit < lowestGuardianLevel) {
            guardianLevelLimit = lowestGuardianLevel;
        }

        final int finalGuardianLevelLimit = guardianLevelLimit;
        return this.guardianService.findGuardiansByIds(ids).stream()
                .filter(x -> x.getLevel() <= finalGuardianLevelLimit)
                .collect(Collectors.toList());
    }

    private float getAveragePlayerLevel(List<RoomPlayer> roomPlayers) {
        List<RoomPlayer> activePlayingPlayers = roomPlayers.stream().filter(x -> x.getPosition() < 4).collect(Collectors.toList());
        List<Integer> playerLevels = activePlayingPlayers.stream().map(x -> (int) x.getPlayer().getLevel()).collect(Collectors.toList());
        int levelSum = playerLevels.stream().reduce(0, Integer::sum);
        float averagePlayerLevel = levelSum / activePlayingPlayers.size();
        return averagePlayerLevel;
    }

    private int getGuardianLevelLimit(float averagePlayerLevel) {
        int minGuardianLevelLimit = 10;
        int roundLevel = 5 * (Math.round(averagePlayerLevel / 5));
        if (roundLevel < averagePlayerLevel) {
            if (averagePlayerLevel < minGuardianLevelLimit) return minGuardianLevelLimit;
            return (int) averagePlayerLevel;
        }

        if (roundLevel < minGuardianLevelLimit) return minGuardianLevelLimit;
        return roundLevel;
    }

    private void handleSpawnReviveCrystalBasedOnProbability(Connection connection, MatchplayGuardianGame game) {
        Random random = new Random();
        int proba = random.nextInt(101);
        if (proba < 36) {
            short crystalId = (short) (game.getLastCrystalId() + 1);
            game.setLastCrystalId(crystalId);
            SkillCrystal skillCrystal = new SkillCrystal();
            skillCrystal.setId(crystalId);
            skillCrystal.setExplicitSkillId((short) 4);
            game.getSkillCrystals().add(skillCrystal);

            S2CMatchplayPlaceSkillCrystal placeSkillCrystal = new S2CMatchplayPlaceSkillCrystal(skillCrystal.getId(), this.getRandomPoint());
            this.sendPacketToAllClientsInSameGameSession(placeSkillCrystal, connection);
        }
    }

    private void handleRevivePlayer(Connection connection, MatchplayGuardianGame game, Skill skill, C2SMatchplaySkillHitsTarget skillHitsTarget) {
        PlayerBattleState playerBattleState = game.reviveAnyPlayer(skill.getDamage().shortValue());
        if (playerBattleState != null) {
            S2CMatchplayDealDamage damageToPlayerPacket =
                    new S2CMatchplayDealDamage(playerBattleState.getPosition(), playerBattleState.getCurrentHealth(), skillHitsTarget.getSkillId(), skillHitsTarget.getXKnockbackPosition(), skillHitsTarget.getYKnockbackPosition());
            this.sendPacketToAllClientsInSameGameSession(damageToPlayerPacket, connection);
        }
    }

    private void placeCrystalRandomly(Connection connection, MatchplayGuardianGame game) {
        GameSession gameSession = connection.getClient().getActiveGameSession();
        if (gameSession == null) return;
        Point2D point = this.getRandomPoint();

        short crystalId = (short) (game.getLastCrystalId() + 1);
        game.setLastCrystalId(crystalId);
        SkillCrystal skillCrystal = new SkillCrystal();
        skillCrystal.setId(crystalId);
        game.getSkillCrystals().add(skillCrystal);

        S2CMatchplayPlaceSkillCrystal placeSkillCrystal = new S2CMatchplayPlaceSkillCrystal(skillCrystal.getId(), point);
        this.sendPacketToAllClientsInSameGameSession(placeSkillCrystal, connection);

        Runnable despawnCrystalRunnable = () -> {
            if (gameSession == null) return;
            boolean isCrystalStillAvailable = game.getSkillCrystals().stream().anyMatch(x -> x.getId() == skillCrystal.getId());
            if (isCrystalStillAvailable) {
                S2CMatchplayLetCrystalDisappear letCrystalDisappearPacket = new S2CMatchplayLetCrystalDisappear(skillCrystal.getId());
                this.sendPacketToAllClientsInSameGameSession(letCrystalDisappearPacket, connection);
                game.getSkillCrystals().remove(skillCrystal);
                RunnableEvent runnableEvent = runnableEventHandler.createRunnableEvent(() -> this.placeCrystalRandomly(connection, game), this.crystalDefaultRespawnTime);
                gameSession.getRunnableEvents().add(runnableEvent);
            }
        };

        RunnableEvent runnableEvent = runnableEventHandler.createRunnableEvent(despawnCrystalRunnable, this.crystalDefaultDespawnTime);
        gameSession.getRunnableEvents().add(runnableEvent);
    }

    private void handleQuickSlotItemUse(Connection connection, C2SMatchplayUsesSkill playerUseSkill) {
        Player player = connection.getClient().getActivePlayer();
        QuickSlotEquipment quickSlotEquipment = player.getQuickSlotEquipment();
        int itemId = -1;
        switch (playerUseSkill.getQuickSlotIndex()) {
            case 0:
                itemId = quickSlotEquipment.getSlot1();
                break;
            case 1:
                itemId = quickSlotEquipment.getSlot2();
                break;
            case 2:
                itemId = quickSlotEquipment.getSlot3();
                break;
            case 3:
                itemId = quickSlotEquipment.getSlot4();
                break;
            case 4:
                itemId = quickSlotEquipment.getSlot5();
                break;
        }

        if (itemId > -1) {
            PlayerPocket playerPocket = this.playerPocketService.findById((long) itemId);
            playerPocket = this.playerPocketService.decrementPocketItemCount(playerPocket);
            if (playerPocket.getItemCount() == 0) {
                playerPocketService.remove(playerPocket.getId());
                pocketService.decrementPocketBelongings(player.getPocket());
            }
        }
    }

    private Point2D getRandomPoint() {
        int negator = (int) (Math.random() * 2) == 0 ? -1 : 1;
        float xPos = (float) (Math.random() * 60) * negator;
        float yPos = (short) (Math.random() * 120) * -1;
        yPos = Math.abs(yPos) < 5 ? -5 : yPos;
        return new Point2D.Float(xPos, yPos);
    }

    private int getRandomSkillBasedOnProbability(Player player) {
        SkillDropRate skillDropRate = skillDropRateService.findSkillDropRateByPlayer(player);
        String dropRates = skillDropRate.getDropRates();
        List<Integer> dropRatesInt = Arrays.stream(dropRates.split(",")).map(x -> Integer.parseInt(x)).collect(Collectors.toList());

        List<SkillDrop> skillDrops = new ArrayList<>();
        int currentPercentage = 0;
        for (int i = 0; i < dropRatesInt.size(); i++) {
            int item = dropRatesInt.get(i);
            if (item != 0) {
                SkillDrop skillDrop = new SkillDrop();
                skillDrop.setId(i);
                skillDrop.setFrom(currentPercentage);
                skillDrop.setTo(currentPercentage + item);
                skillDrops.add(skillDrop);
                currentPercentage += item;
            }
        }

        Random random = new Random();
        int randValue = random.nextInt(101);
        SkillDrop skillDrop = skillDrops.stream().filter(x -> x.getFrom() <= randValue && x.getTo() >= randValue).findFirst().orElse(null);
        return skillDrop.getId();
    }

    private RoomPlayer getRoomPlayerFromConnection(Connection connection) {
        RoomPlayer roomPlayer = connection.getClient().getActiveRoom().getRoomPlayerList().stream()
                .filter(x -> x.getPlayer().getId() == connection.getClient().getActivePlayer().getId())
                .findFirst()
                .orElse(null);
        return roomPlayer;
    }

    private void sendPacketToAllClientsInSameGameSession(Packet packet, Connection connection) {
        GameSession gameSession = connection.getClient().getActiveGameSession();
        gameSession.getClients().forEach(c -> c.getConnection().sendTCP(packet));
    }
}
