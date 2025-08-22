package com.sec.trade;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.*;
import quickfix.fix44.*;

public class MessageHandler {

    // 处理新订单消息
    public static void handleNewOrderSingle(NewOrderSingle order, SessionID sessionId) 
            throws FieldNotFound {
        String clOrdId = order.getClOrdID().getValue();
        String symbol = order.getSymbol().getValue();
        char side = order.getSide().getValue();
        double quantity = order.getOrderQty().getValue();
        double price = order.isSetPrice() ? order.getPrice().getValue() : 0.0;
        char ordType = order.getOrdType().getValue();
        
        System.out.printf("[处理] 新订单 - ID: %s, 股票: %s, 方向: %c, 数量: %.2f, 价格: %.2f, 类型: %c%n",
                clOrdId, symbol, side, quantity, price, ordType);
    }

    // 处理执行报告
    public static void handleExecutionReport(ExecutionReport report, SessionID sessionId) 
            throws FieldNotFound {
        String clOrdId = report.getClOrdID().getValue();
        String symbol = report.getSymbol().getValue();
        char ordStatus = report.getOrdStatus().getValue();
        char execType = report.getExecType().getValue();
        double cumQty = report.getCumQty().getValue();
        double leavesQty = report.getLeavesQty().getValue();
        double lastPx = report.isSetLastPx() ? report.getLastPx().getValue() : 0.0;
        
        System.out.printf("[处理] 执行报告 - ID: %s, 股票: %s, 状态: %c, 类型: %c, 已成交: %.2f, 剩余: %.2f, 价格: %.2f%n",
                clOrdId, symbol, ordStatus, execType, cumQty, leavesQty, lastPx);
    }

    // 处理撤单请求
    public static void handleOrderCancelRequest(OrderCancelRequest cancel, SessionID sessionId) 
            throws FieldNotFound {
        String origClOrdId = cancel.getOrigClOrdID().getValue();
        String clOrdId = cancel.getClOrdID().getValue();
        String symbol = cancel.getSymbol().getValue();
        char side = cancel.getSide().getValue();
        
        System.out.printf("[处理] 撤单请求 - 原订单ID: %s, 新订单ID: %s, 股票: %s, 方向: %c%n",
                origClOrdId, clOrdId, symbol, side);
    }

    // 处理心跳消息
    public static void handleHeartbeat(Heartbeat heartbeat, SessionID sessionId) {
        System.out.println("[处理] 心跳消息");
    }

    // 处理测试请求
    public static void handleTestRequest(TestRequest testRequest, SessionID sessionId) 
            throws FieldNotFound {
        String testReqID = testRequest.getTestReqID().getValue();
        System.out.printf("[处理] 测试请求 - ID: %s%n", testReqID);
    }

    // 通用消息处理器
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
                    System.out.printf("[处理] 未知消息类型: %s%n", msgType);
            }
        } catch (FieldNotFound e) {
            System.err.println("处理消息时出错: " + e.getMessage());
        }
    }
}