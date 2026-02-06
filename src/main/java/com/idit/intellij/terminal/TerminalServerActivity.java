package com.idit.intellij.terminal;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.WindowManager;
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
import java.awt.Window;

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
                Headers requestHeaders = exchange.getRequestHeaders();
                String workingDir = requestHeaders.getFirst("X-Working-Dir");

                String command = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                // Logic to find the focused project window
                ApplicationManager.getApplication().invokeLater(() -> {
                    Project activeProject = findActiveProject();
                    if (activeProject != null) {
                        openAndRun(activeProject, command, workingDir);
                    }
                });

                byte[] response = "OK".getBytes();
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Project findActiveProject() {
        // Look for the IDE window that currently has focus
        Window focusedWindow = WindowManager.getInstance().getMostRecentFocusedWindow();
        Project project = WindowManager.getInstance().getProject(focusedWindow);

        // Fallback: If no focus, just take the first available project
        if (project == null) {
            Project[] projects = com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects();
            return projects.length > 0 ? projects[0] : null;
        }
        return project;
    }

    private void openAndRun(Project project, String command, String workingDir) {
        TerminalToolWindowManager manager = TerminalToolWindowManager.getInstance(project);
        String startDir = (workingDir != null && !workingDir.isEmpty()) ? workingDir : project.getBasePath();

        // Compatibility: createShellWidget is available in 243.x
        TerminalWidget widget = manager.createShellWidget(startDir, "IDIT Build", true, true);
        widget.sendCommandToExecute(command);
    }
}