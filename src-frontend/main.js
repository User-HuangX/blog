import {AllCommunityModule, ModuleRegistry} from "ag-grid-community";
import {createApp} from "vue";
import App from "./App.vue";
import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-quartz.css";
import "./style.css";

ModuleRegistry.registerModules([AllCommunityModule]);

createApp(App).mount("#app");
