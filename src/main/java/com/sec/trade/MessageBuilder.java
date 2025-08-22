package com.sec.trade;

import quickfix.field.*;
import quickfix.fix44.*;
import java.util.Date;

public class MessageBuilder {

    // 创建新订单消息
    public static NewOrderSingle createNewOrder(
            String clOrdId, 
            char side, 
            String symbol, 
            double quantity, 
            double price,
            char ordType) {
        
        NewOrderSingle order = new NewOrderSingle(
            new ClOrdID(clOrdId),
            new Side(side),
            new TransactTime(),
            new OrdType(ordType)
        );
        
        order.set(new Symbol(symbol));
        order.set(new OrderQty(quantity));
        
        if (ordType == OrdType.LIMIT) {
            order.set(new Price(price));
        }
        
        order.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION));
        return order;
    }

    // 创建撤单请求
    public static OrderCancelRequest createCancelRequest(
            String origClOrdId, 
            String clOrdId, 
            String symbol, 
            char side) {
        
        OrderCancelRequest cancel = new OrderCancelRequest(
            new OrigClOrdID(origClOrdId),
            new ClOrdID(clOrdId),
            new Side(side),
            new TransactTime()
        );
        
        cancel.set(new Symbol(symbol));
        return cancel;
    }

    // 创建执行报告
    public static ExecutionReport createExecutionReport(
            String clOrdId,
            String symbol,
            char side,
            char ordStatus,
            char execType,
            double orderQty,
            double cumQty,
            double leavesQty,
            double lastPx) {
        
        ExecutionReport report = new ExecutionReport(
            new OrderID("EX_" + System.currentTimeMillis()),
            new ExecID("EXEC_" + System.currentTimeMillis()),
            new ExecType(execType),
            new OrdStatus(ordStatus),
            new Side(side),
            new LeavesQty(leavesQty),
            new CumQty(cumQty), null
        );
        
        report.set(new ClOrdID(clOrdId));
        report.set(new Symbol(symbol));
        report.set(new OrderQty(orderQty));
        report.set(new LastQty(cumQty));
        report.set(new LastPx(lastPx));
        report.set(new AvgPx(lastPx));
        
        return report;
    }

    // 创建心跳消息
    public static Heartbeat createHeartbeat() {
        return new Heartbeat();
    }

    // 创建测试请求
    public static TestRequest createTestRequest(String testReqID) {
        return new TestRequest(new TestReqID(testReqID));
    }
}