package com.sec.trade.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class MainConsole {
	
	//启动主控制台循环
	 public static void start() {
	        Scanner scanner = new Scanner(System.in);
	        
	        while (true) {
	            // 显示主菜单
	            String command = display();
	            
	            // 处理用户命令
	            handleCommand(command);
	            
	            // 添加短暂延迟，避免过快循环
	            try {
	                Thread.sleep(100);
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	                break;
	            }
	        }
	    }
	// 使用专门的用户操作日志记录器
    private static final Logger userActionLogger = LoggerFactory.getLogger("USER_ACTION_LOGGER");
    
    public static String display() {
    	//记录用户访问菜单
    	userActionLogger.info("用户访问主菜单");
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n====== 证券交易系统主菜单 ======");
        System.out.println("1. 发送新订单");
        System.out.println("2. 查看订单状态");
        System.out.println("3. 查看执行报告");
        System.out.println("4. 系统设置");
        System.out.println("5. 退出系统");
        System.out.print("请选择操作: ");	
        
        return scanner.nextLine();
    }
    
    public static void handleCommand(String command) {
    	// 记录用户执行的操作
    	userActionLogger.info("用户执行操作: {}", command);
    	
        switch (command) {	
            case "1":
                System.out.println("发送新订单...");
                userActionLogger.info("用户选择发送新订单");
                // 调用订单发送逻辑
                break;
            case "2":
                System.out.println("查看订单状态...");
                // 调用订单查询逻辑
                break;
            case "3":
                System.out.println("查看执行报告...");
                // 调用报告查看逻辑
                break;
            case "4":
                System.out.println("系统设置...");
                // 调用系统设置逻辑
                break;
            case "5":
                System.out.println("退出系统...");
                System.exit(0);
            default:
                System.out.println("无效选择，请重新输入");
        }
    }
}
