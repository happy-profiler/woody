package happy2b.woody.boot;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import happy2b.woody.common.api.WoodyCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/20
 */
public class WoodyClient {

    private static final JsonAdapter<WoodyCommand> JSON_ADAPTER = new Moshi.Builder().build().adapter(WoodyCommand.class);

    // 服务器地址和端口
    private final String host;
    private final int port;
    private final long pid;

    private String terminalCommandFlag;

    Terminal terminal = null;
    EventLoopGroup group = null;
    Bootstrap bootstrap = null;
    Channel channel = null;

    LineReader reader = null;

    volatile CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean serverStop = new AtomicBoolean(false);

    volatile long abortTime;

    public WoodyClient(String host, int port, long pid) {
        this.host = host;
        this.port = port;
        this.pid = pid;
        this.terminalCommandFlag = String.format("[woody@%d]$ ", pid);
    }

    public void boot() throws Exception {
        try {
            initTerminal();
            initClient();
            startWorking();
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            destroy();
        }
    }

    private void initTerminal() throws IOException {
        terminal = TerminalBuilder.builder().system(true).jansi(true).build();
        reader = LineReaderBuilder.builder().terminal(terminal).build();

        terminal.handle(Terminal.Signal.INT, signal -> {
            abortTime = System.currentTimeMillis();
            latch.countDown();
        });
    }

    private void initClient() throws InterruptedException {
        Thread thread = Thread.currentThread();
        PrintWriter terminalWriter = terminal.writer();

        ChannelInboundHandlerAdapter channelHandler = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                try {
                    WoodyCommand command = JSON_ADAPTER.fromJson((String) msg);
                    if (command.getTime() > abortTime) {
                        terminalWriter.println(command.getResult());
                        if (!command.isAsync()) {
                            latch.countDown();
                        }
                    }
                } catch (Exception e) {
                    System.err.println(msg);
                    e.printStackTrace();
                }
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                terminalWriter.println("服务端断开连接,程序退出");
                serverStop.set(true);
                thread.interrupt();
            }
        };
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                        ch.pipeline().addLast(new LengthFieldPrepender(4));
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(new StringEncoder());
                        pipeline.addLast(channelHandler);
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        System.out.println("已连接到服务器: " + host + ":" + port);

        channel = future.channel();
    }

    private void startWorking() {
        String input;
        System.out.println("请输入命令,输入stop退出:");
        while ((input = safeReadLine(reader)) != null) {
            if (serverStop.get()) {
                break;
            }
            if (input.isEmpty()) {
                continue;
            }
            if ("stop".equalsIgnoreCase(input.trim())) {
                channel.writeAndFlush("stop");
                break;
            }
            channel.writeAndFlush(input);
            try {
                if (latch.getCount() == 0) {
                    latch = new CountDownLatch(1);
                }
                latch.await();
            } catch (InterruptedException e) {
            }
            latch = new CountDownLatch(1);
        }
    }

    private void destroy() throws IOException {
        if (channel != null) {
            channel.close();
        }
        if (terminal != null) {
            terminal.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    private String safeReadLine(LineReader reader) {
        try {
            return reader.readLine(terminalCommandFlag);
        } catch (UserInterruptException us) {
            if (serverStop.get()) {
                return "stop";
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}
