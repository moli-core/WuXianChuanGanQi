package com.wireless.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class IndexController {

    // 首页由 static/index.html 提供

    /** 数字孪生快捷入口 */
    @GetMapping("/3d")
    public String digitalTwin() {
        return "redirect:/digital-twin/index.html";
    }
}
