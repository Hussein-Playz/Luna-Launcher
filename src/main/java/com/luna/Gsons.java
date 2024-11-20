/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.luna;

import java.awt.Color;
import java.time.Instant;
import java.util.Date;

import com.luna.annot.ExcludeFromGsonSerialization;
import com.luna.data.AbstractAccount;
import com.luna.data.AccountTypeAdapter;
import com.luna.data.ColorTypeAdapter;
import com.luna.data.DateTypeAdapter;
import com.luna.data.InstantTypeAdapter;
import com.luna.data.PackVersion;
import com.luna.data.PackVersionTypeAdapter;
import com.luna.data.microsoft.OauthTokenResponse;
import com.luna.data.microsoft.OauthTokenResponseTypeAdapter;
import com.luna.data.minecraft.Arguments;
import com.luna.data.minecraft.ArgumentsTypeAdapter;
import com.luna.data.minecraft.Library;
import com.luna.data.minecraft.LibraryTypeAdapter;
import com.luna.data.minecraft.loaders.fabric.FabricLibrary;
import com.luna.data.minecraft.loaders.fabric.FabricLibraryTypeAdapter;
import com.luna.data.minecraft.loaders.fabric.FabricMetaLauncherMeta;
import com.luna.data.minecraft.loaders.fabric.FabricMetaLauncherMetaTypeAdapter;
import com.luna.data.minecraft.loaders.forge.ForgeLibrary;
import com.luna.data.minecraft.loaders.forge.ForgeLibraryTypeAdapter;
import com.luna.data.minecraft.loaders.legacyfabric.LegacyFabricLibrary;
import com.luna.data.minecraft.loaders.legacyfabric.LegacyFabricLibraryTypeAdapter;
import com.luna.data.minecraft.loaders.legacyfabric.LegacyFabricMetaLauncherMeta;
import com.luna.data.minecraft.loaders.legacyfabric.LegacyFabricMetaLauncherMetaTypeAdapter;
import com.luna.data.minecraft.loaders.quilt.QuiltLibrary;
import com.luna.data.minecraft.loaders.quilt.QuiltLibraryTypeAdapter;
import com.luna.data.minecraft.loaders.quilt.QuiltMetaLauncherMeta;
import com.luna.data.minecraft.loaders.quilt.QuiltMetaLauncherMetaTypeAdapter;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Gsons {
    public static final ExclusionStrategy exclusionAnnotationStrategy = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            return field.getAnnotation(ExcludeFromGsonSerialization.class) != null;
        }
    };

    private static final Gson BASE = new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(AbstractAccount.class, new AccountTypeAdapter())
            .registerTypeAdapter(Date.class, new DateTypeAdapter())
            .registerTypeAdapter(Color.class, new ColorTypeAdapter())
            .registerTypeAdapter(OauthTokenResponse.class, new OauthTokenResponseTypeAdapter())
            .registerTypeAdapter(PackVersion.class, new PackVersionTypeAdapter())
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(Library.class, new LibraryTypeAdapter())
            .registerTypeAdapter(Arguments.class, new ArgumentsTypeAdapter())
            .registerTypeAdapter(FabricMetaLauncherMeta.class, new FabricMetaLauncherMetaTypeAdapter())
            .registerTypeAdapter(FabricLibrary.class, new FabricLibraryTypeAdapter())
            .registerTypeAdapter(LegacyFabricMetaLauncherMeta.class, new LegacyFabricMetaLauncherMetaTypeAdapter())
            .registerTypeAdapter(LegacyFabricLibrary.class, new LegacyFabricLibraryTypeAdapter())
            .registerTypeAdapter(ForgeLibrary.class, new ForgeLibraryTypeAdapter())
            .registerTypeAdapter(QuiltLibrary.class, new QuiltLibraryTypeAdapter())
            .registerTypeAdapter(QuiltMetaLauncherMeta.class, new QuiltMetaLauncherMetaTypeAdapter())
            .addSerializationExclusionStrategy(exclusionAnnotationStrategy).create();

    public static final Gson DEFAULT = BASE.newBuilder().setPrettyPrinting().create();

    public static final Gson DEFAULT_SLIM = BASE.newBuilder().create();
}
