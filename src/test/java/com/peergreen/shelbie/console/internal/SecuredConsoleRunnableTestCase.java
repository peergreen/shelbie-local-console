package com.peergreen.shelbie.console.internal;

import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandSession;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.peergreen.security.UsernamePasswordAuthenticateService;
import com.peergreen.security.AutoLoginService;

/**
 * User: guillaume
 * Date: 18/03/13
 * Time: 16:59
 */
public class SecuredConsoleRunnableTestCase {
    @Mock
    Runnable runnable;
    @Mock
    CommandSession commandSession;
    @Mock
    UsernamePasswordAuthenticateService authenticateService;
    @Mock
    AutoLoginService autoLoginService;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAutoLoginAuthenticationSucceeded() throws Exception {
        when(autoLoginService.authenticate()).thenReturn(new Subject());

        SecuredConsoleRunnable secured = new SecuredConsoleRunnable(
                runnable,
                commandSession,
                null,
                autoLoginService,
                null
        );

        secured.run();

        verify(commandSession).put(eq(Subject.class.getName()), any(Subject.class));
    }

    @Test(timeOut = 200)
    public void testAutoLoginAuthenticationFailed() throws Exception {
        when(autoLoginService.authenticate()).thenReturn(null);
        when(authenticateService.authenticate("guillaume", "s3cr3t")).thenReturn(new Subject());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(os);
        when(commandSession.getConsole()).thenReturn(out);

        ByteArrayInputStream is = new ByteArrayInputStream("guillaume\ns3cr3t\n".getBytes());
        when(commandSession.getKeyboard()).thenReturn(is);

        SecuredConsoleRunnable secured = new SecuredConsoleRunnable(
                runnable,
                commandSession,
                authenticateService,
                autoLoginService,
                null
        );

        secured.run();

        verify(commandSession).put(eq(Subject.class.getName()), any(Subject.class));
        verify(autoLoginService, times(1)).authenticate();
        assertContains(os.toString(),
                Arrays.asList("Auto-login failed, switch to manual login process.\n",
                        "Username: ",
                        "Password: "));
    }

    @Test
    public void testManualLoginAuthentication() throws Exception {
        when(authenticateService.authenticate("guillaume", "s3cr3t")).thenReturn(new Subject());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(os);
        when(commandSession.getConsole()).thenReturn(out);

        ByteArrayInputStream is = new ByteArrayInputStream("guillaume\ns3cr3t\n".getBytes());
        when(commandSession.getKeyboard()).thenReturn(is);

        SecuredConsoleRunnable secured = new SecuredConsoleRunnable(
                runnable,
                commandSession,
                authenticateService,
                null,
                null
        );

        secured.run();

        verify(commandSession).put(eq(Subject.class.getName()), any(Subject.class));
        assertContains(os.toString(),
                Arrays.asList("Username: ",
                        "Password: "));
    }

    @Test
    public void testManualLoginAuthenticationFailed() throws Exception {
        when(authenticateService.authenticate("guillaume", "s3cr3t")).thenReturn(new Subject());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(os);
        when(commandSession.getConsole()).thenReturn(out);

        // First attempt made with wrong credentials
        // We will wait for 3 seconds
        ByteArrayInputStream is = new ByteArrayInputStream(
                ("unknown\nwrong\n" +
                 "guillaume\ns3cr3t\n").getBytes());
        when(commandSession.getKeyboard()).thenReturn(is);

        SecuredConsoleRunnable secured = new SecuredConsoleRunnable(
                runnable,
                commandSession,
                authenticateService,
                null,
                null
        );

        secured.run();

        InOrder inOrder = inOrder(authenticateService);
        inOrder.verify(authenticateService).authenticate("unknown", "wrong");
        inOrder.verify(authenticateService).authenticate("guillaume", "s3cr3t");

        verify(commandSession).put(eq(Subject.class.getName()), any(Subject.class));
        assertContains(os.toString(),
                Arrays.asList("Username: ",
                        "Password: ",
                        "Username: ",
                        "Password: "));
    }

    private static void assertContains(String actual, List<String> expected) {
        int index = 0;
        for (String expect : expected) {
            int found = actual.indexOf(expect, index);
            if (found == -1) {
                // Not found starting at the given index
                fail(format("Cannot find expected '%s' starting at index %d in remaining String '%s'", expect, index, actual.substring(index)));
            }
            // Move cursor after the found string
            index = found + expect.length();
        }
    }
}
