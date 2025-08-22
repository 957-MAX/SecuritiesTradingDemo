package com.sec.trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.ConfigError;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SocketAcceptor;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.MsgType;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class TradeServer extends MessageCracker implements Application {
    private static final Logger log = LoggerFactory.getLogger(TradeServer.class);
    private volatile AtomicBoolean shouldRun = new AtomicBoolean(true);
    private SocketAcceptor acceptor;

    // 设置Acceptor
    public void setAcceptor(SocketAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    // 关闭方法
    public void stop() {
        shouldRun.set(false);
        if (acceptor != null) {
            try {
                acceptor.stop(); // 或 acceptor.stop(false);
                log.info("服务端已安全关闭");
            } catch (Exception e) {
                log.error("关闭服务端时出错", e);
            }
        }
    }

    // 添加控制台监听线程
    public void startConsoleListener() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            log.info("服务端控制台已启动 (输入 'shutdown' 关闭服务端)");

            while (shouldRun.get()) {
                String command = scanner.nextLine();
                if ("shutdown".equalsIgnoreCase(command)) {
                    shutdown();
                    break;
                }
            }
            scanner.close();
            log.info("控制台已关闭");
        }).start();
    }

    private void shutdown() {
        stop();
    }

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("交易所会话创建: {}", sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        log.info("客户端登录成功: {}", sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        log.info("客户端登出: {}", sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        log.debug("交易所发送管理消息: {}", message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        log.debug("交易所接收管理消息: {}", message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        log.debug("交易所发送应用消息: {}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        String msgType = message.getHeader().getString(MsgType.FIELD);
        
        // 修复：使用字符串常量比较
        if (MsgType.ORDER_SINGLE.equals(msgType)) {
            log.info("交易所接收应用消息: 新订单");
            try {
                NewOrderSingle order = (NewOrderSingle) message;
                handleNewOrder(order, sessionId);
            } catch (Exception e) {
                log.error("处理新订单时出错: {}", e.getMessage());
            }
        } else {
            log.info("收到应用消息类型: {}", msgType);
        }
    }
    
    // 处理新订单请求
    private void handleNewOrder(NewOrderSingle order, SessionID sessionId)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        log.info("收到新订单请求");
        
        try {
            // 提取订单字段
            String clOrdId = order.getString(ClOrdID.FIELD);
            String symbol = order.getString(Symbol.FIELD);
            char side = order.getChar(Side.FIELD);
            double quantity = order.getDouble(OrderQty.FIELD);
            double price = order.isSetField(Price.FIELD) ? order.getDouble(Price.FIELD) : 0.0;
            
            log.info("新订单详情 | ID: {} | 股票: {} | 方向: {} | 数量: {} | 价格: {}",
                    clOrdId, symbol, getSideText(side), quantity, price);
            
            // 创建模拟执行报告（假设部分成交50股）
            ExecutionReport executionReport = createExecutionReport(
                    clOrdId, symbol, side, quantity, OrdStatus.PARTIALLY_FILLED,
                    50, price);
            
            sendToClient(executionReport, sessionId);
            log.info("✔️ 已发送执行报告 | 已成交: 50 | 剩余: 50");
        } catch (FieldNotFound e) {
            log.error("处理新订单时缺少字段: {}", e.getMessage());
        }
    }
    
    // 方向转换方法
    private String getSideText(char side) {
        switch(side) {
            case Side.BUY: return "买入";
            case Side.SELL: return "卖出";
            default: return Character.toString(side);
        }
    }
    
    // 创建执行报告 (修复变量重复定义问题)
    private ExecutionReport createExecutionReport(String clOrdId, String symbol, 
            char side, double orderQty, char ordStatus, double executedQty, double executionPrice) {
        
        double totalQty = orderQty; // 总数量
        double leavesQty = totalQty - executedQty; // 剩余数量
        
        ExecutionReport report = new ExecutionReport();
        
        // 设置执行报告字段
        report.set(new OrderID("EX_" + System.currentTimeMillis())); // 交易所订单ID
        report.set(new ExecID("EXEC_" + System.currentTimeMillis())); // 执行ID
        report.set(new ExecType(executedQty >= totalQty ? ExecType.FILL : ExecType.PARTIAL_FILL)); // 执行类型 (只设置一次)
        report.set(new OrdStatus(ordStatus)); // 订单状态
        report.set(new Side(side)); // 方向
        report.set(new LeavesQty(leavesQty)); // 剩余数量
        report.set(new CumQty(executedQty)); // 累计数量
        
        report.set(new ClOrdID(clOrdId)); // 客户订单ID
        report.set(new Symbol(symbol)); // 股票代码
        report.set(new OrderQty(totalQty)); // 订单总数量
        report.set(new LastQty(executedQty)); // 最后成交数量
        report.set(new LastPx(executionPrice)); // 最后成交价格
        report.set(new AvgPx(executionPrice)); // 平均成交价格
        
        return report;
    }
    
    // 发送消息给客户端
    private void sendToClient(Message message, SessionID sessionId) {
        try {
            Session.sendToTarget(message, sessionId);
            log.info("成功发送执行报告");
        } catch (SessionNotFound e) {
            log.error("无法发送消息: 会话未找到", e);
        }
    }
}