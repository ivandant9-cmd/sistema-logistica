import 'construct-style-sheets-polyfill';
import { applyTheme as applyBaseTheme } from './theme-lumo.generated.js';
import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';
import './theme-tms-custom.components.generated.js';
let needsReloadOnChanges = false;
import { color } from '@vaadin/vaadin-lumo-styles/color.js';
import '@vaadin/vaadin-lumo-styles/color-global.js';
import { typography } from '@vaadin/vaadin-lumo-styles/typography.js';
import '@vaadin/vaadin-lumo-styles/typography-global.js';
import stylesCss from 'themes/tms-custom/styles.css?inline';

  let themeRemovers = new WeakMap();
  let targets = [];

  export const applyTheme = (target) => {
    const removers = [];
    if (target !== document) {
      removers.push(injectGlobalCss(color.cssText, '', target, true));
removers.push(injectGlobalCss(typography.cssText, '', target, true));
removers.push(injectGlobalCss(stylesCss.toString(), '', target));
    
    }
    applyBaseTheme(target);

    

    if (import.meta.hot) {
      targets.push(new WeakRef(target));
      themeRemovers.set(target, removers);
    }

  }
  

if (import.meta.hot) {
  import.meta.hot.accept((module) => {

    if (needsReloadOnChanges) {
      window.location.reload();
    } else {
      targets.forEach(targetRef => {
        const target = targetRef.deref();
        if (target) {
          themeRemovers.get(target).forEach(remover => remover())
          module.applyTheme(target);
        }
      })
    }
  });

  import.meta.hot.on('vite:afterUpdate', (update) => {
    document.dispatchEvent(new CustomEvent('vaadin-theme-updated', { detail: update }));
  });
}

