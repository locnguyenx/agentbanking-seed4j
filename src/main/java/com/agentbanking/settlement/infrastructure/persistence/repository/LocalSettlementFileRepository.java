package com.agentbanking.settlement.infrastructure.persistence.repository;

import com.agentbanking.settlement.domain.port.out.SettlementFileRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class LocalSettlementFileRepository implements SettlementFileRepository {

  private final Path baseDir;

  public LocalSettlementFileRepository(
      @Value("${settlement.file.base-dir:./settlement-files}") String baseDir) {
    this.baseDir = Paths.get(baseDir);
  }

  @Override
  public Path saveSettlementFile(String filename, String content) {
    try {
      Files.createDirectories(baseDir);
      Path file = baseDir.resolve(filename);
      Files.writeString(file, content);
      return file;
    } catch (Exception e) {
      throw new RuntimeException("Failed to save settlement file: " + filename, e);
    }
  }
}
