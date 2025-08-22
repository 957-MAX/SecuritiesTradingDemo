package com.sec.trade;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SocketInitiator;
import quickfix.UnsupportedMessageType;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.HandlInst;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

public class TradeClient extends MessageCracker implements Application {
    private static final Logger log = LoggerFactory.getLogger(TradeClient.class);
    private SessionID sessionId;
    private boolean isLoggedOn = false;
    private SocketInitiator initiator; // ���������������ĳ�ʼ����
    
    // ��������
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private ScheduledExecutorService reconnectScheduler;
    
    // ���ó�ʼ����������������
    public void setInitiator(SocketInitiator initiator) {
        this.initiator = initiator;
    }

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("�Ự�����ɹ�: {}", sessionId);
        this.sessionId = sessionId;
    }

    @Override
    public void onLogon(SessionID sessionId) {
        log.info("��¼�ɹ�! Session: {}", sessionId);
        this.sessionId = sessionId;
        this.isLoggedOn = true;
        
        // ��¼�ɹ�����ʾ������
        sendSampleOrder();
        
        // ��������������
        resetReconnectAttempts();
    }
    
    // ��������������
    private void resetReconnectAttempts() {
        reconnectAttempts = 0;
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdown();
            reconnectScheduler = null;
        }
    }

    @Override
    public void onLogout(SessionID sessionId) {
        log.info("�ǳ��ɹ�! Session: {}", sessionId);
        this.isLoggedOn = false;
        
        // ������������
        scheduleReconnect();
    }
    
    // ������������
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log.warn("�Ѵﵽ�����������({})��ֹͣ����", MAX_RECONNECT_ATTEMPTS);
            return;
        }
        
        if (reconnectScheduler == null) {
            reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        reconnectAttempts++;
        long delaySeconds = (long) Math.pow(2, reconnectAttempts);
        
        log.info("���� {} ����Ե� {} ������", delaySeconds, reconnectAttempts);
        
        reconnectScheduler.schedule(() -> {
            try {
                if (initiator != null) {
                    log.info("������������...");
                    
                    // ʹ�� start() �� stop() ������ reconnect()
                    // 1. ֹͣ��ǰ����
                    initiator.stop();
                    // 2. ����������
                    initiator.start();
                } else {
                    log.error("����ʧ�ܣ�initiatorδ��ʼ��");
                }
            } catch (Exception e) {
                log.error("����ʧ�ܣ�{}", e.getMessage());
                scheduleReconnect(); // ����
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        log.debug("���͹���Ա��Ϣ: {}", message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectTagValue, IncorrectDataFormat, RejectLogon {
        log.debug("���չ���Ա��Ϣ: {}", message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        log.debug("����Ӧ����Ϣ: {}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType, IncorrectDataFormat {
        log.info("����Ӧ����Ϣ: {}", message);
        
        // ʹ����Ϣ·�ɻ���
        crack(message, sessionId);
    }
    
    // ����ִ�б��棨Order������
    public void onMessage(ExecutionReport report, SessionID sessionId) 
            throws FieldNotFound {
        log.info("���յ�ִ�б���");
        
        String orderId = report.getString(ClOrdID.FIELD);
        String symbol = report.getString(Symbol.FIELD);
        char ordStatus = report.getChar(OrdStatus.FIELD);
        double cumQty = report.getDouble(CumQty.FIELD);
        double leavesQty = report.getDouble(LeavesQty.FIELD);
        
        String statusText = getStatusText(ordStatus); // ת��״̬Ϊ�ɶ��ı�
        log.info("����״̬���� - ID: {}, ״̬: {}, �ѳɽ�: {}, ʣ��: {}",
                orderId, statusText, cumQty, leavesQty);
    }
    
    // ״̬��ת������
    private String getStatusText(char status) {
        switch(status) {
            case OrdStatus.NEW: return "�¶���";
            case OrdStatus.PARTIALLY_FILLED: return "���ֳɽ�";
            case OrdStatus.FILLED: return "ȫ���ɽ�";
            case OrdStatus.CANCELED: return "��ȡ��";
            default: return Character.toString(status);
        }
    }

    // ����ʾ������
    private void sendSampleOrder() {
        if (!isLoggedOn || sessionId == null) {
            log.warn("���Է��Ͷ���ʱδ��¼");
            return;
        }
        
        try {
            NewOrderSingle order = new NewOrderSingle();
            order.set(new ClOrdID("ORD" + System.currentTimeMillis()));  // ����ID
            order.set(new Side(Side.BUY));                               // ������
            order.set(new TransactTime());                               // ����ʱ��
            order.set(new OrdType(OrdType.LIMIT));                       // �޼۵�
            
            order.set(new Symbol("AAPL"));                        // ��Ʊ����
            order.set(new OrderQty(100));                         // ����
            order.set(new Price(150.25));                         // �۸�
            
            // ʹ����ȷ��HandlInstֵ
            order.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION));
            
            // ���Ͷ���
            Session.sendToTarget(order, sessionId);
            log.info("�ѷ����¶���");
            
        } catch (SessionNotFound e) {
            log.error("���Ͷ���ʱ�Ựδ�ҵ�: {}", e.getMessage());
        } catch (Exception e) {
            log.error("���Ͷ���ʱ����: {}", e.getMessage());
        }
    }
}