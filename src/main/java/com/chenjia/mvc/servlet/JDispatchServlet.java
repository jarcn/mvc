package com.chenjia.mvc.servlet;

import com.chenjia.mvc.annotaion.JAutowired;
import com.chenjia.mvc.annotaion.JController;
import com.chenjia.mvc.annotaion.JRequestMapping;
import com.chenjia.mvc.annotaion.JService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author chenjia@joyveb.com
 * @date 2020/11/3 10:38 上午
 */
public class JDispatchServlet extends HttpServlet {

    //加载系统参数
    private Properties contextProperties = new Properties();
    //存储所有bean的ClassName
    private List<String> classNameList = new ArrayList<>();
    //IOC容器
    private Map<String, Object> iocMap = new HashMap<>();
    //HandlerMapping
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            this.doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1:加载配置文件
        this.doLoadConfig(config);
        //2:扫描相关的类
        this.doScanner(contextProperties.getProperty("scan-package"));
        //3:初始化IOC容器，将所有相关的类实例保存到IOC容器中
        this.doInstance();
        //4:依赖注入
        this.doAutowired();
        //5:初始化 HandlerMapping
        this.initHandlerMapping();
        System.out.println("--------mvc is init---------");
        //6:打印数据
        this.doTestPrintData();
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        requestURI = requestURI.replaceAll(contextPath, "").replaceAll("/+", "/");
        System.out.println("info:7 request url-->" + requestURI);
        if (!this.handlerMapping.containsKey(requestURI)) {
            resp.getWriter().write("404 not found!");
            return;
        }
        Method method = this.handlerMapping.get(requestURI);
        System.out.println("info:7 method --->" + method);
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        System.out.println("info:7 iocMap.get(beanName)->" + iocMap.get(beanName));
        method.invoke(iocMap.get(beanName), req, resp);
        System.out.println("info:7 method.invoke put {" + iocMap.get(beanName) + "}.");
    }

    private void doTestPrintData() {
        System.out.println("[INFO-6]----data------------------------");
        System.out.println("contextConfig.propertyNames()-->" + contextProperties.propertyNames());
        System.out.println("[classNameList]-->");
        for (String str : classNameList) {
            System.out.println(str);
        }
        System.out.println("[iocMap]-->");
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            System.out.println(entry);
        }
        System.out.println("[handlerMapping]-->");
        for (Map.Entry<String, Method> entry : handlerMapping.entrySet()) {
            System.out.println(entry);
        }
        System.out.println("[INFO-6]----done-----------------------");
        System.out.println("====启动成功====");
    }

    private void initHandlerMapping() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(JController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(JRequestMapping.class)) {
                JRequestMapping jRequestMapping = clazz.getAnnotation(JRequestMapping.class);
                baseUrl = jRequestMapping.value();
            }
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(JRequestMapping.class)) {
                    continue;
                }
                JRequestMapping jRequestMapping = method.getAnnotation(JRequestMapping.class);
                String url = ("/" + baseUrl + "/" + jRequestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("info:5 handlerMapping put {" + url + "} - {" + method + "}.");
            }
        }
    }

    private void doAutowired() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (!field.isAnnotationPresent(JAutowired.class)) {
                    continue;
                }
                System.out.println("info4:Existence XAutowired.");
                JAutowired jAutowired = field.getAnnotation(JAutowired.class);
                String beanName = jAutowired.value().trim();
                if ("".equals(beanName)) {
                    System.out.println("info: jAutowired.value() is null");
                    beanName = field.getType().getName();
                }
                //反射权限
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), iocMap.get(beanName));
                    System.out.println("info:4 field set {" + entry.getValue() + "} - {" + iocMap.get(beanName) + "}.");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void doInstance() {
        if (classNameList.isEmpty()) {
            return;
        }
        try {
            for (String className : classNameList) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(JController.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();
                    iocMap.put(beanName, instance);
                    System.out.println("info:{" + beanName + "} has been saved in iocMap");
                } else if (clazz.isAnnotationPresent(JService.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    JService jService = clazz.getAnnotation(JService.class);
                    if (!"".equals(jService.value())) {
                        beanName = jService.value();
                    }
                    Object instance = clazz.newInstance();
                    iocMap.put(beanName, instance);
                    System.out.println("info:{" + beanName + "} has been saved in iocMap");
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (iocMap.containsKey(i.getName())) {
                            throw new RuntimeException("the bean name is exist.");
                        }
                        iocMap.put(i.getName(), instance);
                        System.out.println("[INFO-3] {" + i.getName() + "} has been saved in iocMap.");
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    private void doScanner(String packageUrl) {
        URL resource = this.getClass().getClassLoader().getResource("/" + packageUrl.replaceAll("\\.", "/"));
        if (null == resource) {
            return;
        }
        File classPath = new File(resource.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                System.out.println("info:[" + file.getName() + "]is Directory");
                doScanner(packageUrl + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    System.out.println("info: {" + file.getName() + "} is not a class file.");
                    continue;
                }
                String className = packageUrl + "." + file.getName().replace(".class", "");
                classNameList.add(className);
                System.out.println("info:{" + className + "} has been saved in classNameList.");
            }
        }
    }

    private void doLoadConfig(ServletConfig config) {
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextProperties.load(resourceAsStream);
            System.out.println("step1:property file has been loaded into contextProperties");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                resourceAsStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
