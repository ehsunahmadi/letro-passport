import type { ConfigPlugin } from "expo/config-plugins";
import withAndroidManifestUpdates from "./withAndroidManifestUpdate";
import withCustomGradleProperties from "./withCustomGradleProperties";
import withCustomProjectBuildGradle from "./withCustomProjectBuildGradle";
import withPbxProjeModification from "./withPbxProjeModification";

const withConfig: ConfigPlugin = (config) => {
	let newConfig = withCustomProjectBuildGradle(config);
	newConfig = withCustomGradleProperties(newConfig);
	newConfig = withAndroidManifestUpdates(newConfig);
	newConfig = withPbxProjeModification(newConfig);
	return newConfig;
};

export default withConfig;
