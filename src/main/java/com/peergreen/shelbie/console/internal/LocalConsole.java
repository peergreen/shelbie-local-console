/*
 * Copyright 2013 Peergreen SAS
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.peergreen.shelbie.console.internal;

import java.io.IOException;
import javax.security.auth.Subject;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.service.command.CommandProcessor;
import org.ow2.shelbie.core.branding.BrandingService;
import org.ow2.shelbie.core.console.JLineConsole;
import org.ow2.shelbie.core.history.HistoryFileProvider;
import org.ow2.shelbie.core.prompt.PromptService;
import org.ow2.shelbie.core.system.SystemService;

import com.peergreen.security.UsernamePasswordAuthenticateService;
import com.peergreen.security.AutoLoginService;
import jline.TerminalFactory;
import jline.console.completer.Completer;

/**
 * A Local Console is launched during JVM startup (it cannot be started
 * after), with the JVM in foreground mode (Sharing System.in is not recommended).
 */
@Component
public class LocalConsole {

    @Requires
    private CommandProcessor processor;

    @Requires
    private SystemService system;

    @Requires(filter = "(type=commands)")
    private Completer completer;

    @Requires(policy = "dynamic-priority")
    private PromptService promptService;

    @Requires(policy = "dynamic-priority")
    private HistoryFileProvider historyProvider;

    @Requires
    private BrandingService brandingService;

    /**
     * Group that will contain the Thread created to run this instance.
     */
    @Requires(filter = "(group.name=peergreen)")
    private ThreadGroup threadGroup;

    @Requires
    private UsernamePasswordAuthenticateService authenticateService;

    /**
     * Use AutoLoginService if any (that means that someone configured
     * the auto-login service). Otherwise, fallback to the classic AuthenticateService.
     */
    @Requires(nullable = false,
              optional = true)
    private AutoLoginService autoLoginService;

    private JLineConsole console;

    @Validate
    public void startup() throws Exception {

        if (System.console() == null) {
            // If there is no console attached to the JVM, abort console startup
            throw new IOException("No console is attached to the JVM, aborting LocalConsole's startup");
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(LocalConsole.class.getClassLoader());

        try {
            // Start the console
            console = new JLineConsole(processor,
                                       completer,
                                       system.getIn(),
                                       system.getOut(),
                                       system.getErr(),
                                       TerminalFactory.get());

            // This is a local console, when closed, the system must shutdown automatically
            console.setCallback(new ShutdownCallback(system, console.getSession()));

            console.setPromptService(promptService);
            console.setBrandingService(brandingService);

            SecuredConsoleRunnable.Authentication callback = new SecuredConsoleRunnable.Authentication() {
                @Override
                public void onSuccess(Subject subject) {
                    console.setHistoryFile(historyProvider.getHistoryFile(subject));
                }
            };

            // TODO Add an uncaught ExceptionHandler to display console error if any ...
            Thread thread = new Thread(threadGroup,
                                       new SecuredConsoleRunnable(console,
                                                                  console.getSession(),
                                                                  authenticateService,
                                                                  autoLoginService,
                                                                  callback),
                                       "Peergreen Local Console");
            thread.setDaemon(true);
            thread.start();

        } catch (Throwable t) {
            t.printStackTrace(system.getErr());
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Invalidate
    public void shutdown() {
        console.close();
    }

}
