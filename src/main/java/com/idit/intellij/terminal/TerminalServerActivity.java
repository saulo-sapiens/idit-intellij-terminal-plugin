package com.idit.intellij.terminal;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
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

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
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
            // Binding to 127.0.0.1 for security
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 9999), 0);
            server.createContext("/exec", exchange -> {
                try {
                    Headers requestHeaders = exchange.getRequestHeaders();
                    String workingDir = requestHeaders.getFirst("X-Working-Dir");

                    String command = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));

                    // Switch to EDT to interact with UI/Projects
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Project activeProject = findActiveProject();
                        if (activeProject != null) {
                            openAndRun(activeProject, command, workingDir);
                        }
                    });

                    byte[] response = "OK".getBytes();
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    exchange.close();
                }
            });
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Project findActiveProject() {
        Window focusedWindow = WindowManager.getInstance().getMostRecentFocusedWindow();
        Project project = null;

        if (focusedWindow != null) {
            try {
                // Get the DataContext from the current focus to find the project
                DataContext context = DataManager.getInstance().getDataContextFromFocusAsync()
                        .blockingGet(500, TimeUnit.MILLISECONDS);
                if (context != null) {
                    project = CommonDataKeys.PROJECT.getData(context);
                }
            } catch (Exception ignored) {}
        }

        // Fallback: Use the first open project if focus-based detection fails
        if (project == null) {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            return projects.length > 0 ? projects[0] : null;
        }
        return project;
    }

    private void openAndRun(Project project, String command, String workingDir) {
        TerminalToolWindowManager manager = TerminalToolWindowManager.getInstance(project);

        // Use provided directory or fallback to project root
        String startDir = (workingDir != null && !workingDir.isEmpty()) ? workingDir : project.getBasePath();

        // createShellWidget(workingDir, tabName, activateToolWindow, runInToolWindow)
        TerminalWidget widget = manager.createShellWidget(startDir, "IDIT Build", true, true);
        widget.sendCommandToExecute(command);
    }
}