package com.chenjia.mvc.service.impl;

import com.chenjia.mvc.annotaion.JService;

/**
 * @author chenjia@joyveb.com
 * @date 2020/11/3 12:35 下午
 */
@JService
public class JTestServiceImpl implements JTestService {
    @Override
    public String listClassName() {
        return "listClassName is come from database";
    }
}
