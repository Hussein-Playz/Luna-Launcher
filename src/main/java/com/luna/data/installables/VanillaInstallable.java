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
package com.luna.data.installables;

import com.luna.data.Pack;
import com.luna.data.PackVersion;
import com.luna.data.minecraft.VersionManifestVersion;
import com.luna.data.minecraft.loaders.LoaderVersion;

public class VanillaInstallable extends lunaFormatInstallable {
    public VanillaInstallable(VersionManifestVersion minecraftVersion, LoaderVersion loaderVersion,
            String description) {
        super();

        this.pack = new Pack();
        this.pack.vanillaInstance = true;
        this.pack.name = "Minecraft";
        this.pack.description = description;

        this.packVersion = new PackVersion();
        this.packVersion.version = minecraftVersion.id;
        this.packVersion.minecraftVersion = minecraftVersion;
        this.packVersion.hasLoader = loaderVersion != null;

        this.loaderVersion = loaderVersion;
    }
}
