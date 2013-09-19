/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 * Proprietary and confidential.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.peergreen.shelbie.console.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandSession;

import com.peergreen.security.UsernamePasswordAuthenticateService;
import com.peergreen.security.AutoLoginService;

/**
 * User: guillaume
 * Date: 18/03/13
 * Time: 15:35
 */
public class SecuredConsoleRunnable implements Runnable {

    private static final int SECOND = 1000;
    private static final int THREE_SECONDS = 3 * SECOND;

    private final CommandSession session;
    private final Runnable console;
    private final UsernamePasswordAuthenticateService authenticateService;
    private final AutoLoginService autoLoginService;
    private final Authentication callback;
    private int retries = 0;
    private BufferedReader reader;

    public SecuredConsoleRunnable(Runnable console,
                                  CommandSession session,
                                  UsernamePasswordAuthenticateService authenticateService,
                                  AutoLoginService autoLoginService,
                                  Authentication callback) {
        this.console = console;
        this.session = session;
        this.authenticateService = authenticateService;
        this.autoLoginService = autoLoginService;
        this.callback = callback;
    }

    @Override
    public void run() {
        boolean exit = false;
        Subject subject = null;
        while ((subject == null) && !exit) {
            // Sleeping time increase with the number of failures
            // 0 -> 3 -> 6 -> 9 -> 12
            try {
                // If auto-login failed, do not wait for the first manual login
                if ((autoLoginService != null) && (retries < 1)) {
                    Thread.sleep(retries * THREE_SECONDS);
                }
                subject = authenticate();
            } catch (InterruptedException e) {
                exit = true;
                session.getConsole().printf("Interruption: Exit local console without starting it.");
                session.getConsole().printf("Server should probably stop by itself...%n");
            } catch (IOException e) {
                exit = true;
                session.getConsole().printf("IOException: %s%n", e.getMessage());
                session.getConsole().printf("Abandon Local console bootstrap%n");
                session.getConsole().printf("Server is still running...%n");
            }
        }

        if (subject != null) {
            if (callback != null) {
                callback.onSuccess(subject);
            }
            // Store the Subject in the session for later use
            session.put(Subject.class.getName(), subject);
            // Run the console
            new PrivilegedRunnable(subject, console).run();
        }
    }

    private Subject authenticate() throws IOException {
        try {
            if (autoLoginService != null && retries == 0) {
                // Only execute auto-login once
                // If something failed, that will fail indefinitely, so switch to the manual mode
                //AutoLoginService service = autoLoginService;
                Subject subject = autoLoginService.authenticate();
                if (subject == null) {
                    session.getConsole().printf("Auto-login failed, switch to manual login process.%n");
                }
                return subject;
            } else {
                if (reader == null) {
                    initializeReader();
                }
                session.getConsole().printf("%nUsername: ");
                session.getConsole().flush();
                String username = reader.readLine();
                session.getConsole().printf("%nPassword: ");
                session.getConsole().flush();
                String password = reader.readLine();
                return authenticateService.authenticate(username, password);
            }
        } catch (Throwable t) {
            t.printStackTrace(session.getConsole());
            throw t;
        } finally {
            retries++;
        }

    }

    private void initializeReader() {
        this.reader = new BufferedReader(new InputStreamReader(session.getKeyboard()));
    }

    public interface Authentication {
        void onSuccess(Subject subject);
    }
}
