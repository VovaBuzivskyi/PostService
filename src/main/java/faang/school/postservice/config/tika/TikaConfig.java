package faang.school.postservice.config.tika;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TikaConfig {

    @Bean
    public Tika tika() {
        return new Tika();
    }
}
