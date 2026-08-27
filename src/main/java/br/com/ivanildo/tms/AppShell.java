package br.com.ivanildo.tms;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

@Push
@Theme("tms") // <-- Correção: passe o nome direto sem "name ="
public class AppShell implements AppShellConfigurator {
}