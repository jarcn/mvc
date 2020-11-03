package com.chenjia.mvc.ctrl;

import com.chenjia.mvc.annotaion.JAutowired;
import com.chenjia.mvc.annotaion.JController;
import com.chenjia.mvc.annotaion.JRequestMapping;
import com.chenjia.mvc.service.impl.JTestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author chenjia@joyveb.com
 * @date 2020/11/3 12:36 下午
 */
@JController
@JRequestMapping("/test")
public class JTestCtrl {

    @JAutowired
    private JTestService jTestService;

    @JRequestMapping("/query")
    public void jTest(HttpServletRequest req, HttpServletResponse resp) {
        if (req.getParameter("username") == null) {
            try {
                resp.getWriter().write("param username is null");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String paramName = req.getParameter("username");
            try {
                resp.getWriter().write("param username is " + paramName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("[INFO-req] New request param username-->" + paramName);
        }
    }

    @JRequestMapping("/listClassName")
    public void listClassName(HttpServletRequest req, HttpServletResponse resp) {
        String str = jTestService.listClassName();
        System.out.println("testXService----------=-=-=>" + str);
        try {
            resp.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
