import type { ConfigPlugin } from "expo/config-plugins";
import withAndroidManifestUpdates from "./withAndroidManifestUpdate";
import withCustomGradleProperties from "./withCustomGradleProperties";
import withCustomProjectBuildGradle from "./withCustomProjectBuildGradle";

const withConfig: ConfigPlugin = (config) => {
	let newConfig = withCustomProjectBuildGradle(config);
	newConfig = withCustomGradleProperties(newConfig);
	newConfig = withAndroidManifestUpdates(newConfig);
	return newConfig;
};

export default withConfig;
