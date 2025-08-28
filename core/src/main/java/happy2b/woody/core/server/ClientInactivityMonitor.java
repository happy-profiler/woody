package happy2b.woody.core.server;

import happy2b.woody.common.thread.AgentThreadFactory;
import happy2b.woody.common.utils.AnsiLog;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/27
 */
public class ClientInactivityMonitor implements Runnable {

    private Thread workerThread;
    private int activityThresholdInMills = 10 * 60 * 1000;
    private volatile long lastActivityTime = System.currentTimeMillis();

    public static ClientInactivityMonitor INSTANCE = new ClientInactivityMonitor();

    private ClientInactivityMonitor() {
        workerThread = AgentThreadFactory.newAgentThread(AgentThreadFactory.AgentThread.WOODY_CLIENT_INACTIVITY_MONITOR, this);
    }

    public void start() {
        workerThread.start();
    }

    public void refresh() {
        lastActivityTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(lastActivityTime + activityThresholdInMills - System.currentTimeMillis());
                if (System.currentTimeMillis() >= lastActivityTime + activityThresholdInMills) {
                    AnsiLog.info("client inactivity timeout, close connection, stop server");
                    break;
                }
            }
        } catch (Exception e) {
            // ignore
        } finally {
            WoodyBootstrap.getInstance().destroy();
        }
    }

    public static void destroy() {
        if (INSTANCE != null) {
            INSTANCE.workerThread.interrupt();
            INSTANCE.workerThread = null;
            INSTANCE = null;
        }
    }
}
