/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.core.restore;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Util;
import net.szum123321.textile_backup.core.ActionInitiator;

import javax.annotation.Nullable;
import java.util.UUID;

public record RestoreContext(RestoreHelper.RestoreableFile file,
                             MinecraftServer server,
                             @Nullable String comment,
                             ActionInitiator initiator,
                             ServerCommandSource commandSource) {

    public RestoreHelper.RestoreableFile getFile() {
        return file;
    }

    public MinecraftServer getServer() {
        return server;
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    public ActionInitiator getInitiator() {
        return initiator;
    }

    /**
     * @return If backup was started by a player, return the corresponding message sender. Otherwise
     * null.
     *
     * @see net.minecraft.network.message.MessageSender
     */
    public @org.jetbrains.annotations.Nullable MessageSender getInitiatorAsMessageSender() {
        return initiator.equals(ActionInitiator.Player) && commandSource.getEntity() != null ? commandSource.getChatMessageSender() : null;
    }

    /**
     * Only non-null when explicitly started by a command
     * ({@link net.szum123321.textile_backup.core.ActionInitiator#Player} or
     * {@link net.szum123321.textile_backup.core.ActionInitiator#ServerConsole}).
     */
    @org.jetbrains.annotations.Nullable
    public ServerCommandSource getCommandSource() {
        return commandSource;
    }

    public static final class Builder {
        private RestoreHelper.RestoreableFile file;
        private MinecraftServer server;
        private String comment;
        private ServerCommandSource serverCommandSource;

        private Builder() {
        }

        public static Builder newRestoreContextBuilder() {
            return new Builder();
        }

        public Builder setFile(RestoreHelper.RestoreableFile file) {
            this.file = file;
            return this;
        }

        public Builder setServer(MinecraftServer server) {
            this.server = server;
            return this;
        }

        public Builder setComment(@Nullable String comment) {
            this.comment = comment;
            return this;
        }

        public Builder setCommandSource(ServerCommandSource commandSource) {
            this.serverCommandSource = commandSource;
            return this;
        }

        public RestoreContext build() {
            if (server == null) server = serverCommandSource.getServer();

            ActionInitiator initiator = serverCommandSource.getEntity() instanceof PlayerEntity ? ActionInitiator.Player : ActionInitiator.ServerConsole;

            return new RestoreContext(file, server, comment, initiator, serverCommandSource);
        }
    }
}
