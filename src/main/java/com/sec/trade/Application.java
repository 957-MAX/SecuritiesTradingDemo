package com.sec.trade;

import com.sec.trade.ui.LoginConsole;

import quickfix.*;
import quickfix.SessionSettings;
import quickfix.MemoryStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.DefaultMessageFactory;
import quickfix.SocketAcceptor;
import quickfix.SocketInitiator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    // 使用标准日志记录器
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // 显示登录界面
        if (!LoginConsole.authenticate()) {
            System.exit(0); // 登录失败退出系统
        }
        
        
        log.info("====== 证券交易系统启动 ======");
        
        try {
            // 启动交易所服务端
            TradeServer tradeServer = startExchangeServer();
            
            // 给服务端启动时间
            TimeUnit.SECONDS.sleep(1);
            
            // 启动交易客户端
            TradeClient tradeClient = startTradingClient();
            
            // 启动服务端控制台监听
            tradeServer.startConsoleListener();
            
        } catch (Exception e) {
            log.error("系统启动失败", e);
        }
    }

    private static TradeServer startExchangeServer() throws Exception {
        log.info("正在启动交易所服务端...");
        
        // 加载服务端配置
        InputStream serverConfigStream = new FileInputStream("src/main/resources/server.cfg");
        SessionSettings serverSettings = new SessionSettings(serverConfigStream);
        serverConfigStream.close();

        // 创建服务端应用
        TradeServer tradeServer = new TradeServer();
        
        // 创建消息存储工厂（内存存储）
        MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
        
        // 创建日志工厂（控制台日志）
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        
        // 创建消息工厂
        MessageFactory messageFactory = new DefaultMessageFactory();

        // 创建并启动Acceptor
        SocketAcceptor acceptor = new SocketAcceptor(
            tradeServer,  // 直接使用tradeServer实例
            messageStoreFactory, 
            serverSettings, 
            logFactory, 
            messageFactory
        );
        
        // 将acceptor绑定到TradeServer
        tradeServer.setAcceptor(acceptor);
        
        acceptor.start();
        log.info("交易所服务端已启动，监听端口: 5001");
        
        return tradeServer;
    }

    private static TradeClient startTradingClient() throws Exception {
        log.info("正在启动交易客户端...");
        
        // 加载客户端配置
        InputStream clientConfigStream = new FileInputStream("src/main/resources/client.cfg");
        SessionSettings clientSettings = new SessionSettings(clientConfigStream);
        clientConfigStream.close();

        // 创建客户端应用
        TradeClient tradeClient = new TradeClient();
        
        // 创建消息存储工厂（内存存储）
        MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
        
        // 创建日志工厂（控制台日志）
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        
        // 创建消息工厂
        MessageFactory messageFactory = new DefaultMessageFactory();

        // 创建并启动Initiator
        SocketInitiator initiator = new SocketInitiator(
            tradeClient,  // 直接使用tradeClient实例
            messageStoreFactory, 
            clientSettings, 
            logFactory, 
            messageFactory
        );
        
        // 关键修复：设置initiator实例（用于重连功能）
        tradeClient.setInitiator(initiator);
        
        initiator.start();
        log.info("交易客户端已启动，连接至: localhost:5001");
        
        return tradeClient;
    }
}