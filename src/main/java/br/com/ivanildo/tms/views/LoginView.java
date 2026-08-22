package br.com.ivanildo.tms.views;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | TMS Logística")
@AnonymousAllowed
@CssImport("./styles/styles.css")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        getStyle()
            .set("background", "linear-gradient(135deg, #0f172a 0%, #1e293b 100%)")
            .set("font-family", "system-ui, -apple-system, sans-serif");

        VerticalLayout card = new VerticalLayout();
        card.setWidth("420px");
        card.setPadding(true);
        card.setSpacing(true);
        card.setAlignItems(Alignment.CENTER);

        card.getStyle()
            .set("background", "rgba(30, 41, 59, 0.85)")
            .set("backdrop-filter", "blur(12px)")
            .set("border", "1px solid rgba(255, 255, 255, 0.1)")
            .set("border-radius", "16px")
            .set("box-shadow", "0 25px 50px -12px rgba(0, 0, 0, 0.5)");

        Span icon = new Span("🚚");
        icon.getStyle().set("font-size", "42px");

        H1 title = new H1("Gestão Logística");
        title.getStyle()
            .set("color", "#f8fafc")
            .set("font-size", "22px")
            .set("font-weight", "700")
            .set("margin", "8px 0 0 0");

        Span subtitle = new Span("Acesse o painel operacional para continuar");
        subtitle.getStyle()
            .set("color", "#94a3b8")
            .set("font-size", "13px")
            .set("margin-bottom", "8px");

        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle(""); 
        i18nForm.setUsername("Usuário / E-mail");
        i18nForm.setPassword("Senha");
        i18nForm.setSubmit("Acessar Sistema");
        i18nForm.setForgotPassword("Esqueceu a senha?");
        i18n.setForm(i18nForm);

        LoginI18n.ErrorMessage i18nError = i18n.getErrorMessage();
        i18nError.setTitle("Erro de Autenticação");
        i18nError.setMessage("Usuário ou senha incorretos.");
        i18n.setErrorMessage(i18nError);

        login.setI18n(i18n);
        login.setAction("login");
        login.getElement().setAttribute("theme", "dark");
        login.getStyle().set("width", "100%");

        card.add(icon, title, subtitle, login);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}