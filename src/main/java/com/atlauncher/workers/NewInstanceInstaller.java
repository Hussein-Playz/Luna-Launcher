/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2019 ATLauncher
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
package com.atlauncher.workers;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.atlauncher.App;
import com.atlauncher.Gsons;
import com.atlauncher.LogManager;
import com.atlauncher.data.Constants;
import com.atlauncher.data.Downloadable;
import com.atlauncher.data.Language;
import com.atlauncher.data.minecraft.ArgumentRule;
import com.atlauncher.data.minecraft.Arguments;
import com.atlauncher.data.minecraft.AssetIndex;
import com.atlauncher.data.minecraft.AssetObject;
import com.atlauncher.data.minecraft.Download;
import com.atlauncher.data.minecraft.Downloads;
import com.atlauncher.data.minecraft.Library;
import com.atlauncher.data.minecraft.LoggingFile;
import com.atlauncher.data.minecraft.MinecraftVersion;
import com.atlauncher.data.minecraft.MojangAssetIndex;
import com.atlauncher.data.minecraft.MojangDownload;
import com.atlauncher.data.minecraft.MojangDownloads;
import com.atlauncher.data.minecraft.VersionManifest;
import com.atlauncher.data.minecraft.VersionManifestVersion;
import com.atlauncher.data.minecraft.loaders.Loader;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.utils.Utils;

import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.ZipUtil;

public class NewInstanceInstaller extends InstanceInstaller {
    public List<Library> libraries = new ArrayList<>();
    public Loader loader;
    public LoaderVersion loaderVersion;
    public com.atlauncher.data.json.Version packVersion;
    public MinecraftVersion minecraftVersion;

    public String mainClass;
    public Arguments arguments;

    public NewInstanceInstaller(String instanceName, com.atlauncher.data.Pack pack,
            com.atlauncher.data.PackVersion version, boolean isReinstall, boolean isServer, String shareCode,
            boolean showModsChooser, com.atlauncher.data.loaders.LoaderVersion loaderVersion) {
        super(instanceName, pack, version, isReinstall, isServer, shareCode, showModsChooser, loaderVersion);
    }

    public NewInstanceInstaller(String instanceName, com.atlauncher.data.Pack pack,
            com.atlauncher.data.PackVersion version, boolean isReinstall, boolean isServer, String shareCode,
            boolean showModsChooser, LoaderVersion loaderVersion) {
        super(instanceName, pack, version, isReinstall, isServer, shareCode, showModsChooser, null);

        this.loaderVersion = loaderVersion;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        LogManager.info("Started install of " + this.pack.getName() + " - " + this.version);

        try {
            downloadPackVersionJson();

            downloadMinecraftVersionJson();

            if (this.packVersion.loader != null) {
                this.loader = this.packVersion.getLoader().getNewLoader(new File(this.getTempDirectory(), "loader"),
                        this, this.loaderVersion);

                downloadLoader();
            }

            if (this.packVersion.messages != null) {
                showMessages();
            }

            determineModsToBeInstalled();

            install();

            return true;
        } catch (Exception e) {
            cancel(true);
            LogManager.logStackTrace(e);
        }

        return false;
    }

    private void downloadPackVersionJson() {
        addPercent(5);
        fireTask(Language.INSTANCE.localize("instance.downloadingpackverisondefinition"));
        fireSubProgressUnknown();

        this.packVersion = Gsons.DEFAULT.fromJson(this.pack.getJSON(version.getVersion()),
                com.atlauncher.data.json.Version.class);

        this.packVersion.compileColours();

        hideSubProgressBar();
    }

    private void downloadMinecraftVersionJson() throws Exception {
        addPercent(5);
        fireTask(Language.INSTANCE.localize("instance.downloadingminecraftdefinition"));
        fireSubProgressUnknown();

        VersionManifest versionManifest = Gsons.MINECRAFT.fromJson(
                new Downloadable(String.format("%s/mc/game/version_manifest.json", Constants.LAUNCHER_META_MINECRAFT),
                        false).getContents(),
                VersionManifest.class);

        VersionManifestVersion minecraftVersion = versionManifest.versions.stream()
                .filter(version -> version.id.equalsIgnoreCase(this.packVersion.getMinecraft())).findFirst()
                .orElse(null);

        if (minecraftVersion == null) {
            throw new Exception(
                    String.format("Failed to find Minecraft version of %s", this.packVersion.getMinecraft()));
        }

        this.minecraftVersion = Gsons.MINECRAFT.fromJson(new Downloadable(minecraftVersion.url, false).getContents(),
                MinecraftVersion.class);

        hideSubProgressBar();
    }

    private void downloadLoader() {
        addPercent(5);
        fireTask(Language.INSTANCE.localize("instance.downloadingloader"));
        fireSubProgressUnknown();

        this.loader.downloadAndExtractInstaller();

        hideSubProgressBar();
    }

    private void showMessages() throws Exception {
        int ret = 0;

        if (this.isReinstall && this.packVersion.messages.update != null) {
            ret = this.packVersion.messages.showUpdateMessage(this.pack);
        } else if (this.packVersion.messages.install != null) {
            ret = this.packVersion.messages.showInstallMessage(this.pack);
        }

        if (ret != 0) {
            throw new Exception("Install cancelled after viewing message!");
        }
    }

    private void determineModsToBeInstalled() {
        this.allMods = sortMods(
                (this.isServer ? this.packVersion.getServerInstallMods() : this.packVersion.getClientInstallMods()));

        boolean hasOptional = this.allMods.stream().anyMatch(mod -> mod.isOptional());

        if (this.allMods.size() != 0 && hasOptional) {
            com.atlauncher.gui.dialogs.ModsChooser modsChooser = new com.atlauncher.gui.dialogs.ModsChooser(this);

            if (this.shareCode != null) {
                modsChooser.applyShareCode(shareCode);
            }

            if (this.showModsChooser) {
                modsChooser.setVisible(true);
            }

            if (modsChooser.wasClosed()) {
                this.cancel(true);
                return;
            }
            this.selectedMods = modsChooser.getSelectedMods();
            this.unselectedMods = modsChooser.getUnselectedMods();
        }

        if (!hasOptional) {
            this.selectedMods = this.allMods;
        }

        modsInstalled = new ArrayList<>();
        for (com.atlauncher.data.json.Mod mod : this.selectedMods) {
            String file = mod.getFile();
            if (this.packVersion.getCaseAllFiles() == com.atlauncher.data.json.CaseType.upper) {
                file = file.substring(0, file.lastIndexOf(".")).toUpperCase() + file.substring(file.lastIndexOf("."));
            } else if (this.packVersion.getCaseAllFiles() == com.atlauncher.data.json.CaseType.lower) {
                file = file.substring(0, file.lastIndexOf(".")).toLowerCase() + file.substring(file.lastIndexOf("."));
            }
            this.modsInstalled
                    .add(new com.atlauncher.data.DisableableMod(mod.getName(), mod.getVersion(), mod.isOptional(), file,
                            com.atlauncher.data.Type.valueOf(com.atlauncher.data.Type.class, mod.getType().toString()),
                            this.packVersion.getColour(mod.getColour()), mod.getDescription(), false, false, true,
                            mod.getCurseModId(), mod.getCurseFileId()));
        }

        if (this.isReinstall && instance.hasCustomMods()
                && instance.getMinecraftVersion().equalsIgnoreCase(version.getMinecraftVersion().getVersion())) {
            for (com.atlauncher.data.DisableableMod mod : instance.getCustomDisableableMods()) {
                modsInstalled.add(mod);
            }
        }
    }

    private Boolean install() throws Exception {
        this.instanceIsCorrupt = true; // From this point on the instance has become corrupt

        getTempDirectory().mkdirs(); // Make the temp directory
        backupSelectFiles();
        makeDirectories();
        addPercent(5);

        determineMainClass();
        determineArguments();

        downloadResources();
        if (isCancelled()) {
            return false;
        }

        downloadMinecraft();
        if (isCancelled()) {
            return false;
        }

        downloadLoggingClient();
        if (isCancelled()) {
            return false;
        }

        downloadLibraries();
        if (isCancelled()) {
            return false;
        }

        organiseLibraries();
        if (isCancelled()) {
            return false;
        }

        if (this.isServer && this.hasJarMods()) {
            fireTask(Language.INSTANCE.localize("server.extractingjar"));
            fireSubProgressUnknown();
            Utils.unzip(getMinecraftJar(), getTempJarDirectory());
        }
        if (!this.isServer && this.hasJarMods() && !this.hasForge()) {
            deleteMetaInf();
        }
        addPercent(5);
        if (selectedMods.size() != 0) {
            addPercent(40);
            fireTask(Language.INSTANCE.localize("instance.downloadingmods"));
            downloadMods(selectedMods);
            if (isCancelled()) {
                return false;
            }
            addPercent(40);
            installMods();
        } else {
            addPercent(80);
        }
        if (isCancelled()) {
            return false;
        }
        if (this.packVersion.shouldCaseAllFiles()) {
            doCaseConversions(getModsDirectory());
        }
        if (isServer && hasJarMods()) {
            fireTask(Language.INSTANCE.localize("server.zippingjar"));
            fireSubProgressUnknown();
            Utils.zip(getTempJarDirectory(), getMinecraftJar());
        }
        if (extractedTexturePack) {
            fireTask(Language.INSTANCE.localize("instance.zippingtexturepackfiles"));
            fireSubProgressUnknown();
            if (!getTexturePacksDirectory().exists()) {
                getTexturePacksDirectory().mkdir();
            }
            Utils.zip(getTempTexturePackDirectory(), new File(getTexturePacksDirectory(), "TexturePack.zip"));
        }
        if (extractedResourcePack) {
            fireTask(Language.INSTANCE.localize("instance.zippingresourcepackfiles"));
            fireSubProgressUnknown();
            if (!getResourcePacksDirectory().exists()) {
                getResourcePacksDirectory().mkdir();
            }
            Utils.zip(getTempResourcePackDirectory(), new File(getResourcePacksDirectory(), "ResourcePack.zip"));
        }
        if (isCancelled()) {
            return false;
        }
        if (hasActions()) {
            doActions();
        }
        if (isCancelled()) {
            return false;
        }
        if (!this.packVersion.hasNoConfigs()) {
            configurePack();
        }
        if (isCancelled()) {
            return false;
        }
        // Copy over common configs if any
        if (App.settings.getCommonConfigsDir().listFiles().length != 0) {
            Utils.copyDirectory(App.settings.getCommonConfigsDir(), getRootDirectory());
        }
        restoreSelectFiles();
        if (isServer) {
            File batFile = new File(getRootDirectory(), "LaunchServer.bat");
            File shFile = new File(getRootDirectory(), "LaunchServer.sh");
            Utils.replaceText(new File(App.settings.getLibrariesDir(), "LaunchServer.bat"), batFile, "%%SERVERJAR%%",
                    getServerJar());
            Utils.replaceText(new File(App.settings.getLibrariesDir(), "LaunchServer.sh"), shFile, "%%SERVERJAR%%",
                    getServerJar());
            batFile.setExecutable(true);
            shFile.setExecutable(true);
        }

        // add in the deselected mods to the instance.json
        for (com.atlauncher.data.json.Mod mod : this.unselectedMods) {
            String file = mod.getFile();
            if (this.packVersion.getCaseAllFiles() == com.atlauncher.data.json.CaseType.upper) {
                file = file.substring(0, file.lastIndexOf(".")).toUpperCase() + file.substring(file.lastIndexOf("."));
            } else if (this.packVersion.getCaseAllFiles() == com.atlauncher.data.json.CaseType.lower) {
                file = file.substring(0, file.lastIndexOf(".")).toLowerCase() + file.substring(file.lastIndexOf("."));
            }

            this.modsInstalled
                    .add(new com.atlauncher.data.DisableableMod(mod.getName(), mod.getVersion(), mod.isOptional(), file,
                            com.atlauncher.data.Type.valueOf(com.atlauncher.data.Type.class, mod.getType().toString()),
                            this.packVersion.getColour(mod.getColour()), mod.getDescription(), false, false, false,
                            mod.getCurseModId(), mod.getCurseFileId()));
        }

        return true;
    }

    private void determineMainClass() {
        if (this.packVersion.mainClass != null) {
            if (this.packVersion.mainClass.depends == null && this.packVersion.mainClass.dependsGroup == null) {
                this.mainClass = this.packVersion.mainClass.mainClass;
            } else if (this.packVersion.mainClass.depends != null) {
                String depends = this.packVersion.mainClass.depends;

                if (this.selectedMods.stream().filter(mod -> mod.name.equalsIgnoreCase(depends)).count() != 0) {
                    this.mainClass = this.packVersion.mainClass.mainClass;
                }
            } else if (this.packVersion.getMainClass().hasDependsGroup()) {
                String dependsGroup = this.packVersion.mainClass.dependsGroup;

                if (this.selectedMods.stream().filter(mod -> mod.group.equalsIgnoreCase(dependsGroup)).count() != 0) {
                    this.mainClass = this.packVersion.mainClass.mainClass;
                }
            }
        }

        // if none set by pack, then use the minecraft one
        if (this.mainClass == null) {
            this.mainClass = this.version.getMinecraftVersion().getMojangVersion().getMainClass();
        }
    }

    private void determineArguments() {
        this.arguments = new Arguments();

        if (this.loader != null) {
            if (this.loader.useMinecraftArguments()) {
                if (this.minecraftVersion.arguments.game != null && this.minecraftVersion.arguments.game.size() != 0) {
                    this.arguments.game.addAll(this.minecraftVersion.arguments.game);
                }

                if (this.minecraftVersion.arguments.jvm != null && this.minecraftVersion.arguments.jvm.size() != 0) {
                    this.arguments.jvm.addAll(this.minecraftVersion.arguments.jvm);
                }
            }

            Arguments loaderArguments = this.loader.getArguments();

            if (loaderArguments != null) {
                if (loaderArguments.game != null && loaderArguments.game.size() != 0) {
                    this.arguments.game.addAll(loaderArguments.game);
                }

                if (loaderArguments.jvm != null && loaderArguments.jvm.size() != 0) {
                    this.arguments.jvm.addAll(loaderArguments.jvm);
                }
            }
        } else {
            if (this.minecraftVersion.arguments.game != null && this.minecraftVersion.arguments.game.size() != 0) {
                this.arguments.game.addAll(this.minecraftVersion.arguments.game);
            }

            if (this.minecraftVersion.arguments.jvm != null && this.minecraftVersion.arguments.jvm.size() != 0) {
                this.arguments.jvm.addAll(this.minecraftVersion.arguments.jvm);
            }
        }

        if (this.packVersion.extraArguments != null) {
            boolean add = false;

            if (this.packVersion.extraArguments.depends == null
                    && this.packVersion.extraArguments.dependsGroup == null) {
                add = true;
            } else if (this.packVersion.extraArguments.depends == null) {
                String depends = this.packVersion.extraArguments.depends;

                if (this.selectedMods.stream().filter(mod -> mod.name.equalsIgnoreCase(depends)).count() != 0) {
                    add = true;
                }
            } else if (this.packVersion.extraArguments.dependsGroup == null) {
                String dependsGroup = this.packVersion.extraArguments.dependsGroup;

                if (this.selectedMods.stream().filter(mod -> mod.group.equalsIgnoreCase(dependsGroup)).count() != 0) {
                    add = true;
                }
            }

            if (add) {
                this.arguments.game.addAll(Arrays.asList(this.packVersion.extraArguments.arguments.split(" ")).stream()
                        .map(argument -> new ArgumentRule(argument)).collect(Collectors.toList()));
            }
        }
    }

    protected void downloadResources() throws Exception {
        addPercent(5);

        if (this.isServer || this.minecraftVersion.assetIndex == null) {
            return;
        }

        fireTask(Language.INSTANCE.localize("instance.downloadingresources"));
        fireSubProgressUnknown();

        MojangAssetIndex assetIndex = this.minecraftVersion.assetIndex;
        File indexFile = new File(App.settings.getIndexesAssetsDir(), assetIndex.id + ".json");

        Downloadable assetIndexDownloadable = new Downloadable(assetIndex.url, indexFile, assetIndex.sha1,
                (int) assetIndex.size, this, false);

        if (assetIndexDownloadable.needToDownload()) {
            assetIndexDownloadable.download();
        }

        AssetIndex index = this.gson.fromJson(new FileReader(indexFile), AssetIndex.class);

        List<Downloadable> resourceDownloads = index.objects.entrySet().stream().map(entry -> {
            AssetObject object = entry.getValue();
            String url = String.format("%s/%s", Constants.MINECRAFT_RESOURCES, entry.getKey());
            String filename = object.hash.substring(0, 2) + "/" + object.hash;
            File file = new File(App.settings.getObjectsAssetsDir(), filename);

            return new Downloadable(url, file, object.hash, (int) object.size, this, false);
        }).collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(App.settings.getConcurrentConnections());
        totalBytes = 0;
        downloadedBytes = 0;

        for (Downloadable download : resourceDownloads) {
            if (download.needToDownload()) {
                totalBytes += download.getFilesize();
            }
        }

        fireSubProgress(0); // Show the subprogress bar
        for (final Downloadable download : resourceDownloads) {
            executor.execute(() -> {
                if (download.needToDownload()) {
                    fireTask(Language.INSTANCE.localize("common.downloading") + " " + download.getFilename());
                    download.download(true);
                } else {
                    download.copyFile();
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        hideSubProgressBar();
    }

    private void downloadMinecraft() {
        addPercent(5);
        fireTask(Language.INSTANCE.localize("instance.downloadingminecraft"));
        fireSubProgressUnknown();
        totalBytes = 0;
        downloadedBytes = 0;

        MojangDownloads downloads = this.minecraftVersion.downloads;

        MojangDownload mojangDownload = this.isServer ? downloads.server : downloads.client;

        Downloadable download = new Downloadable(mojangDownload.url, getMinecraftJarLibrary(), mojangDownload.sha1,
                (int) mojangDownload.size, this, false, getMinecraftJar(), this.isServer);

        if (download.needToDownload()) {
            totalBytes += download.getFilesize();
            download.download(true);
        }

        hideSubProgressBar();
    }

    public File getMinecraftJar() {
        if (isServer) {
            return new File(getRootDirectory(), String.format("minecraft_server.%s.jar", this.minecraftVersion.id));
        }

        return new File(getRootDirectory(), String.format("%s.jar", this.minecraftVersion.id));
    }

    private void downloadLoggingClient() {
        addPercent(5);

        if (this.isServer || this.minecraftVersion.logging == null) {
            return;
        }

        fireTask(Language.INSTANCE.localize("instance.downloadingloggingconfig"));
        fireSubProgressUnknown();
        totalBytes = 0;
        downloadedBytes = 0;

        LoggingFile loggingFile = this.minecraftVersion.logging.client.file;

        Downloadable download = new Downloadable(loggingFile.url,
                new File(App.settings.getLogConfigsDir(), loggingFile.id), loggingFile.sha1, (int) loggingFile.size,
                this, false);

        if (download.needToDownload()) {
            totalBytes += download.getFilesize();
            download.download(true);
        }

        hideSubProgressBar();
    }

    private List<Library> getLibraries() {
        List<Library> libraries = new ArrayList<>();

        List<Library> packVersionLibraries = getPackVersionLibraries();

        if (packVersionLibraries != null && packVersionLibraries.size() != 0) {
            libraries.addAll(packVersionLibraries);
        }

        // Now read in the library jars needed from the loader
        if (this.loader != null) {
            libraries.addAll(this.loader.getLibraries());
        }

        // lastly the Minecraft libraries
        if (this.loader == null || this.loader.useMinecraftArguments()) {
            libraries.addAll(this.minecraftVersion.libraries);
        }

        return libraries;
    }

    private List<Library> getPackVersionLibraries() {
        List<Library> libraries = new ArrayList<>();

        // Now read in the library jars needed from the pack
        for (com.atlauncher.data.json.Library library : this.packVersion.getLibraries()) {
            if (this.isServer && !library.forServer()) {
                continue;
            }

            if (library.depends != null) {
                if (this.selectedMods.stream().filter(mod -> mod.name.equalsIgnoreCase(library.depends)).count() == 0) {
                    continue;
                }
            } else if (library.hasDependsGroup()) {
                if (this.selectedMods.stream().filter(mod -> mod.group.equalsIgnoreCase(library.dependsGroup))
                        .count() == 0) {
                    continue;
                }
            }

            Library minecraftLibrary = new Library();

            minecraftLibrary.name = library.file;

            Download download = new Download();
            download.path = library.path != null ? library.path
                    : (library.server != null ? library.server : library.file);
            download.sha1 = library.md5;
            download.size = library.filesize;
            download.url = String.format("%s/%s", Constants.ATLAUNCHER_DOWNLOAD_SERVER, library.url);

            Downloads downloads = new Downloads();
            downloads.artifact = download;

            minecraftLibrary.downloads = downloads;

            libraries.add(minecraftLibrary);
        }

        return libraries;
    }

    private void downloadLibraries() {
        addPercent(5);
        fireTask(Language.INSTANCE.localize("instance.downloadinglibraries"));
        fireSubProgressUnknown();
        totalBytes = 0;
        downloadedBytes = 0;

        ExecutorService executor;
        List<Downloadable> downloads = getDownloadableLibraries();
        downloads.addAll(getDownloadableNativeLibraries());

        executor = Executors.newFixedThreadPool(App.settings.getConcurrentConnections());

        for (final Downloadable download : downloads) {
            executor.execute(() -> {
                if (download.needToDownload()) {
                    totalBytes += download.getFilesize();
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        fireSubProgress(0); // Show the subprogress bar

        executor = Executors.newFixedThreadPool(App.settings.getConcurrentConnections());

        for (final Downloadable download : downloads) {
            executor.execute(() -> {
                if (download.needToDownload()) {
                    fireTask(Language.INSTANCE.localize("common.downloading") + " " + download.getFilename());
                    download.download(true);
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        hideSubProgressBar();
    }

    private List<Downloadable> getDownloadableLibraries() {
        return this.getLibraries().stream().map(library -> {
            return new Downloadable(library.downloads.artifact.url,
                    new File(App.settings.getGameLibrariesDir(), library.downloads.artifact.path),
                    library.downloads.artifact.sha1, library.downloads.artifact.size, this, false);
        }).collect(Collectors.toList());
    }

    private List<Downloadable> getDownloadableNativeLibraries() {
        return this.getLibraries().stream().filter(library -> library.hasNativeForOS()).map(library -> {
            Download download = library.getNativeDownloadForOS();

            return new Downloadable(download.url, new File(App.settings.getGameLibrariesDir(), download.path),
                    download.sha1, download.size, this, false);
        }).collect(Collectors.toList());
    }

    private void organiseLibraries() {
        addPercent(5);
        fireTask(Language.INSTANCE.localize("instance.organisinglibraries"));
        fireSubProgressUnknown();

        this.getLibraries().stream().forEach(library -> {
            File libraryFile = new File(App.settings.getGameLibrariesDir(), library.downloads.artifact.path);

            if (isServer) {
                File serverFile = new File(getLibrariesDirectory(), library.downloads.artifact.path);

                serverFile.getParentFile().mkdirs();

                Utils.copyFile(libraryFile, serverFile, true);
            } else if (library.hasNativeForOS()) {
                File nativeFile = new File(App.settings.getGameLibrariesDir(), library.getNativeDownloadForOS().path);

                ZipUtil.unpack(nativeFile, this.getNativesDirectory(), new NameMapper() {
                    public String map(String name) {
                        if (library.extract != null && library.extract.shouldExclude(name)) {
                            return null;
                        }

                        return name;
                    }
                });
            }
        });

        hideSubProgressBar();
    }

    private void hideSubProgressBar() {
        fireSubProgress(-1);
    }
}