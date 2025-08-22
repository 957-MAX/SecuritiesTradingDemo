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
    // ʹ�ñ�׼��־��¼��
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // ��ʾ��¼����
        if (!LoginConsole.authenticate()) {
            System.exit(0); // ��¼ʧ���˳�ϵͳ
        }
        
        
        log.info("====== ֤ȯ����ϵͳ���� ======");
        
        try {
            // ���������������
            TradeServer tradeServer = startExchangeServer();
            
            // �����������ʱ��
            TimeUnit.SECONDS.sleep(1);
            
            // �������׿ͻ���
            TradeClient tradeClient = startTradingClient();
            
            // ��������˿���̨����
            tradeServer.startConsoleListener();
            
        } catch (Exception e) {
            log.error("ϵͳ����ʧ��", e);
        }
    }

    private static TradeServer startExchangeServer() throws Exception {
        log.info("�������������������...");
        
        // ���ط��������
        InputStream serverConfigStream = new FileInputStream("src/main/resources/server.cfg");
        SessionSettings serverSettings = new SessionSettings(serverConfigStream);
        serverConfigStream.close();

        // ���������Ӧ��
        TradeServer tradeServer = new TradeServer();
        
        // ������Ϣ�洢�������ڴ�洢��
        MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
        
        // ������־����������̨��־��
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        
        // ������Ϣ����
        MessageFactory messageFactory = new DefaultMessageFactory();

        // ����������Acceptor
        SocketAcceptor acceptor = new SocketAcceptor(
            tradeServer,  // ֱ��ʹ��tradeServerʵ��
            messageStoreFactory, 
            serverSettings, 
            logFactory, 
            messageFactory
        );
        
        // ��acceptor�󶨵�TradeServer
        tradeServer.setAcceptor(acceptor);
        
        acceptor.start();
        log.info("������������������������˿�: 5001");
        
        return tradeServer;
    }

    private static TradeClient startTradingClient() throws Exception {
        log.info("�����������׿ͻ���...");
        
        // ���ؿͻ�������
        InputStream clientConfigStream = new FileInputStream("src/main/resources/client.cfg");
        SessionSettings clientSettings = new SessionSettings(clientConfigStream);
        clientConfigStream.close();

        // �����ͻ���Ӧ��
        TradeClient tradeClient = new TradeClient();
        
        // ������Ϣ�洢�������ڴ�洢��
        MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
        
        // ������־����������̨��־��
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        
        // ������Ϣ����
        MessageFactory messageFactory = new DefaultMessageFactory();

        // ����������Initiator
        SocketInitiator initiator = new SocketInitiator(
            tradeClient,  // ֱ��ʹ��tradeClientʵ��
            messageStoreFactory, 
            clientSettings, 
            logFactory, 
            messageFactory
        );
        
        // �ؼ��޸�������initiatorʵ���������������ܣ�
        tradeClient.setInitiator(initiator);
        
        initiator.start();
        log.info("���׿ͻ�����������������: localhost:5001");
        
        return tradeClient;
    }
}