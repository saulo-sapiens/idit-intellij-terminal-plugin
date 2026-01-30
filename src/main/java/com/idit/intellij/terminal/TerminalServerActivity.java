package com.idit.intellij.terminal;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.terminal.ui.TerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TerminalServerActivity implements ProjectActivity {
    private static HttpServer server = null;

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        synchronized (TerminalServerActivity.class) {
            if (server == null) {
                startServer();
            }
        }
        return Unit.INSTANCE;
    }

    private void startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 9999), 0);
            server.createContext("/exec", exchange -> {
                // 1. Get the Working Directory from the custom header
                Headers requestHeaders = exchange.getRequestHeaders();
                String workingDir = requestHeaders.getFirst("X-Working-Dir");

                // 2. Get the Command from the body
                String command = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                ApplicationManager.getApplication().invokeLater(() -> openAndRun(command, workingDir));

                byte[] response = "OK".getBytes();
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openAndRun(String command, String workingDir) {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) return;
        Project project = projects[0];

        // 2025.2 API
        TerminalToolWindowManager manager = TerminalToolWindowManager.getInstance(project);

        // Use project root as fallback if workingDir is null
        String startDir = (workingDir != null && !workingDir.isEmpty()) ? workingDir : project.getBasePath();

        // createShellWidget(workingDir, tabName, activateToolWindow, runInToolWindow)
        TerminalWidget widget = manager.createShellWidget(startDir, "IDIT Build", true, true);
        widget.sendCommandToExecute(command);
    }
}