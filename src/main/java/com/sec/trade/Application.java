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
    // ʹ�ñ�׼��־��¼��
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // ��ʾ��¼����
        if (!com.sec.trade.ui.LoginConsole.authenticate()) {
            System.exit(0);
        }
        
        log.info("====== ֤ȯ����ϵͳ���� ======");
        
        try {
            // ���������������
            TradeServer tradeServer = startExchangeServer();
            
            // �����������ʱ��
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("�����ӳٱ��ж�", e);
            }
            
            // �������׿ͻ���
            TradeClient tradeClient = startTradingClient();
            
            // ��������˿���̨����
            tradeServer.startConsoleListener();
            
            // �����ͻ���������̨	
            com.sec.trade.ui.MainConsole.start();
            
        } catch (Exception e) {
            log.error("ϵͳ����ʧ��", e);
        }
    }

    private static TradeServer startExchangeServer() throws Exception {
        log.info("�������������������...");
        
        // ���ط��������
        try (InputStream serverConfigStream = new FileInputStream("src/main/resources/server.cfg")) {
            SessionSettings serverSettings = new SessionSettings(serverConfigStream);
            
            // ȷ����־������ȷ����
            ensureLoggingConfig(serverSettings);
            
            // ���������Ӧ��
            TradeServer tradeServer = new TradeServer();
            
            // ������Ϣ�洢�������ڴ�洢��
            MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
            
            // ������Ϣ����
            MessageFactory messageFactory = new DefaultMessageFactory();

            // ����������Acceptor - ������Ļ��־
            SocketAcceptor acceptor = new SocketAcceptor(
                tradeServer, 
                messageStoreFactory, 
                serverSettings, 
                null,  // ������Ļ��־
                messageFactory
            );
            
            // ��acceptor�󶨵�TradeServer
            tradeServer.setAcceptor(acceptor);
            
            acceptor.start();
            log.info("������������������������˿�: 5001");
            
            return tradeServer;
        }
    }

    private static TradeClient startTradingClient() throws Exception {
        log.info("�����������׿ͻ���...");
        
        // ���ؿͻ�������
        try (InputStream clientConfigStream = new FileInputStream("src/main/resources/client.cfg")) {
            SessionSettings clientSettings = new SessionSettings(clientConfigStream);
            
            // ȷ����־������ȷ����
            ensureLoggingConfig(clientSettings);
            
            // �����ͻ���Ӧ��
            TradeClient tradeClient = new TradeClient();
            
            // ������Ϣ�洢�������ڴ�洢��
            MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
            
            // ������Ϣ����
            MessageFactory messageFactory = new DefaultMessageFactory();

            // ����������Initiator - ������Ļ��־
            SocketInitiator initiator = new SocketInitiator(
                tradeClient, 
                messageStoreFactory, 
                clientSettings, 
                null,  // ������Ļ��־
                messageFactory
            );
            
            // �ؼ��޸�:����initiatorʵ��(������������)
            tradeClient.setInitiator(initiator);
            
            initiator.start();
            log.info("���׿ͻ�����������������: localhost:5001");
            
            return tradeClient;
        }
    }
    
//    ȷ����־������ȷ����
    private static void ensureLoggingConfig(SessionSettings settings) throws Exception {
        // ȷ��������Ļ��־ѡ�����
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
        
        // ȷ���ļ���־·��������ȷ
        if (!settings.isSetting("FileLogPath")) {
            settings.setString("FileLogPath", "logs/fix_logs");
        }
    }
}