import {
	AndroidConfig,
	type ConfigPlugin,
	withAndroidManifest,
	withInfoPlist,
} from "expo/config-plugins";

const withMyApiKey: ConfigPlugin<{ apiKey: string }> = (config, { apiKey }) => {
	let newConfig = withInfoPlist(config, (config) => {
		config.modResults.MY_CUSTOM_API_KEY = apiKey;
		return config;
	});

	newConfig = withAndroidManifest(config, (config) => {
		const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(
			config.modResults,
		);

		AndroidConfig.Manifest.addMetaDataItemToMainApplication(
			mainApplication,
			"MY_CUSTOM_API_KEY",
			apiKey,
		);
		return config;
	});

	return newConfig;
};

export default withMyApiKey;
