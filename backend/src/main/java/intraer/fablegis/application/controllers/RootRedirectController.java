package intraer.fablegis.application.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

// A raiz do backend não é API nenhuma -- este backend só serve /v1/**, /oauth2/**,
// /login e /logout. Sem esta rota, qualquer navegação que acabe caindo em "/" (ex.:
// mismatch de host entre app.oauth2.issuer/app.frontend.login-url e a origem real
// do navegador -- localhost vs 127.0.0.1 são origens DIFERENTES para cookie/
// sessionStorage, e o Authorization Server não acha a authorization request salva
// nesse caso, caindo no fallback "/" do formLogin) mostra só o
// {"message":"Não autenticado."} cru do resource server, sem explicação nenhuma.
// Redireciona pra tela de login da SPA em vez disso -- degradação graciosa, não
// um comportamento esperado do fluxo normal (se isso disparar com frequência, é
// sinal de host mismatch na configuração, ver docs/instalacao.md).
@Controller
public class RootRedirectController {

    @Value("${app.frontend.login-url}")
    private String frontendLoginUrl;

    // ResponseEntity com Location, não "redirect:" (view name) -- este app não
    // tem ViewResolver configurado (é uma API REST pura, nunca precisou de
    // um), então "redirect:" quebrava com "Could not resolve view".
    @GetMapping("/")
    public ResponseEntity<Void> raiz() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendLoginUrl))
                .build();
    }
}
