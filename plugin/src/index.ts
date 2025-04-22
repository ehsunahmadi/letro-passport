import type { ConfigPlugin } from "expo/config-plugins";
import withAndroidManifestUpdates from "./withAndroidManifestUpdate";
import withCustomGradleProperties from "./withCustomGradleProperties";
import withCustomProjectBuildGradle from "./withCustomProjectBuildGradle";
import withMyApiKey from "./withMyApiKey";

const withConfig: ConfigPlugin<{ apiKey: string }> = (config, { apiKey }) => {
	let newConfig = withMyApiKey(config, { apiKey });
	newConfig = withCustomProjectBuildGradle(newConfig);
	newConfig = withCustomGradleProperties(newConfig);
	newConfig = withAndroidManifestUpdates(newConfig);
	return newConfig;
};

export default withConfig;
