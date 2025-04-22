import { type ConfigPlugin, withProjectBuildGradle } from "expo/config-plugins";

const withCustomProjectBuildGradle: ConfigPlugin = (config) => {
	return withProjectBuildGradle(config, (config) => {
		const injection = `
        configurations.configureEach {
            resolutionStrategy.dependencySubstitution {
                substitute(platform(module('com.gemalto.jp2:jp2-android'))) using module('com.github.Tgo1014:JP2ForAndroid:1.0.4')
            }
            resolutionStrategy.force 'com.google.guava:guava:31.1-android'
        }`;

		// Note the escaped "\{" and the assignment back to contents
		const allprojectsRegex = /(allprojects\s*\{)/;
		config.modResults.contents = config.modResults.contents.replace(
			allprojectsRegex,
			(_match, open) => `${open}${injection}`,
		);

		return config;
	});
};

export default withCustomProjectBuildGradle;
