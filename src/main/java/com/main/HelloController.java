package com.main;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloController {

  @Autowired
  public HelloController(CompileService service) {
    service.test();
  }

  @RequestMapping("/")
  public String index() {
    return "Greetings from Spring Boot!";
  }

}