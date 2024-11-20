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
package com.luna.data.minecraft.loaders;

import java.util.HashMap;
import java.util.Map;

import com.luna.data.Instance;
import com.luna.managers.ConfigManager;
import com.luna.utils.Pair;

public class LoaderVersion {
    public String version;
    public String rawVersion;
    public boolean recommended;
    public String type;

    public Map<String, Pair<String, Long>> downloadables = new HashMap<>();

    public LoaderVersion(String version, String rawVersion, boolean recommended, String type) {
        this.version = version;
        this.rawVersion = rawVersion;
        this.recommended = recommended;
        this.type = type;
    }

    public LoaderVersion(String version, boolean recommended, String type) {
        this(version, version, recommended, type);
    }

    public LoaderVersion(String version) {
        this(version, version, false, "Dummy");
    }

    public boolean isFabric() {
        return this.type.equalsIgnoreCase("Fabric");
    }

    public boolean isForge() {
        return this.type.equalsIgnoreCase("Forge");
    }

    public boolean isLegacyFabric() {
        return this.type.equalsIgnoreCase("LegacyFabric");
    }

    public boolean isNeoForge() {
        return this.type.equalsIgnoreCase("NeoForge");
    }

    public boolean isQuilt() {
        return this.type.equalsIgnoreCase("Quilt");
    }

    public String toString() {
        if (this.recommended) {
            return this.version + " (Recommended)";
        }

        return this.version;
    }

    public LoaderType getLoaderType() {
        if (isFabric()) {
            return LoaderType.FABRIC;
        }

        if (isLegacyFabric()) {
            return LoaderType.LEGACY_FABRIC;
        }

        if (isNeoForge()) {
            return LoaderType.NEOFORGE;
        }

        if (isQuilt()) {
            return LoaderType.QUILT;
        }

        return LoaderType.FORGE;
    }

    public String toStringWithCurrent(Instance instance) {
        String string = this.version;

        if (this.recommended) {
            string += " (Recommended)";
        }

        if (instance != null && instance.launcher.loaderVersion != null
                && instance.launcher.loaderVersion.version.equals(this.version)) {
            string += " (Current)";
        }

        return string;
    }

    public boolean shouldInstallServerScripts() {
        if (ConfigManager.getConfigItem("neoForgeServerStarterJar.enabled", false) == true) {
            return true;
        }

        return !this.isForgeLikeAndUsesServerStarterJar();
    }

    public boolean isForgeLikeAndUsesServerStarterJar() {
        // Forge 37 and newer (assumed) provide their own scripts for launching
        if (this.isForge() && Integer.parseInt(this.version.substring(0, this.version.indexOf(".")), 10) >= 37) {
            return true;
        }

        // NeoForge also use their own run scripts
        return this.isNeoForge();
    }

    public String getTypeForModrinthExport() {
        if (this.isFabric() || this.isLegacyFabric()) {
            return "fabric-loader";
        }

        if (this.isQuilt()) {
            return "quilt-loader";
        }

        if (this.isNeoForge()) {
            return "neoforge";
        }

        return "forge";
    }

    public String getAnalyticsValue() {
        if (isForge()) {
            return "Forge";
        }

        if (isFabric()) {
            return "Fabric";
        }

        if (isLegacyFabric()) {
            return "Legacy Fabric";
        }

        if (isNeoForge()) {
            return "NeoForge";
        }

        if (isQuilt()) {
            return "Quilt";
        }

        return "None";
    }
}
