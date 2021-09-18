package software.plusminus.sync.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({ "software.plusminus.sync", "company.plusminus.data", "company.plusminus.patch" })
public class SyncConfig {
}
