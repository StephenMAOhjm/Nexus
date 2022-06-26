package com.mao.nexus.io.netty.client.network;

import com.mao.nexus.io.common.MateInfo;
import com.mao.nexus.io.netty.client.channelpool.NettyPoolClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

/**
 * @author ：StephenMao
 * @date ：2022/6/14 13:38
 */
public class NettyRpcClient implements RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyRpcClient.class);

    private final int maxConnection;

    public NettyRpcClient(int maxConnection) {
        this.maxConnection = maxConnection;
    }

    @Override
    public byte[] sendMessage(byte[] data, MateInfo serviceInfo) throws InterruptedException, ExecutionException {
        byte[] result = null;
        final String ip = serviceInfo.getIp();
        final Integer port = serviceInfo.getPort();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, port);
        final NettyPoolClient poolClient = NettyPoolClient.getInstance(maxConnection);
        //根据地址获得池时，如果poolMap没有这个池，则会put一个生成新的池
        final Channel channel = poolClient.getChannel(inetSocketAddress);
        try {
            logger.info("use channel:{}", channel);
            final ByteBuf buffer = Unpooled.buffer(data.length);
            buffer.writeBytes(data);
            channel.writeAndFlush(buffer);
            final ClientChannelHandler handler = (ClientChannelHandler) channel.attr(ChannelManger.attributeKey).get();
            result = handler.response();
        } catch (Exception ex) {
            logger.error("send message error,msg:{}", ex.getMessage());
        } finally {
            //返还给连接池
            poolClient.release(inetSocketAddress, channel);
        }
        return result;
    }
}