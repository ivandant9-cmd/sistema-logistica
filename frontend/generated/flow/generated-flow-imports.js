import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';

import { css, unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';
import $cssFromFile_0 from 'Frontend/styles/styles.css?inline';

injectGlobalCss($cssFromFile_0.toString(), 'CSSImport end', document);
import $cssFromFile_1 from 'Frontend/styles/dashboard-styles.css?inline';

injectGlobalCss($cssFromFile_1.toString(), 'CSSImport end', document);
import $cssFromFile_2 from 'Frontend/styles/vaadin-grid-custom.css?inline';
const $css_2 = typeof $cssFromFile_2  === 'string' ? unsafeCSS($cssFromFile_2) : $cssFromFile_2;
registerStyles('vaadin-grid', $css_2, {moduleId: 'flow_css_mod_2'});
import $cssFromFile_3 from 'Frontend/styles/vaadin-form-fields-custom.css?inline';
const $css_3 = typeof $cssFromFile_3  === 'string' ? unsafeCSS($cssFromFile_3) : $cssFromFile_3;
registerStyles('vaadin-text-field', $css_3, {moduleId: 'flow_css_mod_3'});
import $cssFromFile_4 from 'Frontend/styles/vaadin-form-fields-custom.css?inline';
const $css_4 = typeof $cssFromFile_4  === 'string' ? unsafeCSS($cssFromFile_4) : $cssFromFile_4;
registerStyles('vaadin-date-picker', $css_4, {moduleId: 'flow_css_mod_4'});
import $cssFromFile_5 from 'Frontend/styles/vaadin-form-fields-custom.css?inline';
const $css_5 = typeof $cssFromFile_5  === 'string' ? unsafeCSS($cssFromFile_5) : $cssFromFile_5;
registerStyles('vaadin-select', $css_5, {moduleId: 'flow_css_mod_5'});
import $cssFromFile_6 from 'Frontend/styles/vaadin-form-fields-custom.css?inline';
const $css_6 = typeof $cssFromFile_6  === 'string' ? unsafeCSS($cssFromFile_6) : $cssFromFile_6;
registerStyles('vaadin-combo-box', $css_6, {moduleId: 'flow_css_mod_6'});
import $cssFromFile_7 from 'Frontend/styles/vaadin-form-fields-custom.css?inline';
const $css_7 = typeof $cssFromFile_7  === 'string' ? unsafeCSS($cssFromFile_7) : $cssFromFile_7;
registerStyles('vaadin-text-area', $css_7, {moduleId: 'flow_css_mod_7'});
import $cssFromFile_8 from 'Frontend/styles/vaadin-dialog-custom.css?inline';
const $css_8 = typeof $cssFromFile_8  === 'string' ? unsafeCSS($cssFromFile_8) : $cssFromFile_8;
registerStyles('vaadin-dialog-overlay', $css_8, {moduleId: 'flow_css_mod_8'});
import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/login/theme/lumo/vaadin-login-form.js';
import '@vaadin/vertical-layout/theme/lumo/vaadin-vertical-layout.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import '@vaadin/combo-box/theme/lumo/vaadin-combo-box.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-column.js';
import '@vaadin/tooltip/theme/lumo/vaadin-tooltip.js';
import '@vaadin/icon/theme/lumo/vaadin-icon.js';
import '@vaadin/upload/theme/lumo/vaadin-upload.js';
import '@vaadin/context-menu/theme/lumo/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/form-layout/theme/lumo/vaadin-form-item.js';
import '@vaadin/multi-select-combo-box/theme/lumo/vaadin-multi-select-combo-box.js';
import '@vaadin/grid/theme/lumo/vaadin-grid.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-sorter.js';
import '@vaadin/checkbox/theme/lumo/vaadin-checkbox.js';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import '@vaadin/button/theme/lumo/vaadin-button.js';
import 'Frontend/generated/jar-resources/buttonFunctions.js';
import '@vaadin/text-field/theme/lumo/vaadin-text-field.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/form-layout/theme/lumo/vaadin-form-layout.js';
import '@vaadin/dialog/theme/lumo/vaadin-dialog.js';
import '@vaadin/horizontal-layout/theme/lumo/vaadin-horizontal-layout.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-column-group.js';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import '@vaadin/notification/theme/lumo/vaadin-notification.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/color-global.js';
import '@vaadin/vaadin-lumo-styles/typography-global.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '8ce998c8528d62306773b4edcf7fe08e6aa297a7b9c5277bbc07213d15158f5c') {
    pending.push(import('./chunks/chunk-7a9e716fa58e39312f7ac8215392814597420ab49cde388dd17e9d21eb091e38.js'));
  }
  if (key === '227044da9c2f4fec0f11ff734c50e4d9ba7818045c2cefc7e5dc91ad83d73b6f') {
    pending.push(import('./chunks/chunk-0faf36de7dc9cdbd17f27e6d5842a17d66fe90f617c6e4661ec8e09149942620.js'));
  }
  if (key === 'a5bab6dea61f61ac6600b89c30c730f072810c1f143ee8e43e1ee6323ef523ef') {
    pending.push(import('./chunks/chunk-11c49461b9e917c8f95af9a39722d5d4e32ffedffe21c251018787012a60193a.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}