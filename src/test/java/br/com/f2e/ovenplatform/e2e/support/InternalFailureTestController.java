package br.com.f2e.ovenplatform.e2e.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalFailureTestController {

  @GetMapping("/test/internal-failure")
  void failUnexpectedly() {
    throw new RuntimeException("Simulated internal failure");
  }
}
