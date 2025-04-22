import {
	AndroidConfig,
	type ConfigPlugin,
	withAndroidManifest,
} from "expo/config-plugins";

const withAndroidManifestUpdates: ConfigPlugin = (config) => {
	return withAndroidManifest(config, (config) => {
		const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(
			config.modResults,
		);
		AndroidConfig.Manifest.addMetaDataItemToMainApplication(
			mainApplication,
			"com.google.mlkit.vision.DEPENDENCIES",
			"ocr",
		);

		config.modResults.manifest.application?.[0]?.activity?.push(
			{
				$: {
					"android:name":
						"expo.modules.letropassport.passportreader.ui.activities.CameraActivity",
					"android:screenOrientation": "landscape",
					"android:configChanges": "orientation|keyboardHidden|screenSize",
					"android:theme":
						"@style/Theme.AppCompat.Light.NoActionBar.FullScreen",
					"android:windowSoftInputMode": "stateAlwaysHidden",
				},
			},
			{
				$: {
					"android:name":
						"expo.modules.letropassport.passportreader.ui.activities.NfcActivity",
					"android:screenOrientation": "nosensor",
					"android:keepScreenOn": "true",
				},
			},
			{
				$: {
					"android:name":
						"expo.modules.letropassport.passportreader.ui.activities.SelectionActivity",
					"android:screenOrientation": "nosensor",
				},
			},
		);

		return config;
	});
};

// export default withAndroidManifestUpdates;
// const fs = require("react-native-fs");
// const path = require("path");

// const withAndroidManifestUpdates = (config) => {
// 	return withDangerousMod(config, [
// 		"android",
// 		async (config) => {
// 			const manifestPath = path.join(
// 				config.modRequest.platformProjectRoot,
// 				"app/src/main/AndroidManifest.xml",
// 			);

// 			let contents = await fs.promises.readFile(manifestPath, "utf8");

// 			const additions = `
//         <meta-data
//             android:name="com.google.mlkit.vision.DEPENDENCIES"
//             android:value="ocr" />

//         <activity android:name="expo.modules.letropassport.passportreader.ui.activities.CameraActivity"
//             android:screenOrientation="landscape"
//             android:configChanges="orientation|keyboardHidden|screenSize"
//             android:theme="@style/Theme.AppCompat.Light.NoActionBar.FullScreen"
//             android:windowSoftInputMode="stateAlwaysHidden" />

//         <activity android:name="expo.modules.letropassport.passportreader.ui.activities.NfcActivity"
//             android:screenOrientation="nosensor"
//             android:keepScreenOn="true" />

//         <activity android:name="expo.modules.letropassport.passportreader.ui.activities.SelectionActivity"
//             android:screenOrientation="nosensor" />
//       `;

// 			// Add the elements before the closing </application> tag
// 			if (
// 				!contents.includes(
// 					'android:name="com.google.mlkit.vision.DEPENDENCIES"',
// 				)
// 			) {
// 				contents = contents.replace(
// 					"</application>",
// 					`${additions}\n</application>`,
// 				);
// 				await fs.promises.writeFile(manifestPath, contents, "utf8");
// 			}

// 			return config;
// 		},
// 	]);
// };

export default withAndroidManifestUpdates;
