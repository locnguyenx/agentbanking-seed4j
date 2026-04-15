package com.agentbanking.billeradapter.infrastructure.registry;

import com.agentbanking.billeradapter.domain.model.Biller;
import com.agentbanking.billeradapter.domain.model.BillType;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BillerRegistry implements BillerRegistryPort {

  private static final List<Biller> BILLERS = List.of(
      new Biller("TNB", "Tenaga Nasional Berhad", BillType.UTILITY, "https://api.tnb.com.my", true, true),
      new Biller("MAXIS", "Maxis Berhad", BillType.TELECOM, "https://api.maxis.com.my", true, true),
      new Biller("DIGI", "Digi Telecommunications", BillType.TELECOM, "https://api.digi.com.my", true, true),
      new Biller("UNIFI", "Telekom Malaysia", BillType.TELECOM, "https://api.tm.com.my", true, true),
      new Biller("JOMPAY", "JomPAY", BillType.OTHER, "https://api.jompay.com.my", true, false),
      new Biller("ASTRO", "Astro", BillType.OTHER, "https://api.astro.com.my", true, true),
      new Biller("EPF", "KWSP", BillType.OTHER, "https://api.kwsp.com.my", true, false),
      new Biller("TM", "Telekom Malaysia", BillType.UTILITY, "https://api.tm.com.my", true, true)
  );

  @Override
  public Optional<Biller> findByCode(String billerCode) {
    return BILLERS.stream().filter(b -> b.billerCode().equalsIgnoreCase(billerCode)).findFirst();
  }

  @Override
  public List<Biller> findAll() {
    return BILLERS;
  }
}