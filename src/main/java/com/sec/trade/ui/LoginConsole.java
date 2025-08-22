package com.sec.trade.ui;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginConsole {

	private static final Logger userActionLogger = LoggerFactory.getLogger("USER_ACTION_LOGGER");
    private static final String ADMIN_USERNAME = "admin123";
    private static final String ADMIN_PASSWORD = "123456";
    
    public static boolean authenticate() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("====== 证券交易系统登录 ======");
        
        int attempts = 0;
        while (attempts < 3) {
            System.out.print("用户名: ");
            String username = scanner.nextLine();
            
            System.out.print("密码: ");
            String password = scanner.nextLine();
            
            // 记录登录尝试
            userActionLogger.info("登录尝试 - 用户名: {}", username);
            
            if (authenticate(username, password)) {
                userActionLogger.info("登录成功 - 用户名: {}", username);
                System.out.println("✅ 登录成功!");
                return true;
            } else {
                attempts++;
                userActionLogger.warn("登录失败 - 用户名: {}, 尝试次数: {}", username, attempts);
                System.out.println("❌ 用户名或密码错误，剩余尝试次数: " + (3 - attempts));
            }
        }
        
        userActionLogger.error("登录失败达到最大尝试次数");
        System.out.println("⚠️ 登录失败，系统退出");
        return false;
    }
    
    private static boolean authenticate(String username, String password) {
        return ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password);
    }
}
