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

import com.jfoenix.controls.JFXCheckBox;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.jackhuang.hmcl.event.Event;
import org.jackhuang.hmcl.event.EventManager;
import org.jackhuang.hmcl.game.LauncherHelper;
import org.jackhuang.hmcl.util.Lang;
import org.jackhuang.hmcl.util.Log4jLevel;
import org.jackhuang.hmcl.util.ResourceNotFoundError;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.concurrent.CountDownLatch;

import static org.jackhuang.hmcl.setting.ConfigHolder.config;
import static org.jackhuang.hmcl.ui.FXUtils.newImage;
import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

/**
 *
 * @author huangyuhui
 */
public final class LogWindow extends Stage {

    private final ReadOnlyIntegerWrapper fatal = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper error = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper warn = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper info = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper debug = new ReadOnlyIntegerWrapper(0);
    private final LogWindowImpl impl = new LogWindowImpl();
    private final CountDownLatch latch = new CountDownLatch(1);
    public final EventManager<Event> onDone = new EventManager<>();

    public LogWindow() {
        setScene(new Scene(impl, 800, 480));
        getScene().getStylesheets().addAll(config().getTheme().getStylesheets());
        setTitle(i18n("logwindow.title"));
        getIcons().add(newImage("/assets/img/icon.png"));
    }

    public LogWindow(String text) {
        this();

        onDone.register(() -> logLine(text, Log4jLevel.INFO));
    }

    public ReadOnlyIntegerProperty fatalProperty() {
        return fatal.getReadOnlyProperty();
    }

    public int getFatal() {
        return fatal.get();
    }

    public ReadOnlyIntegerProperty errorProperty() {
        return error.getReadOnlyProperty();
    }

    public int getError() {
        return error.get();
    }

    public ReadOnlyIntegerProperty warnProperty() {
        return warn.getReadOnlyProperty();
    }

    public int getWarn() {
        return warn.get();
    }

    public ReadOnlyIntegerProperty infoProperty() {
        return info.getReadOnlyProperty();
    }

    public int getInfo() {
        return info.get();
    }

    public ReadOnlyIntegerProperty debugProperty() {
        return debug.getReadOnlyProperty();
    }

    public int getDebug() {
        return debug.get();
    }

    public void logLine(String line, Log4jLevel level) {
        Element div = impl.document.createElement("div");
        // a <pre> element to prevent multiple spaces and tabs being removed.
        Element pre = impl.document.createElement("pre");
        pre.setTextContent(line);
        div.appendChild(pre);
        impl.body.appendChild(div);
        //impl.engine.executeScript("checkNewLog(\"" + level.name().toLowerCase() + "\");scrollToBottom();");
        impl.engine.executeScript("checkNewLog(\"" + level.name().toLowerCase() + "\");" + (impl.autoscroll.isSelected() ? "scrollToBottom();" : ""));

        switch (level) {
            case FATAL:
                fatal.set(fatal.get() + 1);
                break;
            case ERROR:
                error.set(error.get() + 1);
                break;
            case WARN:
                warn.set(warn.get() + 1);
                break;
            case INFO:
                info.set(info.get() + 1);
                break;
            case DEBUG:
                debug.set(debug.get() + 1);
                break;
            default:
                // ignore
                break;
        }
    }

    public void waitForLoaded() throws InterruptedException {
        latch.await();
    }

    public class LogWindowImpl extends StackPane {

        @FXML
        private WebView webView;
        @FXML
        private ToggleButton btnFatals;
        @FXML
        private ToggleButton btnErrors;
        @FXML
        private ToggleButton btnWarns;
        @FXML
        private ToggleButton btnInfos;
        @FXML
        private ToggleButton btnDebugs;
        @FXML
        private ComboBox<String> cboLines;
        @FXML
        private CheckBox autoscroll;

        final WebEngine engine;
        Node body;
        Document document;

        LogWindowImpl() {
            FXUtils.loadFXML(this, "/assets/fxml/log.fxml");

            engine = webView.getEngine();
            engine.loadContent(Lang.ignoringException(() -> IOUtils.readFullyAsString(ResourceNotFoundError.getResourceAsStream("/assets/log-window-content.html")))
                    .replace("${FONT}", config().getFontSize() + "px \"" + config().getFontFamily() + "\""));
            engine.getLoadWorker().stateProperty().addListener((a, b, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    document = engine.getDocument();
                    body = document.getElementsByTagName("body").item(0);
                    engine.executeScript("limitedLogs=" + config().getLogLines());
                    latch.countDown();
                    onDone.fireEvent(new Event(LogWindow.this));
                }
            });

            boolean flag = false;
            for (String i : cboLines.getItems())
                if (Integer.toString(config().getLogLines()).equals(i)) {
                    cboLines.getSelectionModel().select(i);
                    flag = true;
                }

            cboLines.getSelectionModel().selectedItemProperty().addListener((a, b, newValue) -> {
                config().setLogLines(newValue == null ? 100 : Integer.parseInt(newValue));
                engine.executeScript("limitedLogs=" + config().getLogLines());
            });

            if (!flag)
                cboLines.getSelectionModel().select(0);

            btnFatals.textProperty().bind(Bindings.concat(fatal, " fatals"));
            btnErrors.textProperty().bind(Bindings.concat(error, " errors"));
            btnWarns.textProperty().bind(Bindings.concat(warn, " warns"));
            btnInfos.textProperty().bind(Bindings.concat(info, " infos"));
            btnDebugs.textProperty().bind(Bindings.concat(debug, " debugs"));

            btnFatals.selectedProperty().addListener(o -> specificChanged());
            btnErrors.selectedProperty().addListener(o -> specificChanged());
            btnWarns.selectedProperty().addListener(o -> specificChanged());
            btnInfos.selectedProperty().addListener(o -> specificChanged());
            btnDebugs.selectedProperty().addListener(o -> specificChanged());
        }

        private void specificChanged() {
            String res = "";
            if (btnFatals.isSelected())
                res += "\"fatal\", ";
            if (btnErrors.isSelected())
                res += "\"error\", ";
            if (btnWarns.isSelected())
                res += "\"warn\", ";
            if (btnInfos.isSelected())
                res += "\"info\", ";
            if (btnDebugs.isSelected())
                res += "\"debug\", ";
            if (StringUtils.isNotBlank(res))
                res = StringUtils.substringBeforeLast(res, ", ");
            engine.executeScript("specific([" + res + "])");
        }

        @FXML
        private void onTerminateGame() {
            LauncherHelper.stopManagedProcesses();
        }

        @FXML
        private void onClear() {
            engine.executeScript("clear()");
        }
    }
}
