package site.minnan.robotmanage.infrastructure;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class BasisErrorPageRegistrar implements ErrorPageRegistrar {


    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        ErrorPage[] errorPages = new ErrorPage[] {
                new ErrorPage(HttpStatus.NOT_FOUND, "/404")
        };
        registry.addErrorPages(errorPages);
    }
}
