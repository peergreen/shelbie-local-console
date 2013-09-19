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

import org.apache.felix.service.command.CommandSession;
import org.ow2.shelbie.core.system.SystemService;

/**
* User: guillaume
* Date: 25/03/13
* Time: 14:33
*/
class ShutdownCallback implements Runnable {
    private final SystemService system;
    private final CommandSession session;

    public ShutdownCallback(SystemService system, CommandSession session) {
        this.system = system;
        this.session = session;
    }

    public void run() {
        if (!system.isStopping()) {
            try {
                // This is called after a graceful shell exit
                session.execute("shelbie:shutdown --force --quiet");
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
