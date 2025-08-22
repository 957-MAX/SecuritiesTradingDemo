package com.sec.trade;

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
        if (!com.sec.trade.ui.LoginConsole.authenticate()) {
            System.exit(0);
        }
        
        log.info("====== 证券交易系统启动 ======");
        
        try {
            // 启动交易所服务端
            TradeServer tradeServer = startExchangeServer();
            
            // 给服务端启动时间
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("启动延迟被中断", e);
            }
            
            // 启动交易客户端
            TradeClient tradeClient = startTradingClient();
            
            // 启动服务端控制台监听
            tradeServer.startConsoleListener();
            
            // 启动客户端主控制台	
            com.sec.trade.ui.MainConsole.start();
            
        } catch (Exception e) {
            log.error("系统启动失败", e);
        }
    }

    private static TradeServer startExchangeServer() throws Exception {
        log.info("正在启动交易所服务端...");
        
        // 加载服务端配置
        try (InputStream serverConfigStream = new FileInputStream("src/main/resources/server.cfg")) {
            SessionSettings serverSettings = new SessionSettings(serverConfigStream);
            
            // 确保日志配置正确设置
            ensureLoggingConfig(serverSettings);
            
            // 创建服务端应用
            TradeServer tradeServer = new TradeServer();
            
            // 创建消息存储工厂（内存存储）
            MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
            
            // 创建消息工厂
            MessageFactory messageFactory = new DefaultMessageFactory();

            // 创建并启动Acceptor - 禁用屏幕日志
            SocketAcceptor acceptor = new SocketAcceptor(
                tradeServer, 
                messageStoreFactory, 
                serverSettings, 
                null,  // 禁用屏幕日志
                messageFactory
            );
            
            // 将acceptor绑定到TradeServer
            tradeServer.setAcceptor(acceptor);
            
            acceptor.start();
            log.info("交易所服务端已启动，监听端口: 5001");
            
            return tradeServer;
        }
    }

    private static TradeClient startTradingClient() throws Exception {
        log.info("正在启动交易客户端...");
        
        // 加载客户端配置
        try (InputStream clientConfigStream = new FileInputStream("src/main/resources/client.cfg")) {
            SessionSettings clientSettings = new SessionSettings(clientConfigStream);
            
            // 确保日志配置正确设置
            ensureLoggingConfig(clientSettings);
            
            // 创建客户端应用
            TradeClient tradeClient = new TradeClient();
            
            // 创建消息存储工厂（内存存储）
            MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
            
            // 创建消息工厂
            MessageFactory messageFactory = new DefaultMessageFactory();

            // 创建并启动Initiator - 禁用屏幕日志
            SocketInitiator initiator = new SocketInitiator(
                tradeClient, 
                messageStoreFactory, 
                clientSettings, 
                null,  // 禁用屏幕日志
                messageFactory
            );
            
            // 关键修复:设置initiator实例(用于重连功能)
            tradeClient.setInitiator(initiator);
            
            initiator.start();
            log.info("交易客户端已启动，连接至: localhost:5001");
            
            return tradeClient;
        }
    }
    
//    确保日志配置正确设置
    private static void ensureLoggingConfig(SessionSettings settings) throws Exception {
        // 确保所有屏幕日志选项被禁用
        String[] logOptions = {
            "UseScreenLog", "ScreenLogShowIncoming", "ScreenLogShowOutgoing",
            "ScreenLogShowEvents", "ScreenLogShowHeartBeats", "ScreenLogShowSession",
            "ScreenLogShowTimestamp"
        };
        
        for (String option : logOptions) {
            if (!settings.isSetting(option)) {
                settings.setString(option, "N");
            } else if (!"N".equals(settings.getString(option))) {
                settings.setString(option, "N");
            }
        }
        
        // 确保文件日志路径设置正确
        if (!settings.isSetting("FileLogPath")) {
            settings.setString("FileLogPath", "logs/fix_logs");
        }
    }
}