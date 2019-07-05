package com.example.demo.controller;

import com.example.demo.Pic;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@CrossOrigin
@Controller
public class TestController {

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public String upload(HttpServletRequest request, @RequestParam("file") MultipartFile multipartFile) {
        System.out.println("123");
        System.out.println(multipartFile.getOriginalFilename());
//        return Pic.MultipartFileToFile(multipartFile, request);
        Pic.MultipartFileToFile(multipartFile, request, true);
        return "ss";
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index() {
        System.out.println("index");
        return "upload";
    }
}
