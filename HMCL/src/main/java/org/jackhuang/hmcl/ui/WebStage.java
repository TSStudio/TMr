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
package org.jackhuang.hmcl.ui;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import static org.jackhuang.hmcl.setting.ConfigHolder.config;
import static org.jackhuang.hmcl.ui.FXUtils.newImage;

public class WebStage extends Stage {
    private final WebView webView = new WebView();

    public WebStage() {
        setScene(new Scene(webView, 800, 480));
        getScene().getStylesheets().addAll(config().getTheme().getStylesheets());
        getIcons().add(newImage("/assets/img/icon.png"));
    }

    public WebView getWebView() {
        return webView;
    }
}
