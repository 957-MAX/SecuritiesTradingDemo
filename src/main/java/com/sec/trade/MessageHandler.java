package com.sec.trade;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.*;
import quickfix.fix44.*;

public class MessageHandler {

    // �����¶�����Ϣ
    public static void handleNewOrderSingle(NewOrderSingle order, SessionID sessionId) 
            throws FieldNotFound {
        String clOrdId = order.getClOrdID().getValue();
        String symbol = order.getSymbol().getValue();
        char side = order.getSide().getValue();
        double quantity = order.getOrderQty().getValue();
        double price = order.isSetPrice() ? order.getPrice().getValue() : 0.0;
        char ordType = order.getOrdType().getValue();
        
        System.out.printf("[����] �¶��� - ID: %s, ��Ʊ: %s, ����: %c, ����: %.2f, �۸�: %.2f, ����: %c%n",
                clOrdId, symbol, side, quantity, price, ordType);
    }

    // ����ִ�б���
    public static void handleExecutionReport(ExecutionReport report, SessionID sessionId) 
            throws FieldNotFound {
        String clOrdId = report.getClOrdID().getValue();
        String symbol = report.getSymbol().getValue();
        char ordStatus = report.getOrdStatus().getValue();
        char execType = report.getExecType().getValue();
        double cumQty = report.getCumQty().getValue();
        double leavesQty = report.getLeavesQty().getValue();
        double lastPx = report.isSetLastPx() ? report.getLastPx().getValue() : 0.0;
        
        System.out.printf("[����] ִ�б��� - ID: %s, ��Ʊ: %s, ״̬: %c, ����: %c, �ѳɽ�: %.2f, ʣ��: %.2f, �۸�: %.2f%n",
                clOrdId, symbol, ordStatus, execType, cumQty, leavesQty, lastPx);
    }

    // ����������
    public static void handleOrderCancelRequest(OrderCancelRequest cancel, SessionID sessionId) 
            throws FieldNotFound {
        String origClOrdId = cancel.getOrigClOrdID().getValue();
        String clOrdId = cancel.getClOrdID().getValue();
        String symbol = cancel.getSymbol().getValue();
        char side = cancel.getSide().getValue();
        
        System.out.printf("[����] �������� - ԭ����ID: %s, �¶���ID: %s, ��Ʊ: %s, ����: %c%n",
                origClOrdId, clOrdId, symbol, side);
    }

    // ����������Ϣ
    public static void handleHeartbeat(Heartbeat heartbeat, SessionID sessionId) {
        System.out.println("[����] ������Ϣ");
    }

    // �����������
    public static void handleTestRequest(TestRequest testRequest, SessionID sessionId) 
            throws FieldNotFound {
        String testReqID = testRequest.getTestReqID().getValue();
        System.out.printf("[����] �������� - ID: %s%n", testReqID);
    }

    // ͨ����Ϣ������
    public static void handleMessage(Message message, SessionID sessionId) {
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            switch (msgType) {
                case MsgType.ORDER_SINGLE:
                    handleNewOrderSingle((NewOrderSingle) message, sessionId);
                    break;
                case MsgType.EXECUTION_REPORT:
                    handleExecutionReport((ExecutionReport) message, sessionId);
                    break;
                case MsgType.ORDER_CANCEL_REQUEST:
                    handleOrderCancelRequest((OrderCancelRequest) message, sessionId);
                    break;
                case MsgType.HEARTBEAT:
                    handleHeartbeat((Heartbeat) message, sessionId);
                    break;
                case MsgType.TEST_REQUEST:
                    handleTestRequest((TestRequest) message, sessionId);
                    break;
                default:
                    System.out.printf("[����] δ֪��Ϣ����: %s%n", msgType);
            }
        } catch (FieldNotFound e) {
            System.err.println("������Ϣʱ����: " + e.getMessage());
        }
    }
}