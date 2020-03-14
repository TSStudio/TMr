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
package org.jackhuang.hmcl.task;

import org.jackhuang.hmcl.util.CacheRepository;
import org.jackhuang.hmcl.util.Logging;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.IOUtils;
import org.jackhuang.hmcl.util.io.NetworkUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @author huangyuhui
 */
public final class GetTask extends Task<String> {

    private final List<URL> urls;
    private final Charset charset;
    private final int retry;
    private CacheRepository repository = CacheRepository.getInstance();

    public GetTask(URL url) {
        this(url, UTF_8);
    }

    public GetTask(URL url, Charset charset) {
        this(url, charset, 5);
    }

    public GetTask(URL url, Charset charset, int retry) {
        this.urls = Collections.singletonList(url);
        this.charset = charset;
        this.retry = retry;

        setName(url.toString());
        setExecutor(Schedulers.io());
    }

    public GetTask(List<URL> urls, Charset charset) {
        this.urls = new ArrayList<>(urls);
        this.charset = charset;
        this.retry = urls.size();

        setName(urls.get(0).toString());
        setExecutor(Schedulers.io());
    }

    public GetTask setCacheRepository(CacheRepository repository) {
        this.repository = repository;
        return this;
    }

    @Override
    public void execute() throws Exception {
        Exception exception = null;
        URL failedURL = null;
        boolean checkETag = true;
        for (int time = 0; time < retry; ++time) {
            URL url = urls.get(time % urls.size());
            try {
                updateProgress(0);
                HttpURLConnection conn = NetworkUtils.createConnection(url);
                if (checkETag) repository.injectConnection(conn);
                conn = NetworkUtils.resolveConnection(conn);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    // Handle cache
                    try {
                        Path cache = repository.getCachedRemoteFile(conn);
                        setResult(FileUtils.readText(cache));
                        return;
                    } catch (IOException e) {
                        Logging.LOG.log(Level.WARNING, "Unable to use cached file, redownload it", e);
                        repository.removeRemoteEntry(conn);
                        continue;
                    }
                } else if (conn.getResponseCode() / 100 != 2) {
                    throw new IOException("Server error, response code: " + conn.getResponseCode());
                }

                InputStream input = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
                int size = conn.getContentLength(), read = 0, len;
                while ((len = input.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                    read += len;

                    if (size >= 0)
                        updateProgress(read, size);

                    if (Thread.currentThread().isInterrupted())
                        return;
                }

                if (size > 0 && size != read)
                    throw new IOException("Not completed! Readed: " + read + ", total size: " + size);

                String result = baos.toString(charset.name());
                setResult(result);

                if (checkETag) {
                    repository.cacheText(result, conn);
                }
                return;
            } catch (IOException ex) {
                failedURL = url;
                exception = ex;
                Logging.LOG.log(Level.WARNING, "Failed to download " + url + ", repeat times: " + (time + 1), ex);
            }
        }
        if (exception != null)
            throw new DownloadException(failedURL, exception);
    }

}
