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

import java.security.PrivilegedAction;
import javax.security.auth.Subject;

/**
* User: guillaume
* Date: 18/03/13
* Time: 15:34
*/
public class PrivilegedRunnable implements Runnable {
    private final Subject subject;
    private final Runnable delegate;

    PrivilegedRunnable(Subject subject, Runnable delegate) {
        this.subject = subject;
        this.delegate = delegate;
    }

    public void run() {
        Subject.doAs(subject, new PrivilegedAction<Object>() {
            public Object run() {
                delegate.run();
                return true;
            }
        });
    }
}
