package happy2b.woody.core.server;

import happy2b.woody.common.bytecode.InstrumentationUtils;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.thread.AgentThreadFactory;
import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.core.command.CommandExecutors;
import happy2b.woody.core.config.Configure;
import happy2b.woody.core.flame.manager.*;
import happy2b.woody.core.flame.resource.transform.ResourceMethodAdvice;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.woody.SpyAPI;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/18
 */
public class WoodyBootstrap {


    private static WoodyBootstrap bootstrapInstance;

    private static final String WOODY_SPY_JAR = "woody-spy.jar";
    private Configure configure;

    private int pid;

    private AtomicBoolean isBindRef = new AtomicBoolean(false);

    private Thread shutdown;

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Instrumentation instrumentation;

    private CountDownLatch latch = new CountDownLatch(1);


    private WoodyBootstrap(Configure configure, Instrumentation instrumentation) throws Throwable {
        this.configure = configure;
        this.pid = configure.getJavaPid();

        this.instrumentation = instrumentation;
        InstrumentationUtils.setInstrumentation(instrumentation);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WoodyBootstrap.this.bind();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        ClientInactivityMonitor.INSTANCE.start();

        initSpy();

        try {
            SpyAPI.init();
        } catch (Throwable e) {
            // ignore
        }

        latch.await();

        shutdown = AgentThreadFactory.newAgentThread(AgentThreadFactory.AgentThread.SHUTDOWN_HOOK, new Runnable() {
            @Override
            public void run() {
                WoodyBootstrap.this.destroy();
            }
        });

        Runtime.getRuntime().addShutdownHook(shutdown);

        WoodyLog.info("WoodyAgent started, working in process id: " + pid);

    }

    public static WoodyBootstrap getInstance() {
        return bootstrapInstance;
    }

    public static WoodyBootstrap getInstance(Instrumentation instrumentation, String args) throws Throwable {
        if (bootstrapInstance != null) {
            return bootstrapInstance;
        }
        bootstrapInstance = new WoodyBootstrap(Configure.toConfigure(args), instrumentation);
        return bootstrapInstance;
    }

    public void bind() throws Throwable {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        try {
            // 服务器启动辅助类
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    // 设置通道类型为NIO
                    .channel(NioServerSocketChannel.class)
                    // 设置服务器端口
                    .localAddress(configure.getServerPort())
                    // 配置通道选项
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 配置子通道处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new WoodyServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind().sync();
            serverChannel = future.channel();
            isBindRef.set(true);
            latch.countDown();
            serverChannel.closeFuture().sync();
        } catch (Throwable e) {
            latch.countDown();
            isBindRef.set(false);
            throw e;
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully().sync();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
            }
        }
    }

    private void initSpy() throws Throwable {
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        Class<?> spyClass = null;
        if (parent != null) {
            try {
                spyClass = parent.loadClass("java.woody.SpyAPI");
            } catch (Throwable e) {
                // ignore
            }
        }
        if (spyClass == null) {
            CodeSource codeSource = WoodyBootstrap.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                File woodyCoreJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                File spyJarFile = new File(woodyCoreJarFile.getParentFile(), WOODY_SPY_JAR);
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(spyJarFile));
            } else {
                throw new IllegalStateException("can not find " + WOODY_SPY_JAR);
            }
        }
    }


    public boolean isBind() {
        return isBindRef.get();
    }

    public void destroy() {
        ProfilingManager.destroy();
        ResourceClassManager.destroy();
        ResourceFetcherManager.destroy();
        ResourceMethodAdvice.destroy();
        ResourceMethodManager.destroy();
        TraceManager.destroy();
        ClientInactivityMonitor.destroy();
        WoodyServerHandler.destroy();
        ReflectionUtils.destroy();
        CommandExecutors.destroy();

        cleanSpyReference();

        serverChannel.writeAndFlush("stop");
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        shutdown = null;
        bootstrapInstance = null;
    }

    private void cleanSpyReference() {
        try {
            SpyAPI.setNopSpy();
            SpyAPI.destroy();
        } catch (Throwable e) {
        }
        try {
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("happy2b.woody.agent.AgentBootstrap");
            Method method = clazz.getDeclaredMethod("resetWoodyClassLoader");
            method.invoke(null);
        } catch (Throwable e) {
            // ignore
        }
    }

    public String getWoodyHome() {
        return configure.getWoodyHomeDir();
    }
}
