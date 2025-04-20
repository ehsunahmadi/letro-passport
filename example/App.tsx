import * as LetroPassport from "letro-passport";
import { Text, View } from "react-native";

export default function App() {
	return (
		<View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
			<Text>API key: {LetroPassport.getApiKey()}</Text>
		</View>
	);
}
