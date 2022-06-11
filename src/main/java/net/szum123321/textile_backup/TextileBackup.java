/*
    A simple backup mod for Fabric
    Copyright (C) 2020  Szum123321

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package net.szum123321.textile_backup;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.szum123321.textile_backup.commands.create.CleanupCommand;
import net.szum123321.textile_backup.commands.create.StartBackupCommand;
import net.szum123321.textile_backup.commands.manage.BlacklistCommand;
import net.szum123321.textile_backup.commands.manage.DeleteCommand;
import net.szum123321.textile_backup.commands.manage.WhitelistCommand;
import net.szum123321.textile_backup.commands.restore.KillRestoreCommand;
import net.szum123321.textile_backup.commands.manage.ListBackupsCommand;
import net.szum123321.textile_backup.commands.restore.RestoreBackupCommand;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.config.ConfigPOJO;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;
import net.szum123321.textile_backup.core.create.BackupHelper;
import net.szum123321.textile_backup.core.create.BackupScheduler;

import java.util.concurrent.Executors;

public class TextileBackup implements ModInitializer {
    public static final String MOD_NAME = "Textile Backup";
    public static final String MOD_ID = "textile_backup";

    private final static TextileLogger log = new TextileLogger(MOD_NAME);
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    @Override
    public void onInitialize() {
        log.info("Starting Textile Backup by Szum123321");

        ConfigHelper.updateInstance(AutoConfig.register(ConfigPOJO.class, JanksonConfigSerializer::new));

        ServerTickEvents.END_SERVER_TICK.register(new BackupScheduler()::tick);

        //Restart Executor Service in single-player
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if(Statics.executorService.isShutdown()) Statics.executorService = Executors.newSingleThreadExecutor();

            Utilities.updateTMPFSFlag(server);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Statics.executorService.shutdown();

            if (config.get().shutdownBackup && Statics.globalShutdownBackupFlag.get()) {
                BackupHelper.create(
                        BackupContext.Builder
                                .newBackupContextBuilder()
                                .setServer(server)
                                .setInitiator(ActionInitiator.Shutdown)
                                .setComment("shutdown")
                                .build()
                ).run();
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                LiteralArgumentBuilder.<ServerCommandSource>literal("backup")
                        .requires((ctx) -> {
                                    try {
                                        return ((config.get().playerWhitelist.contains(ctx.getEntityOrThrow().getEntityName()) ||
                                                ctx.hasPermissionLevel(config.get().permissionLevel)) &&
                                                !config.get().playerBlacklist.contains(ctx.getEntityOrThrow().getEntityName())) ||
                                                (ctx.getServer().isSingleplayer() &&
                                                        config.get().alwaysSingleplayerAllowed);
                                    } catch (Exception ignored) { //Command was called from server console.
                                        return true;
                                    }
                                }
                        )
                        .then(StartBackupCommand.register())
                        .then(CleanupCommand.register())
                        .then(WhitelistCommand.register())
                        .then(BlacklistCommand.register())
                        .then(RestoreBackupCommand.register())
                        .then(ListBackupsCommand.register())
                        .then(DeleteCommand.register())
                        .then(KillRestoreCommand.register())
        ));
    }
}
