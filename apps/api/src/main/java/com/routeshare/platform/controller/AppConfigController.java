package com.routeshare.platform.controller;

import com.routeshare.appreadiness.service.AppReadinessService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class AppConfigController {
  private final AppReadinessService service;

  @GetMapping("/config")
  public Map<String, Object> config() {
    return service.appConfig();
  }
}
