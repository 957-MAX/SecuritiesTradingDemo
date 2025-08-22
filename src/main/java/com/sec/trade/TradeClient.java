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
    private SocketInitiator initiator; // 新增：用于重连的初始化器
    
    // 重连配置
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private ScheduledExecutorService reconnectScheduler;
    
    // 设置初始化器（用于重连）
    public void setInitiator(SocketInitiator initiator) {
        this.initiator = initiator;
    }

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("会话创建成功: {}", sessionId);
        this.sessionId = sessionId;
    }

    @Override
    public void onLogon(SessionID sessionId) {
        log.info("登录成功! Session: {}", sessionId);
        this.sessionId = sessionId;
        this.isLoggedOn = true;
        
        // 登录成功后发送示例订单
        sendSampleOrder();
        
        // 重置重连计数器
        resetReconnectAttempts();
    }
    
    // 重置重连计数器
    private void resetReconnectAttempts() {
        reconnectAttempts = 0;
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdown();
            reconnectScheduler = null;
        }
    }

    @Override
    public void onLogout(SessionID sessionId) {
        log.info("登出成功! Session: {}", sessionId);
        this.isLoggedOn = false;
        
        // 启动重连任务
        scheduleReconnect();
    }
    
    // 安排重连任务
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log.warn("已达到最大重连次数({})，停止重连", MAX_RECONNECT_ATTEMPTS);
            return;
        }
        
        if (reconnectScheduler == null) {
            reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        reconnectAttempts++;
        long delaySeconds = (long) Math.pow(2, reconnectAttempts);
        
        log.info("将在 {} 秒后尝试第 {} 次重连", delaySeconds, reconnectAttempts);
        
        reconnectScheduler.schedule(() -> {
            try {
                if (initiator != null) {
                    log.info("尝试重新连接...");
                    
                    // 使用 start() 和 stop() 组合替代 reconnect()
                    // 1. 停止当前连接
                    initiator.stop();
                    // 2. 启动新连接
                    initiator.start();
                } else {
                    log.error("重连失败：initiator未初始化");
                }
            } catch (Exception e) {
                log.error("重连失败：{}", e.getMessage());
                scheduleReconnect(); // 重试
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        log.debug("发送管理员消息: {}", message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectTagValue, IncorrectDataFormat, RejectLogon {
        log.debug("接收管理员消息: {}", message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        log.debug("发送应用消息: {}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType, IncorrectDataFormat {
        log.info("接收应用消息: {}", message);
        
        // 使用消息路由机制
        crack(message, sessionId);
    }
    
    // 处理执行报告（Order反馈）
    public void onMessage(ExecutionReport report, SessionID sessionId) 
            throws FieldNotFound {
        log.info("接收到执行报告");
        
        String orderId = report.getString(ClOrdID.FIELD);
        String symbol = report.getString(Symbol.FIELD);
        char ordStatus = report.getChar(OrdStatus.FIELD);
        double cumQty = report.getDouble(CumQty.FIELD);
        double leavesQty = report.getDouble(LeavesQty.FIELD);
        
        String statusText = getStatusText(ordStatus); // 转换状态为可读文本
        log.info("订单状态更新 - ID: {}, 状态: {}, 已成交: {}, 剩余: {}",
                orderId, statusText, cumQty, leavesQty);
    }
    
    // 状态码转换方法
    private String getStatusText(char status) {
        switch(status) {
            case OrdStatus.NEW: return "新订单";
            case OrdStatus.PARTIALLY_FILLED: return "部分成交";
            case OrdStatus.FILLED: return "全部成交";
            case OrdStatus.CANCELED: return "已取消";
            default: return Character.toString(status);
        }
    }

    // 发送示例订单
    private void sendSampleOrder() {
        if (!isLoggedOn || sessionId == null) {
            log.warn("尝试发送订单时未登录");
            return;
        }
        
        try {
            NewOrderSingle order = new NewOrderSingle();
            order.set(new ClOrdID("ORD" + System.currentTimeMillis()));  // 订单ID
            order.set(new Side(Side.BUY));                               // 方向：买
            order.set(new TransactTime());                               // 交易时间
            order.set(new OrdType(OrdType.LIMIT));                       // 限价单
            
            order.set(new Symbol("AAPL"));                        // 股票代码
            order.set(new OrderQty(100));                         // 数量
            order.set(new Price(150.25));                         // 价格
            
            // 使用正确的HandlInst值
            order.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION));
            
            // 发送订单
            Session.sendToTarget(order, sessionId);
            log.info("已发送新订单");
            
        } catch (SessionNotFound e) {
            log.error("发送订单时会话未找到: {}", e.getMessage());
        } catch (Exception e) {
            log.error("发送订单时出错: {}", e.getMessage());
        }
    }
}