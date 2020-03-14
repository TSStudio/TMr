/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.download;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * The service provider that provides Minecraft online file downloads.
 *
 * @author huangyuhui
 */
public interface DownloadProvider {

    String getVersionListURL();

    String getAssetBaseURL();

    /**
     * Inject into original URL provided by Mojang and Forge.
     *
     * Since there are many provided URLs that are written in JSONs and are unmodifiable,
     * this method provides a way to change them.
     *
     * @param baseURL original URL provided by Mojang and Forge.
     * @return the URL that is equivalent to [baseURL], but belongs to your own service provider.
     */
    String injectURL(String baseURL);

    default Stream<String> injectURLs(String[] baseURLs) {
        Stream<String> urls = Arrays.stream(baseURLs);
        Stream<String> jsonURLs = Arrays.stream(baseURLs).map(this::injectURL);
        return Stream.concat(jsonURLs, urls);
    }

    /**
     * the specific version list that this download provider provides. i.e. "forge", "liteloader", "game", "optifine"
     *
     * @param id the id of specific version list that this download provider provides. i.e. "forge", "liteloader", "game", "optifine"
     * @return the version list
     * @throws IllegalArgumentException if the version list does not exist
     */
    VersionList<?> getVersionListById(String id);

    /**
     * The maximum download concurrency that this download provider supports.
     * @return the maximum download concurrency.
     */
    int getConcurrency();
}
