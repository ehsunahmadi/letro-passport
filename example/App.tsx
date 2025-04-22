import * as LetroPassport from "letro-passport";
import { useEffect } from "react";
import { Text, View } from "react-native";

export default function App() {
	useEffect(() => {
		const checkSupport = async () => {
			try {
				const isSupported = await LetroPassport.IsSupported;
				console.log("Is supported:", isSupported);
			} catch (error) {
				console.error("Error checking support:", error);
			}
		};

		checkSupport();
	}, []);
	return (
		<View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
			<Text>isSupported: {LetroPassport.hello()}</Text>
		</View>
	);
}
