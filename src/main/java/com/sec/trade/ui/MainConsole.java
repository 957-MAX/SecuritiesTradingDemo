package com.sec.trade.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class MainConsole {
	
	//����������̨ѭ��
	 public static void start() {
	        Scanner scanner = new Scanner(System.in);
	        
	        while (true) {
	            // ��ʾ���˵�
	            String command = display();
	            
	            // �����û�����
	            handleCommand(command);
	            
	            // ��Ӷ����ӳ٣��������ѭ��
	            try {
	                Thread.sleep(100);
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	                break;
	            }
	        }
	    }
	// ʹ��ר�ŵ��û�������־��¼��
    private static final Logger userActionLogger = LoggerFactory.getLogger("USER_ACTION_LOGGER");
    
    public static String display() {
    	//��¼�û����ʲ˵�
    	userActionLogger.info("�û��������˵�");
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n====== ֤ȯ����ϵͳ���˵� ======");
        System.out.println("1. �����¶���");
        System.out.println("2. �鿴����״̬");
        System.out.println("3. �鿴ִ�б���");
        System.out.println("4. ϵͳ����");
        System.out.println("5. �˳�ϵͳ");
        System.out.print("��ѡ�����: ");	
        
        return scanner.nextLine();
    }
    
    public static void handleCommand(String command) {
    	// ��¼�û�ִ�еĲ���
    	userActionLogger.info("�û�ִ�в���: {}", command);
    	
        switch (command) {	
            case "1":
                System.out.println("�����¶���...");
                userActionLogger.info("�û�ѡ�����¶���");
                // ���ö��������߼�
                break;
            case "2":
                System.out.println("�鿴����״̬...");
                // ���ö�����ѯ�߼�
                break;
            case "3":
                System.out.println("�鿴ִ�б���...");
                // ���ñ���鿴�߼�
                break;
            case "4":
                System.out.println("ϵͳ����...");
                // ����ϵͳ�����߼�
                break;
            case "5":
                System.out.println("�˳�ϵͳ...");
                System.exit(0);
            default:
                System.out.println("��Чѡ������������");
        }
    }
}
