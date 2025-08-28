package happy2b.woody.core.server;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.core.command.CommandExecutors;
import happy2b.woody.core.command.StopCommandExecutor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/22
 */
public class WoodyServerHandler extends ChannelInboundHandlerAdapter {

    public static final JsonAdapter<WoodyCommand> JSON_ADAPTER = new Moshi.Builder().build().adapter(WoodyCommand.class);

    private static Channel channel;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (channel == null) {
            channel = ctx.channel();
        }
        ClientInactivityMonitor.INSTANCE.refresh();

        String eval = msg.toString().trim();
        WoodyCommand command = CommandExecutors.execute(eval);
        if (StopCommandExecutor.COMMAND_NAME.equals(eval)) {
            return;
        }
        if (command.getResult() != null) {
            ctx.writeAndFlush(JSON_ADAPTER.toJson(command));
        }
    }

    public static void write(WoodyCommand command) {
        if (channel == null) {
            return;
        }
        channel.writeAndFlush(JSON_ADAPTER.toJson(command));
    }

    public static void destroy() {
        channel = null;
    }
}
